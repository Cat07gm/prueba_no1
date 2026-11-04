package pruebano1.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import pruebano1.modid.client.PiDidiRenderer;
import pruebano1.modid.entity.ModEntities;

public class Prueba_no1Client implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.PI_DIDI, PiDidiRenderer::new);
    }
}
