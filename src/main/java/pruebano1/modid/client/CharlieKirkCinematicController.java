package pruebano1.modid.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.entity.CharlieKirkEntity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Controla la coreografía cinematográfica suave de cámara y la pantalla en negro
 * durante la entrada de 48 segundos (960 ticks) de Charlie Kirk en La Casa Blanca.
 */
public final class CharlieKirkCinematicController {

    public record CameraPose(double x, double y, double z, float yaw, float pitch) {
    }

    private static final double CAMERA_CLEARANCE_RADIUS = 0.42D;
    private static final Vec3d[] CLEARANCE_PROBES = new Vec3d[]{
            new Vec3d(1.0D, 0.0D, 0.0D),
            new Vec3d(-1.0D, 0.0D, 0.0D),
            new Vec3d(0.0D, 1.0D, 0.0D),
            new Vec3d(0.0D, -1.0D, 0.0D),
            new Vec3d(0.0D, 0.0D, 1.0D),
            new Vec3d(0.0D, 0.0D, -1.0D),
            new Vec3d(0.7071D, 0.0D, 0.7071D),
            new Vec3d(-0.7071D, 0.0D, 0.7071D),
            new Vec3d(0.7071D, 0.0D, -0.7071D),
            new Vec3d(-0.7071D, 0.0D, -0.7071D)
    };
    private static final double[] PORTRAIT_ANGLE_PREFERENCES = new double[]{
            0.0D, 0.52D, -0.52D, 1.05D, -1.05D, 1.57D, -1.57D, 2.09D, -2.09D, 2.62D, -2.62D, Math.PI
    };

    private static boolean inSpawnCinematic = false;
    private static int activeEntityId = -1;
    private static int activeTick = 0;
    private static int localElapsedTicks = 0;
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
        int serverTick = charlie.getSpawnCinematicTicks();
        if (!inSpawnCinematic || activeEntityId != charlie.getId()) {
            activeEntityId = charlie.getId();
            localElapsedTicks = Math.max(0, serverTick - 1);
        } else {
            localElapsedTicks++;
            int expectedIndex = Math.max(0, serverTick - 1);
            int drift = expectedIndex - localElapsedTicks;
            if (Math.abs(drift) > 6) {
                localElapsedTicks = expectedIndex;
            } else if (drift > 2) {
                localElapsedTicks++;
            } else if (drift < -2 && localElapsedTicks > 0) {
                localElapsedTicks--;
            }
        }

        inSpawnCinematic = true;
        activeTick = serverTick;
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
        activeEntityId = -1;
        activeTick = 0;
        localElapsedTicks = 0;
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
                && activeTick < CharlieKirkEntity.BLACKOUT_START_TICK
                && (localElapsedTicks + 1) < CharlieKirkEntity.BLACKOUT_START_TICK;
    }

    /**
     * Calcula la opacidad de la pantalla en negro entre el segundo 43 (tick 860) y el segundo 48 (tick 960).
     */
    public static float getBlackoutAlpha(float tickDelta) {
        if (!inSpawnCinematic || activeTick <= 0) {
            return 0.0F;
        }
        float baseTick = Math.max(localElapsedTicks + 1, activeTick >= CharlieKirkEntity.BLACKOUT_START_TICK ? CharlieKirkEntity.BLACKOUT_START_TICK : 0);
        float t = baseTick + MathHelper.clamp(tickDelta, 0.0F, 1.0F);
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
        double sec = MathHelper.clamp((localElapsedTicks + clampedDelta) / 20.0D, 0.0D, 43.0D);

        double cx = centerPos.getX() + 0.5D;
        double by = centerPos.getY();
        double cz = centerPos.getZ() + 0.5D;

        if (sec < 5.5D) {
            // 0.0s - 5.5s: Entrada Sur y Buzón de la Casa Blanca avanzando por el centro del portal sin tocar pilares
            double u = smoothstep(sec / 5.5D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx + 10.0D, by + 3.2D, cz + 165.0D),
                    new Vec3d(cx + 5.5D, by + 3.8D, cz + 150.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx + 14.0D, by + 2.1D, cz + 156.5D),
                    new Vec3d(cx, by + 8.5D, cz + 48.0D),
                    u
            );
            cam = ensureCameraClearSpace(cam, look, null);
            return buildPose(cam, look);
        }

        if (sec < 9.5D) {
            // 5.5s - 9.5s: Retrato cinematográfico lento sobre jugador #1 evitando obstáculos
            double u = (sec - 5.5D) / 4.0D;
            CameraPose playerShot = computeSafePlayerPortraitShot(0, u, clampedDelta, 3.3D, 2.35D, 1.45D, 1.7D, -0.18D, 0.14D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 15.0D) {
            // 9.5s - 15.0s: Plano grúa sobre la Gran Fuente Sur y el frente de los jardines
            double u = smoothstep((sec - 9.5D) / 5.5D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx - 20.0D, by + 6.0D, cz + 110.0D),
                    new Vec3d(cx - 12.0D, by + 8.6D, cz + 86.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx, by + 4.5D, cz + 98.0D),
                    new Vec3d(cx, by + 11.5D, cz + 38.0D),
                    u
            );
            cam = ensureCameraClearSpace(cam, look, null);
            return buildPose(cam, look);
        }

        if (sec < 19.5D) {
            // 15.0s - 19.5s: Marine One, Limusinas Presidenciales y Rose Garden en aire totalmente libre
            double u = smoothstep((sec - 15.0D) / 4.5D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx - 76.0D, by + 11.0D, cz + 96.0D),
                    new Vec3d(cx - 82.0D, by + 10.0D, cz + 76.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx - 105.0D, by + 4.5D, cz + 104.0D),
                    new Vec3d(cx - 100.0D, by + 3.5D, cz + 54.0D),
                    u
            );
            cam = ensureCameraClearSpace(cam, look, null);
            return buildPose(cam, look);
        }

        if (sec < 23.5D) {
            // 19.5s - 23.5s: Retrato cinematográfico sobre jugador #2 (u otro ángulo en solitario)
            double u = (sec - 19.5D) / 4.0D;
            CameraPose playerShot = computeSafePlayerPortraitShot(1, u, clampedDelta, 3.2D, 2.25D, 1.8D, 1.45D, 0.20D, -0.14D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 28.5D) {
            // 23.5s - 28.5s: Estanque Jacqueline Kennedy, Glorieta Clásica, Ala Este y Bandera 3D
            double u = smoothstep((sec - 23.5D) / 5.0D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx + 68.0D, by + 10.5D, cz + 94.0D),
                    new Vec3d(cx + 74.0D, by + 13.0D, cz + 72.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx + 98.0D, by + 4.5D, cz + 76.0D),
                    new Vec3d(cx + 22.0D, by + 18.0D, cz + 10.0D),
                    u
            );
            cam = ensureCameraClearSpace(cam, look, null);
            return buildPose(cam, look);
        }

        if (sec < 32.5D) {
            // 28.5s - 32.5s: Retrato cinematográfico en ángulo heroico sobre jugador #3
            double u = (sec - 28.5D) / 4.0D;
            CameraPose playerShot = computeSafePlayerPortraitShot(2, u, clampedDelta, 3.25D, 2.3D, 1.2D, 1.65D, -0.15D, 0.16D);
            if (playerShot != null) {
                return playerShot;
            }
        }

        if (sec < 38.0D) {
            // 32.5s - 38.0s: Aproximación majestuosa por el eje central hacia el Pórtico Sur y Bandera 3D
            double u = smoothstep((sec - 32.5D) / 5.5D);
            Vec3d cam = lerpVec(
                    new Vec3d(cx, by + 11.5D, cz + 78.0D),
                    new Vec3d(cx, by + 6.2D, cz + 55.0D),
                    u
            );
            Vec3d look = lerpVec(
                    new Vec3d(cx + 2.0D, by + 26.0D, cz + 4.0D),
                    new Vec3d(cx, by + 5.5D, cz + 34.0D),
                    u
            );
            cam = ensureCameraClearSpace(cam, look, null);
            return buildPose(cam, look);
        }

        // 38.0s - 43.0s: Interior de la Casa Blanca -> pasillo central del Salón de Conferencias hacia el podio de Charlie
        double u = smoothstep((sec - 38.0D) / 5.0D);
        Vec3d cam = lerpVec(
                new Vec3d(cx, by + 3.8D, cz + 17.0D),
                new Vec3d(cx, by + 3.3D, cz - 5.0D),
                u
        );
        Vec3d look = new Vec3d(cx, by + 3.6D, cz - 14.0D);
        cam = ensureCameraClearSpace(cam, look, null);
        return buildPose(cam, look);
    }

    @Nullable
    private static CameraPose computeSafePlayerPortraitShot(
            int indexOffset,
            double rawProgress,
            double tickDelta,
            double startDist,
            double endDist,
            double startHeight,
            double endHeight,
            double startOrbitDeltaRad,
            double endOrbitDeltaRad
    ) {
        if (cachedPlayers.isEmpty()) {
            return null;
        }
        PlayerEntity target = cachedPlayers.get(Math.floorMod(indexOffset, cachedPlayers.size()));
        double px = MathHelper.lerp(tickDelta, target.prevX, target.getX());
        double py = MathHelper.lerp(tickDelta, target.prevY, target.getY());
        double pz = MathHelper.lerp(tickDelta, target.prevZ, target.getZ());

        double yawDeg = MathHelper.lerpAngleDegrees((float) tickDelta, target.prevYaw, target.getYaw());
        double yawRad = Math.toRadians(yawDeg);
        double frontBaseAngle = Math.atan2(Math.cos(yawRad), -Math.sin(yawRad));
        double angleSeed = (indexOffset == 0) ? 0.35D : (indexOffset == 1 ? -0.45D : 0.72D);
        double preferredCenter = frontBaseAngle + angleSeed;

        Vec3d lookTarget = new Vec3d(px, py + 1.42D, pz);
        double maxRequestedDist = Math.max(startDist, endDist);
        double midHeight = 0.5D * (startHeight + endHeight);
        double midOrbit = 0.5D * (startOrbitDeltaRad + endOrbitDeltaRad);

        double chosenAngle = preferredCenter;
        double bestClearDist = maxRequestedDist + 0.65D;

        if (activeClient != null && activeClient.world != null) {
            bestClearDist = -1.0D;
            double fullClearRequirement = maxRequestedDist + 0.55D;

            for (double offset : PORTRAIT_ANGLE_PREFERENCES) {
                double candCenter = preferredCenter + offset;
                double c1 = measureRayClearance(target, lookTarget, candCenter + startOrbitDeltaRad, startHeight, fullClearRequirement);
                double c2 = measureRayClearance(target, lookTarget, candCenter + midOrbit, midHeight, fullClearRequirement);
                double c3 = measureRayClearance(target, lookTarget, candCenter + endOrbitDeltaRad, endHeight, fullClearRequirement);
                double minClear = Math.min(c1, Math.min(c2, c3));

                if (minClear >= fullClearRequirement) {
                    chosenAngle = candCenter;
                    bestClearDist = minClear;
                    break;
                }
                if (minClear > bestClearDist) {
                    bestClearDist = minClear;
                    chosenAngle = candCenter;
                }
            }
        }

        double maxSafeDist = Math.max(0.85D, bestClearDist - 0.50D);
        double effStartDist = Math.min(startDist, maxSafeDist);
        double effEndDist = Math.min(endDist, maxSafeDist);

        double eased = smoothstep(rawProgress);
        double angle = MathHelper.lerp(eased, chosenAngle + startOrbitDeltaRad, chosenAngle + endOrbitDeltaRad);
        double dist = MathHelper.lerp(eased, effStartDist, effEndDist);
        double height = MathHelper.lerp(eased, startHeight, endHeight);

        Vec3d desiredCam = new Vec3d(
                px + Math.cos(angle) * dist,
                py + height,
                pz + Math.sin(angle) * dist
        );
        desiredCam = ensureCameraClearSpace(desiredCam, lookTarget, target);
        return buildPose(desiredCam, lookTarget);
    }

    private static double measureRayClearance(
            PlayerEntity target,
            Vec3d origin,
            double angleRad,
            double heightOffset,
            double probeRadius
    ) {
        if (activeClient == null || activeClient.world == null) {
            return probeRadius;
        }
        double dirX = Math.cos(angleRad) * probeRadius;
        double dirY = (target.getY() + heightOffset) - origin.y;
        double dirZ = Math.sin(angleRad) * probeRadius;
        Vec3d end = origin.add(dirX, dirY, dirZ);

        BlockHitResult hit = activeClient.world.raycast(new RaycastContext(
                origin,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                target
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getPos().distanceTo(origin);
        }
        return probeRadius;
    }

    private static Vec3d ensureCameraClearSpace(Vec3d camPos, Vec3d lookTarget, @Nullable PlayerEntity ignoreEntity) {
        if (activeClient == null || activeClient.world == null) {
            return camPos;
        }
        PlayerEntity rayEntity = ignoreEntity != null ? ignoreEntity : activeClient.player;
        if (rayEntity == null) {
            return camPos;
        }

        Vec3d adjusted = camPos;
        BlockPos blockPos = BlockPos.ofFloored(adjusted);
        BlockState curState = activeClient.world.getBlockState(blockPos);
        VoxelShape shape = curState.getCollisionShape(activeClient.world, blockPos);
        if (!shape.isEmpty()) {
            double relX = adjusted.x - blockPos.getX();
            double relY = adjusted.y - blockPos.getY();
            double relZ = adjusted.z - blockPos.getZ();
            var bb = shape.getBoundingBox();
            if (relX >= bb.minX - 0.15D && relX <= bb.maxX + 0.15D
                    && relY >= bb.minY - 0.15D && relY <= bb.maxY + 0.15D
                    && relZ >= bb.minZ - 0.15D && relZ <= bb.maxZ + 0.15D) {
                Vec3d escapeDir = lookTarget.subtract(adjusted);
                if (escapeDir.lengthSquared() > 1.0E-4D) {
                    Vec3d step = escapeDir.normalize().multiply(0.35D);
                    for (int i = 0; i < 6; i++) {
                        adjusted = adjusted.add(step);
                        BlockPos nextBp = BlockPos.ofFloored(adjusted);
                        if (activeClient.world.getBlockState(nextBp).getCollisionShape(activeClient.world, nextBp).isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        Vec3d totalNudge = Vec3d.ZERO;
        for (Vec3d dir : CLEARANCE_PROBES) {
            Vec3d probeEnd = adjusted.add(dir.multiply(CAMERA_CLEARANCE_RADIUS));
            BlockHitResult hit = activeClient.world.raycast(new RaycastContext(
                    adjusted,
                    probeEnd,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    rayEntity
            ));
            if (hit.getType() == HitResult.Type.BLOCK) {
                double hitDist = hit.getPos().distanceTo(adjusted);
                double overlap = CAMERA_CLEARANCE_RADIUS - hitDist;
                if (overlap > 0.0D) {
                    totalNudge = totalNudge.subtract(dir.multiply(overlap));
                }
            }
        }

        if (totalNudge.lengthSquared() > 1.0E-6D) {
            Vec3d nudged = adjusted.add(totalNudge);
            BlockPos nudgedPos = BlockPos.ofFloored(nudged);
            if (activeClient.world.getBlockState(nudgedPos).getCollisionShape(activeClient.world, nudgedPos).isEmpty()) {
                return nudged;
            }
        }
        return adjusted;
    }

    private static CameraPose buildPose(Vec3d camPos, Vec3d lookTarget) {
        Vec3d diff = lookTarget.subtract(camPos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(diff.z, diff.x) * (180.0D / Math.PI)) - 90.0F);
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
