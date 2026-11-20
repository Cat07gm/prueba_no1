package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

/**
 * Paquete S2C enviado únicamente al jugador atrapado por el ataque telequinético de Charlie Kirk
 * para mostrar rápidamente el aviso visual y reproducir el sonido "Efecto jefri".
 */
public record CharlieEpsteinPopupPayload(boolean phase2) implements CustomPayload {

    public static final CustomPayload.Id<CharlieEpsteinPopupPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "charlie_epstein_popup")
    );

    public static final PacketCodec<RegistryByteBuf, CharlieEpsteinPopupPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN, CharlieEpsteinPopupPayload::phase2,
            CharlieEpsteinPopupPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
