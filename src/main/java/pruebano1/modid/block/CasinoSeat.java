package pruebano1.modid.block;

import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.SeatEntity;

import java.util.List;

/**
 * Bloques del casino en los que el jugador se puede sentar con clic derecho
 * (silla negra, sofá modular, silla de bar alta...).
 */
public interface CasinoSeat {

    /** Altura (en bloques) de la superficie del asiento, donde queda sentado el jugador. */
    double getSeatHeight();

    /** Sienta al jugador mirando hacia {@code yaw}. Común a todos los asientos. */
    static ActionResult sit(World world, BlockPos pos, PlayerEntity player, float yaw) {
        if (player.isSpectator() || player.hasVehicle()) {
            return ActionResult.PASS;
        }
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true);
            for (SeatEntity existing : seats) {
                if (existing.hasPassengers()) {
                    if (!existing.hasPassenger(player)) {
                        player.sendMessage(Text.literal("Este asiento ya está ocupado.").formatted(Formatting.RED), true);
                    }
                    return ActionResult.SUCCESS;
                }
                existing.discard();
            }

            SeatEntity seat = ModEntities.SEAT.create(serverWorld, SpawnReason.TRIGGERED);
            if (seat != null) {
                seat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
                serverWorld.spawnEntity(seat);
                player.setYaw(yaw);
                player.setHeadYaw(yaw);
                player.startRiding(seat, true);
            }
        }
        return ActionResult.SUCCESS;
    }

    /** Baja a quien esté sentado y quita el asiento invisible (al romper el bloque). */
    static void clearSeats(World world, BlockPos pos) {
        if (world.isClient()) {
            return;
        }
        for (SeatEntity seat : world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true)) {
            seat.removeAllPassengers();
            seat.discard();
        }
    }
}
