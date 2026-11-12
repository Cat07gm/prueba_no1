package pruebano1.modid;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.world.ModStructures;

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
        Prueba_no1.LOGGER.info("Registrando generación de menas y estructuras en el mundo para " + Prueba_no1.MOD_ID);

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

        // Generador completo de los 520x520 bloques de la Isla Penstein
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.SURFACE_STRUCTURES,
                ModStructures.ISLA_PENSTEIN_PLACED_KEY
        );

        // Generador único del casino "Draftea Tony Diaz" cerca del spawn inicial
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.SURFACE_STRUCTURES,
                ModStructures.DRAFTEA_CASINO_PLACED_KEY
        );

        // Permitir que los 4 niños spawneen en toda el área de 520x520 de la Isla Penstein
        // (ModEntities.canIslandChildSpawn filtra para que SOLO aparezcan dentro de la Isla Penstein)
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), SpawnGroup.MONSTER, ModEntities.YOSTIN_BIBER, 30, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), SpawnGroup.MONSTER, ModEntities.VITOR_JUGO, 20, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), SpawnGroup.MONSTER, ModEntities.FER_OVILLA, 25, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), SpawnGroup.MONSTER, ModEntities.VALENTINA_PRIETA, 25, 1, 2);
    }
}
