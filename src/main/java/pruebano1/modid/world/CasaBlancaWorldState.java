package pruebano1.modid.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import pruebano1.modid.Prueba_no1;

/**
 * Estado persistente por mundo para la ubicación única de "La Casa Blanca".
 */
public class CasaBlancaWorldState extends PersistentState {

    private static final String STATE_KEY = Prueba_no1.MOD_ID + "_casa_blanca";

    private boolean initialized = false;
    private int startChunkX = 0;
    private int baseY = 72;
    private int startChunkZ = 0;

    public static final Type<CasaBlancaWorldState> TYPE = new Type<>(
            CasaBlancaWorldState::new,
            CasaBlancaWorldState::fromNbt,
            null
    );

    public CasaBlancaWorldState() {
    }

    public static CasaBlancaWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        CasaBlancaWorldState state = new CasaBlancaWorldState();
        state.initialized = nbt.getBoolean("Initialized");
        state.startChunkX = nbt.getInt("StartChunkX");
        state.baseY = nbt.getInt("BaseY");
        state.startChunkZ = nbt.getInt("StartChunkZ");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("Initialized", this.initialized);
        nbt.putInt("StartChunkX", this.startChunkX);
        nbt.putInt("BaseY", this.baseY);
        nbt.putInt("StartChunkZ", this.startChunkZ);
        return nbt;
    }

    public static CasaBlancaWorldState get(ServerWorld overworld) {
        PersistentStateManager manager = overworld.getPersistentStateManager();
        return manager.getOrCreate(TYPE, STATE_KEY);
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public int getStartChunkX() {
        return this.startChunkX;
    }

    public int getBaseY() {
        return this.baseY;
    }

    public int getStartChunkZ() {
        return this.startChunkZ;
    }

    public void setAnchor(int startChunkX, int baseY, int startChunkZ) {
        this.initialized = true;
        this.startChunkX = startChunkX;
        this.baseY = baseY;
        this.startChunkZ = startChunkZ;
        this.markDirty();
    }
}
