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
 * Feature ejecutada en TOP_LAYER_MODIFICATION (después de toda la vegetación del bioma) para eliminar
 * lianas flotantes, árboles/vegetación invasiva dentro de caminos y vistas cinematográficas, y asegurar
 * el acabado limpio de La Casa Blanca.
 */
public class CasaBlancaCleanupFeature extends Feature<DefaultFeatureConfig> {

    public CasaBlancaCleanupFeature(Codec<DefaultFeatureConfig> configCodec) {
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

        boolean cleanedAny = false;

        CasaBlancaStructure.WhiteHouseAnchor anchor = CasaBlancaStructure.resolveUniqueAnchor(
                world.getSeed(),
                serverWorld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world
        );

        if (Math.abs(chunkX - anchor.startChunkX()) <= CasaBlancaStructure.CHUNK_RADIUS
                && Math.abs(chunkZ - anchor.startChunkZ()) <= CasaBlancaStructure.CHUNK_RADIUS) {
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

            CasaBlancaPiece.postCleanWhiteHouseChunk(
                    world,
                    chunkBox,
                    anchor.centerX(),
                    anchor.baseY(),
                    anchor.centerZ()
            );
            cleanedAny = true;
        }

        CasaJoseTorresStructure.HouseAnchor joseAnchor = CasaJoseTorresStructure.resolveUniqueAnchor(
                world.getSeed(),
                serverWorld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world
        );

        if (Math.abs(chunkX - joseAnchor.startChunkX()) <= CasaJoseTorresStructure.CHUNK_RADIUS
                && Math.abs(chunkZ - joseAnchor.startChunkZ()) <= CasaJoseTorresStructure.CHUNK_RADIUS) {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            BlockBox chunkBox = new BlockBox(
                    minX,
                    world.getBottomY() + 1,
                    minZ,
                    minX + 15,
                    world.getTopYInclusive(),
                    minZ + 15
            );

            CasaJoseTorresPiece.postCleanJoseTorresChunk(
                    world,
                    chunkBox,
                    joseAnchor.centerX(),
                    joseAnchor.baseY(),
                    joseAnchor.centerZ()
            );
            cleanedAny = true;
        }

        PalacioNacionalStructure.PalaceAnchor palacioAnchor = PalacioNacionalStructure.resolveUniqueAnchor(
                world.getSeed(),
                serverWorld,
                context.getGenerator(),
                context.getGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                world
        );

        if (Math.abs(chunkX - palacioAnchor.startChunkX()) <= PalacioNacionalStructure.CHUNK_RADIUS
                && Math.abs(chunkZ - palacioAnchor.startChunkZ()) <= PalacioNacionalStructure.CHUNK_RADIUS) {
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            BlockBox chunkBox = new BlockBox(
                    minX,
                    Math.max(world.getBottomY() + 1, palacioAnchor.baseY() - 28),
                    minZ,
                    minX + 15,
                    world.getTopYInclusive(),
                    minZ + 15
            );

            PalacioNacionalPiece.postCleanPalacioChunk(
                    world,
                    chunkBox,
                    palacioAnchor.centerX(),
                    palacioAnchor.baseY(),
                    palacioAnchor.centerZ()
            );
            cleanedAny = true;
        }

        return cleanedAny;
    }
}
