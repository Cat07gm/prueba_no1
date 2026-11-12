package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.TonyFase2Entity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D GeckoLib para TonyFase2 (Clientes del casino con sudadera blanca).
 */
public class TonyFase2Renderer extends GeoEntityRenderer<TonyFase2Entity> {

    public TonyFase2Renderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "tony_fase2")));
        this.shadowRadius = 0.5F;
    }
}
