package pruebano1.modid.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.JefriPensteinEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador de GeckoLib para el Jefe Final Jefri penstein.
 * - Tamaño normal igual a Pi Didi (escala 1.0, shadowRadius 0.6F).
 * - Aplica un aura/brillo rojo demoníaco cuando está en "epstein modo diablo" (se apaga al caer en la jaula final).
 */
public class JefriPensteinRenderer extends GeoEntityRenderer<JefriPensteinEntity> {

    public JefriPensteinRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "jefri_penstein")));
        this.shadowRadius = 0.6F;
    }

    @Override
    public Color getRenderColor(JefriPensteinEntity animatable, float partialTick, int packedLight) {
        if (animatable.isDevilMode() && !animatable.isDyingCinematic()) {
            // Pulso de aura roja carmesí en Modo Diablo
            float pulse = 0.55F + 0.45F * (float) Math.sin((animatable.age + partialTick) * 0.35F);
            int gAndB = (int) (130 * pulse);
            return Color.ofRGBA(255, gAndB, gAndB, 255);
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }
}
