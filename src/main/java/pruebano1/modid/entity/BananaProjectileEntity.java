package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import pruebano1.modid.ModSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Proyectil 3D: Plátano Bumerán lanzado por Fer Ovilla.
 * - Vuela girando rápidamente en el aire hacia el jugador.
 * - Al impactar contra el jugador, quita 5 corazones (10.0F) y reproduce el efecto de sonido personalizado.
 */
public class BananaProjectileEntity extends ThrownEntity implements GeoEntity {

    private static final RawAnimation SPIN_ANIM = RawAnimation.begin().thenLoop("spin");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public BananaProjectileEntity(EntityType<? extends ThrownEntity> entityType, World world) {
        super(entityType, world);
    }

    public BananaProjectileEntity(World world, LivingEntity owner) {
        super(ModEntities.BANANA_PROJECTILE, owner.getX(), owner.getEyeY() - 0.15D, owner.getZ(), world);
        this.setOwner(owner);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected double getGravity() {
        // Poca caída para que vuele casi recto como un bumerán
        return 0.005D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > 100) {
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity hitEntity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (hitEntity == owner) {
            return;
        }

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // Quitar 5 corazones de daño (10.0F)
            hitEntity.damage(serverWorld, this.getDamageSources().thrown(this, owner), 10.0F);

            // Reproducir el efecto de sonido personalizado al impactar
            serverWorld.playSound(
                    null,
                    hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                    ModSounds.FER_OVILLA_BANANA_HIT,
                    SoundCategory.HOSTILE,
                    1.5F,
                    1.0F
            );

            serverWorld.spawnParticles(
                    ParticleTypes.CRIT,
                    hitEntity.getX(), hitEntity.getBodyY(0.5D), hitEntity.getZ(),
                    10, 0.25D, 0.25D, 0.25D, 0.1D
            );
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> state.setAndContinue(SPIN_ANIM)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
