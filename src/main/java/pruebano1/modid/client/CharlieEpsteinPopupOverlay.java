package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import pruebano1.modid.ModSounds;
import pruebano1.modid.Prueba_no1;

/**
 * Destello rápido en pantalla que avisa al jugador afectado por la telequinesis de Charlie Kirk
 * junto al sonido "Efecto jefri".
 */
public final class CharlieEpsteinPopupOverlay {

    private static final Identifier POPUP_TEXTURE = Identifier.of(
            Prueba_no1.MOD_ID,
            "textures/gui/charlie_epstein_popup.png"
    );
    private static final float ASPECT_RATIO = 414.0F / 512.0F;

    private static final int TOTAL_DURATION_TICKS = 34; // ~1.7 segundos en pantalla
    private static final float FADE_IN_TICKS = 3.0F;
    private static final float FADE_OUT_TICKS = 10.0F;

    private static int ticksRemaining = 0;

    private CharlieEpsteinPopupOverlay() {}

    public static void trigger(boolean phase2) {
        ticksRemaining = TOTAL_DURATION_TICKS;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.EFECTO_JEFRI, 1.0F, 1.0F));
        }
    }

    public static void reset() {
        ticksRemaining = 0;
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static boolean isActive() {
        return ticksRemaining > 0;
    }

    public static void render(DrawContext context, float tickDelta) {
        if (ticksRemaining <= 0) {
            return;
        }

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();

        float elapsed = (TOTAL_DURATION_TICKS - ticksRemaining) + tickDelta;
        float alpha = 1.0F;
        if (elapsed < FADE_IN_TICKS) {
            alpha = MathHelper.clamp(elapsed / FADE_IN_TICKS, 0.0F, 1.0F);
        } else if ((TOTAL_DURATION_TICKS - elapsed) < FADE_OUT_TICKS) {
            alpha = MathHelper.clamp((TOTAL_DURATION_TICKS - elapsed) / FADE_OUT_TICKS, 0.0F, 1.0F);
        }

        int alphaByte = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        if (alphaByte <= 2) {
            return;
        }

        float popProgress = MathHelper.clamp(elapsed / 5.0F, 0.0F, 1.0F);
        float scale = 0.84F + 0.16F * popProgress;

        int maxCardH = (int) (screenH * 0.70F * scale);
        int maxCardW = (int) (screenW * 0.56F * scale);
        int drawH = Math.max(48, Math.min(maxCardH, Math.round(maxCardW / ASPECT_RATIO)));
        int drawW = Math.max(40, Math.round(drawH * ASPECT_RATIO));

        int x = (screenW - drawW) / 2;
        int y = (screenH - drawH) / 2;

        // Sutil viñeta oscura detrás del aviso
        int vignetteAlpha = MathHelper.clamp((int) (alpha * 105.0F), 0, 255);
        context.fill(0, 0, screenW, screenH, vignetteAlpha << 24);

        int imageColor = (alphaByte << 24) | 0x00FFFFFF;
        context.drawTexture(
                RenderLayer::getGuiTextured,
                POPUP_TEXTURE,
                x, y,
                0.0F, 0.0F,
                drawW, drawH,
                drawW, drawH,
                imageColor
        );
    }
}
