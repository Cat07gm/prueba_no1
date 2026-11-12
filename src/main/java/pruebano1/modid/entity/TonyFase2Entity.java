package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.block.DrafteaBlock;
import pruebano1.modid.world.DrafteaCasinoManager;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * TonyFase2 (Sudadera Blanca): Clientes pasivos del casino "Draftea Tony Diaz".
 * Recorren el interior del casino y se detienen frente a máquinas Draftea con animación de juego.
 */
public class TonyFase2Entity extends TonyCasinoNpcEntity {

    private static final TrackedData<Boolean> AT_MACHINE = DataTracker.registerData(
            TonyFase2Entity.class, TrackedDataHandlerRegistry.BOOLEAN
    );

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation USE_SLOT_ANIM = RawAnimation.begin().thenLoop("use_slot");

    @Nullable
    private BlockPos currentSlotTarget;
    private float slotLookYaw;
    private int slotWaitTicks = 0;

    public TonyFase2Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createTonyFase2Attributes() {
        return createTonyNpcAttributes(false);
    }

    @Override
    protected boolean isStationaryNpc() {
        return false;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(AT_MACHINE, false);
    }

    public boolean isAtMachine() {
        return this.dataTracker.get(AT_MACHINE);
    }

    public void setAtMachine(boolean atMachine) {
        this.dataTracker.set(AT_MACHINE, atMachine);
    }

    /** Configura un cliente inicial ubicado directamente frente a una máquina Draftea. */
    public void assignInitialSlotSpot(BlockPos standPos, BlockPos machinePos, Direction machineFacing) {
        float lookYaw = machineFacing.getOpposite().getPositiveHorizontalDegrees();
        this.anchorAt(new Vec3d(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5), lookYaw, true);
        this.currentSlotTarget = standPos.toImmutable();
        this.slotLookYaw = yawToward(standPos.getX() + 0.5, standPos.getZ() + 0.5,
                machinePos.getX() + 0.5, machinePos.getZ() + 0.5);
        this.slotWaitTicks = 180 + this.random.nextInt(140);
        this.setAtMachine(true);
        this.setYaw(this.slotLookYaw);
        this.setBodyYaw(this.slotLookYaw);
        this.setHeadYaw(this.slotLookYaw);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new StayInsideCasinoGoal(this));
        this.goalSelector.add(2, new UseDrafteaMachineGoal(this));
        this.goalSelector.add(3, new CasinoPatrolGoal(this));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 5.0F, 0.015F) {
            @Override
            public boolean canStart() {
                return !TonyFase2Entity.this.isAtMachine() && super.canStart();
            }
        });
        this.goalSelector.add(5, new LookAroundGoal(this) {
            @Override
            public boolean canStart() {
                return !TonyFase2Entity.this.isAtMachine() && super.canStart();
            }
        });
    }

    private boolean isInsideCasinoWalkBounds(BlockPos pos) {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return true;
        }
        DrafteaCasinoManager.CasinoAnchor anchor = DrafteaCasinoManager.getAnchor(serverWorld.getServer());
        if (anchor == null) {
            return true;
        }
        int dx = pos.getX() - anchor.centerX();
        int dy = pos.getY() - anchor.baseY();
        int dz = pos.getZ() - anchor.centerZ();
        return dx >= -34 && dx <= 34 && dz >= -28 && dz <= 20 && dy >= 0 && dy <= 16;
    }

    private static float yawToward(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("AtMachine", this.isAtMachine());
        nbt.putFloat("SlotLookYaw", this.slotLookYaw);
        nbt.putInt("SlotWaitTicks", this.slotWaitTicks);
        if (this.currentSlotTarget != null) {
            nbt.putLong("SlotTargetPos", this.currentSlotTarget.asLong());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setAtMachine(nbt.getBoolean("AtMachine"));
        this.slotLookYaw = nbt.getFloat("SlotLookYaw");
        this.slotWaitTicks = nbt.getInt("SlotWaitTicks");
        if (nbt.contains("SlotTargetPos")) {
            this.currentSlotTarget = BlockPos.fromLong(nbt.getLong("SlotTargetPos"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 5, state -> {
            if (state.isMoving()) {
                state.setAndContinue(WALK_ANIM);
            } else if (this.isAtMachine()) {
                state.setAndContinue(USE_SLOT_ANIM);
            } else {
                state.setAndContinue(IDLE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }

    /** Mantiene a los clientes del casino dentro de las salas del edificio si se acercan demasiado a las puertas. */
    private static final class StayInsideCasinoGoal extends Goal {
        private final TonyFase2Entity entity;

        StayInsideCasinoGoal(TonyFase2Entity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return this.entity.isCasinoBound()
                    && this.entity.getHomePos() != null
                    && !this.entity.isInsideCasinoWalkBounds(this.entity.getBlockPos());
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart() && !this.entity.getNavigation().isIdle();
        }

        @Override
        public void start() {
            this.entity.setAtMachine(false);
            Vec3d home = this.entity.getHomePos();
            if (home != null) {
                this.entity.getNavigation().startMovingTo(home.x, home.y, home.z, 1.05D);
            }
        }
    }

    /** Busca una máquina Draftea cercana, se coloca frente a ella y reproduce la animación de jugar/apostar. */
    private static final class UseDrafteaMachineGoal extends Goal {
        private final TonyFase2Entity entity;
        private int searchCooldown;
        private int walkTimeout;

        UseDrafteaMachineGoal(TonyFase2Entity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (this.entity.slotWaitTicks > 0 && this.entity.currentSlotTarget != null) {
                return true;
            }
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return false;
            }
            this.searchCooldown = 90 + this.entity.random.nextInt(120);
            return this.pickNearbyMachine();
        }

        @Override
        public boolean shouldContinue() {
            return this.entity.currentSlotTarget != null
                    && (this.entity.slotWaitTicks > 0 || (!this.entity.getNavigation().isIdle() && this.walkTimeout > 0));
        }

        @Override
        public void start() {
            if (this.entity.currentSlotTarget == null) {
                return;
            }
            if (this.entity.slotWaitTicks > 0 && this.isNearTarget()) {
                this.entity.setAtMachine(true);
                return;
            }
            this.entity.setAtMachine(false);
            this.walkTimeout = 160;
            this.entity.getNavigation().startMovingTo(
                    this.entity.currentSlotTarget.getX() + 0.5,
                    this.entity.currentSlotTarget.getY(),
                    this.entity.currentSlotTarget.getZ() + 0.5,
                    0.95D
            );
        }

        @Override
        public void tick() {
            if (this.entity.currentSlotTarget == null) {
                return;
            }
            if (this.isNearTarget()) {
                this.entity.getNavigation().stop();
                this.entity.setAtMachine(true);
                this.entity.setYaw(this.entity.slotLookYaw);
                this.entity.setBodyYaw(this.entity.slotLookYaw);
                this.entity.setHeadYaw(this.entity.slotLookYaw);
                if (this.entity.slotWaitTicks <= 0) {
                    this.entity.slotWaitTicks = 120 + this.entity.random.nextInt(140);
                } else {
                    this.entity.slotWaitTicks--;
                }
            } else {
                this.walkTimeout--;
                this.entity.setAtMachine(false);
            }
        }

        @Override
        public void stop() {
            this.entity.setAtMachine(false);
            this.entity.currentSlotTarget = null;
            this.entity.slotWaitTicks = 0;
        }

        private boolean isNearTarget() {
            return this.entity.currentSlotTarget != null
                    && this.entity.squaredDistanceTo(
                            this.entity.currentSlotTarget.getX() + 0.5,
                            this.entity.currentSlotTarget.getY(),
                            this.entity.currentSlotTarget.getZ() + 0.5
                    ) <= 1.6D;
        }

        private boolean pickNearbyMachine() {
            BlockPos origin = this.entity.getBlockPos();
            World world = this.entity.getWorld();
            List<BlockPos> standCandidates = new ArrayList<>();
            List<Float> yawCandidates = new ArrayList<>();

            int baseY = origin.getY();
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -12; dz <= 12; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos pos = new BlockPos(origin.getX() + dx, baseY + dy, origin.getZ() + dz);
                        BlockState state = world.getBlockState(pos);
                        if (!state.isOf(ModBlocks.DRAFTEA) || state.get(DrafteaBlock.HALF) != DoubleBlockHalf.LOWER) {
                            continue;
                        }
                        Direction facing = state.get(DrafteaBlock.FACING);
                        BlockPos stand = pos.offset(facing, 2);
                        if (this.entity.isCasinoBound() && !this.entity.isInsideCasinoWalkBounds(stand)) {
                            continue;
                        }
                        if (world.isAir(stand) && world.isAir(stand.up()) && world.getBlockState(stand.down()).isSolid()) {
                            standCandidates.add(stand.toImmutable());
                            yawCandidates.add(yawToward(stand.getX() + 0.5, stand.getZ() + 0.5,
                                    pos.getX() + 0.5, pos.getZ() + 0.5));
                        }
                    }
                }
            }

            if (standCandidates.isEmpty()) {
                return false;
            }
            int idx = this.entity.random.nextInt(standCandidates.size());
            this.entity.currentSlotTarget = standCandidates.get(idx);
            this.entity.slotLookYaw = yawCandidates.get(idx);
            this.entity.slotWaitTicks = 0;
            return true;
        }
    }

    /** Paseo relajado dentro del casino (o libre si fue invocado fuera con huevo). */
    private static final class CasinoPatrolGoal extends Goal {
        private final TonyFase2Entity entity;
        private double targetX;
        private double targetY;
        private double targetZ;
        private int cooldown = 20;

        CasinoPatrolGoal(TonyFase2Entity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (this.entity.isAtMachine()) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            this.cooldown = 35 + this.entity.random.nextInt(55);
            Vec3d dest = this.pickDestination();
            if (dest == null) {
                return false;
            }
            this.targetX = dest.x;
            this.targetY = dest.y;
            this.targetZ = dest.z;
            return true;
        }

        @Override
        public boolean shouldContinue() {
            return !this.entity.isAtMachine() && !this.entity.getNavigation().isIdle();
        }

        @Override
        public void start() {
            this.entity.getNavigation().startMovingTo(this.targetX, this.targetY, this.targetZ, 0.9D);
        }

        @Nullable
        private Vec3d pickDestination() {
            for (int i = 0; i < 8; i++) {
                Vec3d candidate = NoPenaltyTargeting.find(this.entity, 10, 3);
                if (candidate == null) {
                    continue;
                }
                if (!this.entity.isCasinoBound() || this.entity.isInsideCasinoWalkBounds(BlockPos.ofFloored(candidate))) {
                    return candidate;
                }
            }
            return this.entity.getHomePos();
        }
    }
}
