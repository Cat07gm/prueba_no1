package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModItem;
import pruebano1.modid.ModSounds;

/**
 * Máquina tragamonedas "Draftea" (2 bloques de alto).
 * Clic derecho con una Tony Coin: consume la moneda, gira la ruleta ~2 segundos
 * y suelta un premio aleatorio de {@link DrafteaRewards} por el frente.
 */
public class DrafteaBlock extends HorizontalFacingBlock implements BlockEntityProvider {

    public static final MapCodec<DrafteaBlock> CODEC = createCodec(DrafteaBlock::new);

    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    /**
     * Pausa (en ticks, 20 ticks = 1 segundo) entre cada "clic" de la ruleta.
     * Empieza rápido y se va frenando. La suma (80 ticks) es la duración total del giro: 4 segundos.
     */
    private static final int[] SPIN_DELAYS = {
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
            3, 3, 3, 3,
            4, 4,
            5, 5, 5,
            6, 7, 8
    };

    /** 0 = en reposo; 1..N = paso actual del giro (solo se usa en la mitad inferior). */
    public static final IntProperty SPIN = IntProperty.of("spin", 0, SPIN_DELAYS.length);

    /** Paso del giro en el que se detiene cada rodillo (izquierdo, central, derecho). El último coincide con el final. */
    private static final int[] REEL_STOP_STEPS = {SPIN_DELAYS.length - 6, SPIN_DELAYS.length - 3, SPIN_DELAYS.length};

    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public DrafteaBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(SPIN, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SPIN);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // ------------------------------------------------------------------
    // Colocación y ruptura de las dos mitades (mismo esquema que una puerta)
    // ------------------------------------------------------------------

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() < world.getTopYInclusive() && world.getBlockState(pos.up()).canReplace(ctx)) {
            return this.getDefaultState()
                    .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                    .with(HALF, DoubleBlockHalf.LOWER);
        }
        return null; // No hay espacio para la mitad de arriba
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER).with(SPIN, 0), Block.NOTIFY_ALL);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canPlaceAt(state, world, pos);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        DoubleBlockHalf half = state.get(HALF);
        boolean towardsPartner = (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);
        if (direction.getAxis() == Direction.Axis.Y && towardsPartner) {
            // Si la otra mitad desapareció, esta también se quita
            if (!neighborState.isOf(this) || neighborState.get(HALF) == half) {
                return Blocks.AIR.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            boolean upper = state.get(HALF) == DoubleBlockHalf.UPPER;
            BlockPos otherPos = upper ? pos.down() : pos.up();
            BlockState other = world.getBlockState(otherPos);
            if (other.isOf(this) && other.get(HALF) != state.get(HALF)) {
                // Solo se puede romper en creativo: al quitar una mitad se quita también la otra (sin soltar nada)
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, WorldEvents.BLOCK_BROKEN, otherPos, Block.getRawIdFromState(other));
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // ------------------------------------------------------------------
    // Uso de la máquina
    // ------------------------------------------------------------------

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(ModItem.TONY_COIN)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }

        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        BlockState lower = world.getBlockState(lowerPos);
        if (!lower.isOf(this)) {
            return ActionResult.PASS;
        }

        if (lower.get(SPIN) > 0) {
            if (!world.isClient()) {
                player.sendMessage(Text.literal("La Draftea ya está girando").formatted(Formatting.RED), true);
            }
            return ActionResult.SUCCESS;
        }

        if (!world.isClient()) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            world.playSound(null, lowerPos, ModSounds.DRAFTEA_COIN, SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(lowerPos, lower.with(SPIN, 1), Block.NOTIFY_ALL);
            world.scheduleBlockTick(lowerPos, this, SPIN_DELAYS[0]);

            // El premio se decide al meter la moneda; los rodillos se van deteniendo en él
            DrafteaBlockEntity screen = getScreen(world, lowerPos);
            if (screen != null) {
                Random random = world.getRandom();
                screen.setPendingReward(DrafteaRewards.pickIndex(random));
                for (int i = 0; i < DrafteaBlockEntity.REELS; i++) {
                    screen.setReel(i, DrafteaRewards.randomDisplayItem(random, screen.getReel(i)));
                }
                screen.sync();
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            player.sendMessage(
                    Text.literal("Draftea: ").formatted(Formatting.GREEN, Formatting.BOLD)
                            .append(Text.literal("Inserta una Tony Coin para girar.").formatted(Formatting.WHITE)),
                    true
            );
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int step = state.get(SPIN);
        if (state.get(HALF) != DoubleBlockHalf.LOWER || step == 0) {
            return;
        }

        // "Clic" de la ruleta: el tono baja conforme la ruleta se frena
        float pitch = 1.8F - 1.1F * step / SPIN_DELAYS.length;
        world.playSound(null, pos, ModSounds.DRAFTEA_SPIN, SoundCategory.BLOCKS, 0.8F, pitch);

        // Premio decidido al meter la moneda (si falta o la tabla cambió, se elige otro)
        DrafteaBlockEntity screen = getScreen(world, pos);
        int rewardIndex = screen != null ? screen.getPendingReward() : -1;
        if (rewardIndex < 0 || rewardIndex >= DrafteaRewards.REWARDS.size()) {
            rewardIndex = DrafteaRewards.pickIndex(random);
            if (screen != null) {
                screen.setPendingReward(rewardIndex);
            }
        }
        DrafteaRewards.Reward reward = DrafteaRewards.REWARDS.get(rewardIndex);
        boolean finished = step >= SPIN_DELAYS.length;

        // Actualizar la pantalla: cada rodillo sigue cambiando hasta su paso de parada
        if (screen != null) {
            for (int i = 0; i < DrafteaBlockEntity.REELS; i++) {
                if (step >= REEL_STOP_STEPS[i]) {
                    if (step == REEL_STOP_STEPS[i] && !finished) {
                        world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.4F, 1.6F);
                    }
                    screen.setReel(i, reward.item());
                } else {
                    screen.setReel(i, DrafteaRewards.randomDisplayItem(random, screen.getReel(i)));
                }
            }
            if (finished) {
                screen.setPendingReward(-1); // Los rodillos se quedan mostrando el último premio
            }
            screen.sync();
        }

        if (!finished) {
            world.setBlockState(pos, state.with(SPIN, step + 1), Block.NOTIFY_LISTENERS);
            world.scheduleBlockTick(pos, this, SPIN_DELAYS[step]);
            return;
        }

        // Fin del giro: soltar premio
        world.setBlockState(pos, state.with(SPIN, 0), Block.NOTIFY_ALL);
        Direction front = state.get(FACING);
        Block.dropStack(world, pos, front, reward.toStack());

        world.playSound(null, pos, ModSounds.DRAFTEA_WIN, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.spawnParticles(new DustParticleEffect(0x9B5CFF, 1.2F),
                pos.getX() + 0.5D + front.getOffsetX() * 0.6D,
                pos.getY() + 1.4D,
                pos.getZ() + 0.5D + front.getOffsetZ() * 0.6D,
                20, 0.4D, 0.4D, 0.4D, 0.0D);
    }

    // ------------------------------------------------------------------
    // Pantalla (entidad de bloque)
    // ------------------------------------------------------------------

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DrafteaBlockEntity(pos, state);
    }

    /** La pantalla vive en la mitad superior; recibe la posición de la mitad inferior. */
    private static @Nullable DrafteaBlockEntity getScreen(World world, BlockPos lowerPos) {
        return world.getBlockEntity(lowerPos.up()) instanceof DrafteaBlockEntity screen ? screen : null;
    }
}
