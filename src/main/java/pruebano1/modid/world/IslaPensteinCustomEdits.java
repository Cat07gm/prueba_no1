package pruebano1.modid.world;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import pruebano1.modid.Prueba_no1;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sistema que permite al jugador modificar cualquier construcción de la Isla Penstein dentro del juego
 * y guardar sus cambios con el comando "/islapenstein guardar", incluyendo el contenido de cofres,
 * barriles, carteles y tablas de botín, para que aparezcan automáticamente en cada nuevo spawneo de la dungeon.
 */
public class IslaPensteinCustomEdits {

    public static final int MIN_SAVE_Y = 60;
    public static final int MAX_SAVE_Y = 104;
    private static final int Y_COUNT = MAX_SAVE_Y - MIN_SAVE_Y + 1;

    // Mapa en memoria: clave "(relChunkX)_(relChunkZ)" -> datos comprimidos del chunk con bloques y BlockEntities (cofres, etc.)
    private static final Map<String, SavedChunkData> SAVED_CHUNKS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public record SavedBlockEntity(int relX, int y, int relZ, NbtCompound nbt) {}

    public record SavedChunkData(List<NbtCompound> paletteNbt, int[] blockIndices, List<SavedBlockEntity> blockEntities) {}

    private static String chunkKey(int relChunkX, int relChunkZ) {
        return relChunkX + "_" + relChunkZ;
    }

    public static synchronized void ensureLoaded() {
        if (initialized) {
            return;
        }
        initialized = true;
        reloadFromDisk();
    }

    public static synchronized int reloadFromDisk() {
        SAVED_CHUNKS.clear();
        try {
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "isla_penstein_custom.nbt");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("isla_penstein_custom.nbt");

            NbtCompound root = null;
            if (Files.exists(configPath)) {
                root = NbtIo.readCompressed(configPath, NbtSizeTracker.ofUnlimitedBytes());
            } else if (Files.exists(devResourcePath)) {
                root = NbtIo.readCompressed(devResourcePath, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (InputStream in = IslaPensteinCustomEdits.class.getResourceAsStream("/assets/" + Prueba_no1.MOD_ID + "/isla_penstein_custom.nbt")) {
                    if (in != null) {
                        root = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
                    }
                }
            }

            if (root != null && root.contains("Chunks", NbtElement.COMPOUND_TYPE)) {
                NbtCompound chunksTag = root.getCompound("Chunks");
                for (String key : chunksTag.getKeys()) {
                    NbtCompound cTag = chunksTag.getCompound(key);
                    NbtList palList = cTag.getList("Palette", NbtElement.COMPOUND_TYPE);
                    List<NbtCompound> palette = new ArrayList<>(palList.size());
                    for (int i = 0; i < palList.size(); i++) {
                        palette.add(palList.getCompound(i));
                    }
                    int[] indices = cTag.getIntArray("Blocks");

                    List<SavedBlockEntity> beList = new ArrayList<>();
                    if (cTag.contains("BlockEntities", NbtElement.LIST_TYPE)) {
                        NbtList beNbtList = cTag.getList("BlockEntities", NbtElement.COMPOUND_TYPE);
                        for (int i = 0; i < beNbtList.size(); i++) {
                            NbtCompound beCompound = beNbtList.getCompound(i);
                            beList.add(new SavedBlockEntity(
                                    beCompound.getInt("x"),
                                    beCompound.getInt("y"),
                                    beCompound.getInt("z"),
                                    beCompound.getCompound("data")
                            ));
                        }
                    }

                    if (indices.length == 16 * 16 * Y_COUNT) {
                        SAVED_CHUNKS.put(key, new SavedChunkData(palette, indices, beList));
                    }
                }
                Prueba_no1.LOGGER.info("Cargados " + SAVED_CHUNKS.size() + " chunks personalizados de Isla Penstein con inventarios.");
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al cargar isla_penstein_custom.nbt", e);
        }
        return SAVED_CHUNKS.size();
    }

    /**
     * Aplica los bloques y el contenido de cofres personalizados guardados por el usuario.
     */
    public static void applyCustomChunkIfPresent(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ
    ) {
        ensureLoaded();
        if (SAVED_CHUNKS.isEmpty()) {
            return;
        }

        int chunkX = chunkBox.getMinX() >> 4;
        int chunkZ = chunkBox.getMinZ() >> 4;
        int startChunkX = centerX >> 4;
        int startChunkZ = centerZ >> 4;
        int relChunkX = chunkX - startChunkX;
        int relChunkZ = chunkZ - startChunkZ;

        SavedChunkData data = SAVED_CHUNKS.get(chunkKey(relChunkX, relChunkZ));
        if (data == null) {
            return;
        }

        RegistryEntryLookup<Block> blockLookup = Registries.BLOCK;
        List<BlockState> decodedPalette = new ArrayList<>(data.paletteNbt().size());
        for (NbtCompound stateNbt : data.paletteNbt()) {
            decodedPalette.add(NbtHelper.toBlockState(blockLookup, stateNbt));
        }

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int idx = 0;

        // 1. Colocar los bloques
        for (int lx = 0; lx < 16; lx++) {
            int wx = baseX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = baseZ + lz;
                for (int y = MIN_SAVE_Y; y <= MAX_SAVE_Y; y++) {
                    int palIndex = data.blockIndices()[idx++];
                    if (palIndex >= 0 && palIndex < decodedPalette.size()) {
                        BlockState state = decodedPalette.get(palIndex);
                        pos.set(wx, y, wz);
                        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        // 2. Restaurar contenido de cofres, barriles, carteles, etc. (BlockEntities)
        if (data.blockEntities() != null) {
            for (SavedBlockEntity sbe : data.blockEntities()) {
                BlockPos targetPos = new BlockPos(baseX + sbe.relX(), sbe.y(), baseZ + sbe.relZ());
                BlockEntity be = world.getBlockEntity(targetPos);
                if (be != null) {
                    try {
                        // Clonar NBT y actualizar coordenadas exactas para que coincidan con la posición de la isla en este mundo
                        NbtCompound beData = sbe.nbt().copy();
                        beData.putInt("x", targetPos.getX());
                        beData.putInt("y", targetPos.getY());
                        beData.putInt("z", targetPos.getZ());
                        be.read(beData, world.toServerWorld().getRegistryManager());
                        be.markDirty();
                    } catch (Exception e) {
                        Prueba_no1.LOGGER.warn("No se pudo restaurar BlockEntity en " + targetPos, e);
                    }
                }
            }
        }
    }

    /**
     * Escanea el complejo de la Isla Penstein donde está parado el jugador y guarda bloques e inventarios de cofres.
     */
    public static int saveIslandFromPlayer(ServerPlayerEntity player, int chunkRadius) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();
        ChunkPos startChunk = IslaPensteinStructure.findIslandStartChunk(
                world.getSeed(),
                playerPos.getX() >> 4,
                playerPos.getZ() >> 4
        );

        if (startChunk == null || !IslaPensteinStructure.isValidIslandCenter(
                world.getSeed(),
                world.getChunkManager().getChunkGenerator(),
                world.getChunkManager().getChunkGenerator().getBiomeSource(),
                world.getChunkManager().getNoiseConfig(),
                world,
                startChunk
        )) {
            player.sendMessage(
                    Text.literal("❌ Debes estar dentro de la Isla Penstein para guardar los cambios.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        ensureLoaded();
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;
        int savedCount = 0;
        int savedChests = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                int relChunkX = cx - startChunk.x;
                int relChunkZ = cz - startChunk.z;
                if (Math.abs(relChunkX) > IslaPensteinStructure.CHUNK_RADIUS
                        || Math.abs(relChunkZ) > IslaPensteinStructure.CHUNK_RADIUS) {
                    continue;
                }

                List<BlockState> paletteStates = new ArrayList<>();
                List<NbtCompound> paletteNbt = new ArrayList<>();
                List<SavedBlockEntity> chunkBlockEntities = new ArrayList<>();
                int[] indices = new int[16 * 16 * Y_COUNT];
                int idx = 0;
                int baseX = cx << 4;
                int baseZ = cz << 4;

                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int y = MIN_SAVE_Y; y <= MAX_SAVE_Y; y++) {
                            pos.set(baseX + lx, y, baseZ + lz);
                            BlockState state = world.getBlockState(pos);

                            int palIdx = paletteStates.indexOf(state);
                            if (palIdx == -1) {
                                palIdx = paletteStates.size();
                                paletteStates.add(state);
                                paletteNbt.add(NbtHelper.fromBlockState(state));
                            }
                            indices[idx++] = palIdx;

                            // Si tiene inventario o datos (cofre, barril, dispensador, etc.), guardar su NBT completo
                            BlockEntity be = world.getBlockEntity(pos);
                            if (be != null) {
                                NbtCompound beTag = be.createNbtWithId(world.getRegistryManager());
                                chunkBlockEntities.add(new SavedBlockEntity(lx, y, lz, beTag));
                                savedChests++;
                            }
                        }
                    }
                }

                SAVED_CHUNKS.put(chunkKey(relChunkX, relChunkZ), new SavedChunkData(paletteNbt, indices, chunkBlockEntities));
                savedCount++;
            }
        }

        writeAllToDisk();
        final int finalSavedChests = savedChests;
        player.sendMessage(
                Text.literal("📦 Se guardaron " + finalSavedChests + " cofres/contenedores con sus inventarios y botín exactos.")
                        .formatted(Formatting.AQUA),
                false
        );
        return savedCount;
    }

    public static synchronized void clearAllSavedEdits() {
        SAVED_CHUNKS.clear();
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("isla_penstein_custom.nbt");
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "isla_penstein_custom.nbt");
            Files.deleteIfExists(configPath);
            Files.deleteIfExists(devResourcePath);
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al borrar isla_penstein_custom.nbt", e);
        }
    }

    private static synchronized void writeAllToDisk() {
        try {
            NbtCompound root = new NbtCompound();
            NbtCompound chunksTag = new NbtCompound();

            for (Map.Entry<String, SavedChunkData> entry : SAVED_CHUNKS.entrySet()) {
                NbtCompound cTag = new NbtCompound();
                NbtList palList = new NbtList();
                palList.addAll(entry.getValue().paletteNbt());
                cTag.put("Palette", palList);
                cTag.putIntArray("Blocks", entry.getValue().blockIndices());

                if (entry.getValue().blockEntities() != null && !entry.getValue().blockEntities().isEmpty()) {
                    NbtList beList = new NbtList();
                    for (SavedBlockEntity sbe : entry.getValue().blockEntities()) {
                        NbtCompound beCompound = new NbtCompound();
                        beCompound.putInt("x", sbe.relX());
                        beCompound.putInt("y", sbe.y());
                        beCompound.putInt("z", sbe.relZ());
                        beCompound.put("data", sbe.nbt());
                        beList.add(beCompound);
                    }
                    cTag.put("BlockEntities", beList);
                }

                chunksTag.put(entry.getKey(), cTag);
            }
            root.put("Chunks", chunksTag);

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("isla_penstein_custom.nbt");
            Files.createDirectories(configPath.getParent());
            NbtIo.writeCompressed(root, configPath);

            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "isla_penstein_custom.nbt");
            if (Files.exists(devResourcePath.getParent())) {
                NbtIo.writeCompressed(root, devResourcePath);
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al guardar isla_penstein_custom.nbt", e);
        }
    }
}
