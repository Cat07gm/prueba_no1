package pruebano1.modid.casino;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.network.CasinoMusicControlPayload;
import pruebano1.modid.network.CasinoMusicStatePayload;

/**
 * Servidor: guarda qué canción eligió el DJ y a qué volumen, y lo manda a todos los jugadores.
 * Cada cliente decide si la escucha según esté dentro o fuera del casino (ver CasinoMusicClient).
 */
public final class CasinoMusicManager {

    public static final int DEFAULT_VOLUME = 70;
    /** Distancia máxima (en bloques) desde la que un jugador puede usar el DJ Controller. */
    private static final double MAX_CONTROL_DISTANCE = 8.0D;

    private CasinoMusicManager() {
    }

    /** Recibe una orden de la interfaz del DJ y la aplica si es válida. */
    public static void handleControl(ServerPlayerEntity player, CasinoMusicControlPayload payload) {
        ServerWorld world = player.getServerWorld();
        BlockPos pos = payload.controllerPos();
        // Solo se acepta si de verdad hay un DJ Controller ahí y el jugador está cerca
        if (!world.getBlockState(pos).isOf(ModBlocks.DJ_CONTROLLER)) {
            Prueba_no1.LOGGER.warn("[Casino DJ] Orden rechazada: en {} no hay un DJ Controller (hay {})",
                    pos, world.getBlockState(pos));
            return;
        }
        if (player.squaredDistanceTo(pos.toCenterPos()) > MAX_CONTROL_DISTANCE * MAX_CONTROL_DISTANCE) {
            Prueba_no1.LOGGER.warn("[Casino DJ] Orden rechazada: {} está demasiado lejos del DJ Controller",
                    player.getName().getString());
            return;
        }
        Prueba_no1.LOGGER.debug("[Casino DJ] {} -> acción {} canción {} volumen {}",
                player.getName().getString(), payload.action(), payload.songIndex(), payload.volume());

        MusicState state = MusicState.get(player.getServer());
        switch (payload.action()) {
            case CasinoMusicControlPayload.ACTION_PLAY -> {
                if (!CasinoSongs.isValid(payload.songIndex())) {
                    return;
                }
                state.songIndex = payload.songIndex();
                state.playSerial++;
            }
            case CasinoMusicControlPayload.ACTION_STOP -> {
                state.songIndex = -1;
                state.playSerial++;
            }
            case CasinoMusicControlPayload.ACTION_VOLUME -> state.volume = MathHelper.clamp(payload.volume(), 0, 100);
            default -> {
                return;
            }
        }
        state.markDirty();
        broadcast(player.getServer());
    }

    public static void broadcast(MinecraftServer server) {
        CasinoMusicStatePayload payload = createPayload(server);
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendTo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, createPayload(player.getServer()));
    }

    private static CasinoMusicStatePayload createPayload(MinecraftServer server) {
        MusicState state = MusicState.get(server);
        int song = CasinoSongs.isValid(state.songIndex) ? state.songIndex : -1;
        return new CasinoMusicStatePayload(song, state.volume, state.playSerial);
    }

    /** Se guarda con el mundo para que la música siga elegida al volver a entrar. */
    public static class MusicState extends PersistentState {

        public static final String STATE_ID = "prueba_no1_casino_music";

        public static final Type<MusicState> TYPE = new Type<>(
                MusicState::new,
                MusicState::fromNbt,
                DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        );

        private int songIndex = -1;
        private int volume = DEFAULT_VOLUME;
        private int playSerial = 0;

        public static MusicState get(MinecraftServer server) {
            return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
        }

        public static MusicState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            MusicState state = new MusicState();
            state.songIndex = nbt.contains("Song") ? nbt.getInt("Song") : -1;
            state.volume = nbt.contains("Volume") ? MathHelper.clamp(nbt.getInt("Volume"), 0, 100) : DEFAULT_VOLUME;
            state.playSerial = nbt.getInt("Serial");
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            nbt.putInt("Song", songIndex);
            nbt.putInt("Volume", volume);
            nbt.putInt("Serial", playSerial);
            return nbt;
        }
    }
}
