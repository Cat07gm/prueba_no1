package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;

import java.util.List;

/**
 * Cohete explosivo disparado cada 20 segundos por la agente con Bazooka de La Casa Blanca
 * o por las bazookas flotantes de Charlie Kirk en Fase 2.
 * La detonación causa daño y empuje en área sin destruir absolutamente ningún bloque de la dungeon.
 */
public class GuardRocketProjectileEntity extends ThrownItemEntity implements FlyingItemEntity {

    public static final float DIRECT_HIT_DAMAGE = 14.0F;
    public static final float MAX_SPLASH_DAMAGE = 11.0F;
    public static final double EXPLOSION_RADIUS = 4.2D;

    private float directHitDamage = DIRECT_HIT_DAMAGE;
    private float maxSplashDamage = MAX_SPLASH_DAMAGE;

    public GuardRocketProjectileEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public GuardRocketProjectileEntity(World world, LivingEntity owner) {
        this(world, owner, DIRECT_HIT_DAMAGE, MAX_SPLASH_DAMAGE);
    }

    public GuardRocketProjectileEntity(World world, LivingEntity owner, float directDamage, float splashDamage) {
        super(ModEntities.GUARD_ROCKET_PROJECTILE, owner, world, new ItemStack(ModItem.GUARD_ROCKET));
        this.directHitDamage = directDamage;
        this.maxSplashDamage = splashDamage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItem.GUARD_ROCKET;
    }

    @Override
    protected double getGravity() {
        return 0.001D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.06D, 0.06D, 0.06D, 0.015D
            );
            serverWorld.spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    2, 0.08D, 0.08D, 0.08D, 0.01D
            );
            if (this.age % 3 == 0) {
                serverWorld.spawnParticles(
                        ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.05D, 0.05D, 0.05D, 0.01D
                );
            }
            if (this.age > 120) {
                this.discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (hitEntity == owner
                || hitEntity instanceof WhiteHouseGuardEntity
                || hitEntity instanceof CharlieKirkEntity
                || CharlieEpsteinSummonHelper.isEpsteinSummon(hitEntity)) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            hitEntity.damage(
                    serverWorld,
                    this.getDamageSources().thrown(this, owner != null ? owner : this),
                    this.directHitDamage
            );
            detonateSafely(serverWorld, entityHitResult.getPos(), hitEntity);
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            detonateSafely(serverWorld, blockHitResult.getPos(), null);
            this.discard();
        }
    }

    /**
     * Detona el cohete causando daño en área, partículas y sonido sin romper bloques de la Casa Blanca.
     */
    private void detonateSafely(ServerWorld serverWorld, Vec3d center, Entity directTarget) {
        Entity owner = this.getOwner();

        serverWorld.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.8F, 0.95F
        );
        serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.3D, center.z,
                1, 0.1D, 0.1D, 0.1D, 0.0D
        );
        serverWorld.spawnParticles(
                ParticleTypes.FLAME,
                center.x, center.y + 0.4D, center.z,
                28, 1.0D, 0.8D, 1.0D, 0.14D
        );
        serverWorld.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                center.x, center.y + 0.5D, center.z,
                22, 1.1D, 0.9D, 1.1D, 0.08D
        );

        Box area = new Box(
                center.x - EXPLOSION_RADIUS, center.y - EXPLOSION_RADIUS, center.z - EXPLOSION_RADIUS,
                center.x + EXPLOSION_RADIUS, center.y + EXPLOSION_RADIUS, center.z + EXPLOSION_RADIUS
        );
        List<LivingEntity> victims = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != owner
                        && !(e instanceof WhiteHouseGuardEntity)
                        && !(e instanceof CharlieKirkEntity)
                        && !CharlieEpsteinSummonHelper.isEpsteinSummon(e)
                        && e.isAlive()
                        && !e.isSpectator()
        );

        for (LivingEntity victim : victims) {
            double dist = Math.sqrt(victim.squaredDistanceTo(center));
            if (dist > EXPLOSION_RADIUS) {
                continue;
            }
            float falloff = (float) Math.max(0.35D, 1.0D - (dist / (EXPLOSION_RADIUS * 1.15D)));
            if (victim != directTarget) {
                victim.damage(
                        serverWorld,
                        this.getDamageSources().thrown(this, owner != null ? owner : this),
                        this.maxSplashDamage * falloff
                );
            }

            Vec3d diff = victim.getPos().subtract(center);
            Vec3d horiz = new Vec3d(diff.x, 0.0D, diff.z);
            if (horiz.lengthSquared() < 0.001D) {
                horiz = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                horiz = horiz.normalize();
            }
            double pushStrength = 1.05D * falloff;
            victim.addVelocity(horiz.x * pushStrength, 0.38D * falloff, horiz.z * pushStrength);
            victim.velocityModified = true;
            if (victim instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
        }
    }
}
