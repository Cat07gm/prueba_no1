package pruebano1.modid.world;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BoatItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.BrushItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.FireChargeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MinecartItem;
import net.minecraft.item.PowderSnowBucketItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SignItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import pruebano1.modid.block.CasinoChairBlock;
import pruebano1.modid.block.DrafteaBlock;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centraliza la protección anti-destrucción/construcción, zona segura con invulnerabilidad,
 * subtítulo de entrada y comandos de guardado para el casino único "Draftea Tony Diaz".
 */
public final class DrafteaCasinoProtection {

    private static final Set<UUID> PLAYERS_IN_CASINO = ConcurrentHashMap.newKeySet();
    private static int zoneCheckTimer = 0;

    private DrafteaCasinoProtection() {
    }

    public static void registerServer() {
        ServerWorldEvents.LOAD.register((server, world) -> DrafteaCasinoManager.onOverworldLoaded(world));

        ServerLifecycleEvents.SERVER_STARTED.register(DrafteaCasinoManager::ensurePlacedOnServerStart);

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PLAYERS_IN_CASINO.clear();
            DrafteaCasinoManager.clearServerCaches(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerPlayNetworking.send(handler.getPlayer(), DrafteaCasinoManager.getBoundsPayloadForServer(server))
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PLAYERS_IN_CASINO.remove(handler.getPlayer().getUuid())
        );

        // Proteger bloques del casino frente a jugadores sin creativo
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient() && shouldCancelBlockAttack(world, player, pos)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                world.isClient() || !shouldCancelBlockAttack(world, player, pos)
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient() && shouldCancelBlockUse(world, player, hand, hitResult)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && shouldCancelItemUse(world, player, hand)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Proteger cuadros, marcos y soportes de armadura decorativos dentro del casino (sin impedir golpes PvP)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && shouldCancelEntityInteraction(world, player, entity)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && shouldCancelEntityInteraction(world, player, entity)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(DrafteaCasinoProtection::tickSafeZone);

        registerCommands();
    }

    public static boolean shouldCancelBlockAttack(World world, PlayerEntity player, BlockPos pos) {
        return !player.isCreative() && DrafteaCasinoManager.isProtectedBlock(world, pos);
    }

    public static boolean shouldCancelBlockUse(World world, PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (player.isCreative()) {
            return false;
        }
        BlockPos pos = hitResult.getBlockPos();
        BlockPos placePos = pos.offset(hitResult.getSide());
        if (!DrafteaCasinoManager.isProtectedBlock(world, pos)
                && !DrafteaCasinoManager.isProtectedBlock(world, placePos)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        ItemStack stack = player.getStackInHand(hand);

        if (isCasinoInteractiveBlock(block)
                && !player.shouldCancelInteraction()
                && !isBlockedPlacementOrGriefItem(stack)) {
            return false;
        }

        if (isBlockedPlacementOrGriefItem(stack)) {
            return true;
        }

        return isProtectedDecorBlock(block);
    }

    private static boolean shouldCancelItemUse(World world, PlayerEntity player, Hand hand) {
        if (player.isCreative()) {
            return false;
        }
        if (!DrafteaCasinoManager.isProtectedBlock(world, player.getBlockPos())) {
            return false;
        }
        Item item = player.getStackInHand(hand).getItem();
        return item instanceof BucketItem
                || item instanceof PowderSnowBucketItem
                || item instanceof BoatItem
                || item instanceof SpawnEggItem;
    }

    private static boolean shouldCancelEntityInteraction(World world, PlayerEntity player, Entity entity) {
        if (player.isCreative()) {
            return false;
        }
        boolean isDecorEntity = entity instanceof ArmorStandEntity
                || entity instanceof ItemFrameEntity
                || entity instanceof PaintingEntity;
        return isDecorEntity && DrafteaCasinoManager.isProtectedBlock(world, entity.getBlockPos());
    }

    private static boolean isCasinoInteractiveBlock(Block block) {
        return block instanceof DrafteaBlock
                || block instanceof CasinoChairBlock
                || block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || block instanceof ButtonBlock;
    }

    private static boolean isBlockedPlacementOrGriefItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BlockItem
                || item instanceof BucketItem
                || item instanceof PowderSnowBucketItem
                || item instanceof FlintAndSteelItem
                || item instanceof FireChargeItem
                || item instanceof BoneMealItem
                || item instanceof BoatItem
                || item instanceof MinecartItem
                || item instanceof DecorationItem
                || item instanceof ArmorStandItem
                || item instanceof EndCrystalItem
                || item instanceof SpawnEggItem
                || item instanceof HangingSignItem
                || item instanceof SignItem
                || item instanceof AxeItem
                || item instanceof ShovelItem
                || item instanceof HoeItem
                || item instanceof HoneycombItem
                || item instanceof BrushItem;
    }

    private static boolean isProtectedDecorBlock(Block block) {
        return block instanceof AbstractSignBlock
                || block instanceof FlowerPotBlock
                || block instanceof CandleBlock
                || block instanceof CandleCakeBlock
                || block instanceof BarrelBlock
                || block instanceof ChestBlock
                || block instanceof TrappedChestBlock
                || block instanceof TrapdoorBlock
                || block instanceof JukeboxBlock
                || block instanceof NoteBlock
                || block instanceof CampfireBlock
                || block instanceof ChiseledBookshelfBlock
                || block instanceof DecoratedPotBlock;
    }

    public static boolean isImmuneInSafeZone(ServerPlayerEntity player, ServerWorld serverWorld, DamageSource source) {
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return DrafteaCasinoManager.isInSafeZone(serverWorld, player.getBlockPos());
    }

    private static void tickSafeZone(MinecraftServer server) {
        zoneCheckTimer++;
        if (zoneCheckTimer < 5) {
            return;
        }
        zoneCheckTimer = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerWorld world = player.getServerWorld();
            boolean inside = DrafteaCasinoManager.isInSafeZone(world, player.getBlockPos());
            if (inside) {
                if (PLAYERS_IN_CASINO.add(player.getUuid())) {
                    player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 45, 15));
                    player.networkHandler.sendPacket(new TitleS2CPacket(
                            Text.literal("CASINO").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                    ));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(
                            Text.literal("Draftea Tony Diaz").formatted(Formatting.WHITE, Formatting.BOLD)
                    ));
                }
                if (player.isOnFire()) {
                    player.extinguish();
                }
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.RESISTANCE,
                        45,
                        4,
                        true,
                        false,
                        true
                ));
            } else {
                PLAYERS_IN_CASINO.remove(player.getUuid());
            }
        }
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("drafteacasino")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("guardar")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    return DrafteaCasinoCustomEdits.saveCasinoFromPlayer(
                                            player, DrafteaCasinoManager.CHUNK_RADIUS
                                    );
                                })
                                .then(CommandManager.argument("radioChunks", IntegerArgumentType.integer(1, DrafteaCasinoManager.CHUNK_RADIUS))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            int radius = IntegerArgumentType.getInteger(context, "radioChunks");
                                            return DrafteaCasinoCustomEdits.saveCasinoFromPlayer(player, radius);
                                        })
                                )
                        )
                        .then(CommandManager.literal("limpiar")
                                .executes(context -> {
                                    DrafteaCasinoCustomEdits.clearAllSavedEdits();
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("🧹 Ediciones guardadas del casino 'Draftea Tony Diaz' borradas.")
                                                    .formatted(Formatting.YELLOW),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("ir")
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    ServerWorld overworld = context.getSource().getServer().getOverworld();
                                    DrafteaCasinoManager.CasinoAnchor anchor = DrafteaCasinoManager.getAnchor(context.getSource().getServer());
                                    if (overworld == null || anchor == null) {
                                        context.getSource().sendError(Text.literal("El casino aún no está inicializado en este mundo."));
                                        return 0;
                                    }
                                    BlockPos tp = anchor.entranceTpPos();
                                    player.teleport(overworld, tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, Set.of(), 180.0f, 0.0f, true);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("info")
                                .executes(context -> {
                                    DrafteaCasinoManager.CasinoAnchor anchor = DrafteaCasinoManager.getAnchor(context.getSource().getServer());
                                    if (anchor == null) {
                                        context.getSource().sendError(Text.literal("El casino aún no está inicializado en este mundo."));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(
                                            () -> Text.literal(
                                                    "🎰 Casino único 'Draftea Tony Diaz' ubicado en ("
                                                            + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ()
                                                            + ") | radio: " + DrafteaCasinoManager.CHUNK_RADIUS + " chunks."
                                            ).formatted(Formatting.LIGHT_PURPLE),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("respawntony")
                                .executes(context -> {
                                    ServerWorld overworld = context.getSource().getServer().getOverworld();
                                    if (overworld == null) {
                                        context.getSource().sendError(Text.literal("Overworld no disponible."));
                                        return 0;
                                    }
                                    int count = DrafteaCasinoManager.respawnCasinoNpcs(overworld);
                                    if (count <= 0) {
                                        context.getSource().sendError(Text.literal("El casino aún no está inicializado en este mundo."));
                                        return 0;
                                    }
                                    context.getSource().sendFeedback(
                                            () -> Text.literal("🕴️ Reaparecidos " + count + " personajes Tony en el casino 'Draftea Tony Diaz'.")
                                                    .formatted(Formatting.GREEN),
                                            true
                                    );
                                    return count;
                                })
                        )
                )
        );
    }
}
