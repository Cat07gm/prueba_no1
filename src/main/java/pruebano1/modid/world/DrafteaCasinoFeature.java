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
 * Feature de generación única del casino "Draftea Tony Diaz" cerca del spawn del Overworld.
 */
public class DrafteaCasinoFeature extends Feature<DefaultFeatureConfig> {

    public DrafteaCasinoFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        ServerWorld overworld = world.toServerWorld();
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }

        DrafteaCasinoManager.CasinoAnchor anchor = DrafteaCasinoManager.resolveAnchorForGeneration(
                overworld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                overworld.getChunkManager().getNoiseConfig(),
                world
        );

        BlockPos origin = context.getOrigin();
        ChunkPos chunkPos = new ChunkPos(origin);
        if (Math.abs(chunkPos.x - anchor.startChunkX()) > DrafteaCasinoManager.CHUNK_RADIUS
                || Math.abs(chunkPos.z - anchor.startChunkZ()) > DrafteaCasinoManager.CHUNK_RADIUS) {
            return false;
        }

        DrafteaCasinoCustomEdits.ensureLoaded();

        int minX = chunkPos.getStartX();
        int minZ = chunkPos.getStartZ();
        BlockBox chunkBox = new BlockBox(
                minX,
                anchor.baseY() - 18,
                minZ,
                minX + 15,
                anchor.baseY() + 45,
                minZ + 15
        );

        DrafteaCasinoPiece.generateChunkForCasino(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ(),
                context.getRandom()
        );
        DrafteaCasinoManager.recordGeneratedChunk(overworld.getServer(), chunkPos.x, chunkPos.z);
        return true;
    }
}
