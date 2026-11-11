package pruebano1.modid.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
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
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModItem;
import pruebano1.modid.ModParticles;
import pruebano1.modid.ModSounds;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Jefe Final de la Dungeon Isla Penstein: Jefri penstein.
 * - Cinemática de invocación ("spawn_cinematic"): aparece mágicamente en el cielo con efectos,
 *   desciende lentamente y farmea aura al tocar el suelo con edición de cámara estilo TikTok al ritmo de la música.
 * - Ataque Especial 1 ("special_summon"): levita 5 bloques, extiende sus manos liberando partículas de hoja verde
 *   e invoca entre 4 y 6 niños aleatorios a su alrededor.
 * - Ataque Especial 2 ("special_kotex"): invoca una ráfaga de proyectiles "toallas kotex" simultáneamente en el cielo
 *   y los deja caer cubriendo un área de 15x15 bloques.
 * - Ataque Especial 3 ("special_telekinesis"): levita 8 bloques mientras levita a todos los jugadores cercanos a su misma altura,
 *   reproduce "sonido_ataqueespecial1" y los lanza a 40 bloques de altura con telequinesis.
 * - Empuje defensivo ("push_wave"): mientras está en el suelo, cada 12 segundos de estar recibiendo daño empuja a todos a su alrededor.
 * - Modo Diablo ("devil_transform" / "devil_idle"): a los 100 de vida o menos, avisa en pantalla "epstein modo diablo",
 *   activa un aura roja, se mantiene a 10 bloques de altura en el aire y aumenta el daño de sus ataques en +20%.
 * - Cinemática de Muerte ("death_cage"): se detiene la música, cae bruscamente al suelo y queda encerrado tras rejas como una jaula antes de desaparecer.
 */
public class JefriPensteinEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DEVIL_IDLE_ANIM = RawAnimation.begin().thenLoop("devil_idle");
    private static final RawAnimation SPAWN_CINEMATIC_ANIM = RawAnimation.begin().thenPlay("spawn_cinematic");
    private static final RawAnimation SPECIAL_SUMMON_ANIM = RawAnimation.begin().thenPlay("special_summon");
    private static final RawAnimation SPECIAL_KOTEX_ANIM = RawAnimation.begin().thenPlay("special_kotex");
    private static final RawAnimation SPECIAL_TELEKINESIS_ANIM = RawAnimation.begin().thenPlay("special_telekinesis");
    private static final RawAnimation PUSH_WAVE_ANIM = RawAnimation.begin().thenPlay("push_wave");
    private static final RawAnimation DEVIL_TRANSFORM_ANIM = RawAnimation.begin().thenPlay("devil_transform");
    private static final RawAnimation DEATH_CAGE_ANIM = RawAnimation.begin().thenPlay("death_cage");

    private static final DustParticleEffect RED_AURA_DUST = new DustParticleEffect(0xFF1111, 1.45F);

    private static final TrackedData<Boolean> IN_COMBAT = DataTracker.registerData(
            JefriPensteinEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> DEVIL_MODE = DataTracker.registerData(
            JefriPensteinEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Integer> SPAWN_CINEMATIC_TICKS = DataTracker.registerData(
            JefriPensteinEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Boolean> DYING_CINEMATIC = DataTracker.registerData(
            JefriPensteinEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.prueba_no1.jefri_penstein"),
            BossBar.Color.RED,
            BossBar.Style.NOTCHED_10
    );

    private boolean spawnInitialized = false;
    private double anchorGroundY = 64.0D;

    // Estado de ataques especiales (0 = ninguno, 1 = invocar niños, 2 = toallas kotex, 3 = telequinesis)
    private int activeSpecialType = 0;
    private int activeSpecialTicks = 0;
    private int specialCooldown = 70;
    private int lastSpecialUsed = 0;
    private int childrenToSummonCount = 5;

    // Contador de 12 segundos (240 ticks) recibiendo daño mientras está en el suelo
    private int recentDamageWindowTicks = 0;
    private int groundDamageAccumTicks = 0;

    // Cinemática final de muerte en jaula
    private int deathCageTicks = 0;
    private boolean cageBlocksPlaced = false;
    private final List<BlockPos> placedCagePositions = new ArrayList<>();

    public JefriPensteinEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
        this.experiencePoints = 500;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(IN_COMBAT, false);
        builder.add(DEVIL_MODE, false);
        builder.add(SPAWN_CINEMATIC_TICKS, 0);
        builder.add(DYING_CINEMATIC, false);
    }

    public boolean isDevilMode() {
        return this.dataTracker.get(DEVIL_MODE);
    }

    /** Updates devil mode purely on the client preview entity (e.g. inside the encyclopedia 3D viewer). */
    public void setClientPreviewDevilMode(boolean devilMode) {
        if (this.getWorld().isClient()) {
            this.dataTracker.set(DEVIL_MODE, devilMode);
        }
    }

    public boolean isInSpawnCinematic() {
        int t = this.dataTracker.get(SPAWN_CINEMATIC_TICKS);
        return t > 0 && t <= 120;
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

    public boolean shouldPlayBossMusic() {
        return !this.isDyingCinematic() && (this.isInSpawnCinematic() || this.dataTracker.get(IN_COMBAT));
    }

    public float getDamageMultiplier() {
        return this.isDevilMode() ? 1.20F : 1.0F;
    }

    public static DefaultAttributeContainer.Builder createJefriPensteinAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 450.0D)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.28D)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0D)
                .add(EntityAttributes.ARMOR, 10.0D)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(EntityAttributes.FOLLOW_RANGE, 56.0D)
                .add(EntityAttributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Inicia la cinemática de invocación en el cielo con música y efectos mágicos.
     */
    public void startSpawnCinematic(ServerWorld world, double groundX, double groundY, double groundZ) {
        this.spawnInitialized = true;
        this.anchorGroundY = groundY;
        this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 1);
        this.dataTracker.set(IN_COMBAT, true);
        this.setNoGravity(true);
        this.refreshPositionAndAngles(groundX, groundY + 14.0D, groundZ, this.getYaw(), 0.0F);
        this.triggerAnim("actionController", "spawn_cinematic");

        world.playSound(
                null, groundX, groundY + 8.0D, groundZ,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 2.0F, 0.85F
        );
        world.playSound(
                null, groundX, groundY + 8.0D, groundZ,
                SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.6F, 1.1F
        );
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        this.bossBar.setPercent(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));

        // Si acaba de aparecer por primera vez (Huevo Generador o Buzón), iniciar cinemática de entrada en el cielo
        if (!this.spawnInitialized) {
            double groundY = findGroundY(world, this.getBlockPos());
            startSpawnCinematic(world, this.getX(), groundY, this.getZ());
        }

        // 1. CINEMÁTICA FINAL DE MUERTE (JAULA Y SIN MÚSICA)
        if (this.isDyingCinematic()) {
            tickDeathCageCinematic(world);
            return;
        }

        // 2. CINEMÁTICA DE ENTRADA / INVOCACIÓN (DESCENSO DEL CIELO + FARMEO DE AURA)
        if (this.isInSpawnCinematic()) {
            tickSpawnCinematic(world);
            return;
        }

        // 3. ACTIVAR "EPSTEIN MODO DIABLO" AL BAJAR A <= 100 DE VIDA
        if (!this.isDevilMode() && this.getHealth() <= 100.0F) {
            activateDevilMode(world);
        }

        // 4. PARTÍCULAS DE AURA ROJA EN MODO DIABLO (optimizadas para no afectar el rendimiento)
        if (this.isDevilMode() && this.age % 2 == 0) {
            world.spawnParticles(
                    RED_AURA_DUST,
                    this.getX(), this.getBodyY(0.5D), this.getZ(),
                    4, 0.45D, 0.65D, 0.45D, 0.01D
            );
            world.spawnParticles(
                    ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getBodyY(0.5D), this.getZ(),
                    2, 0.35D, 0.55D, 0.35D, 0.02D
            );
        }

        // 5. EMPUJE DEFENSIVO CADA 12 SEGUNDOS DE ESTAR RECIBIENDO DAÑO EN EL SUELO
        if (!this.isDevilMode() && this.activeSpecialType == 0) {
            if (this.recentDamageWindowTicks > 0) {
                this.recentDamageWindowTicks--;
                this.groundDamageAccumTicks++;
                if (this.groundDamageAccumTicks >= 240) { // 12 segundos = 240 ticks
                    this.groundDamageAccumTicks = 0;
                    triggerGroundPushWave(world);
                }
            }
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            PlayerEntity nearestPlayer = world.getClosestPlayer(this, 48.0D);
            if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                this.setTarget(nearestPlayer);
                target = nearestPlayer;
            }
        }

        if (target == null || !target.isAlive()) {
            this.dataTracker.set(IN_COMBAT, false);
            if (this.isDevilMode()) {
                maintainHoverHeight(world, 10.0D, null);
            } else {
                this.setNoGravity(false);
            }
            return;
        }

        this.dataTracker.set(IN_COMBAT, true);
        this.getLookControl().lookAt(target, 30.0F, 30.0F);

        // 6. PROCESAR ATAQUE ESPECIAL ACTIVO
        if (this.activeSpecialType != 0) {
            tickActiveSpecialAttack(world, target);
            return;
        }

        // 7. POSICIONAMIENTO EN COMBATE (A 10 bloques de altura en Modo Diablo, o en suelo en Fase 1)
        if (this.isDevilMode()) {
            maintainHoverHeight(world, 10.0D, target);
        } else {
            this.setNoGravity(false);
            double distSq = this.squaredDistanceTo(target);
            if (distSq > 81.0D) {
                this.getNavigation().startMovingTo(target, 1.05D);
            } else if (distSq < 25.0D) {
                this.getNavigation().stop();
            }
        }

        // 8. ELEGIR SIGUIENTE ATAQUE ESPECIAL
        if (this.specialCooldown > 0) {
            this.specialCooldown--;
        } else {
            startNextSpecialAttack(world, target);
        }
    }

    private static final String CINEMATIC_FROZEN_TAG = "jefri_cinematic_frozen";

    /**
     * Otorga inmunidad total a todos los jugadores cercanos y congela a todos los demás mobs
     * para que no se muevan ni ataquen mientras dura una cinemática de Jefri penstein.
     */
    private void applyCinematicPlayerImmunityAndFreezeMobs(ServerWorld world) {
        Box area = this.getBoundingBox().expand(80.0D);

        // 1. Inmunidad a los jugadores cercanos (sin daño por caída, fuego o ataques)
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive);
        for (PlayerEntity player : players) {
            player.fallDistance = 0.0F;
            player.setFireTicks(0);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 4, true, false, false));
        }

        // 2. Congelar a todos los mobs alrededor (niños, monstruos, proyectiles hostiles) para que no ataquen durante la cinemática
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

    /**
     * Descongela a todos los mobs que fueron congelados durante la cinemática.
     */
    private void releaseCinematicFrozenMobs(ServerWorld world) {
        Box area = this.getBoundingBox().expand(100.0D);
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

    /**
     * Cinemática de invocación (120 ticks = 6.0 segundos):
     * - Ticks 1 a 70 (0.0s - 3.5s): Desciende lentamente desde el cielo (groundY + 14 -> groundY) con efectos mágicos.
     * - Ticks 71 a 120 (3.5s - 6.0s): Toca el suelo y farmea aura al ritmo de la música antes de comenzar la pelea.
     */
    private void tickSpawnCinematic(ServerWorld world) {
        int ticks = this.getSpawnCinematicTicks();
        this.getNavigation().stop();
        this.setNoGravity(true);
        applyCinematicPlayerImmunityAndFreezeMobs(world);

        PlayerEntity closest = world.getClosestPlayer(this, 48.0D);
        if (closest != null) {
            this.getLookControl().lookAt(closest, 30.0F, 30.0F);
        }

        if (ticks <= 70) {
            // Descenso lento y mágico desde 14 bloques de altura hasta el suelo
            double progress = (double) ticks / 70.0D;
            double currentY = this.anchorGroundY + (14.0D * (1.0D - progress));
            this.setPosition(this.getX(), currentY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);

            if (ticks % 3 == 0) {
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 1.0D, this.getZ(),
                        5, 0.6D, 0.8D, 0.6D, 0.03D
                );
                world.spawnParticles(
                        ParticleTypes.PORTAL,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        8, 0.8D, 0.8D, 0.8D, 0.1D
                );
            }

            if (ticks == 70) {
                // Impacto al tocar el suelo para empezar a farmear aura
                this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
                world.spawnParticles(
                        ParticleTypes.EXPLOSION,
                        this.getX(), this.anchorGroundY + 0.2D, this.getZ(),
                        3, 0.8D, 0.1D, 0.8D, 0.02D
                );
                world.playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1.5F, 0.9F
                );
            }
        } else {
            // Ticks 71 a 120: en el suelo farmeando aura con brazos cruzados
            this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);
            if (ticks % 4 == 0) {
                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX(), this.getY() + 0.9D, this.getZ(),
                        4, 0.5D, 0.6D, 0.5D, 0.02D
                );
            }
        }

        if (ticks >= 120) {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 0);
            this.setNoGravity(false);
            this.specialCooldown = 40;
            releaseCinematicFrozenMobs(world);
        } else {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, ticks + 1);
        }
    }

    /**
     * Empuje defensivo en el suelo cada 12 segundos de estar recibiendo daño.
     */
    private void triggerGroundPushWave(ServerWorld world) {
        this.triggerAnim("actionController", "push_wave");
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.4F, 1.2F
        );
        world.spawnParticles(
                ParticleTypes.GUST_EMITTER_LARGE,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D
        );

        List<LivingEntity> nearby = world.getEntitiesByClass(
                LivingEntity.class,
                this.getBoundingBox().expand(9.0D, 4.0D, 9.0D),
                e -> e != this && e.isAlive()
        );
        for (LivingEntity entity : nearby) {
            Vec3d dir = entity.getPos().subtract(this.getPos());
            Vec3d horiz = new Vec3d(dir.x, 0.0D, dir.z);
            if (horiz.lengthSquared() < 0.001D) {
                horiz = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                horiz = horiz.normalize();
            }
            entity.setVelocity(horiz.x * 1.95D, 0.65D, horiz.z * 1.95D);
            entity.velocityModified = true;
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
        }
    }

    /**
     * Activa "epstein modo diablo" al llegar a <= 100 de vida:
     * - Muestra el mensaje en pantalla "epstein modo diablo"
     * - Activa el aura roja, sube a 10 bloques de altura y aumenta el daño un +20%.
     */
    private void activateDevilMode(ServerWorld world) {
        this.dataTracker.set(DEVIL_MODE, true);
        this.bossBar.setColor(BossBar.Color.PURPLE);
        this.bossBar.setName(Text.literal("Jefri penstein - Modo Diablo").formatted(Formatting.DARK_RED, Formatting.BOLD));
        this.triggerAnim("actionController", "devil_transform");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.8F, 0.9F
        );
        world.spawnParticles(
                RED_AURA_DUST,
                this.getX(), this.getY() + 1.1D, this.getZ(),
                35, 1.0D, 1.2D, 1.0D, 0.08D
        );

        // Mostrar título "epstein modo diablo" en la pantalla de todos los jugadores cercanos
        List<ServerPlayerEntity> players = world.getPlayers(p -> p.squaredDistanceTo(this) <= 6400.0D);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 50, 15));
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("epstein modo diablo").formatted(Formatting.DARK_RED, Formatting.BOLD)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("+20% Daño • Levitación Demoníaca").formatted(Formatting.RED)
            ));
        }
    }

    private void startNextSpecialAttack(ServerWorld world, LivingEntity target) {
        // Rotar entre los 3 ataques especiales sin repetir el mismo dos veces seguidas
        int nextAttack = 1 + this.random.nextInt(3);
        if (nextAttack == this.lastSpecialUsed) {
            nextAttack = (nextAttack % 3) + 1;
        }
        this.lastSpecialUsed = nextAttack;
        this.activeSpecialType = nextAttack;
        this.activeSpecialTicks = 0;
        this.anchorGroundY = findGroundY(world, this.getBlockPos());

        if (nextAttack == 1) {
            // Ataque Especial 1: Levita 5 bloques e invoca entre 2 y 4 niños con partículas de hoja verde
            this.childrenToSummonCount = 2 + this.random.nextInt(3); // 2, 3 o 4 niños
            this.triggerAnim("actionController", "special_summon");
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.6F, 1.1F
            );
        } else if (nextAttack == 2) {
            // Ataque Especial 2: Ráfaga de "toallas kotex" en el cielo cubriendo un área de 15x15
            this.triggerAnim("actionController", "special_kotex");
            spawnKotexBarrageInSky(world, target);
        } else {
            // Ataque Especial 3: Telequinesis (levita 8 bloques, levanta jugadores y los lanza a 40 bloques de altura)
            this.triggerAnim("actionController", "special_telekinesis");
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.6F, 0.8F
            );
        }
    }

    private void tickActiveSpecialAttack(ServerWorld world, LivingEntity target) {
        this.activeSpecialTicks++;
        this.getNavigation().stop();

        if (this.activeSpecialType == 1) {
            // ATAQUE ESPECIAL 1: INVOCAR 4 A 6 NIÑOS (70 ticks = 3.5s)
            double targetHeight = this.isDevilMode() ? 10.0D : 5.0D;
            maintainHoverHeight(world, targetHeight, target);

            // Liberar partículas personalizadas de hoja verde desde sus manos extendidas
            if (this.activeSpecialTicks % 2 == 0) {
                world.spawnParticles(
                        ModParticles.WEED_LEAF,
                        this.getX(), this.getY() + 1.5D, this.getZ(),
                        5, 1.4D, 0.6D, 1.4D, 0.04D
                );
            }

            // Invocar los 4 a 6 niños de manera escalonada entre los ticks 20 y 55
            int interval = Math.max(5, 36 / Math.max(1, this.childrenToSummonCount));
            if (this.activeSpecialTicks >= 20
                    && this.activeSpecialTicks < 20 + (this.childrenToSummonCount * interval)
                    && (this.activeSpecialTicks - 20) % interval == 0) {
                summonRandomChildAround(world, target);
            }

            if (this.activeSpecialTicks >= 70) {
                finishSpecialAttack();
            }
        } else if (this.activeSpecialType == 2) {
            // ATAQUE ESPECIAL 2: RÁFAGA DE TOALLAS KOTEX 15x15 (50 ticks = 2.5s)
            if (this.isDevilMode()) {
                maintainHoverHeight(world, 10.0D, target);
            } else {
                this.setVelocity(0.0D, this.getVelocity().y, 0.0D);
            }

            if (this.activeSpecialTicks >= 50) {
                finishSpecialAttack();
            }
        } else if (this.activeSpecialType == 3) {
            // ATAQUE ESPECIAL 3: TELEQUINESIS A 40 BLOQUES DE ALTURA (64 ticks = 3.2s)
            double hoverOffset = this.isDevilMode() ? 10.0D : 8.0D;
            maintainHoverHeight(world, hoverOffset, target);

            List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                    PlayerEntity.class,
                    this.getBoundingBox().expand(18.0D, 14.0D, 18.0D),
                    p -> p.isAlive() && !p.isSpectator()
            );

            if (this.activeSpecialTicks < 36) {
                // Ticks 1 a 35: Levita a todos los jugadores cercanos a su misma altura
                double bossY = this.getY();
                for (PlayerEntity player : nearbyPlayers) {
                    double dy = (bossY - player.getY()) * 0.22D;
                    player.setVelocity(player.getVelocity().x * 0.3D, Math.clamp(dy, -0.4D, 0.65D), player.getVelocity().z * 0.3D);
                    player.fallDistance = 0.0F;
                    player.velocityModified = true;
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
                    }
                    if (this.activeSpecialTicks % 3 == 0) {
                        world.spawnParticles(
                                ParticleTypes.WITCH,
                                player.getX(), player.getY() + 0.5D, player.getZ(),
                                3, 0.3D, 0.3D, 0.3D, 0.02D
                        );
                    }
                }
            } else if (this.activeSpecialTicks == 36) {
                // Tick 36 (1.8s): Reproduce "sonido_ataqueespecial1" y lanza a los jugadores a ~40 bloques de altura
                world.playSound(
                        null, this.getX(), this.getY(), this.getZ(),
                        ModSounds.SONIDO_ATAQUE_ESPECIAL_1, SoundCategory.HOSTILE, 2.5F, 1.0F
                );
                world.spawnParticles(
                        ParticleTypes.GUST_EMITTER_LARGE,
                        this.getX(), this.getY() + 1.0D, this.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D
                );

                // Velocidad vertical ~2.85D impulsa al jugador unos 40 bloques de altura en Minecraft
                double launchVy = 2.85D * this.getDamageMultiplier();
                for (PlayerEntity player : nearbyPlayers) {
                    player.setVelocity(player.getVelocity().x * 0.2D, launchVy, player.getVelocity().z * 0.2D);
                    player.velocityModified = true;
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
                    }
                    world.spawnParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY(), player.getZ(),
                            10, 0.35D, 0.35D, 0.35D, 0.06D
                    );
                }
            }

            if (this.activeSpecialTicks >= 64) {
                finishSpecialAttack();
            }
        }
    }

    private void finishSpecialAttack() {
        this.activeSpecialType = 0;
        this.activeSpecialTicks = 0;
        this.specialCooldown = this.isDevilMode() ? 55 : 80;
        if (!this.isDevilMode()) {
            this.setNoGravity(false);
        }
    }

    private void summonRandomChildAround(ServerWorld world, LivingEntity target) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double radius = 2.5D + this.random.nextDouble() * 3.5D;
        double sx = this.getX() + Math.cos(angle) * radius;
        double sz = this.getZ() + Math.sin(angle) * radius;
        double sy = findGroundY(world, BlockPos.ofFloored(sx, this.getY(), sz));

        HostileEntity child = switch (this.random.nextInt(4)) {
            case 0 -> ModEntities.YOSTIN_BIBER.create(world, SpawnReason.MOB_SUMMONED);
            case 1 -> ModEntities.VITOR_JUGO.create(world, SpawnReason.MOB_SUMMONED);
            case 2 -> ModEntities.FER_OVILLA.create(world, SpawnReason.MOB_SUMMONED);
            default -> ModEntities.VALENTINA_PRIETA.create(world, SpawnReason.MOB_SUMMONED);
        };

        if (child != null) {
            child.refreshPositionAndAngles(sx, sy, sz, this.random.nextFloat() * 360.0F, 0.0F);
            if (target != null) {
                child.setTarget(target);
            }
            world.spawnEntity(child);
            world.spawnParticles(
                    ModParticles.WEED_LEAF,
                    sx, sy + 0.6D, sz,
                    10, 0.35D, 0.4D, 0.35D, 0.05D
            );
            world.playSound(
                    null, sx, sy, sz,
                    SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 1.0F, 1.2F
            );
        }
    }

    /**
     * Genera la ráfaga del nuevo ítem proyectil "Toalla Kotex" simultáneamente en el cielo
     * cubriendo un alcance de 15 bloques hacia cada lado alrededor del objetivo (-15 a +15 = 30x30 bloques).
     */
    private void spawnKotexBarrageInSky(ServerWorld world, LivingEntity target) {
        double centerX = target.getX();
        double centerZ = target.getZ();
        double skyY = findGroundY(world, target.getBlockPos()) + 12.0D;
        float dmg = 8.0F * this.getDamageMultiplier(); // 4 corazones base (8.0F) o +20% en Modo Diablo

        world.playSound(
                null, centerX, skyY, centerZ,
                SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 1.8F, 1.1F
        );

        // Cuadrícula 9x9 distribuida a 15 bloques por cada lado (-15.0 a +15.0 en X y Z)
        for (int gx = -4; gx <= 4; gx++) {
            for (int gz = -4; gz <= 4; gz++) {
                double px = centerX + (gx * 3.75D) + ((this.random.nextDouble() - 0.5D) * 1.4D);
                double pz = centerZ + (gz * 3.75D) + ((this.random.nextDouble() - 0.5D) * 1.4D);
                // Algunas Toallas Kotex que quedan clavadas en el suelo se pueden recoger (~15%)
                boolean allowPickup = this.random.nextInt(7) == 0;
                KotexProjectileEntity kotex = new KotexProjectileEntity(world, this, px, skyY, pz, 22, dmg, allowPickup);
                world.spawnEntity(kotex);
            }
        }
    }

    private void maintainHoverHeight(ServerWorld world, double heightAboveGround, @Nullable LivingEntity target) {
        this.setNoGravity(true);
        double groundY = findGroundY(world, this.getBlockPos());
        double desiredY = groundY + heightAboveGround;
        double dy = (desiredY - this.getY()) * 0.18D;

        double vx = 0.0D;
        double vz = 0.0D;
        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizDistSq = dx * dx + dz * dz;
            if (horizDistSq > 100.0D) {
                double len = Math.sqrt(horizDistSq);
                vx = (dx / len) * 0.18D;
                vz = (dz / len) * 0.18D;
            }
        }
        this.setVelocity(vx, Math.clamp(dy, -0.35D, 0.35D), vz);
        this.fallDistance = 0.0F;
    }

    private double findGroundY(ServerWorld world, BlockPos pos) {
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return Math.max(world.getBottomY() + 1, topY);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isInSpawnCinematic() || this.isDyingCinematic()) {
            return false;
        }

        // Si el golpe va a matar a Jefri, iniciar la Cinemática Final de Muerte en Jaula (sin música)
        if (this.getHealth() - amount <= 0.0F) {
            this.setHealth(1.0F);
            startDeathCageCinematic(world);
            return true;
        }

        boolean damaged = super.damage(world, source, amount);
        if (damaged && !this.isDevilMode()) {
            // Activar ventana de 5 segundos (100 ticks) de "recibiendo daño en suelo" para el contador de 12 segundos
            this.recentDamageWindowTicks = 100;
        }
        return damaged;
    }

    private void startDeathCageCinematic(ServerWorld world) {
        this.activeSpecialType = 0;
        this.dataTracker.set(IN_COMBAT, false);
        this.dataTracker.set(DYING_CINEMATIC, true);
        this.deathCageTicks = 0;
        this.anchorGroundY = findGroundY(world, this.getBlockPos());
        this.setNoGravity(false);
        // Cae bruscamente al suelo
        this.setVelocity(0.0D, -1.8D, 0.0D);
        this.velocityModified = true;
        this.triggerAnim("actionController", "death_cage");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.8F, 0.75F
        );
    }

    private void tickDeathCageCinematic(ServerWorld world) {
        this.deathCageTicks++;
        this.getNavigation().stop();
        this.bossBar.setPercent(0.0F);
        applyCinematicPlayerImmunityAndFreezeMobs(world);

        // Caída brusca hasta el suelo en los primeros ticks
        if (this.getY() > this.anchorGroundY + 0.1D && this.deathCageTicks < 14) {
            this.setVelocity(0.0D, -1.8D, 0.0D);
        } else {
            this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);
        }

        // En el tick 16 (0.8s): aparece encerrado tras rejas como una jaula
        if (this.deathCageTicks == 16 && !this.cageBlocksPlaced) {
            this.cageBlocksPlaced = true;
            placeTemporaryCageBars(world, this.getBlockPos());
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 1.6F, 0.8F
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 2.0F, 0.7F
            );
        }

        // Al terminar los 5 segundos (100 ticks): limpiar rejas temporales, descongelar mobs, reproducir sonido de pedo y desaparecer
        if (this.deathCageTicks >= 100) {
            // Otorgar unos ticks adicionales de inmunidad al terminar la animación por seguridad
            Box area = this.getBoundingBox().expand(80.0D);
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive)) {
                player.fallDistance = 0.0F;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, true, false, false));
            }

            removeTemporaryCageBars(world);
            releaseCinematicFrozenMobs(world);
            world.spawnParticles(
                    ParticleTypes.POOF,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    35, 0.6D, 0.8D, 0.6D, 0.05D
            );
            // Soltar entre 16 y 24 Toallas Kotex como recompensa al derrotar a Jefri
            this.dropStack(world, new ItemStack(ModItem.TOALLA_KOTEX, 16 + this.random.nextInt(9)));

            // Reproducir el sonido de pedo justo en el instante en que despawnea
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.JEFRI_DEATH_FART, SoundCategory.HOSTILE, 2.5F, 1.0F
            );
            this.bossBar.clearPlayers();
            this.discard();
        }
    }

    private void placeTemporaryCageBars(ServerWorld world, BlockPos center) {
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = center.add(dx, dy, dz);
                    if (world.getBlockState(p).isAir()) {
                        world.setBlockState(p, bars, 3);
                        this.placedCagePositions.add(p);
                    }
                }
            }
        }
    }

    private void removeTemporaryCageBars(ServerWorld world) {
        for (BlockPos p : this.placedCagePositions) {
            if (world.getBlockState(p).isOf(Blocks.IRON_BARS)) {
                world.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            }
        }
        this.placedCagePositions.clear();
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
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.spawnInitialized = true;
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (this.isDevilMode()) {
                return state.setAndContinue(DEVIL_IDLE_ANIM);
            }
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));

        controllers.add(
                new AnimationController<>(this, "actionController", 2, state -> software.bernie.geckolib.animation.PlayState.STOP)
                        .triggerableAnim("spawn_cinematic", SPAWN_CINEMATIC_ANIM)
                        .triggerableAnim("special_summon", SPECIAL_SUMMON_ANIM)
                        .triggerableAnim("special_kotex", SPECIAL_KOTEX_ANIM)
                        .triggerableAnim("special_telekinesis", SPECIAL_TELEKINESIS_ANIM)
                        .triggerableAnim("push_wave", PUSH_WAVE_ANIM)
                        .triggerableAnim("devil_transform", DEVIL_TRANSFORM_ANIM)
                        .triggerableAnim("death_cage", DEATH_CAGE_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
