package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Secuaz de la Dungeon "Isla Epstein": Yostin Biber
 * - Humanoide pequeño (mitad del tamaño del jugador: escala 0.5).
 * - 10 corazones de vida (20.0D) y 2 puntos de armadura (2.0D).
 * - Visión de zombie (35 bloques), velocidad de movimiento de zombie (0.23D) y velocidad de ataque de zombie (20 ticks).
 * - Ataque cuerpo a cuerpo de 3 corazones de daño (6.0D).
 * - Al morir: da 5 de experiencia (igual que un zombie) y poca probabilidad de soltar entre 0 y 3 lingotes de oro (siendo 3 el menos probable).
 */
public class YostinBiberEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public YostinBiberEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5; // Misma experiencia que un zombie (5 XP)
    }

    public static DefaultAttributeContainer.Builder createYostinBiberAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)        // 10 corazones (1 corazón = 2 puntos)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.23D)    // Misma velocidad de movimiento que un zombie
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0D)      // 3 corazones de daño cuerpo a cuerpo
                .add(EntityAttributes.ARMOR, 2.0D)              // 2 puntos de armadura
                .add(EntityAttributes.FOLLOW_RANGE, 35.0D);     // Misma visión que un zombie (35 bloques)
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // MeleeAttackGoal usa el cooldown estándar de 20 ticks (1 segundo), igual que el ataque de un zombie
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        // Cada uno ataca por su cuenta (sin llamar a manada) en cuanto ve al jugador o si es golpeado
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        boolean attacked = super.tryAttack(world, target);
        if (attacked) {
            // Reproducir animación de golpe cuerpo a cuerpo y sonido de golpe físico
            this.triggerAnim("controller", "attack");
            this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0F, 1.1F);
        }
        return attacked;
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // Probabilidad escalonada de soltar lingotes de oro (entre 0 y 3, siendo 3 el menos probable):
        // - 70% de probabilidad: 0 lingotes
        // - 18% de probabilidad: 1 lingote
        // -  9% de probabilidad: 2 lingotes
        // -  3% de probabilidad: 3 lingotes (el menos probable)
        int roll = this.random.nextInt(100);
        int goldCount = 0;
        if (roll < 3) {
            goldCount = 3;
        } else if (roll < 12) {
            goldCount = 2;
        } else if (roll < 30) {
            goldCount = 1;
        }

        if (goldCount > 0) {
            this.dropStack(world, new ItemStack(Items.GOLD_INGOT, goldCount));
        }
    }

    // Solo sonidos de pasos y golpes físicos (sin voces)
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
