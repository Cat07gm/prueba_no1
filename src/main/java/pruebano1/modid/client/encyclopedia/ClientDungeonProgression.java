package pruebano1.modid.client.encyclopedia;

import org.jetbrains.annotations.Nullable;
import pruebano1.modid.network.DungeonProgressionPayload;
import pruebano1.modid.world.DungeonProgressionState.DungeonStage;

import java.util.EnumSet;
import java.util.Set;

/**
 * Caché en cliente del progreso global de dungeons sincronizado por el servidor.
 * Implementa las reglas de "Modo Sin Spoilers": una dungeon solo se revela cuando
 * el jefe de la dungeon inmediata anterior ya fue derrotado.
 */
public final class ClientDungeonProgression {

    private static final Set<DungeonStage> COMPLETED = EnumSet.noneOf(DungeonStage.class);

    private ClientDungeonProgression() {
    }

    public static synchronized void applyPayload(DungeonProgressionPayload payload) {
        COMPLETED.clear();
        if (payload == null || payload.completedDungeonIds() == null) {
            return;
        }
        for (String id : payload.completedDungeonIds()) {
            DungeonStage stage = DungeonStage.byId(id);
            if (stage != null) {
                COMPLETED.add(stage);
            }
        }
    }

    public static synchronized void reset() {
        COMPLETED.clear();
    }

    public static synchronized boolean isCompleted(DungeonStage stage) {
        return COMPLETED.contains(stage);
    }

    /**
     * Modo Sin Spoilers:
     * - La primera dungeon (Isla Penstein) siempre es visible.
     * - Cualquier dungeon posterior solo se desbloquea/revela si la anterior fue derrotada
     *   (o si ella misma ya fue derrotada).
     */
    public static synchronized boolean isRevealed(DungeonStage stage) {
        if (COMPLETED.contains(stage)) {
            return true;
        }
        DungeonStage prev = stage.previous();
        return prev == null || COMPLETED.contains(prev);
    }

    public static synchronized int getCompletedCount() {
        return COMPLETED.size();
    }

    public static int getTotalImplementedDungeons() {
        return DungeonStage.values().length;
    }

    /**
     * Comprueba si una entrada de estructura de la enciclopedia corresponde a una dungeon
     * que aún debe mantenerse oculta ("???") por no haber vencido al jefe previo.
     */
    public static synchronized boolean isStructureEntrySpoilerLocked(@Nullable String entryId) {
        if (entryId == null) {
            return false;
        }
        DungeonStage stage = DungeonStage.byId(entryId);
        if (stage == null) {
            return false; // Estructuras libres como el Casino Draftea o entradas normales
        }
        return !isRevealed(stage);
    }
}
