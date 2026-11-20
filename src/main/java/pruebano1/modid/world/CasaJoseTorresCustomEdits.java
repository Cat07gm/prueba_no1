package pruebano1.modid.world;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
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
 * Permite modificar cualquier construcción dentro de "La Casa de José Torres" y guardar los cambios
 * con el comando "/casajosetorres guardar", incluyendo cofres, barriles, carteles y decoración,
 * usando alturas relativas a baseY para que funcione en cualquier mundo nevado.
 */
public class CasaJoseTorresCustomEdits {

    public static final int MIN_REL_Y = -8;
    public static final int MAX_REL_Y = 38;
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
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "casa_jose_torres_custom.nbt");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_jose_torres_custom.nbt");

            NbtCompound root = null;
            if (Files.exists(configPath)) {
                root = NbtIo.readCompressed(configPath, NbtSizeTracker.ofUnlimitedBytes());
            } else if (Files.exists(devResourcePath)) {
                root = NbtIo.readCompressed(devResourcePath, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (InputStream in = CasaJoseTorresCustomEdits.class.getResourceAsStream("/assets/" + Prueba_no1.MOD_ID + "/casa_jose_torres_custom.nbt")) {
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
                Prueba_no1.LOGGER.info("Cargados " + SAVED_CHUNKS.size() + " chunks personalizados de La Casa de José Torres.");
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al cargar casa_jose_torres_custom.nbt", e);
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
        BlockState airState = Blocks.AIR.getDefaultState();
        BlockState grassState = Blocks.GRASS_BLOCK.getDefaultState();
        int idx = 0;

        for (int lx = 0; lx < 16; lx++) {
            int wx = baseX + lx;
            int dx = wx - centerX;
            for (int lz = 0; lz < 16; lz++) {
                int wz = baseZ + lz;
                int dz = wz - centerZ;
                if (Math.abs(dx) > CasaJoseTorresPiece.FLAT_BOX_HALF || Math.abs(dz) > CasaJoseTorresPiece.FLAT_BOX_HALF) {
                    idx += Y_COUNT;
                    continue;
                }

                int ry1PalIdx = data.blockIndices()[idx + (1 - MIN_REL_Y)];
                boolean hadWildLogAtBase = ry1PalIdx >= 0 && ry1PalIdx < decodedPalette.size()
                        && isBakedWildLog(decodedPalette.get(ry1PalIdx), dx, dz);

                for (int ry = MIN_REL_Y; ry <= MAX_REL_Y; ry++) {
                    int wy = baseY + ry;
                    int palIndex = data.blockIndices()[idx++];
                    if (palIndex >= 0 && palIndex < decodedPalette.size()) {
                        BlockState state = decodedPalette.get(palIndex);
                        if (ry >= 1 && isBakedWildBiomeVegetation(state, dx, dz)) {
                            state = airState;
                        } else if (ry == 0 && hadWildLogAtBase && (state.isOf(Blocks.DIRT) || state.isOf(Blocks.PODZOL))) {
                            state = grassState;
                        }
                        pos.set(wx, wy, wz);
                        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        if (data.blockEntities() != null) {
            for (SavedBlockEntity savedBe : data.blockEntities()) {
                int beWx = baseX + savedBe.relX();
                int beWz = baseZ + savedBe.relZ();
                if (Math.abs(beWx - centerX) > CasaJoseTorresPiece.FLAT_BOX_HALF
                        || Math.abs(beWz - centerZ) > CasaJoseTorresPiece.FLAT_BOX_HALF) {
                    continue;
                }
                BlockPos bePos = new BlockPos(beWx, baseY + savedBe.relY(), beWz);
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

    private static boolean isBakedWildLog(BlockState st, int dx, int dz) {
        if (st.isOf(Blocks.DARK_OAK_LOG)) {
            return !(dx == -42 && (dz == 5 || dz == 6));
        }
        return st.isOf(Blocks.ACACIA_LOG)
                || st.isOf(Blocks.OAK_LOG)
                || st.isOf(Blocks.BIRCH_LOG)
                || st.isOf(Blocks.JUNGLE_LOG)
                || st.isOf(Blocks.MANGROVE_LOG)
                || st.isOf(Blocks.CHERRY_LOG)
                || st.isOf(Blocks.PALE_OAK_LOG);
    }

    private static boolean isBakedWildBiomeVegetation(BlockState st, int dx, int dz) {
        if (isBakedWildLog(st, dx, dz)) {
            return true;
        }
        if (st.getBlock() instanceof LeavesBlock) {
            return !st.contains(LeavesBlock.PERSISTENT) || !st.get(LeavesBlock.PERSISTENT);
        }
        return st.isOf(Blocks.VINE)
                || st.isOf(Blocks.CAVE_VINES)
                || st.isOf(Blocks.CAVE_VINES_PLANT)
                || st.isOf(Blocks.WEEPING_VINES)
                || st.isOf(Blocks.WEEPING_VINES_PLANT)
                || st.isOf(Blocks.TWISTING_VINES)
                || st.isOf(Blocks.TWISTING_VINES_PLANT)
                || st.isOf(Blocks.SHORT_GRASS)
                || st.isOf(Blocks.TALL_GRASS)
                || st.isOf(Blocks.FERN)
                || st.isOf(Blocks.LARGE_FERN)
                || st.isOf(Blocks.DEAD_BUSH)
                || st.isOf(Blocks.MOSS_CARPET)
                || st.isOf(Blocks.BAMBOO)
                || st.isOf(Blocks.BAMBOO_SAPLING)
                || st.isOf(Blocks.COCOA)
                || st.isOf(Blocks.MELON)
                || st.isOf(Blocks.PUMPKIN)
                || st.isOf(Blocks.BEE_NEST)
                || st.isOf(Blocks.POPPY)
                || st.isOf(Blocks.DANDELION);
    }

    public static synchronized int saveHouseFromPlayer(ServerPlayerEntity player, int chunkRadius) {
        ensureLoaded();
        ServerWorld world = player.getServerWorld();
        CasaJoseTorresStructure.HouseAnchor anchor = CasaJoseTorresStructure.getOrResolveUniqueAnchor(world);
        if (anchor == null) {
            player.sendMessage(
                    Text.literal("❌ No se pudo resolver la ubicación de La Casa de José Torres.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        int playerChunkX = player.getBlockPos().getX() >> 4;
        int playerChunkZ = player.getBlockPos().getZ() >> 4;
        if (Math.abs(playerChunkX - anchor.startChunkX()) > CasaJoseTorresStructure.CHUNK_RADIUS + 2
                || Math.abs(playerChunkZ - anchor.startChunkZ()) > CasaJoseTorresStructure.CHUNK_RADIUS + 2) {
            player.sendMessage(
                    Text.literal("❌ Debes estar dentro de La Casa de José Torres para guardar cambios.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        ChunkPos centerChunk = new ChunkPos( player.getBlockPos() );
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int savedCount = 0;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int cx = centerChunk.x + dx;
                int cz = centerChunk.z + dz;
                int relChunkX = cx - anchor.startChunkX();
                int relChunkZ = cz - anchor.startChunkZ();

                if (Math.abs(relChunkX) > CasaJoseTorresStructure.CHUNK_RADIUS
                        || Math.abs(relChunkZ) > CasaJoseTorresStructure.CHUNK_RADIUS) {
                    continue;
                }

                List<NbtCompound> palette = new ArrayList<>();
                Map<BlockState, Integer> stateToIndex = new java.util.HashMap<>();
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
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_jose_torres_custom.nbt");
            Path devResourcePath = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, "casa_jose_torres_custom.nbt");
            Files.deleteIfExists(configPath);
            Files.deleteIfExists(devResourcePath);
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al limpiar casa_jose_torres_custom.nbt", e);
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

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("casa_jose_torres_custom.nbt");
            Files.createDirectories(configPath.getParent());
            NbtIo.writeCompressed(root, configPath);

            Path devDir = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID);
            if (Files.exists(devDir)) {
                NbtIo.writeCompressed(root, devDir.resolve("casa_jose_torres_custom.nbt"));
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al guardar casa_jose_torres_custom.nbt", e);
        }
    }
}
