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
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Sofá modular del casino: al poner varios juntos se unen solos (puntas con brazo, piezas
 * centrales sin brazos y esquinas en L). Clic derecho para sentarse.
 * FACING = hacia dónde mira quien se sienta (el respaldo queda del lado contrario).
 */
public class CasinoSofaBlock extends HorizontalFacingBlock implements CasinoSeat {

    public static final MapCodec<CasinoSofaBlock> CODEC = createCodec(CasinoSofaBlock::new);

    public enum Shape implements StringIdentifiable {
        SINGLE("single"),       // Suelto: brazos en ambos lados
        LEFT("left"),           // Punta izquierda: brazo a la izquierda
        RIGHT("right"),         // Punta derecha: brazo a la derecha
        MIDDLE("middle"),       // Pieza central: sin brazos
        CORNER_LEFT("corner_left"),   // Esquina: respaldo extra a la izquierda
        CORNER_RIGHT("corner_right"); // Esquina: respaldo extra a la derecha

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    public static final EnumProperty<Shape> SHAPE = EnumProperty.of("shape", Shape.class);

    private static final VoxelShape SEAT = Block.createCuboidShape(0, 0, 0, 16, 8, 16);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // Respaldo en el lado contrario a FACING
        SHAPES.put(Direction.NORTH, VoxelShapes.union(SEAT, Block.createCuboidShape(0, 8, 13, 16, 15, 16)));
        SHAPES.put(Direction.SOUTH, VoxelShapes.union(SEAT, Block.createCuboidShape(0, 8, 0, 16, 15, 3)));
        SHAPES.put(Direction.EAST, VoxelShapes.union(SEAT, Block.createCuboidShape(0, 8, 0, 3, 15, 16)));
        SHAPES.put(Direction.WEST, VoxelShapes.union(SEAT, Block.createCuboidShape(13, 8, 0, 16, 15, 16)));
    }

    public CasinoSofaBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(SHAPE, Shape.SINGLE));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public double getSeatHeight() {
        return 0.5; // Cojín a 8 px
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        return this.getDefaultState()
                .with(FACING, facing)
                .with(SHAPE, computeShape(ctx.getWorld(), ctx.getBlockPos(), facing));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        if (direction.getAxis().isHorizontal()) {
            return state.with(SHAPE, computeShape(world, pos, state.get(FACING)));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    private Shape computeShape(BlockView world, BlockPos pos, Direction facing) {
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();

        // Esquina en L: el sofá de enfrente está girado 90°, así que este bloque lleva un segundo respaldo
        BlockState front = world.getBlockState(pos.offset(facing));
        if (front.isOf(this)) {
            Direction frontFacing = front.get(FACING);
            if (frontFacing == right) {
                return Shape.CORNER_LEFT;
            }
            if (frontFacing == left) {
                return Shape.CORNER_RIGHT;
            }
        }

        boolean leftLinked = connects(world, pos, left, facing);
        boolean rightLinked = connects(world, pos, right, facing);
        if (leftLinked && rightLinked) {
            return Shape.MIDDLE;
        }
        if (leftLinked) {
            return Shape.RIGHT; // Unido por la izquierda: el brazo queda a la derecha
        }
        if (rightLinked) {
            return Shape.LEFT;
        }
        return Shape.SINGLE;
    }

    /** ¿El sofá de al lado forma parte de la misma fila? (mismo sentido, o la esquina que nos mira de lado) */
    private boolean connects(BlockView world, BlockPos pos, Direction side, Direction facing) {
        BlockState neighbor = world.getBlockState(pos.offset(side));
        if (!neighbor.isOf(this)) {
            return false;
        }
        Direction neighborFacing = neighbor.get(FACING);
        return neighborFacing == facing || neighborFacing == side.getOpposite();
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
