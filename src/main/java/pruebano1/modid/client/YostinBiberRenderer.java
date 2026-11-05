package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.YostinBiberEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D de GeckoLib para el secuaz Yostin Biber.
 * Usa withScale(0.5F) para que tenga exactamente la mitad del tamaño de un jugador.
 */
public class YostinBiberRenderer extends GeoEntityRenderer<YostinBiberEntity> {

    public YostinBiberRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "yostin_biber")));
        this.withScale(0.5F); // Mitad del tamaño del jugador
        this.shadowRadius = 0.3F;
    }
}
