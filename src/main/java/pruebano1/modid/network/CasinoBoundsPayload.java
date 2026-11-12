package pruebano1.modid.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import pruebano1.modid.Prueba_no1;

/**
 * Sincroniza con el cliente los límites protegidos del casino único "Draftea Tony Diaz"
 * para evitar parpadeos al intentar romper o colocar bloques dentro del recinto.
 */
public record CasinoBoundsPayload(
        boolean hasBounds,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) implements CustomPayload {

    public static final CustomPayload.Id<CasinoBoundsPayload> ID = new CustomPayload.Id<>(
            Identifier.of(Prueba_no1.MOD_ID, "casino_bounds")
    );

    public static final PacketCodec<RegistryByteBuf, CasinoBoundsPayload> CODEC = new PacketCodec<>() {
        @Override
        public CasinoBoundsPayload decode(RegistryByteBuf buf) {
            boolean has = buf.readBoolean();
            if (!has) {
                return new CasinoBoundsPayload(false, 0, 0, 0, 0, 0, 0);
            }
            return new CasinoBoundsPayload(
                    true,
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        @Override
        public void encode(RegistryByteBuf buf, CasinoBoundsPayload value) {
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

    public static CasinoBoundsPayload empty() {
        return new CasinoBoundsPayload(false, 0, 0, 0, 0, 0, 0);
    }

    public static CasinoBoundsPayload fromBox(BlockBox box) {
        return new CasinoBoundsPayload(
                true,
                box.getMinX(),
                box.getMinY(),
                box.getMinZ(),
                box.getMaxX(),
                box.getMaxY(),
                box.getMaxZ()
        );
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
