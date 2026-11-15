package pruebano1.modid;

import net.fabricmc.api.ModInitializer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.JoseTorresEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.network.PlayerDeathKoPayload;
import pruebano1.modid.world.IslaPensteinCustomEdits;
import pruebano1.modid.world.IslaPensteinStructure;
import pruebano1.modid.world.ModStructures;
import pruebano1.modid.world.PodcastStageManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Prueba_no1 implements ModInitializer {
	public static final String MOD_ID = "prueba_no1";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Set<UUID> PLAYERS_IN_ISLA_PENSTEIN = new HashSet<>();
	private static final Set<UUID> PLAYERS_IN_PODCAST_STAGE = new HashSet<>();
	private static int islandTitleCheckTimer = 0;

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		ModSounds.registerModSounds();
		ModParticles.registerModParticles();
		ModEntities.registerModEntities();
		ModItemGroups.registerItemGroups();
		ModItem.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();
		ModStructures.registerModStructures();
		ModWorldGeneration.generateModWorldGen();

		// Registrar paquetes de red del mod
		PayloadTypeRegistry.playS2C().register(PlayerDeathKoPayload.ID, PlayerDeathKoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.CasinoBoundsPayload.ID,
				pruebano1.modid.network.CasinoBoundsPayload.CODEC
		);

		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.LobbyBoundsPayload.ID,
				pruebano1.modid.network.LobbyBoundsPayload.CODEC
		);

		// Música del casino (DJ Controller): canciones, órdenes del DJ y estado para los clientes
		pruebano1.modid.casino.CasinoSongs.registerCasinoSongs();
		PayloadTypeRegistry.playC2S().register(
				pruebano1.modid.network.CasinoMusicControlPayload.ID,
				pruebano1.modid.network.CasinoMusicControlPayload.CODEC
		);
		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.CasinoMusicStatePayload.ID,
				pruebano1.modid.network.CasinoMusicStatePayload.CODEC
		);
		ServerPlayNetworking.registerGlobalReceiver(pruebano1.modid.network.CasinoMusicControlPayload.ID, (payload, context) ->
				context.server().execute(() -> pruebano1.modid.casino.CasinoMusicManager.handleControl(context.player(), payload))
		);
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				pruebano1.modid.casino.CasinoMusicManager.sendTo(handler.getPlayer())
		);

		// Cargar cambios guardados del casino único y registrar su protección, zona segura y comandos
		pruebano1.modid.world.LobbyTowerCustomEdits.ensureLoaded();
		pruebano1.modid.world.DrafteaCasinoCustomEdits.ensureLoaded();
		pruebano1.modid.world.DrafteaCasinoProtection.registerServer();
		pruebano1.modid.compat.WaystonesIntegration.registerIfLoaded();

		// Ciclo de vida del Escenario del Podcast (Altar de José Torres con el Cassete de Podcast)
		ServerWorldEvents.LOAD.register((server, world) -> PodcastStageManager.onOverworldLoaded(world));
		ServerLifecycleEvents.SERVER_STARTED.register(PodcastStageManager::ensurePlacedOnServerStart);
		ServerLifecycleEvents.SERVER_STOPPED.register(PodcastStageManager::clearServerCaches);

		// Inmunidad total para los jugadores en la zona segura del casino o en cinemáticas de Jefri Penstein / José Torres
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player && player.getWorld() instanceof ServerWorld serverWorld) {
				if (pruebano1.modid.world.DrafteaCasinoProtection.isImmuneInSafeZone(player, serverWorld, source)) {
					return false;
				}
				boolean jefriCinematic = !serverWorld.getEntitiesByClass(
						JefriPensteinEntity.class,
						new Box(player.getBlockPos()).expand(80.0D),
						JefriPensteinEntity::isInAnyCinematic
				).isEmpty();
				if (jefriCinematic) {
					return false;
				}
				boolean joseCinematic = !serverWorld.getEntitiesByClass(
						JoseTorresEntity.class,
						new Box(player.getBlockPos()).expand(80.0D),
						JoseTorresEntity::isInAnyCinematic
				).isEmpty();
				if (joseCinematic) {
					return false;
				}
			}
			return true;
		});

		// Cuando cualquier jugador muere, hacer caer un rayo y enviar el sonido y el efecto visual a todos los jugadores
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayerEntity deadPlayer) {
				// Hacer caer un rayo en la posición donde murió (cosmético para NO quemar los ítems que soltó)
				ServerWorld world = deadPlayer.getServerWorld();
				LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
				if (lightning != null) {
					lightning.refreshPositionAfterTeleport(deadPlayer.getX(), deadPlayer.getY(), deadPlayer.getZ());
					lightning.setCosmetic(true);
					world.spawnEntity(lightning);
				}

				MinecraftServer server = deadPlayer.getServer();
				if (server != null) {
					PlayerDeathKoPayload payload = new PlayerDeathKoPayload(deadPlayer.getName().getString());
					Text chatNotice = Text.literal(deadPlayer.getName().getString() + " FUE TERRY PAPEADO ALABERGA KEIJO ")
							.formatted(Formatting.DARK_RED, Formatting.BOLD);

					for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
						ServerPlayNetworking.send(player, payload);
						player.sendMessage(chatNotice, false);
					}
				}
			}
		});

		// Mostrar el título "Isla Penstein" en la pantalla cuando un jugador ingresa a la dungeon
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			islandTitleCheckTimer++;
			if (islandTitleCheckTimer < 15) {
				return;
			}
			islandTitleCheckTimer = 0;

			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				ServerWorld world = player.getServerWorld();
				boolean insideIsland = IslaPensteinStructure.isPosOnIsland(world, player.getBlockPos());
				UUID playerId = player.getUuid();

				if (insideIsland) {
					if (PLAYERS_IN_ISLA_PENSTEIN.add(playerId)) {
						player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 20));
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("Isla Penstein").formatted(Formatting.GOLD, Formatting.BOLD))
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.literal("La Isla de Albert Penstein").formatted(Formatting.AQUA, Formatting.ITALIC))
						);
					}
				} else {
					PLAYERS_IN_ISLA_PENSTEIN.remove(playerId);
				}

				boolean insidePodcastStage = PodcastStageManager.isPosInPodcastStage(world, player.getBlockPos());
				if (insidePodcastStage) {
					if (PLAYERS_IN_PODCAST_STAGE.add(playerId)) {
						player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 20));
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("Escenario del Podcast").formatted(Formatting.AQUA, Formatting.BOLD))
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.literal("Altar de José Torres — Usa el Acordeón en el Cassete").formatted(Formatting.GOLD, Formatting.ITALIC))
						);
					}
				} else {
					PLAYERS_IN_PODCAST_STAGE.remove(playerId);
				}
			}
		});

		// Comando "/islapenstein guardar" y "/escenariopodcast tp"
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					CommandManager.literal("escenariopodcast")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("tp")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										PodcastStageManager.ensurePlacedOnServerStart(ctx.getSource().getServer());
										PodcastStageManager.StageAnchor anchor = PodcastStageManager.getAnchor(ctx.getSource().getServer());
										if (anchor == null || overworld == null) {
											return 0;
										}
										BlockPos tp = anchor.entranceTpPos();
										player.teleport(overworld, tp.getX() + 0.5D, tp.getY(), tp.getZ() + 0.5D, Set.of(), 180.0F, 0.0F, true);
										player.sendMessage(
												Text.literal("🎙️ Teletransportado al Escenario del Podcast en (" + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ() + ").")
														.formatted(Formatting.AQUA, Formatting.BOLD),
												false
										);
										return 1;
									})
							)
			);
			dispatcher.register(
					CommandManager.literal("islapenstein")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("guardar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										int chunks = IslaPensteinCustomEdits.saveIslandFromPlayer(player, 8);
										if (chunks > 0) {
											player.sendMessage(
													Text.literal("✅ ¡Guardados " + chunks + " chunks de tus construcciones en la Isla Penstein! Ahora aparecerán así en cada nuevo spawneo de la dungeon.")
															.formatted(Formatting.GREEN, Formatting.BOLD),
													false
											);
										}
										return chunks;
									})
									.then(CommandManager.argument("radio_chunks", IntegerArgumentType.integer(1, 16))
											.executes(ctx -> {
												ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
												int r = IntegerArgumentType.getInteger(ctx, "radio_chunks");
												int chunks = IslaPensteinCustomEdits.saveIslandFromPlayer(player, r);
												if (chunks > 0) {
													player.sendMessage(
															Text.literal("✅ ¡Guardados " + chunks + " chunks (radio " + r + ") de la Isla Penstein!")
																	.formatted(Formatting.GREEN, Formatting.BOLD),
															false
													);
												}
												return chunks;
											})
									)
							)
							.then(CommandManager.literal("limpiar")
									.executes(ctx -> {
										IslaPensteinCustomEdits.clearAllSavedEdits();
										ctx.getSource().sendFeedback(
												() -> Text.literal("🗑️ Se borraron los cambios personalizados; la Isla Penstein volverá a su diseño base.")
														.formatted(Formatting.YELLOW),
												true
										);
										return 1;
									})
							)
			);
		});
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
