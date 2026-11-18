package pruebano1.modid.world;

import com.mojang.serialization.Codec;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Feature encargada de generar los 29x29 chunks completos (448x448 bloques) de la dungeon única
 * "La Casa de José Torres" en el bioma de nieve, sin cortes de bordes ni límite de 8 chunks.
 */
public class CasaJoseTorresFeature extends Feature<DefaultFeatureConfig> {

    public CasaJoseTorresFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        ServerWorld serverWorld = world.toServerWorld();
        if (serverWorld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }

        BlockPos origin = context.getOrigin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;

        CasaJoseTorresStructure.HouseAnchor anchor = CasaJoseTorresStructure.resolveUniqueAnchor(
                world.getSeed(),
                serverWorld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world
        );

        if (Math.abs(chunkX - anchor.startChunkX()) > CasaJoseTorresStructure.CHUNK_RADIUS
                || Math.abs(chunkZ - anchor.startChunkZ()) > CasaJoseTorresStructure.CHUNK_RADIUS) {
            return false;
        }

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        BlockBox chunkBox = new BlockBox(
                minX,
                Math.max(world.getBottomY() + 1, anchor.baseY() - 28),
                minZ,
                minX + 15,
                Math.min(world.getTopYInclusive(), anchor.baseY() + 48),
                minZ + 15
        );

        CasaJoseTorresPiece.generateChunkForHouse(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ(),
                context.getRandom()
        );
        return true;
    }
}
