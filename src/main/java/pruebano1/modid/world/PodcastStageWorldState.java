package pruebano1.modid.world;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Estado persistente del Overworld para la estructura única "Escenario del Podcast"
 * donde se encuentra el altar del bloque "Cassete de Podcast" para invocar a José Torres.
 */
public class PodcastStageWorldState extends PersistentState {

    public static final String STATE_ID = "prueba_no1_podcast_stage";

    public static final Type<PodcastStageWorldState> TYPE = new Type<>(
            PodcastStageWorldState::new,
            PodcastStageWorldState::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private boolean initialized;
    private boolean placed;
    private int startChunkX;
    private int startChunkZ;
    private int baseY;

    public static PodcastStageWorldState get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public static PodcastStageWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        PodcastStageWorldState state = new PodcastStageWorldState();
        state.initialized = nbt.getBoolean("Initialized");
        state.placed = nbt.getBoolean("Placed");
        state.startChunkX = nbt.getInt("StartChunkX");
        state.startChunkZ = nbt.getInt("StartChunkZ");
        state.baseY = nbt.getInt("BaseY");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("Initialized", initialized);
        nbt.putBoolean("Placed", placed);
        nbt.putInt("StartChunkX", startChunkX);
        nbt.putInt("StartChunkZ", startChunkZ);
        nbt.putInt("BaseY", baseY);
        return nbt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPlaced() {
        return placed;
    }

    public int getStartChunkX() {
        return startChunkX;
    }

    public int getStartChunkZ() {
        return startChunkZ;
    }

    public int getBaseY() {
        return baseY;
    }

    public void setAnchor(int startChunkX, int baseY, int startChunkZ) {
        this.initialized = true;
        this.startChunkX = startChunkX;
        this.baseY = baseY;
        this.startChunkZ = startChunkZ;
        this.markDirty();
    }

    public void markPlaced() {
        this.placed = true;
        this.markDirty();
    }
}
