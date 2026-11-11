package pruebano1.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pruebano1.modid.client.BananaProjectileRenderer;
import pruebano1.modid.client.DrafteaBlockEntityRenderer;
import pruebano1.modid.client.encyclopedia.EncyclopediaScreen;
import pruebano1.modid.client.FerOvillaRenderer;
import pruebano1.modid.client.HelloKittyParticle;
import pruebano1.modid.client.JefriBossMusicInstance;
import pruebano1.modid.client.JefriPensteinRenderer;
import pruebano1.modid.client.KotexProjectileRenderer;
import pruebano1.modid.client.PiDidiBossMusicInstance;
import pruebano1.modid.client.PiDidiRenderer;
import pruebano1.modid.client.PlayerDeathKoOverlay;
import pruebano1.modid.client.ValentinaPrietaRenderer;
import pruebano1.modid.client.VitorJugoRenderer;
import pruebano1.modid.client.WeedLeafParticle;
import pruebano1.modid.client.YostinBiberRenderer;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;
import pruebano1.modid.network.PlayerDeathKoPayload;

import java.util.List;

public class Prueba_no1Client implements ClientModInitializer {

    private static PiDidiBossMusicInstance activeBossMusic = null;
    private static JefriBossMusicInstance activeJefriMusic = null;
    private static int activeTiktokCinematicTick = 0;
    private static boolean activeDeathCageCinematic = false;
    private static boolean wasInJefriCinematic = false;
    private static boolean savedHudHidden = false;
    private static Perspective savedPerspective = Perspective.FIRST_PERSON;
    private static KeyBinding encyclopediaKey = null;

    @Nullable
    public static KeyBinding getEncyclopediaKey() {
        return encyclopediaKey;
    }

    @Override
    public void onInitializeClient() {
        encyclopediaKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prueba_no1.encyclopedia",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prueba_no1.lon_terry"
        ));

        EntityRendererRegistry.register(ModEntities.PI_DIDI, PiDidiRenderer::new);
        EntityRendererRegistry.register(ModEntities.YOSTIN_BIBER, YostinBiberRenderer::new);
        EntityRendererRegistry.register(ModEntities.VITOR_JUGO, VitorJugoRenderer::new);
        EntityRendererRegistry.register(ModEntities.FER_OVILLA, FerOvillaRenderer::new);
        EntityRendererRegistry.register(ModEntities.BANANA_PROJECTILE, BananaProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.VALENTINA_PRIETA, ValentinaPrietaRenderer::new);
        EntityRendererRegistry.register(ModEntities.JEFRI_PENSTEIN, JefriPensteinRenderer::new);
        EntityRendererRegistry.register(ModEntities.KOTEX_PROJECTILE, KotexProjectileRenderer::new);

        // Pantalla de la Draftea: dibuja los ítems de los rodillos
        BlockEntityRendererFactories.register(ModBlockEntities.DRAFTEA, DrafteaBlockEntityRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.HELLO_KITTY, HelloKittyParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.WEED_LEAF, WeedLeafParticle.Factory::new);

        // Recibir aviso cuando cualquier jugador muere para activar el sonido y el GIF/efecto K.O. en pantalla
        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathKoPayload.ID, (payload, context) -> {
            context.client().execute(() -> PlayerDeathKoOverlay.trigger(payload.deadPlayerName()));
        });

        // Dibujar el efecto K.O. y los destellos/barras de cinemática estilo Edit de TikTok de Jefri penstein
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (PlayerDeathKoOverlay.isActive()) {
                PlayerDeathKoOverlay.render(drawContext, tickCounter.getTickDelta(false));
            }
            if (activeTiktokCinematicTick > 0 || activeDeathCageCinematic) {
                int w = drawContext.getScaledWindowWidth();
                int h = drawContext.getScaledWindowHeight();
                int barHeight = Math.max(16, h / 10);
                // Barras negras cinematográficas arriba y abajo
                drawContext.fill(0, 0, w, barHeight, 0xDD000000);
                drawContext.fill(0, h - barHeight, w, h, 0xDD000000);

                // Flash sutil en los golpes de ritmo de la canción durante la cinemática de entrada
                if (activeTiktokCinematicTick == 70 || (activeTiktokCinematicTick > 70 && activeTiktokCinematicTick % 9 == 0)) {
                    drawContext.fill(0, 0, w, h, 0x35FFFFFF);
                }
            }
        });

        // Dibujar también el efecto K.O. encima de la pantalla de muerte ("¡Has muerto!") del jugador caído
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof DeathScreen) {
                ScreenEvents.afterRender(screen).register((scr, drawContext, mouseX, mouseY, tickDelta) -> {
                    if (PlayerDeathKoOverlay.isActive()) {
                        PlayerDeathKoOverlay.render(drawContext, tickDelta);
                    }
                });
            }
        });

        // Tick de cliente: actualizar efecto K.O., música de jefes y cámara en cinemáticas de Jefri
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerDeathKoOverlay.tick();
            activeTiktokCinematicTick = 0;
            activeDeathCageCinematic = false;

            if (client.world == null || client.player == null || !client.player.isAlive()) {
                restorePlayerViewAfterCinematic(client);
                activeBossMusic = null;
                activeJefriMusic = null;
                return;
            }

            if (encyclopediaKey != null) {
                while (encyclopediaKey.wasPressed()) {
                    if (client.currentScreen == null) {
                        client.setScreen(new EncyclopediaScreen());
                    }
                }
            }

            Box jefriSearchBox = client.player.getBoundingBox().expand(64.0D);
            List<JefriPensteinEntity> nearbyJefris = client.world.getEntitiesByClass(
                    JefriPensteinEntity.class,
                    jefriSearchBox,
                    JefriPensteinEntity::isAlive
            );

            if (!nearbyJefris.isEmpty()) {
                JefriPensteinEntity jefri = nearbyJefris.get(0);

                // 1. Música de Jefri (desde la cinemática de invocación hasta antes de la cinemática final de muerte)
                if (jefri.shouldPlayBossMusic()) {
                    if (activeJefriMusic == null || activeJefriMusic.isDone() || !client.getSoundManager().isPlaying(activeJefriMusic)) {
                        activeJefriMusic = new JefriBossMusicInstance(jefri);
                        client.getSoundManager().play(activeJefriMusic);
                    }
                } else if (activeJefriMusic != null) {
                    client.getSoundManager().stop(activeJefriMusic);
                    activeJefriMusic = null;
                }

                // 2. Si está en cualquiera de las dos cinemáticas: ocultar el HUD, forzar 1ra persona y enfocar la cámara
                if (jefri.isInSpawnCinematic()) {
                    enforceCinematicView(client);
                    int t = jefri.getSpawnCinematicTicks();
                    activeTiktokCinematicTick = t;
                    applyTiktokCinematicCamera(client, jefri, t);
                } else if (jefri.isDyingCinematic()) {
                    enforceCinematicView(client);
                    activeDeathCageCinematic = true;
                    focusCameraOnBoss(client, jefri);
                } else {
                    restorePlayerViewAfterCinematic(client);
                }
                return;
            }

            restorePlayerViewAfterCinematic(client);

            // Si no está Jefri activo, revisar música del mini-jefe Pi Didi
            if (activeBossMusic != null && !activeBossMusic.isDone() && client.getSoundManager().isPlaying(activeBossMusic)) {
                return;
            }

            Box searchBox = client.player.getBoundingBox().expand(40.0D);
            List<PiDidiEntity> nearbyBosses = client.world.getEntitiesByClass(
                    PiDidiEntity.class,
                    searchBox,
                    boss -> boss.isAlive() && boss.isInCombat()
            );

            if (!nearbyBosses.isEmpty()) {
                activeBossMusic = new PiDidiBossMusicInstance(nearbyBosses.get(0));
                client.getSoundManager().play(activeBossMusic);
            }
        });
    }

    /**
     * Enfoca suavemente hacia Jefri penstein y aplica sacudidas/ángulos rítmicos estilo Edit de TikTok
     * sincronizados con el beat de la música mientras desciende del cielo y farmea aura al aterrizar.
     */
    private static void applyTiktokCinematicCamera(MinecraftClient client, JefriPensteinEntity jefri, int tick) {
        if (client.player == null) {
            return;
        }
        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetPos = jefri.getPos().add(0.0D, 1.4D, 0.0D);
        Vec3d diff = targetPos.subtract(eyePos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float targetYaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(diff.y, horizDist) * (180.0D / Math.PI)));

        // Ritmo de la canción (~130 BPM -> golpe fuerte cada 9 ticks)
        float beatKickPitch = 0.0F;
        float beatKickYaw = 0.0F;
        if (tick == 70) {
            // Golpe de impacto al aterrizar y cruzar los brazos
            beatKickPitch = 7.5F;
        } else if (tick > 70) {
            // Fase "Farmeando Aura" (ticks 71 a 120): cortes rítmicos de ángulo estilo edit de TikTok
            int beatPhase = tick % 9;
            if (beatPhase == 0) {
                beatKickPitch = -4.5F;
                beatKickYaw = ((tick / 9) % 2 == 0) ? 5.5F : -5.5F;
            } else if (beatPhase == 1) {
                beatKickPitch = 2.5F;
            }
        } else if (tick % 9 == 0) {
            // Pulsos rítmicos mientras desciende del cielo
            beatKickPitch = -2.8F;
            beatKickYaw = ((tick / 9) % 2 == 0) ? 3.2F : -3.2F;
        }

        float newYaw = MathHelper.lerpAngleDegrees(0.35F, client.player.getYaw(), targetYaw) + beatKickYaw;
        float newPitch = MathHelper.clamp(MathHelper.lerp(0.35F, client.player.getPitch(), targetPitch) + beatKickPitch, -89.0F, 89.0F);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }

    /**
     * Enfoca suavemente la mirada del jugador hacia Jefri penstein durante toda la cinemática final de muerte
     * mientras cae al suelo, queda encerrado tras los barrotes de hierro vanilla y finalmente despawnea.
     */
    private static void focusCameraOnBoss(MinecraftClient client, JefriPensteinEntity jefri) {
        if (client.player == null) {
            return;
        }
        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetPos = jefri.getPos().add(0.0D, 1.0D, 0.0D);
        Vec3d diff = targetPos.subtract(eyePos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float targetYaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(diff.y, horizDist) * (180.0D / Math.PI)));

        float newYaw = MathHelper.lerpAngleDegrees(0.35F, client.player.getYaw(), targetYaw);
        float newPitch = MathHelper.clamp(MathHelper.lerp(0.35F, client.player.getPitch(), targetPitch), -89.0F, 89.0F);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
    }

    /**
     * Oculta el HUD del jugador (como al presionar F1) y cambia automáticamente la cámara a primera persona
     * si estaba en tercera persona, guardando su configuración previa para restaurarla al terminar la cinemática.
     */
    private static void enforceCinematicView(MinecraftClient client) {
        if (client.options == null) {
            return;
        }
        if (!wasInJefriCinematic) {
            wasInJefriCinematic = true;
            savedHudHidden = client.options.hudHidden;
            savedPerspective = client.options.getPerspective();
        }
        client.options.hudHidden = true;
        if (client.options.getPerspective() != Perspective.FIRST_PERSON) {
            client.options.setPerspective(Perspective.FIRST_PERSON);
        }
    }

    /**
     * Restaura el HUD y la perspectiva original del jugador en cuanto termina la cinemática.
     */
    private static void restorePlayerViewAfterCinematic(MinecraftClient client) {
        if (wasInJefriCinematic && client.options != null) {
            wasInJefriCinematic = false;
            client.options.hudHidden = savedHudHidden;
            client.options.setPerspective(savedPerspective);
        }
    }
}
