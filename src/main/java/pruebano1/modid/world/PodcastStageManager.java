package pruebano1.modid.world;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordina la ubicación única y generación del "Escenario del Podcast"
 * (estructura y arena de combate del jefe final José Torres / Jhoseph towers)
 * con el bloque indestructible "Cassete de Podcast" en el centro.
 */
public final class PodcastStageManager {

    /** Radio de chunks (2 = 5x5 chunks = 80x80 bloques) suficiente para cubrir el radio de 23 bloques. */
    public static final int CHUNK_RADIUS = 2;

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<MinecraftServer, StageAnchor> SERVER_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Set<Long>> GENERATED_CHUNKS_IN_SESSION = new ConcurrentHashMap<>();

    public record StageAnchor(int startChunkX, int baseY, int startChunkZ) {
        public int centerX() {
            return (startChunkX << 4) + 8;
        }

        public int centerZ() {
            return (startChunkZ << 4) + 8;
        }

        public BlockPos cassettePos() {
            return new BlockPos(centerX(), baseY + 2, centerZ());
        }

        public BlockPos entranceTpPos() {
            return new BlockPos(centerX(), baseY + 1, centerZ() + 14);
        }

        public boolean isInsideStage(BlockPos pos) {
            int dx = pos.getX() - centerX();
            int dz = pos.getZ() - centerZ();
            int dy = pos.getY() - baseY;
            return (dx * dx + dz * dz) <= (PodcastStagePiece.TOTAL_RADIUS * PodcastStagePiece.TOTAL_RADIUS)
                    && dy >= -4 && dy <= 28;
        }
    }

    private PodcastStageManager() {
    }

    public static void onOverworldLoaded(ServerWorld overworld) {
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        synchronized (ANCHOR_LOCK) {
            PodcastStageWorldState state = PodcastStageWorldState.get(overworld);
            if (state.isInitialized()) {
                SERVER_ANCHORS.put(
                        overworld.getServer(),
                        new StageAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ())
                );
            }
        }
    }

    public static void ensurePlacedOnServerStart(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }

        PodcastStageWorldState state = PodcastStageWorldState.get(overworld);
        StageAnchor anchor;
        synchronized (ANCHOR_LOCK) {
            if (state.isInitialized()) {
                anchor = new StageAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
            } else {
                anchor = resolveAnchorForGeneration(
                        overworld,
                        overworld.getChunkManager().getChunkGenerator(),
                        overworld.getChunkManager().getChunkGenerator().getBiomeSource(),
                        overworld.getChunkManager().getNoiseConfig(),
                        overworld
                );
                state.setAnchor(anchor.startChunkX(), anchor.baseY(), anchor.startChunkZ());
            }
            SERVER_ANCHORS.put(server, anchor);
        }

        if (!state.isPlaced()) {
            Set<Long> alreadyBuilt = GENERATED_CHUNKS_IN_SESSION.get(server);
            int placedMissing = 0;

            for (int dxChunk = -CHUNK_RADIUS; dxChunk <= CHUNK_RADIUS; dxChunk++) {
                for (int dzChunk = -CHUNK_RADIUS; dzChunk <= CHUNK_RADIUS; dzChunk++) {
                    int cx = anchor.startChunkX() + dxChunk;
                    int cz = anchor.startChunkZ() + dzChunk;
                    long chunkKey = ChunkPos.toLong(cx, cz);

                    if (alreadyBuilt != null && alreadyBuilt.contains(chunkKey)) {
                        continue;
                    }

                    overworld.getChunk(cx, cz);
                    int minX = cx << 4;
                    int minZ = cz << 4;
                    BlockBox chunkBox = new BlockBox(
                            minX,
                            anchor.baseY() - 12,
                            minZ,
                            minX + 15,
                            anchor.baseY() + 28,
                            minZ + 15
                    );
                    PodcastStagePiece.generateChunk(
                            overworld,
                            chunkBox,
                            anchor.centerX(),
                            anchor.baseY(),
                            anchor.centerZ()
                    );
                    placedMissing++;
                }
            }

            state.markPlaced();
            Prueba_no1.LOGGER.info(
                    "Escenario del Podcast (Altar de José Torres) fijado en ({}, {}, {}) (chunks completados: {}).",
                    anchor.centerX(), anchor.baseY(), anchor.centerZ(), placedMissing
            );
        }
    }

    public static void clearServerCaches(MinecraftServer server) {
        SERVER_ANCHORS.remove(server);
        GENERATED_CHUNKS_IN_SESSION.remove(server);
    }

    public static void recordGeneratedChunk(MinecraftServer server, int chunkX, int chunkZ) {
        if (server == null) {
            return;
        }
        GENERATED_CHUNKS_IN_SESSION
                .computeIfAbsent(server, s -> ConcurrentHashMap.newKeySet())
                .add(ChunkPos.toLong(chunkX, chunkZ));
    }

    @Nullable
    public static StageAnchor getAnchor(@Nullable MinecraftServer server) {
        if (server == null) {
            return null;
        }
        StageAnchor cached = SERVER_ANCHORS.get(server);
        if (cached != null) {
            return cached;
        }
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            PodcastStageWorldState state = PodcastStageWorldState.get(overworld);
            if (state.isInitialized()) {
                StageAnchor restored = new StageAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
                SERVER_ANCHORS.put(server, restored);
                return restored;
            }
        }
        return null;
    }

    public static boolean isPosInPodcastStage(ServerWorld world, BlockPos pos) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        StageAnchor anchor = getAnchor(world.getServer());
        return anchor != null && anchor.isInsideStage(pos);
    }

    public static StageAnchor resolveAnchorForGeneration(
            ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        MinecraftServer server = overworld.getServer();
        StageAnchor cached = SERVER_ANCHORS.get(server);
        if (cached != null) {
            return cached;
        }

        synchronized (ANCHOR_LOCK) {
            cached = SERVER_ANCHORS.get(server);
            if (cached != null) {
                return cached;
            }

            BlockPos spawnPos = overworld.getSpawnPos();
            int spawnChunkX = spawnPos.getX() >> 4;
            int spawnChunkZ = spawnPos.getZ() >> 4;

            DrafteaCasinoManager.CasinoAnchor casinoAnchor = DrafteaCasinoManager.getAnchor(server);
            int seaLevel = chunkGenerator.getSeaLevel();
            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();

            int bestChunkX = spawnChunkX - 9;
            int bestChunkZ = spawnChunkZ + 6;
            int bestBaseY = Math.max(seaLevel + 2, 65);
            int bestScore = Integer.MAX_VALUE;

            // Buscar un terreno firme y plano a ~9-13 chunks del spawn (~144-210 bloques), alejado del Casino
            int[][] candidateOffsets = {
                    {-9, 5}, {-10, -4}, {9, -6}, {10, 5}, {-6, -9}, {6, 9},
                    {-11, 2}, {11, -2}, {-8, 8}, {8, -8}, {-12, -6}, {12, 6}
            };

            for (int[] offset : candidateOffsets) {
                int candChunkX = spawnChunkX + offset[0];
                int candChunkZ = spawnChunkZ + offset[1];

                if (casinoAnchor != null) {
                    int dCasinoX = candChunkX - casinoAnchor.startChunkX();
                    int dCasinoZ = candChunkZ - casinoAnchor.startChunkZ();
                    if (dCasinoX * dCasinoX + dCasinoZ * dCasinoZ < 64) {
                        continue; // Al menos 8 chunks de separación respecto al Casino Draftea
                    }
                }

                int centerX = (candChunkX << 4) + 8;
                int centerZ = (candChunkZ << 4) + 8;

                RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, (seaLevel + 8) >> 2, centerZ >> 2, sampler);
                boolean aquatic = centerBiome.isIn(BiomeTags.IS_OCEAN)
                        || centerBiome.isIn(BiomeTags.IS_DEEP_OCEAN)
                        || centerBiome.isIn(BiomeTags.IS_RIVER);

                int[] heights = new int[9];
                int idx = 0;
                int minH = Integer.MAX_VALUE;
                int maxH = Integer.MIN_VALUE;
                for (int ox = -16; ox <= 16; ox += 16) {
                    for (int oz = -16; oz <= 16; oz += 16) {
                        int h = chunkGenerator.getHeightInGround(
                                centerX + ox, centerZ + oz, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, heightLimit, noiseConfig
                        );
                        heights[idx++] = h;
                        minH = Math.min(minH, h);
                        maxH = Math.max(maxH, h);
                    }
                }
                Arrays.sort(heights);
                int medianH = Math.max(heights[4], seaLevel + 1);
                int slopePenalty = (maxH - minH) * 4;
                int waterPenalty = (aquatic || minH <= seaLevel) ? 500 : 0;
                int altitudePenalty = Math.max(0, medianH - (seaLevel + 32)) * 3;
                int totalScore = slopePenalty + waterPenalty + altitudePenalty;

                if (totalScore < bestScore) {
                    bestScore = totalScore;
                    bestChunkX = candChunkX;
                    bestChunkZ = candChunkZ;
                    bestBaseY = Math.min(Math.max(medianH, seaLevel + 1), heightLimit.getTopYInclusive() - 36);
                }
            }

            StageAnchor resolved = new StageAnchor(bestChunkX, bestBaseY, bestChunkZ);
            SERVER_ANCHORS.put(server, resolved);
            return resolved;
        }
    }
}
