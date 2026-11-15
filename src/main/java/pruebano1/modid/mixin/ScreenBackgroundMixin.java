package pruebano1.modid.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Shadow
    protected MinecraftClient client;

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void disableMenuBlurOnMainMenuSubscreens(CallbackInfo ci) {
        if (this.client != null && this.client.world == null) {
            ci.cancel();
        }
    }
}
