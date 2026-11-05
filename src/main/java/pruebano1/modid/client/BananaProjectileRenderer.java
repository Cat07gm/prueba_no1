package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.BananaProjectileEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D de GeckoLib para el proyectil de Plátano Bumerán.
 */
public class BananaProjectileRenderer extends GeoEntityRenderer<BananaProjectileEntity> {

    public BananaProjectileRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "banana_projectile")));
        this.shadowRadius = 0.15F;
    }
}
