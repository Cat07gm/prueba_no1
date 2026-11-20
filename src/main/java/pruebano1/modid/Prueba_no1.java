package pruebano1.modid;

import net.fabricmc.api.ModInitializer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.projectile.ProjectileEntity;
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
import net.minecraft.world.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pruebano1.modid.entity.CharlieEpsteinSummonHelper;
import pruebano1.modid.entity.CharlieKirkEntity;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.JoseTorresEntity;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;
import pruebano1.modid.entity.WhiteHouseGuardEntity;
import pruebano1.modid.network.CharlieEpsteinPopupPayload;
import pruebano1.modid.network.PlayerDeathKoPayload;
import pruebano1.modid.world.CasaBlancaCustomEdits;
import pruebano1.modid.world.CasaBlancaPiece;
import pruebano1.modid.world.CasaBlancaStructure;
import pruebano1.modid.world.CasaBlancaWorldState;
import pruebano1.modid.world.CasaJoseTorresCustomEdits;
import pruebano1.modid.world.CasaJoseTorresPiece;
import pruebano1.modid.world.CasaJoseTorresStructure;
import pruebano1.modid.world.CasaJoseTorresWorldState;
import pruebano1.modid.world.DungeonProgressionState;
import pruebano1.modid.world.IslaPensteinCustomEdits;
import pruebano1.modid.world.IslaPensteinStructure;
import pruebano1.modid.world.ModStructures;
import pruebano1.modid.world.PalacioNacionalCustomEdits;
import pruebano1.modid.world.PalacioNacionalPiece;
import pruebano1.modid.world.PalacioNacionalStructure;
import pruebano1.modid.world.PalacioNacionalWorldState;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class Prueba_no1 implements ModInitializer {
	public static final String MOD_ID = "prueba_no1";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final Set<UUID> PLAYERS_IN_ISLA_PENSTEIN = new HashSet<>();
	private static final Set<UUID> PLAYERS_IN_CASA_JOSE_TORRES = new HashSet<>();
	private static final Set<UUID> PLAYERS_IN_CASA_BLANCA = new HashSet<>();
	private static final Set<UUID> PLAYERS_IN_PALACIO_NACIONAL = new HashSet<>();
	private static int islandTitleCheckTimer = 0;

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world");
		ModSounds.registerModSounds();
		ModParticles.registerModParticles();
		ModEffects.registerModEffects();
		ModEntities.registerModEntities();
		ModItemGroups.registerItemGroups();
		ModItem.registerModItems();
		ModBlocks.registerModBlocks();
		ModBlockEntities.registerModBlockEntities();
		ModStructures.registerModStructures();
		ModWorldGeneration.generateModWorldGen();

		// Registrar paquetes de red del mod
		PayloadTypeRegistry.playS2C().register(PlayerDeathKoPayload.ID, PlayerDeathKoPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CharlieEpsteinPopupPayload.ID, CharlieEpsteinPopupPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.CasinoBoundsPayload.ID,
				pruebano1.modid.network.CasinoBoundsPayload.CODEC
		);

		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.LobbyBoundsPayload.ID,
				pruebano1.modid.network.LobbyBoundsPayload.CODEC
		);
		PayloadTypeRegistry.playS2C().register(
				pruebano1.modid.network.DungeonProgressionPayload.ID,
				pruebano1.modid.network.DungeonProgressionPayload.CODEC
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
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			pruebano1.modid.casino.CasinoMusicManager.sendTo(handler.getPlayer());
			DungeonProgressionState.syncToPlayer(handler.getPlayer());
		});

		// Cargar cambios guardados del casino único, la Casa de José Torres, la Casa Blanca y registrar protección y comandos
		pruebano1.modid.world.LobbyTowerCustomEdits.ensureLoaded();
		pruebano1.modid.world.DrafteaCasinoCustomEdits.ensureLoaded();
		CasaJoseTorresCustomEdits.ensureLoaded();
		CasaBlancaCustomEdits.ensureLoaded();
		PalacioNacionalCustomEdits.ensureLoaded();
		pruebano1.modid.world.DrafteaCasinoProtection.registerServer();
		pruebano1.modid.compat.WaystonesIntegration.registerIfLoaded();

		// Ciclo de vida de las ubicaciones únicas de La Casa de José Torres, La Casa Blanca y El Palacio Nacional
		ServerWorldEvents.LOAD.register((server, world) -> {
			CasaJoseTorresStructure.onOverworldLoaded(world);
			CasaBlancaStructure.onOverworldLoaded(world);
			PalacioNacionalStructure.onOverworldLoaded(world);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			CasaJoseTorresStructure.clearCaches();
			CasaBlancaStructure.clearCaches();
			PalacioNacionalStructure.clearCaches();
		});

		// Inmunidad total para los jugadores en la zona segura del casino o en cinemáticas de Jefri Penstein / José Torres / Charlie Kirk
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Entity attacker = source.getAttacker();
			if (attacker == null && source.getSource() instanceof ProjectileEntity projectile) {
				attacker = projectile.getOwner();
			}
			if (CharlieEpsteinSummonHelper.isEpsteinSummon(attacker)
					&& !(entity instanceof LivingEntity livingTarget && CharlieEpsteinSummonHelper.hasEpsteinEffect(livingTarget))) {
				return false;
			}
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
				boolean charlieCinematic = !serverWorld.getEntitiesByClass(
						CharlieKirkEntity.class,
						new Box(player.getBlockPos()).expand(240.0D),
						CharlieKirkEntity::isInAnyCinematic
				).isEmpty();
				if (charlieCinematic) {
					return false;
				}
			}
			return true;
		});

		// Anular aparición de mobs hostiles ajenos dentro de La Casa Blanca y de cualquier mob normal de Minecraft en El Palacio Nacional
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Monster && !isAllowedInCasaBlanca(entity)) {
				if (CasaBlancaStructure.isPosInCasaBlanca(world, entity.getBlockPos())) {
					world.getServer().execute(() -> {
						if (!entity.isRemoved() && CasaBlancaStructure.isPosInCasaBlanca(world, entity.getBlockPos())) {
							entity.discard();
						}
					});
				}
			}
			if (entity instanceof MobEntity && !isAllowedInPalacioNacional(entity)) {
				if (PalacioNacionalStructure.isPosInPalacioNacional(world, entity.getBlockPos())) {
					world.getServer().execute(() -> {
						if (!entity.isRemoved() && PalacioNacionalStructure.isPosInPalacioNacional(world, entity.getBlockPos())) {
							entity.discard();
						}
					});
				}
			}
		});

		// Cuando cualquier jugador muere, hacer caer un rayo y enviar el sonido y el efecto visual a todos los jugadores
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity.getWorld() instanceof ServerWorld bossWorld) {
				if (entity instanceof JefriPensteinEntity) {
					DungeonProgressionState.recordBossDefeated(bossWorld, DungeonProgressionState.DungeonStage.ISLA_PENSTEIN);
				} else if (entity instanceof JoseTorresEntity) {
					DungeonProgressionState.recordBossDefeated(bossWorld, DungeonProgressionState.DungeonStage.CASA_JOSE_TORRES);
				} else if (entity instanceof CharlieKirkEntity) {
					DungeonProgressionState.recordBossDefeated(bossWorld, DungeonProgressionState.DungeonStage.CASA_BLANCA);
				}
			}
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

			int upgradedCasaBlancaChunksThisCycle = 0;
			int upgradedCasaJoseTorresChunksThisCycle = 0;
			int upgradedPalacioNacionalChunksThisCycle = 0;
			ServerWorld overworld = server.getOverworld();
			CasaBlancaStructure.WhiteHouseAnchor whiteHouseAnchor = overworld != null
					? CasaBlancaStructure.getOrResolveUniqueAnchor(overworld)
					: null;
			CasaBlancaWorldState whiteHouseState = (overworld != null && whiteHouseAnchor != null)
					? CasaBlancaWorldState.get(overworld)
					: null;
			CasaJoseTorresStructure.HouseAnchor joseAnchor = overworld != null
					? CasaJoseTorresStructure.getOrResolveUniqueAnchor(overworld)
					: null;
			CasaJoseTorresWorldState joseState = (overworld != null && joseAnchor != null)
					? CasaJoseTorresWorldState.get(overworld)
					: null;
			PalacioNacionalStructure.PalaceAnchor palacioAnchor = overworld != null
					? PalacioNacionalStructure.getOrResolveUniqueAnchor(overworld)
					: null;
			PalacioNacionalWorldState palacioState = (overworld != null && palacioAnchor != null)
					? PalacioNacionalWorldState.get(overworld)
					: null;

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
						DungeonProgressionState.checkAndWarnOnDungeonEntry(player, DungeonProgressionState.DungeonStage.ISLA_PENSTEIN);
					}
				} else {
					PLAYERS_IN_ISLA_PENSTEIN.remove(playerId);
				}

				boolean insideCasaJoseTorres = CasaJoseTorresStructure.isPosInCasaJoseTorres(world, player.getBlockPos());
				if (insideCasaJoseTorres) {
					if (PLAYERS_IN_CASA_JOSE_TORRES.add(playerId)) {
						player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 20));
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("Casa de José Torres").formatted(Formatting.GOLD, Formatting.BOLD))
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.literal("La nueva casa de José Torres").formatted(Formatting.AQUA, Formatting.ITALIC))
						);
						DungeonProgressionState.checkAndWarnOnDungeonEntry(player, DungeonProgressionState.DungeonStage.CASA_JOSE_TORRES);
					}
				} else {
					PLAYERS_IN_CASA_JOSE_TORRES.remove(playerId);
				}

				boolean insideCasaBlanca = CasaBlancaStructure.isPosInCasaBlanca(world, player.getBlockPos());
				if (insideCasaBlanca) {
					if (PLAYERS_IN_CASA_BLANCA.add(playerId)) {
						player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 20));
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("La Casa Blanca").formatted(Formatting.BLUE, Formatting.BOLD))
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.literal("Sede Presidencial de Charlie Kirk").formatted(Formatting.WHITE, Formatting.ITALIC))
						);
						DungeonProgressionState.checkAndWarnOnDungeonEntry(player, DungeonProgressionState.DungeonStage.CASA_BLANCA);
					}
					// Limpiar cualquier mob hostil ajeno que haya aparecido cerca dentro de La Casa Blanca
					for (MobEntity hostile : world.getEntitiesByClass(
							MobEntity.class,
							new Box(player.getBlockPos()).expand(128.0D),
							mob -> mob instanceof Monster && !isAllowedInCasaBlanca(mob) && mob.isAlive()
					)) {
						if (CasaBlancaStructure.isPosInCasaBlanca(world, hostile.getBlockPos())) {
							hostile.discard();
						}
					}
					if (world == overworld && whiteHouseAnchor != null && !player.isSpectator()) {
						maintainCasaBlancaSecurityPatrols(world, player, whiteHouseAnchor);
					}
				} else {
					PLAYERS_IN_CASA_BLANCA.remove(playerId);
				}

				boolean insidePalacioNacional = PalacioNacionalStructure.isPosInPalacioNacional(world, player.getBlockPos());
				if (insidePalacioNacional) {
					if (PLAYERS_IN_PALACIO_NACIONAL.add(playerId)) {
						player.networkHandler.sendPacket(new TitleFadeS2CPacket(15, 70, 20));
						player.networkHandler.sendPacket(
								new TitleS2CPacket(Text.literal("El Palacio Nacional").formatted(Formatting.DARK_GREEN, Formatting.BOLD))
						);
						player.networkHandler.sendPacket(
								new SubtitleS2CPacket(Text.literal("Plaza de la Constitución • México").formatted(Formatting.RED, Formatting.ITALIC))
						);
						DungeonProgressionState.checkAndWarnOnDungeonEntry(player, DungeonProgressionState.DungeonStage.PALACIO_NACIONAL);
					}
					// Limpiar cualquier mob normal de Minecraft (hostil o pasivo/animal/ambiente) dentro de El Palacio Nacional
					for (MobEntity vanillaMob : world.getEntitiesByClass(
							MobEntity.class,
							new Box(player.getBlockPos()).expand(140.0D),
							mob -> !isAllowedInPalacioNacional(mob) && mob.isAlive()
					)) {
						if (PalacioNacionalStructure.isPosInPalacioNacional(world, vanillaMob.getBlockPos())) {
							vanillaMob.discard();
						}
					}
				} else {
					PLAYERS_IN_PALACIO_NACIONAL.remove(playerId);
				}

				// Actualizar progresivamente chunks de La Casa Blanca generados con versiones anteriores
				if (world == overworld
						&& whiteHouseAnchor != null
						&& whiteHouseState != null
						&& upgradedCasaBlancaChunksThisCycle < 12) {
					double ddx = player.getX() - (whiteHouseAnchor.centerX() + 0.5D);
					double ddz = player.getZ() - (whiteHouseAnchor.centerZ() + 0.5D);
					if (ddx * ddx + ddz * ddz <= 260.0D * 260.0D) {
						int budget = 12 - upgradedCasaBlancaChunksThisCycle;
						upgradedCasaBlancaChunksThisCycle += upgradeNearbyCasaBlancaChunks(
								world,
								whiteHouseAnchor,
								whiteHouseState,
								player.getChunkPos().x,
								player.getChunkPos().z,
								budget
						);
					}
				}

				// Actualizar progresivamente chunks de La Casa de José Torres generados con versiones anteriores
				if (world == overworld
						&& joseAnchor != null
						&& joseState != null
						&& upgradedCasaJoseTorresChunksThisCycle < 24) {
					double ddx = player.getX() - (joseAnchor.centerX() + 0.5D);
					double ddz = player.getZ() - (joseAnchor.centerZ() + 0.5D);
					if (ddx * ddx + ddz * ddz <= 300.0D * 300.0D) {
						int budget = 24 - upgradedCasaJoseTorresChunksThisCycle;
						upgradedCasaJoseTorresChunksThisCycle += upgradeNearbyCasaJoseTorresChunks(
								world,
								joseAnchor,
								joseState,
								player.getChunkPos().x,
								player.getChunkPos().z,
								budget
						);
					}
				}

				// Actualizar progresivamente chunks de El Palacio Nacional generados con versiones anteriores
				if (world == overworld
						&& palacioAnchor != null
						&& palacioState != null
						&& upgradedPalacioNacionalChunksThisCycle < 14) {
					double ddx = player.getX() - (palacioAnchor.centerX() + 0.5D);
					double ddz = player.getZ() - (palacioAnchor.centerZ() + 0.5D);
					if (ddx * ddx + ddz * ddz <= 290.0D * 290.0D) {
						int budget = 14 - upgradedPalacioNacionalChunksThisCycle;
						upgradedPalacioNacionalChunksThisCycle += upgradeNearbyPalacioNacionalChunks(
								world,
								palacioAnchor,
								palacioState,
								player.getChunkPos().x,
								player.getChunkPos().z,
								budget
						);
					}
				}
			}
		});

		// Comandos "/casablanca guardar|limpiar|tp", "/casajosetorres guardar|limpiar|tp" y "/islapenstein guardar|limpiar"
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					CommandManager.literal("casablanca")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("guardar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										int chunks = CasaBlancaCustomEdits.saveHouseFromPlayer(player, 8);
										if (chunks > 0) {
											player.sendMessage(
													Text.literal("✅ Guardados " + chunks + " chunks de tus construcciones en La Casa Blanca")
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
												int chunks = CasaBlancaCustomEdits.saveHouseFromPlayer(player, r);
												if (chunks > 0) {
													player.sendMessage(
															Text.literal("✅ Guardados " + chunks + " chunks (radio " + r + ") de La Casa Blanca")
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
										CasaBlancaCustomEdits.clearAllSavedEdits();
										ctx.getSource().sendFeedback(
												() -> Text.literal("🗑️ Se borraron los cambios personalizados; La Casa Blanca volverá a su diseño base.")
														.formatted(Formatting.YELLOW),
												true
										);
										return 1;
									})
							)
							.then(CommandManager.literal("tp")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										BlockPos tp = anchor.frontEntranceTpPos();
										player.teleport(overworld, tp.getX() + 0.5D, tp.getY(), tp.getZ() + 0.5D, Set.of(), 180.0F, 0.0F, true);
										CasaBlancaWorldState state = CasaBlancaWorldState.get(overworld);
										upgradeNearbyCasaBlancaChunks(overworld, anchor, state, anchor.startChunkX(), anchor.startChunkZ(), 36);
										upgradeNearbyCasaBlancaChunks(overworld, anchor, state, tp.getX() >> 4, tp.getZ() >> 4, 24);
										player.sendMessage(
												Text.literal("🏛️ Teletransportado a La Casa Blanca en (" + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ() + ").")
														.formatted(Formatting.BLUE, Formatting.BOLD),
												false
										);
										return 1;
									})
							)
							.then(CommandManager.literal("reparar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										CasaBlancaWorldState state = CasaBlancaWorldState.get(overworld);
										state.resetUpgradedChunks();
										int repaired = upgradeNearbyCasaBlancaChunks(
												overworld,
												anchor,
												state,
												player.getChunkPos().x,
												player.getChunkPos().z,
												1024
										);
										player.sendMessage(
												Text.literal("🔧 Reparados " + repaired + " chunks cargados de La Casa Blanca (iluminación, arquitectura, lianas y vegetación).")
														.formatted(Formatting.GREEN, Formatting.BOLD),
												false
										);
										return repaired;
									})
							)
			);
			dispatcher.register(
					CommandManager.literal("casajosetorres")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("guardar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										int chunks = CasaJoseTorresCustomEdits.saveHouseFromPlayer(player, 8);
										if (chunks > 0) {
											player.sendMessage(
													Text.literal("✅ Guardados " + chunks + " chunks de tus construcciones en La Casa de José Torres")
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
												int chunks = CasaJoseTorresCustomEdits.saveHouseFromPlayer(player, r);
												if (chunks > 0) {
													player.sendMessage(
															Text.literal("✅ Guardados " + chunks + " chunks (radio " + r + ") de La Casa de José Torres")
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
										CasaJoseTorresCustomEdits.clearAllSavedEdits();
										ctx.getSource().sendFeedback(
												() -> Text.literal("🗑️ Se borraron los cambios personalizados; La Casa de José Torres volverá a su diseño base.")
														.formatted(Formatting.YELLOW),
												true
										);
										return 1;
									})
							)
							.then(CommandManager.literal("tp")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										CasaJoseTorresStructure.HouseAnchor anchor = CasaJoseTorresStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										BlockPos tp = anchor.frontEntranceTpPos();
										player.teleport(overworld, tp.getX() + 0.5D, tp.getY(), tp.getZ() + 0.5D, Set.of(), 180.0F, 0.0F, true);
										CasaJoseTorresWorldState state = CasaJoseTorresWorldState.get(overworld);
										upgradeNearbyCasaJoseTorresChunks(overworld, anchor, state, anchor.startChunkX(), anchor.startChunkZ(), 36);
										upgradeNearbyCasaJoseTorresChunks(overworld, anchor, state, tp.getX() >> 4, tp.getZ() >> 4, 24);
										player.sendMessage(
												Text.literal("🏠 Teletransportado a La Casa de José Torres en (" + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ() + ").")
														.formatted(Formatting.GOLD, Formatting.BOLD),
												false
										);
										return 1;
									})
							)
							.then(CommandManager.literal("reparar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										CasaJoseTorresStructure.HouseAnchor anchor = CasaJoseTorresStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										CasaJoseTorresWorldState state = CasaJoseTorresWorldState.get(overworld);
										state.resetUpgradedChunks();
										int repaired = upgradeNearbyCasaJoseTorresChunks(
												overworld,
												anchor,
												state,
												player.getChunkPos().x,
												player.getChunkPos().z,
												1024
										);
										player.sendMessage(
												Text.literal("🔧 Reparados " + repaired + " chunks cargados de La Casa de José Torres (terreno, transición y ríos).")
														.formatted(Formatting.GREEN, Formatting.BOLD),
												false
										);
										return repaired;
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
													Text.literal("✅ Guardados " + chunks + " chunks de tus construcciones en la Isla Penstein. Ahora aparecerán así en cada nuevo spawneo de la dungeon.")
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
															Text.literal("✅ Guardados " + chunks + " chunks (radio " + r + ") de la Isla Penstein")
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
			dispatcher.register(
					CommandManager.literal("palacionacional")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("guardar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										int chunks = PalacioNacionalCustomEdits.savePalacioFromPlayer(player, 8);
										if (chunks > 0) {
											player.sendMessage(
													Text.literal("✅ Guardados " + chunks + " chunks de tus construcciones en El Palacio Nacional")
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
												int chunks = PalacioNacionalCustomEdits.savePalacioFromPlayer(player, r);
												if (chunks > 0) {
													player.sendMessage(
															Text.literal("✅ Guardados " + chunks + " chunks (radio " + r + ") de El Palacio Nacional")
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
										PalacioNacionalCustomEdits.clearAllSavedEdits();
										ctx.getSource().sendFeedback(
												() -> Text.literal("🗑️ Se borraron los cambios personalizados; El Palacio Nacional volverá a su diseño base.")
														.formatted(Formatting.YELLOW),
												true
										);
										return 1;
									})
							)
							.then(CommandManager.literal("tp")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										PalacioNacionalStructure.PalaceAnchor anchor = PalacioNacionalStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										BlockPos tp = anchor.frontEntranceTpPos();
										player.teleport(overworld, tp.getX() + 0.5D, tp.getY(), tp.getZ() + 0.5D, Set.of(), 180.0F, 0.0F, true);
										PalacioNacionalWorldState state = PalacioNacionalWorldState.get(overworld);
										upgradeNearbyPalacioNacionalChunks(overworld, anchor, state, anchor.startChunkX(), anchor.startChunkZ(), 36);
										upgradeNearbyPalacioNacionalChunks(overworld, anchor, state, tp.getX() >> 4, tp.getZ() >> 4, 24);
										player.sendMessage(
												Text.literal("🇲🇽 Teletransportado a El Palacio Nacional en (" + anchor.centerX() + ", " + anchor.baseY() + ", " + anchor.centerZ() + ").")
														.formatted(Formatting.GREEN, Formatting.BOLD),
												false
										);
										return 1;
									})
							)
							.then(CommandManager.literal("reparar")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										ServerWorld overworld = ctx.getSource().getServer().getOverworld();
										if (overworld == null) {
											return 0;
										}
										PalacioNacionalStructure.PalaceAnchor anchor = PalacioNacionalStructure.getOrResolveUniqueAnchor(overworld);
										if (anchor == null) {
											return 0;
										}
										PalacioNacionalWorldState state = PalacioNacionalWorldState.get(overworld);
										state.resetUpgradedChunks();
										int repaired = upgradeNearbyPalacioNacionalChunks(
												overworld,
												anchor,
												state,
												player.getChunkPos().x,
												player.getChunkPos().z,
												1024
										);
										player.sendMessage(
												Text.literal("🔧 Reparados " + repaired + " chunks cargados de El Palacio Nacional (Zócalo, fachada, salones y terreno).")
														.formatted(Formatting.GREEN, Formatting.BOLD),
												false
										);
										return repaired;
									})
							)
			);
			dispatcher.register(
					CommandManager.literal("progresodungeons")
							.requires(source -> source.hasPermissionLevel(2))
							.then(CommandManager.literal("estado")
									.executes(ctx -> {
										DungeonProgressionState state = DungeonProgressionState.get(ctx.getSource().getServer());
										if (state == null) {
											return 0;
										}
										for (DungeonProgressionState.DungeonStage stage : DungeonProgressionState.DungeonStage.values()) {
											boolean done = state.isDungeonCompleted(stage);
											ctx.getSource().sendFeedback(
													() -> Text.literal((done ? "✅ " : "❌ ") + stage.getDungeonDisplayName() + " (" + stage.getBossDisplayName() + ")")
															.formatted(done ? Formatting.GREEN : Formatting.RED),
													false
											);
										}
										return 1;
									})
							)
							.then(CommandManager.literal("completar")
									.then(CommandManager.argument("etapa", com.mojang.brigadier.arguments.StringArgumentType.word())
											.suggests((ctx, builder) -> {
												for (DungeonProgressionState.DungeonStage s : DungeonProgressionState.DungeonStage.values()) {
													builder.suggest(s.getId());
												}
												return builder.buildFuture();
											})
											.executes(ctx -> {
												String raw = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "etapa");
												DungeonProgressionState.DungeonStage stage = DungeonProgressionState.DungeonStage.byId(raw);
												if (stage == null) {
													ctx.getSource().sendError(Text.literal("Etapa desconocida: " + raw));
													return 0;
												}
												DungeonProgressionState state = DungeonProgressionState.get(ctx.getSource().getServer());
												if (state != null) {
													state.markCompleted(stage);
													DungeonProgressionState.broadcastToAll(ctx.getSource().getServer());
												}
												ctx.getSource().sendFeedback(
														() -> Text.literal("✅ Marcada como derrotada: " + stage.getDungeonDisplayName() + " (" + stage.getBossDisplayName() + ")")
																.formatted(Formatting.GREEN, Formatting.BOLD),
														true
												);
												return 1;
											})
									)
							)
							.then(CommandManager.literal("reiniciar")
									.executes(ctx -> {
										DungeonProgressionState state = DungeonProgressionState.get(ctx.getSource().getServer());
										if (state != null) {
											state.resetAll();
											DungeonProgressionState.broadcastToAll(ctx.getSource().getServer());
										}
										ctx.getSource().sendFeedback(
												() -> Text.literal("🔄 Se reinició el progreso global de dungeons derrotadas.")
														.formatted(Formatting.YELLOW),
												true
										);
										return 1;
									})
							)
			);
		});
	}

	private static int upgradeNearbyPalacioNacionalChunks(
			ServerWorld overworld,
			PalacioNacionalStructure.PalaceAnchor anchor,
			PalacioNacionalWorldState state,
			int originChunkX,
			int originChunkZ,
			int maxChunks
	) {
		return upgradeNearbyStructureChunks(
				overworld,
				anchor.startChunkX(),
				anchor.startChunkZ(),
				PalacioNacionalStructure.CHUNK_RADIUS,
				originChunkX,
				originChunkZ,
				maxChunks,
				state::isChunkUpgraded,
				(cx, cz) -> {
					PalacioNacionalPiece.upgradeChunkInPlace(overworld, cx, cz, anchor);
					state.markChunkUpgraded(cx, cz);
				}
		);
	}

	private static int upgradeNearbyCasaBlancaChunks(
			ServerWorld overworld,
			CasaBlancaStructure.WhiteHouseAnchor anchor,
			CasaBlancaWorldState state,
			int originChunkX,
			int originChunkZ,
			int maxChunks
	) {
		return upgradeNearbyStructureChunks(
				overworld,
				anchor.startChunkX(),
				anchor.startChunkZ(),
				CasaBlancaStructure.CHUNK_RADIUS,
				originChunkX,
				originChunkZ,
				maxChunks,
				state::isChunkUpgraded,
				(cx, cz) -> {
					CasaBlancaPiece.upgradeChunkInPlace(overworld, cx, cz, anchor);
					state.markChunkUpgraded(cx, cz);
				}
		);
	}

	private static int upgradeNearbyCasaJoseTorresChunks(
			ServerWorld overworld,
			CasaJoseTorresStructure.HouseAnchor anchor,
			CasaJoseTorresWorldState state,
			int originChunkX,
			int originChunkZ,
			int maxChunks
	) {
		return upgradeNearbyStructureChunks(
				overworld,
				anchor.startChunkX(),
				anchor.startChunkZ(),
				CasaJoseTorresStructure.CHUNK_RADIUS,
				originChunkX,
				originChunkZ,
				maxChunks,
				state::isChunkUpgraded,
				(cx, cz) -> {
					CasaJoseTorresPiece.upgradeChunkInPlace(overworld, cx, cz, anchor);
					state.markChunkUpgraded(cx, cz);
				}
		);
	}

	private static int upgradeNearbyStructureChunks(
			ServerWorld overworld,
			int startChunkX,
			int startChunkZ,
			int radius,
			int originChunkX,
			int originChunkZ,
			int maxChunks,
			BiPredicate<Integer, Integer> isUpgraded,
			BiConsumer<Integer, Integer> upgradeAction
	) {
		List<int[]> pending = new ArrayList<>();
		for (int cx = startChunkX - radius; cx <= startChunkX + radius; cx++) {
			for (int cz = startChunkZ - radius; cz <= startChunkZ + radius; cz++) {
				if (!isUpgraded.test(cx, cz) && overworld.isChunkLoaded(cx, cz)) {
					int distSq = (cx - originChunkX) * (cx - originChunkX) + (cz - originChunkZ) * (cz - originChunkZ);
					pending.add(new int[]{cx, cz, distSq});
				}
			}
		}
		if (pending.isEmpty()) {
			return 0;
		}
		pending.sort(Comparator.comparingInt(a -> a[2]));
		int limit = Math.min(maxChunks, pending.size());
		for (int i = 0; i < limit; i++) {
			int cx = pending.get(i)[0];
			int cz = pending.get(i)[1];
			upgradeAction.accept(cx, cz);
		}
		return limit;
	}

	private static void maintainCasaBlancaSecurityPatrols(
			ServerWorld world,
			ServerPlayerEntity player,
			CasaBlancaStructure.WhiteHouseAnchor anchor
	) {
		if (world.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		boolean charlieInCinematic = !world.getEntitiesByClass(
				CharlieKirkEntity.class,
				new Box(new BlockPos(anchor.centerX(), anchor.baseY() + 2, anchor.centerZ())).expand(240.0D),
				CharlieKirkEntity::isInAnyCinematic
		).isEmpty();
		if (charlieInCinematic) {
			return;
		}

		int guardsNear = world.getEntitiesByClass(
				WhiteHouseGuardEntity.class,
				new Box(player.getBlockPos()).expand(110.0D),
				e -> e.isAlive()
		).size();
		if (guardsNear >= 22) {
			return;
		}

		net.minecraft.util.math.random.Random rand = world.getRandom();
		int spawnedThisCycle = 0;
		for (int attempt = 0; attempt < 12 && spawnedThisCycle < 2 && guardsNear < 22; attempt++) {
			double angle = rand.nextDouble() * (Math.PI * 2.0D);
			double dist = 16.0D + rand.nextDouble() * 48.0D;
			int sx = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
			int sz = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
			int dx = sx - anchor.centerX();
			int dz = sz - anchor.centerZ();

			// Evitar spawnear patrullas dentro del Salón Principal de Conferencias (arena de Charlie Kirk)
			if (Math.abs(dx) <= 38 && dz >= -28 && dz <= 20) {
				continue;
			}
			// Mantener dentro del perímetro interior utilizable de la propiedad
			if (Math.abs(dx) > 154 || Math.abs(dz) > 154) {
				continue;
			}

			boolean inBuilding = (Math.abs(dx) <= 52 && Math.abs(dz) <= 34)
					|| (Math.abs(dx) >= 53 && Math.abs(dx) <= 108 && Math.abs(dz) <= 24);
			int sy = anchor.baseY() + ((inBuilding && rand.nextBoolean()) ? 9 : 1);
			BlockPos candidate = new BlockPos(sx, sy, sz);

			if (!world.isChunkLoaded(candidate) || !CasaBlancaStructure.isPosInCasaBlanca(world, candidate)) {
				continue;
			}
			if (!world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())) {
				continue;
			}
			if (!world.getBlockState(candidate).getCollisionShape(world, candidate).isEmpty()
					|| !world.getFluidState(candidate).isEmpty()
					|| !world.getBlockState(candidate.up()).getCollisionShape(world, candidate.up()).isEmpty()
					|| !world.getFluidState(candidate.up()).isEmpty()) {
				continue;
			}

			// Misma densidad que Isla Penstein (< 12 en radio 28) + 10% (< 13 en radio 28), sin amontonarse en grupos grandes
			if (world.getEntitiesByClass(WhiteHouseGuardEntity.class, new Box(candidate).expand(8.0D), e -> e.isAlive()).size() >= 2) {
				continue;
			}
			if (world.getEntitiesByClass(WhiteHouseGuardEntity.class, new Box(candidate).expand(28.0D), e -> e.isAlive()).size() >= 13) {
				continue;
			}
			boolean tooCloseToPlayer = false;
			for (ServerPlayerEntity p : world.getPlayers()) {
				if (!p.isSpectator() && p.squaredDistanceTo(sx + 0.5D, sy, sz + 0.5D) < 16.0D * 16.0D) {
					tooCloseToPlayer = true;
					break;
				}
			}
			if (tooCloseToPlayer) {
				continue;
			}

			int roll = rand.nextInt(100);
			EntityType<WhiteHouseGuardEntity> guardType;
			if (roll < 44) {
				guardType = ModEntities.WHITE_HOUSE_GUARD_PISTOL;
			} else if (roll < 84) {
				guardType = ModEntities.WHITE_HOUSE_GUARD_RIFLE;
			} else {
				guardType = ModEntities.WHITE_HOUSE_GUARD_BAZOOKA;
			}

			WhiteHouseGuardEntity guard = guardType.create(world, SpawnReason.NATURAL);
			if (guard != null) {
				guard.refreshPositionAndAngles(sx + 0.5D, sy, sz + 0.5D, rand.nextFloat() * 360.0F, 0.0F);
				world.spawnEntity(guard);
				spawnedThisCycle++;
				guardsNear++;
			}
		}
	}

	private static boolean isAllowedInCasaBlanca(net.minecraft.entity.Entity entity) {
		return isProtectedModBoss(entity)
				|| entity instanceof WhiteHouseGuardEntity
				|| CharlieEpsteinSummonHelper.isEpsteinSummon(entity);
	}

	private static boolean isAllowedInPalacioNacional(net.minecraft.entity.Entity entity) {
		if (isProtectedModBoss(entity) || CharlieEpsteinSummonHelper.isEpsteinSummon(entity)) {
			return true;
		}
		if (entity.hasPassengers() || (entity instanceof TameableEntity tameable && tameable.isTamed())) {
			return true;
		}
		Identifier typeId = Registries.ENTITY_TYPE.getId(entity.getType());
		if (!MOD_ID.equals(typeId.getNamespace())) {
			return false; // Bloquear 100% de los mobs normales de Minecraft (hostiles, animales, aldeanos, murciélagos, etc.)
		}
		// Evitar que guardias exclusivos de otras dungeons spawneen aquí por error
		return !(entity instanceof WhiteHouseGuardEntity);
	}

	private static boolean isProtectedModBoss(net.minecraft.entity.Entity entity) {
		return entity instanceof CharlieKirkEntity
				|| entity instanceof JoseTorresEntity
				|| entity instanceof JefriPensteinEntity
				|| entity instanceof PiDidiEntity;
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
