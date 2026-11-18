package pruebano1.modid.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pruebano1.modid.client.CharlieKirkCinematicController;

/**
 * Permite mover la cámara libremente por los planos cinematográficos de La Casa Blanca
 * durante los primeros 43 segundos de la cinemática de entrada de Charlie Kirk.
 */
@Mixin(Camera.class)
public abstract class CharlieCinematicCameraMixin {

    @Shadow
    private boolean thirdPerson;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Inject(method = "update", at = @At("TAIL"))
    private void applyCharlieCinematicCamera(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPerson,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        if (CharlieKirkCinematicController.isCameraOverrideActive()) {
            this.thirdPerson = true;
            CharlieKirkCinematicController.CameraPose pose = CharlieKirkCinematicController.computeCameraPose(tickDelta);
            if (pose != null) {
                this.setPos(pose.x(), pose.y(), pose.z());
                this.setRotation(pose.yaw(), pose.pitch());
            }
        }
    }
}
