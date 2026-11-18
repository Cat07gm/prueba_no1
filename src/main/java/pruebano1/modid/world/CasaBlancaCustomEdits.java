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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permite personalizar construcciones dentro de "La Casa Blanca" y guardar los cambios
 * con "/casablanca guardar", usando alturas relativas a baseY.
 */
public class CasaBlancaCustomEdits {

    public static final int MIN_REL_Y = -8;
    public static final int MAX_REL_Y = 44;
    private static final int Y_COUNT = MAX_REL_Y - MIN_REL_Y + 1;

    private static final Map<String, SavedChunkData> SAVED_CHUNKS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public record SavedBlockEntity(int relX, int relY, int relZ, NbtCompound nbt) {}

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
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "casa_blanca_custom.nbt");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_blanca_custom.nbt");

            NbtCompound root = null;
            if (Files.exists(configPath)) {
                root = NbtIo.readCompressed(configPath, NbtSizeTracker.ofUnlimitedBytes());
            } else if (Files.exists(devResourcePath)) {
                root = NbtIo.readCompressed(devResourcePath, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (InputStream in = CasaBlancaCustomEdits.class.getResourceAsStream("/assets/" + Prueba_no1.MOD_ID + "/casa_blanca_custom.nbt")) {
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
                                    beCompound.getInt("ry"),
                                    beCompound.getInt("z"),
                                    beCompound.getCompound("data")
                            ));
                        }
                    }

                    if (indices.length == 16 * 16 * Y_COUNT) {
                        SAVED_CHUNKS.put(key, new SavedChunkData(palette, indices, beList));
                    }
                }
                Prueba_no1.LOGGER.info("Cargados " + SAVED_CHUNKS.size() + " chunks personalizados de La Casa Blanca.");
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al cargar casa_blanca_custom.nbt", e);
        }
        return SAVED_CHUNKS.size();
    }

    public static void applyCustomChunkIfPresent(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
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

        for (int lx = 0; lx < 16; lx++) {
            int wx = baseX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int wz = baseZ + lz;
                for (int ry = MIN_REL_Y; ry <= MAX_REL_Y; ry++) {
                    int wy = baseY + ry;
                    int palIndex = data.blockIndices()[idx++];
                    if (palIndex >= 0 && palIndex < decodedPalette.size()) {
                        pos.set(wx, wy, wz);
                        world.setBlockState(pos, decodedPalette.get(palIndex), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        if (data.blockEntities() != null) {
            for (SavedBlockEntity savedBe : data.blockEntities()) {
                BlockPos bePos = new BlockPos(baseX + savedBe.relX(), baseY + savedBe.relY(), baseZ + savedBe.relZ());
                BlockState stateAtPos = world.getBlockState(bePos);
                NbtCompound nbtCopy = savedBe.nbt().copy();
                nbtCopy.putInt("x", bePos.getX());
                nbtCopy.putInt("y", bePos.getY());
                nbtCopy.putInt("z", bePos.getZ());

                BlockEntity existingBe = world.getBlockEntity(bePos);
                if (existingBe != null) {
                    existingBe.read(nbtCopy, world.getRegistryManager());
                    existingBe.markDirty();
                } else {
                    BlockEntity createdBe = BlockEntity.createFromNbt(bePos, stateAtPos, nbtCopy, world.getRegistryManager());
                    if (createdBe != null) {
                        world.getChunk(bePos).setBlockEntity(createdBe);
                    }
                }
            }
        }
    }

    public static synchronized int saveHouseFromPlayer(ServerPlayerEntity player, int chunkRadius) {
        ensureLoaded();
        ServerWorld world = player.getServerWorld();
        CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.getOrResolveUniqueAnchor(world);
        if (anchor == null) {
            player.sendMessage(
                    Text.literal("❌ No se pudo resolver la ubicación de La Casa Blanca.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        int playerChunkX = player.getBlockPos().getX() >> 4;
        int playerChunkZ = player.getBlockPos().getZ() >> 4;
        if (Math.abs(playerChunkX - anchor.startChunkX()) > CasaBlancaStructure.CHUNK_RADIUS + 2
                || Math.abs(playerChunkZ - anchor.startChunkZ()) > CasaBlancaStructure.CHUNK_RADIUS + 2) {
            player.sendMessage(
                    Text.literal("❌ Debes estar dentro de La Casa Blanca para guardar cambios.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        ChunkPos centerChunk = new ChunkPos(player.getBlockPos());
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int savedCount = 0;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerChunk.x + dx;
                int cz = centerChunk.z + dz;
                int relChunkX = cx - anchor.startChunkX();
                int relChunkZ = cz - anchor.startChunkZ();

                if (Math.abs(relChunkX) > CasaBlancaStructure.CHUNK_RADIUS
                        || Math.abs(relChunkZ) > CasaBlancaStructure.CHUNK_RADIUS) {
                    continue;
                }

                List<NbtCompound> palette = new ArrayList<>();
                Map<BlockState, Integer> stateToIndex = new HashMap<>();
                int[] indices = new int[16 * 16 * Y_COUNT];
                List<SavedBlockEntity> blockEntities = new ArrayList<>();
                int idx = 0;

                int baseX = cx << 4;
                int baseZ = cz << 4;

                for (int lx = 0; lx < 16; lx++) {
                    int wx = baseX + lx;
                    for (int lz = 0; lz < 16; lz++) {
                        int wz = baseZ + lz;
                        for (int ry = MIN_REL_Y; ry <= MAX_REL_Y; ry++) {
                            int wy = anchor.baseY() + ry;
                            pos.set(wx, wy, wz);
                            BlockState state = world.getBlockState(pos);
                            Integer palIdx = stateToIndex.get(state);
                            if (palIdx == null) {
                                palIdx = palette.size();
                                stateToIndex.put(state, palIdx);
                                palette.add(NbtHelper.fromBlockState(state));
                            }
                            indices[idx++] = palIdx;

                            BlockEntity be = world.getBlockEntity(pos);
                            if (be != null) {
                                NbtCompound beNbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                                blockEntities.add(new SavedBlockEntity(lx, ry, lz, beNbt));
                            }
                        }
                    }
                }

                SAVED_CHUNKS.put(chunkKey(relChunkX, relChunkZ), new SavedChunkData(palette, indices, blockEntities));
                savedCount++;
            }
        }

        writeToDisk();
        return savedCount;
    }

    public static synchronized void clearAllSavedEdits() {
        SAVED_CHUNKS.clear();
        try {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_blanca_custom.nbt");
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "casa_blanca_custom.nbt");
            Files.deleteIfExists(configPath);
            Files.deleteIfExists(devResourcePath);
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al limpiar casa_blanca_custom.nbt", e);
        }
    }

    private static void writeToDisk() {
        try {
            NbtCompound root = new NbtCompound();
            NbtCompound chunksTag = new NbtCompound();

            for (Map.Entry<String, SavedChunkData> entry : SAVED_CHUNKS.entrySet()) {
                NbtCompound cTag = new NbtCompound();
                NbtList palList = new NbtList();
                palList.addAll(entry.getValue().paletteNbt());
                cTag.put("Palette", palList);
                cTag.putIntArray("Blocks", entry.getValue().blockIndices());

                NbtList beList = new NbtList();
                if (entry.getValue().blockEntities() != null) {
                    for (SavedBlockEntity savedBe : entry.getValue().blockEntities()) {
                        NbtCompound beTag = new NbtCompound();
                        beTag.putInt("x", savedBe.relX());
                        beTag.putInt("ry", savedBe.relY());
                        beTag.putInt("z", savedBe.relZ());
                        beTag.put("data", savedBe.nbt());
                        beList.add(beTag);
                    }
                }
                cTag.put("BlockEntities", beList);
                chunksTag.put(entry.getKey(), cTag);
            }
            root.put("Chunks", chunksTag);

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_blanca_custom.nbt");
            Files.createDirectories(configPath.getParent());
            NbtIo.writeCompressed(root, configPath);

            Path devDir = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID);
            if (Files.exists(devDir)) {
                NbtIo.writeCompressed(root, devDir.resolve("casa_blanca_custom.nbt"));
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al guardar casa_blanca_custom.nbt", e);
        }
    }
}
