package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import pruebano1.modid.entity.IceBallProjectileEntity;

/**
 * Renderizador del proyectil "Bola de Hielo" (en su versión normal de ráfaga y su versión gigante de "Una Bola de Hielo").
 */
public class IceBallProjectileRenderer extends FlyingItemEntityRenderer<IceBallProjectileEntity> {

    public IceBallProjectileRenderer(EntityRendererFactory.Context context) {
        super(context, 1.15F, false);
    }

    public static IceBallProjectileRenderer createGiant(EntityRendererFactory.Context context) {
        return new IceBallProjectileRenderer(context, 3.6F, true);
    }

    private IceBallProjectileRenderer(EntityRendererFactory.Context context, float scale, boolean lit) {
        super(context, scale, lit);
    }
}
