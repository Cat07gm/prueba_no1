package pruebano1.modid.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Efecto de estado negativo "Efecto Epstein" aplicado por el ataque telequinético de Charlie Kirk.
 * Mientras el jugador lo tenga activo (30s o hasta curarlo con leche), no puede dañar a Charlie Kirk
 * y es perseguido por niños invocados de Isla Penstein dentro de La Casa Blanca.
 */
public class EpsteinStatusEffect extends StatusEffect {

    public EpsteinStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 0x5B21B6);
    }
}
