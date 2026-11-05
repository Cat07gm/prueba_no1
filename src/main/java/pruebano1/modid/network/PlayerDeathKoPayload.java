package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

/**
 * Paquete de red enviado por el servidor a TODOS los jugadores conectados
 * cuando cualquier jugador muere, para reproducir el sonido y el GIF/efecto visual de "TERRY PAPEADO! K.O.!".
 */
public record PlayerDeathKoPayload(String deadPlayerName) implements CustomPayload {

    public static final CustomPayload.Id<PlayerDeathKoPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "player_death_ko")
    );

    public static final PacketCodec<RegistryByteBuf, PlayerDeathKoPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, PlayerDeathKoPayload::deadPlayerName,
            PlayerDeathKoPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
