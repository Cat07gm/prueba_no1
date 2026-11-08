package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.ValentinaPrietaEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D de GeckoLib para Valentina Prieta (mitad del tamaño del jugador).
 */
public class ValentinaPrietaRenderer extends GeoEntityRenderer<ValentinaPrietaEntity> {

    public ValentinaPrietaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "valentina_prieta")));
        this.withScale(0.5F); // Mitad del tamaño del jugador (familia de Niños)
        this.shadowRadius = 0.3F;
    }
}
