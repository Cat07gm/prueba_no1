package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.SeatEntity;

import java.util.List;

/**
 * Silla negra / banquito de casino con asiento giratorio en el que los jugadores pueden sentarse con clic derecho.
 */
public class CasinoChairBlock extends HorizontalFacingBlock {

    public static final MapCodec<CasinoChairBlock> CODEC = createCodec(CasinoChairBlock::new);

    private static final VoxelShape BASE_AND_SEAT = VoxelShapes.union(
            Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 2.0, 13.0),
            Block.createCuboidShape(4.0, 2.0, 4.0, 12.0, 7.0, 12.0),
            Block.createCuboidShape(2.5, 7.0, 2.5, 13.5, 9.5, 13.5)
    );

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(BASE_AND_SEAT, Block.createCuboidShape(3.0, 9.5, 12.0, 13.0, 15.0, 14.0));
    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(BASE_AND_SEAT, Block.createCuboidShape(3.0, 9.5, 2.0, 13.0, 15.0, 4.0));
    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(BASE_AND_SEAT, Block.createCuboidShape(2.0, 9.5, 3.0, 4.0, 15.0, 13.0));
    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(BASE_AND_SEAT, Block.createCuboidShape(12.0, 9.5, 3.0, 14.0, 15.0, 13.0));

    public CasinoChairBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.isSpectator() || player.hasVehicle()) {
            return ActionResult.PASS;
        }

        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true);
            for (SeatEntity existing : seats) {
                if (existing.hasPassengers()) {
                    if (existing.hasPassenger(player)) {
                        return ActionResult.SUCCESS;
                    }
                    player.sendMessage(Text.literal("Este asiento ya está ocupado.").formatted(Formatting.RED), true);
                    return ActionResult.SUCCESS;
                }
                existing.discard();
            }

            SeatEntity seat = ModEntities.SEAT.create(serverWorld, SpawnReason.TRIGGERED);
            if (seat != null) {
                float seatYaw = state.get(FACING).getPositiveHorizontalDegrees();
                seat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, seatYaw, 0.0F);
                serverWorld.spawnEntity(seat);
                player.setYaw(seatYaw);
                player.setHeadYaw(seatYaw);
                player.startRiding(seat, true);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient()) {
            List<SeatEntity> seats = world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true);
            for (SeatEntity seat : seats) {
                seat.removeAllPassengers();
                seat.discard();
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }
}
