package pruebano1.modid.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.TonyFase1Entity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador 3D GeckoLib para TonyFase1 (Bartender del casino con sudadera verde y utilería animada).
 */
public class TonyFase1Renderer extends GeoEntityRenderer<TonyFase1Entity> {

    public TonyFase1Renderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "tony_fase1")));
        this.shadowRadius = 0.5F;
    }

    @Override
    public @Nullable RenderLayer getRenderType(
            TonyFase1Entity animatable,
            Identifier texture,
            @Nullable VertexConsumerProvider bufferSource,
            float partialTick
    ) {
        return RenderLayer.getEntityTranslucent(texture);
    }
}
