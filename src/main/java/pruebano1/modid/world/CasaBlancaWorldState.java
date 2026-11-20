package pruebano1.modid.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import pruebano1.modid.Prueba_no1;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado persistente por mundo para la ubicación única de "La Casa Blanca"
 * y el seguimiento de actualización de chunks hacia la versión actual del diseño.
 */
public class CasaBlancaWorldState extends PersistentState {

    public static final int CURRENT_LAYOUT_VERSION = 4;
    private static final String STATE_KEY = Prueba_no1.MOD_ID + "_casa_blanca";

    private boolean initialized = false;
    private int startChunkX = 0;
    private int baseY = 72;
    private int startChunkZ = 0;
    private int layoutVersion = 0;
    private final Set<Long> upgradedChunks = ConcurrentHashMap.newKeySet();

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
        int loadedVersion = nbt.getInt("LayoutVersion");
        if (loadedVersion >= CURRENT_LAYOUT_VERSION && nbt.contains("UpgradedChunks")) {
            state.layoutVersion = loadedVersion;
            for (long packed : nbt.getLongArray("UpgradedChunks")) {
                state.upgradedChunks.add(packed);
            }
        } else {
            state.layoutVersion = CURRENT_LAYOUT_VERSION;
            state.upgradedChunks.clear();
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("Initialized", this.initialized);
        nbt.putInt("StartChunkX", this.startChunkX);
        nbt.putInt("BaseY", this.baseY);
        nbt.putInt("StartChunkZ", this.startChunkZ);
        nbt.putInt("LayoutVersion", this.layoutVersion);
        long[] arr = this.upgradedChunks.stream().mapToLong(Long::longValue).toArray();
        nbt.putLongArray("UpgradedChunks", arr);
        return nbt;
    }

    public static synchronized CasaBlancaWorldState get(ServerWorld overworld) {
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
        if (this.layoutVersion < CURRENT_LAYOUT_VERSION) {
            this.layoutVersion = CURRENT_LAYOUT_VERSION;
        }
        this.markDirty();
    }

    public boolean isChunkUpgraded(int chunkX, int chunkZ) {
        return this.layoutVersion >= CURRENT_LAYOUT_VERSION && this.upgradedChunks.contains(packChunkPos(chunkX, chunkZ));
    }

    public void markChunkUpgraded(int chunkX, int chunkZ) {
        this.layoutVersion = CURRENT_LAYOUT_VERSION;
        if (this.upgradedChunks.add(packChunkPos(chunkX, chunkZ))) {
            this.markDirty();
        }
    }

    public void resetUpgradedChunks() {
        this.layoutVersion = CURRENT_LAYOUT_VERSION;
        this.upgradedChunks.clear();
        this.markDirty();
    }

    private static long packChunkPos(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }
}
