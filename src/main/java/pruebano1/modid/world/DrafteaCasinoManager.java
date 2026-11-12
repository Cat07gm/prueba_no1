package pruebano1.modid.world;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.TonyCasinoNpcEntity;
import pruebano1.modid.entity.TonyFase1Entity;
import pruebano1.modid.entity.TonyFase2Entity;
import pruebano1.modid.entity.TonyFase3Entity;
import pruebano1.modid.network.CasinoBoundsPayload;

import java.util.List;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordina la ubicación única, generación sin cortes, sincronización de límites al cliente,
 * protección anti-destrucción y zona segura del casino "Draftea Tony Diaz".
 */
public final class DrafteaCasinoManager {

    /** Radio de chunks (4 = 9x9 chunks = 144x144 bloques) para cubrir estructura, escalinatas y ediciones guardadas. */
    public static final int CHUNK_RADIUS = 4;

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<MinecraftServer, CasinoAnchor> SERVER_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Set<Long>> GENERATED_CHUNKS_IN_SESSION = new ConcurrentHashMap<>();

    // Caché en cliente (actualizada por CasinoBoundsPayload) para evitar parpadeos en multijugador
    private static volatile BlockBox clientProtectedBox = null;

    public record CasinoAnchor(int startChunkX, int baseY, int startChunkZ) {
        public int centerX() {
            return (startChunkX << 4) + 8;
        }

        public int centerZ() {
            return (startChunkZ << 4) + 8;
        }

        public BlockBox protectedBox() {
            return DrafteaCasinoWorldState.computeProtectedBox(centerX(), baseY, centerZ());
        }

        public boolean isInsideSafeZone(BlockPos pos) {
            return DrafteaCasinoWorldState.isInsideSafeZone(centerX(), baseY, centerZ(), pos);
        }

        public BlockPos entranceTpPos() {
            return new BlockPos(centerX(), baseY + 1, centerZ() + 34);
        }
    }

    private DrafteaCasinoManager() {
    }

    /** Llamado desde ServerWorldEvents.LOAD para restaurar la ubicación ya guardada al abrir un mundo existente. */
    public static void onOverworldLoaded(ServerWorld overworld) {
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        synchronized (ANCHOR_LOCK) {
            DrafteaCasinoWorldState state = DrafteaCasinoWorldState.get(overworld);
            if (state.isInitialized()) {
                SERVER_ANCHORS.put(
                        overworld.getServer(),
                        new CasinoAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ())
                );
            }
        }
    }

    /**
     * Se ejecuta cuando el servidor termina de arrancar. Si el casino aún no fue colocado en este mundo
     * (por ejemplo, en mundos creados previamente o si algún chunk del borde quedó fuera del radio inicial),
     * completa los chunks faltantes exactamente una vez y persiste su ubicación.
     */
    public static void ensurePlacedOnServerStart(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return;
        }

        DrafteaCasinoWorldState state = DrafteaCasinoWorldState.get(overworld);
        CasinoAnchor anchor;
        synchronized (ANCHOR_LOCK) {
            if (state.isInitialized()) {
                anchor = new CasinoAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
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

                    // Forzar carga del chunk y generar su porción del casino en mundos ya existentes o bordes pendientes
                    overworld.getChunk(cx, cz);
                    int minX = cx << 4;
                    int minZ = cz << 4;
                    BlockBox chunkBox = new BlockBox(
                            minX,
                            anchor.baseY() - 18,
                            minZ,
                            minX + 15,
                            anchor.baseY() + 45,
                            minZ + 15
                    );
                    DrafteaCasinoPiece.generateChunkForCasino(
                            overworld,
                            chunkBox,
                            anchor.centerX(),
                            anchor.baseY(),
                            anchor.centerZ(),
                            overworld.getRandom()
                    );
                    placedMissing++;
                }
            }

            state.markPlaced();
            Prueba_no1.LOGGER.info(
                    "Casino único 'Draftea Tony Diaz' fijado en ({}, {}, {}) cerca del spawn (chunks completados al arranque: {}).",
                    anchor.centerX(), anchor.baseY(), anchor.centerZ(), placedMissing
            );
        }

        if (!state.areNpcsSpawned()) {
            int spawned = spawnCasinoNpcs(overworld, anchor, false);
            if (spawned > 0) {
                state.markNpcsSpawned();
            }
        }
    }

    public static void clearServerCaches(MinecraftServer server) {
        SERVER_ANCHORS.remove(server);
        GENERATED_CHUNKS_IN_SESSION.remove(server);
    }

    public static void setClientBounds(CasinoBoundsPayload payload) {
        if (payload == null || !payload.hasBounds()) {
            clientProtectedBox = null;
        } else {
            clientProtectedBox = new BlockBox(
                    payload.minX(), payload.minY(), payload.minZ(),
                    payload.maxX(), payload.maxY(), payload.maxZ()
            );
        }
    }

    public static void clearClientBounds() {
        clientProtectedBox = null;
    }

    /**
     * Cliente: ¿está esta posición dentro del edificio del casino (zona segura)?
     * Usa los límites recibidos por CasinoBoundsPayload para reconstruir el centro del casino.
     */
    public static boolean hasClientBounds() {
        return clientProtectedBox != null;
    }

    public static boolean isInsideCasinoClient(BlockPos pos) {
        BlockBox box = clientProtectedBox;
        if (box == null) {
            return false;
        }
        int centerX = box.getMinX() - DrafteaCasinoWorldState.PROTECTED_MIN_DX;
        int baseY = box.getMinY() - DrafteaCasinoWorldState.PROTECTED_MIN_REL_Y;
        int centerZ = box.getMinZ() - DrafteaCasinoWorldState.PROTECTED_MIN_DZ;
        return DrafteaCasinoWorldState.isInsideSafeZone(centerX, baseY, centerZ, pos);
    }

    public static CasinoBoundsPayload getBoundsPayloadForServer(MinecraftServer server) {
        CasinoAnchor anchor = getAnchor(server);
        if (anchor == null) {
            return CasinoBoundsPayload.empty();
        }
        return CasinoBoundsPayload.fromBox(anchor.protectedBox());
    }

    public static void recordGeneratedChunk(MinecraftServer server, int chunkX, int chunkZ) {
        if (server == null) {
            return;
        }
        GENERATED_CHUNKS_IN_SESSION
                .computeIfAbsent(server, s -> ConcurrentHashMap.newKeySet())
                .add(ChunkPos.toLong(chunkX, chunkZ));
    }

    /**
     * Resuelve de forma determinista y segura entre hilos el punto exacto donde nace el casino único
     * cerca del punto de aparición inicial del mundo.
     */
    public static CasinoAnchor resolveAnchorForGeneration(
            ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        MinecraftServer server = overworld.getServer();
        CasinoAnchor cached = SERVER_ANCHORS.get(server);
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

            // Priorizar ubicaciones al Norte del spawn (~176-208 bloques) para dejar amplio espacio al Lobby principal
            // sin perder visibilidad desde el inicio; alternativas en anillo de 11 a 15 chunks.
            int[][] candidateOffsets = {
                    {0, -11}, {1, -11}, {-1, -11},
                    {0, -12}, {2, -12}, {-2, -12},
                    {-11, 0}, {11, 0}, {0, 11},
                    {8, -8}, {-8, -8}, {8, 8}, {-8, 8},
                    {0, -13}, {-13, 0}, {13, 0}, {0, 13},
                    {0, -14}, {6, -12}, {-6, -12}, {-14, 0}, {14, 0}, {0, 14},
                    {0, -15}, {-15, 0}, {15, 0}, {0, 15}
            };

            CasinoAnchor bestAnchor = null;
            double bestScore = Double.MAX_VALUE;
            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();
            int seaLevel = chunkGenerator.getSeaLevel();

            for (int i = 0; i < candidateOffsets.length; i++) {
                int cx = spawnChunkX + candidateOffsets[i][0];
                int cz = spawnChunkZ + candidateOffsets[i][1];
                int centerX = (cx << 4) + 8;
                int centerZ = (cz << 4) + 8;

                int[] heights = new int[25];
                int hIdx = 0;
                int minH = Integer.MAX_VALUE;
                int maxH = Integer.MIN_VALUE;
                int waterOrOceanCount = 0;

                for (int ox = -36; ox <= 36; ox += 18) {
                    for (int oz = -32; oz <= 32; oz += 16) {
                        int sx = centerX + ox;
                        int sz = centerZ + oz;

                        RegistryEntry<Biome> biome = biomeSource.getBiome(sx >> 2, 64 >> 2, sz >> 2, sampler);
                        int surfaceY = chunkGenerator.getHeightInGround(
                                sx, sz, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, heightLimit, noiseConfig
                        );
                        int oceanFloorY = chunkGenerator.getHeightInGround(
                                sx, sz, Heightmap.Type.OCEAN_FLOOR_WG, heightLimit, noiseConfig
                        );

                        if (biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN)
                                || biome.isIn(BiomeTags.IS_RIVER) || oceanFloorY + 1 < surfaceY
                                || surfaceY < seaLevel) {
                            waterOrOceanCount++;
                        }

                        heights[hIdx++] = surfaceY;
                        if (surfaceY < minH) minH = surfaceY;
                        if (surfaceY > maxH) maxH = surfaceY;
                    }
                }

                Arrays.sort(heights);
                int medianH = heights[heights.length / 2];
                int slope = maxH - minH;

                double score = (waterOrOceanCount * 600.0)
                        + (slope * 12.0)
                        + (i * 1.5);
                if (medianH > 105) {
                    score += (medianH - 105) * 4.0;
                }

                if (score < bestScore) {
                    bestScore = score;
                    int baseY = Math.max(heightLimit.getBottomY() + 10, medianH + 1);
                    baseY = Math.min(heightLimit.getTopYInclusive() - 48, baseY);
                    bestAnchor = new CasinoAnchor(cx, baseY, cz);
                }

                // Aceptar rápido si la primera/segunda opción norte es tierra firme muy plana
                if (waterOrOceanCount == 0 && slope <= 4 && i <= 2) {
                    break;
                }
            }

            if (bestAnchor == null) {
                bestAnchor = new CasinoAnchor(spawnChunkX, Math.max(heightLimit.getBottomY() + 10, spawnPos.getY()), spawnChunkZ - 11);
            }

            SERVER_ANCHORS.put(server, bestAnchor);
            return bestAnchor;
        }
    }

    @Nullable
    public static CasinoAnchor getAnchor(@Nullable MinecraftServer server) {
        if (server == null) {
            return null;
        }
        CasinoAnchor cached = SERVER_ANCHORS.get(server);
        if (cached != null) {
            return cached;
        }
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            DrafteaCasinoWorldState state = DrafteaCasinoWorldState.get(overworld);
            if (state.isInitialized()) {
                CasinoAnchor restored = new CasinoAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
                SERVER_ANCHORS.put(server, restored);
                return restored;
            }
        }
        return null;
    }

    public static boolean isProtectedBlock(@Nullable World world, @Nullable BlockPos pos) {
        if (world == null || pos == null || world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        if (world.isClient()) {
            BlockBox box = clientProtectedBox;
            return box != null && box.contains(pos);
        }
        CasinoAnchor anchor = getAnchor(world.getServer());
        return anchor != null && anchor.protectedBox().contains(pos);
    }

    public static boolean isInSafeZone(@Nullable ServerWorld world, @Nullable BlockPos pos) {
        if (world == null || pos == null || world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        CasinoAnchor anchor = getAnchor(world.getServer());
        return anchor != null && anchor.isInsideSafeZone(pos);
    }

    /** Reaparece desde cero al personal y clientes Tony dentro del casino "Draftea Tony Diaz". */
    public static int respawnCasinoNpcs(ServerWorld overworld) {
        CasinoAnchor anchor = getAnchor(overworld.getServer());
        if (anchor == null) {
            return 0;
        }
        DrafteaCasinoWorldState state = DrafteaCasinoWorldState.get(overworld);
        int count = spawnCasinoNpcs(overworld, anchor, true);
        if (count > 0) {
            state.markNpcsSpawned();
        }
        return count;
    }

    private static int spawnCasinoNpcs(ServerWorld overworld, CasinoAnchor anchor, boolean clearExisting) {
        if (clearExisting) {
            for (int dxChunk = -CHUNK_RADIUS; dxChunk <= CHUNK_RADIUS; dxChunk++) {
                for (int dzChunk = -CHUNK_RADIUS; dzChunk <= CHUNK_RADIUS; dzChunk++) {
                    overworld.getChunk(anchor.startChunkX() + dxChunk, anchor.startChunkZ() + dzChunk);
                }
            }
            Box searchBox = Box.from(anchor.protectedBox()).expand(8.0);
            List<TonyCasinoNpcEntity> existing = overworld.getEntitiesByClass(
                    TonyCasinoNpcEntity.class, searchBox, entity -> true
            );
            for (TonyCasinoNpcEntity npc : existing) {
                npc.discard();
            }
        }

        int spawned = 0;

        // 1. TonyFase1 (Sudadera Verde): 1 Bartender fijo detrás de la barra Norte mirando al Sur
        spawned += spawnBartender(overworld, anchor, 0.5, 1.0, -25.5, 0.0F) ? 1 : 0;

        // 2. TonyFase3 (Sudadera Negra): 2 Guardias en la entrada principal bajo la marquesina
        spawned += spawnGuard(overworld, anchor, -2.5, 1.0, 25.5, 0.0F, TonyFase3Entity.POSE_CROSSED) ? 1 : 0;
        spawned += spawnGuard(overworld, anchor, 3.5, 1.0, 25.5, 0.0F, TonyFase3Entity.POSE_ALERT) ? 1 : 0;

        // 3. TonyFase3 (Sudadera Negra): 4 Guardias internos distribuidos estratégicamente
        spawned += spawnGuard(overworld, anchor, -17.5, 1.0, 18.5, -90.0F, TonyFase3Entity.POSE_ALERT) ? 1 : 0;
        spawned += spawnGuard(overworld, anchor, 18.5, 1.0, 14.5, 70.0F, TonyFase3Entity.POSE_HANDS_CLASPED) ? 1 : 0;
        spawned += spawnGuard(overworld, anchor, -15.5, 1.0, -18.5, 0.0F, TonyFase3Entity.POSE_CROSSED) ? 1 : 0;
        spawned += spawnGuard(overworld, anchor, 0.5, 10.0, -17.5, 0.0F, TonyFase3Entity.POSE_HANDS_CLASPED) ? 1 : 0;

        // 4. TonyFase2 (Sudadera Blanca): 7 Clientes (3 inicialmente frente a máquinas Draftea + 4 paseando)
        spawned += spawnSlotClient(overworld, anchor, -10, 1, 5, -10, 1, 3, Direction.SOUTH) ? 1 : 0;
        spawned += spawnSlotClient(overworld, anchor, 13, 1, 2, 11, 1, 2, Direction.EAST) ? 1 : 0;
        spawned += spawnSlotClient(overworld, anchor, -28, 1, 4, -28, 1, 2, Direction.SOUTH) ? 1 : 0;

        spawned += spawnRoamingClient(overworld, anchor, 4.5, 1.0, 12.5, 150.0F) ? 1 : 0;
        spawned += spawnRoamingClient(overworld, anchor, 0.5, 1.0, -2.5, 0.0F) ? 1 : 0;
        spawned += spawnRoamingClient(overworld, anchor, 19.5, 1.0, -6.5, 90.0F) ? 1 : 0;
        spawned += spawnRoamingClient(overworld, anchor, -27.5, 10.0, -1.5, 0.0F) ? 1 : 0;

        return spawned;
    }

    private static boolean spawnBartender(
            ServerWorld world,
            CasinoAnchor anchor,
            double dx,
            double dy,
            double dz,
            float yaw
    ) {
        TonyFase1Entity bartender = new TonyFase1Entity(ModEntities.TONY_FASE1, world);
        return placeCasinoNpc(world, anchor, bartender, dx, dy, dz, yaw);
    }

    private static boolean spawnGuard(
            ServerWorld world,
            CasinoAnchor anchor,
            double dx,
            double dy,
            double dz,
            float yaw,
            int pose
    ) {
        TonyFase3Entity guard = new TonyFase3Entity(ModEntities.TONY_FASE3, world);
        guard.setGuardPose(pose);
        return placeCasinoNpc(world, anchor, guard, dx, dy, dz, yaw);
    }

    private static boolean spawnRoamingClient(
            ServerWorld world,
            CasinoAnchor anchor,
            double dx,
            double dy,
            double dz,
            float yaw
    ) {
        TonyFase2Entity client = new TonyFase2Entity(ModEntities.TONY_FASE2, world);
        return placeCasinoNpc(world, anchor, client, dx, dy, dz, yaw);
    }

    private static boolean spawnSlotClient(
            ServerWorld world,
            CasinoAnchor anchor,
            int standDx,
            int relY,
            int standDz,
            int machineDx,
            int machineRelY,
            int machineDz,
            Direction facing
    ) {
        BlockPos standPos = new BlockPos(anchor.centerX() + standDx, anchor.baseY() + relY, anchor.centerZ() + standDz);
        BlockPos machinePos = new BlockPos(anchor.centerX() + machineDx, anchor.baseY() + machineRelY, anchor.centerZ() + machineDz);
        world.getChunk(standPos.getX() >> 4, standPos.getZ() >> 4);

        TonyFase2Entity client = new TonyFase2Entity(ModEntities.TONY_FASE2, world);
        client.assignInitialSlotSpot(standPos, machinePos, facing);
        return world.spawnEntity(client);
    }

    private static boolean placeCasinoNpc(
            ServerWorld world,
            CasinoAnchor anchor,
            TonyCasinoNpcEntity npc,
            double dx,
            double dy,
            double dz,
            float yaw
    ) {
        Vec3d worldPos = new Vec3d(anchor.centerX() + dx, anchor.baseY() + dy, anchor.centerZ() + dz);
        BlockPos blockPos = BlockPos.ofFloored(worldPos);
        world.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        npc.anchorAt(worldPos, yaw, true);
        return world.spawnEntity(npc);
    }
}
