package pruebano1.modid.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estructura de mundo: "La Isla de Albert Penstein" (Isla Penstein) a escala 5x (520x520 bloques).
 * Se genera únicamente en medio del océano profundo, verificando una zona de 720x720 bloques sin tierra firme
 * para que nunca choque con continentes ni islas normales de Minecraft.
 */
public class IslaPensteinStructure extends Structure {

    public static final MapCodec<IslaPensteinStructure> CODEC = createCodec(IslaPensteinStructure::new);

    // Misma configuración exacta que worldgen/structure_set/isla_penstein.json
    public static final RandomSpreadStructurePlacement PLACEMENT = new RandomSpreadStructurePlacement(
            180,
            60,
            SpreadType.LINEAR,
            194827364
    );

    // Radio de chunks (18 chunks = ±288 bloques) que abarca la isla + su foso oceánico de seguridad
    public static final int CHUNK_RADIUS = 18;

    private static final Map<Long, Boolean> VALID_CENTER_CACHE = new ConcurrentHashMap<>();

    public IslaPensteinStructure(Config config) {
        super(config);
    }

    /**
     * Busca el startChunk de la Isla Penstein dentro de un radio de ±CHUNK_RADIUS chunks,
     * comprobando también las regiones vecinas de 180x180 chunks para que la isla JAMÁS se corte
     * en línea recta cuando cruza el borde de una región de RandomSpreadStructurePlacement.
     */
    public static ChunkPos findIslandStartChunk(long seed, int chunkX, int chunkZ) {
        int r = CHUNK_RADIUS;
        int[][] cornerOffsets = {{0, 0}, {-r, -r}, {r, -r}, {-r, r}, {r, r}};
        for (int[] off : cornerOffsets) {
            ChunkPos candidate = PLACEMENT.getStartChunk(seed, chunkX + off[0], chunkZ + off[1]);
            if (Math.abs(chunkX - candidate.x) <= r && Math.abs(chunkZ - candidate.z) <= r) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isValidIslandCenter(
            long seed,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView world,
            ChunkPos startChunk
    ) {
        long cacheKey = (seed * 31L) ^ startChunk.toLong();
        return VALID_CENTER_CACHE.computeIfAbsent(cacheKey, k -> {
            int centerX = startChunk.getCenterX();
            int centerZ = startChunk.getCenterZ();
            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();

            // 1. Verificar que el centro sea estrictamente océano profundo
            RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, 62 >> 2, centerZ >> 2, sampler);
            if (!centerBiome.isIn(BiomeTags.IS_DEEP_OCEAN)) {
                return false;
            }

            // 2. Verificar una cuadrícula completa de 720x720 bloques (-360 a +360 cada 90 bloques)
            // para garantizar que TODOS los alrededores sean bioma oceánico y estén bajo el nivel del mar (sin islas ni costas cerca)
            for (int ox = -360; ox <= 360; ox += 90) {
                for (int oz = -360; oz <= 360; oz += 90) {
                    int sampleX = centerX + ox;
                    int sampleZ = centerZ + oz;

                    RegistryEntry<Biome> biome = biomeSource.getBiome(sampleX >> 2, 62 >> 2, sampleZ >> 2, sampler);
                    if (!biome.isIn(BiomeTags.IS_OCEAN)) {
                        return false;
                    }

                    // Dentro de los 300 bloques de radio donde se asienta la isla, exigir que el suelo natural esté sumergido
                    if (Math.abs(ox) <= 270 && Math.abs(oz) <= 270) {
                        int floorY = chunkGenerator.getHeightInGround(
                                sampleX,
                                sampleZ,
                                Heightmap.Type.OCEAN_FLOOR_WG,
                                world,
                                noiseConfig
                        );
                        if (floorY > 57) {
                            return false;
                        }
                    }
                }
            }
            return true;
        });
    }

    /**
     * Comprueba en O(1) si una posición del mundo se encuentra dentro de los 520x520 bloques de una Isla Penstein válida.
     */
    public static boolean isPosOnIsland(ServerWorldAccess worldAccess, BlockPos pos) {
        ServerWorld serverWorld = worldAccess.toServerWorld();
        long seed = serverWorld.getSeed();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        ChunkPos startChunk = findIslandStartChunk(seed, chunkX, chunkZ);
        if (startChunk == null) {
            return false;
        }

        int dx = pos.getX() - startChunk.getCenterX();
        int dz = pos.getZ() - startChunk.getCenterZ();
        if (Math.abs(dx) > IslaPensteinPiece.ISLAND_RADIUS || Math.abs(dz) > IslaPensteinPiece.ISLAND_RADIUS) {
            return false;
        }

        return isValidIslandCenter(
                seed,
                serverWorld.getChunkManager().getChunkGenerator(),
                serverWorld.getChunkManager().getChunkGenerator().getBiomeSource(),
                serverWorld.getChunkManager().getNoiseConfig(),
                serverWorld,
                startChunk
        );
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        if (!isValidIslandCenter(
                context.seed(),
                context.chunkGenerator(),
                context.biomeSource(),
                context.noiseConfig(),
                context.world(),
                chunkPos
        )) {
            return Optional.empty();
        }

        int centerX = chunkPos.getCenterX();
        int centerZ = chunkPos.getCenterZ();
        BlockPos pos = new BlockPos(centerX, IslaPensteinPiece.SEA_LEVEL, centerZ);
        return Optional.of(new StructurePosition(pos, collector -> {
            collector.addPiece(new IslaPensteinPiece(centerX, centerZ));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.ISLA_PENSTEIN_TYPE;
    }
}
