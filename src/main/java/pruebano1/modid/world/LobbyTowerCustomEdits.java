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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.compat.WaystonesIntegration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda con "/lobby guardar" lo que decores en la torre del Lobby y lo reaplica en mundos nuevos.
 * Solo se guardan y se pegan las columnas dentro de la plaza circular: el terreno de alrededor
 * nunca entra en el archivo, así que no puede provocar cortes en otro mundo.
 */
public final class LobbyTowerCustomEdits {

    private static final String FILE_NAME = "lobby_tower_custom.nbt";
    private static final int RADIUS = LobbyTowerPiece.SITE_RADIUS;
    private static final int SIZE = RADIUS * 2 + 1;
    private static final int MIN_Y = LobbyTowerPiece.MIN_REL_Y;
    private static final int HEIGHT = LobbyTowerPiece.MAX_REL_Y - MIN_Y + 1;
    /** Celda que no se toca al aplicar (fuera de la plaza, o un waystone). */
    private static final int SKIP = -1;

    private record SavedBlockEntity(int dx, int relY, int dz, NbtCompound nbt) {
    }

    private record Saved(List<BlockState> palette, int[] blocks, List<SavedBlockEntity> blockEntities) {
    }

    private static volatile Saved saved = null;
    private static boolean initialized = false;

    private LobbyTowerCustomEdits() {
    }

    private static int index(int dx, int relY, int dz) {
        return ((dx + RADIUS) * SIZE + (dz + RADIUS)) * HEIGHT + (relY - MIN_Y);
    }

    private static Path devResourcePath() {
        Path parentSrc = Paths.get("..", "src", "main", "resources", "assets", Prueba_no1.MOD_ID, FILE_NAME);
        if (Files.exists(parentSrc.getParent())) {
            return parentSrc;
        }
        return Paths.get("src", "main", "resources", "assets", Prueba_no1.MOD_ID, FILE_NAME);
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static synchronized void ensureLoaded() {
        if (initialized) {
            return;
        }
        initialized = true;
        saved = null;
        try {
            NbtCompound root = null;
            if (Files.exists(configPath())) {
                root = NbtIo.readCompressed(configPath(), NbtSizeTracker.ofUnlimitedBytes());
            } else if (Files.exists(devResourcePath())) {
                root = NbtIo.readCompressed(devResourcePath(), NbtSizeTracker.ofUnlimitedBytes());
            } else {
                try (InputStream in = LobbyTowerCustomEdits.class.getResourceAsStream(
                        "/assets/" + Prueba_no1.MOD_ID + "/" + FILE_NAME)) {
                    if (in != null) {
                        root = NbtIo.readCompressed(in, NbtSizeTracker.ofUnlimitedBytes());
                    }
                }
            }
            if (root == null) {
                return;
            }

            int[] blocks = root.getIntArray("Blocks");
            if (blocks.length != SIZE * SIZE * HEIGHT) {
                Prueba_no1.LOGGER.warn("{} tiene un tamaño inesperado y se ignora.", FILE_NAME);
                return;
            }
            NbtList paletteNbt = root.getList("Palette", NbtElement.COMPOUND_TYPE);
            List<BlockState> palette = new ArrayList<>(paletteNbt.size());
            for (int i = 0; i < paletteNbt.size(); i++) {
                palette.add(NbtHelper.toBlockState(Registries.BLOCK, paletteNbt.getCompound(i)));
            }
            List<SavedBlockEntity> blockEntities = new ArrayList<>();
            NbtList beNbt = root.getList("BlockEntities", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < beNbt.size(); i++) {
                NbtCompound tag = beNbt.getCompound(i);
                blockEntities.add(new SavedBlockEntity(
                        tag.getInt("dx"), tag.getInt("relY"), tag.getInt("dz"), tag.getCompound("data")
                ));
            }
            saved = new Saved(palette, blocks, blockEntities);
            Prueba_no1.LOGGER.info("Cargadas las ediciones guardadas de la torre del Lobby.");
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al cargar " + FILE_NAME, e);
        }
    }

    public static void applyIfPresent(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        ensureLoaded();
        Saved data = saved;
        if (data == null) {
            return;
        }

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int wx = chunkBox.getMinX(); wx <= chunkBox.getMaxX(); wx++) {
            int dx = wx - centerX;
            for (int wz = chunkBox.getMinZ(); wz <= chunkBox.getMaxZ(); wz++) {
                int dz = wz - centerZ;
                if (!LobbyTowerPiece.isInsideSite(dx, dz)) {
                    continue;
                }
                int idx = index(dx, MIN_Y, dz);
                for (int h = 0; h < HEIGHT; h++) {
                    int palIndex = data.blocks()[idx + h];
                    if (palIndex >= 0 && palIndex < data.palette().size()) {
                        pos.set(wx, baseY + MIN_Y + h, wz);
                        world.setBlockState(pos, data.palette().get(palIndex), Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }

        for (SavedBlockEntity sbe : data.blockEntities()) {
            BlockPos target = new BlockPos(centerX + sbe.dx(), baseY + sbe.relY(), centerZ + sbe.dz());
            if (target.getX() < chunkBox.getMinX() || target.getX() > chunkBox.getMaxX()
                    || target.getZ() < chunkBox.getMinZ() || target.getZ() > chunkBox.getMaxZ()) {
                continue;
            }
            BlockEntity be = world.getBlockEntity(target);
            if (be == null) {
                continue;
            }
            try {
                NbtCompound beData = sbe.nbt().copy();
                beData.putInt("x", target.getX());
                beData.putInt("y", target.getY());
                beData.putInt("z", target.getZ());
                be.read(beData, world.toServerWorld().getRegistryManager());
                be.markDirty();
            } catch (Exception e) {
                Prueba_no1.LOGGER.warn("No se pudo restaurar BlockEntity del lobby en " + target, e);
            }
        }
    }

    public static int saveFromPlayer(ServerPlayerEntity player) {
        ServerWorld world = player.getServer().getOverworld();
        LobbyTowerManager.LobbyAnchor anchor = LobbyTowerManager.getAnchor(player.getServer());
        if (anchor == null) {
            player.sendMessage(Text.literal("❌ No hay torre del Lobby en este mundo.").formatted(Formatting.RED), false);
            return 0;
        }

        List<BlockState> palette = new ArrayList<>();
        List<NbtCompound> paletteNbt = new ArrayList<>();
        List<SavedBlockEntity> blockEntities = new ArrayList<>();
        int[] blocks = new int[SIZE * SIZE * HEIGHT];
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                boolean inside = LobbyTowerPiece.isInsideSite(dx, dz);
                int idx = index(dx, MIN_Y, dz);
                for (int h = 0; h < HEIGHT; h++) {
                    if (!inside) {
                        blocks[idx + h] = SKIP;
                        continue;
                    }
                    pos.set(anchor.centerX() + dx, anchor.baseY() + MIN_Y + h, anchor.centerZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    // El waystone se crea aparte en cada mundo: copiar su bloque lo dejaría sin registrar
                    if (WaystonesIntegration.isWaystoneBlock(state.getBlock())) {
                        blocks[idx + h] = SKIP;
                        continue;
                    }
                    int palIdx = palette.indexOf(state);
                    if (palIdx == -1) {
                        palIdx = palette.size();
                        palette.add(state);
                        paletteNbt.add(NbtHelper.fromBlockState(state));
                    }
                    blocks[idx + h] = palIdx;

                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        blockEntities.add(new SavedBlockEntity(
                                dx, MIN_Y + h, dz, be.createNbtWithId(world.getRegistryManager())
                        ));
                    }
                }
            }
        }

        try {
            NbtCompound root = new NbtCompound();
            NbtList palList = new NbtList();
            palList.addAll(paletteNbt);
            root.put("Palette", palList);
            root.putIntArray("Blocks", blocks);
            NbtList beList = new NbtList();
            for (SavedBlockEntity sbe : blockEntities) {
                NbtCompound tag = new NbtCompound();
                tag.putInt("dx", sbe.dx());
                tag.putInt("relY", sbe.relY());
                tag.putInt("dz", sbe.dz());
                tag.put("data", sbe.nbt());
                beList.add(tag);
            }
            root.put("BlockEntities", beList);

            Files.createDirectories(configPath().getParent());
            NbtIo.writeCompressed(root, configPath());
            Path dev = devResourcePath();
            if (Files.exists(dev.getParent())) {
                NbtIo.writeCompressed(root, dev);
            }
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al guardar " + FILE_NAME, e);
            player.sendMessage(Text.literal("❌ No se pudo guardar la torre del Lobby.").formatted(Formatting.RED), false);
            return 0;
        }

        saved = new Saved(palette, blocks, blockEntities);
        initialized = true;
        player.sendMessage(
                Text.literal("🗼 Torre del Lobby guardada (" + blockEntities.size() + " elementos especiales). Se aplicará en mundos nuevos.")
                        .formatted(Formatting.AQUA),
                false
        );
        return 1;
    }

    public static synchronized void clearAllSavedEdits() {
        saved = null;
        try {
            Files.deleteIfExists(configPath());
            Files.deleteIfExists(devResourcePath());
        } catch (Exception e) {
            Prueba_no1.LOGGER.error("Error al borrar " + FILE_NAME, e);
        }
    }
}
