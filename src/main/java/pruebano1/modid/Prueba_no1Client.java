package pruebano1.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import pruebano1.modid.client.BananaProjectileRenderer;
import pruebano1.modid.client.CharlieEpsteinPopupOverlay;
import pruebano1.modid.client.CharlieKirkBossMusicInstance;
import pruebano1.modid.client.CharlieKirkCinematicController;
import pruebano1.modid.client.CharlieKirkRenderer;
import pruebano1.modid.client.CharlieWeaponParticle;
import pruebano1.modid.client.DrafteaBlockEntityRenderer;
import pruebano1.modid.client.EncapsulatedVirusParticle;
import pruebano1.modid.client.encyclopedia.EncyclopediaScreen;
import pruebano1.modid.client.FerOvillaRenderer;
import pruebano1.modid.client.GuardProjectileRenderer;
import pruebano1.modid.client.HelloKittyParticle;
import pruebano1.modid.client.IceBallProjectileRenderer;
import pruebano1.modid.client.JefriBossMusicInstance;
import pruebano1.modid.client.JefriPensteinRenderer;
import pruebano1.modid.client.JoseTorresBossMusicInstance;
import pruebano1.modid.client.JoseTorresRenderer;
import pruebano1.modid.client.JoseTorresVoiceSoundInstance;
import pruebano1.modid.client.KotexProjectileRenderer;
import pruebano1.modid.client.PiDidiBossMusicInstance;
import pruebano1.modid.client.PiDidiRenderer;
import pruebano1.modid.client.PlayerDeathKoOverlay;
import pruebano1.modid.client.TonyFase1Renderer;
import pruebano1.modid.client.TonyFase2Renderer;
import pruebano1.modid.client.TonyFase3Renderer;
import pruebano1.modid.client.ValentinaPrietaRenderer;
import pruebano1.modid.client.VitorJugoRenderer;
import pruebano1.modid.client.WeedLeafParticle;
import pruebano1.modid.client.WhiteHouseGuardRenderer;
import pruebano1.modid.client.YostinBiberRenderer;
import pruebano1.modid.entity.CharlieKirkEntity;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.JoseTorresEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;
import pruebano1.modid.network.CharlieEpsteinPopupPayload;
import pruebano1.modid.network.PlayerDeathKoPayload;

import java.util.List;

public class Prueba_no1Client implements ClientModInitializer {

    private static PiDidiBossMusicInstance activeBossMusic = null;
    private static JefriBossMusicInstance activeJefriMusic = null;
    private static JoseTorresBossMusicInstance activeJoseTorresMusic = null;
    private static JoseTorresVoiceSoundInstance activeJoseTorresVoice = null;
    private static CharlieKirkBossMusicInstance activeCharlieKirkMusic = null;
    private static int lastCharlieMusicEntityId = -1;
    private static boolean charlieIntroFinished = false;
    private static int lastJoseEntityId = -1;
    private static int lastJoseVoiceSeq = 0;
    private static int activeTiktokCinematicTick = 0;
    private static int activeJoseTorresCinematicTick = 0;
    private static int activeCharlieKirkCinematicTick = 0;
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
        EntityRendererRegistry.register(ModEntities.JOSE_TORRES, JoseTorresRenderer::new);
        EntityRendererRegistry.register(ModEntities.KOTEX_PROJECTILE, KotexProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.ICE_BALL_PROJECTILE, IceBallProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.GIANT_ICE_BALL_PROJECTILE, IceBallProjectileRenderer::createGiant);
        EntityRendererRegistry.register(ModEntities.SEAT, net.minecraft.client.render.entity.EmptyEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TONY_FASE1, TonyFase1Renderer::new);
        EntityRendererRegistry.register(ModEntities.TONY_FASE2, TonyFase2Renderer::new);
        EntityRendererRegistry.register(ModEntities.TONY_FASE3, TonyFase3Renderer::new);
        EntityRendererRegistry.register(ModEntities.CHARLIE_KIRK, CharlieKirkRenderer::new);
        EntityRendererRegistry.register(ModEntities.WHITE_HOUSE_GUARD_PISTOL, WhiteHouseGuardRenderer::pistol);
        EntityRendererRegistry.register(ModEntities.WHITE_HOUSE_GUARD_RIFLE, WhiteHouseGuardRenderer::rifle);
        EntityRendererRegistry.register(ModEntities.WHITE_HOUSE_GUARD_BAZOOKA, WhiteHouseGuardRenderer::bazooka);
        EntityRendererRegistry.register(ModEntities.GUARD_BULLET_PROJECTILE, GuardProjectileRenderer::bullet);
        EntityRendererRegistry.register(ModEntities.GUARD_ROCKET_PROJECTILE, GuardProjectileRenderer::rocket);

        // Pantalla de la Draftea: dibuja los ítems de los rodillos
        BlockEntityRendererFactories.register(ModBlockEntities.DRAFTEA, DrafteaBlockEntityRenderer::new);
        // Plantas del casino: las hojas tienen huecos transparentes
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.CASINO_PALM_POT, ModBlocks.CASINO_MONSTERA_POT);

        ParticleFactoryRegistry.getInstance().register(ModParticles.HELLO_KITTY, HelloKittyParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.WEED_LEAF, WeedLeafParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.ENCAPSULATED_VIRUS, EncapsulatedVirusParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(ModParticles.CHARLIE_WEAPON, CharlieWeaponParticle.Factory::new);

        // Recibir aviso cuando cualquier jugador muere para activar el sonido y el GIF/efecto K.O. en pantalla
        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathKoPayload.ID, (payload, context) -> {
            context.client().execute(() -> PlayerDeathKoOverlay.trigger(payload.deadPlayerName()));
        });

        // Recibir aviso rápido de telequinesis / Efecto Epstein de Charlie Kirk
        ClientPlayNetworking.registerGlobalReceiver(CharlieEpsteinPopupPayload.ID, (payload, context) -> {
            context.client().execute(() -> CharlieEpsteinPopupOverlay.trigger(payload.phase2()));
        });

        // Recibir límites protegidos del casino "Draftea Tony Diaz" y bloquear clics en cliente sin parpadeo
        ClientPlayNetworking.registerGlobalReceiver(pruebano1.modid.network.CasinoBoundsPayload.ID, (payload, context) -> {
            context.client().execute(() -> pruebano1.modid.world.DrafteaCasinoManager.setClientBounds(payload));
        });

        // Lo mismo para la torre del Lobby principal
        ClientPlayNetworking.registerGlobalReceiver(pruebano1.modid.network.LobbyBoundsPayload.ID, (payload, context) -> {
            context.client().execute(() -> pruebano1.modid.world.LobbyTowerManager.setClientBounds(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(pruebano1.modid.network.DungeonProgressionPayload.ID, (payload, context) -> {
            context.client().execute(() -> pruebano1.modid.client.encyclopedia.ClientDungeonProgression.applyPayload(payload));
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            pruebano1.modid.world.LobbyTowerManager.clearClientBounds();
            pruebano1.modid.client.encyclopedia.ClientDungeonProgression.reset();
        });

        // Música del casino: recibir lo que eligió el DJ, abrir su interfaz y reproducir solo dentro del casino
        ClientPlayNetworking.registerGlobalReceiver(pruebano1.modid.network.CasinoMusicStatePayload.ID, (payload, context) ->
                context.client().execute(() -> pruebano1.modid.client.CasinoMusicClient.onState(payload))
        );
        pruebano1.modid.block.DjControllerBlock.clientScreenOpener = pos ->
                MinecraftClient.getInstance().setScreen(new pruebano1.modid.client.DjControllerScreen(pos));
        ClientTickEvents.END_CLIENT_TICK.register(pruebano1.modid.client.CasinoMusicClient::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(pruebano1.modid.client.CasinoMusicClient::reset)
        );

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                pruebano1.modid.world.DrafteaCasinoManager.clearClientBounds()
        );
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> {
                    CharlieKirkCinematicController.reset(client);
                    CharlieEpsteinPopupOverlay.reset();
                })
        );

        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient() && pruebano1.modid.world.DrafteaCasinoProtection.shouldCancelBlockAttack(world, player, pos)) {
                return net.minecraft.util.ActionResult.FAIL;
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient() && pruebano1.modid.world.DrafteaCasinoProtection.shouldCancelBlockUse(world, player, hand, hitResult)) {
                return net.minecraft.util.ActionResult.FAIL;
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        // Dibujar el efecto K.O. y los destellos/barras de cinemática estilo Edit de TikTok de Jefri penstein
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            float tickDelta = tickCounter.getTickDelta(false);
            if (PlayerDeathKoOverlay.isActive()) {
                PlayerDeathKoOverlay.render(drawContext, tickDelta);
            }
            if (CharlieEpsteinPopupOverlay.isActive()) {
                CharlieEpsteinPopupOverlay.render(drawContext, tickDelta);
            }
            if (activeTiktokCinematicTick > 0 || activeDeathCageCinematic || activeJoseTorresCinematicTick > 0 || activeCharlieKirkCinematicTick > 0) {
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
            float charlieBlackoutAlpha = CharlieKirkCinematicController.getBlackoutAlpha(tickDelta);
            if (charlieBlackoutAlpha > 0.001F) {
                int w = drawContext.getScaledWindowWidth();
                int h = drawContext.getScaledWindowHeight();
                int alpha = MathHelper.clamp((int) (charlieBlackoutAlpha * 255.0F), 0, 255);
                drawContext.fill(0, 0, w, h, alpha << 24);
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

        // Tick de cliente: actualizar efecto K.O., avisos, música de jefes y cámara en cinemáticas
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerDeathKoOverlay.tick();
            CharlieEpsteinPopupOverlay.tick();
            activeTiktokCinematicTick = 0;
            activeJoseTorresCinematicTick = 0;
            activeCharlieKirkCinematicTick = 0;
            activeDeathCageCinematic = false;

            if (client.world == null || client.player == null || !client.player.isAlive()) {
                CharlieKirkCinematicController.reset(client);
                CharlieEpsteinPopupOverlay.reset();
                restorePlayerViewAfterCinematic(client);
                activeBossMusic = null;
                activeJefriMusic = null;
                activeJoseTorresMusic = null;
                activeCharlieKirkMusic = null;
                lastCharlieMusicEntityId = -1;
                charlieIntroFinished = false;
                return;
            }

            if (encyclopediaKey != null) {
                while (encyclopediaKey.wasPressed()) {
                    if (client.currentScreen == null) {
                        client.setScreen(new EncyclopediaScreen());
                    }
                }
            }

            Box charlieSearchBox = client.player.getBoundingBox().expand(280.0D);
            List<CharlieKirkEntity> nearbyCharlies = client.world.getEntitiesByClass(
                    CharlieKirkEntity.class,
                    charlieSearchBox,
                    CharlieKirkEntity::isAlive
            );

            if (!nearbyCharlies.isEmpty()) {
                CharlieKirkEntity charlie = nearbyCharlies.get(0);
                if (charlie.getId() != lastCharlieMusicEntityId) {
                    lastCharlieMusicEntityId = charlie.getId();
                    charlieIntroFinished = false;
                    if (activeCharlieKirkMusic != null) {
                        client.getSoundManager().stop(activeCharlieKirkMusic);
                        activeCharlieKirkMusic = null;
                    }
                }

                if (charlie.shouldPlayBossMusic()) {
                    boolean musicStoppedOrDone = activeCharlieKirkMusic == null
                            || activeCharlieKirkMusic.isDone()
                            || (activeCharlieKirkMusic.getTicksPlayed() >= 5 && !client.getSoundManager().isPlaying(activeCharlieKirkMusic));
                    if (musicStoppedOrDone) {
                        if (activeCharlieKirkMusic != null
                                && !activeCharlieKirkMusic.isLoopTrack()
                                && (activeCharlieKirkMusic.isDone() || activeCharlieKirkMusic.hasStartedPlaying() || activeCharlieKirkMusic.getTicksPlayed() >= 4490)) {
                            charlieIntroFinished = true;
                        }
                        if (activeCharlieKirkMusic != null) {
                            client.getSoundManager().stop(activeCharlieKirkMusic);
                        }
                        activeCharlieKirkMusic = new CharlieKirkBossMusicInstance(charlie, charlieIntroFinished);
                        client.getSoundManager().play(activeCharlieKirkMusic);
                    }
                } else if (activeCharlieKirkMusic != null) {
                    client.getSoundManager().stop(activeCharlieKirkMusic);
                    activeCharlieKirkMusic = null;
                    charlieIntroFinished = false;
                }

                if (charlie.isInSpawnCinematic()) {
                    enforceCinematicView(client);
                    activeCharlieKirkCinematicTick = charlie.getSpawnCinematicTicks();
                    CharlieKirkCinematicController.update(client, charlie);
                    client.player.setVelocity(Vec3d.ZERO);
                    if (client.options != null) {
                        client.options.forwardKey.setPressed(false);
                        client.options.backKey.setPressed(false);
                        client.options.leftKey.setPressed(false);
                        client.options.rightKey.setPressed(false);
                        client.options.jumpKey.setPressed(false);
                        client.options.sprintKey.setPressed(false);
                    }
                    if (activeCharlieKirkCinematicTick >= CharlieKirkEntity.TELEPORT_PLAYERS_TICK) {
                        focusCameraOnEntity(client, charlie, 1.35D);
                    }
                } else if (charlie.isDyingCinematic()) {
                    CharlieKirkCinematicController.reset(client);
                    enforceCinematicView(client);
                    activeCharlieKirkCinematicTick = 1;
                    focusCameraOnEntity(client, charlie, 1.35D);
                } else {
                    CharlieKirkCinematicController.reset(client);
                    restorePlayerViewAfterCinematic(client);
                }
                return;
            } else {
                CharlieKirkCinematicController.reset(client);
                lastCharlieMusicEntityId = -1;
                charlieIntroFinished = false;
                if (activeCharlieKirkMusic != null) {
                    client.getSoundManager().stop(activeCharlieKirkMusic);
                    activeCharlieKirkMusic = null;
                }
            }

            Box joseSearchBox = client.player.getBoundingBox().expand(64.0D);
            List<JoseTorresEntity> nearbyJoseTorres = client.world.getEntitiesByClass(
                    JoseTorresEntity.class,
                    joseSearchBox,
                    JoseTorresEntity::isAlive
            );

            if (!nearbyJoseTorres.isEmpty()) {
                JoseTorresEntity jose = nearbyJoseTorres.get(0);

                // Detectar y reproducir audios de ataques especiales ("Zombies", "Dialecto" y "Una Bola de Hielo") sin atenuación por distancia
                if (jose.getId() != lastJoseEntityId) {
                    lastJoseEntityId = jose.getId();
                    lastJoseVoiceSeq = jose.getSpecialVoiceSeq();
                } else if (jose.getSpecialVoiceSeq() != lastJoseVoiceSeq) {
                    lastJoseVoiceSeq = jose.getSpecialVoiceSeq();
                    int specialType = jose.getActiveSpecialType();
                    if (specialType == JoseTorresEntity.SPECIAL_ZOMBIES
                            || specialType == JoseTorresEntity.SPECIAL_DIALECTO
                            || specialType == JoseTorresEntity.SPECIAL_GIANT_ICE_BALL) {
                        if (activeJoseTorresVoice != null) {
                            client.getSoundManager().stop(activeJoseTorresVoice);
                        }
                        net.minecraft.sound.SoundEvent voiceEvent = switch (specialType) {
                            case JoseTorresEntity.SPECIAL_ZOMBIES -> ModSounds.JOSE_TORRES_ZOMBIES;
                            case JoseTorresEntity.SPECIAL_DIALECTO -> ModSounds.JOSE_TORRES_DIALECTO;
                            default -> ModSounds.JOSE_TORRES_BOLA_HIELO;
                        };
                        activeJoseTorresVoice = new JoseTorresVoiceSoundInstance(jose, voiceEvent);
                        client.getSoundManager().play(activeJoseTorresVoice);
                    }
                }

                // 1. Música de Fase 1 (José Torres) o Fase 2 (Jhoseph towers, recortada desde el segundo 9)
                if (jose.shouldPlayPhase1Music()) {
                    if (activeJoseTorresMusic == null
                            || activeJoseTorresMusic.isPhase2Track()
                            || activeJoseTorresMusic.isDone()
                            || !client.getSoundManager().isPlaying(activeJoseTorresMusic)) {
                        if (activeJoseTorresMusic != null) {
                            client.getSoundManager().stop(activeJoseTorresMusic);
                        }
                        activeJoseTorresMusic = new JoseTorresBossMusicInstance(jose, false);
                        client.getSoundManager().play(activeJoseTorresMusic);
                    }
                } else if (jose.shouldPlayPhase2Music()) {
                    if (activeJoseTorresMusic == null
                            || !activeJoseTorresMusic.isPhase2Track()
                            || activeJoseTorresMusic.isDone()
                            || !client.getSoundManager().isPlaying(activeJoseTorresMusic)) {
                        if (activeJoseTorresMusic != null) {
                            client.getSoundManager().stop(activeJoseTorresMusic);
                        }
                        activeJoseTorresMusic = new JoseTorresBossMusicInstance(jose, true);
                        client.getSoundManager().play(activeJoseTorresMusic);
                    }
                } else if (activeJoseTorresMusic != null) {
                    client.getSoundManager().stop(activeJoseTorresMusic);
                    activeJoseTorresMusic = null;
                }

                // 2. Cinemática de entrada (5 s tocando acordeón) o cinemática final de muerte (5 s)
                if (jose.isInSpawnCinematic()) {
                    enforceCinematicView(client);
                    activeJoseTorresCinematicTick = jose.getSpawnCinematicTicks();
                    focusCameraOnEntity(client, jose, 1.35D);
                } else if (jose.isDyingCinematic()) {
                    enforceCinematicView(client);
                    activeJoseTorresCinematicTick = 1;
                    focusCameraOnEntity(client, jose, 1.35D);
                } else {
                    restorePlayerViewAfterCinematic(client);
                }
                return;
            } else {
                lastJoseEntityId = -1;
                if (activeJoseTorresVoice != null) {
                    client.getSoundManager().stop(activeJoseTorresVoice);
                    activeJoseTorresVoice = null;
                }
                if (activeJoseTorresMusic != null) {
                    client.getSoundManager().stop(activeJoseTorresMusic);
                    activeJoseTorresMusic = null;
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
        focusCameraOnEntity(client, jefri, 1.0D);
    }

    private static void focusCameraOnEntity(MinecraftClient client, net.minecraft.entity.Entity target, double heightOffset) {
        if (client.player == null) {
            return;
        }
        Vec3d eyePos = client.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0.0D, heightOffset, 0.0D);
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
