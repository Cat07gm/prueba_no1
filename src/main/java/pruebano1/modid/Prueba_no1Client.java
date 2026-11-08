package pruebano1.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.Box;
import pruebano1.modid.client.BananaProjectileRenderer;
import pruebano1.modid.client.FerOvillaRenderer;
import pruebano1.modid.client.HelloKittyParticle;
import pruebano1.modid.client.PiDidiBossMusicInstance;
import pruebano1.modid.client.PiDidiRenderer;
import pruebano1.modid.client.PlayerDeathKoOverlay;
import pruebano1.modid.client.ValentinaPrietaRenderer;
import pruebano1.modid.client.VitorJugoRenderer;
import pruebano1.modid.client.YostinBiberRenderer;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;
import pruebano1.modid.network.PlayerDeathKoPayload;

import java.util.List;

public class Prueba_no1Client implements ClientModInitializer {

    private static PiDidiBossMusicInstance activeBossMusic = null;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.PI_DIDI, PiDidiRenderer::new);
        EntityRendererRegistry.register(ModEntities.YOSTIN_BIBER, YostinBiberRenderer::new);
        EntityRendererRegistry.register(ModEntities.VITOR_JUGO, VitorJugoRenderer::new);
        EntityRendererRegistry.register(ModEntities.FER_OVILLA, FerOvillaRenderer::new);
        EntityRendererRegistry.register(ModEntities.BANANA_PROJECTILE, BananaProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.VALENTINA_PRIETA, ValentinaPrietaRenderer::new);

        ParticleFactoryRegistry.getInstance().register(ModParticles.HELLO_KITTY, HelloKittyParticle.Factory::new);

        // Recibir aviso cuando cualquier jugador muere para activar el sonido y el GIF/efecto K.O. en pantalla
        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathKoPayload.ID, (payload, context) -> {
            context.client().execute(() -> PlayerDeathKoOverlay.trigger(payload.deadPlayerName()));
        });

        // Dibujar el efecto K.O. en el HUD de todos los jugadores vivos
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (PlayerDeathKoOverlay.isActive()) {
                PlayerDeathKoOverlay.render(drawContext, tickCounter.getTickDelta(false));
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

        // Tick de cliente: actualizar contador del efecto K.O. y detectar soundtrack de Pi Didi
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerDeathKoOverlay.tick();

            if (client.world == null || client.player == null || !client.player.isAlive()) {
                activeBossMusic = null;
                return;
            }

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
}
