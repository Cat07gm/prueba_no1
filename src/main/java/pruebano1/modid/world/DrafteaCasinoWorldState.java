package pruebano1.modid.world;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

/**
 * Estado persistente del Overworld que garantiza que el casino único "Draftea Tony Diaz"
 * se genere una sola vez por mundo cerca del spawn inicial y conserve sus límites exactos.
 */
public class DrafteaCasinoWorldState extends PersistentState {

    public static final String STATE_ID = "prueba_no1_draftea_casino";

    public static final int PROTECTED_MIN_DX = -48;
    public static final int PROTECTED_MAX_DX = 48;
    public static final int PROTECTED_MIN_DZ = -38;
    public static final int PROTECTED_MAX_DZ = 42;
    public static final int PROTECTED_MIN_REL_Y = -8;
    public static final int PROTECTED_MAX_REL_Y = 44;

    public static final int CURRENT_NPC_LAYOUT_VERSION = 3;

    public static final Type<DrafteaCasinoWorldState> TYPE = new Type<>(
            DrafteaCasinoWorldState::new,
            DrafteaCasinoWorldState::fromNbt,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private boolean initialized;
    private boolean placed;
    private boolean npcsSpawned;
    private int npcLayoutVersion;
    private boolean waystonePlaced;
    private int startChunkX;
    private int startChunkZ;
    private int baseY;

    public DrafteaCasinoWorldState() {
    }

    public static DrafteaCasinoWorldState get(ServerWorld overworld) {
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public static DrafteaCasinoWorldState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        DrafteaCasinoWorldState state = new DrafteaCasinoWorldState();
        state.initialized = nbt.getBoolean("Initialized");
        state.placed = nbt.getBoolean("Placed");
        state.npcsSpawned = nbt.getBoolean("NpcsSpawned");
        state.npcLayoutVersion = nbt.getInt("NpcLayoutVersion");
        state.waystonePlaced = nbt.getBoolean("WaystonePlaced");
        state.startChunkX = nbt.getInt("StartChunkX");
        state.startChunkZ = nbt.getInt("StartChunkZ");
        state.baseY = nbt.getInt("BaseY");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putBoolean("Initialized", this.initialized);
        nbt.putBoolean("Placed", this.placed);
        nbt.putBoolean("NpcsSpawned", this.npcsSpawned);
        nbt.putInt("NpcLayoutVersion", this.npcLayoutVersion);
        nbt.putBoolean("WaystonePlaced", this.waystonePlaced);
        nbt.putInt("StartChunkX", this.startChunkX);
        nbt.putInt("StartChunkZ", this.startChunkZ);
        nbt.putInt("BaseY", this.baseY);
        return nbt;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isPlaced() {
        return placed;
    }

    public boolean areNpcsSpawned() {
        return npcsSpawned;
    }

    public int getNpcLayoutVersion() {
        return npcLayoutVersion;
    }

    public void markNpcsSpawned() {
        markNpcsSpawned(CURRENT_NPC_LAYOUT_VERSION);
    }

    public void markNpcsSpawned(int layoutVersion) {
        this.npcsSpawned = true;
        this.npcLayoutVersion = layoutVersion;
        this.markDirty();
    }

    public boolean isWaystonePlaced() {
        return waystonePlaced;
    }

    public void markWaystonePlaced() {
        this.waystonePlaced = true;
        this.markDirty();
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

    public int getCenterX() {
        return new ChunkPos(startChunkX, startChunkZ).getCenterX();
    }

    public int getCenterZ() {
        return new ChunkPos(startChunkX, startChunkZ).getCenterZ();
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

    public BlockBox getProtectedBox() {
        return computeProtectedBox(getCenterX(), baseY, getCenterZ());
    }

    public static BlockBox computeProtectedBox(int centerX, int baseY, int centerZ) {
        return new BlockBox(
                centerX + PROTECTED_MIN_DX,
                baseY + PROTECTED_MIN_REL_Y,
                centerZ + PROTECTED_MIN_DZ,
                centerX + PROTECTED_MAX_DX,
                baseY + PROTECTED_MAX_REL_Y,
                centerZ + PROTECTED_MAX_DZ
        );
    }

    public static boolean isInsideSafeZone(int centerX, int baseY, int centerZ, BlockPos pos) {
        int dx = pos.getX() - centerX;
        int dy = pos.getY() - baseY;
        int dz = pos.getZ() - centerZ;

        if (dy < -1 || dy > 36) {
            return false;
        }
        // Edificio completo (salón principal, alas y mezzanine VIP)
        if (dx >= -36 && dx <= 36 && dz >= -31 && dz <= 24) {
            return true;
        }
        // Vestíbulo / marquesina frontal de entrada
        return dx >= -12 && dx <= 12 && dz >= 24 && dz <= 28 && dy <= 8;
    }
}
