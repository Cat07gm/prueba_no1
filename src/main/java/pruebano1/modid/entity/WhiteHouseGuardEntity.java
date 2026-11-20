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
 * Agente de Seguridad / Servicio Secreto de la 3.ª Dungeon "La Casa Blanca".
 * Tres variantes armadas con estilos de la hoja de armas:
 * - PISTOL: dispara una bala constante cada 4 segundos (80 ticks).
 * - ASSAULT_RIFLE: dispara una ráfaga de 4 balas (daño individual menor que la pistola).
 * - BAZOOKA: dispara un cohete explosivo cada 20 segundos (400 ticks) sin destruir bloques.
 */
public class WhiteHouseGuardEntity extends HostileEntity implements GeoEntity {

    public enum WeaponStyle {
        PISTOL,
        ASSAULT_RIFLE,
        BAZOOKA
    }

    private static final int PISTOL_COOLDOWN_TICKS = 80;       // 4 segundos constantes
    private static final int RIFLE_BURST_COOLDOWN_TICKS = 90;  // 4.5 segundos entre ráfagas
    private static final int RIFLE_BURST_SHOTS = 4;            // Ráfaga de 4 disparos
    private static final int RIFLE_BURST_INTERVAL_TICKS = 5;   // Cada 0.25s dentro de la ráfaga
    private static final int BAZOOKA_COOLDOWN_TICKS = 400;    // 20 segundos constantes

    private static final double RETREAT_DISTANCE_SQ = 6.5D * 6.5D;
    private static final double IDEAL_MIN_DIST_SQ = 10.0D * 10.0D;
    private static final double APPROACH_DIST_SQ = 18.0D * 18.0D;
    private static final double MAX_ATTACK_RANGE_SQ = 24.0D * 24.0D;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");

    private final WeaponStyle weaponStyle;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int attackCooldown;
    private int burstRemaining = 0;
    private int burstTickTimer = 0;

    public WhiteHouseGuardEntity(EntityType<? extends HostileEntity> entityType, World world, WeaponStyle weaponStyle) {
        super(entityType, world);
        this.weaponStyle = weaponStyle;
        this.experiencePoints = 8; // Un poco más que mobs iniciales (5 XP)
        this.attackCooldown = switch (weaponStyle) {
            case PISTOL -> 30 + this.random.nextInt(40);
            case ASSAULT_RIFLE -> 30 + this.random.nextInt(45);
            case BAZOOKA -> 60 + this.random.nextInt(80);
        };
    }

    public static WhiteHouseGuardEntity createPistolGuard(EntityType<? extends HostileEntity> type, World world) {
        return new WhiteHouseGuardEntity(type, world, WeaponStyle.PISTOL);
    }

    public static WhiteHouseGuardEntity createRifleGuard(EntityType<? extends HostileEntity> type, World world) {
        return new WhiteHouseGuardEntity(type, world, WeaponStyle.ASSAULT_RIFLE);
    }

    public static WhiteHouseGuardEntity createBazookaGuard(EntityType<? extends HostileEntity> type, World world) {
        return new WhiteHouseGuardEntity(type, world, WeaponStyle.BAZOOKA);
    }

    public WeaponStyle getWeaponStyle() {
        return this.weaponStyle;
    }

    public static DefaultAttributeContainer.Builder createGuardAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 34.0D)          // 17 corazones (un poco más fuerte que los de Isla Penstein / Dungeon 2)
                .add(EntityAttributes.ARMOR, 6.0D)                // Chaleco táctico del Servicio Secreto
                .add(EntityAttributes.MOVEMENT_SPEED, 0.24D)      // Velocidad táctica equilibrada
                .add(EntityAttributes.ATTACK_DAMAGE, 7.0D)        // Respaldo cuerpo a cuerpo si acorralado
                .add(EntityAttributes.FOLLOW_RANGE, 38.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.85D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 26.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        boolean charlieCinematic = !world.getEntitiesByClass(
                CharlieKirkEntity.class,
                this.getBoundingBox().expand(240.0D),
                CharlieKirkEntity::isInAnyCinematic
        ).isEmpty();
        if (charlieCinematic) {
            this.getNavigation().stop();
            this.setVelocity(Vec3d.ZERO);
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.burstRemaining = 0;
            this.burstTickTimer = 0;
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            return;
        }

        this.getLookControl().lookAt(target, 32.0F, 32.0F);
        double distSq = this.squaredDistanceTo(target);
        boolean hasLineOfSight = this.canSee(target);

        if (this.burstRemaining > 0) {
            this.getNavigation().stop();
        } else if (distSq < RETREAT_DISTANCE_SQ && hasLineOfSight) {
            Vec3d away = this.getPos().subtract(target.getPos());
            Vec3d horiz = new Vec3d(away.x, 0.0D, away.z);
            if (horiz.lengthSquared() > 0.001D) {
                Vec3d retreat = this.getPos().add(horiz.normalize().multiply(6.0D));
                this.getNavigation().startMovingTo(retreat.x, retreat.y, retreat.z, 1.05D);
            }
        } else if (distSq > APPROACH_DIST_SQ || !hasLineOfSight) {
            this.getNavigation().startMovingTo(target, 1.05D);
        } else if (distSq >= IDEAL_MIN_DIST_SQ) {
            this.getNavigation().stop();
        }

        if (this.burstRemaining > 0) {
            if (!hasLineOfSight || distSq > MAX_ATTACK_RANGE_SQ * 1.35D) {
                this.burstRemaining = 0;
                this.burstTickTimer = 0;
                this.attackCooldown = RIFLE_BURST_COOLDOWN_TICKS;
                return;
            }
            if (this.burstTickTimer > 0) {
                this.burstTickTimer--;
            }
            if (this.burstTickTimer <= 0) {
                fireBullet(world, target, GuardBulletProjectileEntity.RIFLE_BULLET_DAMAGE, 2.6F);
                this.triggerAnim("controller", "attack");
                this.burstRemaining--;
                if (this.burstRemaining > 0) {
                    this.burstTickTimer = RIFLE_BURST_INTERVAL_TICKS;
                } else {
                    this.attackCooldown = RIFLE_BURST_COOLDOWN_TICKS;
                }
            }
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
            return;
        }

        if (distSq <= MAX_ATTACK_RANGE_SQ && hasLineOfSight) {
            switch (this.weaponStyle) {
                case PISTOL -> {
                    fireBullet(world, target, GuardBulletProjectileEntity.PISTOL_BULLET_DAMAGE, 2.35F);
                    this.triggerAnim("controller", "attack");
                    this.attackCooldown = PISTOL_COOLDOWN_TICKS;
                }
                case ASSAULT_RIFLE -> {
                    this.burstRemaining = RIFLE_BURST_SHOTS - 1;
                    this.burstTickTimer = RIFLE_BURST_INTERVAL_TICKS;
                    fireBullet(world, target, GuardBulletProjectileEntity.RIFLE_BULLET_DAMAGE, 2.6F);
                    this.triggerAnim("controller", "attack");
                }
                case BAZOOKA -> {
                    fireBazookaRocket(world, target);
                    this.triggerAnim("controller", "attack");
                    this.attackCooldown = BAZOOKA_COOLDOWN_TICKS;
                }
            }
        }
    }

    private void fireBullet(ServerWorld world, LivingEntity target, float damage, float speed) {
        GuardBulletProjectileEntity bullet = new GuardBulletProjectileEntity(world, this, damage);
        Vec3d muzzle = getMuzzlePosition();
        bullet.setPosition(muzzle.x, muzzle.y, muzzle.z);

        double dx = target.getX() - muzzle.x;
        double dy = target.getBodyY(0.55D) - muzzle.y;
        double dz = target.getZ() - muzzle.z;
        double spread = (this.weaponStyle == WeaponStyle.ASSAULT_RIFLE) ? 1.8D : 0.7D;
        bullet.setVelocity(dx, dy, dz, speed, (float) spread);
        world.spawnEntity(bullet);

        world.spawnParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.06D, 0.06D, 0.06D, 0.02D);
        world.spawnParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.03D, 0.03D, 0.03D, 0.01D);
        if (this.weaponStyle != WeaponStyle.ASSAULT_RIFLE || this.burstRemaining == RIFLE_BURST_SHOTS - 1) {
            world.spawnParticles(ModParticles.CHARLIE_WEAPON, muzzle.x, muzzle.y + 0.15D, muzzle.z, 1, 0.1D, 0.1D, 0.1D, 0.02D);
        }
        SoundEvent shotSound = (this.weaponStyle == WeaponStyle.ASSAULT_RIFLE)
                ? ModSounds.GUARD_RIFLE_SHOT
                : ModSounds.GUARD_PISTOL_SHOT;
        float volume = (this.weaponStyle == WeaponStyle.ASSAULT_RIFLE) ? 0.50F : 0.65F;
        float pitch = (this.weaponStyle == WeaponStyle.ASSAULT_RIFLE)
                ? (0.96F + this.random.nextFloat() * 0.08F)
                : (0.95F + this.random.nextFloat() * 0.10F);
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                shotSound, SoundCategory.HOSTILE, volume, pitch
        );
    }

    private void fireBazookaRocket(ServerWorld world, LivingEntity target) {
        GuardRocketProjectileEntity rocket = new GuardRocketProjectileEntity(world, this);
        Vec3d muzzle = getMuzzlePosition().add(0.0D, 0.25D, 0.0D);
        rocket.setPosition(muzzle.x, muzzle.y, muzzle.z);

        double dx = target.getX() - muzzle.x;
        double dy = target.getBodyY(0.45D) - muzzle.y;
        double dz = target.getZ() - muzzle.z;
        rocket.setVelocity(dx, dy, dz, 1.35F, 0.4F);
        world.spawnEntity(rocket);

        world.spawnParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 8, 0.18D, 0.18D, 0.18D, 0.04D);
        world.spawnParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 6, 0.12D, 0.12D, 0.12D, 0.03D);
        world.spawnParticles(ModParticles.CHARLIE_WEAPON, muzzle.x, muzzle.y + 0.2D, muzzle.z, 2, 0.15D, 0.15D, 0.15D, 0.03D);
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1.35F, 0.72F
        );
    }

    private Vec3d getMuzzlePosition() {
        float yawRad = (float) Math.toRadians(this.getYaw());
        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);
        return new Vec3d(
                this.getX() + lookX * 0.55D,
                this.getEyeY() - 0.22D,
                this.getZ() + lookZ * 0.55D
        );
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        if (target instanceof WhiteHouseGuardEntity
                || target instanceof CharlieKirkEntity
                || CharlieEpsteinSummonHelper.isEpsteinSummon(target)) {
            return false;
        }
        return super.canTarget(target);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return distanceSquared > 180.0D * 180.0D;
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);
        if (this.random.nextFloat() < 0.35F) {
            this.dropStack(world, new ItemStack(Items.IRON_NUGGET, 2 + this.random.nextInt(4)));
        }
        if (this.weaponStyle == WeaponStyle.BAZOOKA && this.random.nextFloat() < 0.45F) {
            this.dropStack(world, new ItemStack(Items.GUNPOWDER, 1 + this.random.nextInt(2)));
        }
        if (this.random.nextFloat() < 0.14F) {
            this.dropStack(world, new ItemStack(Items.GOLD_INGOT, 1));
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
        this.playSound(SoundEvents.ENTITY_ZOMBIE_STEP, 0.18F, 1.0F);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 3, state -> {
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
