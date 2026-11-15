package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import pruebano1.modid.Prueba_no1;

/**
 * Sincroniza con el cliente los límites protegidos de la torre del Lobby principal
 * para que romper/colocar bloques dentro no parpadee.
 */
public record LobbyBoundsPayload(
        boolean hasBounds,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) implements CustomPayload {

    public static final CustomPayload.Id<LobbyBoundsPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "lobby_bounds")
    );

    public static final PacketCodec<RegistryByteBuf, LobbyBoundsPayload> CODEC = new PacketCodec<>() {
        @Override
        public LobbyBoundsPayload decode(RegistryByteBuf buf) {
            if (!buf.readBoolean()) {
                return empty();
            }
            return new LobbyBoundsPayload(
                    true,
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readInt()
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, LobbyBoundsPayload value) {
            buf.writeBoolean(value.hasBounds());
            if (value.hasBounds()) {
                buf.writeInt(value.minX());
                buf.writeInt(value.minY());
                buf.writeInt(value.minZ());
                buf.writeInt(value.maxX());
                buf.writeInt(value.maxY());
                buf.writeInt(value.maxZ());
            }
        }
    };

    public static LobbyBoundsPayload empty() {
        return new LobbyBoundsPayload(false, 0, 0, 0, 0, 0, 0);
    }

    public static LobbyBoundsPayload fromBox(BlockBox box) {
        return new LobbyBoundsPayload(
                true,
                box.getMinX(), box.getMinY(), box.getMinZ(),
                box.getMaxX(), box.getMaxY(), box.getMaxZ()
        );
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
