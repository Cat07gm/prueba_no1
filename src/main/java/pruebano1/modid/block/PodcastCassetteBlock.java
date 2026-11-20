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
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;
import pruebano1.modid.entity.JoseTorresEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.world.CasaJoseTorresStructure;

import java.util.List;

/**
 * Bloque indestructible en supervivencia: "Cassete de Podcast".
 * Al hacer clic derecho con el "Acordeon de José Torres", invoca al boss José Torres
 * iniciando su cinemática de entrada de 5 segundos tocando el acordeón al ritmo de su música.
 */
public class PodcastCassetteBlock extends HorizontalFacingBlock {

    public static final MapCodec<PodcastCassetteBlock> CODEC = createCodec(PodcastCassetteBlock::new);

    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.createCuboidShape(1.0D, 0.0D, 2.2D, 15.0D, 5.5D, 13.0D);
    private static final VoxelShape SHAPE_EAST_WEST = Block.createCuboidShape(2.2D, 0.0D, 1.0D, 13.0D, 5.5D, 15.0D);

    public PodcastCassetteBlock(Settings settings) {
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
            return SHAPE_EAST_WEST;
        }
        return SHAPE_NORTH_SOUTH;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            List<JoseTorresEntity> activeBosses = serverWorld.getEntitiesByClass(
                    JoseTorresEntity.class,
                    new Box(pos).expand(80.0D),
                    JoseTorresEntity::isAlive
            );
            if (!activeBosses.isEmpty()) {
                player.sendMessage(
                        Text.literal("José Torres ya ha sido invocado")
                                .formatted(Formatting.RED, Formatting.BOLD),
                        true
                );
                return ActionResult.SUCCESS;
            }

            ItemStack held = player.getMainHandStack();
            boolean hasAccordion = held.isOf(ModItem.JOSE_TORRES_ACCORDION)
                    || player.getOffHandStack().isOf(ModItem.JOSE_TORRES_ACCORDION);
            if (!hasAccordion) {
                world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.BLOCKS, 0.9F, 1.1F);
                player.sendMessage(
                        Text.literal("Cassete de Podcast: ")
                                .formatted(Formatting.GOLD, Formatting.BOLD)
                                .append(Text.literal("Necesitas usar el 'Acordeon de José Torres' para invocar a José Torres.")
                                        .formatted(Formatting.YELLOW)),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (!BossSummonHelper.ensureAllPlayersPresent(
                    serverWorld,
                    pos,
                    player,
                    "Cassete de Podcast",
                    "José Torres",
                    "la Casa de José Torres",
                    120.0D,
                    p -> CasaJoseTorresStructure.isPosInCasaJoseTorres(serverWorld, p)
            )) {
                return ActionResult.SUCCESS;
            }

            if (!player.isCreative()) {
                if (held.isOf(ModItem.JOSE_TORRES_ACCORDION)) {
                    held.decrement(1);
                } else {
                    player.getOffHandStack().decrement(1);
                }
            }

            JoseTorresEntity boss = ModEntities.JOSE_TORRES.create(serverWorld, SpawnReason.EVENT);
            if (boss != null) {
                double spawnX = pos.getX() + 0.5D;
                double spawnY = pos.getY() + 1.0D;
                double spawnZ = pos.getZ() + 0.5D;
                boss.refreshPositionAndAngles(spawnX, spawnY, spawnZ, player.getYaw() + 180.0F, 0.0F);
                boss.startSpawnCinematic(serverWorld, spawnX, spawnY, spawnZ);
                serverWorld.spawnEntity(boss);

                Text encounterMsg = Text.literal(player.getName().getString() + " se acaba de topar con " + boss.getName().getString())
                        .formatted(Formatting.GOLD, Formatting.BOLD);
                serverWorld.getServer().getPlayerManager().broadcast(encounterMsg, false);
            }
        }
        return ActionResult.SUCCESS;
    }
}
