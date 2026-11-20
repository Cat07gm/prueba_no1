package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;
import pruebano1.modid.ModSounds;

/**
 * Proyectil de bala disparado por la Seguridad de la Casa Blanca (Pistola y Rifle de Asalto).
 */
public class GuardBulletProjectileEntity extends ThrownItemEntity implements FlyingItemEntity {

    /** Daño normal de pistola (ajustable luego si se aclara otro valor). */
    public static final float PISTOL_BULLET_DAMAGE = 9.0F;
    /** Daño por bala en ráfaga de 4 disparos del rifle de asalto (menor que la pistola). */
    public static final float RIFLE_BULLET_DAMAGE = 5.0F;

    private float hitDamage = PISTOL_BULLET_DAMAGE;

    public GuardBulletProjectileEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public GuardBulletProjectileEntity(World world, LivingEntity owner, float hitDamage) {
        super(ModEntities.GUARD_BULLET_PROJECTILE, owner, world, new ItemStack(ModItem.GUARD_BULLET));
        this.hitDamage = hitDamage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItem.GUARD_BULLET;
    }

    @Override
    protected double getGravity() {
        return 0.002D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            if (this.age % 2 == 0) {
                serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX(), this.getY(), this.getZ(),
                        1, 0.02D, 0.02D, 0.02D, 0.005D
                );
            }
            if (this.age > 80) {
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
                    this.hitDamage
            );
            serverWorld.playSound(
                    null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                    ModSounds.GUARD_BULLET_HIT, SoundCategory.HOSTILE,
                    0.55F, 0.95F + this.random.nextFloat() * 0.12F
            );
            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    hitEntity.getX(), hitEntity.getBodyY(0.5D), hitEntity.getZ(),
                    6, 0.15D, 0.15D, 0.15D, 0.08D
            );
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d hitPos = blockHitResult.getPos();
            serverWorld.spawnParticles(
                    ParticleTypes.SMOKE,
                    hitPos.x, hitPos.y, hitPos.z,
                    4, 0.08D, 0.08D, 0.08D, 0.02D
            );
            this.discard();
        }
    }
}
