package pruebano1.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * TonyFase3 (Sudadera Negra): Personal de seguridad / guardias fijos del casino "Draftea Tony Diaz".
 * Dos custodian la entrada principal y cuatro se distribuyen en el interior manteniendo posturas de guardia sin moverse.
 */
public class TonyFase3Entity extends TonyCasinoNpcEntity {

    public static final int POSE_CROSSED = 0;
    public static final int POSE_ALERT = 1;
    public static final int POSE_HANDS_CLASPED = 2;

    private static final TrackedData<Integer> GUARD_POSE = DataTracker.registerData(
            TonyFase3Entity.class, TrackedDataHandlerRegistry.INTEGER
    );

    private static final RawAnimation GUARD_CROSSED_ANIM = RawAnimation.begin().thenLoop("guard_crossed");
    private static final RawAnimation GUARD_ALERT_ANIM = RawAnimation.begin().thenLoop("guard_alert");
    private static final RawAnimation GUARD_CLASPED_ANIM = RawAnimation.begin().thenLoop("guard_hands_clasped");

    public TonyFase3Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createTonyFase3Attributes() {
        return createTonyNpcAttributes(true);
    }

    @Override
    protected boolean isStationaryNpc() {
        return true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(GUARD_POSE, POSE_CROSSED);
    }

    @Override
    protected void initGoals() {
        // Guardias decorativos fijos en entrada e interior del casino
    }

    public int getGuardPose() {
        return this.dataTracker.get(GUARD_POSE);
    }

    public void setGuardPose(int pose) {
        int clamped = (pose < POSE_CROSSED || pose > POSE_HANDS_CLASPED) ? POSE_CROSSED : pose;
        this.dataTracker.set(GUARD_POSE, clamped);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("GuardPose", this.getGuardPose());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setGuardPose(nbt.getInt("GuardPose"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            int pose = this.getGuardPose();
            if (pose == POSE_ALERT) {
                state.setAndContinue(GUARD_ALERT_ANIM);
            } else if (pose == POSE_HANDS_CLASPED) {
                state.setAndContinue(GUARD_CLASPED_ANIM);
            } else {
                state.setAndContinue(GUARD_CROSSED_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }
}
