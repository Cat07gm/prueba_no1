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
 * se encuentren presentes en el lugar antes de consumir un ítem de invocación de jefe.
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
            boolean inPlace = sameWorld && (dungeonAreaCheck.test(p.getBlockPos())
                    || p.getBlockPos().isWithinDistance(altarPos, fallbackDistance));
            if (inPlace) {
                presentCount++;
            } else {
                missingNames.add(p.getName().getString());
            }
        }

        if (!missingNames.isEmpty()) {
            serverWorld.playSound(null, altarPos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.9F, 0.75F);
            String joined = String.join(", ", missingNames);
            caller.sendMessage(
                    Text.literal(blockTitle + ": ")
                            .formatted(Formatting.RED, Formatting.BOLD)
                            .append(Text.literal(
                                    "El 100% de los jugadores debe estar presente en el lugar para invocar a "
                                            + bossDisplayName + " (" + presentCount + "/" + totalEligible
                                            + "). Faltan: " + joined
                            ).formatted(Formatting.YELLOW)),
                    false
            );
            return false;
        }
        return true;
    }
}
