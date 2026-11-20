package pruebano1.modid.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.RegistryKey;
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
import net.minecraft.world.biome.BiomeKeys;
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
 * Tercera Gran Dungeon del mod: "La Casa Blanca" (448x448 bloques, tamaño promedio entre Isla Penstein y Casa de José Torres).
 * - ÚNICA en el mundo (solo se genera 1 ejemplar en todo el Overworld).
 * - Muy difícil de encontrar (>= 4,500 bloques de distancia del origen y >= 3,000 bloques de la Casa de José Torres).
 * - Aparece en biomas terrestres firmes sin nieve ni océano, con nivelación y talud suave sin cortes bruscos.
 */
public class CasaBlancaStructure extends Structure {

    public static final MapCodec<CasaBlancaStructure> CODEC = createCodec(CasaBlancaStructure::new);

    // Misma configuración que worldgen/structure_set/casa_blanca.json
    public static final RandomSpreadStructurePlacement PLACEMENT = new RandomSpreadStructurePlacement(
            320,
            120,
            SpreadType.LINEAR,
            938471625
    );

    /** Radio de chunks (14 chunks = ±224 bloques = 448x448 bloques). */
    public static final int CHUNK_RADIUS = 14;

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<Long, WhiteHouseAnchor> SEED_ANCHOR_CACHE = new ConcurrentHashMap<>();

    public record WhiteHouseAnchor(int startChunkX, int baseY, int startChunkZ) {
        public int centerX() {
            return (startChunkX << 4) + 8;
        }

        public int centerZ() {
            return (startChunkZ << 4) + 8;
        }

        public BlockPos frontEntranceTpPos() {
            return new BlockPos(centerX(), baseY + 1, centerZ() + 168);
        }

        public BlockPos entranceMailboxPos() {
            return new BlockPos(
                    centerX() + CasaBlancaPiece.MAILBOX_REL_X,
                    baseY + 1,
                    centerZ() + CasaBlancaPiece.MAILBOX_REL_Z
            );
        }

        public BlockPos conferenceStageSpawnPos() {
            return new BlockPos(
                    centerX() + CasaBlancaPiece.CONFERENCE_SPAWN_REL_X,
                    baseY + CasaBlancaPiece.CONFERENCE_SPAWN_REL_Y,
                    centerZ() + CasaBlancaPiece.CONFERENCE_SPAWN_REL_Z
            );
        }
    }

    public CasaBlancaStructure(Config config) {
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
            CasaBlancaWorldState state = CasaBlancaWorldState.get(overworld);
            if (state.isInitialized()) {
                if (isValidPlacementStartChunk(overworld.getSeed(), state.getStartChunkX(), state.getStartChunkZ())) {
                    SEED_ANCHOR_CACHE.put(
                            overworld.getSeed(),
                            new WhiteHouseAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ())
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
    public static WhiteHouseAnchor getOrResolveUniqueAnchor(ServerWorld overworld) {
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

    public static WhiteHouseAnchor resolveUniqueAnchor(
            long seed,
            @Nullable ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        WhiteHouseAnchor cached = SEED_ANCHOR_CACHE.get(seed);
        if (cached != null) {
            return cached;
        }

        synchronized (ANCHOR_LOCK) {
            cached = SEED_ANCHOR_CACHE.get(seed);
            if (cached != null) {
                return cached;
            }

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                CasaBlancaWorldState state = CasaBlancaWorldState.get(overworld);
                if (state.isInitialized()) {
                    if (isValidPlacementStartChunk(seed, state.getStartChunkX(), state.getStartChunkZ())) {
                        WhiteHouseAnchor fromState = new WhiteHouseAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
                        SEED_ANCHOR_CACHE.put(seed, fromState);
                        return fromState;
                    }
                    state.resetUpgradedChunks();
                }
            }

            CasaJoseTorresStructure.HouseAnchor joseAnchor = CasaJoseTorresStructure.resolveUniqueAnchor(
                    seed, overworld, chunkGenerator, biomeSource, noiseConfig, heightLimit
            );

            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();
            int seaLevel = chunkGenerator.getSeaLevel();
            ChunkPos chosenChunk = null;

            // 1. Buscar en anillos lejanos (>= 4,500 bloques del spawn y >= 3,000 bloques de Casa de José Torres)
            outerSearch:
            for (int ring = 1; ring <= 4; ring++) {
                for (int rx = -ring; rx <= ring; rx++) {
                    for (int rz = -ring; rz <= ring; rz++) {
                        if (Math.abs(rx) != ring && Math.abs(rz) != ring) {
                            continue;
                        }
                        ChunkPos candidate = PLACEMENT.getStartChunk(seed, rx * 320, rz * 320);
                        int cx = candidate.getCenterX();
                        int cz = candidate.getCenterZ();
                        long originDistSq = (long) cx * cx + (long) cz * cz;
                        if (originDistSq < 4500L * 4500L) {
                            continue;
                        }
                        if (isTooCloseToJoseTorres(cx, cz, joseAnchor)) {
                            continue;
                        }
                        if (isValidLandSite(biomeSource, sampler, cx, cz)) {
                            chosenChunk = candidate;
                            break outerSearch;
                        }
                    }
                }
            }

            // 2. Respaldo determinista dentro de la rejilla PLACEMENT (ring = 1..6)
            if (chosenChunk == null) {
                outerFallback:
                for (int ring = 1; ring <= 6; ring++) {
                    for (int rx = -ring; rx <= ring; rx++) {
                        for (int rz = -ring; rz <= ring; rz++) {
                            if (Math.abs(rx) != ring && Math.abs(rz) != ring) {
                                continue;
                            }
                            ChunkPos candidate = PLACEMENT.getStartChunk(seed, rx * 320, rz * 320);
                            int cx = candidate.getCenterX();
                            int cz = candidate.getCenterZ();
                            long originDistSq = (long) cx * cx + (long) cz * cz;
                            if (originDistSq < 4500L * 4500L) {
                                continue;
                            }
                            if (isTooCloseToJoseTorres(cx, cz, joseAnchor)) {
                                continue;
                            }
                            if (isFallbackLandSite(biomeSource, sampler, cx, cz)) {
                                chosenChunk = candidate;
                                break outerFallback;
                            }
                        }
                    }
                }
            }

            // 3. Último respaldo sobre la rejilla PLACEMENT
            if (chosenChunk == null) {
                ChunkPos primaryFallback = PLACEMENT.getStartChunk(seed, -320, 320);
                chosenChunk = isTooCloseToJoseTorres(primaryFallback.getCenterX(), primaryFallback.getCenterZ(), joseAnchor)
                        ? PLACEMENT.getStartChunk(seed, 320, 320)
                        : primaryFallback;
            }

            int centerX = chosenChunk.getCenterX();
            int centerZ = chosenChunk.getCenterZ();

            int[] heights = new int[25];
            int idx = 0;
            for (int ox = -96; ox <= 96; ox += 48) {
                for (int oz = -96; oz <= 96; oz += 48) {
                    int h = chunkGenerator.getHeightInGround(
                            centerX + ox,
                            centerZ + oz,
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            heightLimit,
                            noiseConfig
                    );
                    heights[idx++] = h;
                }
            }
            Arrays.sort(heights);
            int medianY = Math.max(heights[heights.length / 2], seaLevel + 4);
            int baseY = Math.min(Math.max(medianY, 66), 112);

            WhiteHouseAnchor resolved = new WhiteHouseAnchor(chosenChunk.x, baseY, chosenChunk.z);
            SEED_ANCHOR_CACHE.put(seed, resolved);

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                CasaBlancaWorldState.get(overworld).setAnchor(resolved.startChunkX(), resolved.baseY(), resolved.startChunkZ());
            }
            return resolved;
        }
    }

    private static boolean isTooCloseToJoseTorres(int cx, int cz, @Nullable CasaJoseTorresStructure.HouseAnchor joseAnchor) {
        if (joseAnchor == null) {
            return false;
        }
        long dx = cx - joseAnchor.centerX();
        long dz = cz - joseAnchor.centerZ();
        return dx * dx + dz * dz < 3000L * 3000L;
    }

    public static boolean isValidLandSite(
            BiomeSource biomeSource,
            MultiNoiseUtil.MultiNoiseSampler sampler,
            int centerX,
            int centerZ
    ) {
        RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, 72 >> 2, centerZ >> 2, sampler);
        if (!isTemperateLandBiome(centerBiome, new BlockPos(centerX, 72, centerZ))) {
            return false;
        }

        int validLandMatches = 0;
        int totalSamples = 0;
        for (int ox = -140; ox <= 140; ox += 70) {
            for (int oz = -140; oz <= 140; oz += 70) {
                totalSamples++;
                int sx = centerX + ox;
                int sz = centerZ + oz;
                RegistryEntry<Biome> b = biomeSource.getBiome(sx >> 2, 72 >> 2, sz >> 2, sampler);
                if (b.isIn(BiomeTags.IS_OCEAN) || b.isIn(BiomeTags.IS_DEEP_OCEAN)) {
                    return false;
                }
                if (isTemperateLandBiome(b, new BlockPos(sx, 72, sz))) {
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
        if (!isTemperateLandBiome(centerBiome, new BlockPos(centerX, 72, centerZ))) {
            return false;
        }
        int validLandMatches = 0;
        for (int ox = -120; ox <= 120; ox += 120) {
            for (int oz = -120; oz <= 120; oz += 120) {
                int sx = centerX + ox;
                int sz = centerZ + oz;
                RegistryEntry<Biome> b = biomeSource.getBiome(sx >> 2, 72 >> 2, sz >> 2, sampler);
                if (isTemperateLandBiome(b, new BlockPos(sx, 72, sz))) {
                    validLandMatches++;
                }
            }
        }
        return validLandMatches >= 5;
    }

    public static boolean isTemperateLandBiome(RegistryEntry<Biome> biomeEntry, BlockPos pos) {
        if (biomeEntry.isIn(BiomeTags.IS_OCEAN)
                || biomeEntry.isIn(BiomeTags.IS_DEEP_OCEAN)
                || biomeEntry.isIn(BiomeTags.IS_RIVER)
                || biomeEntry.isIn(BiomeTags.IS_BEACH)) {
            return false;
        }
        Optional<RegistryKey<Biome>> keyOpt = biomeEntry.getKey();
        if (keyOpt.isPresent()) {
            RegistryKey<Biome> key = keyOpt.get();
            if (key == BiomeKeys.SNOWY_PLAINS
                    || key == BiomeKeys.SNOWY_TAIGA
                    || key == BiomeKeys.ICE_SPIKES
                    || key == BiomeKeys.GROVE
                    || key == BiomeKeys.SNOWY_SLOPES
                    || key == BiomeKeys.FROZEN_PEAKS
                    || key == BiomeKeys.JAGGED_PEAKS
                    || key == BiomeKeys.SNOWY_BEACH) {
                return false;
            }
        }
        Biome biome = biomeEntry.value();
        return !biome.isCold(pos, 64);
    }

    /**
     * Comprueba en O(1) si un bloque está dentro de los terrenos de "La Casa Blanca".
     */
    public static boolean isPosInCasaBlanca(ServerWorldAccess worldAccess, BlockPos pos) {
        ServerWorld serverWorld = worldAccess.toServerWorld();
        if (serverWorld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        WhiteHouseAnchor anchor = getOrResolveUniqueAnchor(serverWorld);
        if (anchor == null) {
            return false;
        }
        int dx = pos.getX() - anchor.centerX();
        int dz = pos.getZ() - anchor.centerZ();
        return Math.abs(dx) <= CasaBlancaPiece.ESTATE_RADIUS
                && Math.abs(dz) <= CasaBlancaPiece.ESTATE_RADIUS;
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        WhiteHouseAnchor anchor = resolveUniqueAnchor(
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
            collector.addPiece(new CasaBlancaPiece(anchor.centerX(), anchor.baseY(), anchor.centerZ()));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.CASA_BLANCA_TYPE;
    }
}
