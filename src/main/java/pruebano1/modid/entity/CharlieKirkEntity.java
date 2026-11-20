package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModEffects;
import pruebano1.modid.ModItem;
import pruebano1.modid.ModParticles;
import pruebano1.modid.ModSounds;
import pruebano1.modid.network.CharlieEpsteinPopupPayload;
import pruebano1.modid.world.CasaBlancaStructure;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Jefe de la 3.ª Dungeon "La Casa Blanca": Charlie Kirk.
 * - 850 HP (425 corazones), 14 de armadura, 0.30 de velocidad y 12.0 de daño base.
 * - Barra de Jefe Azul.
 * - Al invocarlo en el Buzón de la Casa Blanca con el 100% de jugadores presentes, espera en el interior
 *   hasta que cualquier jugador entre al edificio de La Casa Blanca, disparando la cinemática de 48 segundos
 *   (960 ticks) para todos los jugadores.
 * - Al terminar la pantalla en negro en el segundo 48 aparece flotando en el Salón Principal de Conferencias
 *   con partículas de las 19 armas de fuego, y permanece flotando en el aire durante toda la Fase 1 hasta la Fase 2.
 */
public class CharlieKirkEntity extends HostileEntity implements GeoEntity {

    public static final int SPAWN_CINEMATIC_DURATION_TICKS = 960; // 48.0 segundos (960 ticks)
    public static final int BLACKOUT_START_TICK = 860;            // 43.0 segundos
    public static final int TELEPORT_PLAYERS_TICK = 900;          // 45.0 segundos (en medio de la pantalla negra)
    public static final int REVEAL_MODEL_TICK = 956;              // 47.8 segundos (aparece justo al abrirse la imagen en el 48)
    public static final int DEATH_CINEMATIC_DURATION_TICKS = 100; // 5.0 segundos

    public static final int SPECIAL_NONE = 0;
    public static final int SPECIAL_WEAPON_CIRCLE = 1;
    public static final int SPECIAL_TELEKINESIS_EPSTEIN = 2;

    public static final int WEAPON_CIRCLE_COUNT = 8;
    public static final float WEAPON_CIRCLE_RADIUS = 2.65F;
    public static final float WEAPON_CIRCLE_ROT_SPEED = 0.052F;
    public static final int WEAPON_CIRCLE_DURATION_TICKS = 76;     // 3.8 segundos
    public static final int WEAPON_CIRCLE_BAZOOKA_FIRE_TICK = 36;
    private static final int[] WEAPON_CIRCLE_BURST_TICKS = {22, 32, 42, 52};

    public static final int TELEKINESIS_DURATION_TICKS = 60;       // 3.0 segundos
    private static final int ATTACK_COOLDOWN_TICKS = 120;          // ~6.0 segundos entre ataques especiales
    private static final float CIRCLE_BULLET_DAMAGE = 8.0F;        // 4 corazones por bala
    private static final float CIRCLE_ROCKET_DIRECT_DAMAGE = 16.0F;// 8 corazones por impacto de bazooka
    private static final float CIRCLE_ROCKET_SPLASH_DAMAGE = 14.0F;// 7 corazones máx. en área (sin romper bloques)

    private static final double WAKE_OUTSIDE_DUNGEON_RANGE = 26.0D;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation SPAWN_CINEMATIC_ANIM = RawAnimation.begin().thenPlay("spawn_cinematic");
    private static final RawAnimation PHASE2_TRANSFORM_ANIM = RawAnimation.begin().thenPlay("phase2_transform");
    private static final RawAnimation DEATH_CINEMATIC_ANIM = RawAnimation.begin().thenPlay("death_cinematic");
    private static final RawAnimation WEAPON_CIRCLE_ANIM = RawAnimation.begin().thenPlay("weapon_circle");
    private static final RawAnimation TELEKINESIS_ANIM = RawAnimation.begin().thenPlay("telekinesis");

    private static final DustParticleEffect PRESIDENTIAL_BLUE_DUST = new DustParticleEffect(0x2563EB, 1.35F);
    private static final DustParticleEffect TELEKINESIS_PURPLE_DUST = new DustParticleEffect(0xA855F7, 1.25F);
    private static final String CINEMATIC_FROZEN_TAG = "charlie_kirk_cinematic_frozen";

    private static final TrackedData<Boolean> WAITING_FOR_PLAYER = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> IN_COMBAT = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> PHASE_2 = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Integer> SPAWN_CINEMATIC_TICKS = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Boolean> DYING_CINEMATIC = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<BlockPos> DUNGEON_CENTER_POS = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BLOCK_POS
    );
    private static final TrackedData<Boolean> HAS_DUNGEON_CENTER = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Integer> ACTIVE_SPECIAL_ATTACK = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Integer> SPECIAL_ATTACK_TICKS = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Float> WEAPON_CIRCLE_ANGLE = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.FLOAT
    );
    private static final TrackedData<Integer> TELEKINESIS_TARGET_ID = DataTracker.registerData(
            CharlieKirkEntity.class, TrackedDataHandlerRegistry.INTEGER
    );

    private record SavedPlayerPose(double x, double y, double z, float yaw, float pitch) {}

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.prueba_no1.charlie_kirk"),
            BossBar.Color.BLUE,
            BossBar.Style.NOTCHED_10
    );

    private boolean spawnInitialized = false;
    private double anchorGroundY = 64.0D;
    private double waitingAnchorX = 0.0D;
    private double waitingAnchorZ = 0.0D;
    private int deathCinematicTicks = 0;
    private int specialAttackCooldown = 50;
    private int lastSpecialAttackType = SPECIAL_TELEKINESIS_EPSTEIN;
    @Nullable
    private UUID telekinesisTargetUuid = null;
    @Nullable
    private Vec3d telekinesisStartPos = null;
    @Nullable
    private Vec3d telekinesisHoldPos = null;
    private final Map<UUID, SavedPlayerPose> frozenPlayerPoses = new HashMap<>();

    public CharlieKirkEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
        this.experiencePoints = 900;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(WAITING_FOR_PLAYER, false);
        builder.add(IN_COMBAT, false);
        builder.add(PHASE_2, false);
        builder.add(SPAWN_CINEMATIC_TICKS, 0);
        builder.add(DYING_CINEMATIC, false);
        builder.add(DUNGEON_CENTER_POS, BlockPos.ORIGIN);
        builder.add(HAS_DUNGEON_CENTER, false);
        builder.add(ACTIVE_SPECIAL_ATTACK, SPECIAL_NONE);
        builder.add(SPECIAL_ATTACK_TICKS, 0);
        builder.add(WEAPON_CIRCLE_ANGLE, 0.0F);
        builder.add(TELEKINESIS_TARGET_ID, -1);
    }

    public int getActiveSpecialAttack() {
        return this.dataTracker.get(ACTIVE_SPECIAL_ATTACK);
    }

    public boolean isWeaponCircleActive() {
        return this.getActiveSpecialAttack() == SPECIAL_WEAPON_CIRCLE;
    }

    public int getWeaponCircleTicks() {
        return this.dataTracker.get(SPECIAL_ATTACK_TICKS);
    }

    public float getWeaponCircleAngle() {
        return this.dataTracker.get(WEAPON_CIRCLE_ANGLE);
    }

    public int getTelekinesisTargetId() {
        return this.dataTracker.get(TELEKINESIS_TARGET_ID);
    }

    public static boolean isBazookaSlot(int slot, boolean phase2) {
        return phase2 && (slot % 2 == 0);
    }

    public boolean isWaitingForPlayer() {
        return this.dataTracker.get(WAITING_FOR_PLAYER);
    }

    public boolean isPhase2() {
        return this.dataTracker.get(PHASE_2);
    }

    /** Permite previsualizar la Fase 2 en la Enciclopedia 3D si se requiere. */
    public void setClientPreviewPhase2(boolean phase2) {
        if (this.getWorld().isClient()) {
            this.dataTracker.set(PHASE_2, phase2);
        }
    }

    public boolean isInSpawnCinematic() {
        int t = this.dataTracker.get(SPAWN_CINEMATIC_TICKS);
        return t > 0 && t <= SPAWN_CINEMATIC_DURATION_TICKS;
    }

    public int getSpawnCinematicTicks() {
        return this.dataTracker.get(SPAWN_CINEMATIC_TICKS);
    }

    public boolean isDyingCinematic() {
        return this.dataTracker.get(DYING_CINEMATIC);
    }

    public boolean isInAnyCinematic() {
        return this.isInSpawnCinematic() || this.isDyingCinematic();
    }

    public boolean hasDungeonCenter() {
        return this.dataTracker.get(HAS_DUNGEON_CENTER);
    }

    public BlockPos getDungeonCenterPos() {
        return this.dataTracker.get(DUNGEON_CENTER_POS);
    }

    /**
     * Música de toda la pelea (cinemática de entrada, Fase 1 y Fase 2).
     * Se mantiene apagada mientras espera en el podio y se detiene al morir.
     */
    public boolean shouldPlayBossMusic() {
        return this.isAlive()
                && !this.isWaitingForPlayer()
                && !this.isDyingCinematic()
                && (this.isInSpawnCinematic() || this.dataTracker.get(IN_COMBAT));
    }

    public static DefaultAttributeContainer.Builder createCharlieKirkAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 850.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.30D)
                .add(EntityAttributes.ATTACK_DAMAGE, 12.0D)
                .add(EntityAttributes.ARMOR, 14.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0D)
                .add(EntityAttributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void initGoals() {
        // Jefe estacionario flotando en el aire: sin persecución ni ataques cuerpo a cuerpo
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 48.0F));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Coloca a Charlie Kirk en espera sobre el podio de la Gran Zona Principal de Conferencias
     * después de usar el Buzón de la Casa Blanca en la entrada exterior.
     */
    public void prepareWaitingInConferenceHall(
            ServerWorld world,
            double stageX,
            double stageY,
            double stageZ,
            BlockPos dungeonCenter
    ) {
        this.spawnInitialized = true;
        this.waitingAnchorX = stageX;
        this.anchorGroundY = stageY;
        this.waitingAnchorZ = stageZ;
        this.dataTracker.set(DUNGEON_CENTER_POS, dungeonCenter);
        this.dataTracker.set(HAS_DUNGEON_CENTER, true);
        this.dataTracker.set(WAITING_FOR_PLAYER, true);
        this.dataTracker.set(IN_COMBAT, false);
        this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 0);
        this.bossBar.setVisible(false);
        this.setNoGravity(false);
        this.setTarget(null);
        this.getNavigation().stop();
        this.setVelocity(Vec3d.ZERO);
        this.refreshPositionAndAngles(stageX, stageY, stageZ, 0.0F, 0.0F);
    }

    /**
     * Inicia la cinemática de entrada de 48 segundos para todos los jugadores presentes.
     */
    public void startSpawnCinematic(ServerWorld world, double spawnX, double spawnY, double spawnZ) {
        this.spawnInitialized = true;
        this.waitingAnchorX = spawnX;
        this.anchorGroundY = spawnY;
        this.waitingAnchorZ = spawnZ;

        if (!this.hasDungeonCenter() && CasaBlancaStructure.isPosInCasaBlanca(world, BlockPos.ofFloored(spawnX, spawnY, spawnZ))) {
            CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.getOrResolveUniqueAnchor(world);
            if (anchor != null) {
                this.dataTracker.set(DUNGEON_CENTER_POS, new BlockPos(anchor.centerX(), anchor.baseY(), anchor.centerZ()));
                this.dataTracker.set(HAS_DUNGEON_CENTER, true);
            }
        }

        this.frozenPlayerPoses.clear();
        for (ServerPlayerEntity p : collectParticipatingPlayers(world)) {
            this.frozenPlayerPoses.put(
                    p.getUuid(),
                    new SavedPlayerPose(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch())
            );
        }

        this.dataTracker.set(WAITING_FOR_PLAYER, false);
        this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 1);
        this.dataTracker.set(IN_COMBAT, true);
        this.bossBar.setVisible(true);
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setVelocity(Vec3d.ZERO);
        this.refreshPositionAndAngles(spawnX, spawnY, spawnZ, 0.0F, 0.0F);
        this.triggerAnim("actionController", "spawn_cinematic");
    }

    private List<ServerPlayerEntity> collectParticipatingPlayers(ServerWorld world) {
        return world.getPlayers(p ->
                p.isAlive()
                        && !p.isSpectator()
                        && (CasaBlancaStructure.isPosInCasaBlanca(world, p.getBlockPos())
                                || p.squaredDistanceTo(this) <= 240.0D * 240.0D)
        ).stream().sorted(Comparator.comparing(p -> p.getUuid().toString())).toList();
    }

    private static boolean isPlayerInsideWhiteHouseBuilding(BlockPos pos, BlockPos center) {
        int dx = pos.getX() - center.getX();
        int dz = pos.getZ() - center.getZ();
        int dy = pos.getY() - center.getY();
        boolean inMainResidence = Math.abs(dx) <= 50 && Math.abs(dz) <= 32;
        boolean inWings = Math.abs(dx) <= 107 && Math.abs(dz) <= 23;
        boolean inOvalBay = Math.abs(dx + 82) <= 11 && dz >= 18 && dz <= 30;
        return dy >= 0 && dy <= 18 && (inMainResidence || inWings || inOvalBay);
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        // 0. CINEMÁTICA FINAL DE MUERTE
        if (this.isDyingCinematic()) {
            tickDeathCinematic(world);
            return;
        }

        this.bossBar.setPercent(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
        this.bossBar.setVisible(!this.isWaitingForPlayer());

        if (!this.spawnInitialized) {
            double groundY = findInteriorFloorY(world, this.getBlockPos());
            startSpawnCinematic(world, this.getX(), groundY, this.getZ());
        }

        // 1. MODO ESPERA DENTRO DE LA CASA BLANCA HASTA QUE CUALQUIER JUGADOR ENTRE AL EDIFICIO
        if (this.isWaitingForPlayer()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setVelocity(Vec3d.ZERO);
            this.setPosition(this.waitingAnchorX, this.anchorGroundY, this.waitingAnchorZ);

            boolean triggered = false;
            if (this.hasDungeonCenter()) {
                BlockPos center = this.getDungeonCenterPos();
                for (ServerPlayerEntity p : world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator())) {
                    if (isPlayerInsideWhiteHouseBuilding(p.getBlockPos(), center)) {
                        triggered = true;
                        break;
                    }
                }
            } else {
                List<PlayerEntity> nearby = world.getEntitiesByClass(
                        PlayerEntity.class,
                        this.getBoundingBox().expand(WAKE_OUTSIDE_DUNGEON_RANGE),
                        p -> p.isAlive() && !p.isSpectator()
                );
                triggered = !nearby.isEmpty();
            }

            if (triggered) {
                startSpawnCinematic(world, this.waitingAnchorX, this.anchorGroundY, this.waitingAnchorZ);
            }
            return;
        }

        // 2. CINEMÁTICA DE ENTRADA DE 48 SEGUNDOS
        if (this.isInSpawnCinematic()) {
            tickSpawnCinematic(world);
            return;
        }

        // 3. TRANSICIÓN A SEGUNDA FASE AL BAJAR AL 50% DE VIDA
        if (!this.isPhase2() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            activatePhase2(world);
        }

        // Mantener a Charlie Kirk quieto flotando en el aire durante toda la pelea (Fase 1 y Fase 2), sin partículas pasivas
        maintainAirborneAnchor();

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || target.isSpectator()
                || (target instanceof PlayerEntity p && p.isCreative())) {
            this.setTarget(null);
            PlayerEntity nearestPlayer = world.getClosestPlayer(this, 60.0D);
            if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                this.setTarget(nearestPlayer);
                target = nearestPlayer;
            } else {
                target = null;
            }
        }

        if (target == null || !target.isAlive()) {
            this.dataTracker.set(IN_COMBAT, false);
            if (this.getActiveSpecialAttack() != SPECIAL_NONE) {
                endSpecialAttack();
            }
            return;
        }

        this.dataTracker.set(IN_COMBAT, true);
        this.getLookControl().lookAt(target, 35.0F, 35.0F);
        tickCombatSpecialAttacks(world, target);
    }

    private void tickSpawnCinematic(ServerWorld world) {
        int ticks = this.getSpawnCinematicTicks();
        this.getNavigation().stop();
        this.setNoGravity(true);
        this.setVelocity(Vec3d.ZERO);
        applyCinematicPlayerImmunityAndFreezeMobs(world, 240.0D);

        // Durante 0s..43s (ticks 1..859), mantener quietos a los jugadores en su posición para los planos de cámara
        if (ticks < BLACKOUT_START_TICK) {
            this.setPosition(this.waitingAnchorX, this.anchorGroundY, this.waitingAnchorZ);
            for (ServerPlayerEntity p : world.getPlayers(pl -> pl.isAlive() && !pl.isSpectator())) {
                SavedPlayerPose saved = this.frozenPlayerPoses.get(p.getUuid());
                if (saved == null) {
                    saved = new SavedPlayerPose(p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
                    this.frozenPlayerPoses.put(p.getUuid(), saved);
                }
                if (p.getPos().squaredDistanceTo(saved.x(), saved.y(), saved.z()) > 0.04D) {
                    p.teleport(world, saved.x(), saved.y(), saved.z(), Set.of(), saved.yaw(), saved.pitch(), true);
                }
                p.setVelocity(Vec3d.ZERO);
            }
        }

        // Segundo 45 (tick 900, en mitad de la pantalla en negro): mover jugadores al Salón de Conferencias y elevar a Charlie
        if (ticks == TELEPORT_PLAYERS_TICK) {
            List<ServerPlayerEntity> players = collectParticipatingPlayers(world);
            int count = Math.max(1, players.size());
            double destBaseX = this.hasDungeonCenter() ? this.getDungeonCenterPos().getX() + 0.5D : this.waitingAnchorX;
            double destBaseY = this.hasDungeonCenter() ? this.getDungeonCenterPos().getY() + 1.0D : this.anchorGroundY;
            double destBaseZ = this.hasDungeonCenter() ? this.getDungeonCenterPos().getZ() + 4.0D : this.waitingAnchorZ + 12.0D;

            for (int i = 0; i < players.size(); i++) {
                ServerPlayerEntity p = players.get(i);
                double offsetX = (i - (count - 1) / 2.0D) * 2.5D;
                p.fallDistance = 0.0F;
                p.teleport(world, destBaseX + offsetX, destBaseY, destBaseZ, Set.of(), 180.0F, -8.0F, true);
                p.setVelocity(Vec3d.ZERO);
            }
            this.setPosition(this.waitingAnchorX, this.anchorGroundY + 2.2D, this.waitingAnchorZ);
            this.setYaw(0.0F);
            this.setHeadYaw(0.0F);
        }

        // Desde el tick 901 al 960 mantenerlo flotando sobre el escenario
        if (ticks >= TELEPORT_PLAYERS_TICK) {
            double floatY = this.anchorGroundY + 2.2D + Math.sin(ticks * 0.12D) * 0.18D;
            this.setPosition(this.waitingAnchorX, floatY, this.waitingAnchorZ);
        }

        // Al aparecer frente a los jugadores (ticks 954..960), liberar ráfagas de partículas de las 19 armas
        if (ticks >= REVEAL_MODEL_TICK - 2) {
            spawnWeaponParticlesBurst(world, 6, 1.1D);
        }

        if (ticks >= SPAWN_CINEMATIC_DURATION_TICKS) {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 0);
            this.specialAttackCooldown = 50;
            this.frozenPlayerPoses.clear();
            releaseCinematicFrozenMobs(world, 240.0D);

            // Gran explosión visual de partículas de las 19 armas al entrar en batalla en el segundo 48
            spawnWeaponParticlesBurst(world, 40, 1.5D);
            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    this.getX(), this.getBodyY(0.6D), this.getZ(),
                    28, 1.1D, 1.0D, 1.1D, 0.08D
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.6F, 1.05F
            );

            for (ServerPlayerEntity player : collectParticipatingPlayers(world)) {
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(8, 55, 12));
                player.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.literal("Charlie Kirk").formatted(Formatting.BLUE, Formatting.BOLD)
                ));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(
                        Text.literal("Salón Principal de Conferencias").formatted(Formatting.WHITE, Formatting.ITALIC)
                ));
            }

            PlayerEntity nearest = world.getClosestPlayer(this, 64.0D);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                this.setTarget(nearest);
            }
        } else {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, ticks + 1);
        }
    }

    private void maintainAirborneAnchor() {
        this.setNoGravity(true);
        this.getNavigation().stop();
        double targetHoverY = this.anchorGroundY + 1.95D + Math.sin(this.age * 0.08D) * 0.22D;
        double vx = MathHelper.clamp((this.waitingAnchorX - this.getX()) * 0.28D, -0.35D, 0.35D);
        double vy = MathHelper.clamp((targetHoverY - this.getY()) * 0.28D, -0.35D, 0.35D);
        double vz = MathHelper.clamp((this.waitingAnchorZ - this.getZ()) * 0.28D, -0.35D, 0.35D);
        this.setVelocity(vx, vy, vz);
        this.velocityModified = true;
    }

    private void tickCombatSpecialAttacks(ServerWorld world, LivingEntity target) {
        int active = this.getActiveSpecialAttack();
        if (active == SPECIAL_NONE) {
            if (this.specialAttackCooldown > 0) {
                this.specialAttackCooldown--;
                return;
            }
            int nextAttack = chooseNextSpecialAttack(world);
            if (nextAttack == SPECIAL_WEAPON_CIRCLE) {
                startWeaponCircleAttack(world);
            } else if (nextAttack == SPECIAL_TELEKINESIS_EPSTEIN) {
                startTelekinesisEpsteinAttack(world);
            }
            return;
        }

        if (active == SPECIAL_WEAPON_CIRCLE) {
            tickWeaponCircleAttack(world);
        } else if (active == SPECIAL_TELEKINESIS_EPSTEIN) {
            tickTelekinesisEpsteinAttack(world);
        }
    }

    private int chooseNextSpecialAttack(ServerWorld world) {
        List<ServerPlayerEntity> candidates = collectTelekinesisCandidates(world);
        boolean hasTelekinesisTarget = !candidates.isEmpty();

        if (this.lastSpecialAttackType == SPECIAL_WEAPON_CIRCLE && hasTelekinesisTarget) {
            return SPECIAL_TELEKINESIS_EPSTEIN;
        }
        return SPECIAL_WEAPON_CIRCLE;
    }

    private List<ServerPlayerEntity> collectTelekinesisCandidates(ServerWorld world) {
        List<ServerPlayerEntity> eligible = world.getPlayers(p ->
                p.isAlive()
                        && !p.isSpectator()
                        && !p.isCreative()
                        && p.squaredDistanceTo(this) <= 56.0D * 56.0D
                        && !p.hasStatusEffect(ModEffects.EPSTEIN_EFFECT)
        );
        if (!eligible.isEmpty()) {
            return eligible;
        }
        return world.getPlayers(p ->
                p.isAlive()
                        && !p.isSpectator()
                        && !p.isCreative()
                        && p.squaredDistanceTo(this) <= 56.0D * 56.0D
        );
    }

    private void endSpecialAttack() {
        this.dataTracker.set(ACTIVE_SPECIAL_ATTACK, SPECIAL_NONE);
        this.dataTracker.set(SPECIAL_ATTACK_TICKS, 0);
        this.dataTracker.set(TELEKINESIS_TARGET_ID, -1);
        this.telekinesisTargetUuid = null;
        this.telekinesisStartPos = null;
        this.telekinesisHoldPos = null;
    }

    private void startWeaponCircleAttack(ServerWorld world) {
        this.lastSpecialAttackType = SPECIAL_WEAPON_CIRCLE;
        this.dataTracker.set(ACTIVE_SPECIAL_ATTACK, SPECIAL_WEAPON_CIRCLE);
        this.dataTracker.set(SPECIAL_ATTACK_TICKS, 1);
        this.dataTracker.set(WEAPON_CIRCLE_ANGLE, this.random.nextFloat() * ((float) Math.PI * 2.0F));
        this.triggerAnim("actionController", "weapon_circle");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.1F, 1.35F
        );
    }

    private void tickWeaponCircleAttack(ServerWorld world) {
        int ticks = this.dataTracker.get(SPECIAL_ATTACK_TICKS);
        float angle = this.dataTracker.get(WEAPON_CIRCLE_ANGLE) + WEAPON_CIRCLE_ROT_SPEED;
        this.dataTracker.set(WEAPON_CIRCLE_ANGLE, angle);

        boolean phase2 = this.isPhase2();
        boolean isBurstTick = false;
        for (int burstTick : WEAPON_CIRCLE_BURST_TICKS) {
            if (ticks == burstTick) {
                isBurstTick = true;
                break;
            }
        }

        if (isBurstTick) {
            fireWeaponCircleGuns(world, angle, phase2);
        }
        if (phase2 && ticks == WEAPON_CIRCLE_BAZOOKA_FIRE_TICK) {
            fireWeaponCircleBazookas(world, angle);
        }

        if (ticks >= WEAPON_CIRCLE_DURATION_TICKS) {
            endSpecialAttack();
            this.specialAttackCooldown = ATTACK_COOLDOWN_TICKS;
        } else {
            this.dataTracker.set(SPECIAL_ATTACK_TICKS, ticks + 1);
        }
    }

    private void fireWeaponCircleGuns(ServerWorld world, float baseAngle, boolean phase2) {
        for (int slot = 0; slot < WEAPON_CIRCLE_COUNT; slot++) {
            if (isBazookaSlot(slot, phase2)) {
                continue;
            }
            double slotAngle = baseAngle + slot * ((Math.PI * 2.0D) / WEAPON_CIRCLE_COUNT);
            double dirX = Math.cos(slotAngle);
            double dirZ = Math.sin(slotAngle);
            double dirY = -0.095D;

            double muzzleRadius = WEAPON_CIRCLE_RADIUS + 0.55D;
            double mx = this.getX() + dirX * muzzleRadius;
            double my = this.getY() + 0.30D;
            double mz = this.getZ() + dirZ * muzzleRadius;

            GuardBulletProjectileEntity bullet = new GuardBulletProjectileEntity(world, this, CIRCLE_BULLET_DAMAGE);
            bullet.setPosition(mx, my, mz);
            bullet.setVelocity(dirX, dirY, dirZ, 1.55F, 1.0F);
            world.spawnEntity(bullet);

            world.spawnParticles(ParticleTypes.SMOKE, mx, my, mz, 2, 0.04D, 0.04D, 0.04D, 0.01D);
        }

        // Un solo sonido nítido por ráfaga para evitar saturación de audio
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                ModSounds.GUARD_RIFLE_SHOT, SoundCategory.HOSTILE,
                0.95F, 0.94F + this.random.nextFloat() * 0.12F
        );
    }

    private void fireWeaponCircleBazookas(ServerWorld world, float baseAngle) {
        for (int slot = 0; slot < WEAPON_CIRCLE_COUNT; slot++) {
            if (!isBazookaSlot(slot, true)) {
                continue;
            }
            double slotAngle = baseAngle + slot * ((Math.PI * 2.0D) / WEAPON_CIRCLE_COUNT);
            double dirX = Math.cos(slotAngle);
            double dirZ = Math.sin(slotAngle);
            double dirY = -0.115D;

            double muzzleRadius = WEAPON_CIRCLE_RADIUS + 0.65D;
            double mx = this.getX() + dirX * muzzleRadius;
            double my = this.getY() + 0.30D;
            double mz = this.getZ() + dirZ * muzzleRadius;

            GuardRocketProjectileEntity rocket = new GuardRocketProjectileEntity(
                    world, this, CIRCLE_ROCKET_DIRECT_DAMAGE, CIRCLE_ROCKET_SPLASH_DAMAGE
            );
            rocket.setPosition(mx, my, mz);
            rocket.setVelocity(dirX, dirY, dirZ, 1.15F, 0.5F);
            world.spawnEntity(rocket);

            world.spawnParticles(ParticleTypes.FLAME, mx, my, mz, 5, 0.08D, 0.08D, 0.08D, 0.025D);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, mx, my, mz, 5, 0.10D, 0.10D, 0.10D, 0.02D);
        }

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1.25F, 0.78F
        );
    }

    private void startTelekinesisEpsteinAttack(ServerWorld world) {
        List<ServerPlayerEntity> candidates = collectTelekinesisCandidates(world);
        if (candidates.isEmpty()) {
            startWeaponCircleAttack(world);
            return;
        }

        ServerPlayerEntity victim = candidates.get(this.random.nextInt(candidates.size()));
        this.lastSpecialAttackType = SPECIAL_TELEKINESIS_EPSTEIN;
        this.telekinesisTargetUuid = victim.getUuid();
        this.telekinesisStartPos = victim.getPos();
        double targetLiftY = Math.min(victim.getY() + 2.8D, this.anchorGroundY + 4.8D);
        this.telekinesisHoldPos = new Vec3d(victim.getX(), targetLiftY, victim.getZ());

        this.dataTracker.set(ACTIVE_SPECIAL_ATTACK, SPECIAL_TELEKINESIS_EPSTEIN);
        this.dataTracker.set(SPECIAL_ATTACK_TICKS, 1);
        this.dataTracker.set(TELEKINESIS_TARGET_ID, victim.getId());
        this.triggerAnim("actionController", "telekinesis");
        this.getLookControl().lookAt(victim, 45.0F, 45.0F);

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1.2F, 0.85F
        );
    }

    private void tickTelekinesisEpsteinAttack(ServerWorld world) {
        int ticks = this.dataTracker.get(SPECIAL_ATTACK_TICKS);
        PlayerEntity rawVictim = this.telekinesisTargetUuid != null ? world.getPlayerByUuid(this.telekinesisTargetUuid) : null;
        if (!(rawVictim instanceof ServerPlayerEntity victim)
                || !victim.isAlive()
                || victim.isSpectator()
                || victim.isCreative()) {
            endSpecialAttack();
            this.specialAttackCooldown = 60;
            return;
        }

        this.getLookControl().lookAt(victim, 45.0F, 45.0F);

        if (ticks < TELEKINESIS_DURATION_TICKS) {
            Vec3d start = this.telekinesisStartPos != null ? this.telekinesisStartPos : victim.getPos();
            Vec3d hold = this.telekinesisHoldPos != null ? this.telekinesisHoldPos : start.add(0.0D, 2.8D, 0.0D);

            double progress = Math.min(1.0D, ticks / 16.0D);
            double smoothT = progress * progress * (3.0D - 2.0D * progress);
            double desiredX = MathHelper.lerp(smoothT, start.x, hold.x);
            double desiredY = MathHelper.lerp(smoothT, start.y, hold.y) + Math.sin(ticks * 0.25D) * 0.08D;
            double desiredZ = MathHelper.lerp(smoothT, start.z, hold.z);

            victim.fallDistance = 0.0F;
            Vec3d delta = new Vec3d(desiredX - victim.getX(), desiredY - victim.getY(), desiredZ - victim.getZ());
            if (delta.lengthSquared() > 0.65D * 0.65D) {
                victim.teleport(world, desiredX, desiredY, desiredZ, PositionFlag.ROT, 0.0F, 0.0F, false);
            }
            victim.setVelocity(
                    MathHelper.clamp(delta.x * 0.50D, -0.75D, 0.75D),
                    MathHelper.clamp(delta.y * 0.50D + 0.078D, -0.45D, 0.75D),
                    MathHelper.clamp(delta.z * 0.50D, -0.75D, 0.75D)
            );
            victim.velocityModified = true;
            victim.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(victim));

            if (ticks % 2 == 0) {
                spawnTelekinesisParticles(world, victim);
            }
            this.dataTracker.set(SPECIAL_ATTACK_TICKS, ticks + 1);
            return;
        }

        // Tick 60: soltar suavemente, activar aviso + sonido "Efecto jefri", aplicar Efecto Epstein y spawnear niños
        completeTelekinesisEpsteinEffect(world, victim);
        endSpecialAttack();
        this.specialAttackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    private void spawnTelekinesisParticles(ServerWorld world, ServerPlayerEntity victim) {
        Vec3d origin = new Vec3d(this.getX(), this.getBodyY(0.68D), this.getZ());
        Vec3d targetCenter = new Vec3d(victim.getX(), victim.getBodyY(0.5D), victim.getZ());
        Vec3d span = targetCenter.subtract(origin);
        for (int s = 1; s <= 5; s++) {
            double f = s / 6.0D;
            world.spawnParticles(
                    TELEKINESIS_PURPLE_DUST,
                    origin.x + span.x * f,
                    origin.y + span.y * f,
                    origin.z + span.z * f,
                    1, 0.08D, 0.08D, 0.08D, 0.01D
            );
        }
        double ringAngle = (this.age % 36) * (Math.PI * 2.0D / 12.0D);
        world.spawnParticles(
                ParticleTypes.WITCH,
                targetCenter.x + Math.cos(ringAngle) * 0.85D,
                targetCenter.y,
                targetCenter.z + Math.sin(ringAngle) * 0.85D,
                1, 0.04D, 0.08D, 0.04D, 0.01D
        );
    }

    private void completeTelekinesisEpsteinEffect(ServerWorld world, ServerPlayerEntity victim) {
        boolean phase2 = this.isPhase2();
        victim.fallDistance = 0.0F;
        victim.setVelocity(Vec3d.ZERO);
        victim.velocityModified = true;
        victim.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(victim));

        int amplifier = phase2 ? 1 : 0;
        victim.addStatusEffect(new StatusEffectInstance(
                ModEffects.EPSTEIN_EFFECT,
                20 * 30,
                amplifier,
                false,
                true,
                true
        ));

        ServerPlayNetworking.send(victim, new CharlieEpsteinPopupPayload(phase2));

        int kidCount = phase2 ? 8 : 5;
        spawnEpsteinKidsForVictim(world, victim, kidCount);
    }

    private void spawnEpsteinKidsForVictim(ServerWorld world, ServerPlayerEntity victim, int count) {
        EntityType<?>[] kidTypes = {
                ModEntities.YOSTIN_BIBER,
                ModEntities.VITOR_JUGO,
                ModEntities.FER_OVILLA,
                ModEntities.VALENTINA_PRIETA
        };
        int startVariant = this.random.nextInt(kidTypes.length);

        for (int i = 0; i < count; i++) {
            EntityType<?> chosenType = i < kidTypes.length
                    ? kidTypes[(startVariant + i) % kidTypes.length]
                    : kidTypes[this.random.nextInt(kidTypes.length)];
            Entity created = chosenType.create(world, SpawnReason.MOB_SUMMONED);
            if (!(created instanceof MobEntity kid)) {
                continue;
            }

            double angle = (i * (Math.PI * 2.0D) / count) + this.random.nextDouble() * 0.45D;
            double dist = 4.0D + this.random.nextDouble() * 3.5D;
            int sx = (int) Math.floor(victim.getX() + Math.cos(angle) * dist);
            int sz = (int) Math.floor(victim.getZ() + Math.sin(angle) * dist);
            BlockPos spawnPos = findSafeKidSpawnPos(world, sx, (int) Math.round(this.anchorGroundY), sz, victim.getBlockPos());

            float yOffset = (chosenType == ModEntities.VITOR_JUGO) ? 1.5F : 0.0F;
            kid.refreshPositionAndAngles(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + yOffset,
                    spawnPos.getZ() + 0.5D,
                    this.random.nextFloat() * 360.0F,
                    0.0F
            );
            CharlieEpsteinSummonHelper.markAsEpsteinSummon(kid, victim);
            world.spawnEntity(kid);

            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    kid.getX(), kid.getBodyY(0.5D), kid.getZ(),
                    14, 0.35D, 0.45D, 0.35D, 0.10D
            );
            world.spawnParticles(
                    ParticleTypes.WITCH,
                    kid.getX(), kid.getBodyY(0.5D), kid.getZ(),
                    8, 0.25D, 0.35D, 0.25D, 0.04D
            );
        }
    }

    private BlockPos findSafeKidSpawnPos(ServerWorld world, int x, int preferredY, int z, BlockPos fallback) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos pos = new BlockPos(x, preferredY + dy, z);
            if (world.isChunkLoaded(pos)
                    && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                    && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()) {
                return pos;
            }
        }
        return fallback;
    }

    private void spawnWeaponParticlesBurst(ServerWorld world, int count, double spread) {
        world.spawnParticles(
                ModParticles.CHARLIE_WEAPON,
                this.getX(), this.getBodyY(0.55D), this.getZ(),
                count, spread, 0.8D, spread, 0.04D
        );
    }

    private void activatePhase2(ServerWorld world) {
        this.dataTracker.set(PHASE_2, true);
        this.setNoGravity(true);
        this.triggerAnim("actionController", "phase2_transform");
        this.bossBar.setName(Text.translatable("entity.prueba_no1.charlie_kirk")
                .append(Text.literal(" - Fase 2").formatted(Formatting.AQUA, Formatting.BOLD)));

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.2F, 1.15F
        );
        world.spawnParticles(
                PRESIDENTIAL_BLUE_DUST,
                this.getX(), this.getBodyY(0.6D), this.getZ(),
                24, 0.9D, 1.0D, 0.9D, 0.08D
        );

        List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> p.squaredDistanceTo(this) <= 4096.0D);
        for (ServerPlayerEntity player : nearbyPlayers) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 45, 10));
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("SEGUNDA FASE").formatted(Formatting.BLUE, Formatting.BOLD)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("Charlie Kirk desata su poder").formatted(Formatting.RED, Formatting.ITALIC)
            ));
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isWaitingForPlayer() || this.isInAnyCinematic()) {
            return false;
        }

        Entity attacker = source.getAttacker();
        if (attacker == null && source.getSource() instanceof ProjectileEntity projectile) {
            attacker = projectile.getOwner();
        }
        if (attacker instanceof WhiteHouseGuardEntity || CharlieEpsteinSummonHelper.isEpsteinSummon(attacker)) {
            return false;
        }
        if (attacker instanceof PlayerEntity player && player.hasStatusEffect(ModEffects.EPSTEIN_EFFECT)) {
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 0.65F, 0.72F
            );
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new TitleFadeS2CPacket(2, 24, 8));
                serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.literal("No puedes dañarlo we").formatted(Formatting.WHITE, Formatting.BOLD)
                ));
                serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(Text.empty()));
            }
            return false;
        }

        if (this.getHealth() - amount <= 0.0F) {
            this.setHealth(1.0F);
            startDeathCinematic(world);
            return true;
        }

        return super.damage(world, source, amount);
    }

    private void startDeathCinematic(ServerWorld world) {
        this.endSpecialAttack();
        CharlieEpsteinSummonHelper.discardAllEpsteinSummonsNear(world, this.getPos(), 140.0D);
        this.dataTracker.set(IN_COMBAT, false);
        this.dataTracker.set(DYING_CINEMATIC, true);
        this.deathCinematicTicks = 0;
        this.anchorGroundY = findInteriorFloorY(world, this.getBlockPos());
        this.setNoGravity(false);
        this.getNavigation().stop();
        this.setVelocity(0.0D, -0.5D, 0.0D);
        this.velocityModified = true;
        this.triggerAnim("actionController", "death_cinematic");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.8F, 0.9F
        );
    }

    private void tickDeathCinematic(ServerWorld world) {
        this.deathCinematicTicks++;
        this.getNavigation().stop();
        this.bossBar.setPercent(0.0F);
        applyCinematicPlayerImmunityAndFreezeMobs(world, 100.0D);

        if (this.getY() > this.anchorGroundY + 0.1D && this.deathCinematicTicks < 14) {
            this.setVelocity(0.0D, -1.0D, 0.0D);
        } else {
            this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);
        }

        if (this.deathCinematicTicks % 3 == 0) {
            world.spawnParticles(
                    PRESIDENTIAL_BLUE_DUST,
                    this.getX(), this.getY() + 0.8D, this.getZ(),
                    5, 0.65D, 0.6D, 0.65D, 0.04D
            );
        }

        if (this.deathCinematicTicks >= DEATH_CINEMATIC_DURATION_TICKS) {
            Box area = this.getBoundingBox().expand(100.0D);
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive)) {
                player.fallDistance = 0.0F;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, true, false, false));
            }

            releaseCinematicFrozenMobs(world, 120.0D);

            world.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    1, 0.2D, 0.2D, 0.2D, 0.0D
            );
            world.spawnParticles(
                    ParticleTypes.FIREWORK,
                    this.getX(), this.getY() + 1.1D, this.getZ(),
                    85, 1.4D, 1.2D, 1.4D, 0.22D
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.5F, 1.0F
            );

            List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> p.squaredDistanceTo(this) <= 6400.0D);
            for (ServerPlayerEntity player : nearbyPlayers) {
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
                player.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.literal("Charlie Kirk Derrotado").formatted(Formatting.GOLD, Formatting.BOLD)
                ));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(
                        Text.literal("Has conquistado La Casa Blanca").formatted(Formatting.AQUA, Formatting.ITALIC)
                ));
            }

            this.dropStack(world, new ItemStack(ModItem.SCARLET_INGOT, 8 + this.random.nextInt(5)));
            this.dropStack(world, new ItemStack(ModItem.TONY_COIN, 12 + this.random.nextInt(9)));
            this.dropStack(world, new ItemStack(ModItem.LONTOTEM, 1));
            pruebano1.modid.world.DungeonProgressionState.recordBossDefeated(
                    world,
                    pruebano1.modid.world.DungeonProgressionState.DungeonStage.CASA_BLANCA
            );

            this.bossBar.clearPlayers();
            this.discard();
        }
    }

    private void applyCinematicPlayerImmunityAndFreezeMobs(ServerWorld world, double radius) {
        Box area = this.getBoundingBox().expand(radius);
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive);
        for (PlayerEntity player : players) {
            player.fallDistance = 0.0F;
            player.setFireTicks(0);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15, 4, true, false, false));
        }

        List<MobEntity> mobs = world.getEntitiesByClass(
                MobEntity.class,
                area,
                mob -> mob != this && mob.isAlive()
        );
        for (MobEntity mob : mobs) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.setAttacker(null);
            mob.setVelocity(0.0D, Math.min(0.0D, mob.getVelocity().y), 0.0D);
            if (!mob.isAiDisabled()) {
                mob.setAiDisabled(true);
                mob.addCommandTag(CINEMATIC_FROZEN_TAG);
            }
        }
    }

    private void releaseCinematicFrozenMobs(ServerWorld world, double radius) {
        Box area = this.getBoundingBox().expand(radius);
        List<MobEntity> frozenMobs = world.getEntitiesByClass(
                MobEntity.class,
                area,
                mob -> mob.getCommandTags().contains(CINEMATIC_FROZEN_TAG)
        );
        for (MobEntity mob : frozenMobs) {
            mob.setAiDisabled(false);
            mob.removeCommandTag(CINEMATIC_FROZEN_TAG);
        }
    }

    private double findInteriorFloorY(ServerWorld world, BlockPos pos) {
        BlockPos.Mutable scan = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
        int minY = Math.max(world.getBottomY() + 1, pos.getY() - 24);
        for (int y = pos.getY(); y >= minY; y--) {
            scan.setY(y - 1);
            BlockState below = world.getBlockState(scan);
            if (!below.isAir() && !below.isReplaceable() && !below.isLiquid()) {
                return y;
            }
        }
        return pos.getY();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        if (name != null) {
            this.bossBar.setName(name);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("WaitingForPlayer", this.isWaitingForPlayer());
        nbt.putBoolean("Phase2", this.isPhase2());
        nbt.putDouble("AnchorGroundY", this.anchorGroundY);
        nbt.putDouble("WaitingAnchorX", this.waitingAnchorX);
        nbt.putDouble("WaitingAnchorZ", this.waitingAnchorZ);
        nbt.putBoolean("HasDungeonCenter", this.hasDungeonCenter());
        if (this.hasDungeonCenter()) {
            BlockPos dc = this.getDungeonCenterPos();
            nbt.putInt("DungeonCenterX", dc.getX());
            nbt.putInt("DungeonCenterY", dc.getY());
            nbt.putInt("DungeonCenterZ", dc.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.spawnInitialized = true;
        if (nbt.contains("AnchorGroundY")) {
            this.anchorGroundY = nbt.getDouble("AnchorGroundY");
        }
        if (nbt.contains("WaitingAnchorX")) {
            this.waitingAnchorX = nbt.getDouble("WaitingAnchorX");
        }
        if (nbt.contains("WaitingAnchorZ")) {
            this.waitingAnchorZ = nbt.getDouble("WaitingAnchorZ");
        }
        if (nbt.getBoolean("HasDungeonCenter")) {
            this.dataTracker.set(HAS_DUNGEON_CENTER, true);
            this.dataTracker.set(
                    DUNGEON_CENTER_POS,
                    new BlockPos(nbt.getInt("DungeonCenterX"), nbt.getInt("DungeonCenterY"), nbt.getInt("DungeonCenterZ"))
            );
        }
        if (nbt.contains("WaitingForPlayer")) {
            boolean waiting = nbt.getBoolean("WaitingForPlayer");
            this.dataTracker.set(WAITING_FOR_PLAYER, waiting);
            this.bossBar.setVisible(!waiting);
        }
        if (nbt.contains("Phase2")) {
            boolean phase2 = nbt.getBoolean("Phase2");
            this.dataTracker.set(PHASE_2, phase2);
            if (phase2) {
                this.bossBar.setName(Text.translatable("entity.prueba_no1.charlie_kirk")
                        .append(Text.literal(" - Fase 2").formatted(Formatting.AQUA, Formatting.BOLD)));
            }
        }
        if (!this.isWaitingForPlayer() && !this.isDyingCinematic()) {
            this.setNoGravity(true);
        }
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            CharlieEpsteinSummonHelper.discardAllEpsteinSummonsNear(serverWorld, this.getPos(), 140.0D);
        }
        super.remove(reason);
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> state.setAndContinue(IDLE_ANIM)));

        controllers.add(
                new AnimationController<>(this, "actionController", 2, state -> PlayState.STOP)
                        .triggerableAnim("attack", ATTACK_ANIM)
                        .triggerableAnim("spawn_cinematic", SPAWN_CINEMATIC_ANIM)
                        .triggerableAnim("phase2_transform", PHASE2_TRANSFORM_ANIM)
                        .triggerableAnim("death_cinematic", DEATH_CINEMATIC_ANIM)
                        .triggerableAnim("weapon_circle", WEAPON_CIRCLE_ANIM)
                        .triggerableAnim("telekinesis", TELEKINESIS_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
