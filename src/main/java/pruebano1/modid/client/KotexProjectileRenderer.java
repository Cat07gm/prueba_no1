package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import pruebano1.modid.entity.KotexProjectileEntity;

/**
 * Renderizador del proyectil "Toalla Kotex" usando directamente la textura del nuevo ítem Toalla Kotex.
 */
public class KotexProjectileRenderer extends FlyingItemEntityRenderer<KotexProjectileEntity> {

    public KotexProjectileRenderer(EntityRendererFactory.Context context) {
        super(context, 1.15F, false);
    }
}
