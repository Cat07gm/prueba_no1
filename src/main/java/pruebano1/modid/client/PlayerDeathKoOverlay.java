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
 * Video/GIF a pantalla completa de muerte del jugador ("TERRY PAPEADO") con transición difuminada
 * de entrada (fade-in) y salida (fade-out), acompañado del audio para todos los jugadores.
 */
public class PlayerDeathKoOverlay {

    private static final int FRAME_COUNT = 80;
    private static final Identifier[] FRAMES = new Identifier[FRAME_COUNT];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            FRAMES[i] = Identifier.of(
                    Prueba_no1.MOD_ID,
                    String.format("textures/gui/ko_frames/frame_%03d.png", i)
            );
        }
    }

    // Duración total: 110 ticks (5.5 segundos, sincronizado con el audio de Terry Papeado)
    private static final int TOTAL_DURATION_TICKS = 110;
    private static final float FADE_IN_TICKS = 16.0F;  // 0.8s de transición difuminada al aparecer
    private static final float FADE_OUT_TICKS = 22.0F; // 1.1s de transición difuminada al desaparecer

    private static int ticksRemaining = 0;

    public static void trigger(String deadPlayerName) {
        ticksRemaining = TOTAL_DURATION_TICKS;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(ModSounds.PLAYER_DEATH_KO, 1.0F, 1.0F));
        }
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
        float progress = MathHelper.clamp(elapsed / (float) TOTAL_DURATION_TICKS, 0.0F, 1.0F);

        // Seleccionar el fotograma actual del video (0 a 79)
        int frameIndex = MathHelper.clamp((int) (progress * FRAME_COUNT), 0, FRAME_COUNT - 1);

        // Calcular opacidad (alpha) para la transición difuminada al aparecer y al desaparecer
        float alpha = 1.0F;
        if (elapsed < FADE_IN_TICKS) {
            alpha = MathHelper.clamp(elapsed / FADE_IN_TICKS, 0.0F, 1.0F);
        } else if ((TOTAL_DURATION_TICKS - elapsed) < FADE_OUT_TICKS) {
            alpha = MathHelper.clamp((TOTAL_DURATION_TICKS - elapsed) / FADE_OUT_TICKS, 0.0F, 1.0F);
        }

        int alphaByte = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
        int color = (alphaByte << 24) | 0x00FFFFFF;

        // Dibujar el fotograma del video cubriendo toda la pantalla con la opacidad difuminada
        context.drawTexture(
                RenderLayer::getGuiTextured,
                FRAMES[frameIndex],
                0, 0,
                0.0F, 0.0F,
                screenW, screenH,
                screenW, screenH,
                color
        );
    }
}
