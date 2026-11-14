package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
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
import pruebano1.modid.ModSounds;

/**
 * "Mesa Draftea Diaz": mesa de apuestas de cartas de 5x3 bloques.
 * Clic derecho con un lingote o gema: se consume la apuesta, la casa reparte cartas ~2 segundos
 * y puede salir un premio o nada (ver {@link MesaDrafteaDiazRewards}).
 *
 * Cada uno de los 15 bloques guarda su posición en PART. El del centro (PART 7) es el que
 * lleva la partida en curso (BET y DEAL).
 */
public class MesaDrafteaDiazBlock extends HorizontalFacingBlock {

    public static final MapCodec<MesaDrafteaDiazBlock> CODEC = createCodec(MesaDrafteaDiazBlock::new);

    /** Medidas: 5 de largo (a lo ancho del jugador) por 3 de fondo. */
    public static final int HALF_LENGTH = 2;
    public static final int HALF_DEPTH = 1;
    private static final int LENGTH = HALF_LENGTH * 2 + 1;
    public static final int CENTER_PART = HALF_LENGTH + HALF_DEPTH * LENGTH; // 7

    public static final IntProperty PART = IntProperty.of("part", 0, LENGTH * (HALF_DEPTH * 2 + 1) - 1);
    /** 0 = mesa libre; 1..4 = hay una apuesta en curso del nivel (BET - 1). Solo en el centro. */
    public static final IntProperty BET = IntProperty.of("bet", 0, MesaDrafteaDiazRewards.LOSE_CHANCE.length);
    /** Carta que se está repartiendo (solo en el centro). */
    public static final IntProperty DEAL = IntProperty.of("deal", 0, 3);

    /** Ticks entre cada carta repartida (20 ticks = 1 segundo): 4 x 10 = 2 segundos. */
    private static final int DEAL_DELAY = 10;

    private static final int PURPLE = 0x9B5CFF;

    // Formas: tablero (con baranda acolchada en los bordes) y pedestal bajo la fila central
    private static final VoxelShape TOP = Block.createCuboidShape(0, 10, 0, 16, 13, 16);
    private static final VoxelShape TOP_RAIL = Block.createCuboidShape(0, 10, 0, 16, 15, 16);
    private static final VoxelShape PEDESTAL_NS = Block.createCuboidShape(0, 0, 3, 16, 10, 13);
    private static final VoxelShape PEDESTAL_EW = Block.createCuboidShape(3, 0, 0, 13, 10, 16);

    public MesaDrafteaDiazBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(PART, CENTER_PART)
                .with(BET, 0)
                .with(DEAL, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, BET, DEAL);
    }

    // ------------------------------------------------------------------
    // Geometría de las 15 partes
    // ------------------------------------------------------------------

    /** Posición a lo largo (-2..2); positivo = a la derecha según FACING (FACING girado en sentido horario). */
    public static int partX(int part) {
        return part % LENGTH - HALF_LENGTH;
    }

    /** Posición a lo hondo (-1..1): +1 = lado de los jugadores (FACING), -1 = lado del crupier. */
    public static int partZ(int part) {
        return part / LENGTH - HALF_DEPTH;
    }

    private static int toPart(int x, int z) {
        return (x + HALF_LENGTH) + (z + HALF_DEPTH) * LENGTH;
    }

    private static BlockPos offset(BlockPos center, Direction facing, int x, int z) {
        Direction right = facing.rotateYClockwise();
        return center.add(right.getOffsetX() * x + facing.getOffsetX() * z, 0,
                right.getOffsetZ() * x + facing.getOffsetZ() * z);
    }

    private static BlockPos centerOf(BlockPos pos, BlockState state) {
        int part = state.get(PART);
        return offset(pos, state.get(FACING), -partX(part), -partZ(part));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int part = state.get(PART);
        int x = partX(part);
        int z = partZ(part);
        boolean edge = Math.abs(x) == HALF_LENGTH || Math.abs(z) == HALF_DEPTH;
        VoxelShape top = edge ? TOP_RAIL : TOP;
        if (z == 0 && Math.abs(x) < HALF_LENGTH) {
            Direction.Axis axis = state.get(FACING).getAxis();
            return VoxelShapes.union(top, axis == Direction.Axis.Z ? PEDESTAL_NS : PEDESTAL_EW);
        }
        return top;
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    // ------------------------------------------------------------------
    // Colocar y romper (las 15 partes van juntas)
    // ------------------------------------------------------------------

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        // La mesa mira hacia el jugador que la coloca: ese lado es el de los jugadores, el otro el del crupier
        Direction facing = ctx.getHorizontalPlayerFacing().getOpposite();
        BlockPos center = ctx.getBlockPos();
        World world = ctx.getWorld();
        for (int x = -HALF_LENGTH; x <= HALF_LENGTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
                BlockPos p = offset(center, facing, x, z);
                if (!world.getWorldBorder().contains(p) || !world.getBlockState(p).canReplace(ctx)) {
                    if (!world.isClient() && ctx.getPlayer() != null) {
                        ctx.getPlayer().sendMessage(Text.literal("No hay espacio: la Mesa Draftea Diaz ocupa 5x3 bloques.")
                                .formatted(Formatting.RED), true);
                    }
                    return null;
                }
            }
        }
        return this.getDefaultState().with(FACING, facing).with(PART, CENTER_PART);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient()) {
            return;
        }
        Direction facing = state.get(FACING);
        for (int x = -HALF_LENGTH; x <= HALF_LENGTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                // FORCE_STATE: que las partes ya puestas no se "rompan" al no ver todavía a sus vecinas
                world.setBlockState(offset(pos, facing, x, z),
                        this.getDefaultState().with(FACING, facing).with(PART, toPart(x, z)),
                        Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
        // Avisar a los bloques de alrededor ya con la mesa completa
        for (int x = -HALF_LENGTH; x <= HALF_LENGTH; x++) {
            for (int z = -HALF_DEPTH; z <= HALF_DEPTH; z++) {
                BlockPos p = offset(pos, facing, x, z);
                world.updateNeighbors(p, this);
            }
        }
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        if (direction.getAxis().isHorizontal()) {
            Direction facing = state.get(FACING);
            Direction right = facing.rotateYClockwise();
            int part = state.get(PART);
            int nx = partX(part) + right.getOffsetX() * direction.getOffsetX() + right.getOffsetZ() * direction.getOffsetZ();
            int nz = partZ(part) + facing.getOffsetX() * direction.getOffsetX() + facing.getOffsetZ() * direction.getOffsetZ();
            boolean inside = Math.abs(nx) <= HALF_LENGTH && Math.abs(nz) <= HALF_DEPTH;
            if (inside && !(neighborState.isOf(this)
                    && neighborState.get(FACING) == facing
                    && neighborState.get(PART) == toPart(nx, nz))) {
                // Falta una parte de la mesa (se rompió): esta también desaparece, sin soltar nada
                return Blocks.AIR.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    /**
     * Al romper cualquier parte desaparecen las demás. Solo el bloque central suelta la mesa
     * (lo dice su loot table), así sale 1 sola. En creativo se quita el centro sin soltar nada.
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player.isCreative() && state.get(PART) != CENTER_PART) {
            BlockPos centerPos = centerOf(pos, state);
            if (world.getBlockState(centerPos).isOf(this)) {
                world.setBlockState(centerPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // ------------------------------------------------------------------
    // Apostar
    // ------------------------------------------------------------------

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        MesaDrafteaDiazRewards.Bet bet = MesaDrafteaDiazRewards.findBet(stack);
        if (bet == null) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
        BlockPos centerPos = centerOf(pos, state);
        BlockState center = world.getBlockState(centerPos);
        if (!center.isOf(this) || center.get(PART) != CENTER_PART) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (center.get(BET) > 0) {
            player.sendMessage(Text.literal("La mesa está repartiendo, espera a que termine la mano").formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }
        if (!player.isCreative() && stack.getCount() < bet.count()) {
            player.sendMessage(Text.literal("Necesitas " + bet.count() + " ")
                    .append(stack.getName())
                    .append(" para apostar en esta mesa")
                    .formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }

        if (!player.isCreative()) {
            stack.decrement(bet.count());
        }
        player.sendMessage(Text.literal("Apostaste " + bet.count() + " ")
                .append(new ItemStack(bet.item()).getName())
                .append(". Repartiendo cartas...")
                .formatted(Formatting.LIGHT_PURPLE), true);

        world.playSound(null, centerPos, ModSounds.DRAFTEA_COIN, SoundCategory.BLOCKS, 0.8F, 0.8F);
        world.playSound(null, centerPos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, 0.8F);
        world.setBlockState(centerPos, center.with(BET, bet.tier() + 1).with(DEAL, 0), Block.NOTIFY_LISTENERS);
        world.scheduleBlockTick(centerPos, this, DEAL_DELAY);
        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            player.sendMessage(
                    Text.literal("Mesa Draftea Diaz: ").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                            .append(Text.literal("apuesta lingotes o gemas (ej. 8 de hierro o 2 diamantes). "
                                    + "Más raro = mejores premios.").formatted(Formatting.WHITE)),
                    true
            );
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int bet = state.get(BET);
        if (state.get(PART) != CENTER_PART || bet == 0) {
            return;
        }
        int deal = state.get(DEAL);
        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 1.0D;
        double cz = pos.getZ() + 0.5D;

        if (deal < 3) {
            // Una carta más sobre el tapete
            world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1.0F, 1.0F + deal * 0.2F);
            world.spawnParticles(ParticleTypes.ENCHANT, cx, cy + 0.2D, cz, 8, 0.6D, 0.1D, 0.4D, 0.4D);
            world.setBlockState(pos, state.with(DEAL, deal + 1), Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, this, DEAL_DELAY);
            return;
        }

        // Fin de la mano: decidir el resultado
        world.setBlockState(pos, state.with(BET, 0).with(DEAL, 0), Block.NOTIFY_LISTENERS);
        PlayerEntity player = world.getClosestPlayer(cx, cy, cz, 8.0D, false);
        int category = MesaDrafteaDiazRewards.roll(bet - 1, random);

        if (category == MesaDrafteaDiazRewards.LOSE) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 1.0F, 0.6F);
            world.spawnParticles(ParticleTypes.SMOKE, cx, cy + 0.2D, cz, 15, 0.5D, 0.1D, 0.3D, 0.01D);
            if (player != null) {
                player.sendMessage(Text.literal("La casa gana: perdiste tu apuesta").formatted(Formatting.GRAY), true);
            }
            return;
        }

        ItemStack prize = MesaDrafteaDiazRewards.createPrize(category, world, random);
        Direction front = state.get(FACING);
        Block.dropStack(world, pos.offset(front), front, prize.copy());

        boolean big = category == MesaDrafteaDiazRewards.CATEGORY_HIGH;
        world.playSound(null, pos, big ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.BLOCKS, big ? 0.8F : 1.0F, 1.0F);
        world.spawnParticles(new DustParticleEffect(PURPLE, 1.2F), cx, cy + 0.3D, cz, 20, 0.7D, 0.2D, 0.5D, 0.0D);
        if (big) {
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy + 0.5D, cz, 40, 0.6D, 0.4D, 0.6D, 0.3D);
        }
        if (player != null) {
            Formatting color = big ? Formatting.GOLD : Formatting.GREEN;
            player.sendMessage(Text.literal(big ? "Gran premio: " : "Ganaste: ").formatted(color, Formatting.BOLD)
                    .append(Text.literal(prize.getCount() + "x ").formatted(Formatting.WHITE))
                    .append(prize.getName().copy().formatted(Formatting.WHITE)), true);
        }
    }
}
