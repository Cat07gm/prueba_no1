package pruebano1.modid.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Unique
    private static final Identifier LONTERRY_LOADING_SCREEN =
            Identifier.of("prueba_no1", "textures/gui/title/loading_screen.png");

    @Unique
    private static final String CLASSPATH_TEXTURE_PATH =
            "/assets/prueba_no1/textures/gui/title/loading_screen.png";

    @Unique
    private static final int IMAGE_WIDTH = 1376;

    @Unique
    private static final int IMAGE_HEIGHT = 768;

    @Unique
    private static final int BACKGROUND_BLACK = 0xFF000000;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private ResourceReload reload;

    @Shadow
    @Final
    private Consumer<Optional<Throwable>> exceptionHandler;

    @Shadow
    @Final
    private boolean reloading;

    @Shadow
    private float progress;

    @Shadow
    private long reloadCompleteTime;

    @Shadow
    private long reloadStartTime;

    @Shadow
    private static int withAlpha(int color, int alpha) {
        throw new AssertionError();
    }

    @Shadow
    private void renderProgressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity) {
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private static void registerLonTerryLoadingTexture(TextureManager textureManager, CallbackInfo ci) {
        textureManager.registerTexture(LONTERRY_LOADING_SCREEN, new ReloadableTexture(LONTERRY_LOADING_SCREEN) {
            @Override
            public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
                Optional<InputStream> resourceStream = resourceManager.getResource(LONTERRY_LOADING_SCREEN)
                        .map(resource -> {
                            try {
                                return resource.getInputStream();
                            } catch (IOException e) {
                                return null;
                            }
                        });

                try (InputStream stream = resourceStream.orElseGet(
                        () -> SplashOverlayMixin.class.getResourceAsStream(CLASSPATH_TEXTURE_PATH))) {
                    if (stream == null) {
                        throw new IOException("Missing loading screen texture: " + CLASSPATH_TEXTURE_PATH);
                    }
                    return new TextureContents(
                            NativeImage.read(stream),
                            new TextureResourceMetadata(true, true)
                    );
                }
            }
        });
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderLonTerryLoadingScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int scaledWidth = context.getScaledWindowWidth();
        int scaledHeight = context.getScaledWindowHeight();
        long now = Util.getMeasuringTimeMs();

        if (this.reloading && this.reloadStartTime == -1L) {
            this.reloadStartTime = now;
        }

        float fadeOutProgress = this.reloadCompleteTime > -1L
                ? (float) (now - this.reloadCompleteTime) / 1000.0F
                : -1.0F;
        float fadeInProgress = this.reloadStartTime > -1L
                ? (float) (now - this.reloadStartTime) / 500.0F
                : -1.0F;

        float imageAlpha;
        if (fadeOutProgress >= 1.0F) {
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(context, 0, 0, delta);
            }
            int bgAlpha = MathHelper.ceil((1.0F - MathHelper.clamp(fadeOutProgress - 1.0F, 0.0F, 1.0F)) * 255.0F);
            context.fill(RenderLayer.getGuiOverlay(), 0, 0, scaledWidth, scaledHeight, withAlpha(BACKGROUND_BLACK, bgAlpha));
            imageAlpha = 1.0F - MathHelper.clamp(fadeOutProgress - 1.0F, 0.0F, 1.0F);
        } else if (this.reloading) {
            if (this.client.currentScreen != null && fadeInProgress < 1.0F) {
                this.client.currentScreen.render(context, mouseX, mouseY, delta);
            }
            int bgAlpha = MathHelper.ceil(MathHelper.clamp((double) fadeInProgress, 0.15, 1.0) * 255.0);
            context.fill(RenderLayer.getGuiOverlay(), 0, 0, scaledWidth, scaledHeight, withAlpha(BACKGROUND_BLACK, bgAlpha));
            imageAlpha = MathHelper.clamp(fadeInProgress, 0.0F, 1.0F);
        } else {
            GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager._clear(16384);
            imageAlpha = 1.0F;
        }

        double scale = Math.max((double) scaledWidth / IMAGE_WIDTH, (double) scaledHeight / IMAGE_HEIGHT);
        int drawWidth = Math.max(1, (int) Math.round(IMAGE_WIDTH * scale));
        int drawHeight = Math.max(1, (int) Math.round(IMAGE_HEIGHT * scale));
        int drawX = (scaledWidth - drawWidth) / 2;
        int drawY = (scaledHeight - drawHeight) / 2;

        context.drawTexture(
                RenderLayer::getGuiTextured,
                LONTERRY_LOADING_SCREEN,
                drawX,
                drawY,
                0.0F,
                0.0F,
                drawWidth,
                drawHeight,
                drawWidth,
                drawHeight,
                ColorHelper.getWhite(imageAlpha)
        );

        int barHalfWidth = (int) (Math.min((double) scaledWidth * 0.75, scaledHeight) * 0.25 * 4.0 * 0.5);
        int barY = (int) ((double) scaledHeight * 0.8325);
        float reloadProgress = this.reload.getProgress();
        this.progress = MathHelper.clamp(this.progress * 0.95F + reloadProgress * 0.050000012F, 0.0F, 1.0F);

        if (fadeOutProgress < 1.0F) {
            this.renderProgressBar(
                    context,
                    scaledWidth / 2 - barHalfWidth,
                    barY - 5,
                    scaledWidth / 2 + barHalfWidth,
                    barY + 5,
                    1.0F - MathHelper.clamp(fadeOutProgress, 0.0F, 1.0F)
            );
        }

        if (fadeOutProgress >= 2.0F) {
            this.client.setOverlay(null);
        }

        if (this.reloadCompleteTime == -1L
                && this.reload.isComplete()
                && (!this.reloading || fadeInProgress >= 2.0F)) {
            try {
                this.reload.throwException();
                this.exceptionHandler.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.exceptionHandler.accept(Optional.of(throwable));
            }

            this.reloadCompleteTime = Util.getMeasuringTimeMs();
            if (this.client.currentScreen != null) {
                this.client.currentScreen.init(this.client, scaledWidth, scaledHeight);
            }
        }

        ci.cancel();
    }
}
