package pruebano1.modid.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuarta Gran Dungeon del mod: "El Palacio Nacional de México" (480x480 bloques incluyendo Plaza de la Constitución / Zócalo).
 * - ÚNICA en el mundo (solo se genera 1 ejemplar en todo el Overworld).
 * - Extremadamente difícil de encontrar (>= 5,500 bloques del origen y >= 3,200 bloques de Casa de José Torres y La Casa Blanca).
 * - Aparece en biomas terrestres templados/cálidos de bajo relieve, con talud suave sin cortes bruscos ni terreno flotante.
 * - Protegida contra rotura por explosiones y libre de aparición de mobs normales de Minecraft.
 */
public class PalacioNacionalStructure extends Structure {

    public static final MapCodec<PalacioNacionalStructure> CODEC = createCodec(PalacioNacionalStructure::new);

    // Misma configuración que worldgen/structure_set/palacio_nacional.json
    public static final RandomSpreadStructurePlacement PLACEMENT = new RandomSpreadStructurePlacement(
            360,
            140,
            SpreadType.LINEAR,
            729384615
    );

    /** Radio de chunks (15 chunks = ±240 bloques = 480x480 bloques). */
    public static final int CHUNK_RADIUS = 15;

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<Long, PalaceAnchor> SEED_ANCHOR_CACHE = new ConcurrentHashMap<>();

    public record PalaceAnchor(int startChunkX, int baseY, int startChunkZ) {
        public int centerX() {
            return (startChunkX << 4) + 8;
        }

        public int centerZ() {
            return (startChunkZ << 4) + 8;
        }

        public BlockPos frontEntranceTpPos() {
            return new BlockPos(centerX(), baseY + 1, centerZ() + 158);
        }

        public BlockPos miniBossArenaSpawnPos() {
            return new BlockPos(
                    centerX() + PalacioNacionalPiece.MINI_BOSS_SPAWN_REL_X,
                    baseY + PalacioNacionalPiece.MINI_BOSS_SPAWN_REL_Y,
                    centerZ() + PalacioNacionalPiece.MINI_BOSS_SPAWN_REL_Z
            );
        }

        public BlockPos finalBossArenaSpawnPos() {
            return new BlockPos(
                    centerX() + PalacioNacionalPiece.FINAL_BOSS_SPAWN_REL_X,
                    baseY + PalacioNacionalPiece.FINAL_BOSS_SPAWN_REL_Y,
                    centerZ() + PalacioNacionalPiece.FINAL_BOSS_SPAWN_REL_Z
            );
        }
    }

    private record SiteEval(ChunkPos chunk, int medianY, int reliefScore) {}

    public PalacioNacionalStructure(Config config) {
        super(config);
    }

    public static boolean isValidPlacementStartChunk(long seed, int chunkX, int chunkZ) {
        ChunkPos expected = PLACEMENT.getStartChunk(seed, chunkX, chunkZ);
        return expected.x == chunkX && expected.z == chunkZ;
    }

    public static void clearCaches() {
        SEED_ANCHOR_CACHE.clear();
    }

    public static void onOverworldLoaded(ServerWorld overworld) {
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        boolean needsRecalc = false;
        synchronized (ANCHOR_LOCK) {
            PalacioNacionalWorldState state = PalacioNacionalWorldState.get(overworld);
            if (state.isInitialized()) {
                if (isValidPlacementStartChunk(overworld.getSeed(), state.getStartChunkX(), state.getStartChunkZ())) {
                    SEED_ANCHOR_CACHE.put(
                            overworld.getSeed(),
                            new PalaceAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ())
                    );
                } else {
                    SEED_ANCHOR_CACHE.remove(overworld.getSeed());
                    state.resetUpgradedChunks();
                    needsRecalc = true;
                }
            }
        }
        if (needsRecalc) {
            getOrResolveUniqueAnchor(overworld);
        }
    }

    @Nullable
    public static PalaceAnchor getOrResolveUniqueAnchor(ServerWorld overworld) {
        ServerWorld targetWorld = overworld.getServer().getOverworld();
        if (targetWorld == null) {
            return null;
        }
        return resolveUniqueAnchor(
                targetWorld.getSeed(),
                targetWorld,
                targetWorld.getChunkManager().getChunkGenerator(),
                targetWorld.getChunkManager().getChunkGenerator().getBiomeSource(),
                targetWorld.getChunkManager().getNoiseConfig(),
                targetWorld
        );
    }

    public static PalaceAnchor resolveUniqueAnchor(
            long seed,
            @Nullable ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        PalaceAnchor cached = SEED_ANCHOR_CACHE.get(seed);
        if (cached != null) {
            return cached;
        }

        synchronized (ANCHOR_LOCK) {
            cached = SEED_ANCHOR_CACHE.get(seed);
            if (cached != null) {
                return cached;
            }

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                PalacioNacionalWorldState state = PalacioNacionalWorldState.get(overworld);
                if (state.isInitialized()) {
                    if (isValidPlacementStartChunk(seed, state.getStartChunkX(), state.getStartChunkZ())) {
                        PalaceAnchor fromState = new PalaceAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
                        SEED_ANCHOR_CACHE.put(seed, fromState);
                        return fromState;
                    }
                    state.resetUpgradedChunks();
                }
            }

            CasaJoseTorresStructure.HouseAnchor joseAnchor = CasaJoseTorresStructure.resolveUniqueAnchor(
                    seed, overworld, chunkGenerator, biomeSource, noiseConfig, heightLimit
            );
            CasaBlancaStructure.WhiteHouseAnchor whiteHouseAnchor = CasaBlancaStructure.resolveUniqueAnchor(
                    seed, overworld, chunkGenerator, biomeSource, noiseConfig, heightLimit
            );

            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();
            int seaLevel = chunkGenerator.getSeaLevel();
            SiteEval chosenEval = null;

            // 1. Búsqueda principal extremadamente lejana (>= 5,500 bloques del spawn y >= 3,200 de las otras dungeons)
            for (int ring = 1; ring <= 5; ring++) {
                SiteEval bestInRing = null;
                for (int rx = -ring; rx <= ring; rx++) {
                    for (int rz = -ring; rz <= ring; rz++) {
                        if (Math.abs(rx) != ring && Math.abs(rz) != ring) {
                            continue;
                        }
                        ChunkPos candidate = PLACEMENT.getStartChunk(seed, rx * 360, rz * 360);
                        int cx = candidate.getCenterX();
                        int cz = candidate.getCenterZ();
                        long originDistSq = (long) cx * cx + (long) cz * cz;
                        if (originDistSq < 5500L * 5500L) {
                            continue;
                        }
                        if (isTooCloseToOtherDungeons(cx, cz, joseAnchor, whiteHouseAnchor)) {
                            continue;
                        }
                        if (!isValidLandSite(biomeSource, sampler, cx, cz)) {
                            continue;
                        }
                        SiteEval eval = evaluateSiteRelief(candidate, chunkGenerator, noiseConfig, heightLimit, seaLevel);
                        if (bestInRing == null || eval.reliefScore() < bestInRing.reliefScore()) {
                            bestInRing = eval;
                        }
                    }
                }
                if (bestInRing != null) {
                    chosenEval = bestInRing;
                    break;
                }
            }

            // 2. Respaldo determinista en anillos 1..7 con filtro terrestre relajado
            if (chosenEval == null) {
                for (int ring = 1; ring <= 7; ring++) {
                    SiteEval bestInRing = null;
                    for (int rx = -ring; rx <= ring; rx++) {
                        for (int rz = -ring; rz <= ring; rz++) {
                            if (Math.abs(rx) != ring && Math.abs(rz) != ring) {
                                continue;
                            }
                            ChunkPos candidate = PLACEMENT.getStartChunk(seed, rx * 360, rz * 360);
                            int cx = candidate.getCenterX();
                            int cz = candidate.getCenterZ();
                            long originDistSq = (long) cx * cx + (long) cz * cz;
                            if (originDistSq < 5500L * 5500L) {
                                continue;
                            }
                            if (isTooCloseToOtherDungeons(cx, cz, joseAnchor, whiteHouseAnchor)) {
                                continue;
                            }
                            if (!isFallbackLandSite(biomeSource, sampler, cx, cz)) {
                                continue;
                            }
                            SiteEval eval = evaluateSiteRelief(candidate, chunkGenerator, noiseConfig, heightLimit, seaLevel);
                            if (bestInRing == null || eval.reliefScore() < bestInRing.reliefScore()) {
                                bestInRing = eval;
                            }
                        }
                    }
                    if (bestInRing != null) {
                        chosenEval = bestInRing;
                        break;
                    }
                }
            }

            // 3. Último respaldo determinista garantizado lejos del origen y de las demás dungeons
            if (chosenEval == null) {
                int[][] fallbackCells = {{-720, -720}, {720, -720}, {-720, 720}, {720, 720}};
                for (int[] cell : fallbackCells) {
                    ChunkPos fb = PLACEMENT.getStartChunk(seed, cell[0], cell[1]);
                    if (!isTooCloseToOtherDungeons(fb.getCenterX(), fb.getCenterZ(), joseAnchor, whiteHouseAnchor)) {
                        chosenEval = evaluateSiteRelief(fb, chunkGenerator, noiseConfig, heightLimit, seaLevel);
                        break;
                    }
                }
                if (chosenEval == null) {
                    ChunkPos fb = PLACEMENT.getStartChunk(seed, -1080, 720);
                    chosenEval = evaluateSiteRelief(fb, chunkGenerator, noiseConfig, heightLimit, seaLevel);
                }
            }

            ChunkPos chosenChunk = chosenEval.chunk();
            int baseY = Math.min(Math.max(chosenEval.medianY(), Math.max(66, seaLevel + 4)), 108);

            PalaceAnchor resolved = new PalaceAnchor(chosenChunk.x, baseY, chosenChunk.z);
            SEED_ANCHOR_CACHE.put(seed, resolved);

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                PalacioNacionalWorldState.get(overworld).setAnchor(resolved.startChunkX(), resolved.baseY(), resolved.startChunkZ());
            }
            return resolved;
        }
    }

    private static SiteEval evaluateSiteRelief(
            ChunkPos candidate,
            ChunkGenerator chunkGenerator,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit,
            int seaLevel
    ) {
        int centerX = candidate.getCenterX();
        int centerZ = candidate.getCenterZ();
        int[] heights = new int[25];
        int idx = 0;
        int submergedCount = 0;

        for (int ox = -144; ox <= 144; ox += 72) {
            for (int oz = -144; oz <= 144; oz += 72) {
                int h = chunkGenerator.getHeightInGround(
                        centerX + ox,
                        centerZ + oz,
                        Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                        heightLimit,
                        noiseConfig
                );
                if (h <= seaLevel) {
                    submergedCount++;
                }
                heights[idx++] = h;
            }
        }
        Arrays.sort(heights);
        int medianY = heights[heights.length / 2];
        int iqrSpread = heights[20] - heights[4];
        int fullSpread = heights[24] - heights[0];
        int highPenalty = Math.max(0, medianY - 96) * 3;
        int score = iqrSpread * 4 + fullSpread + submergedCount * 25 + highPenalty;
        return new SiteEval(candidate, medianY, score);
    }

    private static boolean isTooCloseToOtherDungeons(
            int cx,
            int cz,
            @Nullable CasaJoseTorresStructure.HouseAnchor joseAnchor,
            @Nullable CasaBlancaStructure.WhiteHouseAnchor whiteHouseAnchor
    ) {
        if (joseAnchor != null) {
            long dx = cx - joseAnchor.centerX();
            long dz = cz - joseAnchor.centerZ();
            if (dx * dx + dz * dz < 3200L * 3200L) {
                return true;
            }
        }
        if (whiteHouseAnchor != null) {
            long dx = cx - whiteHouseAnchor.centerX();
            long dz = cz - whiteHouseAnchor.centerZ();
            if (dx * dx + dz * dz < 3200L * 3200L) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidLandSite(
            BiomeSource biomeSource,
            MultiNoiseUtil.MultiNoiseSampler sampler,
            int centerX,
            int centerZ
    ) {
        RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, 72 >> 2, centerZ >> 2, sampler);
        if (!CasaBlancaStructure.isTemperateLandBiome(centerBiome, new BlockPos(centerX, 72, centerZ))) {
            return false;
        }

        int validLandMatches = 0;
        int totalSamples = 0;
        for (int ox = -150; ox <= 150; ox += 75) {
            for (int oz = -150; oz <= 150; oz += 75) {
                totalSamples++;
                int sx = centerX + ox;
                int sz = centerZ + oz;
                RegistryEntry<Biome> b = biomeSource.getBiome(sx >> 2, 72 >> 2, sz >> 2, sampler);
                if (b.isIn(BiomeTags.IS_OCEAN) || b.isIn(BiomeTags.IS_DEEP_OCEAN)) {
                    return false;
                }
                if (CasaBlancaStructure.isTemperateLandBiome(b, new BlockPos(sx, 72, sz))) {
                    validLandMatches++;
                }
            }
        }
        return validLandMatches >= (totalSamples * 3) / 5;
    }

    private static boolean isFallbackLandSite(
            BiomeSource biomeSource,
            MultiNoiseUtil.MultiNoiseSampler sampler,
            int centerX,
            int centerZ
    ) {
        RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, 72 >> 2, centerZ >> 2, sampler);
        if (!CasaBlancaStructure.isTemperateLandBiome(centerBiome, new BlockPos(centerX, 72, centerZ))) {
            return false;
        }
        int validLandMatches = 0;
        for (int ox = -130; ox <= 130; ox += 130) {
            for (int oz = -130; oz <= 130; oz += 130) {
                int sx = centerX + ox;
                int sz = centerZ + oz;
                RegistryEntry<Biome> b = biomeSource.getBiome(sx >> 2, 72 >> 2, sz >> 2, sampler);
                if (CasaBlancaStructure.isTemperateLandBiome(b, new BlockPos(sx, 72, sz))) {
                    validLandMatches++;
                }
            }
        }
        return validLandMatches >= 5;
    }

    /**
     * Comprueba en O(1) si un bloque está dentro de los terrenos de "El Palacio Nacional de México"
     * (incluyendo el edificio completo, la Plaza de la Constitución / Zócalo y sus portales perimetrales).
     */
    public static boolean isPosInPalacioNacional(ServerWorldAccess worldAccess, BlockPos pos) {
        ServerWorld serverWorld = worldAccess.toServerWorld();
        if (serverWorld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        PalaceAnchor anchor = getOrResolveUniqueAnchor(serverWorld);
        if (anchor == null) {
            return false;
        }
        int dx = pos.getX() - anchor.centerX();
        int dz = pos.getZ() - anchor.centerZ();
        return Math.abs(dx) <= PalacioNacionalPiece.ESTATE_RADIUS
                && Math.abs(dz) <= PalacioNacionalPiece.ESTATE_RADIUS;
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        PalaceAnchor anchor = resolveUniqueAnchor(
                context.seed(),
                null,
                context.chunkGenerator(),
                context.biomeSource(),
                context.noiseConfig(),
                context.world()
        );

        if (chunkPos.x != anchor.startChunkX() || chunkPos.z != anchor.startChunkZ()) {
            return Optional.empty();
        }

        BlockPos pos = new BlockPos(anchor.centerX(), anchor.baseY(), anchor.centerZ());
        return Optional.of(new StructurePosition(pos, collector -> {
            collector.addPiece(new PalacioNacionalPiece(anchor.centerX(), anchor.baseY(), anchor.centerZ()));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.PALACIO_NACIONAL_TYPE;
    }
}
