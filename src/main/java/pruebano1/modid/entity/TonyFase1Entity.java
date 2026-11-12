package pruebano1.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * TonyFase1 (Sudadera Verde): Bartender fijo del Lounge Bar en el casino "Draftea Tony Diaz".
 * No puede moverse de su puesto y alterna animaciones de coctelería (batir bebidas, limpiar vasos y pose de barra).
 */
public class TonyFase1Entity extends TonyCasinoNpcEntity {

    public static final int ACTION_SHAKE_DRINKS = 0;
    public static final int ACTION_WIPE_GLASS = 1;
    public static final int ACTION_IDLE = 2;

    private static final TrackedData<Integer> ACTION_MODE = DataTracker.registerData(
            TonyFase1Entity.class, TrackedDataHandlerRegistry.INTEGER
    );

    private static final RawAnimation SHAKE_ANIM = RawAnimation.begin().thenLoop("shake_drinks");
    private static final RawAnimation WIPE_ANIM = RawAnimation.begin().thenLoop("wipe_glass");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private int cycleTicks = 0;

    public TonyFase1Entity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createTonyFase1Attributes() {
        return createTonyNpcAttributes(true);
    }

    @Override
    protected boolean isStationaryNpc() {
        return true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ACTION_MODE, ACTION_SHAKE_DRINKS);
    }

    @Override
    protected void initGoals() {
        // Permanece fijo atendiendo detrás de la barra sin caminar ni girar fuera de su puesto
    }

    public int getActionMode() {
        return this.dataTracker.get(ACTION_MODE);
    }

    public void setActionMode(int mode) {
        int clamped = (mode < ACTION_SHAKE_DRINKS || mode > ACTION_IDLE) ? ACTION_SHAKE_DRINKS : mode;
        this.dataTracker.set(ACTION_MODE, clamped);
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);
        this.cycleTicks++;

        // Ciclo natural de bartender detrás de la barra (15 segundos por rotación completa):
        // 0..119 ticks (6s): batir bebidas con coctelera
        // 120..239 ticks (6s): limpiar vaso con paño
        // 240..299 ticks (3s): postura relajada mirando la barra
        int phase = Math.floorMod(this.cycleTicks, 300);
        int desiredMode;
        if (phase < 120) {
            desiredMode = ACTION_SHAKE_DRINKS;
        } else if (phase < 240) {
            desiredMode = ACTION_WIPE_GLASS;
        } else {
            desiredMode = ACTION_IDLE;
        }

        if (this.getActionMode() != desiredMode) {
            this.setActionMode(desiredMode);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("ActionMode", this.getActionMode());
        nbt.putInt("CycleTicks", this.cycleTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setActionMode(nbt.getInt("ActionMode"));
        this.cycleTicks = nbt.getInt("CycleTicks");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 6, state -> {
            int mode = this.getActionMode();
            if (mode == ACTION_WIPE_GLASS) {
                state.setAndContinue(WIPE_ANIM);
            } else if (mode == ACTION_IDLE) {
                state.setAndContinue(IDLE_ANIM);
            } else {
                state.setAndContinue(SHAKE_ANIM);
            }
            return PlayState.CONTINUE;
        }));
    }
}
