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

    public static final Feature<DefaultFeatureConfig> LOBBY_TOWER_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "lobby_tower_feature"),
            new LobbyTowerFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> LOBBY_TOWER_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "lobby_tower_feature_placed")
    );

    public static final RegistryKey<Structure> CASA_JOSE_TORRES_KEY = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_jose_torres")
    );

    public static final StructurePieceType CASA_JOSE_TORRES_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_jose_torres_piece"),
            CasaJoseTorresPiece::new
    );

    public static final StructureType<CasaJoseTorresStructure> CASA_JOSE_TORRES_TYPE = Registry.register(
            Registries.STRUCTURE_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_jose_torres"),
            () -> CasaJoseTorresStructure.CODEC
    );

    public static final Feature<DefaultFeatureConfig> CASA_JOSE_TORRES_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_jose_torres_feature"),
            new CasaJoseTorresFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> CASA_JOSE_TORRES_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_jose_torres_feature_placed")
    );

    public static final RegistryKey<Structure> CASA_BLANCA_KEY = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca")
    );

    public static final StructurePieceType CASA_BLANCA_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca_piece"),
            CasaBlancaPiece::new
    );

    public static final StructureType<CasaBlancaStructure> CASA_BLANCA_TYPE = Registry.register(
            Registries.STRUCTURE_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca"),
            () -> CasaBlancaStructure.CODEC
    );

    public static final Feature<DefaultFeatureConfig> CASA_BLANCA_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca_feature"),
            new CasaBlancaFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> CASA_BLANCA_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca_feature_placed")
    );

    public static final Feature<DefaultFeatureConfig> CASA_BLANCA_CLEANUP_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca_cleanup_feature"),
            new CasaBlancaCleanupFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> CASA_BLANCA_CLEANUP_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "casa_blanca_cleanup_feature_placed")
    );

    public static final RegistryKey<Structure> PALACIO_NACIONAL_KEY = RegistryKey.of(
            RegistryKeys.STRUCTURE,
            Identifier.of(Prueba_no1.MOD_ID, "palacio_nacional")
    );

    public static final StructurePieceType PALACIO_NACIONAL_PIECE = Registry.register(
            Registries.STRUCTURE_PIECE,
            Identifier.of(Prueba_no1.MOD_ID, "palacio_nacional_piece"),
            PalacioNacionalPiece::new
    );

    public static final StructureType<PalacioNacionalStructure> PALACIO_NACIONAL_TYPE = Registry.register(
            Registries.STRUCTURE_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "palacio_nacional"),
            () -> PalacioNacionalStructure.CODEC
    );

    public static final Feature<DefaultFeatureConfig> PALACIO_NACIONAL_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "palacio_nacional_feature"),
            new PalacioNacionalFeature(DefaultFeatureConfig.CODEC)
    );

    public static final RegistryKey<PlacedFeature> PALACIO_NACIONAL_PLACED_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            Identifier.of(Prueba_no1.MOD_ID, "palacio_nacional_feature_placed")
    );

    public static void registerModStructures() {
        Prueba_no1.LOGGER.info("Registrando Isla Penstein, Casino 'Draftea Tony Diaz', La Casa de José Torres, La Casa Blanca y El Palacio Nacional para " + Prueba_no1.MOD_ID);
    }
}
