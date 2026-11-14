package pruebano1.modid.compat;

import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.MutableWaystone;
import net.blay09.mods.waystones.api.Waystone;
import net.blay09.mods.waystones.api.WaystoneOrigin;
import net.blay09.mods.waystones.api.WaystoneStyles;
import net.blay09.mods.waystones.api.WaystoneVisibility;
import net.blay09.mods.waystones.block.WaystoneBlockBase;
import net.blay09.mods.waystones.block.entity.WaystoneBlockEntityBase;
import net.blay09.mods.waystones.config.WaystonesConfig;
import net.blay09.mods.waystones.config.WaystonesConfigData;
import net.blay09.mods.waystones.core.WaystoneManagerImpl;
import net.blay09.mods.waystones.core.WaystoneSyncManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import pruebano1.modid.Prueba_no1;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Integración activa con Waystones + Balm (cargada únicamente cuando Waystones está instalado).
 */
final class WaystonesCompat {

    private static final EnumSet<WaystoneOrigin> RESTRICTED_ORIGINS = EnumSet.of(
            WaystoneOrigin.PLAYER,
            WaystoneOrigin.VILLAGE,
            WaystoneOrigin.WILDERNESS,
            WaystoneOrigin.DUNGEON,
            WaystoneOrigin.UNKNOWN
    );

    private WaystonesCompat() {
    }

    static void register() {
        applyLonTerryConfig();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> applyLonTerryConfig());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> applyLonTerryConfig());
    }

    static void applyLonTerryConfig() {
        try {
            if (Balm.getConfig().getSchema(WaystonesConfigData.class) == null) {
                return;
            }
            WaystonesConfigData current = WaystonesConfig.getActive();
            boolean needsUpdate = current == null
                    || current.worldGen.chunksBetweenWildWaystones != 0
                    || current.worldGen.spawnInVillages != WaystonesConfigData.VillageWaystoneGeneration.DISABLED
                    || current.teleports.enableCosts
                    || hasXpRequirements(current.teleports.warpRequirements)
                    || !current.general.restrictedWaystones.containsAll(RESTRICTED_ORIGINS);

            if (!needsUpdate) {
                return;
            }

            Balm.getConfig().updateLocalConfig(WaystonesConfigData.class, cfg -> {
                // Desactivar waystones salvajes y de aldeas: solo aparecerán en estructuras de zona segura
                cfg.worldGen.chunksBetweenWildWaystones = 0;
                cfg.worldGen.spawnInVillages = WaystonesConfigData.VillageWaystoneGeneration.DISABLED;

                // Sin costo de experiencia al teletransportarse entre waystones
                cfg.teleports.enableCosts = false;
                cfg.teleports.warpRequirements = stripXpRequirements(cfg.teleports.warpRequirements);

                // Evitar edición/renombrado de waystones generadas por jugadores en supervivencia
                Set<WaystoneOrigin> restricted = new LinkedHashSet<>(cfg.general.restrictedWaystones);
                restricted.addAll(RESTRICTED_ORIGINS);
                cfg.general.restrictedWaystones = restricted;
            });

            Prueba_no1.LOGGER.info("Ajustes de Waystones aplicados para Lon Terry: sin generacion natural/aldeas y sin costo de XP.");
        } catch (Throwable t) {
            Prueba_no1.LOGGER.warn("No se pudieron aplicar los ajustes automáticos de Waystones.", t);
        }
    }

    private static boolean hasXpRequirements(List<String> rules) {
        if (rules == null) {
            return false;
        }
        for (String rule : rules) {
            if (isXpRule(rule)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> stripXpRequirements(List<String> rules) {
        List<String> filtered = new ArrayList<>();
        if (rules != null) {
            for (String rule : rules) {
                if (!isXpRule(rule)) {
                    filtered.add(rule);
                }
            }
        }
        return filtered;
    }

    private static boolean isXpRule(String rule) {
        return rule != null && (rule.contains("xp_cost") || rule.contains("level_cost"));
    }

    static boolean ensureSafeZoneEntranceWaystone(
            ServerWorld world,
            BlockPos lowerPos,
            Direction facing,
            String displayName
    ) {
        if (world == null || lowerPos == null) {
            return false;
        }
        try {
            world.getChunk(lowerPos.getX() >> 4, lowerPos.getZ() >> 4);
            BlockPos upperPos = lowerPos.up();
            BlockState existingLower = world.getBlockState(lowerPos);

            if (!(existingLower.getBlock() instanceof WaystoneBlockBase)) {
                Block waystoneBlock = Registries.BLOCK.get(WaystoneStyles.BLACKSTONE.getBlockRegistryName());
                if (!(waystoneBlock instanceof WaystoneBlockBase)) {
                    waystoneBlock = Registries.BLOCK.get(WaystoneStyles.DEFAULT.getBlockRegistryName());
                }
                if (!(waystoneBlock instanceof WaystoneBlockBase)) {
                    return false;
                }

                BlockState baseState = waystoneBlock.getDefaultState()
                        .with(WaystoneBlockBase.FACING, facing)
                        .with(WaystoneBlockBase.ORIGIN, WaystoneOrigin.DUNGEON)
                        .with(WaystoneBlockBase.WATERLOGGED, false);

                world.setBlockState(
                        lowerPos,
                        baseState.with(WaystoneBlockBase.HALF, DoubleBlockHalf.LOWER),
                        Block.NOTIFY_ALL
                );
                world.setBlockState(
                        upperPos,
                        baseState.with(WaystoneBlockBase.HALF, DoubleBlockHalf.UPPER),
                        Block.NOTIFY_ALL
                );
            }

            BlockEntity lowerBe = world.getBlockEntity(lowerPos);
            if (!(lowerBe instanceof WaystoneBlockEntityBase tile)) {
                return false;
            }

            Waystone waystone = tile.getWaystone();
            if (waystone == null || !waystone.isValid()) {
                tile.initializeWaystone(world, null, WaystoneOrigin.DUNGEON);
                waystone = tile.getWaystone();
            }

            if (world.getBlockEntity(upperPos) instanceof WaystoneBlockEntityBase upperTile) {
                upperTile.initializeFromBase(tile);
            }

            if (waystone == null || !waystone.isValid()) {
                return false;
            }

            if (waystone instanceof MutableWaystone mutable) {
                mutable.setName(Text.literal(displayName));
                mutable.setVisibility(WaystoneVisibility.ACTIVATION);
            }

            WaystoneManagerImpl.get(world.getServer()).updateWaystone(waystone);
            WaystoneSyncManager.sendWaystoneUpdateToAll(world.getServer(), waystone);
            tile.markDirty();
            return true;
        } catch (Throwable t) {
            Prueba_no1.LOGGER.warn("No se pudo colocar el Waystone de zona segura '{}' en {}.", displayName, lowerPos, t);
            return false;
        }
    }

    static boolean isWaystoneBlock(Block block) {
        return block instanceof WaystoneBlockBase;
    }

    static boolean isUnbreakableWaystone(World world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WaystoneBlockBase)) {
            return false;
        }
        return state.get(WaystoneBlockBase.ORIGIN) != WaystoneOrigin.PLAYER;
    }
}
