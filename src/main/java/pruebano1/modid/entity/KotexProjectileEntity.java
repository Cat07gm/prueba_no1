package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModItem;

import java.util.List;

/**
 * Entidad proyectil "Toalla Kotex" (como flecha):
 * - Usa la textura del ítem Toalla Kotex.
 * - Hace 4 corazones de daño directo (8.0F base, o 9.6F en Modo Diablo de Jefri).
 * - Se queda clavada en el suelo unos segundos como las flechas y permite recogerla si está habilitado.
 */
public class KotexProjectileEntity extends PersistentProjectileEntity implements FlyingItemEntity {

    private static final TrackedData<Boolean> RELEASED = DataTracker.registerData(
            KotexProjectileEntity.class,
            TrackedDataHandlerRegistry.BOOLEAN
    );

    private int hoverTicks = 0;
    private float directHitDamage = 8.0F; // 4 corazones base
    private boolean fromBossSkyBarrage = false;
    private int groundLifeTicks = 0;

    public KotexProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.dataTracker.set(RELEASED, true);
    }

    public KotexProjectileEntity(World world, double x, double y, double z, ItemStack stack, @Nullable ItemStack shotFrom) {
        super(ModEntities.KOTEX_PROJECTILE, x, y, z, world, stack, shotFrom);
        this.directHitDamage = 8.0F;
        this.dataTracker.set(RELEASED, true);
    }

    public KotexProjectileEntity(World world, LivingEntity owner, ItemStack stack, @Nullable ItemStack shotFrom) {
        super(ModEntities.KOTEX_PROJECTILE, owner, world, stack, shotFrom);
        this.directHitDamage = 8.0F;
        this.dataTracker.set(RELEASED, true);
    }

    /**
     * Constructor usado por Jefri penstein para su ráfaga en el cielo (15 bloques por cada lado).
     */
    public KotexProjectileEntity(World world, LivingEntity owner, double x, double y, double z, int hoverTicks, float damageAmount, boolean allowPlayerPickup) {
        super(ModEntities.KOTEX_PROJECTILE, x, y, z, world, new ItemStack(ModItem.TOALLA_KOTEX), null);
        this.setOwner(owner);
        this.hoverTicks = hoverTicks;
        this.directHitDamage = damageAmount;
        this.fromBossSkyBarrage = true;
        this.pickupType = allowPlayerPickup ? PickupPermission.ALLOWED : PickupPermission.DISALLOWED;
        this.dataTracker.set(RELEASED, false);
        this.setNoGravity(true);
        this.setVelocity(0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(RELEASED, true);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItem.TOALLA_KOTEX);
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(ModItem.TOALLA_KOTEX);
    }

    @Override
    protected double getGravity() {
        return this.dataTracker.get(RELEASED) ? 0.05D : 0.0D;
    }

    @Override
    public void tick() {
        if (!this.getWorld().isClient()) {
            if (this.hoverTicks > 0) {
                this.hoverTicks--;
                this.setVelocity(0.0D, 0.0D, 0.0D);
                if (this.hoverTicks == 0) {
                    this.dataTracker.set(RELEASED, true);
                    this.setNoGravity(false);
                    this.setVelocity(0.0D, -1.35D, 0.0D);
                    this.velocityModified = true;
                    this.getWorld().playSound(
                            null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 0.5F, 1.15F
                    );
                }
                return;
            }
        } else if (!this.dataTracker.get(RELEASED)) {
            this.setVelocity(0.0D, 0.0D, 0.0D);
            return;
        }

        super.tick();

        if (!this.getWorld().isClient()) {
            if (this.isInGround()) {
                this.groundLifeTicks++;
                // Se quedan clavadas en el suelo unos segundos (6s si son de la ráfaga de Jefri, 20s si las disparó el jugador)
                int maxGroundTicks = this.fromBossSkyBarrage ? 120 : 400;
                if (this.groundLifeTicks >= maxGroundTicks) {
                    this.discard();
                }
            } else if (this.age > 200) {
                this.discard();
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (hitEntity == owner || (owner instanceof JefriPensteinEntity && hitEntity instanceof JefriPensteinEntity)) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            hitEntity.damage(serverWorld, this.getDamageSources().arrow(this, owner != null ? owner : this), this.directHitDamage);
            serverWorld.playSound(
                    null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                    SoundEvents.ENTITY_ARROW_HIT, SoundCategory.HOSTILE, 1.0F, 1.1F
            );
            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    hitEntity.getX(), hitEntity.getBodyY(0.5D), hitEntity.getZ(),
                    5, 0.2D, 0.2D, 0.2D, 0.05D
            );
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.fromBossSkyBarrage && this.getWorld() instanceof ServerWorld serverWorld && this.groundLifeTicks == 0) {
            // Al clavarse en el suelo durante el ataque de Jefri, daña a jugadores muy cercanos al impacto (1.25 bloques)
            Entity owner = this.getOwner();
            List<LivingEntity> nearby = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    new Box(this.getBlockPos()).expand(1.25D),
                    e -> e != owner && !(e instanceof JefriPensteinEntity) && e.isAlive()
            );
            for (LivingEntity target : nearby) {
                target.damage(serverWorld, this.getDamageSources().arrow(this, owner != null ? owner : this), this.directHitDamage);
            }
        }
    }
}
