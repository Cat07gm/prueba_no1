package pruebano1.modid.block;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Helper compartido para garantizar que el 100% de los jugadores conectados (no espectadores)
 * se encuentren presentes dentro de la dungeon correspondiente antes de invocar a su jefe.
 */
public final class BossSummonHelper {

    private BossSummonHelper() {
    }

    public static boolean ensureAllPlayersPresent(
            ServerWorld serverWorld,
            BlockPos altarPos,
            PlayerEntity caller,
            String blockTitle,
            String bossDisplayName,
            double fallbackDistance,
            Predicate<BlockPos> dungeonAreaCheck
    ) {
        return ensureAllPlayersPresent(
                serverWorld,
                altarPos,
                caller,
                blockTitle,
                bossDisplayName,
                "la dungeon",
                fallbackDistance,
                dungeonAreaCheck
        );
    }

    public static boolean ensureAllPlayersPresent(
            ServerWorld serverWorld,
            BlockPos altarPos,
            PlayerEntity caller,
            String blockTitle,
            String bossDisplayName,
            String dungeonDisplayName,
            double fallbackDistance,
            Predicate<BlockPos> dungeonAreaCheck
    ) {
        boolean altarInsideDungeon = dungeonAreaCheck.test(altarPos);
        List<ServerPlayerEntity> allPlayers = serverWorld.getServer().getPlayerManager().getPlayerList();
        List<String> missingNames = new ArrayList<>();
        int presentCount = 0;
        int totalEligible = 0;

        for (ServerPlayerEntity p : allPlayers) {
            if (p.isSpectator()) {
                continue;
            }
            totalEligible++;
            boolean sameWorld = p.getServerWorld() == serverWorld;
            // Si el altar está dentro de la dungeon, exigir estrictamente que TODOS los jugadores estén dentro de esa dungeon.
            // Solo si el altar fue colocado fuera de la dungeon (ej. creativo) usar la distancia de respaldo.
            boolean inPlace = sameWorld && (altarInsideDungeon
                    ? dungeonAreaCheck.test(p.getBlockPos())
                    : p.getBlockPos().isWithinDistance(altarPos, fallbackDistance));
            if (inPlace) {
                presentCount++;
            } else {
                missingNames.add(p.getName().getString());
            }
        }

        if (!missingNames.isEmpty()) {
            serverWorld.playSound(null, altarPos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.9F, 0.75F);
            String joined = String.join(", ", missingNames);
            String placeLabel = altarInsideDungeon ? dungeonDisplayName : "el área de invocación";
            caller.sendMessage(
                    Text.literal(blockTitle + ": ")
                            .formatted(Formatting.RED, Formatting.BOLD)
                            .append(Text.literal(
                                    "Todos los jugadores conectados deben estar dentro de " + placeLabel
                                            + " para invocar a " + bossDisplayName
                                            + " (" + presentCount + "/" + totalEligible
                                            + "). Faltan: " + joined
                            ).formatted(Formatting.YELLOW)),
                    false
            );
            return false;
        }
        return true;
    }
}
