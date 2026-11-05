package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.FerOvillaEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D de GeckoLib para la francotiradora Fer Ovilla (mitad del tamaño del jugador).
 */
public class FerOvillaRenderer extends GeoEntityRenderer<FerOvillaEntity> {

    public FerOvillaRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "fer_ovilla")));
        this.withScale(0.5F); // Mitad del tamaño del jugador
        this.shadowRadius = 0.3F;
    }
}
