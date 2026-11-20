package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.WhiteHouseGuardEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D de GeckoLib para los agentes de Seguridad / Servicio Secreto de La Casa Blanca.
 */
public class WhiteHouseGuardRenderer extends GeoEntityRenderer<WhiteHouseGuardEntity> {

    private WhiteHouseGuardRenderer(EntityRendererFactory.Context renderManager, String modelName) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, modelName)));
        this.shadowRadius = 0.5F;
    }

    public static WhiteHouseGuardRenderer pistol(EntityRendererFactory.Context context) {
        return new WhiteHouseGuardRenderer(context, "white_house_guard_pistol");
    }

    public static WhiteHouseGuardRenderer rifle(EntityRendererFactory.Context context) {
        return new WhiteHouseGuardRenderer(context, "white_house_guard_rifle");
    }

    public static WhiteHouseGuardRenderer bazooka(EntityRendererFactory.Context context) {
        return new WhiteHouseGuardRenderer(context, "white_house_guard_bazooka");
    }
}
