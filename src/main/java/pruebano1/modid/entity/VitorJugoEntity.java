package pruebano1.modid.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.FlyGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import pruebano1.modid.ModItem;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Secuaz de la Dungeon "Isla Epstein": Vitor Jugo
 * - Mitad del tamaño del jugador, 10 corazones de vida y 2 de armadura (igual que Yostin Biber).
 * - Flota acostado boca arriba con los brazos extendidos, "nadando" por el aire.
 * - NO ataca: persigue al jugador y, si está cerca, le pone UN efecto negativo aleatorio
 *   (Lentitud I, Debilidad I, Hambre I o Fatiga minera I).
 * - El efecto NO se quita hasta que Vitor Jugo muera o el jugador se aleje lo suficiente.
 * - Al morir: probabilidad alta de carne podrida y media de "Marijuana".
 */
public class VitorJugoEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation FLOAT_ANIM = RawAnimation.begin().thenLoop("float");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");

    // Efectos negativos posibles (amplificador 0 = nivel I)
    private static final List<RegistryEntry<StatusEffect>> NEGATIVE_EFFECTS = List.of(
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.HUNGER,
            StatusEffects.MINING_FATIGUE
    );

    private static final double APPLY_DISTANCE_SQ = 6.0D * 6.0D;    // Se aplica el efecto a 6 bloques o menos
    private static final double REMOVE_DISTANCE_SQ = 16.0D * 16.0D; // Se quita al alejarse más de 16 bloques
    private static final int EFFECT_DURATION = 100;                  // Se renueva constantemente mientras siga cerca

    // Jugadores afectados y el efecto que le tocó a cada uno
    private final Map<UUID, RegistryEntry<StatusEffect>> affectedPlayers = new HashMap<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public VitorJugoEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.moveControl = new FlightMoveControl(this, 20, true); // Vuelo / flotación
        this.setNoGravity(true);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createVitorJugoAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0D)      // 10 corazones
                .add(EntityAttributes.ARMOR, 2.0D)            // 2 puntos de armadura
                .add(EntityAttributes.MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.FLYING_SPEED, 0.4D)     // Velocidad al flotar
                .add(EntityAttributes.FOLLOW_RANGE, 35.0D);
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        BirdNavigation navigation = new BirdNavigation(this, world);
        navigation.setCanPathThroughDoors(false);
        navigation.setCanSwim(true);
        return navigation;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        // Deambula flotando solo cuando no tiene a nadie a quien perseguir
        this.goalSelector.add(5, new FlyGoal(this, 1.0D) {
            @Override
            public boolean canStart() {
                return VitorJugoEntity.this.getTarget() == null && super.canStart();
            }
        });

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld world) {
        super.mobTick(world);

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.getLookControl().lookAt(target, 30.0F, 30.0F);

            // Distancia horizontal (X, Z) hacia el jugador
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            // Cuando está lejos (a más de 5 bloques), sube alto en el cielo (+6 bloques sobre el jugador).
            // Conforme se acerca al jugador, baja suavemente hasta quedar 1 bloque por encima de su cabeza.
            double minHeightOffset = target.getHeight() + 1.0D; // 1 bloque por encima de la cabeza del jugador (~2.8 bloques desde los pies)
            double maxHeightOffset = target.getHeight() + 6.0D; // Alto en el cielo cuando está lejos (~7.8 bloques)
            double heightFactor = Math.min(1.0D, Math.max(0.0D, (horizontalDist - 2.0D) / 10.0D));
            double desiredY = target.getY() + minHeightOffset + (maxHeightOffset - minHeightOffset) * heightFactor;

            if (horizontalDist > 1.5D || Math.abs(this.getY() - desiredY) > 0.5D) {
                this.getMoveControl().moveTo(target.getX(), desiredY, target.getZ(), 1.1D);
            } else {
                this.setVelocity(this.getVelocity().multiply(0.6D));
            }
        } else {
            // Si no tiene objetivo y está muy cerca del suelo, sube flotando hacia arriba para mantenerse elevado
            if (!world.isAir(this.getBlockPos().down(1)) || !world.isAir(this.getBlockPos().down(2)) || !world.isAir(this.getBlockPos().down(3))) {
                this.setVelocity(this.getVelocity().x, 0.08D, this.getVelocity().z);
            }
        }

        // Revisar el "aura" de efectos negativos cada medio segundo
        if (this.age % 10 == 0) {
            this.updateCurseAura(world);
        }
    }

    private void updateCurseAura(ServerWorld world) {
        // 1. Renovar o quitar el efecto a los jugadores que ya estaban afectados
        Iterator<Map.Entry<UUID, RegistryEntry<StatusEffect>>> it = this.affectedPlayers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, RegistryEntry<StatusEffect>> entry = it.next();
            PlayerEntity player = world.getPlayerByUuid(entry.getKey());
            if (player == null || !isValidVictim(player) || this.squaredDistanceTo(player) > REMOVE_DISTANCE_SQ) {
                if (player != null) {
                    removeOurEffect(player, entry.getValue());
                }
                it.remove();
            } else {
                player.addStatusEffect(new StatusEffectInstance(entry.getValue(), EFFECT_DURATION, 0, false, true, true), this);
            }
        }

        // 2. Poner un efecto aleatorio a los jugadores nuevos que se acerquen
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (this.affectedPlayers.containsKey(player.getUuid())) continue;
            if (!isValidVictim(player) || this.squaredDistanceTo(player) > APPLY_DISTANCE_SQ) continue;

            RegistryEntry<StatusEffect> effect = NEGATIVE_EFFECTS.get(this.random.nextInt(NEGATIVE_EFFECTS.size()));
            this.affectedPlayers.put(player.getUuid(), effect);
            player.addStatusEffect(new StatusEffectInstance(effect, EFFECT_DURATION, 0, false, true, true), this);
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 0.8F, 1.4F);
        }
    }

    private static boolean isValidVictim(PlayerEntity player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    /** Quita el efecto solo si es el que puso Vitor Jugo (no borra pociones del jugador). */
    private static void removeOurEffect(PlayerEntity player, RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current != null && current.getAmplifier() == 0 && current.getDuration() <= EFFECT_DURATION) {
            player.removeStatusEffect(effect);
        }
    }

    /** Quita todos los efectos que haya puesto (al morir o desaparecer). */
    private void clearCurseAura() {
        if (this.getWorld() instanceof ServerWorld world) {
            for (Map.Entry<UUID, RegistryEntry<StatusEffect>> entry : this.affectedPlayers.entrySet()) {
                PlayerEntity player = world.getPlayerByUuid(entry.getKey());
                if (player != null) {
                    removeOurEffect(player, entry.getValue());
                }
            }
        }
        this.affectedPlayers.clear();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        this.clearCurseAura();
        super.onDeath(damageSource);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.clearCurseAura();
        super.remove(reason);
    }

    // Flota: nunca recibe daño por caída
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);

        // Probabilidad ALTA (80%) de carne podrida (1 o 2)
        if (this.random.nextFloat() < 0.80F) {
            this.dropStack(world, new ItemStack(Items.ROTTEN_FLESH, 1 + this.random.nextInt(2)));
        }
        // Probabilidad MEDIA (40%) de Marijuana
        if (this.random.nextFloat() < 0.40F) {
            this.dropStack(world, new ItemStack(ModItem.MARIJUANA, 1));
        }
    }

    // Sin voces, solo sonidos de golpe
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "controller", 6, state -> {
                    if (state.isMoving()) {
                        return state.setAndContinue(SWIM_ANIM);   // "Nada" por el aire al avanzar
                    }
                    return state.setAndContinue(FLOAT_ANIM);      // Flota suavemente al estar quieto
                })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
