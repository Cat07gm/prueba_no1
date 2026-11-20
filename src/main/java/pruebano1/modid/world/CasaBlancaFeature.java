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
 * Feature encargada de generar los 29x29 chunks completos (448x448 bloques) de la 3.ª gran dungeon
 * única "La Casa Blanca", sin cortes en bordes de chunk.
 */
public class CasaBlancaFeature extends Feature<DefaultFeatureConfig> {

    public CasaBlancaFeature(Codec<DefaultFeatureConfig> configCodec) {
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

        CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.resolveUniqueAnchor(
                world.getSeed(),
                serverWorld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world
        );

        if (Math.abs(chunkX - anchor.startChunkX()) > CasaBlancaStructure.CHUNK_RADIUS
                || Math.abs(chunkZ - anchor.startChunkZ()) > CasaBlancaStructure.CHUNK_RADIUS) {
            return false;
        }

        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        BlockBox chunkBox = new BlockBox(
                minX,
                Math.max(world.getBottomY() + 1, anchor.baseY() - 28),
                minZ,
                minX + 15,
                world.getTopYInclusive(),
                minZ + 15
        );

        CasaBlancaPiece.generateChunkForWhiteHouse(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ(),
                context.getRandom(),
                true
        );
        CasaBlancaWorldState.get(serverWorld).markChunkUpgraded(chunkX, chunkZ);
        return true;
    }
}
