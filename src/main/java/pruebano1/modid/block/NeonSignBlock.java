package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cuadro / letrero neón de pared (cartas, ficha, dados, 777). Es un panel delgado que se pega
 * a la pared en la que se coloca y da luz.
 */
public class NeonSignBlock extends HorizontalFacingBlock {

    public static final MapCodec<NeonSignBlock> CODEC = createCodec(NeonSignBlock::new);

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // FACING = hacia dónde mira el dibujo; el panel queda pegado a la pared de atrás
        SHAPES.put(Direction.NORTH, Block.createCuboidShape(0, 0, 15, 16, 16, 16));
        SHAPES.put(Direction.SOUTH, Block.createCuboidShape(0, 0, 0, 16, 16, 1));
        SHAPES.put(Direction.EAST, Block.createCuboidShape(0, 0, 0, 1, 16, 16));
        SHAPES.put(Direction.WEST, Block.createCuboidShape(15, 0, 0, 16, 16, 16));
    }

    public NeonSignBlock(Settings settings) {
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
        Direction side = ctx.getSide();
        Direction facing = side.getAxis().isHorizontal() ? side : ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState().with(FACING, facing);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }
}
