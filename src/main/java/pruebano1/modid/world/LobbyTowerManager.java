package pruebano1.modid.world;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.compat.WaystonesIntegration;
import pruebano1.modid.network.LobbyBoundsPayload;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordina la torre única del Lobby principal: ubicación en el spawn del mundo, colocación,
 * punto de aparición en la cima, límites protegidos y zona segura.
 */
public final class LobbyTowerManager {

    /** 3 chunks de radio cubren la plaza (radio 24) más el talud de transición al terreno. */
    public static final int CHUNK_RADIUS = 3;
    public static final String WAYSTONE_NAME = "Lobby Principal";

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<MinecraftServer, LobbyAnchor> SERVER_ANCHORS = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Set<Long>> GENERATED_CHUNKS_IN_SESSION = new ConcurrentHashMap<>();
    private static final Set<MinecraftServer> DISABLED_SERVERS = ConcurrentHashMap.newKeySet();

    private static volatile BlockBox clientProtectedBox = null;

    public record LobbyAnchor(int chunkX, int baseY, int chunkZ) {
        public int centerX() {
            return (chunkX << 4) + 8;
        }

        public int centerZ() {
            return (chunkZ << 4) + 8;
        }

        public BlockBox protectedBox() {
            int r = LobbyTowerPiece.SITE_RADIUS;
            return new BlockBox(
                    centerX() - r, baseY + LobbyTowerPiece.MIN_REL_Y, centerZ() - r,
                    centerX() + r, baseY + LobbyTowerPiece.MAX_REL_Y, centerZ() + r
            );
        }

        /** Sobre el techo, tres bloques al sur del waystone central. */
        public BlockPos spawnPos() {
            return new BlockPos(centerX(), baseY + LobbyTowerPiece.ROOF_Y + 1, centerZ() + 3);
        }

        public BlockPos waystonePos() {
            return new BlockPos(centerX(), baseY + LobbyTowerPiece.ROOF_Y + 1, centerZ());
        }
    }

    private LobbyTowerManager() {
    }

    public static BlockBox chunkBox(int chunkX, int chunkZ, int baseY) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        return new BlockBox(minX, baseY - 40, minZ, minX + 15, baseY + 100, minZ + 15);
    }

    public static boolean isDisabled(MinecraftServer server) {
        return DISABLED_SERVERS.contains(server);
    }

    /** ServerWorldEvents.LOAD: restaura la torre guardada o descarta mundos que ya existían sin lobby. */
    public static void onOverworldLoaded(ServerWorld overworld) {
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        MinecraftServer server = overworld.getServer();
        LobbyTowerWorldState state = LobbyTowerWorldState.get(overworld);
        if (state.isDisabled()) {
            DISABLED_SERVERS.add(server);
        } else if (state.isInitialized()) {
            SERVER_ANCHORS.put(server, new LobbyAnchor(state.getChunkX(), state.getBaseY(), state.getChunkZ()));
        } else if (overworld.getTime() > 0) {
            // Mundo creado antes de que existiera el lobby: no se pisa lo que el jugador tenga en el spawn.
            state.setDisabled(true);
            DISABLED_SERVERS.add(server);
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null || isDisabled(server)) {
            return;
        }
        placeAndFinish(overworld);
    }

    /** "/lobby generar": fuerza la torre en el spawn actual aunque el mundo ya existiera. */
    public static boolean forceGenerate(ServerWorld overworld) {
        MinecraftServer server = overworld.getServer();
        LobbyTowerWorldState state = LobbyTowerWorldState.get(overworld);
        if (state.isInitialized() && state.isPlaced()) {
            return false;
        }
        DISABLED_SERVERS.remove(server);
        state.setDisabled(false);
        placeAndFinish(overworld);
        LobbyBoundsPayload payload = getBoundsPayloadForServer(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
        return true;
    }

    private static void placeAndFinish(ServerWorld overworld) {
        MinecraftServer server = overworld.getServer();
        LobbyTowerWorldState state = LobbyTowerWorldState.get(overworld);
        LobbyAnchor anchor;
        if (state.isInitialized()) {
            anchor = new LobbyAnchor(state.getChunkX(), state.getBaseY(), state.getChunkZ());
            SERVER_ANCHORS.put(server, anchor);
        } else {
            anchor = resolveAnchorForGeneration(
                    overworld,
                    overworld.getChunkManager().getChunkGenerator(),
                    overworld.getChunkManager().getNoiseConfig(),
                    overworld
            );
            state.setAnchor(anchor.chunkX(), anchor.baseY(), anchor.chunkZ());
        }

        if (!state.isPlaced()) {
            Set<Long> alreadyBuilt = GENERATED_CHUNKS_IN_SESSION.get(server);
            int completed = 0;
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    int cx = anchor.chunkX() + dx;
                    int cz = anchor.chunkZ() + dz;
                    if (alreadyBuilt != null && alreadyBuilt.contains(ChunkPos.toLong(cx, cz))) {
                        continue;
                    }
                    overworld.getChunk(cx, cz);
                    LobbyTowerPiece.generateChunk(
                            overworld,
                            chunkBox(cx, cz, anchor.baseY()),
                            anchor.centerX(),
                            anchor.baseY(),
                            anchor.centerZ()
                    );
                    completed++;
                }
            }
            state.markPlaced();
            overworld.setSpawnPos(anchor.spawnPos(), 180.0F);
            Prueba_no1.LOGGER.info(
                    "Torre del Lobby principal fijada en ({}, {}, {}) (chunks completados al arranque: {}).",
                    anchor.centerX(), anchor.baseY(), anchor.centerZ(), completed
            );
        }

        ensureCenterpiece(overworld, anchor);
    }

    /** Waystone en el centro de la cima; sin el mod Waystones queda una magnetita decorativa. */
    private static void ensureCenterpiece(ServerWorld overworld, LobbyAnchor anchor) {
        BlockPos pos = anchor.waystonePos();
        overworld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        boolean placed = WaystonesIntegration.ensureSafeZoneEntranceWaystone(
                overworld, pos, Direction.SOUTH, WAYSTONE_NAME
        );
        if (!placed && overworld.getBlockState(pos).isAir()) {
            overworld.setBlockState(pos, Blocks.LODESTONE.getDefaultState());
        }
    }

    public static void clearServerCaches(MinecraftServer server) {
        SERVER_ANCHORS.remove(server);
        GENERATED_CHUNKS_IN_SESSION.remove(server);
        DISABLED_SERVERS.remove(server);
    }

    public static void recordGeneratedChunk(MinecraftServer server, int chunkX, int chunkZ) {
        if (server == null) {
            return;
        }
        GENERATED_CHUNKS_IN_SESSION
                .computeIfAbsent(server, s -> ConcurrentHashMap.newKeySet())
                .add(ChunkPos.toLong(chunkX, chunkZ));
    }

    /** Fija, una sola vez por mundo y de forma segura entre hilos, la torre en el chunk del spawn. */
    public static LobbyAnchor resolveAnchorForGeneration(
            ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        MinecraftServer server = overworld.getServer();
        LobbyAnchor cached = SERVER_ANCHORS.get(server);
        if (cached != null) {
            return cached;
        }
        synchronized (ANCHOR_LOCK) {
            cached = SERVER_ANCHORS.get(server);
            if (cached != null) {
                return cached;
            }

            BlockPos spawnPos = overworld.getSpawnPos();
            int chunkX = spawnPos.getX() >> 4;
            int chunkZ = spawnPos.getZ() >> 4;
            int centerX = (chunkX << 4) + 8;
            int centerZ = (chunkZ << 4) + 8;

            // La plaza se apoya en la altura mediana del terreno para mover la menor cantidad posible de tierra
            int[] heights = new int[49];
            int idx = 0;
            for (int ox = -24; ox <= 24; ox += 8) {
                for (int oz = -24; oz <= 24; oz += 8) {
                    heights[idx++] = chunkGenerator.getHeightInGround(
                            centerX + ox, centerZ + oz, Heightmap.Type.OCEAN_FLOOR_WG, heightLimit, noiseConfig
                    );
                }
            }
            Arrays.sort(heights);
            int baseY = Math.max(heights[heights.length / 2], chunkGenerator.getSeaLevel());
            baseY = Math.max(heightLimit.getBottomY() + 24, baseY);
            baseY = Math.min(heightLimit.getTopYInclusive() - LobbyTowerPiece.MAX_REL_Y - 4, baseY);

            LobbyAnchor anchor = new LobbyAnchor(chunkX, baseY, chunkZ);
            SERVER_ANCHORS.put(server, anchor);
            return anchor;
        }
    }

    @Nullable
    public static LobbyAnchor getAnchor(@Nullable MinecraftServer server) {
        if (server == null || isDisabled(server)) {
            return null;
        }
        LobbyAnchor cached = SERVER_ANCHORS.get(server);
        if (cached != null) {
            return cached;
        }
        ServerWorld overworld = server.getOverworld();
        if (overworld != null) {
            LobbyTowerWorldState state = LobbyTowerWorldState.get(overworld);
            if (state.isInitialized() && !state.isDisabled()) {
                LobbyAnchor restored = new LobbyAnchor(state.getChunkX(), state.getBaseY(), state.getChunkZ());
                SERVER_ANCHORS.put(server, restored);
                return restored;
            }
        }
        return null;
    }

    /** Toda la torre y su plaza: no se rompe ni se coloca nada y los jugadores no reciben daño. */
    public static boolean isProtected(@Nullable World world, @Nullable BlockPos pos) {
        if (world == null || pos == null || world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        if (world.isClient()) {
            BlockBox box = clientProtectedBox;
            return box != null && box.contains(pos);
        }
        LobbyAnchor anchor = getAnchor(world.getServer());
        return anchor != null && anchor.protectedBox().contains(pos);
    }

    public static void setClientBounds(LobbyBoundsPayload payload) {
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

    public static LobbyBoundsPayload getBoundsPayloadForServer(MinecraftServer server) {
        LobbyAnchor anchor = getAnchor(server);
        return anchor == null ? LobbyBoundsPayload.empty() : LobbyBoundsPayload.fromBox(anchor.protectedBox());
    }

    public static boolean teleportToTop(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        LobbyAnchor anchor = getAnchor(server);
        if (anchor == null || server == null || !LobbyTowerWorldState.get(server.getOverworld()).isPlaced()) {
            return false;
        }
        BlockPos pos = anchor.spawnPos();
        player.teleport(
                server.getOverworld(),
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                Set.of(), 180.0F, 0.0F, true
        );
        return true;
    }

    /** Primera vez que el jugador entra al mundo: aparece en la cima de la torre. */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        if (player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)) == 0) {
            teleportToTop(player);
        }
    }

    /** Reaparición sin cama/ancla propia: también en la cima. */
    public static void onPlayerRespawn(ServerPlayerEntity player, boolean alive) {
        if (!alive && player.getSpawnPointPosition() == null) {
            teleportToTop(player);
        }
    }
}
