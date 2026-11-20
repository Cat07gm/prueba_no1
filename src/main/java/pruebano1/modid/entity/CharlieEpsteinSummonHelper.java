package pruebano1.modid.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModEffects;
import pruebano1.modid.world.CasaBlancaStructure;

import java.util.List;
import java.util.UUID;

/**
 * Utilidad compartida para los niños de Isla Penstein invocados por el ataque telequinético de Charlie Kirk.
 * - Solo pueden atacar o dañar a jugadores con "Efecto Epstein" dentro de la dungeon.
 * - Si el efecto termina (30 s), se cura con leche, el jugador muere o Charlie Kirk es derrotado,
 *   los niños desaparecen automáticamente en humo.
 */
public final class CharlieEpsteinSummonHelper {

    public static final String EPSTEIN_SUMMON_TAG = "charlie_epstein_kid";
    public static final String VICTIM_TAG_PREFIX = "charlie_epstein_victim:";

    private CharlieEpsteinSummonHelper() {}

    public static void markAsEpsteinSummon(MobEntity kid, ServerPlayerEntity victim) {
        kid.addCommandTag(EPSTEIN_SUMMON_TAG);
        kid.addCommandTag(VICTIM_TAG_PREFIX + victim.getUuidAsString());
        kid.setPersistent();
        kid.setTarget(victim);
    }

    public static boolean isEpsteinSummon(@Nullable Entity entity) {
        return entity != null && entity.getCommandTags().contains(EPSTEIN_SUMMON_TAG);
    }

    public static boolean hasEpsteinEffect(@Nullable LivingEntity entity) {
        return entity instanceof PlayerEntity player
                && player.isAlive()
                && !player.isSpectator()
                && !player.isCreative()
                && player.hasStatusEffect(ModEffects.EPSTEIN_EFFECT);
    }

    public static boolean canEpsteinSummonTarget(MobEntity kid, @Nullable LivingEntity candidate) {
        if (!isEpsteinSummon(kid)) {
            return true;
        }
        if (candidate == null || !hasEpsteinEffect(candidate)) {
            return false;
        }
        if (kid.getWorld() instanceof ServerWorld serverWorld
                && candidate.getWorld() instanceof ServerWorld candidateWorld) {
            if (CasaBlancaStructure.isPosInCasaBlanca(serverWorld, kid.getBlockPos())
                    && !CasaBlancaStructure.isPosInCasaBlanca(candidateWorld, candidate.getBlockPos())) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static UUID extractVictimUuid(MobEntity kid) {
        for (String tag : kid.getCommandTags()) {
            if (tag.startsWith(VICTIM_TAG_PREFIX)) {
                try {
                    return UUID.fromString(tag.substring(VICTIM_TAG_PREFIX.length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Ejecuta la validación por tick de un niño invocado por Charlie Kirk.
     * @return {@code true} si el niño fue descartado y el mob no debe seguir ejecutando su IA en este tick.
     */
    public static boolean tickEpsteinSummon(MobEntity kid, ServerWorld world) {
        if (!isEpsteinSummon(kid)) {
            return false;
        }

        boolean charlieActive = !world.getEntitiesByClass(
                CharlieKirkEntity.class,
                kid.getBoundingBox().expand(140.0D),
                c -> c.isAlive() && !c.isDyingCinematic()
        ).isEmpty();
        if (!charlieActive) {
            despawnWithSmoke(world, kid);
            return true;
        }

        UUID victimId = extractVictimUuid(kid);
        ServerPlayerEntity victim = null;
        if (victimId != null) {
            PlayerEntity rawVictim = world.getPlayerByUuid(victimId);
            if (rawVictim instanceof ServerPlayerEntity sp
                    && hasEpsteinEffect(sp)
                    && sp.squaredDistanceTo(kid) <= 96.0D * 96.0D
                    && (!CasaBlancaStructure.isPosInCasaBlanca(world, kid.getBlockPos())
                            || CasaBlancaStructure.isPosInCasaBlanca(world, sp.getBlockPos()))) {
                victim = sp;
            }
        }

        // Si su víctima asignada perdió el efecto (expiró o tomó leche), salió o murió -> desaparecer
        if (victim == null) {
            despawnWithSmoke(world, kid);
            return true;
        }

        LivingEntity currentTarget = kid.getTarget();
        if (currentTarget != null && !canEpsteinSummonTarget(kid, currentTarget)) {
            kid.setTarget(null);
            currentTarget = null;
        }
        if (kid.getAttacker() != null && !canEpsteinSummonTarget(kid, kid.getAttacker())) {
            kid.setAttacker(null);
        }

        if (currentTarget == null || !currentTarget.isAlive()) {
            kid.setTarget(victim);
        }
        return false;
    }

    public static void discardAllEpsteinSummonsNear(ServerWorld world, Vec3d center, double radius) {
        Box box = new Box(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        List<MobEntity> summonedKids = world.getEntitiesByClass(
                MobEntity.class,
                box,
                mob -> mob.isAlive() && isEpsteinSummon(mob)
        );
        for (MobEntity kid : summonedKids) {
            despawnWithSmoke(world, kid);
        }
    }

    private static void despawnWithSmoke(ServerWorld world, MobEntity kid) {
        world.spawnParticles(
                ParticleTypes.POOF,
                kid.getX(), kid.getBodyY(0.5D), kid.getZ(),
                10, 0.25D, 0.30D, 0.25D, 0.04D
        );
        world.spawnParticles(
                ParticleTypes.WITCH,
                kid.getX(), kid.getBodyY(0.5D), kid.getZ(),
                6, 0.22D, 0.28D, 0.22D, 0.03D
        );
        kid.discard();
    }
}
