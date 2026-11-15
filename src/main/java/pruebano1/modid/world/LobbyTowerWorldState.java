package pruebano1.modid.world;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Estado persistente del Overworld para la torre del Lobby principal: dónde quedó,
 * si ya se terminó de colocar y si este mundo no la lleva (mundos creados antes de existir el lobby).
 */
public class LobbyTowerWorldState extends PersistentState {

    public static final String STATE_ID = "prueba_no1_lobby_tower";

    public static final Type<LobbyTowerWorldState> TYPE = new Type<>(
            LobbyTowerWorldState::new,
            LobbyTowerWorldState::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private boolean initialized;
    private boolean placed;
    private boolean disabled;
    private int chunkX;
    private int chunkZ;
    private int baseY;

    public static LobbyTowerWorldState get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public static LobbyTowerWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        LobbyTowerWorldState state = new LobbyTowerWorldState();
        state.initialized = nbt.getBoolean("Initialized");
        state.placed = nbt.getBoolean("Placed");
        state.disabled = nbt.getBoolean("Disabled");
        state.chunkX = nbt.getInt("ChunkX");
        state.chunkZ = nbt.getInt("ChunkZ");
        state.baseY = nbt.getInt("BaseY");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("Initialized", initialized);
        nbt.putBoolean("Placed", placed);
        nbt.putBoolean("Disabled", disabled);
        nbt.putInt("ChunkX", chunkX);
        nbt.putInt("ChunkZ", chunkZ);
        nbt.putInt("BaseY", baseY);
        return nbt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPlaced() {
        return placed;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getBaseY() {
        return baseY;
    }

    public void setAnchor(int chunkX, int baseY, int chunkZ) {
        this.initialized = true;
        this.disabled = false;
        this.chunkX = chunkX;
        this.baseY = baseY;
        this.chunkZ = chunkZ;
        this.markDirty();
    }

    public void markPlaced() {
        this.placed = true;
        this.markDirty();
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        this.markDirty();
    }
}
