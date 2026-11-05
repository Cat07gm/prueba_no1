package pruebano1.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.util.math.Box;
import pruebano1.modid.client.BananaProjectileRenderer;
import pruebano1.modid.client.FerOvillaRenderer;
import pruebano1.modid.client.PiDidiBossMusicInstance;
import pruebano1.modid.client.PiDidiRenderer;
import pruebano1.modid.client.VitorJugoRenderer;
import pruebano1.modid.client.YostinBiberRenderer;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;

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

        // Detectar cuando Pi Didi entra en batalla cerca del jugador para reproducir su soundtrack de fondo
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
