package pruebano1.modid.client;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.JoseTorresEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador de GeckoLib para el Boss José Torres (Fase 1) y Jhoseph towers (Fase 2).
 * - En Fase 1 muestra a José Torres con su traje negro con dorado, sombrero texano negro y acordeón rojo/dorado.
 * - En Fase 2 (a la mitad de vida) cambia su apariencia al traje completamente blanco con detalles azules,
 *   sombrero blanco y acordeón blanco/negro, y sigue tocando su acordeón en todo momento.
 * - Muestra la gran "bola_hielo_especial" cargándose sobre su cabeza durante el 4.º ataque especial ("Una Bola de Hielo").
 */
public class JoseTorresRenderer extends GeoEntityRenderer<JoseTorresEntity> {

    private static final Identifier PHASE_1_TEXTURE = Identifier.of(Prueba_no1.MOD_ID, "textures/entity/jose_torres.png");
    private static final Identifier PHASE_2_TEXTURE = Identifier.of(Prueba_no1.MOD_ID, "textures/entity/jhoseph_towers.png");

    public JoseTorresRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "jose_torres")));
        this.shadowRadius = 0.6F;
    }

    @Override
    public Identifier getTextureLocation(JoseTorresEntity animatable) {
        return animatable.isPhase2() ? PHASE_2_TEXTURE : PHASE_1_TEXTURE;
    }

    @Override
    public void preRender(
            MatrixStack poseStack,
            JoseTorresEntity animatable,
            BakedGeoModel model,
            @Nullable VertexConsumerProvider bufferSource,
            @Nullable VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, renderColor);
        boolean chargingGiantIceBall = animatable.getActiveSpecialType() == JoseTorresEntity.SPECIAL_GIANT_ICE_BALL;
        model.getBone("bola_hielo_especial").ifPresent(bone -> bone.setHidden(!chargingGiantIceBall));
    }
}
