package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Bloque indestructible de invocación: "Buzón de Jefri penstein".
 * Tiene forma de buzón clásico de casa sobre un poste y servirá como altar de invocación de Jefri.
 */
public class JefriMailboxBlock extends HorizontalFacingBlock {

    public static final MapCodec<JefriMailboxBlock> CODEC = createCodec(JefriMailboxBlock::new);

    // Poste central + caja del buzón arriba (orientación Norte/Sur y Este/Oeste)
    private static final VoxelShape POST_SHAPE = Block.createCuboidShape(6.5D, 0.0D, 6.5D, 9.5D, 9.0D, 9.5D);
    private static final VoxelShape BOX_NORTH_SOUTH = VoxelShapes.union(
            POST_SHAPE,
            Block.createCuboidShape(4.0D, 9.0D, 2.0D, 12.0D, 16.0D, 14.0D)
    );
    private static final VoxelShape BOX_EAST_WEST = VoxelShapes.union(
            POST_SHAPE,
            Block.createCuboidShape(2.0D, 9.0D, 4.0D, 14.0D, 16.0D, 12.0D)
    );

    public JefriMailboxBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction dir = state.get(FACING);
        if (dir == Direction.EAST || dir == Direction.WEST) {
            return BOX_EAST_WEST;
        }
        return BOX_NORTH_SOUTH;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.8F, 1.2F);
            player.sendMessage(
                    Text.literal("📬 Buzón de Jefri Penstein: ")
                            .formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("El buzón está sellado... pronto podrás invocar a Jefri aquí.")
                                    .formatted(Formatting.YELLOW)),
                    true
            );
        }
        return ActionResult.SUCCESS;
    }
}
