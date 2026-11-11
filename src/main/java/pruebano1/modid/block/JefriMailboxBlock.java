package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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
import pruebano1.modid.ModItem;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.ModEntities;

import java.util.List;

/**
 * Bloque indestructible de invocación: "Buzón de Jefri penstein".
 * Al hacer clic derecho con la "Orden de arresto", invoca a Jefri penstein
 * iniciando su cinemática de aparición mágica en el cielo con música y edición de cámara estilo TikTok.
 */
public class JefriMailboxBlock extends HorizontalFacingBlock {

    public static final MapCodec<JefriMailboxBlock> CODEC = createCodec(JefriMailboxBlock::new);

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
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            List<JefriPensteinEntity> activeBosses = serverWorld.getEntitiesByClass(
                    JefriPensteinEntity.class,
                    new Box(pos).expand(80.0D),
                    JefriPensteinEntity::isAlive
            );
            if (!activeBosses.isEmpty()) {
                player.sendMessage(
                        Text.literal("Jefri penstein ya ha sido invocado")
                                .formatted(Formatting.RED, Formatting.BOLD),
                        true
                );
                return ActionResult.SUCCESS;
            }

            ItemStack held = player.getMainHandStack();
            boolean hasWarrant = held.isOf(ModItem.ARREST_WARRANT) || player.getOffHandStack().isOf(ModItem.ARREST_WARRANT);
            if (!hasWarrant) {
                world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.9F, 0.8F);
                player.sendMessage(
                        Text.literal(" Buzón de Jefri penstein: ")
                                .formatted(Formatting.GOLD, Formatting.BOLD)
                                .append(Text.literal("Necesitas depositar la 'Orden de arresto' de Pi Didi para invocar a Jefri.")
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (!player.isCreative()) {
                if (held.isOf(ModItem.ARREST_WARRANT)) {
                    held.decrement(1);
                } else {
                    player.getOffHandStack().decrement(1);
                }
            }

            JefriPensteinEntity boss = ModEntities.JEFRI_PENSTEIN.create(serverWorld, SpawnReason.EVENT);
            if (boss != null) {
                double spawnX = pos.getX() + 0.5D;
                double spawnY = pos.getY() + 1.0D;
                double spawnZ = pos.getZ() + 0.5D;
                boss.refreshPositionAndAngles(spawnX, spawnY + 14.0D, spawnZ, player.getYaw() + 180.0F, 0.0F);
                boss.startSpawnCinematic(serverWorld, spawnX, spawnY, spawnZ);
                serverWorld.spawnEntity(boss);
            }
        }
        return ActionResult.SUCCESS;
    }
}
