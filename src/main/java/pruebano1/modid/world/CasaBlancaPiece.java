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
import pruebano1.modid.block.CasinoSofaBlock;

/**
 * Generador colosal (448x448 bloques) de la 3.ª gran Dungeon: "La Casa Blanca".
 * - Terrenos exteriores extremadamente detallados por los 4 lados: reja presidencial de hierro y mármol,
 *   casetas del Servicio Secreto, 4 torres de vigilancia, "Buzón de la Casa Blanca" exactamente en la entrada Sur,
 *   Jardín Sur con Gran Fuente y rotondas florales, Rose Garden con Helipuerto ("Marine One") y limusinas blindadas,
 *   Jardín Este (Jacqueline Kennedy) con estanque reflectante y glorieta, y Explanada Norte con fuente y monumentos.
 * - Arquitectura neoclásica completa de La Casa Blanca: Residencia Ejecutiva central de 2 plantas + ático,
 *   Pórtico Sur semicircular con Balcón Truman, Pórtico Norte con frontón triangular monumental,
 *   Columnatas Este y Oeste, Ala Oeste (con Oficina Oval sobresaliendo al Sur) y Ala Este, tejado de pizarra,
 *   balaustradas y gran mástil central con la Bandera de EE. UU. en 3D.
 * - Interior lujoso completamente amueblado y la enorme Gran Zona Principal de Conferencias (arena de doble altura)
 *   con escenario Norte, podio presidencial y enormes banderas de EE. UU. donde aparece Charlie Kirk al ser invocado.
 */
public class CasaBlancaPiece extends StructurePiece {

    /** Radio cuadrado interior plano que cubre toda la reja (160x160) y torres de esquina (±164). */
    public static final int FLAT_BOX_HALF = 166;
    /** Radio interior de la propiedad (186 bloques = 372x372 bloques). */
    public static final int ESTATE_RADIUS = 186;
    /** Franja perimetral de 38 bloques para talud suave (total 448x448 bloques). */
    public static final int GRADE_RANGE = 38;
    public static final int TOTAL_RADIUS = ESTATE_RADIUS + GRADE_RANGE; // 224 -> 448x448 bloques
    /** Radio de despeje de árboles salvajes (cubre la propiedad + borde de transición). */
    public static final int TREE_CLEAR_RADIUS = ESTATE_RADIUS + 12; // 198
    /** Radio de bloqueo con placeholder GREEN_TERRACOTTA para evitar troncos cuyo follaje invada el perímetro. */
    public static final int TREE_BLOCKER_RADIUS = ESTATE_RADIUS + 20; // 206

    /** Posición relativa del "Buzón de la Casa Blanca" en la entrada principal Sur (lejos del edificio principal). */
    public static final int MAILBOX_REL_X = 14;
    public static final int MAILBOX_REL_Z = 156;

    /** Posición relativa de aparición de Charlie Kirk sobre el escenario del Salón Principal de Conferencias. */
    public static final int CONFERENCE_SPAWN_REL_X = 0;
    public static final int CONFERENCE_SPAWN_REL_Y = 2;
    public static final int CONFERENCE_SPAWN_REL_Z = -14;

    /** Flags para limpieza sin soltar ítems ni provocar cascadas de lianas/vegetación. */
    private static final int CLEAR_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;

    private final int centerX;
    private final int baseY;
    private final int centerZ;

    public CasaBlancaPiece(int centerX, int baseY, int centerZ) {
        super(
                ModStructures.CASA_BLANCA_PIECE,
                0,
                new BlockBox(
                        centerX - 140,
                        baseY - 24,
                        centerZ - 140,
                        centerX + 140,
                        baseY + 180,
                        centerZ + 140
                )
        );
        this.centerX = centerX;
        this.baseY = baseY;
        this.centerZ = centerZ;
        this.setOrientation(Direction.SOUTH);
    }

    public CasaBlancaPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.CASA_BLANCA_PIECE, nbt);
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
        // Generado íntegramente por generateChunkForWhiteHouse desde CasaBlancaFeature
    }

    public static void generateChunkForWhiteHouse(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        generateChunkForWhiteHouse(world, chunkBox, centerX, baseY, centerZ, random, false);
    }

    public static void generateChunkForWhiteHouse(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random,
            boolean useTreeBlockerPlaceholder
    ) {
        // 1. Nivelación de los 448x448 bloques y transición suave sin cortes bruscos con el terreno exterior
        prepareEstateTerrainAndBlending(world, chunkBox, centerX, baseY, centerZ, useTreeBlockerPlaceholder);

        // 2. Reja perimetral presidencial, casetas del Servicio Secreto, jardines, fuentes, helipuerto Marine One, limusinas y Buzón
        buildWhiteHouseGroundsAndGardens(world, chunkBox, centerX, baseY, centerZ, random, useTreeBlockerPlaceholder);

        // 3. Casco arquitectónico de La Casa Blanca (Residencia Central, Pórticos Norte y Sur, Pabellones de enlace, Alas Este/Oeste, Oficina Oval y Tejado con Bandera)
        buildWhiteHouseExteriorShellAndPorticos(world, chunkBox, centerX, baseY, centerZ);

        // 4. Interior extremadamente detallado e iluminado: Gran Zona Principal de Conferencias (arena de Charlie Kirk) + Alas + 2.º Piso
        buildWhiteHouseInteriorAndConferenceHall(world, chunkBox, centerX, baseY, centerZ, random);

        // 5. Árboles monumentales de los jardines Norte, Sur, Este y Oeste
        buildWhiteHouseTrees(world, chunkBox, centerX, baseY, centerZ);

        // 6. Aplicar cualquier modificación personalizada guardada con "/casablanca guardar"
        if (!useTreeBlockerPlaceholder) {
            CasaBlancaCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
        }
    }

    /**
     * Reconstruye y repara un chunk ya generado en un mundo guardado (iluminación, arquitectura unificada,
     * eliminación de lianas/terreno flotante y despeje selectivo de vegetación).
     */
    public static void upgradeChunkInPlace(
            ServerWorld world,
            int chunkX,
            int chunkZ,
            CasaBlancaStructure.WhiteHouseAnchor anchor
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
        generateChunkForWhiteHouse(
                world,
                chunkBox,
                anchor.centerX(),
                anchor.baseY(),
                anchor.centerZ(),
                world.getRandom(),
                false
        );
        postCleanWhiteHouseChunk(world, chunkBox, anchor.centerX(), anchor.baseY(), anchor.centerZ());
    }

    // =========================================================================
    // 1. TERRENO Y TRANSICIÓN SUAVE SIN CORTES BRUSCOS
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
        int cachedChunkTopClearY = Math.min(worldTopY, baseY + 60);

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
                int capY = Math.min(worldTopY, baseY + (int) Math.floor((distOut * distOut) / 14.0D + distOut * 0.55D));
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
                    // Eliminar terreno sólido flotando por encima de solidTopY sin tocar troncos ni copas de árboles sanos
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

                int slopeTargetY = baseY - (int) Math.ceil(distOut * 0.65D);
                pos.set(wx, slopeTargetY, wz);
                BlockState atSlope = world.getBlockState(pos);
                if (atSlope.isAir() || atSlope.isLiquid() || atSlope.isReplaceable()) {
                    for (int y = slopeTargetY; y >= Math.max(bottomY, slopeTargetY - 22); y--) {
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
                    // effectiveDist > TREE_BLOCKER_RADIUS: quitar solo lianas y nieve sobre huecos, sin cortar copas de árboles
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
    // 2. REJA PERIMETRAL, CASETAS, JARDINES, FUENTES, MARINE ONE Y BUZÓN
    // =========================================================================

    private static void buildWhiteHouseGroundsAndGardens(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random,
            boolean useTreeBlockerPlaceholder
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState quartzPillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState andesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState ironBars = Blocks.IRON_BARS.getDefaultState();
        BlockState goldBlock = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState lantern = Blocks.LANTERN.getDefaultState();
        BlockState oakLeaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState azaleaLeaves = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState floweringAzalea = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        int fenceRadius = 160;

        for (int wx = chunkBox.getMinX(); wx <= chunkBox.getMaxX(); wx++) {
            int dx = wx - centerX;
            for (int wz = chunkBox.getMinZ(); wz <= chunkBox.getMaxZ(); wz++) {
                int dz = wz - centerZ;

                // 1. Reja presidencial perimetral con zócalo de cuarzo, rejas de hierro y faroles cada 8 bloques
                boolean onFenceX = Math.abs(dx) == fenceRadius && Math.abs(dz) <= fenceRadius;
                boolean onFenceZ = Math.abs(dz) == fenceRadius && Math.abs(dx) <= fenceRadius;
                boolean isSouthGate = dz == fenceRadius && Math.abs(dx) <= 13;
                boolean isNorthGate = dz == -fenceRadius && Math.abs(dx) <= 13;
                boolean isWestGate = dx == -fenceRadius && Math.abs(dz) <= 8;
                boolean isEastGate = dx == fenceRadius && Math.abs(dz) <= 8;

                if ((onFenceX || onFenceZ) && !isSouthGate && !isNorthGate && !isWestGate && !isEastGate) {
                    boolean isPillar = (dx % 8 == 0) || (dz % 8 == 0);
                    if (isPillar) {
                        setBlock(world, chunkBox, wx, baseY + 1, wz, chiseledQuartz);
                        setBlock(world, chunkBox, wx, baseY + 2, wz, quartz);
                        setBlock(world, chunkBox, wx, baseY + 3, wz, quartzPillar);
                        setBlock(world, chunkBox, wx, baseY + 4, wz, quartzPillar);
                        setBlock(world, chunkBox, wx, baseY + 5, wz, chiseledQuartz);
                        setBlock(world, chunkBox, wx, baseY + 6, wz, ((dx + dz) % 16 == 0) ? seaLantern : Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
                    } else {
                        setBlock(world, chunkBox, wx, baseY + 1, wz, quartz);
                        setBlock(world, chunkBox, wx, baseY + 2, wz, diorite);
                        setBlock(world, chunkBox, wx, baseY + 3, wz, ironBars);
                        setBlock(world, chunkBox, wx, baseY + 4, wz, ironBars);
                        setBlock(world, chunkBox, wx, baseY + 5, wz, ironBars);
                    }
                }

                // 2. Calzadas, avenidas y paseos pavimentados en los 4 frentes
                boolean inSouthGateRoad = dz >= 134 && dz <= 176 && Math.abs(dx) <= 12;
                boolean inNorthGateRoad = dz >= -176 && dz <= -134 && Math.abs(dx) <= 12;

                // Rotonda elíptica Sur (dz = 42..138) y explanada amplia del Pórtico Sur
                double southDriveNorm = ((double) dx * dx) / (78.0D * 78.0D) + ((double) (dz - 82) * (dz - 82)) / (54.0D * 54.0D);
                boolean inSouthEllipticalDrive = dz >= 42 && dz <= 138 && southDriveNorm <= 1.05D && southDriveNorm >= 0.72D;
                boolean inSouthPorticoApron = dz >= 42 && dz <= 58 && Math.abs(dx) <= 42;

                // Rotonda elíptica Norte (dz = -138..-42) y explanada amplia del Pórtico Norte
                double northDriveNorm = ((double) dx * dx) / (78.0D * 78.0D) + ((double) (dz + 82) * (dz + 82)) / (54.0D * 54.0D);
                boolean inNorthEllipticalDrive = dz >= -138 && dz <= -42 && northDriveNorm <= 1.05D && northDriveNorm >= 0.72D;
                boolean inNorthPorticoApron = dz >= -58 && dz <= -42 && Math.abs(dx) <= 42;

                // Caminos Este-Oeste y bulevares laterales conectados con las rotondas Norte/Sur sin solapar las Alas (|dx|<=108) ni la Glorieta (dx=+108..+124, dz=+68..+84)
                boolean inWestApproach = dz >= -8 && dz <= 8 && dx >= -176 && dx <= -110;
                boolean inEastApproach = dz >= -8 && dz <= 8 && dx >= 110 && dx <= 176;
                boolean inWestMotorcadeDrive = dx >= -128 && dx <= -112 && dz >= -86 && dz <= 86;
                boolean inWestEllipseLink = dx >= -128 && dx <= -70 && (Math.abs(dz - 82) <= 4 || Math.abs(dz + 82) <= 4);
                boolean inEastWalkway = dx >= 114 && dx <= 122 && dz >= -86 && dz <= 67;
                boolean inEastEllipseLink = (dx >= 70 && dx <= 122 && Math.abs(dz + 82) <= 4)
                        || (dx >= 70 && dx <= 92 && Math.abs(dz - 78) <= 4)
                        || (dx >= 94 && dx <= 122 && dz >= 62 && dz <= 67);
                boolean inSouthLawnPath = Math.abs(dx) <= 5 && dz >= 56 && dz <= 134;
                boolean inNorthLawnPath = Math.abs(dx) <= 5 && dz >= -134 && dz <= -56;

                if (inSouthGateRoad || inNorthGateRoad || inSouthEllipticalDrive || inSouthPorticoApron
                        || inNorthEllipticalDrive || inNorthPorticoApron || inWestApproach || inEastApproach
                        || inWestMotorcadeDrive || inWestEllipseLink || inEastWalkway || inEastEllipseLink) {
                    boolean curb = (inSouthGateRoad && Math.abs(dx) == 12)
                            || (inNorthGateRoad && Math.abs(dx) == 12)
                            || (inWestApproach && Math.abs(dz) == 8)
                            || (inEastApproach && Math.abs(dz) == 8);
                    BlockState roadBlock = curb ? quartz
                            : (((dx + dz) % 12 == 0) ? andesite
                            : (((dx * 17 + dz * 29) & 7) == 0 ? blackstone : Blocks.GRAY_CONCRETE.getDefaultState()));
                    setBlock(world, chunkBox, wx, baseY, wz, roadBlock);
                } else if (inSouthLawnPath || inNorthLawnPath) {
                    boolean border = Math.abs(dx) == 5;
                    setBlock(world, chunkBox, wx, baseY, wz, border ? quartz : (Math.abs(dx) <= 1 ? diorite : smoothStone));
                }
            }
        }

        // 3. Portales monumentales Norte y Sur + 4 Torres/Pabellones en las esquinas
        buildMonumentalGatehouse(world, chunkBox, centerX, baseY, centerZ + fenceRadius, true, random);
        buildMonumentalGatehouse(world, chunkBox, centerX, baseY, centerZ - fenceRadius, false, random);
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                buildCornerGuardTower(world, chunkBox, centerX + sx * fenceRadius, baseY, centerZ + sz * fenceRadius);
            }
        }

        // 4. BUZÓN DE LA CASA BLANCA (exactamente en la entrada principal Sur, lejos de la Casa Blanca)
        int mbX = centerX + MAILBOX_REL_X;
        int mbZ = centerZ + MAILBOX_REL_Z;
        fillBox(world, chunkBox, mbX - 2, baseY, mbZ - 2, mbX + 2, baseY, mbZ + 2, quartz);
        fillBox(world, chunkBox, mbX - 1, baseY, mbZ - 1, mbX + 1, baseY, mbZ + 1, Blocks.BLUE_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, mbX, baseY, mbZ, goldBlock);
        setBlock(world, chunkBox, mbX, baseY + 1, mbZ,
                ModBlocks.WHITE_HOUSE_MAILBOX.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, mbX - 2, baseY + 1, mbZ, chiseledQuartz);
        setBlock(world, chunkBox, mbX - 2, baseY + 2, mbZ, lantern);
        setBlock(world, chunkBox, mbX + 2, baseY + 1, mbZ, chiseledQuartz);
        setBlock(world, chunkBox, mbX + 2, baseY + 2, mbZ, lantern);

        // 5. Gran Fuente Presidencial Sur (dz = +98) con rotonda patriótica de flores rojas, blancas y azules
        buildPresidentialFountain(world, chunkBox, centerX, baseY, centerZ + 98, 16, true, useTreeBlockerPlaceholder);
        // Fuente Norte (dz = -95)
        buildPresidentialFountain(world, chunkBox, centerX, baseY, centerZ - 95, 13, false, useTreeBlockerPlaceholder);

        // 6. Jardín de las Rosas (Rose Garden, Suroeste: dx = -102..-58, dz = +32..+66)
        buildRoseGarden(world, chunkBox, centerX - 80, baseY, centerZ + 49, oakLeaves, azaleaLeaves);

        // 7. Helipuerto Presidencial y Helicóptero "Marine One" (dx = -105, dz = +105)
        buildMarineOneHelipad(world, chunkBox, centerX - 105, baseY, centerZ + 105);

        // 8. Dos Limusinas Blindadas Presidenciales ("La Bestia") centradas en el bulevar Oeste (dx = -120)
        buildPresidentialLimousine(world, chunkBox, centerX - 120, baseY + 1, centerZ + 44);
        buildPresidentialLimousine(world, chunkBox, centerX - 120, baseY + 1, centerZ + 66);

        // 9. Jardín Este (Jacqueline Kennedy Garden): Estanque reflectante + Glorieta clásica
        buildJacquelineKennedyGarden(world, chunkBox, centerX + 82, baseY, centerZ + 76, oakLeaves, floweringAzalea);

        // 10. Explanada Norte: Tarimas de prensa exteriores + Monumentos neoclásicos
        buildNorthPressAndMonuments(world, chunkBox, centerX, baseY, centerZ);

        // 11. Iluminación exterior completa: farolas ornamentales en avenidas/senderos y reflectores en fachadas
        buildWhiteHouseExteriorLighting(world, chunkBox, centerX, baseY, centerZ);
    }

    private static void buildMonumentalGatehouse(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int gx,
            int baseY,
            int gz,
            boolean isSouthMainGate,
            Random random
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState blue = Blocks.BLUE_CONCRETE.getDefaultState();
        BlockState slate = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();

        // Grandes pilares flanqueando la entrada
        for (int side : new int[]{-14, 14}) {
            fillBox(world, chunkBox, gx + side - 1, baseY + 1, gz - 1, gx + side + 1, baseY + 9, gz + 1, quartz);
            fillBox(world, chunkBox, gx + side - 2, baseY + 10, gz - 2, gx + side + 2, baseY + 10, gz + 2, chiseled);
            setBlock(world, chunkBox, gx + side, baseY + 11, gz, gold);
            setBlock(world, chunkBox, gx + side, baseY + 12, gz, Blocks.SEA_LANTERN.getDefaultState());
            // Banderas patrióticas en los pilares
            Direction flagDir = isSouthMainGate ? Direction.SOUTH : Direction.NORTH;
            setBlock(world, chunkBox, gx + side, baseY + 7, gz + (isSouthMainGate ? 2 : -2),
                    Blocks.BLUE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, flagDir));
            setBlock(world, chunkBox, gx + side - 1, baseY + 7, gz + (isSouthMainGate ? 2 : -2),
                    Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, flagDir));
            setBlock(world, chunkBox, gx + side + 1, baseY + 7, gz + (isSouthMainGate ? 2 : -2),
                    Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, flagDir));
        }

        // Arco superior monumental sobre la puerta
        fillBox(world, chunkBox, gx - 15, baseY + 9, gz - 1, gx + 15, baseY + 10, gz + 1, quartz);
        fillBox(world, chunkBox, gx - 6, baseY + 11, gz - 1, gx + 6, baseY + 12, gz + 1, chiseled);
        fillBox(world, chunkBox, gx - 2, baseY + 11, gz - 1, gx + 2, baseY + 11, gz + 1, blue);
        setBlock(world, chunkBox, gx, baseY + 12, gz, gold);

        // Casetas de guardia del Servicio Secreto a ambos lados
        for (int side : new int[]{-1, 1}) {
            int bx = gx + side * 20;
            fillBox(world, chunkBox, bx - 3, baseY, gz - 3, bx + 3, baseY, gz + 3, quartz);
            fillBox(world, chunkBox, bx - 3, baseY + 1, gz - 3, bx + 3, baseY + 4, gz + 3, quartz);
            fillBox(world, chunkBox, bx - 2, baseY + 1, gz - 2, bx + 2, baseY + 4, gz + 2, Blocks.AIR.getDefaultState());
            // Ventanas panorámicas de caseta
            fillBox(world, chunkBox, bx - 2, baseY + 2, gz - 3, bx + 2, baseY + 3, gz - 3, glass);
            fillBox(world, chunkBox, bx - 2, baseY + 2, gz + 3, bx + 2, baseY + 3, gz + 3, glass);
            fillBox(world, chunkBox, bx - 3, baseY + 2, gz - 2, bx - 3, baseY + 3, gz + 2, glass);
            fillBox(world, chunkBox, bx + 3, baseY + 2, gz - 2, bx + 3, baseY + 3, gz + 2, glass);
            // Puerta hacia el camino central
            int doorX = bx - side * 3;
            placeSingleDoor(world, chunkBox, doorX, baseY + 1, gz, side < 0 ? Direction.WEST : Direction.EAST, DoorHinge.LEFT);
            // Tejado inclinado
            fillBox(world, chunkBox, bx - 4, baseY + 5, gz - 4, bx + 4, baseY + 5, gz + 4, chiseled);
            fillBox(world, chunkBox, bx - 3, baseY + 6, gz - 3, bx + 3, baseY + 6, gz + 3, slate);
            setBlock(world, chunkBox, bx, baseY + 7, gz, Blocks.LANTERN.getDefaultState());
            // Escritorio, monitor y cofre de suministros de guardia
            setBlock(world, chunkBox, bx, baseY + 1, gz + (isSouthMainGate ? 1 : -1), Blocks.DARK_OAK_PLANKS.getDefaultState());
            setBlock(world, chunkBox, bx, baseY + 2, gz + (isSouthMainGate ? 1 : -1), Blocks.BLACK_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, bx, baseY + 4, gz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
            if (isSouthMainGate && side == -1) {
                placeLootChest(world, chunkBox, new BlockPos(bx - 1, baseY + 1, gz - 1), Direction.SOUTH, LootTables.SIMPLE_DUNGEON_CHEST, random);
            }
        }
        // Pilar decorativo interno bajo el arco
        setBlock(world, chunkBox, gx - 13, baseY + 1, gz, pillar);
        setBlock(world, chunkBox, gx + 13, baseY + 1, gz, pillar);
    }

    private static void buildCornerGuardTower(StructureWorldAccess world, BlockBox chunkBox, int tx, int baseY, int tz) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState slate = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();

        fillBox(world, chunkBox, tx - 3, baseY, tz - 3, tx + 3, baseY, tz + 3, quartz);
        fillBox(world, chunkBox, tx - 3, baseY + 1, tz - 3, tx + 3, baseY + 6, tz + 3, quartz);
        fillBox(world, chunkBox, tx - 2, baseY + 1, tz - 2, tx + 2, baseY + 6, tz + 2, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, tx - 1, baseY + 3, tz - 3, tx + 1, baseY + 4, tz - 3, glass);
        fillBox(world, chunkBox, tx - 1, baseY + 3, tz + 3, tx + 1, baseY + 4, tz + 3, glass);
        fillBox(world, chunkBox, tx - 3, baseY + 3, tz - 1, tx - 3, baseY + 4, tz + 1, glass);
        fillBox(world, chunkBox, tx + 3, baseY + 3, tz - 1, tx + 3, baseY + 4, tz + 1, glass);
        fillBox(world, chunkBox, tx - 4, baseY + 7, tz - 4, tx + 4, baseY + 7, tz + 4, chiseled);
        fillBox(world, chunkBox, tx - 3, baseY + 8, tz - 3, tx + 3, baseY + 8, tz + 3, slate);
        fillBox(world, chunkBox, tx - 1, baseY + 9, tz - 1, tx + 1, baseY + 9, tz + 1, slate);
        setBlock(world, chunkBox, tx, baseY + 10, tz, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, tx, baseY + 6, tz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
    }

    private static void buildPresidentialFountain(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int fx,
            int baseY,
            int fz,
            int radius,
            boolean withFlowerRings,
            boolean useTreeBlockerPlaceholder
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState prismarine = Blocks.PRISMARINE_BRICKS.getDefaultState();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (withFlowerRings && d > radius - 3.2D && d <= radius) {
                    if (useTreeBlockerPlaceholder) {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz, Blocks.GREEN_TERRACOTTA.getDefaultState());
                    } else {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz, Blocks.GRASS_BLOCK.getDefaultState());
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, getPatrioticRingFlower(d));
                    }
                    continue;
                }

                double basinMax = withFlowerRings ? (radius - 3.2D) : radius;
                if (d <= basinMax) {
                    if (d >= basinMax - 1.5D) {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz, chiseled);
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, quartz);
                    } else {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz,
                                (dx % 3 == 0 && dz % 3 == 0) ? seaLantern : prismarine);
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, water);
                    }
                }
            }
        }
        // Columna esculpida con doble taza de agua
        for (int y = 1; y <= 8; y++) {
            setBlock(world, chunkBox, fx, baseY + y, fz, chiseled);
        }
        fillBox(world, chunkBox, fx - 3, baseY + 4, fz - 3, fx + 3, baseY + 4, fz + 3, quartz);
        fillBox(world, chunkBox, fx - 2, baseY + 5, fz - 2, fx + 2, baseY + 5, fz + 2, water);
        fillBox(world, chunkBox, fx - 1, baseY + 7, fz - 1, fx + 1, baseY + 7, fz + 1, quartz);
        setBlock(world, chunkBox, fx, baseY + 9, fz, water);
    }

    private static void buildRoseGarden(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int baseY,
            int cz,
            BlockState hedgeLeaf,
            BlockState accentLeaf
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pebble = Blocks.POLISHED_DIORITE.getDefaultState();

        // Paseo pavimentado en cruz y marco blanco
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -14; dz <= 14; dz++) {
                if (Math.abs(dx) <= 2 || Math.abs(dz) <= 2 || Math.abs(dx) == 18 || Math.abs(dz) == 14) {
                    setBlock(world, chunkBox, cx + dx, baseY, cz + dz, (Math.abs(dx) == 18 || Math.abs(dz) == 14) ? quartz : pebble);
                }
            }
        }

        // Cuatro cuadrantes con setos y rosales
        for (int qx : new int[]{-1, 1}) {
            for (int qz : new int[]{-1, 1}) {
                int x0 = cx + (qx < 0 ? -16 : 4);
                int x1 = cx + (qx < 0 ? -4 : 16);
                int z0 = cz + (qz < 0 ? -12 : 4);
                int z1 = cz + (qz < 0 ? -4 : 12);
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean border = x == x0 || x == x1 || z == z0 || z == z1;
                        if (border) {
                            setBlock(world, chunkBox, x, baseY + 1, z, ((x + z) % 4 == 0) ? accentLeaf : hedgeLeaf);
                        } else if ((x + z) % 2 == 0) {
                            BlockState rose = ((x + z) % 4 == 0)
                                    ? Blocks.ROSE_BUSH.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER)
                                    : Blocks.PEONY.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER);
                            setBlock(world, chunkBox, x, baseY + 1, z, rose);
                            setBlock(world, chunkBox, x, baseY + 2, z, rose.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
                        }
                    }
                }
            }
        }

        // Pérgola blanca en el borde Oeste del Jardín de las Rosas
        for (int z = -12; z <= 12; z += 6) {
            for (int y = 1; y <= 5; y++) {
                setBlock(world, chunkBox, cx - 19, baseY + y, cz + z, Blocks.QUARTZ_PILLAR.getDefaultState());
            }
            setBlock(world, chunkBox, cx - 19, baseY + 6, cz + z, Blocks.LANTERN.getDefaultState());
        }
        fillBox(world, chunkBox, cx - 19, baseY + 5, cz - 13, cx - 19, baseY + 5, cz + 13, quartz);
    }

    private static void buildMarineOneHelipad(StructureWorldAccess world, BlockBox chunkBox, int hx, int baseY, int hz) {
        BlockState darkPad = Blocks.GRAY_CONCRETE.getDefaultState();
        BlockState ringPad = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState yellow = Blocks.YELLOW_CONCRETE.getDefaultState();

        for (int dx = -16; dx <= 16; dx++) {
            for (int dz = -16; dz <= 16; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 16.2D) {
                    BlockState tile = d >= 14.4D ? ringPad : darkPad;
                    if (d >= 12.5D && d < 14.4D && ((int) Math.round(Math.atan2(dz, dx) * 6.0D) % 2 == 0)) {
                        tile = yellow;
                    }
                    setBlock(world, chunkBox, hx + dx, baseY, hz + dz, tile);
                    if (d >= 15.0D && ((dx + dz) % 6 == 0)) {
                        setBlock(world, chunkBox, hx + dx, baseY + 1, hz + dz, Blocks.SEA_LANTERN.getDefaultState());
                    }
                }
            }
        }
        // Letra "H" amarilla en el centro del helipuerto
        fillBox(world, chunkBox, hx - 4, baseY, hz - 5, hx - 3, baseY, hz + 5, yellow);
        fillBox(world, chunkBox, hx + 3, baseY, hz - 5, hx + 4, baseY, hz + 5, yellow);
        fillBox(world, chunkBox, hx - 2, baseY, hz - 1, hx + 2, baseY, hz + 1, yellow);

        // Helicóptero presidencial "Marine One" (verde oscuro presidencial + techo blanco + rotor)
        BlockState greenHull = Blocks.GREEN_CONCRETE.getDefaultState();
        BlockState whiteTop = Blocks.WHITE_CONCRETE.getDefaultState();
        BlockState glass = Blocks.BLACK_STAINED_GLASS.getDefaultState();
        BlockState blackMetal = Blocks.POLISHED_BLACKSTONE.getDefaultState();

        // Patines/ruedas de aterrizaje
        fillBox(world, chunkBox, hx - 3, baseY + 1, hz - 5, hx - 3, baseY + 1, hz + 4, blackMetal);
        fillBox(world, chunkBox, hx + 3, baseY + 1, hz - 5, hx + 3, baseY + 1, hz + 4, blackMetal);
        setBlock(world, chunkBox, hx - 2, baseY + 2, hz - 2, blackMetal);
        setBlock(world, chunkBox, hx + 2, baseY + 2, hz - 2, blackMetal);
        setBlock(world, chunkBox, hx - 2, baseY + 2, hz + 2, blackMetal);
        setBlock(world, chunkBox, hx + 2, baseY + 2, hz + 2, blackMetal);

        // Fuselaje principal
        fillBox(world, chunkBox, hx - 2, baseY + 2, hz - 6, hx + 2, baseY + 4, hz + 4, greenHull);
        fillBox(world, chunkBox, hx - 1, baseY + 3, hz - 7, hx + 1, baseY + 4, hz - 7, glass);
        fillBox(world, chunkBox, hx - 2, baseY + 3, hz - 6, hx + 2, baseY + 4, hz - 4, glass);
        fillBox(world, chunkBox, hx - 2, baseY + 5, hz - 5, hx + 2, baseY + 5, hz + 4, whiteTop);
        // Cola del helicóptero
        fillBox(world, chunkBox, hx - 1, baseY + 4, hz + 5, hx + 1, baseY + 5, hz + 11, greenHull);
        fillBox(world, chunkBox, hx, baseY + 5, hz + 10, hx, baseY + 8, hz + 12, greenHull);
        setBlock(world, chunkBox, hx + 1, baseY + 7, hz + 11, whiteTop);
        // Mástil y 4 aspas del rotor principal
        fillBox(world, chunkBox, hx, baseY + 6, hz - 1, hx, baseY + 7, hz - 1, blackMetal);
        fillBox(world, chunkBox, hx - 10, baseY + 7, hz - 1, hx + 10, baseY + 7, hz - 1, blackMetal);
        fillBox(world, chunkBox, hx, baseY + 7, hz - 11, hx, baseY + 7, hz + 9, blackMetal);
        setBlock(world, chunkBox, hx, baseY + 8, hz - 1, Blocks.RED_CONCRETE.getDefaultState());
    }

    private static void buildPresidentialLimousine(StructureWorldAccess world, BlockBox chunkBox, int lx, int by, int lz) {
        BlockState black = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState darkGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState wheel = Blocks.COAL_BLOCK.getDefaultState();

        fillBox(world, chunkBox, lx - 2, by, lz - 6, lx + 2, by, lz + 6, black);
        fillBox(world, chunkBox, lx - 2, by + 1, lz - 3, lx + 2, by + 1, lz + 4, darkGlass);
        fillBox(world, chunkBox, lx - 1, by + 2, lz - 2, lx + 1, by + 2, lz + 3, black);
        // Ruedas y luces
        for (int wz : new int[]{-5, 0, 5}) {
            setBlock(world, chunkBox, lx - 2, by, lz + wz, wheel);
            setBlock(world, chunkBox, lx + 2, by, lz + wz, wheel);
        }
        setBlock(world, chunkBox, lx - 1, by, lz - 6, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, lx + 1, by, lz - 6, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, lx - 1, by, lz + 6, Blocks.RED_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, lx + 1, by, lz + 6, Blocks.RED_CONCRETE.getDefaultState());
        // Banderitas presidenciales en el cofre delantero
        setBlock(world, chunkBox, lx - 2, by + 1, lz - 5, Blocks.BLUE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, lx + 2, by + 1, lz - 5, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.EAST));
    }

    private static void buildJacquelineKennedyGarden(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int gx,
            int baseY,
            int gz,
            BlockState hedgeLeaf,
            BlockState flowerLeaf
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();

        // Estanque reflectante largo
        fillBox(world, chunkBox, gx - 7, baseY, gz - 28, gx + 7, baseY, gz + 28, quartz);
        fillBox(world, chunkBox, gx - 5, baseY, gz - 26, gx + 5, baseY, gz + 26, Blocks.PRISMARINE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, gx - 5, baseY + 1, gz - 26, gx + 5, baseY + 1, gz + 26, water);
        // Borde bajo del estanque y parterres de flores laterales
        for (int dz = -28; dz <= 28; dz++) {
            setBlock(world, chunkBox, gx - 6, baseY + 1, gz + dz, quartz);
            setBlock(world, chunkBox, gx + 6, baseY + 1, gz + dz, quartz);
            if (Math.abs(dz) == 27) {
                fillBox(world, chunkBox, gx - 6, baseY + 1, gz + dz, gx + 6, baseY + 1, gz + dz, quartz);
            }
            if (dz % 4 == 0) {
                setBlock(world, chunkBox, gx - 10, baseY + 1, gz + dz, (dz % 8 == 0) ? flowerLeaf : hedgeLeaf);
                setBlock(world, chunkBox, gx + 10, baseY + 1, gz + dz, (dz % 8 == 0) ? flowerLeaf : hedgeLeaf);
            }
        }

        // Glorieta clásica de mármol al Este del estanque (gx + 34, gz)
        int px = gx + 34;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 8.2D) {
                    setBlock(world, chunkBox, px + dx, baseY, gz + dz,
                            d >= 7.0D ? Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState() : Blocks.POLISHED_DIORITE.getDefaultState());
                }
                if (d >= 5.2D && d <= 6.2D) {
                    setBlock(world, chunkBox, px + dx, baseY + 6, gz + dz, quartz);
                }
                if (d <= 5.5D) {
                    setBlock(world, chunkBox, px + dx, baseY + 7, gz + dz, quartz);
                }
                if (d <= 3.5D) {
                    setBlock(world, chunkBox, px + dx, baseY + 8, gz + dz, Blocks.YELLOW_WOOL.getDefaultState());
                }
            }
        }
        int[][] gazeboCols = {{-5, -3}, {-5, 3}, {5, -3}, {5, 3}, {-3, -5}, {3, -5}, {-3, 5}, {3, 5}};
        for (int[] c : gazeboCols) {
            fillBox(world, chunkBox, px + c[0], baseY + 1, gz + c[1], px + c[0], baseY + 5, gz + c[1], Blocks.QUARTZ_PILLAR.getDefaultState());
        }
        setBlock(world, chunkBox, px, baseY + 6, gz, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, px, baseY + 1, gz, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
    }

    private static void buildNorthPressAndMonuments(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();

        // Dos monumentos neoclásicos en el Jardín Norte (dx = ±92, dz = -112) fuera del cruce vial
        for (int side : new int[]{-1, 1}) {
            int mx = centerX + side * 92;
            int mz = centerZ - 112;
            fillBox(world, chunkBox, mx - 6, baseY, mz - 6, mx + 6, baseY, mz + 6, quartz);
            fillBox(world, chunkBox, mx - 3, baseY + 1, mz - 3, mx + 3, baseY + 1, mz + 3, chiseled);
            fillBox(world, chunkBox, mx - 1, baseY + 2, mz - 1, mx + 1, baseY + 7, mz + 1, quartz);
            fillBox(world, chunkBox, mx - 1, baseY + 8, mz - 1, mx + 1, baseY + 9, mz + 1, gold);
            setBlock(world, chunkBox, mx, baseY + 10, mz, Blocks.SEA_LANTERN.getDefaultState());
            setBlock(world, chunkBox, mx - 4, baseY + 1, mz - 4, Blocks.LANTERN.getDefaultState());
            setBlock(world, chunkBox, mx + 4, baseY + 1, mz - 4, Blocks.LANTERN.getDefaultState());
            setBlock(world, chunkBox, mx - 4, baseY + 1, mz + 4, Blocks.LANTERN.getDefaultState());
            setBlock(world, chunkBox, mx + 4, baseY + 1, mz + 4, Blocks.LANTERN.getDefaultState());
        }
        // Tarima exterior de prensa frente al Pórtico Norte (dx = -28..-20, dz = -70..-62)
        fillBox(world, chunkBox, centerX - 28, baseY, centerZ - 70, centerX - 20, baseY + 1, centerZ - 64, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, centerX - 24, baseY + 2, centerZ - 65, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 27, baseY + 2, centerZ - 69, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, centerX - 21, baseY + 2, centerZ - 69, Blocks.SEA_LANTERN.getDefaultState());
    }

    private static void buildWhiteHouseExteriorLighting(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        // Farolas en entradas Norte y Sur
        for (int side : new int[]{-13, 13}) {
            for (int dz : new int[]{138, 148, 168, -138, -148, -168}) {
                placeStreetLamp(world, chunkBox, centerX + side, baseY, centerZ + dz);
            }
        }
        // Farolas a lo largo de los paseos centrales Sur/Norte y bordes de fuentes
        for (int side : new int[]{-6, 6}) {
            for (int dz : new int[]{64, 77, 118, 128, -64, -77, -114, -128}) {
                placeStreetLamp(world, chunkBox, centerX + side, baseY, centerZ + dz);
            }
        }
        // Farolas en accesos Este/Oeste
        for (int sideZ : new int[]{-9, 9}) {
            for (int dx : new int[]{-152, -138, -124, 124, 138, 152}) {
                placeStreetLamp(world, chunkBox, centerX + dx, baseY, centerZ + sideZ);
            }
        }
        // Farolas a lo largo del bulevar Oeste y paseo Este
        for (int dz : new int[]{-66, -42, -18, 18, 42, 66}) {
            placeStreetLamp(world, chunkBox, centerX - 129, baseY, centerZ + dz);
            if (dz <= 42) {
                placeStreetLamp(world, chunkBox, centerX + 123, baseY, centerZ + dz);
            }
        }
        // Reflectores empotrados en el suelo iluminando las 4 fachadas de La Casa Blanca
        for (int dx = -48; dx <= 48; dx += 8) {
            if (Math.abs(dx) > 20) {
                setBlock(world, chunkBox, centerX + dx, baseY, centerZ + 36, seaLantern);
                setBlock(world, chunkBox, centerX + dx, baseY, centerZ - 36, seaLantern);
            }
        }
        for (int absX = 56; absX <= 104; absX += 8) {
            setBlock(world, chunkBox, centerX - absX, baseY, centerZ - 26, seaLantern);
            setBlock(world, chunkBox, centerX + absX, baseY, centerZ - 26, seaLantern);
            setBlock(world, chunkBox, centerX + absX, baseY, centerZ + 26, seaLantern);
            if (absX < 70 || absX > 94) {
                setBlock(world, chunkBox, centerX - absX, baseY, centerZ + 26, seaLantern);
            }
        }
        for (int dz = -20; dz <= 20; dz += 8) {
            setBlock(world, chunkBox, centerX - 110, baseY, centerZ + dz, seaLantern);
            setBlock(world, chunkBox, centerX + 110, baseY, centerZ + dz, seaLantern);
        }
    }

    private static void placeStreetLamp(StructureWorldAccess world, BlockBox chunkBox, int x, int baseY, int z) {
        setBlock(world, chunkBox, x, baseY + 1, z, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        setBlock(world, chunkBox, x, baseY + 2, z, Blocks.QUARTZ_PILLAR.getDefaultState());
        setBlock(world, chunkBox, x, baseY + 3, z, Blocks.QUARTZ_PILLAR.getDefaultState());
        setBlock(world, chunkBox, x, baseY + 4, z, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, x, baseY + 5, z, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
    }

    // =========================================================================
    // 3. CASCO ARQUITECTÓNICO DE LA CASA BLANCA (4 LADOS, PÓRTICOS, ALAS, TEJADO)
    // =========================================================================

    private static void buildWhiteHouseExteriorShellAndPorticos(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState quartzBricks = Blocks.QUARTZ_BRICKS.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillarY = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState blue = Blocks.BLUE_CONCRETE.getDefaultState();
        BlockState red = Blocks.RED_CONCRETE.getDefaultState();
        BlockState white = Blocks.WHITE_CONCRETE.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState slate = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState slateSlab = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
        BlockState quartzSlab = Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();

        // A. RESIDENCIA EJECUTIVA CENTRAL (dx = -52..+52, dz = -34..+34)
        // Piso base (y = baseY)
        fillBox(world, chunkBox, centerX - 52, baseY, centerZ - 34, centerX + 52, baseY, centerZ + 34, diorite);
        // Limpiar el enorme volumen interior (y = baseY+1..baseY+15)
        fillBox(world, chunkBox, centerX - 51, baseY + 1, centerZ - 33, centerX + 51, baseY + 15, centerZ + 33, Blocks.AIR.getDefaultState());

        // Muros exteriores de las 4 fachadas de la Residencia Central (y = baseY+1 .. baseY+16)
        for (int ry = 1; ry <= 16; ry++) {
            BlockState wallMat = (ry <= 3) ? quartzBricks : ((ry == 8 || ry == 15 || ry == 16) ? chiseled : quartz);
            // Fachada Sur (dz = +34) & Fachada Norte (dz = -34)
            for (int dx = -52; dx <= 52; dx++) {
                boolean isPilaster = (Math.abs(dx) % 8 == 4) || Math.abs(dx) == 52;
                BlockState colMat = isPilaster && ry >= 4 && ry <= 14 ? pillarY : wallMat;
                setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + 34, colMat);
                setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 34, colMat);
            }
            // Fachada Oeste (dx = -52) & Fachada Este (dx = +52)
            for (int dz = -34; dz <= 34; dz++) {
                boolean isPilaster = (Math.abs(dz) % 8 == 2) || Math.abs(dz) == 34;
                BlockState colMat = isPilaster && ry >= 4 && ry <= 14 ? pillarY : wallMat;
                setBlock(world, chunkBox, centerX - 52, baseY + ry, centerZ + dz, colMat);
                setBlock(world, chunkBox, centerX + 52, baseY + ry, centerZ + dz, colMat);
            }
        }

        // Ventanales clásicos neoclásicos en las 4 fachadas (Planta Baja y 2.º Piso)
        for (int dx = -48; dx <= 48; dx += 8) {
            if (Math.abs(dx) <= 4) {
                continue; // Reservado para las puertas monumentales Norte y Sur
            }
            // Planta baja ventanas
            buildClassicalWindow(world, chunkBox, centerX + dx, baseY + 2, centerZ + 34, 3, 4, true, quartz, glass, Direction.SOUTH);
            buildClassicalWindow(world, chunkBox, centerX + dx, baseY + 2, centerZ - 34, 3, 4, true, quartz, glass, Direction.NORTH);
            // Segundo piso ventanas
            buildClassicalWindow(world, chunkBox, centerX + dx, baseY + 10, centerZ + 34, 3, 4, true, quartz, glass, Direction.SOUTH);
            buildClassicalWindow(world, chunkBox, centerX + dx, baseY + 10, centerZ - 34, 3, 4, true, quartz, glass, Direction.NORTH);
        }
        for (int dz = -26; dz <= 26; dz += 8) {
            // Ventanales Este y Oeste solo fuera de los Pabellones de enlace (|dz| <= 24 queda integrado con las Alas)
            if (Math.abs(dz) >= 26) {
                buildClassicalWindow(world, chunkBox, centerX - 52, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, Direction.WEST);
                buildClassicalWindow(world, chunkBox, centerX + 52, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, Direction.EAST);
                buildClassicalWindow(world, chunkBox, centerX - 52, baseY + 2, centerZ + dz, 3, 4, false, quartz, glass, Direction.WEST);
                buildClassicalWindow(world, chunkBox, centerX + 52, baseY + 2, centerZ + dz, 3, 4, false, quartz, glass, Direction.EAST);
            }
        }

        // Puertas monumentales Sur (dz = +34) y Norte (dz = -34)
        for (int pz : new int[]{34, -34}) {
            fillBox(world, chunkBox, centerX - 3, baseY + 1, centerZ + pz, centerX + 3, baseY + 6, centerZ + pz, chiseled);
            fillBox(world, chunkBox, centerX - 2, baseY + 1, centerZ + pz, centerX + 2, baseY + 5, centerZ + pz, Blocks.AIR.getDefaultState());
            fillBox(world, chunkBox, centerX - 2, baseY + 4, centerZ + pz, centerX + 2, baseY + 5, centerZ + pz, glass);
            Direction doorFace = pz > 0 ? Direction.NORTH : Direction.SOUTH;
            placeDoubleWoodDoor(world, chunkBox, centerX - 1, baseY + 1, centerZ + pz, doorFace, false);
            placeDoubleWoodDoor(world, chunkBox, centerX, baseY + 1, centerZ + pz, doorFace, true);
        }
        // Puerta central del Balcón Truman en 2.º piso Sur (dx = -1..+1, ry = 9..13, dz = +34)
        fillBox(world, chunkBox, centerX - 2, baseY + 9, centerZ + 34, centerX + 2, baseY + 13, centerZ + 34, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, centerX - 2, baseY + 12, centerZ + 34, centerX + 2, baseY + 13, centerZ + 34, glass);
        placeDoubleWoodDoor(world, chunkBox, centerX - 1, baseY + 9, centerZ + 34, Direction.NORTH, false);
        placeDoubleWoodDoor(world, chunkBox, centerX, baseY + 9, centerZ + 34, Direction.NORTH, true);

        // B. PÓRTICO SUR SEMICIRCULAR Y BALCÓN TRUMAN (dz = +34..+50, dx = -19..+19 encajado a la fachada Sur)
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = 34; dz <= 50; dz++) {
                double norm = (double) (dx * dx) / (18.0D * 18.0D) + (double) ((dz - 34) * (dz - 34)) / (15.0D * 15.0D);
                if (norm <= 1.02D) {
                    if (dz >= 35) {
                        setBlock(world, chunkBox, centerX + dx, baseY, centerZ + dz, quartz);
                    }
                    if (norm <= 0.94D) {
                        if (dz >= 35) {
                            setBlock(world, chunkBox, centerX + dx, baseY + 8, centerZ + dz, quartz); // Balcón Truman
                        }
                        setBlock(world, chunkBox, centerX + dx, baseY + 15, centerZ + dz, chiseled); // Techo Pórtico Sur
                        setBlock(world, chunkBox, centerX + dx, baseY + 16, centerZ + dz, quartz);
                    }
                    if (dz >= 35 && norm >= 0.78D && norm <= 0.94D) {
                        setBlock(world, chunkBox, centerX + dx, baseY + 9, centerZ + dz, quartzSlab); // Barandal Balcón Truman
                        setBlock(world, chunkBox, centerX + dx, baseY + 17, centerZ + dz, Blocks.DIORITE_WALL.getDefaultState());
                    }
                }
            }
        }
        // 6 Monumentales Columnas Jónicas del Pórtico Sur
        int[][] southPorticoCols = {{-16, 37}, {-11, 44}, {-4, 48}, {4, 48}, {11, 44}, {16, 37}};
        for (int[] col : southPorticoCols) {
            fillBox(world, chunkBox, centerX + col[0], baseY + 1, centerZ + col[1],
                    centerX + col[0], baseY + 14, centerZ + col[1], pillarY);
            setBlock(world, chunkBox, centerX + col[0], baseY + 1, centerZ + col[1], chiseled);
            setBlock(world, chunkBox, centerX + col[0], baseY + 14, centerZ + col[1], chiseled);
        }
        // Escaleras del Pórtico Sur y faroles de techo
        fillBox(world, chunkBox, centerX - 8, baseY, centerZ + 49, centerX + 8, baseY, centerZ + 52,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX, baseY + 14, centerZ + 41, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 13, centerZ + 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, centerX - 9, baseY + 7, centerZ + 40, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, centerX + 9, baseY + 7, centerZ + 40, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, centerX, baseY + 7, centerZ + 43, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

        // C. PÓRTICO NORTE MONUMENTAL CON FRONTÓN TRIANGULAR (dz = -48..-35, dx = -20..+20)
        fillBox(world, chunkBox, centerX - 20, baseY, centerZ - 48, centerX + 20, baseY, centerZ - 35, quartz);
        fillBox(world, chunkBox, centerX - 18, baseY, centerZ - 51, centerX + 18, baseY, centerZ - 49,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 20, baseY + 15, centerZ - 48, centerX + 20, baseY + 16, centerZ - 34, chiseled);
        for (int colX : new int[]{-18, -12, -6, -2, 2, 6, 12, 18}) {
            fillBox(world, chunkBox, centerX + colX, baseY + 1, centerZ - 47, centerX + colX, baseY + 14, centerZ - 47, pillarY);
            setBlock(world, chunkBox, centerX + colX, baseY + 1, centerZ - 47, chiseled);
            setBlock(world, chunkBox, centerX + colX, baseY + 14, centerZ - 47, chiseled);
        }
        // Frontón triangular clásico del Pórtico Norte encajado sin huecos al tejado principal (ry = 17..23)
        for (int step = 0; step <= 6; step++) {
            int halfW = 20 - step * 3;
            fillBox(world, chunkBox, centerX - halfW, baseY + 17 + step, centerZ - 48,
                    centerX + halfW, baseY + 17 + step, centerZ - 48, quartz);
            fillBox(world, chunkBox, centerX - halfW, baseY + 17 + step, centerZ - 47,
                    centerX + halfW, baseY + 17 + step, centerZ - 30 + step * 3, (step == 0) ? chiseled : slate);
        }
        // Escudo dorado y azul en el centro del frontón Norte
        fillBox(world, chunkBox, centerX - 3, baseY + 18, centerZ - 48, centerX + 3, baseY + 19, centerZ - 48, blue);
        setBlock(world, chunkBox, centerX, baseY + 19, centerZ - 48, gold);
        setBlock(world, chunkBox, centerX, baseY + 14, centerZ - 41, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 13, centerZ - 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, centerX - 10, baseY + 14, centerZ - 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        setBlock(world, chunkBox, centerX + 10, baseY + 14, centerZ - 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

        // D. TEJADO, BALAUSTRADA, 4 CHIMENEAS Y GRAN MÁSTIL CON BANDERA DE EE. UU. EN 3D
        fillBox(world, chunkBox, centerX - 53, baseY + 16, centerZ - 35, centerX + 53, baseY + 16, centerZ + 35, chiseled);
        // Balaustrada blanca de azotea
        for (int dx = -52; dx <= 52; dx++) {
            boolean post = (dx % 4 == 0);
            setBlock(world, chunkBox, centerX + dx, baseY + 17, centerZ + 34, post ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, centerX + dx, baseY + 17, centerZ - 34, post ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            if (post) {
                setBlock(world, chunkBox, centerX + dx, baseY + 18, centerZ + 34, quartzSlab);
                setBlock(world, chunkBox, centerX + dx, baseY + 18, centerZ - 34, quartzSlab);
            }
        }
        for (int dz = -34; dz <= 34; dz++) {
            boolean post = (dz % 4 == 0);
            setBlock(world, chunkBox, centerX - 52, baseY + 17, centerZ + dz, post ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, centerX + 52, baseY + 17, centerZ + dz, post ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            if (post) {
                setBlock(world, chunkBox, centerX - 52, baseY + 18, centerZ + dz, quartzSlab);
                setBlock(world, chunkBox, centerX + 52, baseY + 18, centerZ + dz, quartzSlab);
            }
        }
        // Tejado inclinado de pizarra oscura (ry = 17..24)
        for (int step = 0; step < 8; step++) {
            int x0 = -48 + step * 3;
            int x1 = 48 - step * 3;
            int z0 = -30 + step * 3;
            int z1 = 30 - step * 3;
            fillBox(world, chunkBox, centerX + x0, baseY + 17 + step, centerZ + z0,
                    centerX + x1, baseY + 17 + step, centerZ + z1, slate);
        }
        // Tragaluz central (penthouse roof)
        fillBox(world, chunkBox, centerX - 10, baseY + 25, centerZ - 8, centerX + 10, baseY + 25, centerZ + 8, chiseled);
        fillBox(world, chunkBox, centerX - 8, baseY + 25, centerZ - 6, centerX + 8, baseY + 25, centerZ + 6, Blocks.TINTED_GLASS.getDefaultState());
        // 4 Chimeneas clásicas
        for (int cx : new int[]{-34, 34}) {
            for (int cz : new int[]{-22, 22}) {
                fillBox(world, chunkBox, centerX + cx - 1, baseY + 17, centerZ + cz - 1,
                        centerX + cx + 1, baseY + 27, centerZ + cz + 1, quartz);
                fillBox(world, chunkBox, centerX + cx - 2, baseY + 28, centerZ + cz - 2,
                        centerX + cx + 2, baseY + 28, centerZ + cz + 2, chiseled);
                setBlock(world, chunkBox, centerX + cx, baseY + 27, centerZ + cz, Blocks.CAMPFIRE.getDefaultState());
            }
        }
        // Gran Mástil Presidencial y Bandera de Estados Unidos en 3D (ondeando hacia el Este)
        fillBox(world, chunkBox, centerX, baseY + 26, centerZ, centerX, baseY + 42, centerZ, Blocks.IRON_BARS.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 26, centerZ, gold);
        setBlock(world, chunkBox, centerX, baseY + 43, centerZ, gold);
        for (int fx = 1; fx <= 13; fx++) {
            int waveZ = (fx / 4) % 2;
            for (int fy = 35; fy <= 41; fy++) {
                boolean inCanton = fx <= 5 && fy >= 38;
                BlockState flagMat;
                if (inCanton) {
                    flagMat = ((fx + fy) % 2 == 0) ? white : blue;
                } else {
                    flagMat = (fy % 2 == 1) ? red : white;
                }
                setBlock(world, chunkBox, centerX + fx, baseY + fy, centerZ + waveZ, flagMat);
            }
        }

        // E. COLUMNATAS LATERALES Y ALAS OESTE Y ESTE (con Bahía Ovalada de la Oficina Oval al Sur del Ala Oeste)
        buildWingWithColonnade(world, chunkBox, centerX, baseY, centerZ, -1); // Ala Oeste (dx = -108..-53)
        buildWingWithColonnade(world, chunkBox, centerX, baseY, centerZ, 1);  // Ala Este (dx = +53..+108)
        buildOvalOfficeSouthBay(world, chunkBox, centerX - 82, baseY, centerZ + 24);
    }

    private static void buildWingWithColonnade(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int side
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
        BlockState slateSlab = Blocks.DEEPSLATE_TILE_SLAB.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState blueCarpet = Blocks.BLUE_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState lanternHang = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
        BlockState ochreGlow = Blocks.OCHRE_FROGLIGHT.getDefaultState();

        // 1. Cuerpo unificado de 2 plantas: Pabellón de Enlace (|dx| = 53..66) + Ala (|dx| = 67..108)
        int fullMinX = side < 0 ? centerX - 108 : centerX + 53;
        int fullMaxX = side < 0 ? centerX - 53 : centerX + 108;
        int cMinX = side < 0 ? centerX - 66 : centerX + 53;
        int cMaxX = side < 0 ? centerX - 53 : centerX + 66;
        int wMinX = side < 0 ? centerX - 108 : centerX + 67;
        int wMaxX = side < 0 ? centerX - 67 : centerX + 108;

        // Casco continuo completo de |dx| = 53..108, dz = -24..+24 (sin cortes entre Residencia y Alas)
        fillBox(world, chunkBox, fullMinX, baseY, centerZ - 24, fullMaxX, baseY, centerZ + 24, diorite);
        fillBox(world, chunkBox, fullMinX, baseY + 1, centerZ - 24, fullMaxX, baseY + 15, centerZ + 24, quartz);
        fillBox(world, chunkBox, fullMinX, baseY + 1, centerZ - 23, fullMaxX, baseY + 7, centerZ + 23, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, fullMinX, baseY + 8, centerZ - 23, fullMaxX, baseY + 8, centerZ + 23, diorite);
        fillBox(world, chunkBox, fullMinX, baseY + 9, centerZ - 23, fullMaxX, baseY + 14, centerZ + 23, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, fullMinX - (side < 0 ? 1 : 0), baseY + 15, centerZ - 25,
                fullMaxX + (side > 0 ? 1 : 0), baseY + 15, centerZ + 25, chiseled);

        // Muro extremo exterior (dx = ±108) reconstruido sólidamente con ventanas y puerta al jardín
        int outerX = side < 0 ? wMinX : wMaxX;
        fillBox(world, chunkBox, outerX, baseY + 1, centerZ - 24, outerX, baseY + 15, centerZ + 24, quartz);

        // Muro divisor interior en |dx| = 67 con amplios arcos de 2 pisos entre Pabellón de Enlace y Ala
        int wingWallX = side < 0 ? centerX - 67 : centerX + 67;
        for (int dz = -23; dz <= 23; dz++) {
            boolean floor1Arch = (dz >= -16 && dz <= -10) || (dz >= 10 && dz <= 16);
            boolean floor2Arch = Math.abs(dz) <= 14;
            for (int ry = 1; ry <= 14; ry++) {
                if (ry == 8) {
                    continue;
                }
                if (ry <= 5 && floor1Arch) {
                    continue;
                }
                if (ry >= 9 && ry <= 13 && floor2Arch) {
                    continue;
                }
                BlockState wallMat = (ry == 1 || ry == 7 || ry == 9 || ry == 14) ? chiseled : quartz;
                setBlock(world, chunkBox, wingWallX, baseY + ry, centerZ + dz, wallMat);
            }
            if (floor2Arch && dz % 5 == 0) {
                fillBox(world, chunkBox, wingWallX, baseY + 9, centerZ + dz, wingWallX, baseY + 13, centerZ + dz, pillar);
            }
        }

        // Pasos de puerta/arco en muro exterior de la Residencia Central (|dx| = 52) hacia el Pabellón de Enlace (ambos pisos)
        int innerWallX = side < 0 ? centerX - 52 : centerX + 52;
        Direction doorDir = side < 0 ? Direction.EAST : Direction.WEST;
        for (int az : new int[]{-10, 14}) {
            fillBox(world, chunkBox, innerWallX, baseY + 1, centerZ + az - 1, innerWallX, baseY + 5, centerZ + az + 2, Blocks.AIR.getDefaultState());
            setBlock(world, chunkBox, innerWallX, baseY + 6, centerZ + az - 1, chiseled);
            setBlock(world, chunkBox, innerWallX, baseY + 6, centerZ + az + 2, chiseled);
            placeDoubleDoorPair(world, chunkBox, innerWallX, baseY + 1, centerZ + az, false, doorDir);
        }
        // Paso en 2.º piso (dx = ±52, dz = +1..+4, evitando las camas de ry=9 en dz=-4..-3)
        fillBox(world, chunkBox, innerWallX, baseY + 9, centerZ + 1, innerWallX, baseY + 13, centerZ + 4, Blocks.AIR.getDefaultState());
        placeDoubleDoorPair(world, chunkBox, innerWallX, baseY + 9, centerZ + 2, false, doorDir);

        // Balaustrada y techo continuo unificado de |dx| = 53..108
        for (int x = fullMinX; x <= fullMaxX; x++) {
            boolean pilaster = ((x - fullMinX) % 4 == 0);
            if (pilaster) {
                fillBox(world, chunkBox, x, baseY + 4, centerZ - 24, x, baseY + 14, centerZ - 24, pillar);
                fillBox(world, chunkBox, x, baseY + 4, centerZ + 24, x, baseY + 14, centerZ + 24, pillar);
            }
            setBlock(world, chunkBox, x, baseY + 16, centerZ - 24, pilaster ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, x, baseY + 16, centerZ + 24, pilaster ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            int minRoofX = side < 0 ? fullMinX + 3 : fullMinX;
            int maxRoofX = side < 0 ? fullMaxX : fullMaxX - 3;
            if (x >= minRoofX && x <= maxRoofX) {
                fillBox(world, chunkBox, x, baseY + 16, centerZ - 21, x, baseY + 16, centerZ + 21, slateSlab.with(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }
        for (int dz = -24; dz <= 24; dz++) {
            setBlock(world, chunkBox, outerX, baseY + 16, centerZ + dz, (dz % 4 == 0) ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
        }

        // Ventanas clásicas continuas tanto en el Pabellón de Enlace como en el Ala
        for (int x = fullMinX + 6; x <= fullMaxX - 5; x += 8) {
            buildClassicalWindow(world, chunkBox, x, baseY + 2, centerZ - 24, 3, 4, true, quartz, glass, Direction.NORTH);
            buildClassicalWindow(world, chunkBox, x, baseY + 10, centerZ - 24, 3, 4, true, quartz, glass, Direction.NORTH);
            buildClassicalWindow(world, chunkBox, x, baseY + 10, centerZ + 24, 3, 4, true, quartz, glass, Direction.SOUTH);
            if (!(side < 0 && x >= centerX - 96 && x <= centerX - 68)) {
                buildClassicalWindow(world, chunkBox, x, baseY + 2, centerZ + 24, 3, 4, true, quartz, glass, Direction.SOUTH);
            }
        }
        for (int dz = -16; dz <= 16; dz += 8) {
            buildClassicalWindow(world, chunkBox, outerX, baseY + 2, centerZ + dz, 3, 4, false, quartz, glass, side < 0 ? Direction.WEST : Direction.EAST);
            buildClassicalWindow(world, chunkBox, outerX, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, side < 0 ? Direction.WEST : Direction.EAST);
        }
        // Puerta exterior al jardín en el extremo del ala
        placeDoubleDoorPair(world, chunkBox, outerX, baseY + 1, centerZ, false, doorDir);

        // Mobiliario, alfombras e iluminación del Pabellón de Enlace (|dx| = 53..66) en Planta Baja y 2.º Piso
        placeRug(world, chunkBox, cMinX + 2, baseY + 1, centerZ - 18, cMaxX - 2, centerZ + 18, side < 0 ? blueCarpet : redCarpet, yellowCarpet);
        placeRug(world, chunkBox, cMinX + 2, baseY + 9, centerZ - 18, cMaxX - 2, centerZ + 18, redCarpet, yellowCarpet);
        int midConnX = (cMinX + cMaxX) / 2;
        setBlock(world, chunkBox, midConnX, baseY + 1, centerZ - 21, ModBlocks.CASINO_PALM_POT.getDefaultState());
        setBlock(world, chunkBox, midConnX, baseY + 1, centerZ + 21, ModBlocks.CASINO_MONSTERA_POT.getDefaultState());
        setBlock(world, chunkBox, midConnX, baseY + 9, centerZ - 21, ModBlocks.CASINO_PALM_POT.getDefaultState());
        setBlock(world, chunkBox, midConnX, baseY + 9, centerZ + 21, ModBlocks.CASINO_MONSTERA_POT.getDefaultState());
        for (int lz : new int[]{-14, 0, 14}) {
            setBlock(world, chunkBox, midConnX, baseY + 7, centerZ + lz, lanternHang);
            setBlock(world, chunkBox, midConnX, baseY, centerZ + lz, ochreGlow);
            setBlock(world, chunkBox, midConnX, baseY + 14, centerZ + lz, lanternHang);
            setBlock(world, chunkBox, midConnX, baseY + 15, centerZ + lz, ochreGlow);
        }
    }

    private static void buildOvalOfficeSouthBay(StructureWorldAccess world, BlockBox chunkBox, int ox, int baseY, int oz) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = 0; dz <= 8; dz++) {
                double norm = (double) (dx * dx) / (12.0D * 12.0D) + (double) (dz * dz) / (8.0D * 8.0D);
                if (norm <= 1.02D) {
                    setBlock(world, chunkBox, ox + dx, baseY, oz + dz, diorite);
                    if (dz == 0) {
                        // Conectar limpiamente la Oficina Oval con el muro Sur del Ala Oeste (oz = centerZ + 24) solo en planta baja
                        if (Math.abs(dx) <= 9) {
                            for (int ry = 1; ry <= 6; ry++) {
                                setBlock(world, chunkBox, ox + dx, baseY + ry, oz, Blocks.AIR.getDefaultState());
                            }
                        } else {
                            for (int ry = 1; ry <= 7; ry++) {
                                setBlock(world, chunkBox, ox + dx, baseY + ry, oz, (ry == 7) ? chiseled : pillar);
                            }
                        }
                        continue;
                    }
                    if (norm >= 0.78D) {
                        for (int ry = 1; ry <= 7; ry++) {
                            boolean isWindow = (Math.abs(dx) <= 1 || Math.abs(Math.abs(dx) - 6) <= 1) && ry >= 2 && ry <= 5;
                            BlockState st = isWindow ? glass : (ry == 7 ? chiseled : ((Math.abs(dx) % 4 == 0) ? pillar : quartz));
                            setBlock(world, chunkBox, ox + dx, baseY + ry, oz + dz, st);
                        }
                    } else {
                        for (int ry = 1; ry <= 7; ry++) {
                            setBlock(world, chunkBox, ox + dx, baseY + ry, oz + dz, Blocks.AIR.getDefaultState());
                        }
                    }
                    setBlock(world, chunkBox, ox + dx, baseY + 8, oz + dz, chiseled);
                    if (norm >= 0.78D) {
                        setBlock(world, chunkBox, ox + dx, baseY + 9, oz + dz, Blocks.DIORITE_WALL.getDefaultState());
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. INTERIOR DETALLADO + GRAN ZONA PRINCIPAL DE CONFERENCIAS
    // =========================================================================

    private static void buildWhiteHouseInteriorAndConferenceHall(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState darkOak = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState goldGlow = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState blueCarpet = Blocks.BLUE_CARPET.getDefaultState();
        BlockState whiteCarpet = Blocks.WHITE_CARPET.getDefaultState();
        BlockState greenCarpet = Blocks.GREEN_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState lanternHang = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
        BlockState lanternStand = Blocks.LANTERN.getDefaultState();

        // ---------------------------------------------------------------------
        // A. DIVISIONES INTERIORES DE LA RESIDENCIA CENTRAL (Respetando el Salón de Conferencias en dx=-38..+38, dz=-28..+20)
        // ---------------------------------------------------------------------
        // Segundo piso alrededor del Salón de Conferencias (|dx| = 39..51 y Sur dz = 21..33, Norte dz = -33..-29)
        for (int dx = -51; dx <= 51; dx++) {
            for (int dz = -33; dz <= 33; dz++) {
                boolean insideConferenceArena = Math.abs(dx) <= 38 && dz >= -28 && dz <= 20;
                if (!insideConferenceArena) {
                    setBlock(world, chunkBox, centerX + dx, baseY + 8, centerZ + dz, quartz);
                }
            }
        }
        // Mezzanine / Balcón perimetral de vista al Salón de Conferencias en 2.º piso (|dx| = 33..38, dz = -20..+20)
        for (int side : new int[]{-1, 1}) {
            int bx0 = side < 0 ? -38 : 33;
            int bx1 = side < 0 ? -33 : 38;
            fillBox(world, chunkBox, centerX + bx0, baseY + 8, centerZ - 20, centerX + bx1, baseY + 8, centerZ + 20, quartz);
            int railX = side < 0 ? -33 : 33;
            for (int dz = -20; dz <= 20; dz++) {
                setBlock(world, chunkBox, centerX + railX, baseY + 9, centerZ + dz,
                        (dz % 5 == 0) ? pillar : Blocks.GLASS_PANE.getDefaultState());
            }
        }
        // Muros divisorios entre Salón de Conferencias y las Salas Laterales (|dx| = 39) con amplios arcos
        for (int dz = -33; dz <= 33; dz++) {
            boolean groundArch = (dz >= -2 && dz <= 4) || (dz >= 13 && dz <= 17);
            boolean upperArch = (dz >= -10 && dz <= -6) || (dz >= 12 && dz <= 16);
            for (int ry = 1; ry <= 14; ry++) {
                if (ry <= 7 && groundArch && ry <= 4) {
                    continue;
                }
                if (ry >= 9 && upperArch && ry <= 12) {
                    continue;
                }
                BlockState mat = (ry == 1 || ry == 8 || ry == 9) ? chiseled : quartz;
                setBlock(world, chunkBox, centerX - 39, baseY + ry, centerZ + dz, mat);
                setBlock(world, chunkBox, centerX + 39, baseY + ry, centerZ + dz, mat);
            }
        }
        // Muros entre Vestíbulo Sur (dz = 21) y Salón de Conferencias con gran arco ceremonial central (dx = -10..+10)
        for (int dx = -38; dx <= 38; dx++) {
            for (int ry = 1; ry <= 7; ry++) {
                if (Math.abs(dx) <= 10 && ry <= 5) {
                    continue; // Arco monumental hacia la Zona de Conferencias
                }
                setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + 21, (ry == 1 || ry == 7) ? chiseled : quartz);
            }
            // Barandal del balcón Sur del 2.º piso mirando al Salón de Conferencias
            if (Math.abs(dx) <= 32) {
                setBlock(world, chunkBox, centerX + dx, baseY + 9, centerZ + 21,
                        (dx % 4 == 0) ? pillar : Blocks.GLASS_PANE.getDefaultState());
            }
        }
        // Muro trasero del escenario de conferencias (dz = -28)
        for (int dx = -38; dx <= 38; dx++) {
            for (int ry = 1; ry <= 14; ry++) {
                if (Math.abs(dx) >= 34 && ry <= 4) {
                    continue; // Pasillos traseros laterales hacia la entrada Norte
                }
                setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 28, (ry == 1 || ry == 14) ? chiseled : quartz);
            }
        }

        // ---------------------------------------------------------------------
        // B. GRAN VESTÍBULO SUR (dz = +22..+33) Y ESCALINATAS IMPERIALES AL 2.º PISO
        // ---------------------------------------------------------------------
        for (int dz = 22; dz <= 33; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ + dz,
                        Math.abs(dx) == 3 ? yellowCarpet : redCarpet);
            }
        }
        // Columnas ceremoniales y recepción en el Vestíbulo Sur
        for (int side : new int[]{-1, 1}) {
            for (int cz : new int[]{24, 30}) {
                fillBox(world, chunkBox, centerX + side * 8, baseY + 1, centerZ + cz,
                        centerX + side * 8, baseY + 7, centerZ + cz, pillar);
            }
            // Escalinatas hacia el 2.º piso (dx = ±18..±22, subiendo de dz=23..29 hasta la losa en dz=30)
            for (int step = 0; step < 7; step++) {
                int sy = baseY + 1 + step;
                int sz = centerZ + 23 + step;
                int sx0 = centerX + (side < 0 ? -22 : 18);
                int sx1 = centerX + (side < 0 ? -18 : 22);
                fillBox(world, chunkBox, sx0, baseY + 1, sz, sx1, sy - 1, sz, quartz);
                fillBox(world, chunkBox, sx0, sy, sz, sx1, sy, sz,
                        Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            }
            fillBox(world, chunkBox, centerX + (side < 0 ? -22 : 18), baseY + 8, centerZ + 23,
                    centerX + (side < 0 ? -18 : 22), baseY + 8, centerZ + 29, Blocks.AIR.getDefaultState());
            setBlock(world, chunkBox, centerX + side * 12, baseY + 1, centerZ + 32, ModBlocks.CASINO_PALM_POT.getDefaultState());
            setBlock(world, chunkBox, centerX + side * 26, baseY + 1, centerZ + 32, ModBlocks.CASINO_MONSTERA_POT.getDefaultState());
        }

        // Iluminación y candelabros del Gran Vestíbulo Sur (dz = +22..+33)
        for (int cx : new int[]{-12, 0, 12}) {
            placeChandelier(world, chunkBox, centerX + cx, baseY + 7, centerZ + 27, 1, gold, goldGlow);
        }
        for (int dz : new int[]{23, 27, 31}) {
            setBlock(world, chunkBox, centerX, baseY, centerZ + dz, goldGlow);
        }
        for (int side : new int[]{-1, 1}) {
            setBlock(world, chunkBox, centerX + side * 20, baseY + 7, centerZ + 24, lanternHang);
            setBlock(world, chunkBox, centerX + side * 20, baseY + 7, centerZ + 30, lanternHang);
            setBlock(world, chunkBox, centerX + side * 28, baseY + 7, centerZ + 27, lanternHang);
        }

        // ---------------------------------------------------------------------
        // C. GRAN ZONA PRINCIPAL DE CONFERENCIAS (dx = -38..+38, dz = -28..+20)
        //    Arena principal amplia, muy bien iluminada y despejada donde aparece Charlie Kirk
        // ---------------------------------------------------------------------
        // 1. Suelo de mármol con el Gran Sello Presidencial incrustado en el centro (dx=0, dz=2) y luminarias empotradas
        for (int dx = -38; dx <= 38; dx++) {
            for (int dz = -27; dz <= 20; dz++) {
                double dist = Math.sqrt(dx * dx + (dz - 2) * (dz - 2));
                BlockState floorTile;
                if (dist <= 11.5D) {
                    if (dist >= 10.2D) {
                        floorTile = ((dx + dz) % 5 == 0) ? seaLantern : Blocks.YELLOW_WOOL.getDefaultState();
                    } else if (dist >= 8.2D) {
                        floorTile = Blocks.BLUE_CONCRETE.getDefaultState();
                    } else if (dist >= 4.2D) {
                        floorTile = ((Math.abs(dx) == 6 && dz == 2) || (dx == 0 && Math.abs(dz - 2) == 6))
                                ? seaLantern
                                : (((dx + dz) % 2 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_CONCRETE.getDefaultState());
                    } else {
                        floorTile = (Math.abs(dx) <= 1 && Math.abs(dz - 2) <= 1) ? gold : Blocks.RED_CONCRETE.getDefaultState();
                    }
                } else {
                    boolean floorLightGrid = dz >= -10 && (Math.abs(dx) == 14 || Math.abs(dx) == 24 || Math.abs(dx) == 32)
                            && (dz == -8 || dz == 0 || dz == 8 || dz == 16);
                    boolean underCarpetLight = (Math.abs(dx) == 2) && (dz >= -10 && (dz + 10) % 4 == 0);
                    floorTile = floorLightGrid ? seaLantern
                            : (underCarpetLight ? goldGlow
                            : (((dx + dz) % 2 == 0) ? quartz : diorite));
                }
                setBlock(world, chunkBox, centerX + dx, baseY, centerZ + dz, floorTile);
            }
        }
        // Alfombra presidencial central hacia el podio
        for (int dz = -11; dz <= 20; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ + dz,
                        Math.abs(dx) == 3 ? yellowCarpet : blueCarpet);
            }
        }

        // 2. Gran Escenario Norte de Conferencias (dx = -18..+18, dz = -27..-13, plataforma en baseY+1)
        fillBox(world, chunkBox, centerX - 18, baseY + 1, centerZ - 27, centerX + 18, baseY + 1, centerZ - 13, darkOak);
        fillBox(world, chunkBox, centerX - 16, baseY + 1, centerZ - 26, centerX + 16, baseY + 1, centerZ - 14, quartz);
        // Focos empotrados en el escenario y bajo su alfombra
        for (int lx = -15; lx <= 15; lx += 4) {
            if (Math.abs(lx) >= 3) {
                setBlock(world, chunkBox, centerX + lx, baseY + 1, centerZ - 13, seaLantern);
            }
        }
        for (int lx = -8; lx <= 8; lx += 4) {
            for (int lz = -23; lz <= -15; lz += 4) {
                setBlock(world, chunkBox, centerX + lx, baseY + 1, centerZ + lz, goldGlow);
            }
        }
        // Alfombra azul del escenario
        placeRug(world, chunkBox, centerX - 10, baseY + 2, centerZ - 24, centerX + 10, centerZ - 14, blueCarpet, yellowCarpet);
        // Escalones suaves al frente y costados del escenario (dz = -12)
        fillBox(world, chunkBox, centerX - 18, baseY + 1, centerZ - 12, centerX + 18, baseY + 1, centerZ - 12,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        // Atril / Podio Presidencial justo frente al punto de spawn de Charlie Kirk (dz = -13, Charlie spawnea en dz = -14)
        setBlock(world, chunkBox, centerX, baseY + 2, centerZ - 13, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 3, centerZ - 13, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 1, baseY + 2, centerZ - 13, Blocks.LANTERN.getDefaultState());
        setBlock(world, chunkBox, centerX + 1, baseY + 2, centerZ - 13, Blocks.LANTERN.getDefaultState());

        // 3. Muro Monumental de Fondo con Dos Grandes Banderas de EE. UU. y Escudo Dorado (dz = -27, inspirado en foto 2)
        for (int side : new int[]{-1, 1}) {
            int flagStart = side < 0 ? -17 : 5;
            int flagEnd = side < 0 ? -5 : 17;
            for (int dx = flagStart; dx <= flagEnd; dx++) {
                for (int ry = 3; ry <= 12; ry++) {
                    boolean inCanton = (side < 0 ? dx <= -12 : dx <= 10) && ry >= 8;
                    BlockState flagMat;
                    if (inCanton) {
                        flagMat = ((dx + ry) % 2 == 0) ? Blocks.WHITE_WOOL.getDefaultState() : Blocks.BLUE_WOOL.getDefaultState();
                    } else {
                        flagMat = (ry % 2 == 1) ? Blocks.RED_WOOL.getDefaultState() : Blocks.WHITE_WOOL.getDefaultState();
                    }
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 27, flagMat);
                }
            }
            // Cámaras de prensa en tarimas laterales
            int camX = side * 16;
            setBlock(world, chunkBox, centerX + camX, baseY + 2, centerZ - 15, Blocks.IRON_BARS.getDefaultState());
            setBlock(world, chunkBox, centerX + camX, baseY + 3, centerZ - 15, Blocks.BLACK_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, centerX + camX, baseY + 3, centerZ - 14, Blocks.SEA_LANTERN.getDefaultState());
            // Plantas del escenario
            setBlock(world, chunkBox, centerX + side * 17, baseY + 2, centerZ - 25, ModBlocks.CASINO_PALM_POT.getDefaultState());
        }
        // Sello Presidencial central entre las dos banderas (dx = -3..+3, ry = 4..10)
        fillBox(world, chunkBox, centerX - 3, baseY + 4, centerZ - 27, centerX + 3, baseY + 10, centerZ - 27, Blocks.BLUE_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 2, baseY + 5, centerZ - 27, centerX + 2, baseY + 9, centerZ - 27, gold);
        setBlock(world, chunkBox, centerX, baseY + 7, centerZ - 27, Blocks.WHITE_CONCRETE.getDefaultState());
        // Iluminación de estudio empotrada sobre el escenario y en todo el techo del Salón (enrasada a baseY+16 bajo pizarra)
        for (int lx = -16; lx <= 16; lx += 4) {
            setBlock(world, chunkBox, centerX + lx, baseY + 15, centerZ - 25, seaLantern);
            setBlock(world, chunkBox, centerX + lx, baseY + 15, centerZ - 16, seaLantern);
        }
        for (int lx = -36; lx <= 36; lx += 6) {
            for (int lz = -24; lz <= 18; lz += 6) {
                setBlock(world, chunkBox, centerX + lx, baseY + 16, centerZ + lz, ((lx + lz) % 12 == 0) ? seaLantern : goldGlow);
            }
        }
        // Faroles bajo y sobre los balcones laterales de 2.º piso
        for (int side : new int[]{-1, 1}) {
            for (int lz = -18; lz <= 18; lz += 6) {
                setBlock(world, chunkBox, centerX + side * 35, baseY + 7, centerZ + lz, lanternHang);
            }
        }

        // 4. Galerías de Butacas de Prensa (solo en los laterales |dx| = 27..36 para mantener el centro 100% libre para el combate)
        for (int side : new int[]{-1, 1}) {
            int startX = side < 0 ? -36 : 27;
            int endX = side < 0 ? -27 : 36;
            for (int rowZ : new int[]{-4, 2, 8, 14}) {
                for (int x = startX; x <= endX; x += 2) {
                    setBlock(world, chunkBox, centerX + x, baseY + 1, centerZ + rowZ,
                            ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
                }
            }
        }

        // 5. Nueve Grandes Candelabros Colgantes en el techo del Salón de Conferencias (anclados desde baseY+15)
        for (int cx : new int[]{-22, 0, 22}) {
            for (int cz : new int[]{-16, -2, 12}) {
                placeChandelier(world, chunkBox, centerX + cx, baseY + 15, centerZ + cz, 3, gold, goldGlow);
            }
        }

        // ---------------------------------------------------------------------
        // D. SALAS DE ESTADO LATERALES DE LA RESIDENCIA (Planta Baja y 2.º Piso)
        // ---------------------------------------------------------------------
        // West State Rooms (dx = -51..-40): Red Room (Sur) & Diplomatic Lounge (Norte)
        placeRug(world, chunkBox, centerX - 49, baseY + 1, centerZ + 8, centerX - 41, centerZ + 28, redCarpet, yellowCarpet);
        placeSofaRow(world, chunkBox, centerX - 48, baseY + 1, centerZ + 22, 4, false, Direction.NORTH);
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ + 19, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
        placeFireplace(world, chunkBox, centerX - 51, baseY + 1, centerZ + 25, Direction.EAST);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 50, baseY + 1, centerZ + 32), Direction.SOUTH, LootTables.WOODLAND_MANSION_CHEST, random);

        placeRug(world, chunkBox, centerX - 49, baseY + 1, centerZ - 29, centerX - 41, centerZ - 8, blueCarpet, whiteCarpet);
        fillBox(world, chunkBox, centerX - 50, baseY + 1, centerZ - 26, centerX - 50, baseY + 5, centerZ - 14, Blocks.BOOKSHELF.getDefaultState());
        placeComputerDesk(world, chunkBox, centerX - 44, baseY + 1, centerZ - 24, Direction.SOUTH);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 41, baseY + 1, centerZ - 32), Direction.SOUTH, LootTables.BASTION_TREASURE_CHEST, random);

        // East State Rooms (dx = +40..+51): Green Room (Sur) & Treaty Parlor (Norte)
        placeRug(world, chunkBox, centerX + 41, baseY + 1, centerZ + 8, centerX + 49, centerZ + 28, greenCarpet, yellowCarpet);
        placeSofaRow(world, chunkBox, centerX + 44, baseY + 1, centerZ + 22, 4, false, Direction.NORTH);
        setBlock(world, chunkBox, centerX + 46, baseY + 1, centerZ + 19, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
        placeFireplace(world, chunkBox, centerX + 51, baseY + 1, centerZ + 25, Direction.WEST);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 50, baseY + 1, centerZ + 32), Direction.SOUTH, LootTables.WOODLAND_MANSION_CHEST, random);

        placeRug(world, chunkBox, centerX + 41, baseY + 1, centerZ - 29, centerX + 49, centerZ - 8, yellowCarpet, whiteCarpet);
        fillBox(world, chunkBox, centerX + 50, baseY + 1, centerZ - 26, centerX + 50, baseY + 5, centerZ - 14, Blocks.BOOKSHELF.getDefaultState());
        placeComputerDesk(world, chunkBox, centerX + 45, baseY + 1, centerZ - 24, Direction.SOUTH);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 41, baseY + 1, centerZ - 32), Direction.SOUTH, LootTables.END_CITY_TREASURE_CHEST, random);

        // 2.º Piso Suites: Yellow Oval Room (Sur), Lincoln Bedroom (Oeste), Queen's Bedroom (Este)
        int f2 = baseY + 9;
        placeRug(world, chunkBox, centerX - 10, f2, centerZ + 23, centerX + 10, centerZ + 31, yellowCarpet, whiteCarpet);
        placeSofaRow(world, chunkBox, centerX - 6, f2, centerZ + 25, 4, false, Direction.SOUTH);
        placeSofaRow(world, chunkBox, centerX + 3, f2, centerZ + 25, 4, false, Direction.SOUTH);
        setBlock(world, chunkBox, centerX, f2, centerZ + 27, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 12, f2, centerZ + 32, ModBlocks.CASINO_PALM_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 12, f2, centerZ + 32, ModBlocks.CASINO_PALM_POT.getDefaultState());

        // Dormitorio Lincoln (Oeste 2.º piso)
        placeRug(world, chunkBox, centerX - 49, f2, centerZ - 18, centerX - 41, centerZ + 18, redCarpet, yellowCarpet);
        placeDoubleBed(world, chunkBox, centerX - 47, f2, centerZ - 4, Direction.WEST, Blocks.RED_BED);
        placeDoubleBed(world, chunkBox, centerX - 47, f2, centerZ - 3, Direction.WEST, Blocks.RED_BED);
        placeFireplace(world, chunkBox, centerX - 51, f2, centerZ + 12, Direction.EAST);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 49, f2, centerZ - 26), Direction.EAST, LootTables.END_CITY_TREASURE_CHEST, random);

        // Suite Presidencial Este (2.º piso)
        placeRug(world, chunkBox, centerX + 41, f2, centerZ - 18, centerX + 49, centerZ + 18, blueCarpet, whiteCarpet);
        placeDoubleBed(world, chunkBox, centerX + 47, f2, centerZ - 4, Direction.EAST, Blocks.BLUE_BED);
        placeDoubleBed(world, chunkBox, centerX + 47, f2, centerZ - 3, Direction.EAST, Blocks.BLUE_BED);
        placeFireplace(world, chunkBox, centerX + 51, f2, centerZ + 12, Direction.WEST);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 49, f2, centerZ - 26), Direction.WEST, LootTables.BASTION_TREASURE_CHEST, random);

        // Iluminación abundante de pasillos laterales y suites (Planta Baja, 2.º Piso, balcón Sur y bajo alfombras)
        for (int side : new int[]{-1, 1}) {
            for (int lz : new int[]{-26, -16, -6, 4, 14, 24, 30}) {
                setBlock(world, chunkBox, centerX + side * 45, baseY + 7, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + side * 45, baseY + 14, centerZ + lz, lanternHang);
                if (lz >= -26 && lz <= 24) {
                    setBlock(world, chunkBox, centerX + side * 45, baseY + 16, centerZ + lz, goldGlow);
                }
            }
            for (int lz : new int[]{-22, -12, 12, 22}) {
                setBlock(world, chunkBox, centerX + side * 45, baseY, centerZ + lz, goldGlow);
            }
        }
        for (int lx = -30; lx <= 30; lx += 10) {
            setBlock(world, chunkBox, centerX + lx, baseY + 14, centerZ + 27, lanternHang);
            setBlock(world, chunkBox, centerX + lx, baseY + 16, centerZ + 27, goldGlow);
        }

        // ---------------------------------------------------------------------
        // E. ALA OESTE INTERIOR: OFICINA OVAL, GABINETE Y SITUATION ROOM
        // ---------------------------------------------------------------------
        buildWestWingInterior(world, chunkBox, centerX, baseY, centerZ, random);

        // ---------------------------------------------------------------------
        // F. ALA ESTE INTERIOR: COMEDOR DE ESTADO, TEATRO Y BIBLIOTECA-MUSEO
        // ---------------------------------------------------------------------
        buildEastWingInterior(world, chunkBox, centerX, baseY, centerZ, random);
    }

    private static void buildWestWingInterior(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        BlockState darkOak = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState blueCarpet = Blocks.BLUE_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState lanternHang = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

        // Muro divisorio interno Norte-Sur entre Oficina Oval (Sur, dz=+2..+23) y Sala del Gabinete (Norte, dz=-23..0)
        for (int dx = -107; dx <= -68; dx++) {
            for (int ry = 1; ry <= 7; ry++) {
                boolean isDoor = (dx == -84 || dx == -83) && ry <= 3;
                if (!isDoor) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + 1, (ry == 1 || ry == 7) ? chiseled : quartz);
                }
            }
        }
        placeDoubleDoorPair(world, chunkBox, centerX - 84, baseY + 1, centerZ + 1, true, Direction.NORTH);

        // 1. OFICINA OVAL (centro en dx = -82, dz = +16)
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -9; dz <= 10; dz++) {
                double d = (double) (dx * dx) / 100.0D + (double) (dz * dz) / 100.0D;
                if (d <= 0.85D) {
                    setBlock(world, chunkBox, centerX - 82 + dx, baseY + 1, centerZ + 16 + dz,
                            d >= 0.65D ? yellowCarpet : blueCarpet);
                }
            }
        }
        // Escritorio Resolute Desk frente a los ventanales curvos del Sur
        fillBox(world, chunkBox, centerX - 85, baseY + 1, centerZ + 21, centerX - 79, baseY + 1, centerZ + 22, darkOak);
        setBlock(world, chunkBox, centerX - 82, baseY + 2, centerZ + 21, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 85, baseY + 2, centerZ + 22, Blocks.LANTERN.getDefaultState());
        setBlock(world, chunkBox, centerX - 79, baseY + 2, centerZ + 22, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX - 82, baseY + 1, centerZ + 24,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
        // Banderas junto al escritorio
        setBlock(world, chunkBox, centerX - 87, baseY + 3, centerZ + 22, Blocks.BLUE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 77, baseY + 3, centerZ + 22, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
        // Sofás frente a la chimenea Norte de la Oficina Oval
        placeFireplace(world, chunkBox, centerX - 82, baseY + 1, centerZ + 2, Direction.SOUTH);
        placeSofaRow(world, chunkBox, centerX - 86, baseY + 1, centerZ + 7, 3, true, Direction.EAST);
        placeSofaRow(world, chunkBox, centerX - 78, baseY + 1, centerZ + 7, 3, true, Direction.WEST);
        setBlock(world, chunkBox, centerX - 82, baseY + 1, centerZ + 8, ModBlocks.CASINO_COFFEE_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 96, baseY + 1, centerZ + 16, ModBlocks.CASINO_PALM_POT.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(centerX - 72, baseY + 1, centerZ + 18), Direction.WEST, LootTables.END_CITY_TREASURE_CHEST, random);
        setBlock(world, chunkBox, centerX - 82, baseY + 7, centerZ + 16, lanternHang);

        // 2. SALA DEL GABINETE (dx = -105..-70, dz = -22..-2)
        fillBox(world, chunkBox, centerX - 98, baseY + 1, centerZ - 14, centerX - 76, baseY + 1, centerZ - 10, darkOak);
        for (int x = -98; x <= -76; x += 3) {
            setBlock(world, chunkBox, centerX + x, baseY + 1, centerZ - 16,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
            setBlock(world, chunkBox, centerX + x, baseY + 1, centerZ - 8,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
        }
        placeLootChest(world, chunkBox, new BlockPos(centerX - 105, baseY + 1, centerZ - 22), Direction.EAST, LootTables.WOODLAND_MANSION_CHEST, random);
        setBlock(world, chunkBox, centerX - 87, baseY + 7, centerZ - 12, lanternHang);

        // Escaleras al 2.º piso del Ala Oeste (Situation Room)
        for (int step = 0; step < 7; step++) {
            int sy = baseY + 1 + step;
            int sx = centerX - 104 + step;
            fillBox(world, chunkBox, sx, baseY + 1, centerZ + 10, sx, sy - 1, centerZ + 13, quartz);
            fillBox(world, chunkBox, sx, sy, centerZ + 10, sx, sy, centerZ + 13,
                    Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        }
        fillBox(world, chunkBox, centerX - 104, baseY + 8, centerZ + 10, centerX - 98, baseY + 8, centerZ + 13, Blocks.AIR.getDefaultState());

        // 3. SITUATION ROOM (2.º piso del Ala Oeste, ry = 9..14)
        int f2 = baseY + 9;
        fillBox(world, chunkBox, centerX - 106, f2 - 1, centerZ - 22, centerX - 68, f2 - 1, centerZ + 22, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        // Gran Mapa Táctico Luminoso en el muro Norte
        fillBox(world, chunkBox, centerX - 96, f2 + 1, centerZ - 23, centerX - 78, f2 + 4, centerZ - 23, Blocks.BLUE_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 92, f2 + 2, centerZ - 23, centerX - 88, f2 + 3, centerZ - 23, Blocks.LIME_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 84, f2 + 2, centerZ - 23, centerX - 80, f2 + 3, centerZ - 23, Blocks.GREEN_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX - 86, f2 + 2, centerZ - 23, gold);
        // Consolas de mando y cofres de arsenal
        fillBox(world, chunkBox, centerX - 94, f2, centerZ - 10, centerX - 80, f2, centerZ + 2, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX - 92, f2, centerZ - 8, centerX - 82, f2, centerZ, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        placeLootChest(world, chunkBox, new BlockPos(centerX - 70, f2, centerZ - 21), Direction.WEST, LootTables.BASTION_TREASURE_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 70, f2, centerZ + 21), Direction.WEST, LootTables.END_CITY_TREASURE_CHEST, random);
        // Iluminación completa del Ala Oeste (Planta Baja, Oficina Oval, Gabinete y Situation Room)
        for (int lx : new int[]{-98, -87, -76}) {
            for (int lz : new int[]{-16, -4, 10, 20}) {
                setBlock(world, chunkBox, centerX + lx, baseY + 7, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + lx, baseY + 14, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + lx, baseY + 15, centerZ + lz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            }
        }
        setBlock(world, chunkBox, centerX - 82, baseY + 7, centerZ + 28, lanternHang);
        setBlock(world, chunkBox, centerX - 82, baseY, centerZ + 16, Blocks.OCHRE_FROGLIGHT.getDefaultState());
    }

    private static void buildEastWingInterior(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        BlockState darkOak = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState gold = Blocks.YELLOW_WOOL.getDefaultState();
        BlockState lanternHang = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

        // Muro divisorio interno Norte-Sur entre Teatro (Sur) y Comedor de Estado (Norte)
        for (int dx = 68; dx <= 107; dx++) {
            for (int ry = 1; ry <= 7; ry++) {
                boolean isDoor = (dx == 84 || dx == 85) && ry <= 3;
                if (!isDoor) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + 1, (ry == 1 || ry == 7) ? chiseled : quartz);
                }
            }
        }
        placeDoubleDoorPair(world, chunkBox, centerX + 84, baseY + 1, centerZ + 1, true, Direction.NORTH);

        // 1. TEATRO PRESIDENCIAL (Sur: dx = +70..+105, dz = +4..+22)
        placeRug(world, chunkBox, centerX + 72, baseY + 1, centerZ + 5, centerX + 102, centerZ + 20, redCarpet, yellowCarpet);
        fillBox(world, chunkBox, centerX + 98, baseY + 2, centerZ + 6, centerX + 98, baseY + 6, centerZ + 18, Blocks.BLACK_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX + 98, baseY + 5, centerZ + 5, Blocks.RED_WOOL.getDefaultState());
        setBlock(world, chunkBox, centerX + 98, baseY + 5, centerZ + 19, Blocks.RED_WOOL.getDefaultState());
        for (int rowX : new int[]{78, 83, 88}) {
            for (int z = 7; z <= 17; z += 2) {
                setBlock(world, chunkBox, centerX + rowX, baseY + 1, centerZ + z,
                        Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            }
        }
        setBlock(world, chunkBox, centerX + 87, baseY + 7, centerZ + 12, lanternHang);

        // 2. COMEDOR DE ESTADO Y COCINA PRESIDENCIAL (Norte: dx = +70..+105, dz = -22..-2)
        fillBox(world, chunkBox, centerX + 76, baseY + 1, centerZ - 14, centerX + 94, baseY + 1, centerZ - 10, darkOak);
        for (int x = 76; x <= 94; x += 3) {
            setBlock(world, chunkBox, centerX + x, baseY + 1, centerZ - 16,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
            setBlock(world, chunkBox, centerX + x, baseY + 1, centerZ - 8,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.NORTH));
            setBlock(world, chunkBox, centerX + x, baseY + 2, centerZ - 12, Blocks.CAKE.getDefaultState());
        }
        // Cocina gourmet al extremo Este
        fillBox(world, chunkBox, centerX + 100, baseY + 1, centerZ - 21, centerX + 106, baseY + 1, centerZ - 17, quartz);
        setBlock(world, chunkBox, centerX + 102, baseY + 1, centerZ - 21, Blocks.SMOKER.getDefaultState().with(SmokerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 104, baseY + 1, centerZ - 21, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        placeLootChest(world, chunkBox, new BlockPos(centerX + 106, baseY + 1, centerZ - 10), Direction.WEST, LootTables.VILLAGE_SAVANNA_HOUSE_CHEST, random);
        setBlock(world, chunkBox, centerX + 87, baseY + 7, centerZ - 12, lanternHang);

        // Escaleras al 2.º piso del Ala Este
        for (int step = 0; step < 7; step++) {
            int sy = baseY + 1 + step;
            int sx = centerX + 104 - step;
            fillBox(world, chunkBox, sx, baseY + 1, centerZ - 4, sx, sy - 1, centerZ - 1, quartz);
            fillBox(world, chunkBox, sx, sy, centerZ - 4, sx, sy, centerZ - 1,
                    Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        }
        fillBox(world, chunkBox, centerX + 98, baseY + 8, centerZ - 4, centerX + 104, baseY + 8, centerZ - 1, Blocks.AIR.getDefaultState());

        // 3. BIBLIOTECA Y MUSEO PRESIDENCIAL (2.º piso del Ala Este, ry = 9..14)
        int f2 = baseY + 9;
        placeRug(world, chunkBox, centerX + 70, f2, centerZ - 20, centerX + 104, centerZ + 20, redCarpet, yellowCarpet);
        fillBox(world, chunkBox, centerX + 72, f2, centerZ - 21, centerX + 100, f2 + 4, centerZ - 21, Blocks.BOOKSHELF.getDefaultState());
        fillBox(world, chunkBox, centerX + 72, f2, centerZ + 21, centerX + 100, f2 + 4, centerZ + 21, Blocks.BOOKSHELF.getDefaultState());
        // Vitrinas doradas del museo con botín épico
        for (int vx : new int[]{78, 88, 98}) {
            setBlock(world, chunkBox, centerX + vx, f2, centerZ, chiseled);
            setBlock(world, chunkBox, centerX + vx, f2 + 1, centerZ, gold);
            setBlock(world, chunkBox, centerX + vx, f2 + 2, centerZ, Blocks.GLASS.getDefaultState());
        }
        placeLootChest(world, chunkBox, new BlockPos(centerX + 70, f2, centerZ - 18), Direction.EAST, LootTables.WOODLAND_MANSION_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 70, f2, centerZ + 18), Direction.EAST, LootTables.END_CITY_TREASURE_CHEST, random);
        // Iluminación completa del Ala Este (Teatro, Comedor de Estado y Biblioteca-Museo)
        for (int lx : new int[]{76, 87, 98}) {
            for (int lz : new int[]{-16, -4, 10, 18}) {
                setBlock(world, chunkBox, centerX + lx, baseY + 7, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + lx, baseY + 14, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + lx, baseY + 15, centerZ + lz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            }
        }
        for (int lz : new int[]{-12, 12}) {
            setBlock(world, chunkBox, centerX + 87, baseY, centerZ + lz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
            setBlock(world, chunkBox, centerX + 87, baseY + 8, centerZ + lz, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        }
    }

    // =========================================================================
    // 5. ÁRBOLES MONUMENTALES DE LOS JARDINES DE LA CASA BLANCA
    // =========================================================================

    private static final int[][] OAK_TREE_COORDS = {
            {-46, 130}, {46, 130}, {-92, 138}, {92, 138},
            {-140, 128}, {140, 128}, {-142, 48}, {142, 48},
            {-142, -48}, {142, -48}, {-138, -130}, {138, -130},
            {-48, -132}, {48, -132}, {-112, -136}, {112, -136}
    };

    private static final int[][] MAGNOLIA_TREE_COORDS = {
            {-28, 66}, {28, 66}, {-62, -40}, {62, -40},
            {-64, 62}, {64, 62}
    };

    private static boolean isMonumentalWhiteHouseTreeTrunk(int dx, int dz) {
        for (int[] c : OAK_TREE_COORDS) {
            if (dx == c[0] && dz == c[1]) {
                return true;
            }
        }
        for (int[] m : MAGNOLIA_TREE_COORDS) {
            if (dx == m[0] && dz == m[1]) {
                return true;
            }
        }
        return false;
    }

    private static BlockState getPatrioticRingFlower(double distFromCenter) {
        int band = ((int) Math.floor(distFromCenter)) % 3;
        return switch (band) {
            case 0 -> Blocks.RED_TULIP.getDefaultState();
            case 1 -> Blocks.WHITE_TULIP.getDefaultState();
            default -> Blocks.CORNFLOWER.getDefaultState();
        };
    }

    private static void buildWhiteHouseTrees(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        // Robles y magnolios frondosos flanqueando los jardines sin tapar las fachadas ni el Buzón
        for (int[] c : OAK_TREE_COORDS) {
            buildMonumentalOakTree(world, chunkBox, centerX + c[0], baseY + 1, centerZ + c[1], 13, false);
        }

        // Magnolios históricos (hojas de azalea florecida) flanqueando los jardines sin invadir vías ni edificios
        for (int[] m : MAGNOLIA_TREE_COORDS) {
            buildMonumentalOakTree(world, chunkBox, centerX + m[0], baseY + 1, centerZ + m[1], 11, true);
        }
    }

    private static void buildMonumentalOakTree(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int tx,
            int ty,
            int tz,
            int height,
            boolean magnolia
    ) {
        BlockState log = Blocks.OAK_LOG.getDefaultState();
        BlockState leaf = (magnolia ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.OAK_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT, true);

        for (int y = 0; y < height - 2; y++) {
            setBlock(world, chunkBox, tx, ty + y, tz, log);
        }
        int crownStart = height / 2;
        for (int y = crownStart; y <= height; y++) {
            int maxR = (y == height) ? 1 : (y >= height - 2 ? 3 : 4);
            for (int dx = -maxR; dx <= maxR; dx++) {
                for (int dz = -maxR; dz <= maxR; dz++) {
                    if (dx * dx + dz * dz <= maxR * maxR + 1) {
                        if (!(dx == 0 && dz == 0 && y < height - 2)) {
                            setBlock(world, chunkBox, tx + dx, ty + y, tz + dz, leaf);
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // UTILIDADES ARQUITECTÓNICAS Y DE COLOCACIÓN POR CHUNK
    // =========================================================================

    private static void buildClassicalWindow(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int baseY,
            int cz,
            int width,
            int height,
            boolean alongX,
            BlockState frame,
            BlockState glass,
            Direction facing
    ) {
        int half = width / 2;
        int ox = facing.getOffsetX();
        int oz = facing.getOffsetZ();

        for (int i = -half; i <= half; i++) {
            int wx = alongX ? cx + i : cx;
            int wz = alongX ? cz : cz + i;
            for (int ry = 0; ry < height; ry++) {
                setBlock(world, chunkBox, wx, baseY + ry, wz, glass);
            }
            // Repisa inferior y dintel superior sobresalientes
            setBlock(world, chunkBox, wx + ox, baseY - 1, wz + oz,
                    Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
            setBlock(world, chunkBox, wx + ox, baseY + height, wz + oz,
                    Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, facing.getOpposite()));
        }
        // Frontón triangular central en la parte superior del ventanal
        setBlock(world, chunkBox, cx + ox, baseY + height + 1, cz + oz, frame);
    }

    private static void placeSofaRow(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int startX,
            int y,
            int startZ,
            int length,
            boolean alongZ,
            Direction facing
    ) {
        for (int i = 0; i < length; i++) {
            int x = alongZ ? startX : startX + i;
            int z = alongZ ? startZ + i : startZ;
            CasinoSofaBlock.Shape shape;
            if (length == 1) {
                shape = CasinoSofaBlock.Shape.SINGLE;
            } else if (i == 0) {
                shape = CasinoSofaBlock.Shape.LEFT;
            } else if (i == length - 1) {
                shape = CasinoSofaBlock.Shape.RIGHT;
            } else {
                shape = CasinoSofaBlock.Shape.MIDDLE;
            }
            setBlock(world, chunkBox, x, y, z,
                    ModBlocks.CASINO_SOFA.getDefaultState()
                            .with(HorizontalFacingBlock.FACING, facing)
                            .with(CasinoSofaBlock.SHAPE, shape));
        }
    }

    private static void placeFireplace(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int fx,
            int floorY,
            int fz,
            Direction facing
    ) {
        Direction side = facing.rotateYClockwise();
        int sx = side.getOffsetX();
        int sz = side.getOffsetZ();
        int ox = facing.getOffsetX();
        int oz = facing.getOffsetZ();

        for (int i = -1; i <= 1; i++) {
            for (int ry = 0; ry <= 4; ry++) {
                setBlock(world, chunkBox, fx + i * sx, floorY + ry, fz + i * sz, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
            }
        }
        setBlock(world, chunkBox, fx + ox, floorY, fz + oz, Blocks.CAMPFIRE.getDefaultState());
        setBlock(world, chunkBox, fx + ox - sx, floorY, fz + oz - sz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setBlock(world, chunkBox, fx + ox + sx, floorY, fz + oz + sz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setBlock(world, chunkBox, fx + ox - sx, floorY + 1, fz + oz - sz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setBlock(world, chunkBox, fx + ox + sx, floorY + 1, fz + oz + sz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        for (int i = -1; i <= 1; i++) {
            setBlock(world, chunkBox, fx + ox + i * sx, floorY + 2, fz + oz + i * sz, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        }
        setBlock(world, chunkBox, fx + ox, floorY + 3, fz + oz, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
    }

    private static void placeComputerDesk(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int floorY,
            int cz,
            Direction facing
    ) {
        Direction side = facing.rotateYClockwise();
        int sx = side.getOffsetX();
        int sz = side.getOffsetZ();
        int fx = facing.getOffsetX();
        int fz = facing.getOffsetZ();

        setBlock(world, chunkBox, cx - sx, floorY, cz - sz,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, facing));
        setBlock(world, chunkBox, cx - sx, floorY + 1, cz - sz, Blocks.LANTERN.getDefaultState());

        setBlock(world, chunkBox, cx, floorY, cz, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, cx, floorY + 1, cz, Blocks.BLACK_CONCRETE.getDefaultState());

        setBlock(world, chunkBox, cx + sx, floorY, cz + sz, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, cx + sx, floorY + 1, cz + sz, Blocks.POTTED_FERN.getDefaultState());

        setBlock(world, chunkBox, cx + fx, floorY, cz + fz,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(HorizontalFacingBlock.FACING, facing.getOpposite()));
    }

    private static void placeChandelier(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int topY,
            int cz,
            int chainLen,
            BlockState frame,
            BlockState light
    ) {
        for (int i = 0; i < chainLen; i++) {
            setBlock(world, chunkBox, cx, topY - i, cz, Blocks.CHAIN.getDefaultState());
        }
        int midY = topY - chainLen;
        fillBox(world, chunkBox, cx - 1, midY, cz - 1, cx + 1, midY, cz + 1, frame);
        setBlock(world, chunkBox, cx, midY, cz, light);
        setBlock(world, chunkBox, cx, midY - 1, cz, light);
        setBlock(world, chunkBox, cx - 1, midY - 1, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx + 1, midY - 1, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY - 1, cz - 1, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY - 1, cz + 1, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx - 2, midY, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx + 2, midY, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY, cz - 2, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY, cz + 2, Blocks.END_ROD.getDefaultState());
    }

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

    private static void placeDoubleDoorPair(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int x,
            int y,
            int z,
            boolean alongX,
            Direction facing
    ) {
        int x2 = alongX ? x + 1 : x;
        int z2 = alongX ? z : z + 1;
        placeSingleDoor(world, chunkBox, x, y, z, facing, DoorHinge.LEFT);
        placeSingleDoor(world, chunkBox, x2, y, z2, facing, DoorHinge.RIGHT);
    }

    private static void placeDoubleWoodDoor(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int x,
            int y,
            int z,
            Direction facing,
            boolean rightHinge
    ) {
        placeSingleDoor(world, chunkBox, x, y, z, facing, rightHinge ? DoorHinge.RIGHT : DoorHinge.LEFT);
    }

    private static void placeSingleDoor(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int x,
            int y,
            int z,
            Direction facing,
            DoorHinge hinge
    ) {
        BlockState lower = Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, facing)
                .with(DoorBlock.HINGE, hinge)
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = lower.with(DoorBlock.HALF, DoubleBlockHalf.UPPER);
        setBlock(world, chunkBox, x, y, z, lower);
        setBlock(world, chunkBox, x, y + 1, z, upper);
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

    // =========================================================================
    // 6. LIMPIEZA POST-VEGETACIÓN (ELIMINAR LIANAS FLOTANTES Y DESPEJAR VISTAS)
    // =========================================================================

    public static boolean shouldKeepClearOfWildVegetation(int dx, int dz) {
        return getEffectiveEstateDistance(dx, dz) <= TREE_CLEAR_RADIUS;
    }

    public static void postCleanWhiteHouseChunk(
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
        int cachedTopY = Math.min(worldTopY, baseY + 60);

        // Fase 1: Restaurar placeholder de césped (y anillo floral), limpiar el perímetro y eliminar troncos salvajes (incluyendo 2x2)
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
                    pos.set(wx, baseY, wz);
                    BlockState groundSt = world.getBlockState(pos);
                    if (groundSt.isOf(Blocks.GREEN_TERRACOTTA) || isSoilVariant(groundSt)) {
                        world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                    }
                    if (insideCoreChunk && Math.abs(dx) <= 16 && Math.abs(dz - 98) <= 16) {
                        double fountainDist = Math.sqrt((double) dx * dx + (double) (dz - 98) * (dz - 98));
                        if (fountainDist > 16.0D - 3.2D && fountainDist <= 16.0D) {
                            world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                            world.setBlockState(pos.set(wx, baseY + 1, wz), getPatrioticRingFlower(fountainDist), Block.NOTIFY_LISTENERS);
                        }
                    }

                    boolean keepOwnTrunk = isMonumentalWhiteHouseTreeTrunk(dx, dz);
                    for (int y = baseY + 1; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (isVineBlock(st)) {
                            world.setBlockState(pos, air, CLEAR_FLAGS);
                        } else if (isWildVegetationBlock(st) && !(keepOwnTrunk && st.isIn(BlockTags.LOGS))) {
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

        // Fase 2: Limpiar hojas flotantes y troncos decapitados en la corona exterior (191 < effectiveDist <= TOTAL_RADIUS)
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

        // Asegurar nuestros 22 árboles monumentales ornamentales y cualquier edición manual guardada
        buildWhiteHouseTrees(world, chunkBox, centerX, baseY, centerZ);
        CasaBlancaCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
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
                boolean insideChunk = chunkBox.contains(nx, baseYFromChunkBox(chunkBox), nz);
                if (!insideChunk && !world.isValidForSetBlock(scan.set(nx, baseYFromChunkBox(chunkBox), nz))) {
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

    private static int baseYFromChunkBox(BlockBox chunkBox) {
        return Math.min(chunkBox.getMaxY(), chunkBox.getMinY() + 28);
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

    private static int getChunkMaxClearY(StructureWorldAccess world, Chunk chunk, int baseY) {
        int highestIdx = chunk.getHighestNonEmptySection();
        int sectionTop = highestIdx >= 0 ? (chunk.sectionIndexToCoord(highestIdx) << 4) + 15 : baseY;
        return Math.min(world.getTopYInclusive(), Math.max(baseY + 56, sectionTop));
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
