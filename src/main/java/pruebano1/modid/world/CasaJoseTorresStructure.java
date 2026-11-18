package pruebano1.modid.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Segunda gran Dungeon del mod: "La Casa de José Torres" (420x420 bloques, casi del tamaño de Isla Penstein).
 * - ÚNICA en el mundo (solo se genera 1 ejemplar en todo el Overworld).
 * - Súper y extremadamente difícil de encontrar (~4,500 a 9,500 bloques de distancia del origen).
 * - Aparece en cualquier bioma terrestre sin nieve, con nivelación y transición suave sin cortes bruscos de terreno.
 */
public class CasaJoseTorresStructure extends Structure {

    public static final MapCodec<CasaJoseTorresStructure> CODEC = createCodec(CasaJoseTorresStructure::new);

    // Misma configuración que worldgen/structure_set/casa_jose_torres.json
    public static final RandomSpreadStructurePlacement PLACEMENT = new RandomSpreadStructurePlacement(
            320,
            120,
            SpreadType.LINEAR,
            847291635
    );

    /** Radio de chunks (14 chunks = ±224 bloques = 448x448 bloques) que abarca toda la propiedad y su talud suave. */
    public static final int CHUNK_RADIUS = 14;

    private static final Object ANCHOR_LOCK = new Object();
    private static final Map<Long, HouseAnchor> SEED_ANCHOR_CACHE = new ConcurrentHashMap<>();

    public record HouseAnchor(int startChunkX, int baseY, int startChunkZ) {
        public int centerX() {
            return (startChunkX << 4) + 8;
        }

        public int centerZ() {
            return (startChunkZ << 4) + 8;
        }

        public BlockPos frontEntranceTpPos() {
            return new BlockPos(centerX(), baseY + 1, centerZ() + 72);
        }

        public BlockPos backyardCassettePos() {
            return new BlockPos(centerX() + CasaJoseTorresPiece.CASSETTE_REL_X, baseY + 2, centerZ() + CasaJoseTorresPiece.CASSETTE_REL_Z);
        }
    }

    public CasaJoseTorresStructure(Config config) {
        super(config);
    }

    public static void clearCaches() {
        SEED_ANCHOR_CACHE.clear();
    }

    public static void onOverworldLoaded(ServerWorld overworld) {
        if (overworld.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        synchronized (ANCHOR_LOCK) {
            CasaJoseTorresWorldState state = CasaJoseTorresWorldState.get(overworld);
            if (state.isInitialized()) {
                SEED_ANCHOR_CACHE.put(
                        overworld.getSeed(),
                        new HouseAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ())
                );
            }
        }
    }

    @Nullable
    public static HouseAnchor getOrResolveUniqueAnchor(ServerWorld overworld) {
        ServerWorld targetWorld = overworld.getServer().getOverworld();
        if (targetWorld == null) {
            return null;
        }
        return resolveUniqueAnchor(
                targetWorld.getSeed(),
                targetWorld,
                targetWorld.getChunkManager().getChunkGenerator(),
                targetWorld.getChunkManager().getChunkGenerator().getBiomeSource(),
                targetWorld.getChunkManager().getNoiseConfig(),
                targetWorld
        );
    }

    /**
     * Resuelve de forma determinista la ÚNICA ubicación de La Casa de José Torres para la semilla del mundo:
     * busca lejos del spawn (~4,500 a 9,500 bloques) un bioma con tierra firme libre de nieve.
     */
    public static HouseAnchor resolveUniqueAnchor(
            long seed,
            @Nullable ServerWorld overworld,
            ChunkGenerator chunkGenerator,
            BiomeSource biomeSource,
            NoiseConfig noiseConfig,
            HeightLimitView heightLimit
    ) {
        HouseAnchor cached = SEED_ANCHOR_CACHE.get(seed);
        if (cached != null) {
            return cached;
        }

        synchronized (ANCHOR_LOCK) {
            cached = SEED_ANCHOR_CACHE.get(seed);
            if (cached != null) {
                return cached;
            }

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                CasaJoseTorresWorldState state = CasaJoseTorresWorldState.get(overworld);
                if (state.isInitialized()) {
                    HouseAnchor fromState = new HouseAnchor(state.getStartChunkX(), state.getBaseY(), state.getStartChunkZ());
                    SEED_ANCHOR_CACHE.put(seed, fromState);
                    return fromState;
                }
            }

            MultiNoiseUtil.MultiNoiseSampler sampler = noiseConfig.getMultiNoiseSampler();
            int seaLevel = chunkGenerator.getSeaLevel();

            ChunkPos chosenChunk = null;

            // 1. Buscar en anillos lejanos de RandomSpreadStructurePlacement (entre ~4,200 y ~14,000 bloques del origen)
            // para que sea súper difícil de encontrar y coincida con la rejilla de estructuras.
            outerSearch:
            for (int ring = 1; ring <= 4; ring++) {
                for (int rx = -ring; rx <= ring; rx++) {
                    for (int rz = -ring; rz <= ring; rz++) {
                        if (Math.abs(rx) != ring && Math.abs(rz) != ring) {
                            continue;
                        }
                        ChunkPos candidate = PLACEMENT.getStartChunk(seed, rx * 320, rz * 320);
                        long blockDistSq = (long) candidate.getCenterX() * candidate.getCenterX()
                                + (long) candidate.getCenterZ() * candidate.getCenterZ();
                        if (blockDistSq < 4200L * 4200L) {
                            continue; // Garantizar que esté muy lejos del spawn (>= 4,200 bloques)
                        }
                        if (isValidSnowyEstateSite(biomeSource, sampler, candidate.getCenterX(), candidate.getCenterZ())) {
                            chosenChunk = candidate;
                            break outerSearch;
                        }
                    }
                }
            }

            // 2. Respaldo determinista: escanear en espiral a partir de 4,800 bloques hasta hallar un bioma terrestre amplio sin nieve
            if (chosenChunk == null) {
                outerFallback:
                for (int distBlocks = 4800; distBlocks <= 16000; distBlocks += 320) {
                    int steps = 36;
                    for (int i = 0; i < steps; i++) {
                        double angle = (2.0 * Math.PI * i) / steps + ((seed & 0xFF) * 0.05);
                        int wx = (int) Math.round(Math.cos(angle) * distBlocks);
                        int wz = (int) Math.round(Math.sin(angle) * distBlocks);
                        int cx = wx >> 4;
                        int cz = wz >> 4;
                        if (isValidSnowyEstateSite(biomeSource, sampler, (cx << 4) + 8, (cz << 4) + 8)) {
                            chosenChunk = new ChunkPos(cx, cz);
                            break outerFallback;
                        }
                    }
                }
            }

            // 3. Último respaldo si la semilla no encontrara bioma terrestre cercano
            if (chosenChunk == null) {
                chosenChunk = PLACEMENT.getStartChunk(seed, 320, -320);
            }

            int centerX = chosenChunk.getCenterX();
            int centerZ = chosenChunk.getCenterZ();

            // Calcular la altura mediana del terreno para asentar la propiedad con el menor movimiento de tierra posible
            int[] heights = new int[25];
            int idx = 0;
            for (int ox = -96; ox <= 96; ox += 48) {
                for (int oz = -96; oz <= 96; oz += 48) {
                    int h = chunkGenerator.getHeightInGround(
                            centerX + ox,
                            centerZ + oz,
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            heightLimit,
                            noiseConfig
                    );
                    heights[idx++] = h;
                }
            }
            Arrays.sort(heights);
            int medianY = Math.max(heights[heights.length / 2], seaLevel + 4);
            int baseY = Math.min(Math.max(medianY, 66), 112);

            HouseAnchor resolved = new HouseAnchor(chosenChunk.x, baseY, chosenChunk.z);
            SEED_ANCHOR_CACHE.put(seed, resolved);

            if (overworld != null && overworld.getRegistryKey() == World.OVERWORLD) {
                CasaJoseTorresWorldState.get(overworld).setAnchor(resolved.startChunkX(), resolved.baseY(), resolved.startChunkZ());
            }
            return resolved;
        }
    }

    /**
     * Verifica que el centro y sus alrededores estén en cualquier bioma terrestre firme sin nieve (no océano, río ni playa).
     */
    public static boolean isValidSnowyEstateSite(
            BiomeSource biomeSource,
            MultiNoiseUtil.MultiNoiseSampler sampler,
            int centerX,
            int centerZ
    ) {
        RegistryEntry<Biome> centerBiome = biomeSource.getBiome(centerX >> 2, 72 >> 2, centerZ >> 2, sampler);
        if (!isSnowyLandBiome(centerBiome, new BlockPos(centerX, 72, centerZ))) {
            return false;
        }

        int validLandMatches = 0;
        int totalSamples = 0;
        for (int ox = -140; ox <= 140; ox += 70) {
            for (int oz = -140; oz <= 140; oz += 70) {
                totalSamples++;
                int sx = centerX + ox;
                int sz = centerZ + oz;
                RegistryEntry<Biome> b = biomeSource.getBiome(sx >> 2, 72 >> 2, sz >> 2, sampler);
                if (b.isIn(BiomeTags.IS_OCEAN) || b.isIn(BiomeTags.IS_DEEP_OCEAN)) {
                    return false;
                }
                if (isSnowyLandBiome(b, new BlockPos(sx, 72, sz))) {
                    validLandMatches++;
                }
            }
        }
        return validLandMatches >= (totalSamples * 3) / 5;
    }

    public static boolean isSnowyLandBiome(RegistryEntry<Biome> biomeEntry, BlockPos pos) {
        if (biomeEntry.isIn(BiomeTags.IS_OCEAN)
                || biomeEntry.isIn(BiomeTags.IS_DEEP_OCEAN)
                || biomeEntry.isIn(BiomeTags.IS_RIVER)
                || biomeEntry.isIn(BiomeTags.IS_BEACH)) {
            return false;
        }
        Optional<RegistryKey<Biome>> keyOpt = biomeEntry.getKey();
        if (keyOpt.isPresent()) {
            RegistryKey<Biome> key = keyOpt.get();
            if (key == BiomeKeys.SNOWY_PLAINS
                    || key == BiomeKeys.SNOWY_TAIGA
                    || key == BiomeKeys.ICE_SPIKES
                    || key == BiomeKeys.GROVE
                    || key == BiomeKeys.SNOWY_SLOPES
                    || key == BiomeKeys.FROZEN_PEAKS
                    || key == BiomeKeys.JAGGED_PEAKS
                    || key == BiomeKeys.SNOWY_BEACH) {
                return false;
            }
        }
        Biome biome = biomeEntry.value();
        return !biome.isCold(pos, 64);
    }

    /**
     * Comprueba en O(1) si un jugador se encuentra dentro de los 420x420 bloques de "La Casa de José Torres".
     */
    public static boolean isPosInCasaJoseTorres(ServerWorldAccess worldAccess, BlockPos pos) {
        ServerWorld serverWorld = worldAccess.toServerWorld();
        if (serverWorld.getRegistryKey() != World.OVERWORLD) {
            return false;
        }
        HouseAnchor anchor = getOrResolveUniqueAnchor(serverWorld);
        if (anchor == null) {
            return false;
        }
        int dx = pos.getX() - anchor.centerX();
        int dz = pos.getZ() - anchor.centerZ();
        return Math.abs(dx) <= CasaJoseTorresPiece.ESTATE_RADIUS
                && Math.abs(dz) <= CasaJoseTorresPiece.ESTATE_RADIUS;
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        HouseAnchor anchor = resolveUniqueAnchor(
                context.seed(),
                null,
                context.chunkGenerator(),
                context.biomeSource(),
                context.noiseConfig(),
                context.world()
        );

        if (chunkPos.x != anchor.startChunkX() || chunkPos.z != anchor.startChunkZ()) {
            return Optional.empty();
        }

        BlockPos pos = new BlockPos(anchor.centerX(), anchor.baseY(), anchor.centerZ());
        return Optional.of(new StructurePosition(pos, collector -> {
            collector.addPiece(new CasaJoseTorresPiece(anchor.centerX(), anchor.baseY(), anchor.centerZ()));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.CASA_JOSE_TORRES_TYPE;
    }
}
