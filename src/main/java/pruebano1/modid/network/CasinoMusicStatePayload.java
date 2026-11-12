package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

/**
 * Servidor -> Clientes: qué canción suena en el casino (-1 = ninguna), a qué volumen (0-100)
 * y un contador que cambia cada vez que se (re)inicia una canción.
 */
public record CasinoMusicStatePayload(int songIndex, int volume, int playSerial) implements CustomPayload {

    public static final CustomPayload.Id<CasinoMusicStatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "casino_music_state")
    );

    public static final PacketCodec<RegistryByteBuf, CasinoMusicStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, CasinoMusicStatePayload::songIndex,
            PacketCodecs.VAR_INT, CasinoMusicStatePayload::volume,
            PacketCodecs.VAR_INT, CasinoMusicStatePayload::playSerial,
            CasinoMusicStatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
