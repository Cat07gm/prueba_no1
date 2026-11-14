package pruebano1.modid.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import pruebano1.modid.world.DrafteaCasinoManager;

/**
 * Puente seguro y opcional hacia el mod Waystones.
 * No importa ninguna clase de Waystones/Balm aquí para que Lon Terry funcione igual
 * aunque Waystones no esté instalado.
 */
public final class WaystonesIntegration {

    public static final String MOD_ID = "waystones";

    private WaystonesIntegration() {
    }

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static void registerIfLoaded() {
        if (isLoaded()) {
            WaystonesCompat.register();
        }
    }

    public static boolean ensureCasinoEntranceWaystone(ServerWorld world, DrafteaCasinoManager.CasinoAnchor anchor) {
        if (!isLoaded() || world == null || anchor == null) {
            return false;
        }
        // Centro de la alfombra de acceso, justo frente a la entrada principal del casino "Draftea Tony Diaz"
        BlockPos waystonePos = new BlockPos(anchor.centerX(), anchor.baseY() + 1, anchor.centerZ() + 32);
        return WaystonesCompat.ensureSafeZoneEntranceWaystone(
                world,
                waystonePos,
                Direction.SOUTH,
                "Draftea Tony Diaz"
        );
    }

    public static boolean ensureSafeZoneEntranceWaystone(
            ServerWorld world,
            BlockPos lowerPos,
            Direction facing,
            String displayName
    ) {
        if (!isLoaded()) {
            return false;
        }
        return WaystonesCompat.ensureSafeZoneEntranceWaystone(world, lowerPos, facing, displayName);
    }

    public static boolean isWaystoneBlock(Block block) {
        return isLoaded() && WaystonesCompat.isWaystoneBlock(block);
    }

    public static boolean isUnbreakableWaystone(World world, BlockPos pos) {
        return isLoaded() && WaystonesCompat.isUnbreakableWaystone(world, pos);
    }
}
