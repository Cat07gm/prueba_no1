package pruebano1.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.network.PlayerDeathKoPayload;

public class Prueba_no1 implements ModInitializer {
	public static final String MOD_ID = "prueba_no1";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		ModSounds.registerModSounds();
		ModEntities.registerModEntities();
		ModItemGroups.registerItemGroups();
		ModItem.registerModItems();
		ModBlocks.registerModBlocks();
		ModWorldGeneration.generateModWorldGen();

		// Registrar paquete de red para el efecto de muerte del jugador
		PayloadTypeRegistry.playS2C().register(PlayerDeathKoPayload.ID, PlayerDeathKoPayload.CODEC);

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
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
