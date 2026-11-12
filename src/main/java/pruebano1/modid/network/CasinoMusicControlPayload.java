package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import pruebano1.modid.Prueba_no1;

/**
 * Cliente -> Servidor: el jugador usó la interfaz del DJ Controller.
 * action: PLAY (reproduce songIndex), STOP (silencio) o VOLUME (cambia volume 0-100).
 */
public record CasinoMusicControlPayload(BlockPos controllerPos, int action, int songIndex, int volume)
        implements CustomPayload {

    public static final int ACTION_PLAY = 0;
    public static final int ACTION_STOP = 1;
    public static final int ACTION_VOLUME = 2;

    public static final CustomPayload.Id<CasinoMusicControlPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "casino_music_control")
    );

    public static final PacketCodec<RegistryByteBuf, CasinoMusicControlPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, CasinoMusicControlPayload::controllerPos,
            PacketCodecs.VAR_INT, CasinoMusicControlPayload::action,
            PacketCodecs.VAR_INT, CasinoMusicControlPayload::songIndex,
            PacketCodecs.VAR_INT, CasinoMusicControlPayload::volume,
            CasinoMusicControlPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
