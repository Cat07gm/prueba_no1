package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.entity.CharlieKirkEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Controla la coreografía suave de cámara y la pantalla en negro de la cinemática de entrada
 * de 48 segundos (960 ticks) de Charlie Kirk en La Casa Blanca.
 */
public final class CharlieKirkCinematicController {

    public record CameraPose(double x, double y, double z, float yaw, float pitch) {
    }

    private static boolean inSpawnCinematic = false;
    private static int activeTick = 0;
    private static BlockPos centerPos = BlockPos.ORIGIN;
    private static List<AbstractClientPlayerEntity> cachedPlayers = Collections.emptyList();
    private static MinecraftClient activeClient = null;

    private static boolean chunkCullingOverridden = false;
    private static boolean savedChunkCulling = true;

    private CharlieKirkCinematicController() {
    }

    public static void update(MinecraftClient client, @Nullable CharlieKirkEntity charlie) {
        if (client == null || client.world == null || charlie == null || !charlie.isInSpawnCinematic()) {
            reset(client);
            return;
        }

        activeClient = client;
        inSpawnCinematic = true;
        activeTick = charlie.getSpawnCinematicTicks();
        centerPos = charlie.hasDungeonCenter() ? charlie.getDungeonCenterPos() : charlie.getBlockPos();

        double maxDistSq = 260.0D * 260.0D;
        Vec3d centerVec = new Vec3d(centerPos.getX() + 0.5D, centerPos.getY() + 1.0D, centerPos.getZ() + 0.5D);
        cachedPlayers = client.world.getPlayers().stream()
                .filter(p -> p.isAlive() && !p.isSpectator() && p.squaredDistanceTo(centerVec) <= maxDistSq)
                .sorted(Comparator.comparing(p -> p.getUuid().toString()))
                .toList();

        if (isCameraOverrideActive()) {
            if (!chunkCullingOverridden) {
                savedChunkCulling = client.chunkCullingEnabled;
                chunkCullingOverridden = true;
            }
            client.chunkCullingEnabled = false;
        } else {
            restoreChunkCulling(client);
        }
    }

    public static void reset(@Nullable MinecraftClient client) {
        inSpawnCinematic = false;
        activeTick = 0;
        centerPos = BlockPos.ORIGIN;
        cachedPlayers = Collections.emptyList();
        restoreChunkCulling(client != null ? client : activeClient);
        if (client != null) {
            activeClient = null;
        }
    }

    private static void restoreChunkCulling(@Nullable MinecraftClient client) {
        if (chunkCullingOverridden && client != null) {
            client.chunkCullingEnabled = savedChunkCulling;
        }
        chunkCullingOverridden = false;
    }

    public static boolean isCameraOverrideActive() {
        return inSpawnCinematic
                && activeTick > 0
                && activeTick < CharlieKirkEntity.BLACKOUT_START_TICK;
    }

    /**
     * Calcula la opacidad de la pantalla en negro entre el segundo 43 (tick 860) y el segundo 48 (tick 960).
     */
    public static float getBlackoutAlpha(float tickDelta) {
        if (!inSpawnCinematic || activeTick <= 0) {
            return 0.0F;
        }
        float t = activeTick + MathHelper.clamp(tickDelta, 0.0F, 1.0F);
        if (t < CharlieKirkEntity.BLACKOUT_START_TICK || t >= CharlieKirkEntity.SPAWN_CINEMATIC_DURATION_TICKS) {
            return 0.0F;
        }
        if (t < CharlieKirkEntity.BLACKOUT_START_TICK + 5.0F) {
            float u = (t - CharlieKirkEntity.BLACKOUT_START_TICK) / 5.0F;
            return smoothstep(u);
        }
        if (t <= CharlieKirkEntity.SPAWN_CINEMATIC_DURATION_TICKS - 5.0F) {
            return 1.0F;
        }
        float u = (t - (CharlieKirkEntity.SPAWN_CINEMATIC_DURATION_TICKS - 5.0F)) / 5.0F;
        return 1.0F - smoothstep(u);
    }

    @Nullable
    public static CameraPose computeCameraPose(float tickDelta) {
        if (!isCameraOverrideActive()) {
            return null;
        }

        double clampedDelta = MathHelper.clamp(tickDelta, 0.0D, 1.0D);
        double sec = MathHelper.clamp(((activeTick - 1) + clampedDelta) / 20.0D, 0.0D, 43.0D);

        double cx = centerPos.getX() + 0.5D;
        double by = centerPos.getY();
        double cz = centerPos.getZ() + 0.5D;

        if (sec < 6.0D) {
            // 0.0s - 6.0s: Enfoque en la entrada sur y el Buzón de la Casa Blanca elevándose hacia la avenida
            double u = smoothstep(sec / 6.0D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx + 22.0D, by + 5.5D, cz + 174.0D),
                    new Vec3d(cx + 4.0D, by + 9.0D, cz + 136.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx + 14.5D, by + 2.2D, cz + 156.5D),
                    new Vec3d(cx, by + 10.0D, cz + 40.0D),
                    u
            );
            return buildPose(cam, look);
        }

        if (sec < 10.5D) {
            // 6.0s - 10.5s: Toma cinematográfica alrededor del jugador #1 quieto
            double u = (sec - 6.0D) / 4.5D;
            CameraPose playerShot = computePlayerShot(0, u, clampedDelta, 4.2D, 1.8D, 2.4D, 0.35D, 1.45D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 16.5D) {
            // 10.5s - 16.5s: Paneo amplio sobre jardines sur, gran fuente, Rose Garden y Marine One / Limusinas
            double u = smoothstep((sec - 10.5D) / 6.0D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx - 28.0D, by + 14.0D, cz + 115.0D),
                    new Vec3d(cx - 88.0D, by + 15.0D, cz + 72.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx, by + 4.5D, cz + 98.0D),
                    new Vec3d(cx - 96.0D, by + 5.0D, cz + 68.0D),
                    u
            );
            return buildPose(cam, look);
        }

        if (sec < 21.0D) {
            // 16.5s - 21.0s: Toma cinematográfica alrededor del jugador #2 (o ángulo opuesto en solitario)
            double u = (sec - 16.5D) / 4.5D;
            CameraPose playerShot = computePlayerShot(1, u, clampedDelta, 4.0D, 2.3D, 1.85D, 2.35D, 3.35D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 27.5D) {
            // 21.0s - 27.5s: Vuelo por el jardín este, estanque con glorieta, Pórtico Norte y bandera 3D
            double u = smoothstep((sec - 21.0D) / 6.5D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx + 95.0D, by + 14.0D, cz + 92.0D),
                    new Vec3d(cx + 55.0D, by + 22.0D, cz - 55.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx + 98.0D, by + 5.0D, cz + 76.0D),
                    new Vec3d(cx, by + 22.0D, cz - 10.0D),
                    u
            );
            return buildPose(cam, look);
        }

        if (sec < 32.0D) {
            // 27.5s - 32.0s: Toma cinematográfica alrededor del jugador #3
            double u = (sec - 27.5D) / 4.5D;
            CameraPose playerShot = computePlayerShot(2, u, clampedDelta, 3.8D, 1.9D, 2.4D, 4.1D, 5.15D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 38.0D) {
            // 32.0s - 38.0s: Aproximación majestuosa por el frente hacia el Pórtico Sur y Balcón Truman
            double u = smoothstep((sec - 32.0D) / 6.0D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx, by + 18.0D, cz + 112.0D),
                    new Vec3d(cx, by + 8.5D, cz + 48.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx, by + 12.0D, cz + 25.0D),
                    new Vec3d(cx, by + 10.0D, cz + 10.0D),
                    u
            );
            return buildPose(cam, look);
        }

        // 38.0s - 43.0s: Interior de la Casa Blanca -> Salón Principal de Conferencias acercándose al podio de Charlie
        double u = smoothstep((sec - 38.0D) / 5.0D);
        Vec3d cam = lerpVec(
                new Vec3d(cx, by + 4.5D, cz + 18.0D),
                new Vec3d(cx, by + 3.6D, cz - 5.5D),
                u
        );
        Vec3d look = new Vec3d(cx, by + 3.8D, cz - 14.0D);
        return buildPose(cam, look);
    }

    @Nullable
    private static CameraPose computePlayerShot(
            int indexOffset,
            double rawProgress,
            double tickDelta,
            double radius,
            double startHeight,
            double endHeight,
            double startAngleRad,
            double endAngleRad
    ) {
        if (cachedPlayers.isEmpty()) {
            return null;
        }
        PlayerEntity target = cachedPlayers.get(Math.floorMod(indexOffset, cachedPlayers.size()));
        double px = MathHelper.lerp(tickDelta, target.prevX, target.getX());
        double py = MathHelper.lerp(tickDelta, target.prevY, target.getY());
        double pz = MathHelper.lerp(tickDelta, target.prevZ, target.getZ());

        double eased = smoothstep(rawProgress);
        double angle = MathHelper.lerp(eased, startAngleRad, endAngleRad);
        double height = MathHelper.lerp(eased, startHeight, endHeight);

        Vec3d lookTarget = new Vec3d(px, py + 1.35D, pz);
        Vec3d desiredCam = new Vec3d(
                px + Math.cos(angle) * radius,
                py + height,
                pz + Math.sin(angle) * radius
        );

        // Evitar que la cámara entre dentro de bloques sólidos si el jugador está junto a un muro o columna
        if (activeClient != null && activeClient.world != null) {
            BlockHitResult hit = activeClient.world.raycast(new RaycastContext(
                    lookTarget,
                    desiredCam,
                    RaycastContext.ShapeType.VISUAL,
                    RaycastContext.FluidHandling.NONE,
                    target
            ));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3d dir = desiredCam.subtract(lookTarget);
                double len = dir.length();
                double hitDist = hit.getPos().distanceTo(lookTarget);
                if (len > 1.0E-4D && hitDist > 0.65D) {
                    desiredCam = lookTarget.add(dir.multiply((hitDist - 0.35D) / len));
                }
            }
        }

        return buildPose(desiredCam, lookTarget);
    }

    private static CameraPose buildPose(Vec3d camPos, Vec3d lookTarget) {
        Vec3d diff = lookTarget.subtract(camPos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (MathHelper.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(diff.y, Math.max(1.0E-5D, horizDist)) * (180.0D / Math.PI)));
        pitch = MathHelper.clamp(pitch, -89.0F, 89.0F);
        return new CameraPose(camPos.x, camPos.y, camPos.z, yaw, pitch);
    }

    private static Vec3d lerpVec(Vec3d a, Vec3d b, double t) {
        return new Vec3d(
                MathHelper.lerp(t, a.x, b.x),
                MathHelper.lerp(t, a.y, b.y),
                MathHelper.lerp(t, a.z, b.z)
        );
    }

    private static double smoothstep(double t) {
        double c = MathHelper.clamp(t, 0.0D, 1.0D);
        return c * c * (3.0D - 2.0D * c);
    }

    private static float smoothstep(float t) {
        float c = MathHelper.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }
}
