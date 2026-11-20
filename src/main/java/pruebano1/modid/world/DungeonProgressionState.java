package pruebano1.modid.world;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.network.DungeonProgressionPayload;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Estado persistente global del mundo que registra qué dungeons han sido conquistadas.
 * Orden oficial de progresión:
 *   1. Isla Penstein (Jefri Penstein)
 *   2. Casa de José Torres (José Torres / Jhoseph towers)
 *   3. La Casa Blanca (Charlie Kirk)
 *   4. El Palacio Nacional (Jefe Final del Palacio Nacional)
 *   5+. Futuras dungeons por desarrollar
 *
 * Basta con que UN solo jugador derrote al jefe de una dungeon para que quede registrada
 * globalmente y ningún jugador vuelva a recibir la advertencia al entrar a la siguiente.
 */
public class DungeonProgressionState extends PersistentState {

    private static final String STATE_KEY = Prueba_no1.MOD_ID + "_dungeon_progression";

    public enum DungeonStage {
        ISLA_PENSTEIN("isla_penstein", "Isla Penstein", "Jefri Penstein"),
        CASA_JOSE_TORRES("casa_jose_torres", "Casa de José Torres", "José Torres"),
        CASA_BLANCA("casa_blanca", "La Casa Blanca", "Charlie Kirk"),
        PALACIO_NACIONAL("palacio_nacional", "El Palacio Nacional", "Jefe Final del Palacio Nacional");

        private final String id;
        private final String dungeonDisplayName;
        private final String bossDisplayName;

        DungeonStage(String id, String dungeonDisplayName, String bossDisplayName) {
            this.id = id;
            this.dungeonDisplayName = dungeonDisplayName;
            this.bossDisplayName = bossDisplayName;
        }

        public String getId() {
            return this.id;
        }

        public String getDungeonDisplayName() {
            return this.dungeonDisplayName;
        }

        public String getBossDisplayName() {
            return this.bossDisplayName;
        }

        @Nullable
        public DungeonStage previous() {
            int idx = this.ordinal();
            return idx > 0 ? values()[idx - 1] : null;
        }

        @Nullable
        public static DungeonStage byId(String id) {
            for (DungeonStage stage : values()) {
                if (stage.id.equalsIgnoreCase(id)) {
                    return stage;
                }
            }
            return null;
        }
    }

    private final Set<DungeonStage> completedDungeons = EnumSet.noneOf(DungeonStage.class);

    public static final Type<DungeonProgressionState> TYPE = new Type<>(
            DungeonProgressionState::new,
            DungeonProgressionState::fromNbt,
            null
    );

    public DungeonProgressionState() {
    }

    public static DungeonProgressionState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        DungeonProgressionState state = new DungeonProgressionState();
        if (nbt.contains("CompletedDungeons", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("CompletedDungeons", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size(); i++) {
                DungeonStage stage = DungeonStage.byId(list.getString(i));
                if (stage != null) {
                    state.completedDungeons.add(stage);
                }
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (DungeonStage stage : this.completedDungeons) {
            list.add(NbtString.of(stage.getId()));
        }
        nbt.put("CompletedDungeons", list);
        return nbt;
    }

    @Nullable
    public static synchronized DungeonProgressionState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (overworld == null) {
            return null;
        }
        PersistentStateManager manager = overworld.getPersistentStateManager();
        return manager.getOrCreate(TYPE, STATE_KEY);
    }

    public synchronized boolean isDungeonCompleted(DungeonStage stage) {
        return this.completedDungeons.contains(stage);
    }

    public synchronized boolean markCompleted(DungeonStage stage) {
        if (this.completedDungeons.add(stage)) {
            this.markDirty();
            return true;
        }
        return false;
    }

    public synchronized void resetAll() {
        if (!this.completedDungeons.isEmpty()) {
            this.completedDungeons.clear();
            this.markDirty();
        }
    }

    public synchronized DungeonProgressionPayload toPayload() {
        List<String> ids = new ArrayList<>(this.completedDungeons.size());
        for (DungeonStage stage : this.completedDungeons) {
            ids.add(stage.getId());
        }
        return new DungeonProgressionPayload(ids);
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        DungeonProgressionState state = get(server);
        if (state != null) {
            ServerPlayNetworking.send(player, state.toPayload());
        }
    }

    public static void broadcastToAll(MinecraftServer server) {
        DungeonProgressionState state = get(server);
        if (state == null) {
            return;
        }
        DungeonProgressionPayload payload = state.toPayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Registra globalmente que el jefe principal de una dungeon ha sido derrotado.
     */
    public static void recordBossDefeated(ServerWorld world, DungeonStage stage) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return;
        }
        DungeonProgressionState state = get(server);
        if (state != null && state.markCompleted(stage)) {
            Prueba_no1.LOGGER.info("Progresión de dungeons: {} ({}) ha sido completada.", stage.getDungeonDisplayName(), stage.getBossDisplayName());
            broadcastToAll(server);
        }
    }

    /**
     * Comprueba si la dungeon anterior en el orden oficial ya fue superada por al menos un jugador.
     * Si aún no ha sido derrotada, envía una advertencia clara al jugador que acaba de entrar.
     */
    public static void checkAndWarnOnDungeonEntry(ServerPlayerEntity player, DungeonStage enteringStage) {
        DungeonStage previous = enteringStage.previous();
        if (previous == null) {
            return; // Isla Penstein es la primera dungeon, no tiene requisito previo
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        DungeonProgressionState state = get(server);
        if (state == null || state.isDungeonCompleted(previous)) {
            return;
        }

        // Sonido de advertencia para el jugador
        player.playSoundToPlayer(SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 0.9F, 0.75F);
        player.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.MASTER, 1.0F, 0.6F);

        // Mensaje destacado en el chat
        Text chatWarning = Text.literal("⚠ ¡ADVERTENCIA DE PROGRESIÓN! ")
                .formatted(Formatting.DARK_RED, Formatting.BOLD)
                .append(Text.literal("Has entrado a ")
                        .formatted(Formatting.RED))
                .append(Text.literal(enteringStage.getDungeonDisplayName())
                        .formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal(" sin haber derrotado antes al jefe de la dungeon anterior: ")
                        .formatted(Formatting.RED))
                .append(Text.literal(previous.getDungeonDisplayName() + " (" + previous.getBossDisplayName() + ")")
                        .formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(".")
                        .formatted(Formatting.RED));

        Text orderHint = Text.literal("   Orden de conquista: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal("Isla Penstein ➔ Casa de José Torres ➔ Casa Blanca ➔ Palacio Nacional")
                        .formatted(Formatting.AQUA));

        player.sendMessage(chatWarning, false);
        player.sendMessage(orderHint, false);

        // Aviso inmediato en la barra de acción (sobre el inventario)
        Text actionBarWarning = Text.literal("⚠ Aún no han derrotado el boss anterior: " + previous.getBossDisplayName() + " (" + previous.getDungeonDisplayName() + ")")
                .formatted(Formatting.RED, Formatting.BOLD);
        player.sendMessage(actionBarWarning, true);
    }
}
