package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Dismounting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.block.CasinoSeat;
import pruebano1.modid.block.CasinoChairBlock;

/**
 * Entidad invisible de asiento para sentarse en los asientos del casino (silla negra, sofá, silla de bar).
 */
public class SeatEntity extends Entity {

    private int emptyTicks = 0;

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.baseTick();
        if (!this.getWorld().isClient()) {
            BlockState state = this.getWorld().getBlockState(this.getBlockPos());
            if (!(state.getBlock() instanceof CasinoSeat)) {
                this.removeAllPassengers();
                this.discard();
                return;
            }
            if (!this.hasPassengers()) {
                this.emptyTicks++;
                if (this.emptyTicks > 2) {
                    this.discard();
                }
            } else {
                this.emptyTicks = 0;
            }
        }
    }

    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // Cada asiento (silla, sofá, silla de bar alta) dice a qué altura está su cojín
        BlockState state = this.getWorld().getBlockState(this.getBlockPos());
        double height = state.getBlock() instanceof CasinoSeat seat ? seat.getSeatHeight() : 0.59;
        return new Vec3d(0.0, height, 0.0);
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        BlockPos chairPos = this.getBlockPos();
        BlockState state = this.getWorld().getBlockState(chairPos);
        Direction facing = state.contains(CasinoChairBlock.FACING)
                ? state.get(CasinoChairBlock.FACING)
                : Direction.NORTH;

        // Priorizar salir hacia los costados o detrás antes que encima para no atravesar máquinas/barras
        Direction[] candidates = {
                facing.rotateYClockwise(),
                facing.rotateYCounterclockwise(),
                facing.getOpposite(),
                facing
        };
        for (Direction dir : candidates) {
            Vec3d sidePos = Dismounting.findRespawnPos(passenger.getType(), this.getWorld(), chairPos.offset(dir), false);
            if (sidePos != null) {
                return sidePos;
            }
        }
        Vec3d topPos = Dismounting.findRespawnPos(passenger.getType(), this.getWorld(), chairPos.up(), false);
        if (topPos != null) {
            return topPos;
        }
        return new Vec3d(chairPos.getX() + 0.5, chairPos.getY() + 0.95, chairPos.getZ() + 0.5);
    }
}
