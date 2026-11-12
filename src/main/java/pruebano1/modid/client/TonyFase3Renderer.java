package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.TonyFase3Entity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D GeckoLib para TonyFase3 (Personal de seguridad del casino con sudadera negra).
 */
public class TonyFase3Renderer extends GeoEntityRenderer<TonyFase3Entity> {

    public TonyFase3Renderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "tony_fase3")));
        this.shadowRadius = 0.5F;
    }
}
