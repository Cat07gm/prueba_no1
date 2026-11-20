package pruebano1.modid.world;

import net.minecraft.block.*;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import pruebano1.modid.ModBlocks;

/**
 * Generador colosal (480x480 bloques) de la 4.ª gran Dungeon: "El Palacio Nacional de México".
 *
 * Incluye:
 * - Nivelación suave sin cortes bruscos ni terreno o vegetación flotante (reglas idénticas y pulidas de La Casa Blanca).
 * - Gran Explanada de la Plaza de la Constitución (El Zócalo) al frente:
 *     * Asta Bandera Monumental de 58 bloques con gigantesca Bandera de México 3D ondeando y Escudo Nacional.
 *     * Área Especial de Combate del Mini-Jefe justo enfrente del Palacio Nacional (con mosaico de la Piedra del Sol al ras del piso).
 *     * Circuito vehicular perimetral con pasos peatonales, semáforos, Turibús, taxis, entradas del Metro Zócalo, fuentes y jardines.
 * - Fachada monumental de 233 bloques de ancho fiel al Palacio Nacional:
 *     * Basamento y 1.er cuerpo de cantera ocre/dorada con ventanas enrejadas.
 *     * 2.º cuerpo de tezontle rojo oscuro con balcones corridos de herrería y sus icónicos toldos rojos en cada ventana.
 *     * 3.er cuerpo con galería corrida de arcos de medio punto en cantera gris, cornisa, pretil y pináculos.
 *     * Las 3 grandes portadas barrocas (Puerta Mariana, Puerta Central con Balcón Presidencial y Campana de Dolores, y Puerta de Honor) + 2 torreones esquineros.
 * - Interior de 3 pisos lujosamente decorado e iluminado + Gran Patio Central / Salón Magno (101x81 bloques y domo a 35 bloques de altura)
 *   libre de obstáculos centrales para el combate contra el Jefe Final de la dungeon.
 */
public class PalacioNacionalPiece extends StructurePiece {

    /** Radio cuadrado interior plano que cubre rejas perimetrales (±184) y jardines del Zócalo. */
    public static final int FLAT_BOX_HALF = 184;
    /** Radio interior total de la propiedad (200 bloques = 400x400 bloques). */
    public static final int ESTATE_RADIUS = 200;
    /** Franja perimetral de 40 bloques para talud suave sin cortes bruscos (total 480x480 bloques). */
    public static final int GRADE_RANGE = 40;
    public static final int TOTAL_RADIUS = ESTATE_RADIUS + GRADE_RANGE; // 240 -> 480x480 bloques
    /** Radio de despeje de árboles salvajes. */
    public static final int TREE_CLEAR_RADIUS = ESTATE_RADIUS + 12; // 212
    /** Radio de bloqueo con placeholder GREEN_TERRACOTTA para evitar troncos cuyo follaje invada el perímetro. */
    public static final int TREE_BLOCKER_RADIUS = ESTATE_RADIUS + 20; // 220

    /** Posición relativa del centro de combate del Mini-Jefe en la explanada del Zócalo frente al Palacio Nacional. */
    public static final int MINI_BOSS_SPAWN_REL_X = 0;
    public static final int MINI_BOSS_SPAWN_REL_Y = 1;
    public static final int MINI_BOSS_SPAWN_REL_Z = 46;

    /** Posición relativa del Jefe Final sobre el Gran Estrado dentro del Patio Central / Salón Magno. */
    public static final int FINAL_BOSS_SPAWN_REL_X = 0;
    public static final int FINAL_BOSS_SPAWN_REL_Y = 3;
    public static final int FINAL_BOSS_SPAWN_REL_Z = -114;

    // Límites del edificio del Palacio Nacional (233 bloques de ancho x 141 bloques de fondo)
    private static final int PALACE_MIN_X = -116;
    private static final int PALACE_MAX_X = 116;
    private static final int PALACE_MIN_Z = -156;
    private static final int PALACE_MAX_Z = -16;

    private static final int CLEAR_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;

    // Árboles ornamentales propios en las alamedas laterales y jardines perimetrales
    private static final int[][] MONUMENTAL_TREES = {
            {-166, 20}, {-166, 52}, {-166, 84}, {-166, 116}, {-166, 148},
            {166, 20}, {166, 52}, {166, 84}, {166, 116}, {166, 148},
            {-166, -40}, {-166, -80}, {-166, -120},
            {166, -40}, {166, -80}, {166, -120},
            {-90, -172}, {-45, -172}, {45, -172}, {90, -172},
            {-90, 170}, {-45, 170}, {45, 170}, {90, 170}
    };

    private final int centerX;
    private final int baseY;
    private final int centerZ;

    public PalacioNacionalPiece(int centerX, int baseY, int centerZ) {
        super(
                ModStructures.PALACIO_NACIONAL_PIECE,
                0,
                new BlockBox(
                        centerX - 180,
                        baseY - 24,
                        centerZ - 180,
                        centerX + 180,
                        baseY + 90,
                        centerZ + 180
                )
        );
        this.centerX = centerX;
        this.baseY = baseY;
        this.centerZ = centerZ;
        this.setOrientation(Direction.SOUTH);
    }

    public PalacioNacionalPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.PALACIO_NACIONAL_PIECE, nbt);
        this.centerX = nbt.getInt("CenterX");
        this.baseY = nbt.getInt("BaseY");
        this.centerZ = nbt.getInt("CenterZ");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("CenterX", this.centerX);
        nbt.putInt("BaseY", this.baseY);
        nbt.putInt("CenterZ", this.centerZ);
    }

    @Override
    public void generate(
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator,
            Random random,
            BlockBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        // Generado íntegramente por generateChunkForPalacio desde PalacioNacionalFeature
    }

    public static void generateChunkForPalacio(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random,
            boolean useTreeBlockerPlaceholder
    ) {
        // 1. Nivelación de los 480x480 bloques y transición suave sin cortes bruscos ni terreno flotante
        prepareEstateTerrainAndBlending(world, chunkBox, centerX, baseY, centerZ, useTreeBlockerPlaceholder);

        // 2. Explanada del Zócalo, Asta Bandera Monumental con Bandera 3D, Arena del Mini-Jefe, Avenidas, Metro y Jardines
        buildZocaloPlazaAndGrounds(world, chunkBox, centerX, baseY, centerZ, random, useTreeBlockerPlaceholder);

        // 3. Casco arquitectónico colosal del Palacio Nacional (Cantera + Tezontle + Toldos Rojos + Arcada Superior + 3 Portadas + Campana de Dolores)
        buildPalacioExteriorShellAndPortals(world, chunkBox, centerX, baseY, centerZ);

        // 4. Interior lujosamente decorado de 3 plantas + Gran Patio Central / Salón Magno para el Jefe Final
        buildPalacioInteriorAndFinalBossArena(world, chunkBox, centerX, baseY, centerZ, random);

        // 5. Árboles ornamentales en alamedas perimetrales y patios interiores
        buildPalacioTrees(world, chunkBox, centerX, baseY, centerZ);

        // 6. Aplicar cualquier modificación personalizada guardada con "/palacionacional guardar"
        if (!useTreeBlockerPlaceholder) {
            PalacioNacionalCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
        }
    }

    public static void upgradeChunkInPlace(
            ServerWorld world,
            int chunkX,
            int chunkZ,
            PalacioNacionalStructure.PalaceAnchor anchor
    ) {
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
        generateChunkForPalacio(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ(),
                world.getRandom(),
                false
        );
        postCleanPalacioChunk(world, chunkBox, anchor.centerX(), anchor.baseY(), anchor.centerZ());
    }

    // =========================================================================
    // 1. TERRENO Y TRANSICIÓN SUAVE SIN CORTES BRUSCOS NI TERRENO FLOTANTE
    // =========================================================================

    public static double getEffectiveEstateDistance(int dx, int dz) {
        int qx = Math.max(0, Math.abs(dx) - FLAT_BOX_HALF);
        int qz = Math.max(0, Math.abs(dz) - FLAT_BOX_HALF);
        double cornerDist = Math.sqrt((double) qx * qx + (double) qz * qz);
        return Math.max(Math.max(Math.abs(dx), Math.abs(dz)), FLAT_BOX_HALF + cornerDist);
    }

    private static void prepareEstateTerrainAndBlending(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            boolean useTreeBlockerPlaceholder
    ) {
        int minX = Math.max(centerX - TOTAL_RADIUS, chunkBox.getMinX());
        int maxX = Math.min(centerX + TOTAL_RADIUS, chunkBox.getMaxX());
        int minZ = Math.max(centerZ - TOTAL_RADIUS, chunkBox.getMinZ());
        int maxZ = Math.min(centerZ + TOTAL_RADIUS, chunkBox.getMaxZ());

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int bottomY = Math.max(world.getBottomY() + 1, baseY - 26);
        int worldTopY = world.getTopYInclusive();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        BlockState air = Blocks.AIR.getDefaultState();
        BlockState stoneFoundation = Blocks.STONE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState treeBlocker = Blocks.GREEN_TERRACOTTA.getDefaultState();

        int cachedChunkX = Integer.MIN_VALUE;
        int cachedChunkZ = Integer.MIN_VALUE;
        int cachedChunkTopClearY = Math.min(worldTopY, baseY + 66);

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            int chunkX = wx >> 4;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                int chunkZ = wz >> 4;
                if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
                    cachedChunkX = chunkX;
                    cachedChunkZ = chunkZ;
                    cachedChunkTopClearY = getChunkMaxClearY(world, world.getChunk(chunkX, chunkZ), baseY);
                }
                int topClearY = cachedChunkTopClearY;

                double effectiveDist = getEffectiveEstateDistance(dx, dz);

                if (effectiveDist <= ESTATE_RADIUS) {
                    for (int y = bottomY; y < baseY - 3; y++) {
                        pos.set(wx, y, wz);
                        BlockState cur = world.getBlockState(pos);
                        if (cur.isAir() || cur.isLiquid() || cur.isReplaceable()) {
                            world.setBlockState(pos, stoneFoundation, Block.NOTIFY_LISTENERS);
                        }
                    }
                    for (int y = baseY - 3; y < baseY; y++) {
                        pos.set(wx, y, wz);
                        world.setBlockState(pos, dirt, Block.NOTIFY_LISTENERS);
                    }

                    pos.set(wx, baseY, wz);
                    BlockState groundBlock = useTreeBlockerPlaceholder ? treeBlocker : grass;
                    world.setBlockState(pos, groundBlock, Block.NOTIFY_LISTENERS);

                    for (int y = baseY + 1; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        if (!world.getBlockState(pos).isAir()) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                    continue;
                }

                if (effectiveDist > TOTAL_RADIUS) {
                    continue;
                }

                double distOut = effectiveDist - ESTATE_RADIUS;
                int capY = Math.min(worldTopY, baseY + (int) Math.floor((distOut * distOut) / 15.0D + distOut * 0.55D));
                int solidTopY = findSolidGroundTopY(world, pos, wx, wz, bottomY, topClearY, baseY - 10);
                int surfaceY = solidTopY;

                if (solidTopY > capY) {
                    for (int y = topClearY; y > capY; y--) {
                        pos.set(wx, y, wz);
                        if (!world.getBlockState(pos).isAir()) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                    if (capY >= bottomY) {
                        BlockState surfaceBlock = (useTreeBlockerPlaceholder && effectiveDist <= TREE_BLOCKER_RADIUS) ? treeBlocker : grass;
                        world.setBlockState(pos.set(wx, capY, wz), surfaceBlock, Block.NOTIFY_LISTENERS);
                        if (capY - 1 >= bottomY) {
                            world.setBlockState(pos.set(wx, capY - 1, wz), dirt, Block.NOTIFY_LISTENERS);
                        }
                    }
                    surfaceY = capY;
                } else {
                    for (int y = topClearY; y > solidTopY; y--) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (!st.isAir()
                                && !st.isLiquid()
                                && st.getFluidState().isEmpty()
                                && !st.isIn(BlockTags.LEAVES)
                                && !st.isIn(BlockTags.LOGS)
                                && !isWildVegetationBlock(st)
                                && !isVineBlock(st)
                                && !st.isOf(Blocks.SNOW)
                                && !st.isOf(Blocks.SNOW_BLOCK)) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                }

                int slopeTargetY = baseY - (int) Math.ceil(distOut * 0.62D);
                pos.set(wx, slopeTargetY, wz);
                BlockState atSlope = world.getBlockState(pos);
                if (atSlope.isAir() || atSlope.isLiquid() || atSlope.isReplaceable()) {
                    for (int y = slopeTargetY; y >= Math.max(bottomY, slopeTargetY - 24); y--) {
                        pos.set(wx, y, wz);
                        BlockState cur = world.getBlockState(pos);
                        if (!cur.isAir() && !cur.isLiquid() && !cur.isReplaceable()) {
                            break;
                        }
                        if (y == slopeTargetY) {
                            BlockState surfaceBlock = (useTreeBlockerPlaceholder && effectiveDist <= TREE_BLOCKER_RADIUS) ? treeBlocker : grass;
                            world.setBlockState(pos, surfaceBlock, Block.NOTIFY_LISTENERS);
                        } else if (y >= slopeTargetY - 3) {
                            world.setBlockState(pos, dirt, Block.NOTIFY_LISTENERS);
                        } else {
                            world.setBlockState(pos, stoneFoundation, Block.NOTIFY_LISTENERS);
                        }
                    }
                    if (slopeTargetY > surfaceY) {
                        surfaceY = slopeTargetY;
                    }
                }

                if (effectiveDist <= TREE_BLOCKER_RADIUS) {
                    for (int y = Math.max(bottomY, surfaceY + 1); y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (isVineBlock(st) || st.isOf(Blocks.SNOW) || st.isOf(Blocks.SNOW_BLOCK)
                                || (effectiveDist <= TREE_CLEAR_RADIUS && isWildVegetationBlock(st))) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                    if (useTreeBlockerPlaceholder && surfaceY >= bottomY) {
                        pos.set(wx, surfaceY, wz);
                        BlockState surfSt = world.getBlockState(pos);
                        boolean waterAbove = surfaceY + 1 <= worldTopY
                                && world.getFluidState(pos.set(wx, surfaceY + 1, wz)).isIn(FluidTags.WATER);
                        if (!waterAbove && !surfSt.isAir() && !surfSt.isLiquid() && surfSt.getFluidState().isEmpty() && !surfSt.isReplaceable()) {
                            world.setBlockState(pos.set(wx, surfaceY, wz), treeBlocker, Block.NOTIFY_LISTENERS);
                        }
                    }
                } else {
                    for (int y = Math.max(bottomY, surfaceY + 1); y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (isVineBlock(st)) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                }
            }
        }
    }

    private static int findSolidGroundTopY(
            StructureWorldAccess world,
            BlockPos.Mutable pos,
            int wx,
            int wz,
            int bottomY,
            int topY,
            int fallbackY
    ) {
        for (int y = topY; y >= bottomY; y--) {
            BlockState st = world.getBlockState(pos.set(wx, y, wz));
            if (isGroundScanIgnored(st)) {
                continue;
            }
            int airBelow = 0;
            boolean floating = false;
            int checkFloor = Math.max(bottomY, y - 14);
            for (int cy = y - 1; cy >= checkFloor; cy--) {
                BlockState sub = world.getBlockState(pos.set(wx, cy, wz));
                if (sub.isAir() || sub.isIn(BlockTags.LEAVES) || isVineBlock(sub) || sub.isOf(Blocks.SNOW)) {
                    airBelow++;
                    if (airBelow >= 4) {
                        floating = true;
                        break;
                    }
                } else if (!isGroundScanIgnored(sub)) {
                    airBelow = 0;
                }
            }
            if (!floating) {
                return y;
            }
        }
        return fallbackY;
    }

    private static boolean isGroundScanIgnored(BlockState st) {
        return st.isAir()
                || st.isLiquid()
                || !st.getFluidState().isEmpty()
                || st.isReplaceable()
                || st.isIn(BlockTags.LEAVES)
                || st.isIn(BlockTags.LOGS)
                || st.isOf(Blocks.SNOW)
                || st.isOf(Blocks.SNOW_BLOCK)
                || isVineBlock(st)
                || isWildVegetationBlock(st);
    }

    // =========================================================================
    // 2. PLAZA DE LA CONSTITUCIÓN (ZÓCALO), ASTA BANDERA, ARENA MINI-JEFE Y AVENIDAS
    // =========================================================================

    private static void buildZocaloPlazaAndGrounds(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random,
            boolean useTreeBlockerPlaceholder
    ) {
        BlockState andesite = Blocks.ANDESITE.getDefaultState();
        BlockState polAndesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState tuff = Blocks.TUFF.getDefaultState();
        BlockState chiseledTuff = Blocks.CHISELED_TUFF.getDefaultState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState sandstone = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState cutSandstone = Blocks.CUT_SANDSTONE.getDefaultState();
        BlockState asphalt = Blocks.GRAY_CONCRETE.getDefaultState();
        BlockState darkAsphalt = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState whiteLine = Blocks.WHITE_CONCRETE.getDefaultState();
        BlockState yellowCurb = Blocks.YELLOW_TERRACOTTA.getDefaultState();
        BlockState ironBars = Blocks.IRON_BARS.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState ochreLight = Blocks.OCHRE_FROGLIGHT.getDefaultState();

        int fenceRadius = 184;

        for (int wx = chunkBox.getMinX(); wx <= chunkBox.getMaxX(); wx++) {
            int dx = wx - centerX;
            for (int wz = chunkBox.getMinZ(); wz <= chunkBox.getMaxZ(); wz++) {
                int dz = wz - centerZ;

                if (Math.abs(dx) > fenceRadius + 4 || Math.abs(dz) > fenceRadius + 4) {
                    continue;
                }

                // A. Reja perimetral histórica de cantera y hierro forjado (radio 184)
                boolean onFenceX = Math.abs(dx) == fenceRadius && Math.abs(dz) <= fenceRadius;
                boolean onFenceZ = Math.abs(dz) == fenceRadius && Math.abs(dx) <= fenceRadius;
                boolean isSouthGate = dz == fenceRadius && Math.abs(dx) <= 14;
                boolean isNorthGate = dz == -fenceRadius && Math.abs(dx) <= 12;
                boolean isWestGate = dx == -fenceRadius && Math.abs(dz - 40) <= 10;
                boolean isEastGate = dx == fenceRadius && Math.abs(dz - 40) <= 10;

                if ((onFenceX || onFenceZ) && !isSouthGate && !isNorthGate && !isWestGate && !isEastGate) {
                    boolean isPillar = (dx % 8 == 0) || (dz % 8 == 0);
                    if (isPillar) {
                        setBlock(world, chunkBox, wx, baseY + 1, wz, polAndesite);
                        setBlock(world, chunkBox, wx, baseY + 2, wz, cutSandstone);
                        setBlock(world, chunkBox, wx, baseY + 3, wz, sandstone);
                        setBlock(world, chunkBox, wx, baseY + 4, wz, cutSandstone);
                        setBlock(world, chunkBox, wx, baseY + 5, wz, chiseledTuff);
                        setBlock(world, chunkBox, wx, baseY + 6, wz, ((dx + dz) % 16 == 0) ? ochreLight : Blocks.SMOOTH_SANDSTONE_SLAB.getDefaultState());
                    } else {
                        setBlock(world, chunkBox, wx, baseY + 1, wz, polAndesite);
                        setBlock(world, chunkBox, wx, baseY + 2, wz, sandstone);
                        setBlock(world, chunkBox, wx, baseY + 3, wz, ironBars);
                        setBlock(world, chunkBox, wx, baseY + 4, wz, ironBars);
                        setBlock(world, chunkBox, wx, baseY + 5, wz, ironBars);
                    }
                }

                // Evitar sobreescribir el interior del edificio del Palacio Nacional
                if (dx >= PALACE_MIN_X && dx <= PALACE_MAX_X && dz >= PALACE_MIN_Z && dz <= PALACE_MAX_Z) {
                    continue;
                }

                // B. Banqueta Real frente y alrededor del Palacio Nacional
                boolean inFrontSidewalk = dz >= -15 && dz <= -4 && Math.abs(dx) <= 134;
                boolean inSidePalaceWalk = dz >= -164 && dz <= -4 && ((dx >= -134 && dx <= -117) || (dx >= 117 && dx <= 134));
                boolean inRearPalacePlaza = dz >= -168 && dz <= -157 && Math.abs(dx) <= 134;

                if (inFrontSidewalk || inSidePalaceWalk || inRearPalacePlaza) {
                    boolean gridLine = (Math.floorMod(dx, 6) == 0) || (Math.floorMod(dz, 6) == 0);
                    BlockState walkTile = gridLine ? polAndesite : (((dx + dz) & 1) == 0 ? sandstone : cutSandstone);
                    if (inFrontSidewalk && Math.abs(dx) <= 5) {
                        // Alfombra ceremonial de piedra roja y dorada frente a la Puerta Central
                        walkTile = Math.abs(dx) == 5 ? Blocks.GOLD_BLOCK.getDefaultState() : Blocks.RED_NETHER_BRICKS.getDefaultState();
                    }
                    setBlock(world, chunkBox, wx, baseY, wz, walkTile);
                    // Bolardos de hierro en el borde de la banqueta frente a la avenida (dejando libre el paso central)
                    if (dz == -4 && Math.abs(dx) >= 18 && Math.abs(dx) <= 128 && dx % 6 == 0) {
                        setBlock(world, chunkBox, wx, baseY + 1, wz, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
                    }
                    continue;
                }

                // C. Circuito vehicular Plaza de la Constitución (como en la fotografía)
                boolean inNorthAve = dz >= -3 && dz <= 9 && Math.abs(dx) <= 148;
                boolean inSouthAve = dz >= 146 && dz <= 158 && Math.abs(dx) <= 148;
                boolean inWestAve = dx >= -148 && dx <= -135 && dz >= -3 && dz <= 158;
                boolean inEastAve = dx >= 135 && dx <= 148 && dz >= -3 && dz <= 158;
                boolean inSouthGateApproach = dz >= 159 && dz <= 186 && Math.abs(dx) <= 14;

                if (inNorthAve || inSouthAve || inWestAve || inEastAve || inSouthGateApproach) {
                    BlockState road = ((dx * 31 + dz * 17) % 11 == 0) ? darkAsphalt : asphalt;

                    // Gran paso peatonal trapezoidal/cebra frente a la Puerta Central (tal como en la foto)
                    if (inNorthAve && Math.abs(dx) <= 16) {
                        road = (Math.floorMod(dz, 2) == 0) ? whiteLine : polAndesite;
                    } else if (inNorthAve && (Math.abs(dx - 68) <= 6 || Math.abs(dx + 68) <= 6)) {
                        road = (Math.floorMod(dz, 2) == 0) ? whiteLine : asphalt;
                    } else if ((inWestAve || inEastAve) && (Math.abs(dz - 14) <= 4 || Math.abs(dz - 140) <= 4)) {
                        road = (Math.floorMod(dx, 2) == 0) ? whiteLine : asphalt;
                    } else if (inNorthAve && dz == 3 && Math.floorMod(dx, 8) < 4) {
                        road = whiteLine;
                    } else if (inSouthAve && dz == 152 && Math.floorMod(dx, 8) < 4) {
                        road = whiteLine;
                    } else if ((inWestAve && dx == -141 || inEastAve && dx == 141) && Math.floorMod(dz, 8) < 4) {
                        road = whiteLine;
                    } else if ((inNorthAve && (dz == -3 || dz == 9)) || (inSouthAve && (dz == 146 || dz == 158))) {
                        road = yellowCurb;
                    }
                    setBlock(world, chunkBox, wx, baseY, wz, road);
                    continue;
                }

                // D. GRAN EXPLANADA DE LA PLAZA DE LA CONSTITUCIÓN (EL ZÓCALO: dx = -134..134, dz = 10..145)
                if (Math.abs(dx) <= 134 && dz >= 10 && dz <= 145) {
                    BlockState zocaloTile = computeZocaloFloorTile(dx, dz, polAndesite, andesite, tuff, smoothStone, chiseledTuff, sandstone, ochreLight, seaLantern);
                    setBlock(world, chunkBox, wx, baseY, wz, zocaloTile);
                    continue;
                }

                // E. Paseos peatonales exteriores hacia las puertas Norte, Este y Oeste
                boolean inNorthGateWalk = Math.abs(dx) <= 10 && dz >= -184 && dz <= -169;
                boolean inWestGateWalk = Math.abs(dz - 40) <= 8 && dx >= -184 && dx <= -149;
                boolean inEastGateWalk = Math.abs(dz - 40) <= 8 && dx >= 149 && dx <= 184;
                boolean inPerimeterPromenade = (Math.abs(dx) >= 149 && Math.abs(dx) <= 155 && dz >= -168 && dz <= 164)
                        || (dz >= 159 && dz <= 165 && Math.abs(dx) <= 155);

                if (inNorthGateWalk || inWestGateWalk || inEastGateWalk || inPerimeterPromenade) {
                    boolean border = (Math.floorMod(dx, 6) == 0) || (Math.floorMod(dz, 6) == 0);
                    setBlock(world, chunkBox, wx, baseY, wz, border ? polAndesite : smoothStone);
                }
            }
        }

        // 1. ASTA BANDERA MONUMENTAL Y GIGANTESCA BANDERA DE MÉXICO EN 3D (dx = 0, dz = +108)
        buildMonumentalMexicanFlagpole(world, chunkBox, centerX, baseY, centerZ + 108);

        // 2. MARCADORES PERIMETRALES DEL ÁREA DEL MINI-JEFE ENFRENTE DEL PALACIO (dx = ±36, dz = +16..+76)
        buildMiniBossPlazaPerimeterMarkers(world, chunkBox, centerX, baseY, centerZ);

        // 3. Portales monumentales Norte/Sur/Este/Oeste y 4 Torreones esquineros de vigilancia
        buildColonialPerimeterGate(world, chunkBox, centerX, baseY, centerZ + fenceRadius, true);
        buildColonialPerimeterGate(world, chunkBox, centerX, baseY, centerZ - fenceRadius, false);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                buildCornerColonialPavilion(world, chunkBox, centerX + sx * fenceRadius, baseY, centerZ + sz * fenceRadius);
            }
        }

        // 4. Dos accesos clásicos a la Estación Metro Zócalo/Tenochtitlan con botín subterráneo (dx = ±96, dz = +82)
        buildMetroZocaloEntrance(world, chunkBox, centerX - 96, baseY, centerZ + 82, random);
        buildMetroZocaloEntrance(world, chunkBox, centerX + 96, baseY, centerZ + 82, random);

        // 5. Fuentes coloniales de talavera en las alamedas Este y Oeste (dx = ±168, dz = -10 y dz = +100)
        buildTalaveraFountain(world, chunkBox, centerX - 168, baseY, centerZ - 10, 9);
        buildTalaveraFountain(world, chunkBox, centerX + 168, baseY, centerZ - 10, 9);
        buildTalaveraFountain(world, chunkBox, centerX - 168, baseY, centerZ + 100, 9);
        buildTalaveraFountain(world, chunkBox, centerX + 168, baseY, centerZ + 100, 9);

        // 6. Semáforos urbanos, Turibús y vehículos en el circuito del Zócalo (como en la foto de referencia)
        buildZocaloTrafficLightsAndVehicles(world, chunkBox, centerX, baseY, centerZ);

        // 7. Farolas históricas de hierro fundido iluminando toda la Plaza de la Constitución y banquetas
        buildZocaloHistoricLighting(world, chunkBox, centerX, baseY, centerZ);
    }

    private static BlockState computeZocaloFloorTile(
            int dx,
            int dz,
            BlockState polAndesite,
            BlockState andesite,
            BlockState tuff,
            BlockState smoothStone,
            BlockState chiseledTuff,
            BlockState sandstone,
            BlockState ochreLight,
            BlockState seaLantern
    ) {
        // Mosaico ceremonial al ras del suelo para el Área de Combate del Mini-Jefe (centro en dx = 0, dz = +46, radio 28)
        int mdx = dx - MINI_BOSS_SPAWN_REL_X;
        int mdz = dz - MINI_BOSS_SPAWN_REL_Z;
        double mDist = Math.sqrt((double) mdx * mdx + (double) mdz * mdz);
        if (mDist <= 28.5D) {
            if (mDist >= 26.8D) {
                return ((mdx + mdz) % 4 == 0) ? ochreLight : Blocks.POLISHED_BLACKSTONE.getDefaultState();
            }
            if (mDist >= 24.8D && mDist < 26.8D) {
                if (mdx < -6) {
                    return Blocks.GREEN_CONCRETE.getDefaultState();
                } else if (mdx > 6) {
                    return Blocks.RED_NETHER_BRICKS.getDefaultState();
                } else {
                    return Blocks.SMOOTH_QUARTZ.getDefaultState();
                }
            }
            if (mDist >= 15.5D && mDist <= 17.0D) {
                return ((mdx * mdx + mdz * mdz) % 5 == 0) ? ochreLight : sandstone;
            }
            if (mDist <= 4.5D) {
                return mDist <= 1.8D ? Blocks.GOLD_BLOCK.getDefaultState() : Blocks.RED_NETHER_BRICKS.getDefaultState();
            }
            // Rayos solares estilo Piedra del Sol
            if ((Math.abs(mdx) <= 1 || Math.abs(mdz) <= 1 || Math.abs(Math.abs(mdx) - Math.abs(mdz)) <= 1) && mDist <= 15.5D) {
                return (Math.round(mDist) % 4 == 0) ? ochreLight : sandstone;
            }
            return ((mdx + mdz) & 1) == 0 ? polAndesite : chiseledTuff;
        }

        // Iluminación empotrada discreta al ras del suelo cada 14 bloques para evitar oscuridad sin estorbar el paso
        if (Math.floorMod(dx, 14) == 0 && Math.floorMod(dz, 14) == 0) {
            return seaLantern;
        }
        // Marcos geométricos coloniales de recinto volcánico cada 14 bloques
        if (Math.floorMod(dx, 14) == 0 || Math.floorMod(dz, 14) == 0) {
            return polAndesite;
        }
        if (Math.abs(dx) >= 131 || dz <= 12 || dz >= 143) {
            return sandstone;
        }
        int hash = Math.floorMod(dx * 19 + dz * 37, 7);
        if (hash == 0) {
            return tuff;
        } else if (hash == 1) {
            return smoothStone;
        }
        return andesite;
    }

    private static void buildMonumentalMexicanFlagpole(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int fx,
            int baseY,
            int fz
    ) {
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState chiseledBlack = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState ironBlock = Blocks.IRON_BLOCK.getDefaultState();
        BlockState wallPole = Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState();

        // Pedestal escalonado octagonal de granito negro y mármol
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 8.2D) {
                    setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, d >= 6.8D ? blackstone : quartz);
                }
                if (d <= 5.5D) {
                    setBlock(world, chunkBox, fx + dx, baseY + 2, fz + dz, d >= 4.2D ? chiseledBlack : blackstone);
                }
                if (d <= 3.0D) {
                    setBlock(world, chunkBox, fx + dx, baseY + 3, fz + dz, chiseledBlack);
                }
                if (Math.abs(d - 7.2D) < 0.6D && (dx == 0 || dz == 0)) {
                    setBlock(world, chunkBox, fx + dx, baseY + 2, fz + dz, Blocks.SEA_LANTERN.getDefaultState());
                }
            }
        }

        // Gran Asta Bandera Monumental de acero oscuro (58 bloques de altura)
        fillBox(world, chunkBox, fx - 1, baseY + 4, fz - 1, fx + 1, baseY + 14, fz + 1, blackstone);
        fillBox(world, chunkBox, fx, baseY + 15, fz, fx, baseY + 38, fz, ironBlock);
        fillBox(world, chunkBox, fx, baseY + 39, fz, fx, baseY + 58, fz, wallPole);
        setBlock(world, chunkBox, fx, baseY + 59, fz, gold);
        setBlock(world, chunkBox, fx, baseY + 60, fz, Blocks.END_ROD.getDefaultState());

        // GIGANTESCA BANDERA DE MÉXICO EN 3D ONDEANDO AL VIENTO (36 bloques de largo x 21 bloques de alto)
        BlockState greenCloth = Blocks.GREEN_WOOL.getDefaultState();
        BlockState whiteCloth = Blocks.WHITE_WOOL.getDefaultState();
        BlockState redCloth = Blocks.RED_WOOL.getDefaultState();
        BlockState eagleBrown = Blocks.BROWN_WOOL.getDefaultState();
        BlockState eagleGold = Blocks.YELLOW_TERRACOTTA.getDefaultState();
        BlockState cactusGreen = Blocks.LIME_TERRACOTTA.getDefaultState();
        BlockState lakeCyan = Blocks.CYAN_TERRACOTTA.getDefaultState();

        int flagMinY = baseY + 36;
        int flagHeight = 21; // ry = 0..20
        int flagLength = 36; // col = 1..36 hacia el Este (+X)

        for (int col = 1; col <= flagLength; col++) {
            int waveZ = (int) Math.round(Math.sin(col * 0.34D) * 1.6D);
            int sagY = col / 14; // Caída natural leve del paño monumental

            for (int row = 0; row < flagHeight; row++) {
                BlockState cloth;
                if (col <= 12) {
                    cloth = greenCloth;
                } else if (col <= 24) {
                    cloth = whiteCloth;
                    // Escudo Nacional (Águila Real sobre nopal devorando la serpiente) en el centro de la franja blanca
                    int ec = col - 18; // -5..+6
                    int er = row - 10; // -10..+10
                    if (er >= -5 && er <= -3 && Math.abs(ec) <= 3) {
                        cloth = (er == -5) ? lakeCyan : cactusGreen;
                    } else if (er >= -2 && er <= 4 && Math.abs(ec) <= 4) {
                        boolean wingShape = (Math.abs(ec) >= 2 && er >= 0 && er <= 4)
                                || (Math.abs(ec) <= 1 && er >= -2 && er <= 3);
                        if (wingShape) {
                            cloth = (ec == 0 && er == 1) ? eagleGold : eagleBrown;
                        } else if (ec == 2 && (er == -1 || er == 0)) {
                            cloth = cactusGreen; // Serpiente en el pico/garra
                        }
                    }
                } else {
                    cloth = redCloth;
                }

                setBlock(world, chunkBox, fx + col, flagMinY + row - sagY, fz + waveZ, cloth);
            }
        }
    }

    private static void buildMiniBossPlazaPerimeterMarkers(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // 6 pebeteros/columnas ceremoniales en el borde exterior (|dx| = 36) dejando el círculo de 70x64 100% despejado
        int[] zPositions = {18, 46, 74};
        for (int side : new int[]{-36, 36}) {
            for (int zp : zPositions) {
                int px = centerX + side;
                int pz = centerZ + zp;
                fillBox(world, chunkBox, px - 1, baseY + 1, pz - 1, px + 1, baseY + 1, pz + 1, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setBlock(world, chunkBox, px, baseY + 2, pz, Blocks.CHISELED_TUFF.getDefaultState());
                setBlock(world, chunkBox, px, baseY + 3, pz, Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
                setBlock(world, chunkBox, px, baseY + 4, pz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
                setBlock(world, chunkBox, px, baseY + 5, pz, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
            }
        }
    }

    private static void buildColonialPerimeterGate(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int gx,
            int baseY,
            int gz,
            boolean isSouthMainGate
    ) {
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState chiluca = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState tezontle = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        Direction bannerDir = isSouthMainGate ? Direction.SOUTH : Direction.NORTH;
        int bannerOffsetZ = isSouthMainGate ? 2 : -2;

        for (int side : new int[]{-15, 15}) {
            fillBox(world, chunkBox, gx + side - 1, baseY + 1, gz - 1, gx + side + 1, baseY + 10, gz + 1, chiluca);
            fillBox(world, chunkBox, gx + side - 1, baseY + 4, gz - 1, gx + side + 1, baseY + 8, gz + 1, tezontle);
            fillBox(world, chunkBox, gx + side - 2, baseY + 11, gz - 2, gx + side + 2, baseY + 11, gz + 2, cantera);
            setBlock(world, chunkBox, gx + side, baseY + 12, gz, gold);
            setBlock(world, chunkBox, gx + side, baseY + 13, gz, Blocks.OCHRE_FROGLIGHT.getDefaultState());

            // Estandartes tricolores mexicanos
            setBlock(world, chunkBox, gx + side - 1, baseY + 8, gz + bannerOffsetZ,
                    Blocks.GREEN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, bannerDir));
            setBlock(world, chunkBox, gx + side, baseY + 8, gz + bannerOffsetZ,
                    Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, bannerDir));
            setBlock(world, chunkBox, gx + side + 1, baseY + 8, gz + bannerOffsetZ,
                    Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, bannerDir));
        }
        fillBox(world, chunkBox, gx - 16, baseY + 10, gz - 1, gx + 16, baseY + 11, gz + 1, chiluca);
        fillBox(world, chunkBox, gx - 6, baseY + 12, gz - 1, gx + 6, baseY + 13, gz + 1, cantera);
        setBlock(world, chunkBox, gx, baseY + 14, gz, gold);
    }

    private static void buildCornerColonialPavilion(StructureWorldAccess world, BlockBox chunkBox, int tx, int baseY, int tz) {
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState tezontle = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState chiluca = Blocks.POLISHED_ANDESITE.getDefaultState();

        fillBox(world, chunkBox, tx - 3, baseY + 1, tz - 3, tx + 3, baseY + 8, tz + 3, tezontle);
        fillBox(world, chunkBox, tx - 2, baseY + 1, tz - 2, tx + 2, baseY + 7, tz + 2, Blocks.AIR.getDefaultState());
        for (int sx : new int[]{-3, 3}) {
            for (int sz : new int[]{-3, 3}) {
                fillBox(world, chunkBox, tx + sx, baseY + 1, tz + sz, tx + sx, baseY + 8, tz + sz, chiluca);
            }
        }
        fillBox(world, chunkBox, tx - 4, baseY + 9, tz - 4, tx + 4, baseY + 9, tz + 4, cantera);
        fillBox(world, chunkBox, tx - 2, baseY + 10, tz - 2, tx + 2, baseY + 10, tz + 2, chiluca);
        setBlock(world, chunkBox, tx, baseY + 11, tz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        setBlock(world, chunkBox, tx, baseY + 7, tz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    private static void buildMetroZocaloEntrance(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int mx,
            int baseY,
            int mz,
            Random random
    ) {
        BlockState polAndesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState orangeSign = Blocks.ORANGE_CONCRETE.getDefaultState();

        // Sala subterránea iluminada del Metro Zócalo con maqueta del Templo Mayor y cofre
        fillBox(world, chunkBox, mx - 6, baseY - 5, mz - 8, mx + 6, baseY - 1, mz + 8, polAndesite);
        fillBox(world, chunkBox, mx - 5, baseY - 4, mz - 7, mx + 5, baseY - 1, mz + 7, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, mx - 5, baseY - 5, mz - 7, mx + 5, baseY - 5, mz + 7, quartz);
        setBlock(world, chunkBox, mx - 4, baseY - 1, mz - 5, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, mx + 4, baseY - 1, mz - 5, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, mx - 4, baseY - 1, mz + 5, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, mx + 4, baseY - 1, mz + 5, Blocks.SEA_LANTERN.getDefaultState());

        // Pequeña maqueta de pirámide de Tenochtitlan en vitrina
        fillBox(world, chunkBox, mx - 1, baseY - 4, mz + 2, mx + 1, baseY - 4, mz + 4, Blocks.SMOOTH_SANDSTONE.getDefaultState());
        setBlock(world, chunkBox, mx, baseY - 3, mz + 3, Blocks.CHISELED_SANDSTONE.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(mx + 4, baseY - 4, mz + 6), Direction.NORTH, LootTables.SIMPLE_DUNGEON_CHEST, random);

        // Barandal de piedra en superficie y escalinata descendente
        fillBox(world, chunkBox, mx - 3, baseY + 1, mz - 6, mx + 3, baseY + 1, mz - 2, polAndesite);
        fillBox(world, chunkBox, mx - 2, baseY, mz - 5, mx + 2, baseY + 1, mz - 2, Blocks.AIR.getDefaultState());
        for (int step = 0; step < 4; step++) {
            int sy = baseY - step;
            int sz = mz - 5 + step;
            fillBox(world, chunkBox, mx - 2, sy, sz, mx + 2, sy, sz,
                    Blocks.ANDESITE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            fillBox(world, chunkBox, mx - 2, sy + 1, sz, mx + 2, baseY + 2, sz, Blocks.AIR.getDefaultState());
        }
        // Tótem naranja icónico del Metro CDMX
        setBlock(world, chunkBox, mx - 3, baseY + 2, mz - 6, polAndesite);
        setBlock(world, chunkBox, mx - 3, baseY + 3, mz - 6, orangeSign);
        setBlock(world, chunkBox, mx - 3, baseY + 4, mz - 6, Blocks.OCHRE_FROGLIGHT.getDefaultState());
    }

    private static void buildTalaveraFountain(StructureWorldAccess world, BlockBox chunkBox, int fx, int baseY, int fz, int radius) {
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState blueTile = Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= radius + 0.2D) {
                    if (d >= radius - 1.3D) {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz, cantera);
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, ((dx + dz) & 1) == 0 ? cantera : blueTile);
                    } else {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz,
                                (dx % 3 == 0 && dz % 3 == 0) ? Blocks.SEA_LANTERN.getDefaultState() : blueTile);
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, water);
                    }
                }
            }
        }
        fillBox(world, chunkBox, fx, baseY + 1, fz, fx, baseY + 6, fz, Blocks.CHISELED_SANDSTONE.getDefaultState());
        fillBox(world, chunkBox, fx - 2, baseY + 4, fz - 2, fx + 2, baseY + 4, fz + 2, cantera);
        setBlock(world, chunkBox, fx, baseY + 7, fz, water);
    }

    private static void buildZocaloTrafficLightsAndVehicles(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Semáforos en las esquinas del circuito vehicular
        int[][] trafficLights = {{-134, 10}, {134, 10}, {-134, 145}, {134, 145}};
        for (int[] tl : trafficLights) {
            int tx = centerX + tl[0];
            int tz = centerZ + tl[1];
            fillBox(world, chunkBox, tx, baseY + 1, tz, tx, baseY + 6, tz, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
            setBlock(world, chunkBox, tx, baseY + 7, tz, Blocks.BLACK_CONCRETE.getDefaultState());
            int armDir = tl[0] > 0 ? 1 : -1;
            setBlock(world, chunkBox, tx + armDir, baseY + 7, tz, Blocks.RED_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, tx + armDir * 2, baseY + 7, tz, Blocks.YELLOW_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, tx + armDir * 3, baseY + 7, tz, Blocks.LIME_CONCRETE.getDefaultState());
        }

        // Autobús amarillo / Turibús en la avenida Este (como en la esquina derecha de la foto)
        buildMexicanBus(world, chunkBox, centerX + 140, baseY + 1, centerZ + 88, Blocks.YELLOW_CONCRETE.getDefaultState());
        buildMexicanBus(world, chunkBox, centerX - 142, baseY + 1, centerZ + 62, Blocks.RED_CONCRETE.getDefaultState());

        // Taxis CDMX (rosa con blanco) y camionetas escolta
        buildCompactCar(world, chunkBox, centerX + 144, baseY + 1, centerZ + 112, Blocks.PINK_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState());
        buildCompactCar(world, chunkBox, centerX + 138, baseY + 1, centerZ + 126, Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.WHITE_CONCRETE.getDefaultState());
        buildCompactCar(world, chunkBox, centerX - 138, baseY + 1, centerZ + 98, Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState());
    }

    private static void buildMexicanBus(StructureWorldAccess world, BlockBox chunkBox, int bx, int by, int bz, BlockState bodyColor) {
        BlockState wheel = Blocks.COAL_BLOCK.getDefaultState();
        BlockState glass = Blocks.TINTED_GLASS.getDefaultState();
        fillBox(world, chunkBox, bx - 2, by, bz - 6, bx + 2, by + 1, bz + 6, bodyColor);
        fillBox(world, chunkBox, bx - 2, by + 2, bz - 5, bx + 2, by + 2, bz + 5, glass);
        fillBox(world, chunkBox, bx - 2, by + 3, bz - 6, bx + 2, by + 3, bz + 6, Blocks.WHITE_CONCRETE.getDefaultState());
        for (int wz : new int[]{-4, 4}) {
            setBlock(world, chunkBox, bx - 2, by, bz + wz, wheel);
            setBlock(world, chunkBox, bx + 2, by, bz + wz, wheel);
        }
        setBlock(world, chunkBox, bx - 1, by, bz - 6, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, bx + 1, by, bz - 6, Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildCompactCar(StructureWorldAccess world, BlockBox chunkBox, int cx, int cy, int cz, BlockState lower, BlockState roof) {
        BlockState wheel = Blocks.COAL_BLOCK.getDefaultState();
        fillBox(world, chunkBox, cx - 1, cy, cz - 3, cx + 1, cy, cz + 3, lower);
        fillBox(world, chunkBox, cx - 1, cy + 1, cz - 1, cx + 1, cy + 1, cz + 1, Blocks.TINTED_GLASS.getDefaultState());
        fillBox(world, chunkBox, cx - 1, cy + 2, cz - 1, cx + 1, cy + 2, cz + 1, roof);
        setBlock(world, chunkBox, cx - 1, cy, cz - 2, wheel);
        setBlock(world, chunkBox, cx + 1, cy, cz - 2, wheel);
        setBlock(world, chunkBox, cx - 1, cy, cz + 2, wheel);
        setBlock(world, chunkBox, cx + 1, cy, cz + 2, wheel);
    }

    private static void buildZocaloHistoricLighting(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Farolas dobles coloniales a lo largo de la banqueta del Palacio Nacional y bordes del Zócalo
        for (int dx = -126; dx <= 126; dx += 18) {
            if (Math.abs(dx) > 8) {
                placeHistoricDoubleLamp(world, chunkBox, centerX + dx, baseY + 1, centerZ - 5);
                placeHistoricDoubleLamp(world, chunkBox, centerX + dx, baseY + 1, centerZ + 11);
                placeHistoricDoubleLamp(world, chunkBox, centerX + dx, baseY + 1, centerZ + 144);
            }
        }
        for (int dz = 24; dz <= 136; dz += 22) {
            placeHistoricDoubleLamp(world, chunkBox, centerX - 133, baseY + 1, centerZ + dz);
            placeHistoricDoubleLamp(world, chunkBox, centerX + 133, baseY + 1, centerZ + dz);
        }
    }

    private static void placeHistoricDoubleLamp(StructureWorldAccess world, BlockBox chunkBox, int lx, int by, int lz) {
        setBlock(world, chunkBox, lx, by, lz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        fillBox(world, chunkBox, lx, by + 1, lz, lx, by + 5, lz, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
        fillBox(world, chunkBox, lx - 1, by + 5, lz, lx + 1, by + 5, lz, Blocks.IRON_BARS.getDefaultState());
        setBlock(world, chunkBox, lx - 1, by + 4, lz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, lx + 1, by + 4, lz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, lx, by + 6, lz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
    }

    // =========================================================================
    // 3. CASCO EXTERIOR MONUMENTAL DEL PALACIO NACIONAL (FIEL A LA FOTO)
    // =========================================================================

    private static void buildPalacioExteriorShellAndPortals(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Si el chunk no intersecta la huella ampliada del Palacio (con toldos y balcones), salir rápido
        if (chunkBox.getMaxX() < centerX + PALACE_MIN_X - 3
                || chunkBox.getMinX() > centerX + PALACE_MAX_X + 3
                || chunkBox.getMaxZ() < centerZ + PALACE_MIN_Z - 3
                || chunkBox.getMinZ() > centerZ + PALACE_MAX_Z + 3) {
            return;
        }

        BlockState plinth = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState canteraSmooth = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState canteraCut = Blocks.CUT_SANDSTONE.getDefaultState();
        BlockState chiluca = Blocks.STONE_BRICKS.getDefaultState();
        BlockState chiseledChiluca = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState polDiorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState tezontle = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState darkTezontle = Blocks.NETHER_BRICKS.getDefaultState();
        BlockState glassPane = Blocks.GLASS_PANE.getDefaultState();
        BlockState ironBars = Blocks.IRON_BARS.getDefaultState();
        BlockState redAwning = Blocks.RED_WOOL.getDefaultState();
        BlockState roofTile = Blocks.TERRACOTTA.getDefaultState();
        BlockState floorMarble = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        // A. Volumen base limpio, pisos estructurales y azotea (dejando hueco del Patio Central y Patios Laterales)
        int minWx = Math.max(centerX + PALACE_MIN_X, chunkBox.getMinX());
        int maxWx = Math.min(centerX + PALACE_MAX_X, chunkBox.getMaxX());
        int minWz = Math.max(centerZ + PALACE_MIN_Z, chunkBox.getMinZ());
        int maxWz = Math.min(centerZ + PALACE_MAX_Z, chunkBox.getMaxZ());

        for (int wx = minWx; wx <= maxWx; wx++) {
            int dx = wx - centerX;
            for (int wz = minWz; wz <= maxWz; wz++) {
                int dz = wz - centerZ;

                boolean inCentralArena = Math.abs(dx) <= 44 && dz >= -121 && dz <= -51;
                boolean inWestCourtyard = dx >= -100 && dx <= -66 && dz >= -98 && dz <= -72;
                boolean inEastCourtyard = dx >= 66 && dx <= 100 && dz >= -98 && dz <= -72;
                boolean inGrandVestibuleVoid = Math.abs(dx) <= 24 && dz >= -43 && dz <= -20;

                // Piso de planta baja
                if (!inWestCourtyard && !inEastCourtyard) {
                    setBlock(world, chunkBox, wx, baseY, wz, (((dx + dz) & 1) == 0) ? floorMarble : polDiorite);
                }
                // Vaciar interior (ry = 1..26)
                for (int ry = 1; ry <= 26; ry++) {
                    setBlock(world, chunkBox, wx, baseY + ry, wz, air);
                }

                // Entrepiso del 2.º Piso (ry = 9..10) y 3.er Piso (ry = 19..20)
                if (!inCentralArena && !inWestCourtyard && !inEastCourtyard && !inGrandVestibuleVoid) {
                    setBlock(world, chunkBox, wx, baseY + 9, wz, Blocks.SMOOTH_STONE.getDefaultState());
                    setBlock(world, chunkBox, wx, baseY + 10, wz, Blocks.DARK_OAK_PLANKS.getDefaultState());
                    setBlock(world, chunkBox, wx, baseY + 19, wz, Blocks.SMOOTH_STONE.getDefaultState());
                    setBlock(world, chunkBox, wx, baseY + 20, wz, polDiorite);
                }

                // Azotea colonial en todo el palacio excepto patios abiertos (el Patio Central lleva su gran domo de cristal)
                if (!inCentralArena && !inWestCourtyard && !inEastCourtyard) {
                    setBlock(world, chunkBox, wx, baseY + 27, wz, roofTile);
                }
            }
        }

        // B. FACHADAS SUR (PRINCIPAL AL ZÓCALO, dz = -16) Y NORTE (TRASERA, dz = -156)
        for (int dx = PALACE_MIN_X; dx <= PALACE_MAX_X; dx++) {
            buildFacadeColumnAlongX(world, chunkBox, centerX, baseY, centerZ, dx, PALACE_MAX_Z, true,
                    plinth, canteraSmooth, canteraCut, chiluca, chiseledChiluca, polDiorite, tezontle, darkTezontle, glassPane, ironBars, redAwning);
            buildFacadeColumnAlongX(world, chunkBox, centerX, baseY, centerZ, dx, PALACE_MIN_Z, false,
                    plinth, canteraSmooth, canteraCut, chiluca, chiseledChiluca, polDiorite, tezontle, darkTezontle, glassPane, ironBars, redAwning);
        }

        // C. FACHADAS LATERALES OESTE (dx = -116) Y ESTE (dx = +116)
        for (int dz = PALACE_MIN_Z + 1; dz <= PALACE_MAX_Z - 1; dz++) {
            buildFacadeColumnAlongZ(world, chunkBox, centerX, baseY, centerZ, PALACE_MIN_X, dz, true,
                    plinth, canteraSmooth, canteraCut, chiluca, polDiorite, tezontle, darkTezontle, glassPane, ironBars, redAwning);
            buildFacadeColumnAlongZ(world, chunkBox, centerX, baseY, centerZ, PALACE_MAX_X, dz, false,
                    plinth, canteraSmooth, canteraCut, chiluca, polDiorite, tezontle, darkTezontle, glassPane, ironBars, redAwning);
        }

        // D. LOS 2 TORREONES ESQUINEROS DEL FRENTE Y ESQUINAS TRASERAS
        buildCornerBastionTower(world, chunkBox, centerX - 110, baseY, centerZ - 21, true);
        buildCornerBastionTower(world, chunkBox, centerX + 110, baseY, centerZ - 21, true);
        buildCornerBastionTower(world, chunkBox, centerX - 110, baseY, centerZ - 151, false);
        buildCornerBastionTower(world, chunkBox, centerX + 110, baseY, centerZ - 151, false);

        // E. LAS 3 GRANDES PORTADAS BARROCAS DE LA FACHADA PRINCIPAL (Fieles a la fotografía)
        // 1) Portada Central + Balcón Presidencial + Campana de Dolores + Asta Central
        buildCentralPresidentialPortal(world, chunkBox, centerX, baseY, centerZ + PALACE_MAX_Z);
        // 2) Puerta Mariana (Izquierda / Oeste, dx = -68)
        buildSideBaroquePortal(world, chunkBox, centerX - 68, baseY, centerZ + PALACE_MAX_Z, true);
        // 3) Puerta de Honor (Derecha / Este, dx = +68)
        buildSideBaroquePortal(world, chunkBox, centerX + 68, baseY, centerZ + PALACE_MAX_Z, true);
        // 4) Portada Norte Trasera (dx = 0, dz = -156)
        buildSideBaroquePortal(world, chunkBox, centerX, baseY, centerZ + PALACE_MIN_Z, false);
    }

    private static void buildFacadeColumnAlongX(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int dx,
            int wallRelZ,
            boolean isSouthFront,
            BlockState plinth,
            BlockState canteraSmooth,
            BlockState canteraCut,
            BlockState chiluca,
            BlockState chiseledChiluca,
            BlockState polDiorite,
            BlockState tezontle,
            BlockState darkTezontle,
            BlockState glassPane,
            BlockState ironBars,
            BlockState redAwning
    ) {
        int wx = centerX + dx;
        int wz = centerZ + wallRelZ;
        if (wx < chunkBox.getMinX() || wx > chunkBox.getMaxX() || wz < chunkBox.getMinZ() - 2 || wz > chunkBox.getMaxZ() + 2) {
            return;
        }

        // Saltar las zonas que serán esculpidas por las Portadas Barrocas y Torreones Esquineros
        if (Math.abs(dx) >= 104) {
            return;
        }
        if (isSouthFront && (Math.abs(dx) <= 11 || Math.abs(dx + 68) <= 8 || Math.abs(dx - 68) <= 8)) {
            return;
        }
        if (!isSouthFront && Math.abs(dx) <= 8) {
            return;
        }

        int outZ = wz + (isSouthFront ? 1 : -1);
        int bayMod = Math.floorMod(dx, 6);
        boolean isPilaster = (bayMod == 0);
        boolean isWindowCenter = (bayMod == 2 || bayMod == 3 || bayMod == 4);
        boolean isWindowInner = (bayMod == 3);

        // 1. Zócalo de recinto y 1.er cuerpo de Cantera Ocre (ry = 1..10)
        setBlock(world, chunkBox, wx, baseY + 1, wz, plinth);
        setBlock(world, chunkBox, wx, baseY + 2, wz, plinth);
        for (int ry = 3; ry <= 10; ry++) {
            BlockState stone = isPilaster ? chiluca : (((dx + ry) % 3 == 0) ? canteraCut : canteraSmooth);
            if ((ry == 4 || ry == 5 || ry == 6) && isWindowInner) {
                stone = glassPane;
                setBlock(world, chunkBox, wx, baseY + ry, outZ, ironBars);
            } else if ((ry == 4 || ry == 5 || ry == 6 || ry == 7) && isWindowCenter) {
                stone = chiluca;
            } else if ((ry == 8 || ry == 9) && isWindowInner) {
                stone = glassPane; // Ventana de entresuelo
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, stone);
        }

        // Cornisa corrida del 2.º piso y balcón de herrería
        setBlock(world, chunkBox, wx, baseY + 11, wz, chiluca);
        setBlock(world, chunkBox, wx, baseY + 11, outZ, Blocks.STONE_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
        if (!isPilaster) {
            setBlock(world, chunkBox, wx, baseY + 12, outZ, ironBars);
        }

        // 2. Segundo cuerpo de Tezontle Rojo con balcones y TOLDOS ROJOS ICÓNICOS (ry = 12..19)
        for (int ry = 12; ry <= 19; ry++) {
            BlockState wall = isPilaster ? chiluca : (((dx + ry) % 4 == 0) ? darkTezontle : tezontle);
            if (ry >= 12 && ry <= 16 && isWindowInner) {
                wall = glassPane;
            } else if (ry >= 12 && ry <= 17 && isWindowCenter) {
                wall = polDiorite;
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, wall);
        }
        // Toldo rojo inclinado sobre cada balcón (ry = 16..17 en outZ)
        if (isWindowCenter) {
            setBlock(world, chunkBox, wx, baseY + 17, outZ, redAwning);
            setBlock(world, chunkBox, wx, baseY + 16, outZ, Blocks.RED_CARPET.getDefaultState());
        }

        // Cornisa del 3.er cuerpo
        setBlock(world, chunkBox, wx, baseY + 20, wz, chiluca);
        setBlock(world, chunkBox, wx, baseY + 20, outZ, Blocks.STONE_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));

        // 3. Tercer cuerpo: Galería Superior de Arcos de Medio Punto (ry = 21..26)
        int archMod = Math.floorMod(dx, 3);
        for (int ry = 21; ry <= 26; ry++) {
            BlockState upper = isPilaster ? chiseledChiluca : tezontle;
            if (!isPilaster) {
                if (archMod == 0) {
                    upper = chiluca;
                } else if (ry >= 22 && ry <= 24) {
                    upper = glassPane;
                } else if (ry == 25) {
                    upper = polDiorite;
                }
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, upper);
        }

        // 4. Gran cornisa superior, pretil calado y pináculos barrocos (ry = 27..30)
        setBlock(world, chunkBox, wx, baseY + 27, wz, chiseledChiluca);
        setBlock(world, chunkBox, wx, baseY + 27, outZ, Blocks.STONE_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
        setBlock(world, chunkBox, wx, baseY + 28, wz, isPilaster ? chiluca : canteraSmooth);
        if (isPilaster) {
            setBlock(world, chunkBox, wx, baseY + 29, wz, chiseledChiluca);
            setBlock(world, chunkBox, wx, baseY + 30, wz, Blocks.COBBLESTONE_WALL.getDefaultState());
        } else if (bayMod == 3) {
            setBlock(world, chunkBox, wx, baseY + 29, wz, Blocks.STONE_BRICK_SLAB.getDefaultState());
        }
    }

    private static void buildFacadeColumnAlongZ(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int wallRelX,
            int dz,
            boolean isWestSide,
            BlockState plinth,
            BlockState canteraSmooth,
            BlockState canteraCut,
            BlockState chiluca,
            BlockState polDiorite,
            BlockState tezontle,
            BlockState darkTezontle,
            BlockState glassPane,
            BlockState ironBars,
            BlockState redAwning
    ) {
        if (dz >= -27 || dz <= -145) {
            return; // Cubierto por los 4 torreones esquineros
        }
        int wx = centerX + wallRelX;
        int wz = centerZ + dz;
        if (wx < chunkBox.getMinX() - 2 || wx > chunkBox.getMaxX() + 2 || wz < chunkBox.getMinZ() || wz > chunkBox.getMaxZ()) {
            return;
        }

        int outX = wx + (isWestSide ? -1 : 1);
        int bayMod = Math.floorMod(dz, 6);
        boolean isPilaster = (bayMod == 0);
        boolean isWindowCenter = (bayMod == 2 || bayMod == 3 || bayMod == 4);
        boolean isWindowInner = (bayMod == 3);

        setBlock(world, chunkBox, wx, baseY + 1, wz, plinth);
        setBlock(world, chunkBox, wx, baseY + 2, wz, plinth);
        for (int ry = 3; ry <= 10; ry++) {
            BlockState stone = isPilaster ? chiluca : (((dz + ry) % 3 == 0) ? canteraCut : canteraSmooth);
            if ((ry == 4 || ry == 5 || ry == 6) && isWindowInner) {
                stone = glassPane;
                setBlock(world, chunkBox, outX, baseY + ry, wz, ironBars);
            } else if ((ry == 4 || ry == 5 || ry == 6 || ry == 7) && isWindowCenter) {
                stone = chiluca;
            } else if ((ry == 8 || ry == 9) && isWindowInner) {
                stone = glassPane;
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, stone);
        }

        setBlock(world, chunkBox, wx, baseY + 11, wz, chiluca);
        setBlock(world, chunkBox, outX, baseY + 11, wz, Blocks.STONE_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
        if (!isPilaster) {
            setBlock(world, chunkBox, outX, baseY + 12, wz, ironBars);
        }

        for (int ry = 12; ry <= 19; ry++) {
            BlockState wall = isPilaster ? chiluca : (((dz + ry) % 4 == 0) ? darkTezontle : tezontle);
            if (ry >= 12 && ry <= 16 && isWindowInner) {
                wall = glassPane;
            } else if (ry >= 12 && ry <= 17 && isWindowCenter) {
                wall = polDiorite;
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, wall);
        }
        if (isWindowCenter) {
            setBlock(world, chunkBox, outX, baseY + 17, wz, redAwning);
            setBlock(world, chunkBox, outX, baseY + 16, wz, Blocks.RED_CARPET.getDefaultState());
        }

        setBlock(world, chunkBox, wx, baseY + 20, wz, chiluca);
        int archMod = Math.floorMod(dz, 3);
        for (int ry = 21; ry <= 26; ry++) {
            BlockState upper = isPilaster ? chiluca : tezontle;
            if (!isPilaster) {
                if (archMod == 0) {
                    upper = chiluca;
                } else if (ry >= 22 && ry <= 24) {
                    upper = glassPane;
                } else if (ry == 25) {
                    upper = polDiorite;
                }
            }
            setBlock(world, chunkBox, wx, baseY + ry, wz, upper);
        }
        setBlock(world, chunkBox, wx, baseY + 27, wz, chiluca);
        setBlock(world, chunkBox, wx, baseY + 28, wz, isPilaster ? chiluca : canteraSmooth);
        if (isPilaster) {
            setBlock(world, chunkBox, wx, baseY + 29, wz, chiluca);
            setBlock(world, chunkBox, wx, baseY + 30, wz, Blocks.COBBLESTONE_WALL.getDefaultState());
        }
    }

    private static void buildCornerBastionTower(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int tx,
            int baseY,
            int tz,
            boolean isFrontTower
    ) {
        BlockState chiluca = Blocks.STONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState tezontle = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState glass = Blocks.GLASS_PANE.getDefaultState();

        // Casco exterior del torreón (13x11 bloques, sobresale ligeramente con sillares de chiluca)
        for (int ox = -6; ox <= 6; ox++) {
            for (int oz = -5; oz <= 5; oz++) {
                boolean onEdge = Math.abs(ox) == 6 || Math.abs(oz) == 5;
                if (!onEdge) {
                    continue;
                }
                boolean isCornerQuoin = Math.abs(ox) >= 5 && Math.abs(oz) >= 4;
                for (int ry = 1; ry <= 31; ry++) {
                    BlockState st;
                    if (ry <= 2 || ry == 11 || ry == 20 || ry == 28 || ry >= 30 || isCornerQuoin) {
                        st = (ry == 28 || ry == 31) ? chiseled : chiluca;
                    } else if (ry <= 10) {
                        st = cantera;
                    } else {
                        st = tezontle;
                    }
                    if (!isCornerQuoin && Math.abs(ox) <= 2 && ((ry >= 4 && ry <= 6) || (ry >= 13 && ry <= 16) || (ry >= 22 && ry <= 25))) {
                        st = glass;
                    }
                    setBlock(world, chunkBox, tx + ox, baseY + ry, tz + oz, st);
                }
            }
        }
        // Techo del torreón y remates barrocos
        fillBox(world, chunkBox, tx - 5, baseY + 29, tz - 4, tx + 5, baseY + 29, tz + 4, chiluca);
        if (isFrontTower) {
            int frontZ = tz + 6;
            fillBox(world, chunkBox, tx - 2, baseY + 17, frontZ, tx + 2, baseY + 17, frontZ, Blocks.RED_WOOL.getDefaultState());
            fillBox(world, chunkBox, tx - 2, baseY + 11, frontZ, tx + 2, baseY + 11, frontZ, Blocks.STONE_BRICK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
            fillBox(world, chunkBox, tx - 2, baseY + 12, frontZ, tx + 2, baseY + 12, frontZ, Blocks.IRON_BARS.getDefaultState());
        }
        for (int sx : new int[]{-6, 0, 6}) {
            for (int sz : new int[]{-5, 5}) {
                setBlock(world, chunkBox, tx + sx, baseY + 32, tz + sz, chiseled);
                setBlock(world, chunkBox, tx + sx, baseY + 33, tz + sz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            }
        }
    }

    /**
     * Portada Central Barroca con Puerta Central abierta, Balcón Presidencial, Escudo en relieve,
     * Nicho de la Campana de Dolores y Asta Central sobre el frontispicio.
     */
    private static void buildCentralPresidentialPortal(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int px,
            int baseY,
            int pz
    ) {
        BlockState chiluca = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState stoneBricks = Blocks.STONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState polDiorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState rawGold = Blocks.RAW_GOLD_BLOCK.getDefaultState();

        // Muro monumental de cantera gris chiluca (dx = -11..+11, ry = 1..30)
        fillBox(world, chunkBox, px - 11, baseY + 1, pz - 1, px + 11, baseY + 30, pz, stoneBricks);
        // Gran Arco de Entrada Abierto (Puerta Central: dx = -4..+4, ry = 1..7)
        fillBox(world, chunkBox, px - 4, baseY + 1, pz - 2, px + 4, baseY + 6, pz + 1, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, px - 3, baseY + 7, pz - 2, px + 3, baseY + 7, pz + 1, Blocks.AIR.getDefaultState());

        // Columnas corintias pareadas flanqueando la Puerta Central y el Balcón Presidencial
        for (int colX : new int[]{-9, -6, 6, 9}) {
            fillBox(world, chunkBox, px + colX, baseY + 1, pz + 1, px + colX, baseY + 27, pz + 1, pillar);
            setBlock(world, chunkBox, px + colX, baseY + 11, pz + 1, chiseled);
            setBlock(world, chunkBox, px + colX, baseY + 20, pz + 1, chiseled);
            setBlock(world, chunkBox, px + colX, baseY + 28, pz + 1, chiseled);
        }

        // BALCÓN PRESIDENCIAL DEL GRITO (ry = 11..17)
        fillBox(world, chunkBox, px - 5, baseY + 11, pz + 1, px + 5, baseY + 11, pz + 2, polDiorite);
        fillBox(world, chunkBox, px - 5, baseY + 12, pz + 2, px + 5, baseY + 12, pz + 2, Blocks.IRON_BARS.getDefaultState());
        // Puerta abierta al Balcón Presidencial desde el Salón Central / Galería 2.º piso
        fillBox(world, chunkBox, px - 2, baseY + 12, pz - 1, px + 2, baseY + 16, pz, Blocks.AIR.getDefaultState());
        // Toldo rojo presidencial sobre el balcón central
        fillBox(world, chunkBox, px - 3, baseY + 17, pz + 1, px + 3, baseY + 17, pz + 2, Blocks.RED_WOOL.getDefaultState());
        // Pendones tricolores bajo el Balcón Presidencial
        setBlock(world, chunkBox, px - 2, baseY + 10, pz + 2, Blocks.GREEN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, px, baseY + 10, pz + 2, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, px + 2, baseY + 10, pz + 2, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));

        // Relieve esculpido del Escudo Nacional en el 3.er cuerpo (ry = 21..26)
        fillBox(world, chunkBox, px - 3, baseY + 21, pz + 1, px + 3, baseY + 26, pz + 1, polDiorite);
        fillBox(world, chunkBox, px - 1, baseY + 23, pz + 1, px + 1, baseY + 25, pz + 1, rawGold);

        // ESPADAÑA BARROCA Y NICHO DE LA CAMPANA DE DOLORES (ry = 31..39, coronando el centro como en la foto)
        fillBox(world, chunkBox, px - 8, baseY + 31, pz - 1, px + 8, baseY + 33, pz, chiluca);
        fillBox(world, chunkBox, px - 6, baseY + 34, pz - 1, px + 6, baseY + 36, pz, stoneBricks);
        fillBox(world, chunkBox, px - 4, baseY + 37, pz - 1, px + 4, baseY + 38, pz, chiseled);
        fillBox(world, chunkBox, px - 2, baseY + 39, pz - 1, px + 2, baseY + 39, pz, polDiorite);

        // Nicho abierto con la histórica Campana de Dolores dorada e iluminada
        fillBox(world, chunkBox, px - 1, baseY + 32, pz - 1, px + 1, baseY + 35, pz + 1, Blocks.AIR.getDefaultState());
        setBlock(world, chunkBox, px, baseY + 35, pz, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, px, baseY + 34, pz, gold);
        setBlock(world, chunkBox, px, baseY + 33, pz, Blocks.BELL.getDefaultState());
        setBlock(world, chunkBox, px - 2, baseY + 34, pz + 1, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        setBlock(world, chunkBox, px + 2, baseY + 34, pz + 1, Blocks.OCHRE_FROGLIGHT.getDefaultState());

        // Mástil central sobre la espadaña (visible en la fotografía)
        fillBox(world, chunkBox, px, baseY + 40, pz, px, baseY + 52, pz, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
        setBlock(world, chunkBox, px, baseY + 53, pz, gold);
    }

    private static void buildSideBaroquePortal(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int px,
            int baseY,
            int pz,
            boolean isSouthFront
    ) {
        BlockState stoneBricks = Blocks.STONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState polDiorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        int outZ = pz + (isSouthFront ? 1 : -1);

        fillBox(world, chunkBox, px - 8, baseY + 1, pz - (isSouthFront ? 1 : 0), px + 8, baseY + 29, pz + (isSouthFront ? 0 : 1), stoneBricks);
        // Portal abierto de acceso a las Alas Este/Oeste y Norte (5 bloques de ancho x 6 de alto)
        fillBox(world, chunkBox, px - 2, baseY + 1, pz - 2, px + 2, baseY + 6, pz + 2, Blocks.AIR.getDefaultState());

        for (int colX : new int[]{-6, -4, 4, 6}) {
            fillBox(world, chunkBox, px + colX, baseY + 1, outZ, px + colX, baseY + 26, outZ, pillar);
        }
        // Balcón noble y toldo rojo sobre la portada
        fillBox(world, chunkBox, px - 3, baseY + 11, outZ, px + 3, baseY + 11, outZ, polDiorite);
        fillBox(world, chunkBox, px - 3, baseY + 12, outZ, px + 3, baseY + 12, outZ, Blocks.IRON_BARS.getDefaultState());
        fillBox(world, chunkBox, px - 2, baseY + 13, pz, px + 2, baseY + 16, pz, Blocks.GLASS_PANE.getDefaultState());
        fillBox(world, chunkBox, px - 2, baseY + 17, outZ, px + 2, baseY + 17, outZ, Blocks.RED_WOOL.getDefaultState());

        // Frontón barroco escalonado superior (ry = 30..34)
        fillBox(world, chunkBox, px - 6, baseY + 30, pz, px + 6, baseY + 31, pz, chiseled);
        fillBox(world, chunkBox, px - 4, baseY + 32, pz, px + 4, baseY + 33, pz, polDiorite);
        setBlock(world, chunkBox, px, baseY + 34, pz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
    }

    // =========================================================================
    // 4. INTERIOR LUJOSO DE 3 PLANTAS + GRAN SALÓN / PATIO CENTRAL DEL JEFE FINAL
    // =========================================================================

    private static void buildPalacioInteriorAndFinalBossArena(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        if (chunkBox.getMaxX() < centerX + PALACE_MIN_X
                || chunkBox.getMinX() > centerX + PALACE_MAX_X
                || chunkBox.getMaxZ() < centerZ + PALACE_MIN_Z
                || chunkBox.getMinZ() > centerZ + PALACE_MAX_Z) {
            return;
        }

        // A. GRAN VESTÍBULO IMPERIAL Y ESCALERAS MONUMENTALES CON MURALES DE DIEGO RIVERA (dx = -48..48, dz = -45..-17)
        buildGrandVestibuleAndMurals(world, chunkBox, centerX, baseY, centerZ);

        // B. GRAN PATIO CENTRAL / SALÓN MAGNO DEL JEFE FINAL (101x81 bloques, dx = -50..50, dz = -126..-46)
        buildColossalFinalBossArena(world, chunkBox, centerX, baseY, centerZ);

        // C. ALA OESTE / PONIENTE (Tesorería, Patio de la Emperatriz, Despacho Presidencial, Gabinete, Salón Morisco, Biblioteca Madero)
        buildWestWingRooms(world, chunkBox, centerX, baseY, centerZ, random);

        // D. ALA ESTE / ORIENTE (Recinto Juárez, Patio Mariano, Cocinas Reales, Salón Embajadores, Recinto Parlamentario, Comedor de Estado, Pinacoteca)
        buildEastWingRooms(world, chunkBox, centerX, baseY, centerZ, random);

        // E. ALA NORTE / GUARDIA PRESIDENCIAL (detrás del escenario del Jefe Final: dx = -49..49, dz = -155..-127)
        buildNorthGuardWing(world, chunkBox, centerX, baseY, centerZ, random);

        // F. Iluminación general de techos en las 3 plantas para garantizar nivel de luz óptimo y ambiente cálido real
        buildPalacioInteriorCeilingLights(world, chunkBox, centerX, baseY, centerZ);
    }

    private static void buildGrandVestibuleAndMurals(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState chiluca = Blocks.STONE_BRICKS.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();

        // Muros divisorios laterales del Gran Vestíbulo (dx = ±49, dz = -45..-17) con amplios arcos hacia las Alas
        for (int side : new int[]{-49, 49}) {
            fillBox(world, chunkBox, centerX + side, baseY + 1, centerZ - 45, centerX + side, baseY + 26, centerZ - 17, chiluca);
            // Arcos de conexión en los 3 pisos hacia las Alas Oeste y Este
            for (int floorBase : new int[]{1, 11, 21}) {
                fillBox(world, chunkBox, centerX + side, baseY + floorBase, centerZ - 34, centerX + side, baseY + floorBase + 4, centerZ - 28, Blocks.AIR.getDefaultState());
            }
        }

        // Gran Alfombra Imperial desde la Puerta Central (dz = -17) hasta el Arco del Jefe Final (dz = -46)
        placeRug(world, chunkBox, centerX - 4, baseY + 1, centerZ - 45, centerX + 4, centerZ - 17, redCarpet, yellowCarpet);

        // Columnata imperial del vestíbulo
        for (int dz : new int[]{-22, -30, -38}) {
            for (int dx : new int[]{-14, 14}) {
                fillBox(world, chunkBox, centerX + dx, baseY + 1, centerZ + dz, centerX + dx, baseY + 26, centerZ + dz, pillar);
                setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ + dz, gold);
                setBlock(world, chunkBox, centerX + dx, baseY + 20, centerZ + dz, gold);
            }
        }

        // ESCALERAS IMPERIALES GEMELAS (conectando Planta Baja -> 2.º Piso en dx = ±26..±36 y 2.º -> 3.er Piso)
        for (int side : new int[]{-1, 1}) {
            int x0 = centerX + (side < 0 ? -36 : 28);
            int x1 = centerX + (side < 0 ? -28 : 36);
            // Tramo Planta Baja (ry=1, dz=-24) -> 2.º Piso (ry=10, dz=-33)
            for (int step = 0; step < 10; step++) {
                int sy = baseY + 1 + step;
                int sz = centerZ - 24 - step;
                fillBox(world, chunkBox, x0, baseY + 1, sz, x1, sy - 1, sz, Blocks.SMOOTH_SANDSTONE.getDefaultState());
                fillBox(world, chunkBox, x0, sy, sz, x1, sy, sz,
                        Blocks.SANDSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
                fillBox(world, chunkBox, x0, sy + 1, sz, x1, sy + 6, sz, Blocks.AIR.getDefaultState());
                setBlock(world, chunkBox, (x0 + x1) / 2, sy, sz, Blocks.RED_NETHER_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            }
            // Descanso intermedio 2.º piso (dz = -34..-42)
            fillBox(world, chunkBox, x0 - 4, baseY + 10, centerZ - 44, x1 + 4, baseY + 10, centerZ - 34, Blocks.SMOOTH_QUARTZ.getDefaultState());

            // Tramo 2.º Piso -> 3.er Piso (pegado a muros exteriores dx = ±38..±44)
            int ux0 = centerX + (side < 0 ? -45 : 39);
            int ux1 = centerX + (side < 0 ? -39 : 45);
            for (int step = 0; step < 10; step++) {
                int sy = baseY + 11 + step;
                int sz = centerZ - 34 + step;
                fillBox(world, chunkBox, ux0, baseY + 11, sz, ux1, sy - 1, sz, Blocks.SMOOTH_SANDSTONE.getDefaultState());
                fillBox(world, chunkBox, ux0, sy, sz, ux1, sy, sz,
                        Blocks.SANDSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
                fillBox(world, chunkBox, ux0, sy + 1, sz, ux1, sy + 5, sz, Blocks.AIR.getDefaultState());
            }
        }

        // MURALES MONUMENTALES ESTILO DIEGO RIVERA ("Epopeya del Pueblo Mexicano") en las paredes de las escaleras (dx = ±48)
        for (int side : new int[]{-48, 48}) {
            buildHistoricalMuralWall(world, chunkBox, centerX + side, baseY + 12, centerZ - 42, centerZ - 20);
        }

        // Gran Candelabro Imperial en el centro del Vestíbulo (dx = 0, dz = -30)
        buildRoyalChandelier(world, chunkBox, centerX, baseY + 26, centerZ - 30, 6);
    }

    private static void buildHistoricalMuralWall(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int mx,
            int minY,
            int minZ,
            int maxZ
    ) {
        BlockState[] muralPalette = {
                Blocks.ORANGE_TERRACOTTA.getDefaultState(),
                Blocks.YELLOW_TERRACOTTA.getDefaultState(),
                Blocks.CYAN_TERRACOTTA.getDefaultState(),
                Blocks.RED_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_TERRACOTTA.getDefaultState(),
                Blocks.GOLD_BLOCK.getDefaultState(),
                Blocks.LAPIS_BLOCK.getDefaultState(),
                Blocks.WHITE_TERRACOTTA.getDefaultState()
        };
        for (int wz = minZ; wz <= maxZ; wz++) {
            for (int wy = minY; wy <= minY + 7; wy++) {
                boolean frame = (wz == minZ || wz == maxZ || wy == minY || wy == minY + 7);
                if (frame) {
                    setBlock(world, chunkBox, mx, wy, wz, Blocks.DARK_OAK_WOOD.getDefaultState());
                } else {
                    int idx = Math.floorMod((wz * 5) + (wy * 3) + (wz ^ wy), muralPalette.length);
                    setBlock(world, chunkBox, mx, wy, wz, muralPalette[idx]);
                }
            }
        }
    }

    /**
     * GRAN PATIO CENTRAL / SALÓN MAGNO DEL JEFE FINAL:
     * - Dimensiones totales: 101 x 81 bloques (dx = -50..+50, dz = -126..-46).
     * - Pista central de combate 100% libre de columnas: 89 x 71 bloques (dx = -44..+44, dz = -121..-51).
     * - Rodeado en sus 4 costados por 3 niveles de arcadas virreinales de cantera y coronado a 35 bloques
     *   de altura por un colosal domo abovedado de cristal, hierro negro y oro.
     */
    private static void buildColossalFinalBossArena(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        if (chunkBox.getMaxX() < centerX - 51 || chunkBox.getMinX() > centerX + 51
                || chunkBox.getMaxZ() < centerZ - 127 || chunkBox.getMinZ() > centerZ - 45) {
            return;
        }

        BlockState chiluca = Blocks.STONE_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
        BlockState polAndesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState marble = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState blackMarble = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState ochreLight = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState ironBars = Blocks.IRON_BARS.getDefaultState();

        // 1. PISO MONUMENTAL DE LA ARENA DEL JEFE FINAL (dx = -44..+44, dz = -121..-51)
        int minX = Math.max(centerX - 44, chunkBox.getMinX());
        int maxX = Math.min(centerX + 44, chunkBox.getMaxX());
        int minZ = Math.max(centerZ - 121, chunkBox.getMinZ());
        int maxZ = Math.min(centerZ - 51, chunkBox.getMaxZ());

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                int adz = dz + 86; // Centro geométrico de la arena en (dx = 0, dz = -86)
                double dist = Math.sqrt((double) dx * dx + (double) adz * adz);

                BlockState floorTile;
                if (Math.abs(dx) <= 4 && dz >= -109 && dz <= -51 && dist > 18.0D) {
                    // Alfombra real de mármol carmesí y oro rumbo al Estrado del Jefe Final
                    floorTile = (Math.abs(dx) == 4) ? gold : Blocks.RED_NETHER_BRICKS.getDefaultState();
                } else if (dist <= 18.0D) {
                    if (dist >= 16.5D) {
                        floorTile = ((dx + dz) % 3 == 0) ? ochreLight : blackMarble;
                    } else if (dist >= 14.2D) {
                        floorTile = (dx < -4) ? Blocks.EMERALD_BLOCK.getDefaultState()
                                : (dx > 4 ? Blocks.RED_NETHER_BRICKS.getDefaultState() : marble);
                    } else if (dist <= 3.5D) {
                        floorTile = dist <= 1.5D ? gold : ochreLight;
                    } else if (Math.abs(dx) <= 1 || Math.abs(adz) <= 1 || Math.abs(Math.abs(dx) - Math.abs(adz)) <= 1) {
                        floorTile = cantera;
                    } else {
                        floorTile = (((dx + dz) & 1) == 0) ? marble : Blocks.POLISHED_DIORITE.getDefaultState();
                    }
                } else if (Math.floorMod(dx, 10) == 0 && Math.floorMod(dz, 10) == 0) {
                    floorTile = ochreLight; // Luz empotrada cada 10 bloques para iluminación total 15
                } else if (Math.floorMod(dx, 10) == 0 || Math.floorMod(dz, 10) == 0) {
                    floorTile = blackMarble;
                } else {
                    floorTile = (((dx + dz) & 1) == 0) ? cantera : polAndesite;
                }
                setBlock(world, chunkBox, wx, baseY, wz, floorTile);
            }
        }

        // 2. ARCADAS VIRREINALES DE 3 PISOS RODEANDO LA ARENA (Oeste dx=-50..-45, Este dx=45..50, Sur dz=-50..-46, Norte dz=-126..-122)
        // Galerías perimetrales en 2.º piso (ry=10) y 3.er piso (ry=20)
        for (int wx = Math.max(centerX - 50, chunkBox.getMinX()); wx <= Math.min(centerX + 50, chunkBox.getMaxX()); wx++) {
            int dx = wx - centerX;
            for (int wz = Math.max(centerZ - 126, chunkBox.getMinZ()); wz <= Math.min(centerZ - 46, chunkBox.getMaxZ()); wz++) {
                int dz = wz - centerZ;
                boolean inGalleryRing = Math.abs(dx) >= 45 || dz <= -122 || dz >= -50;
                if (!inGalleryRing) {
                    continue;
                }

                // Pisos de galería perimetral
                setBlock(world, chunkBox, wx, baseY + 9, wz, chiluca);
                setBlock(world, chunkBox, wx, baseY + 10, wz, cantera);
                setBlock(world, chunkBox, wx, baseY + 19, wz, chiluca);
                setBlock(world, chunkBox, wx, baseY + 20, wz, cantera);
                setBlock(world, chunkBox, wx, baseY + 27, wz, chiluca);

                // Muros exteriores del anillo del Patio Central (dx = ±50, dz = -46, dz = -126)
                boolean onOuterWall = Math.abs(dx) == 50 || dz == -46 || dz == -126;
                if (onOuterWall) {
                    boolean isMainSouthPortal = (dz == -46 && Math.abs(dx) <= 6);
                    boolean isNorthRearDoor = (dz == -126 && Math.abs(dx) <= 4);
                    boolean isWingSideDoor = (Math.abs(dx) == 50 && (Math.abs(dz + 56) <= 3 || Math.abs(dz + 86) <= 3 || Math.abs(dz + 114) <= 3));
                    for (int ry = 1; ry <= 26; ry++) {
                        if (isMainSouthPortal && ry <= 8) {
                            continue;
                        }
                        if ((isNorthRearDoor || isWingSideDoor) && ((ry >= 1 && ry <= 5) || (ry >= 11 && ry <= 15) || (ry >= 21 && ry <= 24))) {
                            continue;
                        }
                        setBlock(world, chunkBox, wx, baseY + ry, wz, (ry == 10 || ry == 20) ? chiseled : cantera);
                    }
                }

                // Arcadas interiores mirando a la Arena (dx = ±45, dz = -50, dz = -122)
                boolean onInnerArcadeX = Math.abs(dx) == 45 && dz >= -122 && dz <= -50;
                boolean onInnerArcadeZ = (dz == -50 || dz == -122) && Math.abs(dx) <= 45;
                if (onInnerArcadeX || onInnerArcadeZ) {
                    boolean southEntryGap = (dz == -50 && Math.abs(dx) <= 6);
                    int coord = onInnerArcadeX ? dz : dx;
                    int mod = Math.floorMod(coord, 6);
                    boolean isPillar = (mod == 0);

                    if (!southEntryGap) {
                        for (int tierBase : new int[]{1, 11, 21}) {
                            int tierTop = (tierBase == 21) ? 26 : tierBase + 7;
                            for (int ry = tierBase; ry <= tierTop; ry++) {
                                if (isPillar || ry == tierTop) {
                                    setBlock(world, chunkBox, wx, baseY + ry, wz, (ry == tierTop) ? chiseled : Blocks.QUARTZ_PILLAR.getDefaultState());
                                } else if (ry == tierTop - 1 && (mod == 1 || mod == 5)) {
                                    setBlock(world, chunkBox, wx, baseY + ry, wz, chiluca);
                                } else if (tierBase > 1 && ry == tierBase) {
                                    setBlock(world, chunkBox, wx, baseY + ry, wz, ironBars); // Barandal de los pisos 2 y 3
                                }
                            }
                            if (mod == 3) {
                                setBlock(world, chunkBox, wx, baseY + tierTop - 1, wz,
                                        Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
                            }
                        }
                    }
                }
            }
        }

        // 3. DOMO MONUMENTAL ABOVEDADO DE CRISTAL, HIERRO Y ORO (ry = 28..35 sobre toda la arena)
        BlockState domeFrame = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState domeGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState clearSkylight = Blocks.WHITE_STAINED_GLASS.getDefaultState();

        for (int wx = Math.max(centerX - 46, chunkBox.getMinX()); wx <= Math.min(centerX + 46, chunkBox.getMaxX()); wx++) {
            int dx = wx - centerX;
            for (int wz = Math.max(centerZ - 123, chunkBox.getMinZ()); wz <= Math.min(centerZ - 49, chunkBox.getMaxZ()); wz++) {
                int dz = wz - centerZ;
                double normX = Math.abs(dx) / 46.0D;
                double normZ = Math.abs(dz + 86) / 37.0D;
                double edgeDist = Math.max(normX, normZ);
                int domeY = baseY + 28 + (int) Math.round((1.0D - edgeDist * edgeDist) * 7.0D);

                boolean isRib = (Math.floorMod(dx, 9) == 0) || (Math.floorMod(dz, 9) == 0) || Math.abs(dx) >= 45 || dz <= -122 || dz >= -50;
                boolean isLightNode = (Math.floorMod(dx, 9) == 0 && Math.floorMod(dz, 9) == 0);
                BlockState roofBlock = isLightNode ? seaLantern : (isRib ? ((Math.abs(dx) <= 9 && Math.abs(dz + 86) <= 9) ? gold : domeFrame)
                        : (edgeDist < 0.45D ? clearSkylight : domeGlass));
                setBlock(world, chunkBox, wx, domeY, wz, roofBlock);

                // Cerrar faldón vertical perimetral entre ry = 27 y la base de la bóveda
                if (Math.abs(dx) == 46 || dz == -123 || dz == -49) {
                    for (int y = baseY + 27; y <= domeY; y++) {
                        setBlock(world, chunkBox, wx, y, wz, domeFrame);
                    }
                }
            }
        }

        // 4. GRAN ESTRADO / TRONO PRESIDENCIAL DEL JEFE FINAL EN EL EXTREMO NORTE DE LA ARENA (dx = -16..16, dz = -120..-110)
        fillBox(world, chunkBox, centerX - 16, baseY + 1, centerZ - 120, centerX + 16, baseY + 1, centerZ - 110, marble);
        fillBox(world, chunkBox, centerX - 14, baseY + 2, centerZ - 120, centerX + 14, baseY + 2, centerZ - 112, marble);
        placeRug(world, chunkBox, centerX - 12, baseY + 3, centerZ - 119, centerX + 12, centerZ - 113,
                Blocks.RED_CARPET.getDefaultState(), Blocks.YELLOW_CARPET.getDefaultState());

        // Escalinata frontal amplia hacia la arena
        fillBox(world, chunkBox, centerX - 10, baseY + 1, centerZ - 109, centerX + 10, baseY + 1, centerZ - 109,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        fillBox(world, chunkBox, centerX - 10, baseY + 2, centerZ - 111, centerX + 10, baseY + 2, centerZ - 111,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));

        // Podio / Atril Presidencial y Trono Dorado de Honor (dejando libre FINAL_BOSS_SPAWN en 0, baseY+3, -114)
        fillBox(world, chunkBox, centerX - 1, baseY + 3, centerZ - 112, centerX + 1, baseY + 3, centerZ - 112, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 4, centerZ - 112, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 1, baseY + 4, centerZ - 112, gold);
        setBlock(world, chunkBox, centerX + 1, baseY + 4, centerZ - 112, gold);

        // Gran Trono y Telón Tricolor Monumental detrás del Jefe Final (dz = -121, ry = 3..18)
        fillBox(world, chunkBox, centerX - 2, baseY + 3, centerZ - 119, centerX + 2, baseY + 7, centerZ - 119, Blocks.RED_NETHER_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX - 1, baseY + 3, centerZ - 118, centerX + 1, baseY + 3, centerZ - 118, gold);
        setBlock(world, chunkBox, centerX, baseY + 8, centerZ - 119, gold);

        for (int side : new int[]{-11, 11}) {
            for (int ry = 4; ry <= 17; ry++) {
                setBlock(world, chunkBox, centerX + side - 2, baseY + ry, centerZ - 121, Blocks.GREEN_WOOL.getDefaultState());
                setBlock(world, chunkBox, centerX + side - 1, baseY + ry, centerZ - 121, Blocks.GREEN_WOOL.getDefaultState());
                setBlock(world, chunkBox, centerX + side, baseY + ry, centerZ - 121,
                        (ry >= 9 && ry <= 12) ? Blocks.RAW_GOLD_BLOCK.getDefaultState() : Blocks.WHITE_WOOL.getDefaultState());
                setBlock(world, chunkBox, centerX + side + 1, baseY + ry, centerZ - 121, Blocks.RED_WOOL.getDefaultState());
                setBlock(world, chunkBox, centerX + side + 2, baseY + ry, centerZ - 121, Blocks.RED_WOOL.getDefaultState());
            }
        }

        // Pebeteros dorados en las esquinas del estrado
        for (int sx : new int[]{-15, 15}) {
            setBlock(world, chunkBox, centerX + sx, baseY + 3, centerZ - 112, Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
            setBlock(world, chunkBox, centerX + sx, baseY + 4, centerZ - 112, ochreLight);
        }

        // 4 Grandes Candelabros Reales colgando muy alto (ry = 27..30) para iluminar toda la Arena sin estorbar combates aéreos
        buildRoyalChandelier(world, chunkBox, centerX - 22, baseY + 30, centerZ - 68, 4);
        buildRoyalChandelier(world, chunkBox, centerX + 22, baseY + 30, centerZ - 68, 4);
        buildRoyalChandelier(world, chunkBox, centerX - 22, baseY + 30, centerZ - 104, 4);
        buildRoyalChandelier(world, chunkBox, centerX + 22, baseY + 30, centerZ - 104, 4);
    }

    // =========================================================================
    // C. ALA OESTE / PONIENTE (3 PLANTAS COMPLETAMENTE AMUEBLADAS)
    // =========================================================================

    private static void buildWestWingRooms(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        if (chunkBox.getMinX() > centerX - 50) {
            return;
        }

        BlockState wallStone = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState darkOak = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState bookshelf = Blocks.BOOKSHELF.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();

        // Muros divisorios interiores con amplios arcos (conecta Norte-Sur en dx = -58)
        for (int floorY : new int[]{1, 11, 21}) {
            int h = (floorY == 21) ? 5 : 7;
            fillBox(world, chunkBox, centerX - 115, baseY + floorY, centerZ - 64, centerX - 51, baseY + floorY + h, centerZ - 64, wallStone);
            fillBox(world, chunkBox, centerX - 115, baseY + floorY, centerZ - 106, centerX - 51, baseY + floorY + h, centerZ - 106, wallStone);
            // Pasillos abiertos amplios
            fillBox(world, chunkBox, centerX - 61, baseY + floorY, centerZ - 64, centerX - 55, baseY + floorY + 4, centerZ - 64, Blocks.AIR.getDefaultState());
            fillBox(world, chunkBox, centerX - 61, baseY + floorY, centerZ - 106, centerX - 55, baseY + floorY + 4, centerZ - 106, Blocks.AIR.getDefaultState());
        }

        // Escalera interna del Ala Oeste (dx = -112..-104, dz = -90..-78) conectando Planta Baja -> 2.º -> 3.er Piso
        buildInternalWingStairwell(world, chunkBox, centerX - 108, baseY, centerZ - 85);

        // 1. PLANTA BAJA SUR: TESORERÍA NACIONAL Y BÓVEDA REAL (dx = -114..-52, dz = -63..-18)
        fillBox(world, chunkBox, centerX - 102, baseY + 1, centerZ - 42, centerX - 68, baseY + 1, centerZ - 42, darkOak);
        fillBox(world, chunkBox, centerX - 102, baseY + 2, centerZ - 42, centerX - 68, baseY + 3, centerZ - 42, Blocks.IRON_BARS.getDefaultState());
        // Bóveda blindada con reservas de oro y cofres de tesoro
        fillBox(world, chunkBox, centerX - 112, baseY + 1, centerZ - 62, centerX - 94, baseY + 6, centerZ - 48, Blocks.DEEPSLATE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX - 111, baseY + 1, centerZ - 61, centerX - 95, baseY + 5, centerZ - 49, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, centerX - 94, baseY + 1, centerZ - 56, centerX - 94, baseY + 4, centerZ - 54, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, centerX - 109, baseY + 1, centerZ - 59, centerX - 106, baseY + 3, centerZ - 57, gold);
        fillBox(world, chunkBox, centerX - 102, baseY + 1, centerZ - 59, centerX - 100, baseY + 2, centerZ - 57, Blocks.EMERALD_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 103, baseY + 5, centerZ - 55, Blocks.SEA_LANTERN.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(centerX - 110, baseY + 1, centerZ - 51), Direction.EAST, LootTables.ANCIENT_CITY_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 98, baseY + 1, centerZ - 60), Direction.SOUTH, LootTables.BASTION_TREASURE_CHEST, random);

        // 2. PLANTA BAJA CENTRO: PATIO DE LA EMPERATRIZ / JARDÍN BOTÁNICO (dx = -100..-66, dz = -98..-72)
        buildInteriorCourtyardGarden(world, chunkBox, centerX - 83, baseY, centerZ - 85, true);

        // 3. PLANTA BAJA NORTE: MUSEO DE ARMAS VIRREINALES (dx = -114..-52, dz = -154..-107)
        placeRug(world, chunkBox, centerX - 96, baseY + 1, centerZ - 146, centerX - 68, centerZ - 114,
                Blocks.RED_CARPET.getDefaultState(), Blocks.YELLOW_CARPET.getDefaultState());
        for (int vz : new int[]{-142, -130, -118}) {
            for (int vx : new int[]{-92, -72}) {
                setBlock(world, chunkBox, centerX + vx, baseY + 1, centerZ + vz, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
                setBlock(world, chunkBox, centerX + vx, baseY + 2, centerZ + vz, (vz == -130) ? gold : Blocks.AMETHYST_CLUSTER.getDefaultState());
                setBlock(world, chunkBox, centerX + vx, baseY + 3, centerZ + vz, Blocks.GLASS.getDefaultState());
            }
        }
        placeLootChest(world, chunkBox, new BlockPos(centerX - 110, baseY + 1, centerZ - 148), Direction.EAST, LootTables.WOODLAND_MANSION_CHEST, random);

        // 4. SEGUNDO PISO SUR: DESPACHO PRESIDENCIAL Y SALÓN DE GABINETE (ry = 11..18, dz = -63..-18)
        fillBox(world, chunkBox, centerX - 84, baseY + 11, centerZ - 63, centerX - 84, baseY + 18, centerZ - 18, wallStone);
        fillBox(world, chunkBox, centerX - 84, baseY + 11, centerZ - 42, centerX - 84, baseY + 14, centerZ - 38, Blocks.AIR.getDefaultState());

        // Despacho Presidencial (dx = -114..-85)
        placeRug(world, chunkBox, centerX - 108, baseY + 11, centerZ - 54, centerX - 90, centerZ - 26,
                Blocks.GREEN_CARPET.getDefaultState(), Blocks.YELLOW_CARPET.getDefaultState());
        fillBox(world, chunkBox, centerX - 102, baseY + 11, centerZ - 32, centerX - 96, baseY + 11, centerZ - 30, darkOak);
        setBlock(world, chunkBox, centerX - 99, baseY + 11, centerZ - 28,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
        fillBox(world, chunkBox, centerX - 114, baseY + 11, centerZ - 58, centerX - 114, baseY + 15, centerZ - 44, bookshelf);
        // Sala de juntas del Despacho con sofás del mod
        for (int sz = -48; sz <= -42; sz++) {
            setBlock(world, chunkBox, centerX - 92, baseY + 11, centerZ + sz,
                    ModBlocks.CASINO_SOFA.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        }
        setBlock(world, chunkBox, centerX - 95, baseY + 11, centerZ - 45, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 112, baseY + 11, centerZ - 22, ModBlocks.CASINO_PALM_POT.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(centerX - 112, baseY + 11, centerZ - 60), Direction.SOUTH, LootTables.ANCIENT_CITY_CHEST, random);

        // Salón de Acuerdos del Gabinete (dx = -83..-52): Gran mesa para 18 ministros
        fillBox(world, chunkBox, centerX - 70, baseY + 11, centerZ - 54, centerX - 66, baseY + 11, centerZ - 26, darkOak);
        fillBox(world, chunkBox, centerX - 70, baseY + 12, centerZ - 54, centerX - 66, baseY + 12, centerZ - 26, Blocks.GREEN_CARPET.getDefaultState());
        for (int cz = -52; cz <= -28; cz += 3) {
            setBlock(world, chunkBox, centerX - 72, baseY + 11, centerZ + cz,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
            setBlock(world, chunkBox, centerX - 64, baseY + 11, centerZ + cz,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        }

        // 5. SEGUNDO PISO NORTE: SALÓN MORISCO Y SALÓN GUILLERMO PRIETO (dz = -154..-107)
        for (int mx = -104; mx <= -64; mx++) {
            for (int mz = -148; mz <= -114; mz++) {
                BlockState tile = (((mx + mz) % 4 == 0)) ? Blocks.YELLOW_GLAZED_TERRACOTTA.getDefaultState()
                        : (((mx - mz) % 4 == 0) ? Blocks.BLUE_GLAZED_TERRACOTTA.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState());
                setBlock(world, chunkBox, centerX + mx, baseY + 10, centerZ + mz, tile);
            }
        }
        buildRoyalChandelier(world, chunkBox, centerX - 84, baseY + 18, centerZ - 131, 3);

        // 6. TERCER PISO: BIBLIOTECA FONDO HISTÓRICO FRANCISCO I. MADERO (ry = 21..26)
        for (int bx = -108; bx <= -62; bx += 8) {
            fillBox(world, chunkBox, centerX + bx, baseY + 21, centerZ - 58, centerX + bx, baseY + 25, centerZ - 24, bookshelf);
        }
        // Mesas de lectura con lámparas verdes de biblioteca
        for (int tz : new int[]{-50, -38, -28}) {
            fillBox(world, chunkBox, centerX - 88, baseY + 21, centerZ + tz - 1, centerX - 80, baseY + 21, centerZ + tz + 1, darkOak);
            setBlock(world, chunkBox, centerX - 84, baseY + 22, centerZ + tz, Blocks.LANTERN.getDefaultState());
        }
        setBlock(world, chunkBox, centerX - 84, baseY + 21, centerZ - 130, Blocks.ENCHANTING_TABLE.getDefaultState());
        fillBox(world, chunkBox, centerX - 86, baseY + 21, centerZ - 132, centerX - 82, baseY + 22, centerZ - 132, bookshelf);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 110, baseY + 21, centerZ - 148), Direction.EAST, LootTables.STRONGHOLD_LIBRARY_CHEST, random);
    }

    // =========================================================================
    // D. ALA ESTE / ORIENTE (3 PLANTAS COMPLETAMENTE AMUEBLADAS)
    // =========================================================================

    private static void buildEastWingRooms(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        if (chunkBox.getMaxX() < centerX + 50) {
            return;
        }

        BlockState wallStone = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState darkOak = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState marble = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();

        for (int floorY : new int[]{1, 11, 21}) {
            int h = (floorY == 21) ? 5 : 7;
            fillBox(world, chunkBox, centerX + 51, baseY + floorY, centerZ - 64, centerX + 115, baseY + floorY + h, centerZ - 64, wallStone);
            fillBox(world, chunkBox, centerX + 51, baseY + floorY, centerZ - 106, centerX + 115, baseY + floorY + h, centerZ - 106, wallStone);
            fillBox(world, chunkBox, centerX + 55, baseY + floorY, centerZ - 64, centerX + 61, baseY + floorY + 4, centerZ - 64, Blocks.AIR.getDefaultState());
            fillBox(world, chunkBox, centerX + 55, baseY + floorY, centerZ - 106, centerX + 61, baseY + floorY + 4, centerZ - 106, Blocks.AIR.getDefaultState());
        }

        // Escalera interna del Ala Este (dx = +104..+112, dz = -90..-78)
        buildInternalWingStairwell(world, chunkBox, centerX + 108, baseY, centerZ - 85);

        // 1. PLANTA BAJA SUR: RECINTO DE HOMENAJE A BENITO JUÁREZ (dx = +52..+114, dz = -63..-18)
        placeRug(world, chunkBox, centerX + 66, baseY + 1, centerZ - 54, centerX + 98, centerZ - 26,
                Blocks.RED_CARPET.getDefaultState(), Blocks.WHITE_CARPET.getDefaultState());
        // Monumento central en basalto negro y oro
        fillBox(world, chunkBox, centerX + 80, baseY + 1, centerZ - 42, centerX + 84, baseY + 2, centerZ - 38, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        fillBox(world, chunkBox, centerX + 81, baseY + 3, centerZ - 41, centerX + 83, baseY + 5, centerZ - 39, Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState());
        setBlock(world, chunkBox, centerX + 82, baseY + 6, centerZ - 40, gold);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 110, baseY + 1, centerZ - 58), Direction.WEST, LootTables.WOODLAND_MANSION_CHEST, random);

        // 2. PLANTA BAJA CENTRO: PATIO MARIANO (dx = +66..+100, dz = -98..-72)
        buildInteriorCourtyardGarden(world, chunkBox, centerX + 83, baseY, centerZ - 85, false);

        // 3. PLANTA BAJA NORTE: COCINAS REALES Y BODEGA DE GALA (dx = +52..+114, dz = -154..-107)
        for (int ox = 72; ox <= 96; ox += 4) {
            setBlock(world, chunkBox, centerX + ox, baseY + 1, centerZ - 152, Blocks.SMOKER.getDefaultState().with(FurnaceBlock.FACING, Direction.SOUTH));
            setBlock(world, chunkBox, centerX + ox + 1, baseY + 1, centerZ - 152, Blocks.BRICKS.getDefaultState());
            setBlock(world, chunkBox, centerX + ox, baseY + 2, centerZ - 152, Blocks.BRICK_WALL.getDefaultState());
        }
        fillBox(world, chunkBox, centerX + 74, baseY + 1, centerZ - 134, centerX + 94, baseY + 1, centerZ - 130, Blocks.POLISHED_DIORITE.getDefaultState());
        fillBox(world, chunkBox, centerX + 106, baseY + 1, centerZ - 146, centerX + 112, baseY + 3, centerZ - 146,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        placeLootChest(world, chunkBox, new BlockPos(centerX + 110, baseY + 1, centerZ - 140), Direction.WEST, LootTables.VILLAGE_BUTCHER_CHEST, random);

        // 4. SEGUNDO PISO SUR: SALÓN DE EMBAJADORES Y RECEPCIONES (ry = 11..18, dz = -63..-18)
        placeRug(world, chunkBox, centerX + 60, baseY + 11, centerZ - 56, centerX + 104, centerZ - 24,
                Blocks.RED_CARPET.getDefaultState(), Blocks.YELLOW_CARPET.getDefaultState());
        // Dosel diplomático dorado
        fillBox(world, chunkBox, centerX + 106, baseY + 11, centerZ - 44, centerX + 112, baseY + 11, centerZ - 36, marble);
        setBlock(world, chunkBox, centerX + 110, baseY + 12, centerZ - 40,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        fillBox(world, chunkBox, centerX + 113, baseY + 12, centerZ - 43, centerX + 113, baseY + 17, centerZ - 37, Blocks.RED_WOOL.getDefaultState());
        buildRoyalChandelier(world, chunkBox, centerX + 76, baseY + 18, centerZ - 40, 3);
        buildRoyalChandelier(world, chunkBox, centerX + 94, baseY + 18, centerZ - 40, 3);

        // 5. SEGUNDO PISO NORTE: RECINTO PARLAMENTARIO DEL SIGLO XIX Y GRAN COMEDOR DE ESTADO (dz = -154..-107)
        // Hemiciclo Parlamentario semicircular mirando hacia la Tribuna Norte (dz = -152)
        int tribX = centerX + 82;
        int tribZ = centerZ - 148;
        fillBox(world, chunkBox, tribX - 8, baseY + 11, tribZ - 3, tribX + 8, baseY + 12, tribZ, marble);
        setBlock(world, chunkBox, tribX, baseY + 13, tribZ - 1, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        for (int col : new int[]{-7, -4, 4, 7}) {
            fillBox(world, chunkBox, tribX + col, baseY + 13, tribZ - 3, tribX + col, baseY + 17, tribZ - 3, Blocks.QUARTZ_PILLAR.getDefaultState());
        }
        // Curules semicirculares de caoba
        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = 6; dz <= 22; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if ((Math.abs(r - 10.0D) < 0.7D || Math.abs(r - 15.0D) < 0.7D || Math.abs(r - 20.0D) < 0.7D) && Math.abs(dx) > 2) {
                    setBlock(world, chunkBox, tribX + dx, baseY + 11, tribZ + dz,
                            Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
                }
            }
        }
        placeLootChest(world, chunkBox, new BlockPos(centerX + 110, baseY + 11, centerZ - 148), Direction.WEST, LootTables.ANCIENT_CITY_CHEST, random);

        // 6. TERCER PISO: PINACOTECA VIRREINAL Y GRAN COMEDOR DE GALA (ry = 21..26)
        // Gran mesa de banquete de Estado para 28 invitados en el ala Norte del 3.er piso
        fillBox(world, chunkBox, centerX + 78, baseY + 21, centerZ - 148, centerX + 84, baseY + 21, centerZ - 114, darkOak);
        fillBox(world, chunkBox, centerX + 78, baseY + 22, centerZ - 148, centerX + 84, baseY + 22, centerZ - 114, Blocks.WHITE_CARPET.getDefaultState());
        for (int bz = -146; bz <= -116; bz += 3) {
            setBlock(world, chunkBox, centerX + 76, baseY + 21, centerZ + bz,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
            setBlock(world, chunkBox, centerX + 86, baseY + 21, centerZ + bz,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
            if (bz % 6 == 0) {
                setBlock(world, chunkBox, centerX + 81, baseY + 22, centerZ + bz, Blocks.LANTERN.getDefaultState());
            }
        }
        placeLootChest(world, chunkBox, new BlockPos(centerX + 110, baseY + 21, centerZ - 40), Direction.WEST, LootTables.BASTION_TREASURE_CHEST, random);
    }

    private static void buildNorthGuardWing(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        if (chunkBox.getMinZ() > centerZ - 126 || chunkBox.getMaxX() < centerX - 49 || chunkBox.getMinX() > centerX + 49) {
            return;
        }

        // Pasillo real Norte, Cuartel de la Guardia Presidencial y Armería con botín de alto nivel
        placeRug(world, chunkBox, centerX - 3, baseY + 1, centerZ - 154, centerX + 3, centerZ - 127,
                Blocks.RED_CARPET.getDefaultState(), Blocks.YELLOW_CARPET.getDefaultState());

        // Camas del cuartel al Oeste (dx = -40..-16)
        for (int bx = -38; bx <= -18; bx += 5) {
            placeDoubleBed(world, chunkBox, centerX + bx, baseY + 1, centerZ - 148, Direction.NORTH, Blocks.RED_BED);
        }
        // Armería y mesa táctica al Este (dx = +16..+40)
        fillBox(world, chunkBox, centerX + 22, baseY + 1, centerZ - 144, centerX + 32, baseY + 1, centerZ - 138, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, centerX + 27, baseY + 2, centerZ - 141, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(centerX + 42, baseY + 1, centerZ - 150), Direction.WEST, LootTables.ANCIENT_CITY_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 42, baseY + 1, centerZ - 150), Direction.EAST, LootTables.STRONGHOLD_CORRIDOR_CHEST, random);
    }

    private static void buildInteriorCourtyardGarden(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int baseY,
            int cz,
            boolean botanicalStyle
    ) {
        BlockState cantera = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState chiluca = Blocks.STONE_BRICKS.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState leaves = (botanicalStyle ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES)
                .getDefaultState().with(LeavesBlock.PERSISTENT, true);

        // Muros con arcos rodeando el patio interior (36x28 bloques: cx±18, cz±14)
        for (int ox = -18; ox <= 18; ox++) {
            for (int oz = -14; oz <= 14; oz++) {
                boolean onRing = Math.abs(ox) == 18 || Math.abs(oz) == 14;
                if (onRing) {
                    boolean isCol = (ox % 4 == 0) && (oz % 4 == 0 || Math.abs(ox) == 18 || Math.abs(oz) == 14);
                    for (int ry = 1; ry <= 27; ry++) {
                        if (ry == 9 || ry == 10 || ry == 19 || ry == 20 || ry == 27) {
                            setBlock(world, chunkBox, cx + ox, baseY + ry, cz + oz, chiluca);
                        } else if ((Math.floorMod(ox + oz, 4) == 0)) {
                            setBlock(world, chunkBox, cx + ox, baseY + ry, cz + oz, pillar);
                        } else if (ry == 11 || ry == 21) {
                            setBlock(world, chunkBox, cx + ox, baseY + ry, cz + oz, Blocks.IRON_BARS.getDefaultState());
                        }
                    }
                    continue;
                }

                // Suelo del jardín interior con senderos en cruz y parterres verdes
                if (Math.abs(ox) <= 2 || Math.abs(oz) <= 2) {
                    setBlock(world, chunkBox, cx + ox, baseY, cz + oz, cantera);
                } else {
                    setBlock(world, chunkBox, cx + ox, baseY, cz + oz, grass);
                    if ((Math.abs(ox) == 16 || Math.abs(oz) == 12) && (ox + oz) % 2 == 0) {
                        setBlock(world, chunkBox, cx + ox, baseY + 1, cz + oz, leaves);
                    } else if (botanicalStyle && Math.abs(ox) == 9 && Math.abs(oz) == 7) {
                        setBlock(world, chunkBox, cx + ox, baseY, cz + oz, Blocks.SAND.getDefaultState());
                        fillBox(world, chunkBox, cx + ox, baseY + 1, cz + oz, cx + ox, baseY + 3, cz + oz, Blocks.CACTUS.getDefaultState());
                    }
                }
            }
        }
        buildTalaveraFountain(world, chunkBox, cx, baseY, cz, 5);
    }

    private static void buildInternalWingStairwell(StructureWorldAccess world, BlockBox chunkBox, int sx, int baseY, int sz) {
        // Hueco de escalera y tramos en U para subir cómodamente entre los 3 niveles de cada ala
        fillBox(world, chunkBox, sx - 3, baseY + 9, sz - 5, sx + 3, baseY + 20, sz + 5, Blocks.AIR.getDefaultState());
        for (int i = 0; i < 10; i++) {
            fillBox(world, chunkBox, sx - 3, baseY + 1 + i, sz - 4 + i, sx - 1, baseY + 1 + i, sz - 4 + i,
                    Blocks.SANDSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            fillBox(world, chunkBox, sx + 1, baseY + 11 + i, sz + 4 - i, sx + 3, baseY + 11 + i, sz + 4 - i,
                    Blocks.SANDSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        }
        fillBox(world, chunkBox, sx - 3, baseY + 10, sz + 5, sx + 3, baseY + 10, sz + 6, Blocks.SMOOTH_SANDSTONE.getDefaultState());
    }

    private static void buildRoyalChandelier(StructureWorldAccess world, BlockBox chunkBox, int cx, int topY, int cz, int armLen) {
        fillBox(world, chunkBox, cx, topY - 3, cz, cx, topY, cz, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, cx, topY - 4, cz, Blocks.GOLD_BLOCK.getDefaultState());
        setBlock(world, chunkBox, cx, topY - 5, cz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        for (int r = 1; r <= armLen; r++) {
            setBlock(world, chunkBox, cx + r, topY - 4, cz, Blocks.GOLD_BLOCK.getDefaultState());
            setBlock(world, chunkBox, cx - r, topY - 4, cz, Blocks.GOLD_BLOCK.getDefaultState());
            setBlock(world, chunkBox, cx, topY - 4, cz + r, Blocks.GOLD_BLOCK.getDefaultState());
            setBlock(world, chunkBox, cx, topY - 4, cz - r, Blocks.GOLD_BLOCK.getDefaultState());
            if (r == armLen || r == armLen / 2) {
                setBlock(world, chunkBox, cx + r, topY - 3, cz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
                setBlock(world, chunkBox, cx - r, topY - 3, cz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
                setBlock(world, chunkBox, cx, topY - 3, cz + r, Blocks.OCHRE_FROGLIGHT.getDefaultState());
                setBlock(world, chunkBox, cx, topY - 3, cz - r, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            }
        }
    }

    private static void buildPalacioInteriorCeilingLights(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState ochre = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        int minX = Math.max(centerX + PALACE_MIN_X + 4, chunkBox.getMinX());
        int maxX = Math.min(centerX + PALACE_MAX_X - 4, chunkBox.getMaxX());
        int minZ = Math.max(centerZ + PALACE_MIN_Z + 4, chunkBox.getMinZ());
        int maxZ = Math.min(centerZ + PALACE_MAX_Z - 4, chunkBox.getMaxZ());

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            if (Math.floorMod(dx, 8) != 0) {
                continue;
            }
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                if (Math.floorMod(dz, 8) != 0) {
                    continue;
                }
                boolean inCentralArena = Math.abs(dx) <= 44 && dz >= -121 && dz <= -51;
                boolean inWestCourtyard = dx >= -100 && dx <= -66 && dz >= -98 && dz <= -72;
                boolean inEastCourtyard = dx >= 66 && dx <= 100 && dz >= -98 && dz <= -72;
                boolean inVestibuleVoid = Math.abs(dx) <= 24 && dz >= -43 && dz <= -20;

                if (!inCentralArena && !inWestCourtyard && !inEastCourtyard) {
                    if (!inVestibuleVoid) {
                        setBlock(world, chunkBox, wx, baseY + 9, wz, ochre);
                        setBlock(world, chunkBox, wx, baseY + 19, wz, ochre);
                    }
                    setBlock(world, chunkBox, wx, baseY + 27, wz, ochre);
                }
            }
        }
    }

    // =========================================================================
    // 5. ÁRBOLES ORNAMENTALES DE LAS ALAMEDAS PERIMETRALES
    // =========================================================================

    private static void buildPalacioTrees(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState log = Blocks.DARK_OAK_LOG.getDefaultState();
        BlockState jacarandaBloom = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState oakLeaves = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        for (int[] t : MONUMENTAL_TREES) {
            int tx = centerX + t[0];
            int tz = centerZ + t[1];
            if (tx + 5 < chunkBox.getMinX() || tx - 5 > chunkBox.getMaxX()
                    || tz + 5 < chunkBox.getMinZ() || tz - 5 > chunkBox.getMaxZ()) {
                continue;
            }
            fillBox(world, chunkBox, tx - 1, baseY, tz - 1, tx + 1, baseY, tz + 1, Blocks.PODZOL.getDefaultState());
            fillBox(world, chunkBox, tx, baseY + 1, tz, tx, baseY + 7, tz, log);
            for (int ox = -4; ox <= 4; ox++) {
                for (int oz = -4; oz <= 4; oz++) {
                    for (int oy = 5; oy <= 10; oy++) {
                        double d = Math.sqrt(ox * ox + oz * oz + (oy - 7.2D) * (oy - 7.2D) * 1.4D);
                        if (d <= 4.3D && !(ox == 0 && oz == 0 && oy <= 7)) {
                            BlockState lf = ((ox + oz + oy) % 3 == 0) ? jacarandaBloom : oakLeaves;
                            setBlock(world, chunkBox, tx + ox, baseY + oy, tz + oz, lf);
                        }
                    }
                }
            }
        }
    }

    private static boolean isMonumentalPalacioTreeTrunk(int dx, int dz) {
        for (int[] t : MONUMENTAL_TREES) {
            if (dx == t[0] && dz == t[1]) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // 6. LIMPIEZA POST-VEGETACIÓN (SIN CORTES BRUSCOS NI PEDAZOS FLOTANTES)
    // =========================================================================

    public static void postCleanPalacioChunk(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        int minX = Math.max(centerX - TOTAL_RADIUS, chunkBox.getMinX() - 8);
        int maxX = Math.min(centerX + TOTAL_RADIUS, chunkBox.getMaxX() + 8);
        int minZ = Math.max(centerZ - TOTAL_RADIUS, chunkBox.getMinZ() - 8);
        int maxZ = Math.min(centerZ + TOTAL_RADIUS, chunkBox.getMaxZ() + 8);
        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int bottomY = Math.max(world.getBottomY() + 1, baseY - 28);
        int worldTopY = world.getTopYInclusive();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();

        int cachedChunkX = Integer.MIN_VALUE;
        int cachedChunkZ = Integer.MIN_VALUE;
        int cachedTopY = Math.min(worldTopY, baseY + 66);

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            boolean insideCoreX = wx >= chunkBox.getMinX() && wx <= chunkBox.getMaxX();
            int chunkX = wx >> 4;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                boolean insideCoreChunk = insideCoreX && wz >= chunkBox.getMinZ() && wz <= chunkBox.getMaxZ();
                int chunkZ = wz >> 4;
                if (!insideCoreChunk && !world.isValidForSetBlock(pos.set(wx, baseY, wz))) {
                    continue;
                }
                if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
                    cachedChunkX = chunkX;
                    cachedChunkZ = chunkZ;
                    cachedTopY = getChunkMaxClearY(world, world.getChunk(chunkX, chunkZ), baseY);
                }
                int topClearY = cachedTopY;

                double effectiveDist = getEffectiveEstateDistance(dx, dz);
                if (effectiveDist > TOTAL_RADIUS) {
                    continue;
                }

                if (effectiveDist <= ESTATE_RADIUS) {
                    // Preservar el mosaico tricolor de la Arena del Mini-Jefe en el Zócalo
                    boolean inMiniBossRing = Math.abs(dx - MINI_BOSS_SPAWN_REL_X) <= 29
                            && Math.abs(dz - MINI_BOSS_SPAWN_REL_Z) <= 29;
                    if (!inMiniBossRing) {
                        pos.set(wx, baseY, wz);
                        BlockState groundSt = world.getBlockState(pos);
                        if (groundSt.isOf(Blocks.GREEN_TERRACOTTA) || isSoilVariant(groundSt)) {
                            world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                        }
                    }

                    boolean keepOwnTrunk = isMonumentalPalacioTreeTrunk(dx, dz);
                    boolean insidePalaceFootprint = dx >= PALACE_MIN_X && dx <= PALACE_MAX_X && dz >= PALACE_MIN_Z && dz <= PALACE_MAX_Z;
                    for (int y = baseY + 1; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (isVineBlock(st)) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        } else if (!insidePalaceFootprint && isWildVegetationBlock(st) && !(keepOwnTrunk && st.isIn(BlockTags.LOGS))) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        }
                    }
                } else {
                    for (int y = bottomY; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (st.isOf(Blocks.GREEN_TERRACOTTA)) {
                            world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                            continue;
                        }
                        if (isVineBlock(st) || isClutterGroundPlant(st)) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                            continue;
                        }
                        if (effectiveDist <= TREE_CLEAR_RADIUS) {
                            if (effectiveDist <= TREE_CLEAR_RADIUS - 7) {
                                if (isWildVegetationBlock(st)) {
                                    world.setBlockState(pos, air, CLEAR_FLAGS);
                                }
                            } else if (st.isIn(BlockTags.LOGS)) {
                                world.setBlockState(pos, air, CLEAR_FLAGS);
                                clearAdjacent2x2LogColumns(world, wx, wz, centerX, centerZ, bottomY, topClearY, chunkBox);
                            } else if (isGroundHerbBlock(st)) {
                                world.setBlockState(pos, air, CLEAR_FLAGS);
                            }
                        }
                    }
                }
            }
        }

        // Fase 2: Limpiar hojas flotantes y troncos decapitados en la corona exterior
        cachedChunkX = Integer.MIN_VALUE;
        cachedChunkZ = Integer.MIN_VALUE;
        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            boolean insideCoreX = wx >= chunkBox.getMinX() && wx <= chunkBox.getMaxX();
            int chunkX = wx >> 4;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                boolean insideCoreChunk = insideCoreX && wz >= chunkBox.getMinZ() && wz <= chunkBox.getMaxZ();
                int chunkZ = wz >> 4;
                if (!insideCoreChunk && !world.isValidForSetBlock(pos.set(wx, baseY, wz))) {
                    continue;
                }
                double effectiveDist = getEffectiveEstateDistance(dx, dz);
                if (effectiveDist <= TREE_CLEAR_RADIUS - 7 || effectiveDist > TOTAL_RADIUS) {
                    continue;
                }
                if (chunkX != cachedChunkX || chunkZ != cachedChunkZ) {
                    cachedChunkX = chunkX;
                    cachedChunkZ = chunkZ;
                    cachedTopY = getChunkMaxClearY(world, world.getChunk(chunkX, chunkZ), baseY);
                }
                int topClearY = cachedTopY;

                for (int y = bottomY; y <= topClearY; y++) {
                    pos.set(wx, y, wz);
                    BlockState st = world.getBlockState(pos);
                    if (isVineBlock(st) || isClutterGroundPlant(st)) {
                        world.setBlockState(pos, air, CLEAR_FLAGS);
                        continue;
                    }
                    boolean isNonPersistentLeaf = st.isIn(BlockTags.LEAVES) && !isPersistentLeaf(st);
                    if ((st.isIn(BlockTags.LOGS) || isNonPersistentLeaf)
                            && !hasValidOuterTreeSupport(world, wx, y, wz, centerX, baseY, centerZ)) {
                        world.setBlockState(pos, air, CLEAR_FLAGS);
                    }
                }
            }
        }

        buildPalacioTrees(world, chunkBox, centerX, baseY, centerZ);
        PalacioNacionalCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
    }

    private static void clearAdjacent2x2LogColumns(
            StructureWorldAccess world,
            int wx,
            int wz,
            int centerX,
            int centerZ,
            int bottomY,
            int topClearY,
            BlockBox chunkBox
    ) {
        BlockPos.Mutable scan = new BlockPos.Mutable();
        BlockState air = Blocks.AIR.getDefaultState();
        int refY = Math.min(chunkBox.getMaxY(), chunkBox.getMinY() + 28);
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int nx = wx + ox;
                int nz = wz + oz;
                double dist = getEffectiveEstateDistance(nx - centerX, nz - centerZ);
                if (dist <= TREE_CLEAR_RADIUS || dist > TREE_CLEAR_RADIUS + 2) {
                    continue;
                }
                if (!chunkBox.contains(nx, refY, nz) && !world.isValidForSetBlock(scan.set(nx, refY, nz))) {
                    continue;
                }
                for (int cy = bottomY; cy <= topClearY; cy++) {
                    scan.set(nx, cy, nz);
                    BlockState nst = world.getBlockState(scan);
                    if (nst.isIn(BlockTags.LOGS) || isVineBlock(nst)) {
                        world.setBlockState(scan, air, CLEAR_FLAGS);
                    }
                }
            }
        }
    }

    private static boolean hasValidOuterTreeSupport(
            StructureWorldAccess world,
            int wx,
            int y,
            int wz,
            int centerX,
            int baseY,
            int centerZ
    ) {
        int bottomY = Math.max(world.getBottomY() + 1, baseY - 28);
        int topLimit = Math.min(world.getTopYInclusive(), Math.max(baseY + 72, y + 32));
        BlockPos.Mutable scan = new BlockPos.Mutable();

        for (int ox = -6; ox <= 6; ox++) {
            int tx = wx + ox;
            int tdx = tx - centerX;
            for (int oz = -6; oz <= 6; oz++) {
                int tz = wz + oz;
                int tdz = tz - centerZ;
                if (getEffectiveEstateDistance(tdx, tdz) <= TREE_CLEAR_RADIUS) {
                    continue;
                }
                if (!world.isChunkLoaded(tx >> 4, tz >> 4)) {
                    continue;
                }

                int tGroundY = findSolidGroundTopY(world, scan, tx, tz, bottomY, Math.min(topLimit, Math.max(baseY + 48, y + 2)), Integer.MIN_VALUE);
                if (tGroundY == Integer.MIN_VALUE) {
                    continue;
                }

                int rootY = -1;
                for (int ry = tGroundY + 1; ry <= Math.min(topLimit, tGroundY + 3); ry++) {
                    if (world.getBlockState(scan.set(tx, ry, tz)).isIn(BlockTags.LOGS)) {
                        rootY = ry;
                        break;
                    }
                }
                if (rootY < 0 || rootY > y + 2) {
                    continue;
                }

                int curX = tx;
                int curZ = tz;
                int topTrunkY = rootY;
                int gap = 0;
                boolean reachedNearY = (rootY >= y - 4 && rootY <= y + 2)
                        && Math.abs(wx - curX) <= 6 && Math.abs(wz - curZ) <= 6;

                for (int cy = rootY + 1; cy <= topLimit; cy++) {
                    int foundX = Integer.MIN_VALUE;
                    int foundZ = Integer.MIN_VALUE;
                    if (world.getBlockState(scan.set(curX, cy, curZ)).isIn(BlockTags.LOGS)) {
                        foundX = curX;
                        foundZ = curZ;
                    } else {
                        outerBranch:
                        for (int bx = -1; bx <= 1; bx++) {
                            for (int bz = -1; bz <= 1; bz++) {
                                if (bx == 0 && bz == 0) {
                                    continue;
                                }
                                int nx = curX + bx;
                                int nz = curZ + bz;
                                if (Math.abs(nx - tx) <= 3 && Math.abs(nz - tz) <= 3
                                        && world.isChunkLoaded(nx >> 4, nz >> 4)
                                        && world.getBlockState(scan.set(nx, cy, nz)).isIn(BlockTags.LOGS)) {
                                    foundX = nx;
                                    foundZ = nz;
                                    break outerBranch;
                                }
                            }
                        }
                    }

                    if (foundX != Integer.MIN_VALUE) {
                        curX = foundX;
                        curZ = foundZ;
                        topTrunkY = cy;
                        gap = 0;
                        if (cy >= y - 4 && cy <= y + 2
                                && Math.abs(wx - curX) <= 6 && Math.abs(wz - curZ) <= 6) {
                            reachedNearY = true;
                        }
                    } else {
                        gap++;
                        if (gap > 1) {
                            break;
                        }
                    }
                }

                if (reachedNearY && topTrunkY >= y - 4 && hasCrownLeaves(world, scan, curX, topTrunkY, curZ, topLimit)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCrownLeaves(
            StructureWorldAccess world,
            BlockPos.Mutable scan,
            int cx,
            int topTrunkY,
            int cz,
            int topLimit
    ) {
        int leavesCount = 0;
        int minY = topTrunkY - 2;
        int maxY = Math.min(topLimit, topTrunkY + 5);
        for (int ly = minY; ly <= maxY; ly++) {
            for (int ox = -3; ox <= 3; ox++) {
                for (int oz = -3; oz <= 3; oz++) {
                    int lx = cx + ox;
                    int lz = cz + oz;
                    if (!world.isChunkLoaded(lx >> 4, lz >> 4)) {
                        continue;
                    }
                    if (world.getBlockState(scan.set(lx, ly, lz)).isIn(BlockTags.LEAVES)) {
                        leavesCount++;
                        if (leavesCount >= 4) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // =========================================================================
    // HELPERS DE CONSTRUCCIÓN Y BLOQUES
    // =========================================================================

    private static void placeRug(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int minX,
            int y,
            int minZ,
            int maxX,
            int maxZ,
            BlockState centerCarpet,
            BlockState borderCarpet
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean border = x == minX || x == maxX || z == minZ || z == maxZ;
                setBlock(world, chunkBox, x, y, z, border ? borderCarpet : centerCarpet);
            }
        }
    }

    private static void placeDoubleBed(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int footX,
            int y,
            int footZ,
            Direction headFacing,
            Block bedBlock
    ) {
        BlockPos footPos = new BlockPos(footX, y, footZ);
        BlockPos headPos = footPos.offset(headFacing);
        if (chunkBox.contains(footPos)) {
            world.setBlockState(
                    footPos,
                    bedBlock.getDefaultState().with(BedBlock.FACING, headFacing).with(BedBlock.PART, BedPart.FOOT),
                    Block.NOTIFY_LISTENERS
            );
        }
        if (chunkBox.contains(headPos)) {
            world.setBlockState(
                    headPos,
                    bedBlock.getDefaultState().with(BedBlock.FACING, headFacing).with(BedBlock.PART, BedPart.HEAD),
                    Block.NOTIFY_LISTENERS
            );
        }
    }

    private static void placeLootChest(
            StructureWorldAccess world,
            BlockBox chunkBox,
            BlockPos pos,
            Direction facing,
            RegistryKey<LootTable> lootTable,
            Random random
    ) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing), Block.NOTIFY_LISTENERS);
            if (world.getBlockEntity(pos) instanceof LootableContainerBlockEntity chest) {
                chest.setLootTable(lootTable, random.nextLong());
            }
        }
    }

    private static void setBlock(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int wx,
            int wy,
            int wz,
            BlockState state
    ) {
        if (chunkBox.contains(wx, wy, wz)) {
            world.setBlockState(new BlockPos(wx, wy, wz), state, Block.NOTIFY_LISTENERS);
        }
    }

    private static void fillBox(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            BlockState state
    ) {
        int x0 = Math.max(minX, chunkBox.getMinX());
        int x1 = Math.min(maxX, chunkBox.getMaxX());
        int y0 = Math.max(minY, chunkBox.getMinY());
        int y1 = Math.min(maxY, chunkBox.getMaxY());
        int z0 = Math.max(minZ, chunkBox.getMinZ());
        int z1 = Math.min(maxZ, chunkBox.getMaxZ());

        if (x0 > x1 || y0 > y1 || z0 > z1) {
            return;
        }

        BlockPos.Mutable mpos = new BlockPos.Mutable();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    mpos.set(x, y, z);
                    world.setBlockState(mpos, state, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    private static int getChunkMaxClearY(StructureWorldAccess world, Chunk chunk, int baseY) {
        int highestIdx = chunk.getHighestNonEmptySection();
        int sectionTop = highestIdx >= 0 ? (chunk.sectionIndexToCoord(highestIdx) << 4) + 15 : baseY;
        return Math.min(world.getTopYInclusive(), Math.max(baseY + 64, sectionTop));
    }

    private static boolean isPersistentLeaf(BlockState st) {
        return st.getBlock() instanceof LeavesBlock
                && st.contains(LeavesBlock.PERSISTENT)
                && st.get(LeavesBlock.PERSISTENT);
    }

    private static boolean isVineBlock(BlockState st) {
        return st.isOf(Blocks.VINE)
                || st.isOf(Blocks.CAVE_VINES)
                || st.isOf(Blocks.CAVE_VINES_PLANT)
                || st.isOf(Blocks.WEEPING_VINES)
                || st.isOf(Blocks.WEEPING_VINES_PLANT)
                || st.isOf(Blocks.TWISTING_VINES)
                || st.isOf(Blocks.TWISTING_VINES_PLANT);
    }

    private static boolean isClutterGroundPlant(BlockState st) {
        return st.isOf(Blocks.BAMBOO)
                || st.isOf(Blocks.BAMBOO_SAPLING)
                || st.isOf(Blocks.COCOA)
                || st.isOf(Blocks.MANGROVE_ROOTS)
                || st.isOf(Blocks.BEE_NEST)
                || st.isOf(Blocks.SHROOMLIGHT)
                || st.isOf(Blocks.MELON)
                || st.isOf(Blocks.PUMPKIN)
                || st.isOf(Blocks.BIG_DRIPLEAF)
                || st.isOf(Blocks.BIG_DRIPLEAF_STEM)
                || st.isOf(Blocks.SMALL_DRIPLEAF)
                || st.isOf(Blocks.LARGE_FERN);
    }

    private static boolean isGroundHerbBlock(BlockState st) {
        return st.isOf(Blocks.SHORT_GRASS)
                || st.isOf(Blocks.TALL_GRASS)
                || st.isOf(Blocks.FERN)
                || st.isOf(Blocks.DEAD_BUSH)
                || st.isOf(Blocks.MOSS_CARPET);
    }

    private static boolean isWildVegetationBlock(BlockState st) {
        if (isPersistentLeaf(st)) {
            return false;
        }
        return st.isIn(BlockTags.LOGS)
                || st.isIn(BlockTags.LEAVES)
                || isGroundHerbBlock(st)
                || isClutterGroundPlant(st);
    }

    private static boolean isSoilVariant(BlockState st) {
        return st.isOf(Blocks.DIRT)
                || st.isOf(Blocks.PODZOL)
                || st.isOf(Blocks.COARSE_DIRT)
                || st.isOf(Blocks.ROOTED_DIRT)
                || st.isOf(Blocks.MUD);
    }
}
