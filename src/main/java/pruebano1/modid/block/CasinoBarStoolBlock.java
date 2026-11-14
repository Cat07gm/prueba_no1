package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Silla de bar alta del casino (cromo, cuero negro y cojín morado) para la barra de bebidas.
 * Al colocarla, quien se siente mira hacia donde miraba el jugador (normalmente, hacia la barra).
 */
public class CasinoBarStoolBlock extends HorizontalFacingBlock implements CasinoSeat {

    public static final MapCodec<CasinoBarStoolBlock> CODEC = createCodec(CasinoBarStoolBlock::new);

    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(4, 0, 4, 12, 1, 12),   // Base
            Block.createCuboidShape(7, 1, 7, 9, 13, 9),    // Poste
            Block.createCuboidShape(3, 13, 3, 13, 16, 13)  // Asiento
    );

    public CasinoBarStoolBlock(Settings settings) {
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
    public double getSeatHeight() {
        return 1.0; // Cojín a 16 px
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return CasinoSeat.sit(world, pos, player, state.get(FACING).getPositiveHorizontalDegrees());
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            CasinoSeat.clearSeats(world, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }
}
