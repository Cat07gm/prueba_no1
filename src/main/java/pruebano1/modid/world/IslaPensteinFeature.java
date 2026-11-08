package pruebano1.modid.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Feature encargada de generar los 37x37 chunks completos (520x520 bloques + margen oceánico suave)
 * de la Isla Penstein sin estar limitada al radio de 8 chunks de StructureReferences.
 */
public class IslaPensteinFeature extends Feature<DefaultFeatureConfig> {

    public IslaPensteinFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        long seed = world.getSeed();

        ChunkPos startChunk = IslaPensteinStructure.findIslandStartChunk(seed, chunkX, chunkZ);

        if (startChunk == null) {
            return false;
        }

        ServerWorld serverWorld = world.toServerWorld();
        if (!IslaPensteinStructure.isValidIslandCenter(
                seed,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world,
                startChunk
        )) {
            return false;
        }

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        BlockBox chunkBox = new BlockBox(minX, 34, minZ, minX + 15, 115, minZ + 15);
        IslaPensteinPiece.generateChunkForIsland(
                world,
                chunkBox,
                startChunk.getCenterX(),
                startChunk.getCenterZ(),
                context.getRandom()
        );
        return true;
    }
}
