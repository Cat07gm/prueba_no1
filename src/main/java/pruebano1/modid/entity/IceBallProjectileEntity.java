package pruebano1.modid.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
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
 * Nuevo proyectil "Bola de Hielo" (usado en "La Bola de Hielo Ráfaga", en "Una Bola de Hielo" gigante
 * y también arrojadizo por el jugador):
 * - Parecido a una bola de nieve pero con textura de hielo cristalino y sonidos de hielo al impactar.
 * - Modo Ráfaga: causa un daño un poco más fuerte que la Toalla Kotex (11.0F = 5.5 corazones en Fase 1,
 *   13.0F = 6.5 corazones en Fase 2; la Toalla Kotex hace 8.0F / 9.6F) y aplica Lentitud/Congelación.
 * - Modo Gigante ("Una Bola de Hielo"): proyectil masivo cargado durante el audio del Sol/Bola de Hielo
 *   que al impactar desata una gran explosión de hielo, causando muchísimo daño (28.0F / 32.0F) y empujando
 *   fuertemente al jugador y a todo lo que esté alrededor.
 */
public class IceBallProjectileEntity extends ThrownItemEntity implements FlyingItemEntity {

    private static final TrackedData<Boolean> GIANT = DataTracker.registerData(
            IceBallProjectileEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final BlockStateParticleEffect PACKED_ICE_PARTICLE =
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState());
    private static final BlockStateParticleEffect BLUE_ICE_PARTICLE =
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState());
    private static final DustParticleEffect ICE_GLOW_DUST = new DustParticleEffect(0x66D9FF, 1.5F);

    private float hitDamage = 11.0F; // Un poco más fuerte que la Toalla Kotex (8.0F)

    public IceBallProjectileEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public IceBallProjectileEntity(World world, LivingEntity owner, ItemStack stack, float hitDamage, boolean giant) {
        super(giant ? ModEntities.GIANT_ICE_BALL_PROJECTILE : ModEntities.ICE_BALL_PROJECTILE, owner, world, stack);
        this.hitDamage = hitDamage;
        this.dataTracker.set(GIANT, giant);
        if (giant) {
            this.setNoGravity(true);
        }
    }

    public IceBallProjectileEntity(World world, double x, double y, double z, ItemStack stack) {
        super(ModEntities.ICE_BALL_PROJECTILE, x, y, z, world, stack);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(GIANT, false);
    }

    public boolean isGiant() {
        return this.dataTracker.get(GIANT);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItem.ICE_BALL;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (this.isGiant()) {
                // Estela intensa de la Gran Bola de Hielo ("Una Bola de Hielo")
                serverWorld.spawnParticles(
                        ParticleTypes.SNOWFLAKE,
                        this.getX(), this.getY() + 0.4D, this.getZ(),
                        8, 0.55D, 0.55D, 0.55D, 0.04D
                );
                serverWorld.spawnParticles(
                        BLUE_ICE_PARTICLE,
                        this.getX(), this.getY() + 0.4D, this.getZ(),
                        6, 0.45D, 0.45D, 0.45D, 0.08D
                );
                serverWorld.spawnParticles(
                        ICE_GLOW_DUST,
                        this.getX(), this.getY() + 0.4D, this.getZ(),
                        5, 0.4D, 0.4D, 0.4D, 0.02D
                );
            } else if (this.age % 2 == 0) {
                // Estela ligera de escarcha en la ráfaga de bolas de hielo
                serverWorld.spawnParticles(
                        ParticleTypes.SNOWFLAKE,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.08D, 0.08D, 0.08D, 0.01D
                );
            }

            if (this.age > 160) {
                this.discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (hitEntity == owner || (owner instanceof JoseTorresEntity && hitEntity instanceof JoseTorresEntity)) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (this.isGiant()) {
                detonateGiantIceBall(serverWorld, hitEntity.getPos());
            } else {
                applyNormalIceBallImpact(serverWorld, hitEntity);
            }
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d hitPos = blockHitResult.getPos();
            if (this.isGiant()) {
                detonateGiantIceBall(serverWorld, hitPos);
            } else {
                // Sonidos y partículas de hielo al chocar contra un bloque
                serverWorld.playSound(
                        null, hitPos.x, hitPos.y, hitPos.z,
                        SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.95F, 1.25F
                );
                serverWorld.spawnParticles(
                        PACKED_ICE_PARTICLE,
                        hitPos.x, hitPos.y + 0.1D, hitPos.z,
                        10, 0.2D, 0.2D, 0.2D, 0.08D
                );
                serverWorld.spawnParticles(
                        ParticleTypes.SNOWFLAKE,
                        hitPos.x, hitPos.y + 0.1D, hitPos.z,
                        6, 0.2D, 0.2D, 0.2D, 0.03D
                );
            }
            this.discard();
        }
    }

    /**
     * Impacto de una bola de hielo normal (de "La Bola de Hielo Ráfaga" o lanzada por el jugador):
     * - Daño un poco más fuerte que la Toalla Kotex (11.0F / 13.0F frente a 8.0F / 9.6F).
     * - Sonidos de hielo rompiéndose y congelación + Lentitud.
     */
    private void applyNormalIceBallImpact(ServerWorld serverWorld, Entity hitEntity) {
        Entity owner = this.getOwner();
        hitEntity.damage(
                serverWorld,
                this.getDamageSources().thrown(this, owner != null ? owner : this),
                this.hitDamage
        );

        if (hitEntity instanceof LivingEntity living) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 4, 1), owner);
            living.setFrozenTicks(Math.min(living.getMinFreezeDamageTicks() + 100, living.getFrozenTicks() + 120));
        }

        serverWorld.playSound(
                null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.25F, 1.15F
        );
        serverWorld.playSound(
                null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 1.2F, 1.0F
        );
        serverWorld.spawnParticles(
                PACKED_ICE_PARTICLE,
                hitEntity.getX(), hitEntity.getBodyY(0.5D), hitEntity.getZ(),
                16, 0.3D, 0.35D, 0.3D, 0.1D
        );
        serverWorld.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                hitEntity.getX(), hitEntity.getBodyY(0.5D), hitEntity.getZ(),
                12, 0.35D, 0.35D, 0.35D, 0.05D
        );
    }

    /**
     * Explosión masiva del 4.º ataque especial "Una Bola de Hielo":
     * - Causa muchísimo daño al jugador más cercano y a las entidades alrededor (radio de 7.5 bloques).
     * - Empuja muchísimo a todo lo que está alrededor y aplica congelación intensa + efectos especiales de hielo.
     */
    private void detonateGiantIceBall(ServerWorld serverWorld, Vec3d center) {
        Entity owner = this.getOwner();

        serverWorld.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.5F, 0.8F
        );
        serverWorld.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.5F, 0.65F
        );
        serverWorld.playSound(
                null, center.x, center.y, center.z,
                SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 2.2F, 0.85F
        );

        serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 0.5D, center.z,
                2, 0.4D, 0.4D, 0.4D, 0.0D
        );
        serverWorld.spawnParticles(
                BLUE_ICE_PARTICLE,
                center.x, center.y + 0.8D, center.z,
                85, 2.2D, 1.4D, 2.2D, 0.25D
        );
        serverWorld.spawnParticles(
                PACKED_ICE_PARTICLE,
                center.x, center.y + 0.8D, center.z,
                65, 2.0D, 1.2D, 2.0D, 0.2D
        );
        serverWorld.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                center.x, center.y + 1.0D, center.z,
                90, 2.5D, 1.5D, 2.5D, 0.14D
        );
        serverWorld.spawnParticles(
                ICE_GLOW_DUST,
                center.x, center.y + 1.0D, center.z,
                50, 2.2D, 1.2D, 2.2D, 0.05D
        );

        double radius = 7.5D;
        Box area = new Box(
                center.x - radius, center.y - 4.0D, center.z - radius,
                center.x + radius, center.y + 6.0D, center.z + radius
        );
        List<LivingEntity> victims = serverWorld.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != owner && !(e instanceof JoseTorresEntity) && e.isAlive() && !e.isSpectator()
        );

        for (LivingEntity victim : victims) {
            double dist = Math.sqrt(victim.squaredDistanceTo(center));
            if (dist > radius) {
                continue;
            }
            // Daño masivo directo y por proximidad (hasta 28-32 puntos = 14-16 corazones)
            float falloff = (float) Math.max(0.55D, 1.0D - (dist / (radius * 1.4D)));
            float finalDamage = this.hitDamage * falloff;

            victim.damage(
                    serverWorld,
                    this.getDamageSources().thrown(this, owner != null ? owner : this),
                    finalDamage
            );
            victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 6, 2), owner);
            victim.setFrozenTicks(victim.getMinFreezeDamageTicks() + 180);

            // Gran empuje radial a todo lo que está alrededor
            Vec3d diff = victim.getPos().subtract(center);
            Vec3d horiz = new Vec3d(diff.x, 0.0D, diff.z);
            if (horiz.lengthSquared() < 0.001D) {
                horiz = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                horiz = horiz.normalize();
            }
            double pushStrength = 2.45D * falloff;
            victim.setVelocity(horiz.x * pushStrength, 0.82D, horiz.z * pushStrength);
            victim.velocityModified = true;
            if (victim instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
        }
    }
}
