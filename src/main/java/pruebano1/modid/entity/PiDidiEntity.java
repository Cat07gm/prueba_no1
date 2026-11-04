package pruebano1.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Mini-Jefe: Pi Didi
 * - 400 de vida (200 corazones), 8 de armadura y Barra de Jefe amarilla.
 * - Ataque normal ("attack"): alcance 20 bloques, cooldown 1.5s, lanza la botella de aceite haciendo 5 corazones (10.0F) + Lentitud por 3 segundos.
 * - Ataque especial ("special") cada 15 segundos (alcance 20 bloques): carga el "dinero_especial" con partículas verdes y lo dispara en el segundo 3.4,
 *   quitando 12 corazones (24.0F), empujando fuerte y causando una explosión de partículas verdes.
 * - Al morir: suelta la "Orden de arresto" (para invocar al jefe final) y ~9 niveles de experiencia.
 */
public class PiDidiEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation SPECIAL_ANIM = RawAnimation.begin().thenPlay("special");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Barra de jefe del color de jefes menores (Amarillo)
    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.prueba_no1.pi_didi"),
            BossBar.Color.YELLOW,
            BossBar.Style.NOTCHED_10
    );

    // Contadores para sincronizar ataques con los tiempos exactos de tus animaciones de Blockbench
    private int normalAttackCooldown = 30; // 1.50 segundos (30 ticks)
    private int normalAttackWindup = -1;

    private int specialCooldown = 300; // 15 segundos (300 ticks)
    private int specialChargeTicks = -1;

    public PiDidiEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent(); // No desaparece nunca al alejarse
        this.experiencePoints = 160; // 9 niveles de experiencia
    }

    public static DefaultAttributeContainer.Builder createPiDidiAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 400.0D) // 200 corazones (1 corazón = 2 puntos)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.ATTACK_DAMAGE, 10.0D) // 5 corazones base
                .add(EntityAttributes.ARMOR, 8.0D) // 8 puntos de armadura
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        // Actualizar barra de vida de jefe arriba de la pantalla
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.normalAttackWindup = -1;
            this.specialChargeTicks = -1;
            return;
        }

        this.getLookControl().lookAt(target, 30.0F, 30.0F);
        double distanceSq = this.squaredDistanceTo(target);

        // PROCESANDO ATAQUE ESPECIAL ...........................................................................

        if (this.specialChargeTicks >= 0) {
            this.specialChargeTicks++;
            this.getNavigation().stop(); // Se queda quieto levantando los brazos mientras invoca el dinero

            // ticks 1 a 60: partículas verdes y sonido en el dinero_especial
            if (this.specialChargeTicks <= 60) {
                world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 2.5D, this.getZ(),
                        4, 0.4D, 0.4D, 0.4D, 0.05D
                );
                if (this.specialChargeTicks % 10 == 0) {
                    world.playSound(
                            null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.HOSTILE, 1.5F, 0.8F + (this.specialChargeTicks * 0.01F)
                    );
                }
            }

            // tick 68 (segundo 3.4): el cubo "dinero_especial" sale disparado hacia el objetivo jugador
            if (this.specialChargeTicks == 68) {
                launchSpecialMoneyProjectile(world, target);
            }

            // Fin de la animación "special" (3.5s = 70 ticks)
            if (this.specialChargeTicks >= 70) {
                this.specialChargeTicks = -1;
                this.specialCooldown = 300; // Reiniciar contador de 15 segundos
            }
            return;
        }

        // Perseguir al objetivo hasta ponerse a 6 bloques de distancia (6 * 6 = 36.0D)
        if (distanceSq > 36.0D) {
            this.getNavigation().startMovingTo(target, 1.15D);
        } else {
            this.getNavigation().stop();
        }

        // Contadores de recarga en combate
        if (this.specialCooldown > 0) {
            this.specialCooldown--;
        }
        if (this.normalAttackCooldown > 0) {
            this.normalAttackCooldown--;
        }

        // Iniciar Ataque Especial cada 15 segundos (300 ticks) si el jugador está a 20 bloques o menos (20 * 20 = 400.0D)
        if (this.specialCooldown <= 0 && distanceSq <= 400.0D && this.normalAttackWindup < 0) {
            this.specialChargeTicks = 0;
            this.triggerAnim("controller", "special");
            world.playSound(
                    null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.6F, 1.2F
            );
            return;
        }

        // ATAQUE NORMAL .............................................................................

        if (this.normalAttackWindup >= 0) {
            this.normalAttackWindup++;

            // En el segundo 0.9 de la animación "attack" (tick 18), lanza la botella de aceite
            if (this.normalAttackWindup == 18) {
                launchOilBottleAttack(world, target);
            }

            // Fin de la animación "attack" (1.5s = 30 ticks)
            if (this.normalAttackWindup >= 30) {
                this.normalAttackWindup = -1;
                this.normalAttackCooldown = 30; // Cooldown de 1.50 segundos (30 ticks)
            }
        } else if (this.normalAttackCooldown <= 0 && distanceSq <= 400.0D && this.canSee(target)) {
            // Alcance de 20 bloques o menos (20 * 20 = 400.0D)
            this.normalAttackWindup = 0;
            this.triggerAnim("controller", "attack");
        }
    }

    /**
     * Ataque Normal: Lanza la botella de aceite hacia el jugador,
     * haciendo 5 corazones de daño (10.0F) y aplicando Lentitud por 3 segundos.
     */
    private void launchOilBottleAttack(ServerWorld world, LivingEntity target) {
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.HOSTILE, 1.2F, 0.9F
        );

        // Dibujar trayectoria de aceite entre Pi Didi y el jugador
        Vec3d start = this.getEyePos();
        Vec3d end = target.getBodyY(0.5D) > 0 ? new Vec3d(target.getX(), target.getBodyY(0.5D), target.getZ()) : target.getPos();
        Vec3d diff = end.subtract(start);
        int steps = 10;
        for (int i = 1; i <= steps; i++) {
            Vec3d p = start.add(diff.multiply((double) i / steps));
            world.spawnParticles(ParticleTypes.DRIPPING_HONEY, p.x, p.y, p.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
        }

        // Impacto de la botella de aceite: 5 corazones (10.0F) + Lentitud por 60 ticks
        target.damage(world, this.getDamageSources().mobAttack(this), 10.0F);
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 3 * 20, 1), this);

        world.playSound(
                null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_SPLASH_POTION_BREAK, SoundCategory.HOSTILE, 1.2F, 1.0F
        );
        world.spawnParticles(
                ParticleTypes.SPLASH,
                target.getX(), target.getY() + 1.0D, target.getZ(),
                20, 0.3D, 0.4D, 0.3D, 0.1D
        );
    }

    /**
     * Ataque Especial: Dispara el proyectil "dinero_especial" con estela de partículas verdes,
     * quita 12 corazones (24.0F), empuja fuerte y provoca una explosión con partículas verdes y sonidos especiales.
     */
    private void launchSpecialMoneyProjectile(ServerWorld world, LivingEntity target) {
        // Sonido especial de disparo del dinero gigante
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 1.5F, 1.15F
        );
        world.playSound(
                null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.HOSTILE, 1.5F, 0.6F
        );

        // Estela del proyectil con partículas verdes desde arriba de la cabeza de Pi Didi hasta el objetivo
        Vec3d start = new Vec3d(this.getX(), this.getY() + 2.4D, this.getZ());
        Vec3d end = new Vec3d(target.getX(), target.getBodyY(0.5D), target.getZ());
        Vec3d diff = end.subtract(start);
        int steps = 16;
        for (int i = 1; i <= steps; i++) {
            Vec3d p = start.add(diff.multiply((double) i / steps));
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 5, 0.15D, 0.15D, 0.15D, 0.02D);
        }

        // 1. Quitar 12 corazones de daño (24.0F)
        target.damage(world, this.getDamageSources().mobAttack(this), 24.0F);

        // 2. Empujar fuerte hacia atrás y arriba
        Vec3d knockbackDir = target.getPos().subtract(this.getPos()).normalize();
        target.addVelocity(knockbackDir.x * 1.8D, 0.65D, knockbackDir.z * 1.8D);
        target.velocityModified = true;

        // 3. Explosión visual y sonora con lluvia de partículas verdes en el punto de impacto (sin partículas de tótem)
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, end.x, end.y, end.z, 60, 1.2D, 1.2D, 1.2D, 0.15D);

        world.playSound(
                null, end.x, end.y, end.z,
                SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.5F, 0.85F
        );
        world.playSound(
                null, end.x, end.y, end.z,
                SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.HOSTILE, 1.6F, 0.7F
        );
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);
        // Soltar siempre el ítem "Orden de arresto" para invocar al jefe final de la Dungeon
        this.dropItem(world, ModItem.ARREST_WARRANT);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 4, state -> {
                    if (state.isMoving()) {
                        return state.setAndContinue(WALK_ANIM);
                    }
                    return state.setAndContinue(IDLE_ANIM);
                })
                        .triggerableAnim("attack", ATTACK_ANIM)
                        .triggerableAnim("special", SPECIAL_ANIM)
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
