package pruebano1.modid.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Feature de generación única del "Escenario del Podcast" (Altar de José Torres) en el Overworld.
 */
public class PodcastStageFeature extends Feature<DefaultFeatureConfig> {

    public PodcastStageFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        ServerWorld overworld = world.toServerWorld();
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }

        PodcastStageManager.StageAnchor anchor = PodcastStageManager.resolveAnchorForGeneration(
                overworld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                overworld.getChunkManager().getNoiseConfig(),
                world
        );

        BlockPos origin = context.getOrigin();
        ChunkPos chunkPos = new ChunkPos(origin);
        if (Math.abs(chunkPos.x - anchor.startChunkX()) > PodcastStageManager.CHUNK_RADIUS
                || Math.abs(chunkPos.z - anchor.startChunkZ()) > PodcastStageManager.CHUNK_RADIUS) {
            return false;
        }

        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        BlockBox chunkBox = new BlockBox(
                minX,
                anchor.baseY() - 12,
                minZ,
                minX + 15,
                anchor.baseY() + 28,
                minZ + 15
        );

        PodcastStagePiece.generateChunk(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ()
        );
        PodcastStageManager.recordGeneratedChunk(overworld.getServer(), chunkPos.x, chunkPos.z);
        return true;
    }
}
