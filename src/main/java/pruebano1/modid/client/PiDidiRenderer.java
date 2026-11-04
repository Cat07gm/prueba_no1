package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.PiDidiEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador de GeckoLib para el mini-jefe Pi Didi.
 * Conecta automáticamente con:
 * - assets/prueba_no1/geo/entity/pi_didi.geo.json
 * - assets/prueba_no1/animations/entity/pi_didi.animation.json
 * - assets/prueba_no1/textures/entity/pi_didi.png
 */
public class PiDidiRenderer extends GeoEntityRenderer<PiDidiEntity> {

    public PiDidiRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "pi_didi")));
        this.shadowRadius = 0.6F;
    }
}
