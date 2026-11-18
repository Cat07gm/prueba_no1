package pruebano1.modid.client;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.CharlieKirkEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador de GeckoLib para Charlie Kirk (jefe de La Casa Blanca).
 * Oculta el modelo y la sombra mientras espera en el podio o durante la cinemática previa
 * al segundo 47.8, de modo que aparezca justo frente a los jugadores al disiparse la pantalla en negro.
 */
public class CharlieKirkRenderer extends GeoEntityRenderer<CharlieKirkEntity> {

    public CharlieKirkRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "charlie_kirk")));
        this.shadowRadius = 0.6F;
    }

    @Override
    public boolean shouldRender(CharlieKirkEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.isWaitingForPlayer()
                || (entity.isInSpawnCinematic() && entity.getSpawnCinematicTicks() < CharlieKirkEntity.REVEAL_MODEL_TICK)) {
            return false;
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }
}
