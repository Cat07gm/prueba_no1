package pruebano1.modid.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Genera, chunk por chunk, la torre única del Lobby principal en el punto de aparición del mundo.
 */
public class LobbyTowerFeature extends Feature<DefaultFeatureConfig> {

    public LobbyTowerFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        ServerWorld overworld = world.toServerWorld();
        if (overworld.getRegistryKey() != World.OVERWORLD
                || LobbyTowerManager.isDisabled(overworld.getServer())) {
            return false;
        }

        LobbyTowerManager.LobbyAnchor anchor = LobbyTowerManager.resolveAnchorForGeneration(
                overworld,
                context.getGenerator(),
                overworld.getChunkManager().getNoiseConfig(),
                world
        );

        ChunkPos chunkPos = new ChunkPos(context.getOrigin());
        if (Math.abs(chunkPos.x - anchor.chunkX()) > LobbyTowerManager.CHUNK_RADIUS
                || Math.abs(chunkPos.z - anchor.chunkZ()) > LobbyTowerManager.CHUNK_RADIUS) {
            return false;
        }

        LobbyTowerPiece.generateChunk(
                world,
                LobbyTowerManager.chunkBox(chunkPos.x, chunkPos.z, anchor.baseY()),
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ()
        );
        LobbyTowerManager.recordGeneratedChunk(overworld.getServer(), chunkPos.x, chunkPos.z);
        return true;
    }
}
