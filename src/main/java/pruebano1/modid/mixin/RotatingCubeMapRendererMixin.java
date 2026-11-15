package pruebano1.modid.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public abstract class RotatingCubeMapRendererMixin {

    @Unique
    private static final Identifier LONTERRY_MENU_BACKGROUND =
            Identifier.of("prueba_no1", "textures/gui/title/menu_background.png");

    @Unique
    private static final int IMAGE_WIDTH = 1376;

    @Unique
    private static final int IMAGE_HEIGHT = 768;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderLonTerryMenuBackground(DrawContext context, int width, int height, float alpha, float tickDelta, CallbackInfo ci) {
        double scale = Math.max((double) width / IMAGE_WIDTH, (double) height / IMAGE_HEIGHT);
        int drawWidth = Math.max(1, (int) Math.round(IMAGE_WIDTH * scale));
        int drawHeight = Math.max(1, (int) Math.round(IMAGE_HEIGHT * scale));
        int drawX = (width - drawWidth) / 2;
        int drawY = (height - drawHeight) / 2;

        context.drawTexture(
                RenderLayer::getGuiTextured,
                LONTERRY_MENU_BACKGROUND,
                drawX,
                drawY,
                0.0F,
                0.0F,
                drawWidth,
                drawHeight,
                drawWidth,
                drawHeight,
                ColorHelper.getWhite(alpha)
        );
        context.draw();
        ci.cancel();
    }
}
