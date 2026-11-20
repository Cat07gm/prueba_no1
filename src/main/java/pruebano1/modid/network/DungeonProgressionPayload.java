package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

import java.util.ArrayList;
import java.util.List;

/**
 * Sincroniza del servidor a los clientes la lista global de dungeons cuyos jefes
 * ya han sido derrotados en el mundo actual, permitiendo actualizar dinámicamente
 * el Mapa de Progresión de Dungeons en la Enciclopedia.
 */
public record DungeonProgressionPayload(List<String> completedDungeonIds) implements CustomPayload {

    public static final CustomPayload.Id<DungeonProgressionPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "dungeon_progression")
    );

    public static final PacketCodec<RegistryByteBuf, DungeonProgressionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING),
            DungeonProgressionPayload::completedDungeonIds,
            DungeonProgressionPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
