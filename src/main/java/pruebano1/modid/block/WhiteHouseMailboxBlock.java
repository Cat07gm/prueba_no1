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
import pruebano1.modid.entity.CharlieKirkEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.world.CasaBlancaStructure;

import java.util.List;

/**
 * Bloque indestructible de invocación: "Buzón de la Casa Blanca".
 * Ubicado en la entrada exterior de la dungeon "La Casa Blanca".
 * Al depositar el "Permiso para portar armas", invoca a Charlie Kirk dentro de la
 * Gran Zona Principal de Conferencias esperando a que el jugador entre a enfrentarlo.
 */
public class WhiteHouseMailboxBlock extends HorizontalFacingBlock {

    public static final MapCodec<WhiteHouseMailboxBlock> CODEC = createCodec(WhiteHouseMailboxBlock::new);

    private static final VoxelShape POST_SHAPE = Block.createCuboidShape(6.5D, 0.0D, 6.5D, 9.5D, 9.0D, 9.5D);
    private static final VoxelShape BOX_NORTH_SOUTH = VoxelShapes.union(
            POST_SHAPE,
            Block.createCuboidShape(4.0D, 9.0D, 2.0D, 12.0D, 16.0D, 14.0D)
    );
    private static final VoxelShape BOX_EAST_WEST = VoxelShapes.union(
            POST_SHAPE,
            Block.createCuboidShape(2.0D, 9.0D, 4.0D, 14.0D, 16.0D, 12.0D)
    );

    public WhiteHouseMailboxBlock(Settings settings) {
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
            List<CharlieKirkEntity> activeBosses = serverWorld.getEntitiesByClass(
                    CharlieKirkEntity.class,
                    new Box(pos).expand(220.0D),
                    CharlieKirkEntity::isAlive
            );
            if (!activeBosses.isEmpty()) {
                player.sendMessage(
                        Text.literal("Charlie Kirk ya ha sido invocado")
                                .formatted(Formatting.RED, Formatting.BOLD),
                        true
                );
                return ActionResult.SUCCESS;
            }

            ItemStack held = player.getMainHandStack();
            boolean hasPermit = held.isOf(ModItem.GUN_PERMIT) || player.getOffHandStack().isOf(ModItem.GUN_PERMIT);
            if (!hasPermit) {
                world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.9F, 0.85F);
                player.sendMessage(
                        Text.literal("Buzón de la Casa Blanca: ")
                                .formatted(Formatting.BLUE, Formatting.BOLD)
                                .append(Text.literal("Necesitas depositar el 'Permiso para portar armas' para invocar a Charlie Kirk.")
                                        .formatted(Formatting.AQUA)),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (!BossSummonHelper.ensureAllPlayersPresent(
                    serverWorld,
                    pos,
                    player,
                    "Buzón de la Casa Blanca",
                    "Charlie Kirk",
                    "La Casa Blanca",
                    100.0D,
                    p -> CasaBlancaStructure.isPosInCasaBlanca(serverWorld, p)
            )) {
                return ActionResult.SUCCESS;
            }

            if (!player.isCreative()) {
                if (held.isOf(ModItem.GUN_PERMIT)) {
                    held.decrement(1);
                } else {
                    player.getOffHandStack().decrement(1);
                }
            }

            CharlieKirkEntity boss = ModEntities.CHARLIE_KIRK.create(serverWorld, SpawnReason.EVENT);
            if (boss != null) {
                boolean insideWhiteHouse = CasaBlancaStructure.isPosInCasaBlanca(serverWorld, pos);
                CasaBlancaStructure.WhiteHouseAnchor anchor = insideWhiteHouse
                        ? CasaBlancaStructure.getOrResolveUniqueAnchor(serverWorld)
                        : null;

                if (anchor != null) {
                    BlockPos stagePos = anchor.conferenceStageSpawnPos();
                    // Asegurar que el chunk del Salón Principal de Conferencias esté cargado antes de spawnear
                    serverWorld.getChunk(stagePos.getX() >> 4, stagePos.getZ() >> 4);
                    double spawnX = stagePos.getX() + 0.5D;
                    double spawnY = stagePos.getY();
                    double spawnZ = stagePos.getZ() + 0.5D;
                    boss.refreshPositionAndAngles(spawnX, spawnY, spawnZ, 0.0F, 0.0F);
                    boss.prepareWaitingInConferenceHall(
                            serverWorld,
                            spawnX, spawnY, spawnZ,
                            new BlockPos(anchor.centerX(), anchor.baseY(), anchor.centerZ())
                    );
                    serverWorld.spawnEntity(boss);

                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 1.2F, 1.1F);
                    Text encounterMsg = Text.literal(
                            player.getName().getString()
                                    + " colocó el Permiso para portar armas en el Buzón de la Casa Blanca. ¡Charlie Kirk espera en el interior de la Casa Blanca!"
                    ).formatted(Formatting.BLUE, Formatting.BOLD);
                    serverWorld.getServer().getPlayerManager().broadcast(encounterMsg, false);
                } else {
                    double spawnX = pos.getX() + 0.5D;
                    double spawnY = pos.getY() + 1.0D;
                    double spawnZ = pos.getZ() + 0.5D;
                    boss.refreshPositionAndAngles(spawnX, spawnY, spawnZ, player.getYaw() + 180.0F, 0.0F);
                    boss.startSpawnCinematic(serverWorld, spawnX, spawnY, spawnZ);
                    serverWorld.spawnEntity(boss);

                    Text encounterMsg = Text.literal(player.getName().getString() + " se acaba de topar con " + boss.getName().getString())
                            .formatted(Formatting.BLUE, Formatting.BOLD);
                    serverWorld.getServer().getPlayerManager().broadcast(encounterMsg, false);
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}
