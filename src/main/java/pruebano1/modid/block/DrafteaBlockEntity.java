package pruebano1.modid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModBlockEntities;

/**
 * Guarda lo que muestra la pantalla de la Draftea: el ítem de cada uno de los 3 rodillos
 * y el premio pendiente mientras gira. Se sincroniza con el cliente para que el renderer lo dibuje.
 * Solo la mitad superior (la de la pantalla) se usa para mostrar.
 */
public class DrafteaBlockEntity extends BlockEntity {

    public static final int REELS = 3;

    private final Item[] reels = {Items.AIR, Items.AIR, Items.AIR};
    /** Índice en DrafteaRewards.REWARDS del premio que va a salir, o -1 si no está girando. */
    private int pendingReward = -1;

    public DrafteaBlockEntity(net.minecraft.util.math.BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAFTEA, pos, state);
    }

    public Item getReel(int index) {
        return reels[index];
    }

    public void setReel(int index, Item item) {
        reels[index] = item;
    }

    public int getPendingReward() {
        return pendingReward;
    }

    public void setPendingReward(int pendingReward) {
        this.pendingReward = pendingReward;
    }

    /** Guarda y manda los cambios a los jugadores cercanos. */
    public void sync() {
        markDirty();
        if (world != null && !world.isClient()) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        for (int i = 0; i < REELS; i++) {
            nbt.putString("Reel" + i, Registries.ITEM.getId(reels[i]).toString());
        }
        nbt.putInt("PendingReward", pendingReward);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        for (int i = 0; i < REELS; i++) {
            Identifier id = Identifier.tryParse(nbt.getString("Reel" + i));
            reels[i] = id != null ? Registries.ITEM.get(id) : Items.AIR;
        }
        pendingReward = nbt.contains("PendingReward") ? nbt.getInt("PendingReward") : -1;
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
