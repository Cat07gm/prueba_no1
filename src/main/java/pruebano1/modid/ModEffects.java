package pruebano1.modid;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import pruebano1.modid.effect.EpsteinStatusEffect;

public class ModEffects {

    public static final RegistryEntry<StatusEffect> EPSTEIN_EFFECT = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(Prueba_no1.MOD_ID, "epstein_effect"),
            new EpsteinStatusEffect()
    );

    public static void registerModEffects() {
        Prueba_no1.LOGGER.info("Registrando efectos de estado personalizados para " + Prueba_no1.MOD_ID);
    }
}
