package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.world.DrafteaCasinoManager;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base común para los NPC pasivos temáticos de Tony en el casino "Draftea Tony Diaz".
 * Gestiona persistencia, invulnerabilidad en zona segura/NPCs estáticos y anclaje de posición.
 */
public abstract class TonyCasinoNpcEntity extends PathAwareEntity implements GeoEntity {

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private Vec3d homePos;
    private float homeYaw;
    private boolean casinoBound;

    protected TonyCasinoNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 0;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createTonyNpcAttributes(boolean stationary) {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, stationary ? 0.0D : 0.23D)
                .add(EntityAttributes.FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, stationary ? 1.0D : 0.3D);
    }

    protected abstract boolean isStationaryNpc();

    public void anchorAt(Vec3d pos, float yaw, boolean bindToCasino) {
        this.homePos = pos;
        this.homeYaw = yaw;
        this.casinoBound = bindToCasino;
        this.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0F);
        this.setBodyYaw(yaw);
        this.prevBodyYaw = yaw;
        this.setHeadYaw(yaw);
        this.prevHeadYaw = yaw;
        this.setPersistent();
    }

    public void ensureDefaultHomeAnchored() {
        if (this.homePos == null) {
            this.homePos = this.getPos();
            this.homeYaw = this.getYaw();
        }
    }

    @Nullable
    public Vec3d getHomePos() {
        return this.homePos;
    }

    public float getHomeYaw() {
        return this.homeYaw;
    }

    public boolean isCasinoBound() {
        return this.casinoBound;
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        if (this.isStationaryNpc()) {
            this.ensureDefaultHomeAnchored();
            this.pinToHome();
        }
    }

    protected void pinToHome() {
        if (this.homePos == null) {
            return;
        }
        this.getNavigation().stop();
        Vec3d vel = this.getVelocity();
        if (vel.x != 0.0 || vel.z != 0.0 || vel.y > 0.0) {
            this.setVelocity(0.0, Math.min(0.0, vel.y), 0.0);
        }
        double dx = this.getX() - this.homePos.x;
        double dz = this.getZ() - this.homePos.z;
        double dy = Math.abs(this.getY() - this.homePos.y);
        if ((dx * dx + dz * dz) > 0.0025 || dy > 1.25) {
            this.setPosition(this.homePos.x, this.homePos.y, this.homePos.z);
        }
        this.setYaw(this.homeYaw);
        this.prevYaw = this.homeYaw;
        this.setBodyYaw(this.homeYaw);
        this.prevBodyYaw = this.homeYaw;
        this.setHeadYaw(this.homeYaw);
        this.prevHeadYaw = this.homeYaw;
        this.setPitch(0.0F);
        this.prevPitch = 0.0F;
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.isStationaryNpc()) {
            super.travel(Vec3d.ZERO);
            return;
        }
        super.travel(movementInput);
    }

    @Override
    public boolean isPushable() {
        return !this.isStationaryNpc() && super.isPushable();
    }

    @Override
    protected void pushAway(Entity entity) {
        if (this.isStationaryNpc()) {
            return;
        }
        super.pushAway(entity);
    }

    @Override
    public void pushAwayFrom(Entity entity) {
        if (this.isStationaryNpc()) {
            return;
        }
        super.pushAwayFrom(entity);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
            return super.damage(world, source, amount);
        }
        if (source.getAttacker() instanceof PlayerEntity player && player.isCreative()) {
            return super.damage(world, source, amount);
        }
        BlockPos pos = this.getBlockPos();
        if (this.isStationaryNpc() || this.casinoBound || DrafteaCasinoManager.isInSafeZone(world, pos)) {
            return false;
        }
        return super.damage(world, source, amount);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Nullable
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
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("CasinoBound", this.casinoBound);
        nbt.putFloat("HomeYaw", this.homeYaw);
        if (this.homePos != null) {
            nbt.putBoolean("HasHomePos", true);
            nbt.putDouble("HomeX", this.homePos.x);
            nbt.putDouble("HomeY", this.homePos.y);
            nbt.putDouble("HomeZ", this.homePos.z);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.casinoBound = nbt.getBoolean("CasinoBound");
        this.homeYaw = nbt.getFloat("HomeYaw");
        if (nbt.getBoolean("HasHomePos")) {
            this.homePos = new Vec3d(
                    nbt.getDouble("HomeX"),
                    nbt.getDouble("HomeY"),
                    nbt.getDouble("HomeZ")
            );
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
