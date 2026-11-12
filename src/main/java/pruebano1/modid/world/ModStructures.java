package pruebano1.modid.world;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import pruebano1.modid.Prueba_no1;

public class ModStructures {

    public static final RegistryKey<Structure> ISLA_PENSTEIN_KEY = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(Prueba_no1.MOD_ID, "isla_penstein")
    );

    public static final StructurePieceType ISLA_PENSTEIN_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE,
            Identifier.of(Prueba_no1.MOD_ID, "isla_penstein_piece"),
            IslaPensteinPiece::new
    );

    public static final StructureType<IslaPensteinStructure> ISLA_PENSTEIN_TYPE = Registry.register(
            Registries.STRUCTURE_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "isla_penstein"),
            () -> IslaPensteinStructure.CODEC
    );

    public static final Feature<DefaultFeatureConfig> ISLA_PENSTEIN_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "isla_penstein_feature"),
            new IslaPensteinFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> ISLA_PENSTEIN_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "isla_penstein_feature_placed")
    );

    public static final Feature<DefaultFeatureConfig> DRAFTEA_CASINO_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "draftea_casino_feature"),
            new DrafteaCasinoFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> DRAFTEA_CASINO_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "draftea_casino_feature_placed")
    );

    public static void registerModStructures() {
        Prueba_no1.LOGGER.info("Registrando Isla Penstein y Casino único 'Draftea Tony Diaz' para " + Prueba_no1.MOD_ID);
    }
}
