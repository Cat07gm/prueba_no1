package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Secuaz Francotiradora de la Dungeon "Isla Epstein": Fer Ovilla
 * - Mitad del tamaño del jugador (escala 0.5), 10 corazones de vida (20.0D) y 2 de armadura (2.0D).
 * - Velocidad de movimiento lenta (0.18D) y ataque lento (cada 3.5s = 70 ticks).
 * - Se mantiene a larga distancia (hasta 22 bloques) e intenta alejarse si el jugador se acerca a menos de 8 bloques.
 * - Lanza un Plátano Bumerán 3D que gira en el aire, hace 5 corazones de daño (10.0F) y reproduce su sonido personalizado al impactar.
 * - Al morir: da 5 XP y probabilidad de soltar el ítem comestible "Plátano" y pepitas/lingote de oro.
 */
public class FerOvillaEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private static final double RETREAT_DISTANCE_SQ = 8.0D * 8.0D;   // Si estás a menos de 8 bloques, retrocede para tomar distancia
    private static final double IDEAL_MIN_DIST_SQ = 12.0D * 12.0D;   // Distancia cómoda mínima (12 bloques)
    private static final double MAX_ATTACK_RANGE_SQ = 22.0D * 22.0D; // Alcance máximo de francotiradora (22 bloques)

    private int attackCooldown = 50; // 3.5s (70 ticks) entre disparos
    private int attackWindup = -1;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public FerOvillaEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createFerOvillaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)      // 10 corazones
                .add(EntityAttributes.MOVEMENT_SPEED, 0.18D)  // Movimiento lento
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0D)   // 5 corazones
                .add(EntityAttributes.ARMOR, 2.0D)            // 2 puntos de armadura
                .add(EntityAttributes.FOLLOW_RANGE, 35.0D);   // Visión de 35 bloques
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.85D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.attackWindup = -1;
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        double distSq = this.squaredDistanceTo(target);

        // Movimiento de Francotiradora:
        // 1. Si el jugador se acerca a menos de 8 bloques, intenta alejarse caminando en dirección contraria.
        // 2. Si el jugador está a más de 18 bloques, se acerca un poco hasta tener tiro limpio.
        // 3. Entre 8 y 18 bloques, mantiene su posición para apuntar y disparar.
        if (this.attackWindup < 0) {
            if (distSq < RETREAT_DISTANCE_SQ) {
                Vec3d awayDir = this.getPos().subtract(target.getPos()).normalize();
                Vec3d retreatPos = this.getPos().add(awayDir.multiply(7.0D));
                this.getNavigation().startMovingTo(retreatPos.x, retreatPos.y, retreatPos.z, 1.1D);
            } else if (distSq > 18.0D * 18.0D || !this.canSee(target)) {
                this.getNavigation().startMovingTo(target, 1.0D);
            } else if (distSq >= IDEAL_MIN_DIST_SQ) {
                this.getNavigation().stop();
            }
        } else {
            this.getNavigation().stop();
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        // Procesar animación y lanzamiento del Plátano Bumerán
        if (this.attackWindup >= 0) {
            this.attackWindup++;

            // En el segundo 0.5 de la animación "attack" (tick 10), lanza el proyectil 3D de plátano bumerán
            if (this.attackWindup == 10) {
                throwBananaBoomerang(world, target);
            }

            // Fin de la animación "attack" (1.0s = 20 ticks)
            if (this.attackWindup >= 20) {
                this.attackWindup = -1;
                this.attackCooldown = 70; // 3.5 segundos (70 ticks) de recarga lenta
            }
        } else if (this.attackCooldown <= 0 && distSq <= MAX_ATTACK_RANGE_SQ && this.canSee(target)) {
            this.attackWindup = 0;
            this.triggerAnim("controller", "attack");
        }
    }

    private void throwBananaBoomerang(ServerWorld world, LivingEntity target) {
        BananaProjectileEntity banana = new BananaProjectileEntity(world, this);

        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.45D) - banana.getY();
        double dz = target.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Disparar el plátano bumerán hacia el jugador
        banana.setVelocity(dx, dy + horizontalDist * 0.04D, dz, 1.35F, 1.0F);
        world.spawnEntity(banana);

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 1.0F, 0.85F
        );
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // 50% de probabilidad de soltar 1 o 2 Plátanos comestibles
        if (this.random.nextFloat() < 0.50F) {
            this.dropStack(world, new ItemStack(ModItem.BANANA, 1 + this.random.nextInt(2)));
        }

        // 30% de probabilidad de soltar pepitas de oro (1-3) y 8% de soltar 1 lingote de oro
        int roll = this.random.nextInt(100);
        if (roll < 8) {
            this.dropStack(world, new ItemStack(Items.GOLD_INGOT, 1));
        } else if (roll < 38) {
            this.dropStack(world, new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(3)));
        }
    }

    // Solo sonidos físicos y de pasos (sin voces)
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.15F, 1.25F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 4, state -> {
                    if (state.isMoving()) {
                        return state.setAndContinue(WALK_ANIM);
                    }
                    return state.setAndContinue(IDLE_ANIM);
                }).triggerableAnim("attack", ATTACK_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
