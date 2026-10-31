package pruebano1.modid;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModWorldGeneration {

    // Mena de piedra: capas positivas donde también sale el diamante (Y=0 a Y=16)
    public static final RegistryKey<PlacedFeature> SCARLET_ORE_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "scarlet_ore_placed")
    );

    // Mena de pizarra profunda: únicamente en capas negativas (Y=-64 a Y=-1)
    public static final RegistryKey<PlacedFeature> DEEPSLATE_SCARLET_ORE_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "deepslate_scarlet_ore_placed")
    );

    public static void generateModWorldGen() {
        Prueba_no1.LOGGER.info("Registrando generación de menas en el mundo para " + Prueba_no1.MOD_ID);

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                SCARLET_ORE_PLACED_KEY
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                DEEPSLATE_SCARLET_ORE_PLACED_KEY
        );
    }
}
