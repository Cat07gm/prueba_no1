package pruebano1.modid.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Jefe Final: José Torres (Fase 1) -> Jhoseph towers (Fase 2 al bajar al 50% de vida).
 * - Un poco más fuerte que Jefri Penstein: 650 de vida (325 corazones) y 12 de armadura.
 * - Se invoca haciendo clic derecho con el "Acordeon de José Torres" en el bloque "Cassete de Podcast"
 *   (ubicado en la nueva estructura "Escenario del Podcast").
 * - Cinemática de entrada (5 segundos = 100 ticks): agarra su acordeón y empieza a tocarlo al ritmo de la
 *   música del boss. Nunca ataca cuerpo a cuerpo y siempre toca su acordeón en ambas fases.
 * - En Fase 2 (Jhoseph towers, vestido completamente de blanco): empuja a los jugadores que se acerquen
 *   a él parecido a la mecánica de Jefri Penstein, causando mucho daño (15.0F, un poco menos que Dialecto).
 * - 4 Ataques Especiales equilibrados con espacio entre ataques para que el jugador pueda contraatacar:
 *   1. "Zombies" (cada 1 minuto): audio "Zombies", partículas de Virus Encapsulado e invoca de 3 a 6 zombies
 *      (en Fase 2 salen con Fuerza I y posibilidad de aparecer equipados).
 *   2. "Dialecto" (cada 1 minuto): audio de Dialecto, flota muy alto (16 bloques) elevando a los jugadores,
 *      los empuja provocando mucho daño (18.0F / 22.0F) y luego baja lentamente.
 *   3. "La Bola de Hielo Ráfaga" (cada 8-9s): lanza ráfagas de Bolas de Hielo (nuevo proyectil con textura
 *      y sonidos de hielo al impactar, daño 11.0F / 13.0F un poco más fuerte que la Toalla Kotex + Congelación).
 *   4. "Una Bola de Hielo" (cada 45s): suena el audio del Sol / Bola de Hielo, se eleva y carga sobre él una
 *      gran bola de hielo más grande que el bloque de billetes de Pi Didi; al terminar el audio la lanza al
 *      jugador más cercano causando muchísimo daño (28.0F / 32.0F) y empujando todo alrededor.
 * - Cinemática Final de Muerte (5 segundos): al ser derrotado detiene la música, se congela temblando y
 *   estalla en una gran explosión de hielo con mensaje de victoria y botín épico.
 */
public class JoseTorresEntity extends HostileEntity implements GeoEntity {

    public static final int SPAWN_CINEMATIC_DURATION_TICKS = 100; // 5.0 segundos (100 ticks)
    public static final int DEATH_CINEMATIC_DURATION_TICKS = 100; // 5.0 segundos (100 ticks)

    public static final int ZOMBIE_ATTACK_COOLDOWN_TICKS = 20 * 60;      // 1 minuto (1200 ticks)
    public static final int DIALECTO_ATTACK_COOLDOWN_TICKS = 20 * 60;    // 1 minuto (1200 ticks)
    public static final int GIANT_ICE_BALL_COOLDOWN_TICKS = 20 * 45;     // 45 segundos (900 ticks)
    public static final int ICE_BARRAGE_COOLDOWN_PHASE1_TICKS = 20 * 9;  // 9 segundos (180 ticks)
    public static final int ICE_BARRAGE_COOLDOWN_PHASE2_TICKS = 20 * 7;  // 7 segundos (140 ticks)

    public static final int SPECIAL_NONE = 0;
    public static final int SPECIAL_ZOMBIES = 1;
    public static final int SPECIAL_DIALECTO = 2;
    public static final int SPECIAL_ICE_BARRAGE = 3;
    public static final int SPECIAL_GIANT_ICE_BALL = 4;

    private static final double DIALECTO_FLOAT_HEIGHT = 16.0D;
    private static final double GIANT_ICE_FLOAT_HEIGHT = 7.0D;

    private static final RawAnimation SPAWN_CINEMATIC_ANIM = RawAnimation.begin().thenPlay("spawn_cinematic");
    private static final RawAnimation IDLE_ACCORDION_ANIM = RawAnimation.begin().thenLoop("idle_accordion");
    private static final RawAnimation WALK_ACCORDION_ANIM = RawAnimation.begin().thenLoop("walk_accordion");
    private static final RawAnimation PHASE2_TRANSFORM_ANIM = RawAnimation.begin().thenPlay("phase2_transform");
    private static final RawAnimation PHASE2_PUSH_ANIM = RawAnimation.begin().thenPlay("phase2_push");
    private static final RawAnimation SPECIAL_ZOMBIES_ANIM = RawAnimation.begin().thenPlay("special_zombies");
    private static final RawAnimation SPECIAL_DIALECTO_ANIM = RawAnimation.begin().thenPlay("special_dialecto");
    private static final RawAnimation SPECIAL_RAFAGA_HIELO_ANIM = RawAnimation.begin().thenPlay("special_rafaga_hielo");
    private static final RawAnimation SPECIAL_BOLA_HIELO_ANIM = RawAnimation.begin().thenPlay("special_bola_hielo");
    private static final RawAnimation DEATH_CINEMATIC_ANIM = RawAnimation.begin().thenPlay("death_cinematic");

    private static final DustParticleEffect VIRUS_BLUE_DUST = new DustParticleEffect(0x1E90FF, 1.35F);
    private static final DustParticleEffect DIALECTO_CYAN_DUST = new DustParticleEffect(0x00E5FF, 1.45F);
    private static final BlockStateParticleEffect BLUE_ICE_PARTICLE =
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BLUE_ICE.getDefaultState());
    private static final BlockStateParticleEffect PACKED_ICE_PARTICLE =
            new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.PACKED_ICE.getDefaultState());

    private static final String CINEMATIC_FROZEN_TAG = "jose_torres_cinematic_frozen";

    private static final TrackedData<Boolean> IN_COMBAT = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Boolean> PHASE_2 = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Integer> SPAWN_CINEMATIC_TICKS = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Boolean> DYING_CINEMATIC = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.BOOLEAN
    );
    private static final TrackedData<Integer> ACTIVE_SPECIAL_TYPE = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.INTEGER
    );
    private static final TrackedData<Integer> SPECIAL_VOICE_SEQ = DataTracker.registerData(
            JoseTorresEntity.class, TrackedDataHandlerRegistry.INTEGER
    );

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.prueba_no1.jose_torres"),
            BossBar.Color.YELLOW,
            BossBar.Style.NOTCHED_10
    );

    private boolean spawnInitialized = false;
    private double anchorGroundY = 64.0D;

    // Contadores de ataques especiales y equilibrio de combate
    private int activeSpecialTicks = 0;
    private int globalAttackGapTicks = 30;       // Espacio entre ataques para que el jugador pueda atacar
    private int iceBarrageCooldown = 50;         // Tercer ataque especial: "La Bola de Hielo Ráfaga"
    private int zombieAttackCooldown = 90;       // Primer ataque especial: "Zombies" (luego cada 1 min = 1200 ticks)
    private int giantIceBallCooldown = 220;      // Cuarto ataque especial: "Una Bola de Hielo" (luego cada 45s = 900 ticks)
    private int dialectoAttackCooldown = 420;    // Segundo ataque especial: "Dialecto" (luego cada 1 min = 1200 ticks)

    // Empuje defensivo en Fase 2 (Jhoseph towers) cuando el jugador se acerca
    private int phase2PushCooldown = 0;

    // Cinemática final de muerte
    private int deathCinematicTicks = 0;

    private int zombiesToSummonCount = 4;
    private final Set<UUID> elevatedPlayerUuids = new HashSet<>();

    public JoseTorresEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
        this.experiencePoints = 750;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(IN_COMBAT, false);
        builder.add(PHASE_2, false);
        builder.add(SPAWN_CINEMATIC_TICKS, 0);
        builder.add(DYING_CINEMATIC, false);
        builder.add(ACTIVE_SPECIAL_TYPE, SPECIAL_NONE);
        builder.add(SPECIAL_VOICE_SEQ, 0);
    }

    public boolean isPhase2() {
        return this.dataTracker.get(PHASE_2);
    }

    /** Permite previsualizar la Fase 2 (Jhoseph towers blanco) en el visor 3D de la Enciclopedia. */
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

    public int getActiveSpecialType() {
        return this.dataTracker.get(ACTIVE_SPECIAL_TYPE);
    }

    public int getSpecialVoiceSeq() {
        return this.dataTracker.get(SPECIAL_VOICE_SEQ);
    }

    /**
     * Música de Fase 1: suena desde que inicia la cinemática de entrada (5s)
     * y dura todo el combate de la primera fase de José Torres.
     */
    public boolean shouldPlayPhase1Music() {
        return this.isAlive()
                && !this.isDyingCinematic()
                && !this.isPhase2()
                && (this.isInSpawnCinematic() || this.dataTracker.get(IN_COMBAT));
    }

    /**
     * Música de Fase 2 (Jhoseph towers): suena durante toda la segunda fase mientras está en combate,
     * y se detiene inmediatamente al iniciar la cinemática final de muerte.
     */
    public boolean shouldPlayPhase2Music() {
        return this.isAlive()
                && !this.isDyingCinematic()
                && this.isPhase2()
                && this.dataTracker.get(IN_COMBAT);
    }

    public static DefaultAttributeContainer.Builder createJoseTorresAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 650.0D) // Un poco más fuerte que Jefri Penstein (500 HP)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.29D)
                .add(EntityAttributes.ATTACK_DAMAGE, 0.0D) // No ataca cuerpo a cuerpo
                .add(EntityAttributes.ARMOR, 12.0D)       // Un poco más blindado que Jefri Penstein (10)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(EntityAttributes.FOLLOW_RANGE, 56.0D)
                .add(EntityAttributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void initGoals() {
        // Sin MeleeAttackGoal porque José Torres NUNCA ataca cuerpo a cuerpo; sólo toca el acordeón y usa ataques especiales
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 32.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /**
     * Inicia la cinemática de entrada de 5 segundos (100 ticks) donde José Torres
     * agarra su acordeón y empieza a tocarlo al ritmo de su música.
     */
    public void startSpawnCinematic(ServerWorld world, double spawnX, double spawnY, double spawnZ) {
        this.spawnInitialized = true;
        this.anchorGroundY = spawnY;
        this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 1);
        this.dataTracker.set(IN_COMBAT, true);
        this.refreshPositionAndAngles(spawnX, spawnY, spawnZ, this.getYaw(), 0.0F);
        this.triggerAnim("actionController", "spawn_cinematic");

        world.playSound(
                null, spawnX, spawnY + 1.0D, spawnZ,
                SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(), SoundCategory.HOSTILE, 1.8F, 1.0F
        );
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        // 0. CINEMÁTICA FINAL DE MUERTE (5 SEGUNDOS CON EXPLOSIÓN DE HIELO Y TÍTULO DE VICTORIA)
        if (this.isDyingCinematic()) {
            tickDeathCinematic(world);
            return;
        }

        this.bossBar.setPercent(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));

        if (!this.spawnInitialized) {
            double groundY = findGroundY(world, this.getBlockPos());
            startSpawnCinematic(world, this.getX(), groundY, this.getZ());
        }

        // 1. CINEMÁTICA DE ENTRADA DE 5 SEGUNDOS (100 TICKS) TOCANDO EL ACORDEÓN
        if (this.isInSpawnCinematic()) {
            tickSpawnCinematic(world);
            return;
        }

        // 2. CAMBIO A FASE 2 ("Jhoseph towers" completamente de blanco) AL LLEGAR A LA MITAD DE VIDA (<= 50%)
        if (!this.isPhase2() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            activatePhase2JhosephTowers(world);
        }

        // 3. SIEMPRE TOCANDO SU ACORDEÓN CON NOTAS MUSICALES (TANTO EN FASE 1 COMO EN FASE 2)
        if (this.age % 6 == 0) {
            double noteColor = (this.age % 24) / 24.0D;
            world.spawnParticles(
                    ParticleTypes.NOTE,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.1D,
                    this.getY() + 1.35D + this.random.nextDouble() * 0.5D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.1D,
                    0, noteColor, 0.0D, 0.0D, 1.0D
            );
        }

        // Reducir los contadores de recarga de los 4 ataques especiales y del empuje de Fase 2
        if (this.iceBarrageCooldown > 0) {
            this.iceBarrageCooldown--;
        }
        if (this.zombieAttackCooldown > 0) {
            this.zombieAttackCooldown--;
        }
        if (this.giantIceBallCooldown > 0) {
            this.giantIceBallCooldown--;
        }
        if (this.dialectoAttackCooldown > 0) {
            this.dialectoAttackCooldown--;
        }
        if (this.phase2PushCooldown > 0) {
            this.phase2PushCooldown--;
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
            if (this.getActiveSpecialType() == SPECIAL_NONE) {
                this.setNoGravity(false);
            }
            return;
        }

        this.dataTracker.set(IN_COMBAT, true);
        this.getLookControl().lookAt(target, 30.0F, 30.0F);

        // 4. PROCESAR ATAQUE ESPECIAL ACTIVO
        if (this.getActiveSpecialType() != SPECIAL_NONE) {
            tickActiveSpecialAttack(world, target);
            return;
        }

        this.setNoGravity(false);

        // 5. EMPUJE DEFENSIVO EN SEGUNDA FASE (JHOSEPH TOWERS) CUANDO EL JUGADOR SE ACERCA
        if (this.isPhase2() && this.phase2PushCooldown <= 0) {
            List<PlayerEntity> closePlayers = world.getEntitiesByClass(
                    PlayerEntity.class,
                    this.getBoundingBox().expand(4.6D, 2.8D, 4.6D),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
            );
            if (!closePlayers.isEmpty()) {
                triggerPhase2ProximityPushWave(world);
                return;
            }
        }

        // 6. POSICIONAMIENTO SIN ATAQUE CUERPO A CUERPO (camina tocando el acordeón manteniendo distancia media)
        double distSq = this.squaredDistanceTo(target);
        if (distSq > 90.0D) {
            this.getNavigation().startMovingTo(target, 1.0D);
        } else if (distSq < 30.0D) {
            this.getNavigation().stop();
        }

        // 7. VENTANA DE ESPACIO ENTRE ATAQUES PARA QUE EL JUGADOR PUEDA GOLPEARLO
        if (this.globalAttackGapTicks > 0) {
            this.globalAttackGapTicks--;
            return;
        }

        // 8. ELEGIR SIGUIENTE ATAQUE ESPECIAL DISPONIBLE (priorizando ataques con gran cooldown cuando están listos)
        if (this.zombieAttackCooldown <= 0) {
            startZombieWaveSpecialAttack(world);
        } else if (this.giantIceBallCooldown <= 0) {
            startGiantIceBallSpecialAttack(world);
        } else if (this.dialectoAttackCooldown <= 0) {
            startDialectoSpecialAttack(world);
        } else if (this.iceBarrageCooldown <= 0) {
            startIceBarrageSpecialAttack(world);
        }
    }

    /**
     * Empuje de Segunda Fase (Jhoseph towers):
     * Cuando los jugadores se acercan a él en su segunda fase, los empuja parecido a la mecánica de
     * Jefri Penstein, causando mucho daño (15.0F = 7.5 corazones, un poco menos que el ataque de Dialecto).
     */
    private void triggerPhase2ProximityPushWave(ServerWorld world) {
        this.phase2PushCooldown = 160; // 8 segundos de recarga para permitir ventanas de ataque cuerpo a cuerpo
        this.globalAttackGapTicks = Math.max(this.globalAttackGapTicks, 35);
        this.triggerAnim("actionController", "phase2_push");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.7F, 1.15F
        );
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.8F, 0.85F
        );
        world.spawnParticles(
                ParticleTypes.GUST_EMITTER_LARGE,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D
        );
        world.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                this.getX(), this.getY() + 1.0D, this.getZ(),
                35, 1.8D, 0.8D, 1.8D, 0.12D
        );

        List<LivingEntity> nearby = world.getEntitiesByClass(
                LivingEntity.class,
                this.getBoundingBox().expand(7.5D, 3.5D, 7.5D),
                e -> e != this && !(e instanceof ZombieEntity) && e.isAlive() && !e.isSpectator()
        );
        for (LivingEntity entity : nearby) {
            if (entity instanceof PlayerEntity player && player.isCreative()) {
                continue;
            }
            // Mucho daño, un poco menos que el ataque de Dialecto (15.0F = 7.5 corazones frente a 18.0F / 22.0F)
            entity.damage(world, this.getDamageSources().mobAttack(this), 15.0F);
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 3, 1), this);
            entity.setFrozenTicks(Math.min(entity.getMinFreezeDamageTicks() + 80, entity.getFrozenTicks() + 100));

            Vec3d dir = entity.getPos().subtract(this.getPos());
            Vec3d horiz = new Vec3d(dir.x, 0.0D, dir.z);
            if (horiz.lengthSquared() < 0.001D) {
                horiz = new Vec3d(1.0D, 0.0D, 0.0D);
            } else {
                horiz = horiz.normalize();
            }
            entity.setVelocity(horiz.x * 2.1D, 0.68D, horiz.z * 2.1D);
            entity.velocityModified = true;
            if (entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
            }
        }
    }

    /**
     * Cinemática de entrada de 5 segundos (100 ticks):
     * José Torres agarra su acordeón y lo toca al ritmo de la música de la Fase 1.
     */
    private void tickSpawnCinematic(ServerWorld world) {
        int ticks = this.getSpawnCinematicTicks();
        this.getNavigation().stop();
        this.setVelocity(0.0D, Math.min(0.0D, this.getVelocity().y), 0.0D);
        applyCinematicPlayerImmunityAndFreezeMobs(world);

        PlayerEntity closest = world.getClosestPlayer(this, 48.0D);
        if (closest != null) {
            this.getLookControl().lookAt(closest, 30.0F, 30.0F);
        }

        if (ticks >= 14 && ticks % 4 == 0) {
            double noteColor = ((ticks * 3) % 24) / 24.0D;
            world.spawnParticles(
                    ParticleTypes.NOTE,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 1.3D,
                    this.getY() + 1.25D + this.random.nextDouble() * 0.65D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 1.3D,
                    0, noteColor, 0.0D, 0.0D, 1.0D
            );
        }

        if (ticks >= SPAWN_CINEMATIC_DURATION_TICKS) {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, 0);
            this.globalAttackGapTicks = 30;
            this.iceBarrageCooldown = 40;
            this.zombieAttackCooldown = 110;
            this.giantIceBallCooldown = 240;
            this.dialectoAttackCooldown = 480;
            releaseCinematicFrozenMobs(world);
        } else {
            this.dataTracker.set(SPAWN_CINEMATIC_TICKS, ticks + 1);
        }
    }

    /**
     * Activa la Fase 2 ("Jhoseph towers" completamente de blanco) al bajar al 50% de vida.
     */
    private void activatePhase2JhosephTowers(ServerWorld world) {
        this.dataTracker.set(PHASE_2, true);
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_NONE);
        this.activeSpecialTicks = 0;
        this.elevatedPlayerUuids.clear();
        this.setNoGravity(false);
        this.globalAttackGapTicks = 45;
        this.zombieAttackCooldown = Math.min(this.zombieAttackCooldown, 80);
        this.giantIceBallCooldown = Math.min(this.giantIceBallCooldown, 200);
        this.dialectoAttackCooldown = Math.min(this.dialectoAttackCooldown, 360);

        this.bossBar.setColor(BossBar.Color.WHITE);
        this.bossBar.setName(Text.translatable("entity.prueba_no1.jhoseph_towers").formatted(Formatting.WHITE, Formatting.BOLD));
        this.triggerAnim("actionController", "phase2_transform");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.8F, 1.05F
        );
        world.spawnParticles(
                ParticleTypes.END_ROD,
                this.getX(), this.getY() + 1.1D, this.getZ(),
                45, 0.8D, 1.0D, 0.8D, 0.12D
        );

        List<ServerPlayerEntity> players = world.getPlayers(p -> p.squaredDistanceTo(this) <= 6400.0D);
        for (ServerPlayerEntity player : players) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 50, 15));
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.literal("Jhoseph towers").formatted(Formatting.WHITE, Formatting.BOLD)
            ));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(
                    Text.literal("¡Llegó la pizza!").formatted(Formatting.AQUA)
            ));
        }
    }

    /**
     * Ataque Especial 1: Invocación de Zombies (cooldown de 1 minuto = 1200 ticks).
     * - Reproduce el audio "Zombies".
     * - Invoca de 3 a 6 zombies con partículas del Virus Encapsulado.
     * - En Fase 2 (Jhoseph towers), los zombies invocados tienen Fuerza I y posibilidad de aparecer equipados.
     */
    private void startZombieWaveSpecialAttack(ServerWorld world) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_ZOMBIES);
        this.dataTracker.set(SPECIAL_VOICE_SEQ, this.getSpecialVoiceSeq() + 1);
        this.activeSpecialTicks = 0;
        this.zombieAttackCooldown = ZOMBIE_ATTACK_COOLDOWN_TICKS; // 1 minuto (1200 ticks)
        this.zombiesToSummonCount = 3 + this.random.nextInt(4);   // De 3 a 6 zombies
        this.triggerAnim("actionController", "special_zombies");
    }

    /**
     * Ataque Especial 2: Ataque de Dialecto (cooldown de 1 minuto = 1200 ticks).
     * - Reproduce el audio de Dialecto (~11 segundos = 220 ticks).
     * - José Torres comienza a flotar muy alto (16 bloques) y eleva a los jugadores cercanos con él,
     *   luego los empuja provocando mucho daño a todos los jugadores que logró elevar,
     *   y después José Torres baja lentamente.
     */
    private void startDialectoSpecialAttack(ServerWorld world) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_DIALECTO);
        this.dataTracker.set(SPECIAL_VOICE_SEQ, this.getSpecialVoiceSeq() + 1);
        this.activeSpecialTicks = 0;
        this.dialectoAttackCooldown = DIALECTO_ATTACK_COOLDOWN_TICKS; // 1 minuto (1200 ticks)
        this.anchorGroundY = findGroundY(world, this.getBlockPos());
        this.elevatedPlayerUuids.clear();
        this.setNoGravity(true);
        this.triggerAnim("actionController", "special_dialecto");

        List<PlayerEntity> nearbyPlayers = world.getEntitiesByClass(
                PlayerEntity.class,
                this.getBoundingBox().expand(20.0D, 12.0D, 20.0D),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
        );
        for (PlayerEntity player : nearbyPlayers) {
            this.elevatedPlayerUuids.add(player.getUuid());
        }
    }

    /**
     * Ataque Especial 3: "La Bola de Hielo Ráfaga" (cooldown equilibrado de 9s en Fase 1 y 7s en Fase 2).
     * - Lanza bolas de hielo en ráfagas (6 a 8 proyectiles en 2 segundos) hacia el jugador.
     * - Cada bola de hielo tiene textura y sonidos de hielo al impactar, causando daño un poco más fuerte
     *   que la Toalla Kotex (11.0F / 13.0F) y aplicando Lentitud/Congelación.
     */
    private void startIceBarrageSpecialAttack(ServerWorld world) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_ICE_BARRAGE);
        this.activeSpecialTicks = 0;
        this.iceBarrageCooldown = this.isPhase2()
                ? ICE_BARRAGE_COOLDOWN_PHASE2_TICKS
                : ICE_BARRAGE_COOLDOWN_PHASE1_TICKS;
        this.triggerAnim("actionController", "special_rafaga_hielo");

        world.playSound(
                null, this.getX(), this.getY() + 1.2D, this.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.8F, 1.2F
        );
    }

    /**
     * Ataque Especial 4: "Una Bola de Hielo" (cooldown de 45 segundos = 900 ticks).
     * - Suena el audio "Todos piensan que el sol es una bola de lumbre, pero el sol es todo lo contrario,
     *   es una bola de hielo" (~11.5 segundos = 230 ticks).
     * - José Torres se eleva y carga sobre su cabeza una gran bola de hielo (más grande que el bloque de billetes
     *   de Pi Didi). Cuando termina el audio, la bola está completamente cargada y la lanza hacia el jugador
     *   más cercano causándole muchísimo daño y empujándolo a él y a todo lo que está alrededor.
     */
    private void startGiantIceBallSpecialAttack(ServerWorld world) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_GIANT_ICE_BALL);
        this.dataTracker.set(SPECIAL_VOICE_SEQ, this.getSpecialVoiceSeq() + 1);
        this.activeSpecialTicks = 0;
        this.giantIceBallCooldown = GIANT_ICE_BALL_COOLDOWN_TICKS;
        this.anchorGroundY = findGroundY(world, this.getBlockPos());
        this.setNoGravity(true);
        this.triggerAnim("actionController", "special_bola_hielo");
    }

    private void tickActiveSpecialAttack(ServerWorld world, LivingEntity target) {
        this.activeSpecialTicks++;
        this.getNavigation().stop();
        int specialType = this.getActiveSpecialType();

        if (specialType == SPECIAL_ZOMBIES) {
            // ATAQUE ESPECIAL 1: OLEADA DE 3 A 6 ZOMBIES + PARTÍCULAS DE VIRUS ENCAPSULADO (76 ticks = 3.8s)
            if (this.activeSpecialTicks % 2 == 0) {
                world.spawnParticles(
                        ModParticles.ENCAPSULATED_VIRUS,
                        this.getX(), this.getY() + 1.25D, this.getZ(),
                        4, 1.35D, 0.65D, 1.35D, 0.04D
                );
                world.spawnParticles(
                        VIRUS_BLUE_DUST,
                        this.getX(), this.getY() + 1.0D, this.getZ(),
                        5, 1.1D, 0.5D, 1.1D, 0.02D
                );
            }

            int interval = Math.max(7, 50 / Math.max(1, this.zombiesToSummonCount));
            if (this.activeSpecialTicks >= 16
                    && this.activeSpecialTicks < 16 + (this.zombiesToSummonCount * interval)
                    && (this.activeSpecialTicks - 16) % interval == 0) {
                summonVirusZombieAround(world, target);
            }

            if (this.activeSpecialTicks >= 76) {
                finishSpecialAttack(70); // 3.5s de margen antes del siguiente ataque
            }
        } else if (specialType == SPECIAL_DIALECTO) {
            // ATAQUE ESPECIAL 2: ATAQUE DE DIALECTO (220 ticks = 11.0s)
            tickDialectoSpecialAttack(world);
        } else if (specialType == SPECIAL_ICE_BARRAGE) {
            // ATAQUE ESPECIAL 3: "LA BOLA DE HIELO RÁFAGA" (40 ticks = 2.0s)
            tickIceBarrageSpecialAttack(world, target);
        } else if (specialType == SPECIAL_GIANT_ICE_BALL) {
            // ATAQUE ESPECIAL 4: "UNA BOLA DE HIELO" (236 ticks = 11.8s)
            tickGiantIceBallSpecialAttack(world, target);
        }
    }

    private void finishSpecialAttack(int gapTicksAfter) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_NONE);
        this.activeSpecialTicks = 0;
        this.globalAttackGapTicks = gapTicksAfter;
        this.setNoGravity(false);
    }

    /**
     * Lógica del 3.º ataque especial "La Bola de Hielo Ráfaga" (40 ticks = 2.0s):
     * Dispara una ráfaga rápida de 7 Bolas de Hielo hacia el jugador (cada 5 ticks entre el tick 6 y el 36).
     */
    private void tickIceBarrageSpecialAttack(ServerWorld world, LivingEntity target) {
        this.setVelocity(0.0D, Math.min(0.0D, this.getVelocity().y), 0.0D);

        if (this.activeSpecialTicks % 3 == 0) {
            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 1.3D, this.getZ(),
                    4, 0.55D, 0.45D, 0.55D, 0.04D
            );
        }

        if (this.activeSpecialTicks >= 6 && this.activeSpecialTicks <= 36 && (this.activeSpecialTicks - 6) % 5 == 0) {
            shootSingleIceBall(world, target);
        }

        if (this.activeSpecialTicks >= 40) {
            finishSpecialAttack(60); // 3 segundos libres para que el jugador ataque
        }
    }

    private void shootSingleIceBall(ServerWorld world, LivingEntity target) {
        float dmg = this.isPhase2() ? 13.0F : 11.0F; // Un poco más fuerte que la Toalla Kotex (8.0F / 9.6F)
        IceBallProjectileEntity iceBall = new IceBallProjectileEntity(
                world, this, new ItemStack(ModItem.ICE_BALL), dmg, false
        );

        double startX = this.getX();
        double startY = this.getY() + 1.45D;
        double startZ = this.getZ();
        iceBall.setPosition(startX, startY, startZ);

        double dx = target.getX() - startX;
        double dy = target.getBodyY(0.45D) - startY;
        double dz = target.getZ() - startZ;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        iceBall.setVelocity(dx, dy + horizDist * 0.14D, dz, 1.65F, 3.5F);
        world.spawnEntity(iceBall);

        world.playSound(
                null, startX, startY, startZ,
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 1.3F, 0.85F + this.random.nextFloat() * 0.25F
        );
        world.playSound(
                null, startX, startY, startZ,
                SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.HOSTILE, 0.9F, 1.35F
        );
    }

    /**
     * Lógica del 4.º y último ataque especial "Una Bola de Hielo" (236 ticks = 11.8 segundos):
     * - Ticks 1 a 225 (0.0s - 11.25s): Mientras suena "Todos piensan que el sol es una bola de lumbre,
     *   pero el sol es todo lo contrario, es una bola de hielo", José Torres se eleva a 7 bloques de altura
     *   y carga sobre su cabeza la gran bola de hielo (más grande que el bloque de billetes de Pi Didi).
     * - Tick 226 (11.3s, justo al terminar el audio): La bola está completamente cargada y la lanza hacia el
     *   jugador más cercano causándole muchísimo daño (28.0F / 32.0F) y empujándolo a él y a todo alrededor.
     * - Ticks 227 a 236: Baja suavemente al suelo.
     */
    private void tickGiantIceBallSpecialAttack(ServerWorld world, LivingEntity target) {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;

        PlayerEntity closestPlayer = world.getClosestPlayer(this, 56.0D);
        LivingEntity aimTarget = (closestPlayer != null && closestPlayer.isAlive() && !closestPlayer.isSpectator())
                ? closestPlayer
                : target;
        if (aimTarget != null) {
            this.getLookControl().lookAt(aimTarget, 30.0F, 30.0F);
        }

        if (this.activeSpecialTicks <= 225) {
            // Elevación a 7 bloques sobre el suelo mientras carga la Gran Bola de Hielo encima de él
            double climbProgress = Math.min(1.0D, (double) this.activeSpecialTicks / 45.0D);
            double desiredY = this.anchorGroundY + (GIANT_ICE_FLOAT_HEIGHT * climbProgress);
            double vy = Math.clamp((desiredY - this.getY()) * 0.22D, -0.35D, 0.42D);
            this.setVelocity(0.0D, vy, 0.0D);

            // Efectos especiales crecientes en la Gran Bola de Hielo sobre su cabeza
            double orbY = this.getY() + 2.8D;
            double spread = 0.25D + (this.activeSpecialTicks / 225.0D) * 0.85D;
            if (this.activeSpecialTicks % 2 == 0) {
                world.spawnParticles(
                        ParticleTypes.SNOWFLAKE,
                        this.getX(), orbY, this.getZ(),
                        6, spread, spread, spread, 0.04D
                );
                world.spawnParticles(
                        BLUE_ICE_PARTICLE,
                        this.getX(), orbY, this.getZ(),
                        4, spread * 0.8D, spread * 0.8D, spread * 0.8D, 0.06D
                );
                world.spawnParticles(
                        DIALECTO_CYAN_DUST,
                        this.getX(), orbY, this.getZ(),
                        4, spread * 0.9D, spread * 0.9D, spread * 0.9D, 0.02D
                );
            }
            if (this.activeSpecialTicks % 16 == 0) {
                float pitch = 0.65F + (this.activeSpecialTicks / 225.0F) * 0.75F;
                world.playSound(
                        null, this.getX(), orbY, this.getZ(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 2.0F, pitch
                );
            }
        } else if (this.activeSpecialTicks == 226) {
            // ¡Termina el audio y lanza la Gran Bola de Hielo completamente cargada hacia el jugador más cercano!
            this.setVelocity(0.0D, 0.0D, 0.0D);
            if (aimTarget != null) {
                launchFullyChargedGiantIceBall(world, aimTarget);
            }
        } else {
            // Ticks 227 a 236: Descenso rápido y suave al suelo tras lanzar la bola gigante
            double descendProgress = Math.min(1.0D, (double) (this.activeSpecialTicks - 226) / 10.0D);
            double desiredY = this.anchorGroundY + (GIANT_ICE_FLOAT_HEIGHT * (1.0D - descendProgress));
            this.setPosition(this.getX(), desiredY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);

            if (this.activeSpecialTicks >= 236) {
                this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
                finishSpecialAttack(85); // ~4.25 segundos de ventana para que el jugador se recupere y ataque
            }
        }
    }

    private void launchFullyChargedGiantIceBall(ServerWorld world, LivingEntity target) {
        double startX = this.getX();
        double startY = this.getY() + 2.6D;
        double startZ = this.getZ();

        world.playSound(
                null, startX, startY, startZ,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.5F, 0.95F
        );
        world.playSound(
                null, startX, startY, startZ,
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.2F, 0.75F
        );

        float massiveDamage = this.isPhase2() ? 32.0F : 28.0F; // 14 a 16 corazones + explosión y empuje masivo
        IceBallProjectileEntity giantOrb = new IceBallProjectileEntity(
                world, this, new ItemStack(ModItem.ICE_BALL), massiveDamage, true
        );
        giantOrb.setPosition(startX, startY, startZ);

        double dx = target.getX() - startX;
        double dy = target.getBodyY(0.4D) - startY;
        double dz = target.getZ() - startZ;
        giantOrb.setVelocity(dx, dy, dz, 2.35F, 0.0F);
        world.spawnEntity(giantOrb);
    }

    /**
     * Lógica paso a paso del Ataque Especial de Dialecto (220 ticks = 11 segundos):
     * 1) Ticks 1 a 135 (0.0s - 6.75s): José Torres flota muy alto (hasta 16 bloques sobre el suelo)
     *    y eleva a los jugadores con él.
     * 2) Tick 136 (6.8s): Empuja violentamente a todos los jugadores que logró elevar y les provoca mucho daño.
     * 3) Ticks 137 a 220 (6.85s - 11.0s): José Torres baja lentamente hasta tocar el suelo.
     */
    private void tickDialectoSpecialAttack(ServerWorld world) {
        this.setNoGravity(true);
        this.fallDistance = 0.0F;

        if (this.activeSpecialTicks <= 135) {
            double climbProgress = Math.min(1.0D, (double) this.activeSpecialTicks / 115.0D);
            double smoothClimb = 1.0D - Math.pow(1.0D - climbProgress, 2.0D);
            double desiredBossY = this.anchorGroundY + (DIALECTO_FLOAT_HEIGHT * smoothClimb);
            double bossVy = Math.clamp((desiredBossY - this.getY()) * 0.25D, -0.4D, 0.55D);
            this.setVelocity(0.0D, bossVy, 0.0D);

            if (this.activeSpecialTicks % 2 == 0) {
                world.spawnParticles(
                        DIALECTO_CYAN_DUST,
                        this.getX(), this.getY() + 0.9D, this.getZ(),
                        6, 0.9D, 0.6D, 0.9D, 0.03D
                );
                world.spawnParticles(
                        ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.2D, this.getZ(),
                        3, 0.6D, 0.2D, 0.6D, 0.02D
                );
            }

            if (this.activeSpecialTicks <= 40) {
                List<PlayerEntity> extraPlayers = world.getEntitiesByClass(
                        PlayerEntity.class,
                        this.getBoundingBox().expand(18.0D, 12.0D, 18.0D),
                        p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                );
                for (PlayerEntity p : extraPlayers) {
                    this.elevatedPlayerUuids.add(p.getUuid());
                }
            }

            for (UUID uuid : this.elevatedPlayerUuids) {
                PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player != null && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                    double targetPlayerY = this.getY() + 0.35D;
                    double dy = (targetPlayerY - player.getY()) * 0.24D;
                    double dx = (this.getX() - player.getX()) * 0.03D;
                    double dz = (this.getZ() - player.getZ()) * 0.03D;
                    player.setVelocity(
                            player.getVelocity().x * 0.45D + dx,
                            Math.clamp(dy, -0.2D, 0.62D),
                            player.getVelocity().z * 0.45D + dz
                    );
                    player.fallDistance = 0.0F;
                    player.velocityModified = true;
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
                    }

                    if (this.activeSpecialTicks % 3 == 0) {
                        world.spawnParticles(
                                DIALECTO_CYAN_DUST,
                                player.getX(), player.getY() + 0.5D, player.getZ(),
                                3, 0.35D, 0.4D, 0.35D, 0.02D
                        );
                    }
                }
            }
        } else if (this.activeSpecialTicks == 136) {
            this.setVelocity(0.0D, 0.0D, 0.0D);
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.5F, 1.0F
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.0F, 0.9F
            );
            world.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    this.getX(), this.getY() + 1.1D, this.getZ(),
                    2, 0.5D, 0.5D, 0.5D, 0.0D
            );
            world.spawnParticles(
                    ParticleTypes.GUST_EMITTER_LARGE,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    2, 0.4D, 0.4D, 0.4D, 0.0D
            );

            float dialectoDamage = this.isPhase2() ? 22.0F : 18.0F;
            for (UUID uuid : this.elevatedPlayerUuids) {
                PlayerEntity player = world.getPlayerByUuid(uuid);
                if (player != null && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
                    player.damage(world, this.getDamageSources().mobAttack(this), dialectoDamage);

                    Vec3d dir = player.getPos().subtract(this.getPos());
                    Vec3d horiz = new Vec3d(dir.x, 0.0D, dir.z);
                    if (horiz.lengthSquared() < 0.001D) {
                        double randAngle = this.random.nextDouble() * Math.PI * 2.0D;
                        horiz = new Vec3d(Math.cos(randAngle), 0.0D, Math.sin(randAngle));
                    } else {
                        horiz = horiz.normalize();
                    }

                    player.setVelocity(horiz.x * 2.35D, 0.72D, horiz.z * 2.35D);
                    player.velocityModified = true;
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        serverPlayer.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(serverPlayer));
                    }

                    world.spawnParticles(
                            ParticleTypes.SONIC_BOOM,
                            player.getX(), player.getY() + 0.8D, player.getZ(),
                            1, 0.0D, 0.0D, 0.0D, 0.0D
                    );
                }
            }
            this.elevatedPlayerUuids.clear();
        } else {
            // Ticks 137 a 220: José Torres baja lentamente hasta tocar el suelo
            double descendProgress = Math.min(1.0D, (double) (this.activeSpecialTicks - 136) / 84.0D);
            double desiredBossY = this.anchorGroundY + (DIALECTO_FLOAT_HEIGHT * (1.0D - descendProgress));
            double dy = desiredBossY - this.getY();
            this.setVelocity(0.0D, Math.clamp(dy * 0.25D, -0.28D, 0.25D), 0.0D);

            if (this.activeSpecialTicks % 3 == 0) {
                world.spawnParticles(
                        ParticleTypes.CLOUD,
                        this.getX(), this.getY() + 0.15D, this.getZ(),
                        2, 0.45D, 0.15D, 0.45D, 0.01D
                );
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        this.getX(), this.getY() + 0.9D, this.getZ(),
                        2, 0.5D, 0.5D, 0.5D, 0.02D
                );
            }

            if (this.activeSpecialTicks >= 220) {
                this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
                this.setVelocity(0.0D, 0.0D, 0.0D);
                finishSpecialAttack(80); // 4 segundos libres tras aterrizar
            }
        }
    }

    private void summonVirusZombieAround(ServerWorld world, LivingEntity target) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        double radius = 2.5D + this.random.nextDouble() * 4.0D;
        double sx = this.getX() + Math.cos(angle) * radius;
        double sz = this.getZ() + Math.sin(angle) * radius;
        double sy = findGroundY(world, BlockPos.ofFloored(sx, this.getY(), sz));

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, SpawnReason.MOB_SUMMONED);
        if (zombie != null) {
            zombie.refreshPositionAndAngles(sx, sy, sz, this.random.nextFloat() * 360.0F, 0.0F);
            zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 180, 0, false, false));

            if (this.isPhase2()) {
                zombie.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 180, 0, false, true));
                maybeEquipPhase2Zombie(zombie);
            }

            if (target != null) {
                zombie.setTarget(target);
            }
            world.spawnEntity(zombie);

            world.spawnParticles(
                    ModParticles.ENCAPSULATED_VIRUS,
                    sx, sy + 0.9D, sz,
                    12, 0.45D, 0.65D, 0.45D, 0.06D
            );
            world.spawnParticles(
                    VIRUS_BLUE_DUST,
                    sx, sy + 0.7D, sz,
                    10, 0.4D, 0.5D, 0.4D, 0.03D
            );
            world.playSound(
                    null, sx, sy, sz,
                    SoundEvents.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 1.3F, 0.95F + this.random.nextFloat() * 0.2F
            );
        }
    }

    private void maybeEquipPhase2Zombie(ZombieEntity zombie) {
        if (this.random.nextFloat() < 0.65F) {
            ItemStack weapon = switch (this.random.nextInt(4)) {
                case 0 -> new ItemStack(Items.IRON_SWORD);
                case 1 -> new ItemStack(Items.IRON_AXE);
                case 2 -> new ItemStack(Items.GOLDEN_SWORD);
                default -> new ItemStack(Items.DIAMOND_SWORD);
            };
            zombie.equipStack(EquipmentSlot.MAINHAND, weapon);
            zombie.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        if (this.random.nextFloat() < 0.55F) {
            ItemStack helmet = switch (this.random.nextInt(4)) {
                case 0 -> new ItemStack(Items.IRON_HELMET);
                case 1 -> new ItemStack(Items.CHAINMAIL_HELMET);
                case 2 -> new ItemStack(Items.GOLDEN_HELMET);
                default -> new ItemStack(Items.DIAMOND_HELMET);
            };
            zombie.equipStack(EquipmentSlot.HEAD, helmet);
            zombie.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
        }
        if (this.random.nextFloat() < 0.50F) {
            ItemStack chest = switch (this.random.nextInt(4)) {
                case 0 -> new ItemStack(Items.IRON_CHESTPLATE);
                case 1 -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                case 2 -> new ItemStack(Items.GOLDEN_CHESTPLATE);
                default -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            };
            zombie.equipStack(EquipmentSlot.CHEST, chest);
            zombie.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0F);
        }
        if (this.random.nextFloat() < 0.45F) {
            ItemStack legs = switch (this.random.nextInt(3)) {
                case 0 -> new ItemStack(Items.IRON_LEGGINGS);
                case 1 -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                default -> new ItemStack(Items.GOLDEN_LEGGINGS);
            };
            zombie.equipStack(EquipmentSlot.LEGS, legs);
            zombie.setEquipmentDropChance(EquipmentSlot.LEGS, 0.0F);
        }
        if (this.random.nextFloat() < 0.45F) {
            ItemStack boots = switch (this.random.nextInt(3)) {
                case 0 -> new ItemStack(Items.IRON_BOOTS);
                case 1 -> new ItemStack(Items.CHAINMAIL_BOOTS);
                default -> new ItemStack(Items.DIAMOND_BOOTS);
            };
            zombie.equipStack(EquipmentSlot.FEET, boots);
            zombie.setEquipmentDropChance(EquipmentSlot.FEET, 0.0F);
        }
    }

    private void applyCinematicPlayerImmunityAndFreezeMobs(ServerWorld world) {
        Box area = this.getBoundingBox().expand(80.0D);
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive);
        for (PlayerEntity player : players) {
            player.fallDistance = 0.0F;
            player.setFireTicks(0);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10, 4, true, false, false));
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

    private double findGroundY(ServerWorld world, BlockPos pos) {
        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return Math.max(world.getBottomY() + 1, topY);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.isInAnyCinematic()) {
            return false;
        }

        // Si el golpe va a derrotar a José Torres / Jhoseph towers, iniciar su Cinemática Final de Muerte con Explosión de Hielo
        if (this.getHealth() - amount <= 0.0F) {
            this.setHealth(1.0F);
            startDeathCinematic(world);
            return true;
        }

        return super.damage(world, source, amount);
    }

    private void startDeathCinematic(ServerWorld world) {
        this.dataTracker.set(ACTIVE_SPECIAL_TYPE, SPECIAL_NONE);
        this.dataTracker.set(IN_COMBAT, false);
        this.dataTracker.set(DYING_CINEMATIC, true);
        this.deathCinematicTicks = 0;
        this.anchorGroundY = findGroundY(world, this.getBlockPos());
        this.setNoGravity(false);
        this.setVelocity(0.0D, -1.5D, 0.0D);
        this.velocityModified = true;
        this.triggerAnim("actionController", "death_cinematic");

        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 2.0F, 0.85F
        );
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.0F, 0.7F
        );
    }

    private void tickDeathCinematic(ServerWorld world) {
        this.deathCinematicTicks++;
        this.getNavigation().stop();
        this.bossBar.setPercent(0.0F);
        applyCinematicPlayerImmunityAndFreezeMobs(world);

        if (this.getY() > this.anchorGroundY + 0.1D && this.deathCinematicTicks < 14) {
            this.setVelocity(0.0D, -1.5D, 0.0D);
        } else {
            this.setPosition(this.getX(), this.anchorGroundY, this.getZ());
            this.setVelocity(0.0D, 0.0D, 0.0D);
        }

        // Escarcha y cristales de hielo acumulándose antes de estallar
        if (this.deathCinematicTicks % 2 == 0) {
            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    8, 0.85D, 0.9D, 0.85D, 0.06D
            );
            world.spawnParticles(
                    BLUE_ICE_PARTICLE,
                    this.getX(), this.getY() + 0.9D, this.getZ(),
                    5, 0.65D, 0.7D, 0.65D, 0.08D
            );
        }
        if (this.deathCinematicTicks % 15 == 0) {
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.5F, 0.7F + (this.deathCinematicTicks * 0.008F)
            );
        }

        if (this.deathCinematicTicks >= DEATH_CINEMATIC_DURATION_TICKS) {
            Box area = this.getBoundingBox().expand(80.0D);
            for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, area, PlayerEntity::isAlive)) {
                player.fallDistance = 0.0F;
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, true, false, false));
            }

            releaseCinematicFrozenMobs(world);

            // Eliminar zombies invocados cercanos al derrotar al boss
            List<ZombieEntity> summonedZombies = world.getEntitiesByClass(
                    ZombieEntity.class,
                    this.getBoundingBox().expand(48.0D),
                    ZombieEntity::isAlive
            );
            for (ZombieEntity z : summonedZombies) {
                world.spawnParticles(ParticleTypes.SNOWFLAKE, z.getX(), z.getY() + 1.0D, z.getZ(), 15, 0.3D, 0.5D, 0.3D, 0.05D);
                z.discard();
            }

            // Gran Explosión de Hielo final
            world.spawnParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 1.0D, this.getZ(),
                    2, 0.3D, 0.3D, 0.3D, 0.0D
            );
            world.spawnParticles(
                    BLUE_ICE_PARTICLE,
                    this.getX(), this.getY() + 1.1D, this.getZ(),
                    95, 1.6D, 1.4D, 1.6D, 0.25D
            );
            world.spawnParticles(
                    PACKED_ICE_PARTICLE,
                    this.getX(), this.getY() + 1.1D, this.getZ(),
                    75, 1.5D, 1.2D, 1.5D, 0.2D
            );
            world.spawnParticles(
                    ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 1.2D, this.getZ(),
                    110, 2.0D, 1.5D, 2.0D, 0.15D
            );

            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2.5F, 0.85F
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.5F, 0.65F
            );
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.5F, 1.0F
            );

            // Mostrar título de victoria a los jugadores cercanos
            List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(p -> p.squaredDistanceTo(this) <= 6400.0D);
            for (ServerPlayerEntity player : nearbyPlayers) {
                player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
                player.networkHandler.sendPacket(new TitleS2CPacket(
                        Text.literal("¡JHOSEPH TOWERS DERROTADO!").formatted(Formatting.AQUA, Formatting.BOLD)
                ));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(
                        Text.literal("¡El Sol sí era una Bola de Hielo!").formatted(Formatting.WHITE, Formatting.ITALIC)
                ));
            }

            // Recompensas del Jefe Final
            this.dropStack(world, new ItemStack(ModItem.ICE_BALL, 16 + this.random.nextInt(9)));
            this.dropStack(world, new ItemStack(ModItem.LONTOTEM, 1));
            this.dropStack(world, new ItemStack(ModItem.SCARLET_INGOT, 6 + this.random.nextInt(5)));

            this.bossBar.clearPlayers();
            this.discard();
        }
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
        nbt.putBoolean("Phase2", this.isPhase2());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.spawnInitialized = true;
        if (nbt.contains("Phase2")) {
            boolean phase2 = nbt.getBoolean("Phase2");
            this.dataTracker.set(PHASE_2, phase2);
            if (phase2) {
                this.bossBar.setColor(BossBar.Color.WHITE);
                this.bossBar.setName(Text.translatable("entity.prueba_no1.jhoseph_towers").formatted(Formatting.WHITE, Formatting.BOLD));
            }
        }
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
            // José siempre está tocando su acordeón, tanto en Fase 1 como cuando se vuelve blanco en Fase 2
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ACCORDION_ANIM);
            }
            return state.setAndContinue(IDLE_ACCORDION_ANIM);
        }));

        controllers.add(
                new AnimationController<>(this, "actionController", 2, state -> software.bernie.geckolib.animation.PlayState.STOP)
                        .triggerableAnim("spawn_cinematic", SPAWN_CINEMATIC_ANIM)
                        .triggerableAnim("phase2_transform", PHASE2_TRANSFORM_ANIM)
                        .triggerableAnim("phase2_push", PHASE2_PUSH_ANIM)
                        .triggerableAnim("special_zombies", SPECIAL_ZOMBIES_ANIM)
                        .triggerableAnim("special_dialecto", SPECIAL_DIALECTO_ANIM)
                        .triggerableAnim("special_rafaga_hielo", SPECIAL_RAFAGA_HIELO_ANIM)
                        .triggerableAnim("special_bola_hielo", SPECIAL_BOLA_HIELO_ANIM)
                        .triggerableAnim("death_cinematic", DEATH_CINEMATIC_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
