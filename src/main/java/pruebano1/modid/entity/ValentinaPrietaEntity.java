package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.ModParticles;
import pruebano1.modid.ModSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Secuaz de la Dungeon "Isla Epstein": Valentina Prieta (4ta integrante de la familia de Niños)
 * - Mitad del tamaño del jugador (escala 0.5), 10 corazones de vida (20.0D) y 2 puntos de armadura (2.0D).
 * - Visión de Creeper normal (16 bloques).
 * - Ataca cuerpo a cuerpo con un pescado crudo: 2.5 corazones de daño (5.0D) + empujón/tropiezo y sonido de pescado mojado.
 * - Torpeza: Cada 12 segundos (240 ticks) caminando se tropieza sola, quitándose 1 corazón (2.0F). Sus tropiezos sí pueden matarla.
 * - Agua: No puede sumergirse (flota en la superficie) y se mueve extremadamente rápido en el agua.
 * - Al morir: Explota con la mitad de daño/fuerza que un Creeper normal (1.5F) SIN romper bloques, reproduciendo el audio personalizado
 *   junto con la explosión normal y lanzando partículas personalizadas de Hello Kitty.
 * - Drops: 1 Pescado (Bacalao crudo), 5 XP y poca probabilidad de pólvora u oro.
 */
public class ValentinaPrietaEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation TRIP_ANIM = RawAnimation.begin().thenPlay("trip");

    // 12 segundos caminando = 240 ticks (20 ticks por segundo)
    private static final int TRIP_INTERVAL_TICKS = 240;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int walkingTicks = 0;
    private boolean hasExplodedOnDeath = false;

    public ValentinaPrietaEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5; // 5 de XP al morir
    }

    public static DefaultAttributeContainer.Builder createValentinaPrietaAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)        // 10 corazones (familia de Niños)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.225D)   // Velocidad reducida un 10% (de 0.25D a 0.225D)
                .add(EntityAttributes.ATTACK_DAMAGE, 5.0D)      // 2.5 corazones por golpe de pescado
                .add(EntityAttributes.ARMOR, 2.0D)              // 2 puntos de armadura (familia de Niños)
                .add(EntityAttributes.FOLLOW_RANGE, 16.0D)      // Visión de un Creeper normal (16 bloques)
                .add(EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        // Comportamiento en el agua: NO puede sumergirse (flota encima) y se vuelve extremadamente rápida
        if (this.isTouchingWater() || this.isSubmergedIn(FluidTags.WATER)) {
            Vec3d vel = this.getVelocity();
            double floatUpY = Math.max(0.10D, vel.y + 0.08D);

            // Impulso horizontal extremo hacia su objetivo o dirección de movimiento en el agua
            double boostX = vel.x * 1.65D;
            double boostZ = vel.z * 1.65D;
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0.001D) {
                    // Velocidad turbo sobre el agua (reducida un 10%: de 0.52D a 0.468D)
                    boostX = (dx / dist) * 0.468D;
                    boostZ = (dz / dist) * 0.468D;
                }
            } else {
                double horizSpeed = Math.sqrt(boostX * boostX + boostZ * boostZ);
                if (horizSpeed > 0.495D) {
                    boostX = (boostX / horizSpeed) * 0.495D;
                    boostZ = (boostZ / horizSpeed) * 0.495D;
                }
            }

            this.setVelocity(boostX, floatUpY, boostZ);
            this.OnGroundSetter();
        }
    }

    private void OnGroundSetter() {
        this.fallDistance = 0.0F;
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        // Contador de tropiezos: cada 12 segundos (240 ticks) caminando se tropieza y pierde 0.5 corazones (1.0F)
        double horizontalSpeedSq = this.getVelocity().horizontalLengthSquared();
        boolean isMoving = horizontalSpeedSq > 0.0008D || this.getNavigation().isFollowingPath();

        if (this.isAlive() && isMoving && !this.isTouchingWater()) {
            this.walkingTicks++;
            if (this.walkingTicks >= TRIP_INTERVAL_TICKS) {
                this.walkingTicks = 0;
                this.performClumsyTrip(world);
            }
        }
    }

    private void performClumsyTrip(ServerWorld world) {
        // Animación de tropiezo hacia adelante
        this.triggerAnim("controller", "trip");

        // Pequeño empujón torpe hacia adelante al tropezarse
        Vec3d look = this.getRotationVec(1.0F);
        this.addVelocity(look.x * 0.25D, 0.15D, look.z * 0.25D);
        this.velocityModified = true;

        // Sonido de tropiezo y pescado mojado
        world.playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.ENTITY_COD_FLOP,
                SoundCategory.HOSTILE,
                1.0F,
                0.85F
        );
        world.playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.ENTITY_PLAYER_SMALL_FALL,
                SoundCategory.HOSTILE,
                1.0F,
                1.1F
        );

        // Quitarse 1 corazón completo (2.0F de daño); sí puede morir y explotar por sus propios tropiezos
        this.damage(world, this.getDamageSources().fall(), 2.0F);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked) {
            // Reproducir animación de pescadazo
            this.triggerAnim("controller", "attack");

            // Sonido de pescado mojado que golpea
            world.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.ENTITY_COD_FLOP,
                    SoundCategory.HOSTILE,
                    1.4F,
                    0.95F + (this.random.nextFloat() * 0.2F)
            );
            world.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.ENTITY_SALMON_FLOP,
                    SoundCategory.HOSTILE,
                    1.2F,
                    1.05F
            );

            // Hacer tropezar / desestabilizar al jugador con el golpe de pescado
            Vec3d dir = new Vec3d(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ()).normalize();
            target.addVelocity(dir.x * 0.42D, 0.22D, dir.z * 0.42D);
            target.velocityModified = true;
        }
        return attacked;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);

        if (!this.getWorld().isClient() && !this.hasExplodedOnDeath && this.getWorld() instanceof ServerWorld serverWorld) {
            this.hasExplodedOnDeath = true;

            // 1. Explosión con la mitad de daño/intensidad de un Creeper normal (1.5F), y SIN romper bloques de la dungeon
            serverWorld.createExplosion(
                    this,
                    this.getX(),
                    this.getY() + 0.4D,
                    this.getZ(),
                    1.5F,
                    World.ExplosionSourceType.NONE
            );

            // 2. Reproducir el sonido adjunto de Valentina junto con el sonido normal de explosión de Creeper
            serverWorld.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    ModSounds.VALENTINA_EXPLOSION,
                    SoundCategory.HOSTILE,
                    2.2F,
                    1.0F
            );
            serverWorld.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(),
                    SoundCategory.HOSTILE,
                    1.5F,
                    1.0F
            );

            // 3. Efectos visuales y partículas personalizadas de Hello Kitty
            serverWorld.spawnParticles(
                    ModParticles.HELLO_KITTY,
                    this.getX(),
                    this.getY() + 0.6D,
                    this.getZ(),
                    45,
                    0.85D,
                    0.75D,
                    0.85D,
                    0.18D
            );
            serverWorld.spawnParticles(
                    ParticleTypes.HEART,
                    this.getX(),
                    this.getY() + 0.7D,
                    this.getZ(),
                    22,
                    0.9D,
                    0.7D,
                    0.9D,
                    0.12D
            );
            serverWorld.spawnParticles(
                    ParticleTypes.CHERRY_LEAVES,
                    this.getX(),
                    this.getY() + 0.6D,
                    this.getZ(),
                    35,
                    1.1D,
                    0.8D,
                    1.1D,
                    0.15D
            );
            serverWorld.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(),
                    this.getY() + 0.5D,
                    this.getZ(),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // 1. Siempre suelta su Pescado (1 Bacalao crudo)
        this.dropStack(world, new ItemStack(Items.COD, 1));

        // 2. Poca probabilidad (25%) de soltar 1 a 2 de Pólvora
        if (this.random.nextInt(100) < 25) {
            this.dropStack(world, new ItemStack(Items.GUNPOWDER, 1 + this.random.nextInt(2)));
        }

        // 3. Poca probabilidad (20%) de soltar Oro (1 lingote de oro o 1-3 pepitas de oro)
        int goldRoll = this.random.nextInt(100);
        if (goldRoll < 8) {
            this.dropStack(world, new ItemStack(Items.GOLD_INGOT, 1));
        } else if (goldRoll < 20) {
            this.dropStack(world, new ItemStack(Items.GOLD_NUGGET, 1 + this.random.nextInt(3)));
        }
    }

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
                })
                        .triggerableAnim("attack", ATTACK_ANIM)
                        .triggerableAnim("trip", TRIP_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
