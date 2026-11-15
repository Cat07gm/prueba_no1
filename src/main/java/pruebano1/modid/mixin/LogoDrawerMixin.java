package pruebano1.modid.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoDrawer.class)
public abstract class LogoDrawerMixin {

    @Unique
    private static final Identifier LONTERRY_TITLE_TEXTURE =
            Identifier.of("prueba_no1", "textures/gui/title/lon_terry_title.png");

    @Unique
    private static final int LONTERRY_WIDTH = 116;

    @Unique
    private static final int LONTERRY_HEIGHT = 22;

    @Shadow
    @Final
    private boolean ignoreAlpha;

    @Inject(method = "draw(Lnet/minecraft/client/gui/DrawContext;IFI)V", at = @At("TAIL"))
    private void drawLonTerrySubtitleAboveLogo(DrawContext context, int screenWidth, float alpha, int y, CallbackInfo ci) {
        float effectiveAlpha = this.ignoreAlpha ? 1.0F : alpha;
        int color = ColorHelper.getWhite(effectiveAlpha);
        int drawX = screenWidth / 2 - LONTERRY_WIDTH / 2;
        int drawY = y - 19;

        context.drawTexture(
                RenderLayer::getGuiTextured,
                LONTERRY_TITLE_TEXTURE,
                drawX,
                drawY,
                0.0F,
                0.0F,
                LONTERRY_WIDTH,
                LONTERRY_HEIGHT,
                LONTERRY_WIDTH,
                LONTERRY_HEIGHT,
                color
        );
    }
}
