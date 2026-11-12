package pruebano1.modid.world;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
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
 * Sistema equivalente a IslaPensteinCustomEdits para que puedas decorar o ajustar cualquier detalle
 * dentro o fuera del casino "Draftea Tony Diaz" en el juego y guardarlo permanentemente con
 * "/drafteacasino guardar".
 * Guarda alturas relativas (relY = worldY - baseY) y entidades de bloque (Drafteas, cofres, carteles, tocadiscos)
 * para que encajen exactamente sobre la altura del terreno de cualquier mundo nuevo.
 */
public final class DrafteaCasinoCustomEdits {

    public static final int MIN_REL_Y = -8;
    public static final int MAX_REL_Y = 44;
    private static final int Y_COUNT = MAX_REL_Y - MIN_REL_Y + 1;
    private static final String FILE_NAME = "draftea_casino_custom.nbt";

    private static final Map<String, SavedChunkData> SAVED_CHUNKS = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public record SavedBlockEntity(int relX, int relY, int relZ, NbtCompound nbt) {
    }

    public record SavedChunkData(List<NbtCompound> paletteNbt, int[] blockIndices, List<SavedBlockEntity> blockEntities) {
    }

    private DrafteaCasinoCustomEdits() {
    }

    private static String chunkKey(int relChunkX, int relChunkZ) {
        return relChunkX + "_" + relChunkZ;
    }

    private static Path resolveDevResourcePath() {
        Path parentSrc = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, FILE_NAME);
        if (Files.exists(parentSrc.getParent())) {
            return parentSrc;
        }
        return Paths.get("src", "main", "resources", "assets", Prueba_no1.MOD_ID, FILE_NAME);
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
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            Path devResourcePath = resolveDevResourcePath();

            NbtCompound root = null;
            if (Files.exists(configPath)) {
                root = NbtIo.readCompressed(configPath, NbtSizeTracker.ofUnlimitedBytes());
            } else if (Files.exists(devResourcePath)) {
                root = NbtIo.readCompressed(devResourcePath, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (InputStream in = DrafteaCasinoCustomEdits.class.getResourceAsStream("/assets/" + Prueba_no1.MOD_ID + "/" + FILE_NAME)) {
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
                                    beCompound.getInt("relY"),
                                    beCompound.getInt("z"),
                                    beCompound.getCompound("data")
                            ));
                        }
                    }

                    if (indices.length == 16 * 16 * Y_COUNT) {
                        SAVED_CHUNKS.put(key, new SavedChunkData(palette, indices, beList));
                    }
                }
                Prueba_no1.LOGGER.info("Cargados " + SAVED_CHUNKS.size() + " chunks personalizados de Draftea Tony Diaz.");
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al cargar " + FILE_NAME, e);
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
                for (int relY = MIN_REL_Y; relY <= MAX_REL_Y; relY++) {
                    int palIndex = data.blockIndices()[idx++];
                    if (palIndex >= 0 && palIndex < decodedPalette.size()) {
                        BlockState state = decodedPalette.get(palIndex);
                        pos.set(wx, baseY + relY, wz);
                        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        if (data.blockEntities() != null) {
            for (SavedBlockEntity sbe : data.blockEntities()) {
                BlockPos targetPos = new BlockPos(baseX + sbe.relX(), baseY + sbe.relY(), baseZ + sbe.relZ());
                BlockEntity be = world.getBlockEntity(targetPos);
                if (be != null) {
                    try {
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
     * Escanea el casino "Draftea Tony Diaz" y guarda todos los bloques y BlockEntities (máquinas Draftea,
     * carteles, cofres, estandartes, etc.) para futuros mundos.
     */
    public static int saveCasinoFromPlayer(ServerPlayerEntity player, int chunkRadius) {
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() != World.OVERWORLD) {
            player.sendMessage(
                    Text.literal("❌ Debes estar en el Overworld dentro del casino para guardar cambios.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        DrafteaCasinoManager.CasinoAnchor anchor = DrafteaCasinoManager.getAnchor(world.getServer());
        if (anchor == null) {
            player.sendMessage(
                    Text.literal("❌ No hay un casino registrado en este mundo.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        BlockPos playerPos = player.getBlockPos();
        int distSq = (playerPos.getX() - anchor.centerX()) * (playerPos.getX() - anchor.centerX())
                + (playerPos.getZ() - anchor.centerZ()) * (playerPos.getZ() - anchor.centerZ());
        if (distSq > 140 * 140) {
            player.sendMessage(
                    Text.literal("❌ Acércate al casino Draftea Tony Diaz (en " + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ() + ") para guardar los cambios.")
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }

        ensureLoaded();
        int radius = Math.max(1, Math.min(DrafteaCasinoManager.CHUNK_RADIUS, chunkRadius));
        int savedChunks = 0;
        int savedBlockEntities = 0;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int relChunkX = -radius; relChunkX <= radius; relChunkX++) {
            for (int relChunkZ = -radius; relChunkZ <= radius; relChunkZ++) {
                int cx = anchor.startChunkX() + relChunkX;
                int cz = anchor.startChunkZ() + relChunkZ;
                world.getChunk(cx, cz);

                List<BlockState> paletteStates = new ArrayList<>();
                List<NbtCompound> paletteNbt = new ArrayList<>();
                List<SavedBlockEntity> chunkBlockEntities = new ArrayList<>();
                int[] indices = new int[16 * 16 * Y_COUNT];
                int idx = 0;
                int baseX = cx << 4;
                int baseZ = cz << 4;

                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        for (int relY = MIN_REL_Y; relY <= MAX_REL_Y; relY++) {
                            pos.set(baseX + lx, anchor.baseY() + relY, baseZ + lz);
                            BlockState state = world.getBlockState(pos);

                            int palIdx = paletteStates.indexOf(state);
                            if (palIdx == -1) {
                                palIdx = paletteStates.size();
                                paletteStates.add(state);
                                paletteNbt.add(NbtHelper.fromBlockState(state));
                            }
                            indices[idx++] = palIdx;

                            BlockEntity be = world.getBlockEntity(pos);
                            if (be != null) {
                                NbtCompound beTag = be.createNbtWithId(world.getRegistryManager());
                                chunkBlockEntities.add(new SavedBlockEntity(lx, relY, lz, beTag));
                                savedBlockEntities++;
                            }
                        }
                    }
                }

                SAVED_CHUNKS.put(chunkKey(relChunkX, relChunkZ), new SavedChunkData(paletteNbt, indices, chunkBlockEntities));
                savedChunks++;
            }
        }

        writeAllToDisk();
        player.sendMessage(
                Text.literal("🎰 Se guardaron " + savedChunks + " chunks y " + savedBlockEntities + " elementos especiales (Drafteas, carteles, cofres) del casino.")
                        .formatted(Formatting.AQUA),
                false
        );
        return savedChunks;
    }

    public static synchronized void clearAllSavedEdits() {
        SAVED_CHUNKS.clear();
        try {
            Files.deleteIfExists(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
            Files.deleteIfExists(resolveDevResourcePath());
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al borrar " + FILE_NAME, e);
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
                        beCompound.putInt("relY", sbe.relY());
                        beCompound.putInt("z", sbe.relZ());
                        beCompound.put("data", sbe.nbt());
                        beList.add(beCompound);
                    }
                    cTag.put("BlockEntities", beList);
                }

                chunksTag.put(entry.getKey(), cTag);
            }
            root.put("Chunks", chunksTag);

            Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
            Files.createDirectories(configPath.getParent());
            NbtIo.writeCompressed(root, configPath);

            Path devResourcePath = resolveDevResourcePath();
            if (Files.exists(devResourcePath.getParent())) {
                NbtIo.writeCompressed(root, devResourcePath);
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al guardar " + FILE_NAME, e);
        }
    }
}
