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
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
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

    /** Radio interior de la propiedad (186 bloques = 372x372 bloques). */
    public static final int ESTATE_RADIUS = 186;
    /** Franja perimetral de 38 bloques para talud suave (total 448x448 bloques). */
    public static final int GRADE_RANGE = 38;
    public static final int TOTAL_RADIUS = ESTATE_RADIUS + GRADE_RANGE; // 224 -> 448x448 bloques

    /** Posición relativa del "Buzón de la Casa Blanca" en la entrada principal Sur (lejos del edificio principal). */
    public static final int MAILBOX_REL_X = 14;
    public static final int MAILBOX_REL_Z = 156;

    /** Posición relativa de aparición de Charlie Kirk sobre el escenario del Salón Principal de Conferencias. */
    public static final int CONFERENCE_SPAWN_REL_X = 0;
    public static final int CONFERENCE_SPAWN_REL_Y = 2;
    public static final int CONFERENCE_SPAWN_REL_Z = -14;

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
                        baseY + 48,
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
        // 1. Nivelación de los 448x448 bloques y transición suave sin cortes bruscos con el terreno exterior
        prepareEstateTerrainAndBlending(world, chunkBox, centerX, baseY, centerZ);

        // 2. Reja perimetral presidencial, casetas del Servicio Secreto, jardines, fuentes, helipuerto Marine One, limusinas y Buzón
        buildWhiteHouseGroundsAndGardens(world, chunkBox, centerX, baseY, centerZ, random);

        // 3. Casco arquitectónico de La Casa Blanca (Residencia Central, Pórticos Norte y Sur, Columnatas, Alas Este/Oeste, Oficina Oval y Tejado con Bandera)
        buildWhiteHouseExteriorShellAndPorticos(world, chunkBox, centerX, baseY, centerZ);

        // 4. Interior extremadamente detallado: Gran Zona Principal de Conferencias (arena de Charlie Kirk) + Alas + 2.º Piso
        buildWhiteHouseInteriorAndConferenceHall(world, chunkBox, centerX, baseY, centerZ, random);

        // 5. Árboles monumentales de los jardines Norte, Sur, Este y Oeste
        buildWhiteHouseTrees(world, chunkBox, centerX, baseY, centerZ);

        // 6. Aplicar cualquier modificación personalizada guardada con "/casablanca guardar"
        CasaBlancaCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
    }

    // =========================================================================
    // 1. TERRENO Y TRANSICIÓN SUAVE SIN CORTES BRUSCOS
    // =========================================================================

    private static void prepareEstateTerrainAndBlending(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        int minX = Math.max(centerX - TOTAL_RADIUS, chunkBox.getMinX());
        int maxX = Math.min(centerX + TOTAL_RADIUS, chunkBox.getMaxX());
        int minZ = Math.max(centerZ - TOTAL_RADIUS, chunkBox.getMinZ());
        int maxZ = Math.min(centerZ + TOTAL_RADIUS, chunkBox.getMaxZ());

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int bottomY = Math.max(world.getBottomY() + 1, baseY - 26);
        int topClearY = Math.min(world.getTopYInclusive(), baseY + 50);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        BlockState air = Blocks.AIR.getDefaultState();
        BlockState stoneFoundation = Blocks.STONE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;

                double chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
                double euclidean = Math.sqrt((double) dx * dx + (double) dz * dz);
                double effectiveDist = chebyshev * 0.55D + euclidean * 0.45D;

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
                    world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);

                    for (int y = baseY + 1; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        if (!world.getBlockState(pos).isAir()) {
                            world.setBlockState(pos, air, Block.NOTIFY_LISTENERS);
                        }
                    }
                    continue;
                }

                if (effectiveDist > TOTAL_RADIUS) {
                    continue;
                }

                double distOut = effectiveDist - ESTATE_RADIUS;
                int capY = baseY + (int) Math.floor((distOut * distOut) / 14.0D + distOut * 0.55D);
                boolean trimmedHighTerrain = false;
                for (int y = topClearY; y > capY; y--) {
                    pos.set(wx, y, wz);
                    BlockState st = world.getBlockState(pos);
                    if (!st.isAir()) {
                        if (!st.isIn(BlockTags.LEAVES) && !st.isIn(BlockTags.LOGS) && !st.isOf(Blocks.SNOW)) {
                            trimmedHighTerrain = true;
                        }
                        world.setBlockState(pos, air, Block.NOTIFY_LISTENERS);
                    }
                }
                if (trimmedHighTerrain) {
                    for (int y = capY; y >= bottomY; y--) {
                        pos.set(wx, y, wz);
                        BlockState st = world.getBlockState(pos);
                        if (!st.isAir() && !st.isLiquid() && !st.isReplaceable()) {
                            world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                            if (y - 1 >= bottomY) {
                                world.setBlockState(pos.set(wx, y - 1, wz), dirt, Block.NOTIFY_LISTENERS);
                            }
                            break;
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
                            world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);
                        } else if (y >= slopeTargetY - 3) {
                            world.setBlockState(pos, dirt, Block.NOTIFY_LISTENERS);
                        } else {
                            world.setBlockState(pos, stoneFoundation, Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }
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
            Random random
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState quartzPillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();
        BlockState andesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState smoothStone = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState blackstone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState ironBars = Blocks.IRON_BARS.getDefaultState();
        BlockState goldBlock = Blocks.GOLD_BLOCK.getDefaultState();
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

                // Rotonda elíptica Sur (dz = 46..136)
                double southDriveNorm = ((double) dx * dx) / (78.0D * 78.0D) + ((double) (dz - 82) * (dz - 82)) / (54.0D * 54.0D);
                boolean inSouthEllipticalDrive = dz >= 44 && dz <= 138 && southDriveNorm <= 1.05D && southDriveNorm >= 0.72D;
                boolean inSouthPorticoApron = dz >= 40 && dz <= 58 && Math.abs(dx) <= 28;

                // Rotonda elíptica Norte (dz = -136..-44)
                double northDriveNorm = ((double) dx * dx) / (78.0D * 78.0D) + ((double) (dz + 82) * (dz + 82)) / (54.0D * 54.0D);
                boolean inNorthEllipticalDrive = dz >= -138 && dz <= -44 && northDriveNorm <= 1.05D && northDriveNorm >= 0.72D;
                boolean inNorthPorticoApron = dz >= -58 && dz <= -40 && Math.abs(dx) <= 28;

                // Caminos Este-Oeste y paseo perimetral interior (Pennsylvania / Executive Ave)
                boolean inWestApproach = dz >= -8 && dz <= 8 && dx >= -176 && dx <= -108;
                boolean inEastApproach = dz >= -8 && dz <= 8 && dx >= 108 && dx <= 176;
                boolean inWestMotorcadeDrive = dx >= -124 && dx <= -106 && dz >= -110 && dz <= 115;
                boolean inEastWalkway = dx >= 108 && dx <= 116 && dz >= -110 && dz <= 115;
                boolean inSouthLawnPath = Math.abs(dx) <= 5 && dz >= 56 && dz <= 134;
                boolean inNorthLawnPath = Math.abs(dx) <= 5 && dz >= -134 && dz <= -56;

                if (inSouthGateRoad || inNorthGateRoad || inSouthEllipticalDrive || inSouthPorticoApron
                        || inNorthEllipticalDrive || inNorthPorticoApron || inWestApproach || inEastApproach
                        || inWestMotorcadeDrive || inEastWalkway) {
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
        buildPresidentialFountain(world, chunkBox, centerX, baseY, centerZ + 98, 16, true);
        // Fuente Norte (dz = -95)
        buildPresidentialFountain(world, chunkBox, centerX, baseY, centerZ - 95, 13, false);

        // 6. Jardín de las Rosas (Rose Garden, Suroeste: dx = -102..-58, dz = +32..+66)
        buildRoseGarden(world, chunkBox, centerX - 80, baseY, centerZ + 49, oakLeaves, azaleaLeaves);

        // 7. Helipuerto Presidencial y Helicóptero "Marine One" (dx = -105, dz = +105)
        buildMarineOneHelipad(world, chunkBox, centerX - 105, baseY, centerZ + 105);

        // 8. Dos Limusinas Blindadas Presidenciales ("La Bestia") en la avenida Oeste
        buildPresidentialLimousine(world, chunkBox, centerX - 115, baseY + 1, centerZ + 44);
        buildPresidentialLimousine(world, chunkBox, centerX - 115, baseY + 1, centerZ + 66);

        // 9. Jardín Este (Jacqueline Kennedy Garden): Estanque reflectante + Glorieta clásica
        buildJacquelineKennedyGarden(world, chunkBox, centerX + 82, baseY, centerZ + 76, oakLeaves, floweringAzalea);

        // 10. Explanada Norte: Tarimas de prensa exteriores + Monumentos neoclásicos
        buildNorthPressAndMonuments(world, chunkBox, centerX, baseY, centerZ);
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
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
            boolean withFlowerRings
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
                    setBlock(world, chunkBox, fx + dx, baseY, fz + dz, Blocks.GRASS_BLOCK.getDefaultState());
                    int band = ((int) Math.floor(d)) % 3;
                    BlockState flower = switch (band) {
                        case 0 -> Blocks.RED_TULIP.getDefaultState();
                        case 1 -> Blocks.WHITE_TULIP.getDefaultState();
                        default -> Blocks.CORNFLOWER.getDefaultState();
                    };
                    if (((dx + dz) & 1) == 0) {
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, flower);
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
                    setBlock(world, chunkBox, px + dx, baseY + 8, gz + dz, Blocks.GOLD_BLOCK.getDefaultState());
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();

        // Dos monumentos neoclásicos en el Jardín Norte (dx = ±80, dz = -88)
        for (int side : new int[]{-1, 1}) {
            int mx = centerX + side * 80;
            int mz = centerZ - 88;
            fillBox(world, chunkBox, mx - 6, baseY, mz - 6, mx + 6, baseY, mz + 6, quartz);
            fillBox(world, chunkBox, mx - 3, baseY + 1, mz - 3, mx + 3, baseY + 1, mz + 3, chiseled);
            fillBox(world, chunkBox, mx - 1, baseY + 2, mz - 1, mx + 1, baseY + 7, mz + 1, quartz);
            fillBox(world, chunkBox, mx - 1, baseY + 8, mz - 1, mx + 1, baseY + 9, mz + 1, gold);
            setBlock(world, chunkBox, mx, baseY + 10, mz, Blocks.SEA_LANTERN.getDefaultState());
        }
        // Tarima exterior de prensa frente al Pórtico Norte (dx = -28..-20, dz = -70..-62)
        fillBox(world, chunkBox, centerX - 28, baseY, centerZ - 70, centerX - 20, baseY + 1, centerZ - 64, Blocks.DARK_OAK_PLANKS.getDefaultState());
        setBlock(world, chunkBox, centerX - 24, baseY + 2, centerZ - 65, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 27, baseY + 2, centerZ - 69, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, centerX - 21, baseY + 2, centerZ - 69, Blocks.SEA_LANTERN.getDefaultState());
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
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
            // Ventanales 2.º piso en fachadas Este y Oeste (encima de las columnatas laterales)
            buildClassicalWindow(world, chunkBox, centerX - 52, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, Direction.WEST);
            buildClassicalWindow(world, chunkBox, centerX + 52, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, Direction.EAST);
            // Ventanas planta baja donde no estorban las columnatas (dz = -26, -18, -10, +26)
            if (dz <= -10 || dz >= 24) {
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

        // B. PÓRTICO SUR SEMICIRCULAR Y BALCÓN TRUMAN (dz = +35..+50, dx = -19..+19)
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = 35; dz <= 50; dz++) {
                double norm = (double) (dx * dx) / (18.0D * 18.0D) + (double) ((dz - 34) * (dz - 34)) / (15.0D * 15.0D);
                if (norm <= 1.02D) {
                    setBlock(world, chunkBox, centerX + dx, baseY, centerZ + dz, quartz);
                    if (norm <= 0.94D) {
                        setBlock(world, chunkBox, centerX + dx, baseY + 8, centerZ + dz, quartz); // Balcón Truman
                        setBlock(world, chunkBox, centerX + dx, baseY + 15, centerZ + dz, chiseled); // Techo Pórtico Sur
                        setBlock(world, chunkBox, centerX + dx, baseY + 16, centerZ + dz, quartz);
                    }
                    if (norm >= 0.78D && norm <= 0.94D) {
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
        // Escaleras del Pórtico Sur
        fillBox(world, chunkBox, centerX - 8, baseY, centerZ + 49, centerX + 8, baseY, centerZ + 52,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX, baseY + 14, centerZ + 41, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 13, centerZ + 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

        // C. PÓRTICO NORTE MONUMENTAL CON FRONTÓN TRIANGULAR (dz = -48..-35, dx = -20..+20)
        fillBox(world, chunkBox, centerX - 20, baseY, centerZ - 48, centerX + 20, baseY, centerZ - 35, quartz);
        fillBox(world, chunkBox, centerX - 18, baseY, centerZ - 51, centerX + 18, baseY, centerZ - 49,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 20, baseY + 15, centerZ - 48, centerX + 20, baseY + 16, centerZ - 35, chiseled);
        for (int colX : new int[]{-18, -12, -6, -2, 2, 6, 12, 18}) {
            fillBox(world, chunkBox, centerX + colX, baseY + 1, centerZ - 47, centerX + colX, baseY + 14, centerZ - 47, pillarY);
            setBlock(world, chunkBox, centerX + colX, baseY + 1, centerZ - 47, chiseled);
            setBlock(world, chunkBox, centerX + colX, baseY + 14, centerZ - 47, chiseled);
        }
        // Frontón triangular clásico del Pórtico Norte (ry = 17..23)
        for (int step = 0; step <= 6; step++) {
            int halfW = 20 - step * 3;
            fillBox(world, chunkBox, centerX - halfW, baseY + 17 + step, centerZ - 48,
                    centerX + halfW, baseY + 17 + step, centerZ - 48, quartz);
            fillBox(world, chunkBox, centerX - halfW, baseY + 17 + step, centerZ - 47,
                    centerX + halfW, baseY + 17 + step, centerZ - 35, (step == 0) ? chiseled : slate);
        }
        // Escudo dorado y azul en el centro del frontón Norte
        fillBox(world, chunkBox, centerX - 3, baseY + 18, centerZ - 48, centerX + 3, baseY + 19, centerZ - 48, blue);
        setBlock(world, chunkBox, centerX, baseY + 19, centerZ - 48, gold);
        setBlock(world, chunkBox, centerX, baseY + 14, centerZ - 41, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 13, centerZ - 41, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

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

        // 1. Columnata de enlace (|dx| = 53..66, dz = +8..+22)
        int cMinX = side < 0 ? centerX - 66 : centerX + 53;
        int cMaxX = side < 0 ? centerX - 53 : centerX + 66;
        fillBox(world, chunkBox, cMinX, baseY, centerZ + 8, cMaxX, baseY, centerZ + 22, diorite);
        fillBox(world, chunkBox, cMinX, baseY + 1, centerZ + 8, cMaxX, baseY + 7, centerZ + 22, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, cMinX, baseY + 8, centerZ + 8, cMaxX, baseY + 8, centerZ + 22, quartz);
        fillBox(world, chunkBox, cMinX, baseY + 9, centerZ + 8, cMaxX, baseY + 9, centerZ + 22, Blocks.AIR.getDefaultState());
        for (int x = cMinX; x <= cMaxX; x++) {
            setBlock(world, chunkBox, x, baseY + 9, centerZ + 8, Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, x, baseY + 9, centerZ + 22, Blocks.DIORITE_WALL.getDefaultState());
            if ((x - cMinX) % 3 == 0) {
                fillBox(world, chunkBox, x, baseY + 1, centerZ + 8, x, baseY + 7, centerZ + 8, pillar);
                fillBox(world, chunkBox, x, baseY + 1, centerZ + 22, x, baseY + 7, centerZ + 22, pillar);
            }
        }
        // Pasos de puerta que conectan Residencia -> Columnata -> Ala
        int innerWallX = side < 0 ? centerX - 52 : centerX + 52;
        int wingWallX = side < 0 ? centerX - 67 : centerX + 67;
        for (int wx : new int[]{innerWallX, wingWallX}) {
            fillBox(world, chunkBox, wx, baseY + 1, centerZ + 13, wx, baseY + 4, centerZ + 17, Blocks.AIR.getDefaultState());
            placeDoubleDoorPair(world, chunkBox, wx, baseY + 1, centerZ + 14, false, side < 0 ? Direction.EAST : Direction.WEST);
        }

        // 2. Edificio del Ala (|dx| = 67..108, dz = -24..+24, 2 plantas)
        int wMinX = side < 0 ? centerX - 108 : centerX + 67;
        int wMaxX = side < 0 ? centerX - 67 : centerX + 108;
        fillBox(world, chunkBox, wMinX, baseY, centerZ - 24, wMaxX, baseY, centerZ + 24, diorite);
        fillBox(world, chunkBox, wMinX, baseY + 1, centerZ - 24, wMaxX, baseY + 15, centerZ + 24, quartz);
        fillBox(world, chunkBox, wMinX + 1, baseY + 1, centerZ - 23, wMaxX - 1, baseY + 7, centerZ + 23, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, wMinX + 1, baseY + 8, centerZ - 23, wMaxX - 1, baseY + 8, centerZ + 23, diorite);
        fillBox(world, chunkBox, wMinX + 1, baseY + 9, centerZ - 23, wMaxX - 1, baseY + 14, centerZ + 23, Blocks.AIR.getDefaultState());
        fillBox(world, chunkBox, wMinX - 1, baseY + 15, centerZ - 25, wMaxX + 1, baseY + 15, centerZ + 25, chiseled);

        // Balaustrada y techo del ala
        for (int x = wMinX; x <= wMaxX; x++) {
            setBlock(world, chunkBox, x, baseY + 16, centerZ - 24, (x % 4 == 0) ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, x, baseY + 16, centerZ + 24, (x % 4 == 0) ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            if (x >= wMinX + 3 && x <= wMaxX - 3) {
                fillBox(world, chunkBox, x, baseY + 16, centerZ - 21, x, baseY + 16, centerZ + 21, slateSlab.with(SlabBlock.TYPE, SlabType.BOTTOM));
            }
        }
        for (int dz = -24; dz <= 24; dz++) {
            setBlock(world, chunkBox, wMinX, baseY + 16, centerZ + dz, (dz % 4 == 0) ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
            setBlock(world, chunkBox, wMaxX, baseY + 16, centerZ + dz, (dz % 4 == 0) ? chiseled : Blocks.DIORITE_WALL.getDefaultState());
        }

        // Ventanas clásicas en los 4 lados del Ala
        for (int x = wMinX + 6; x <= wMaxX - 6; x += 8) {
            buildClassicalWindow(world, chunkBox, x, baseY + 2, centerZ - 24, 3, 4, true, quartz, glass, Direction.NORTH);
            buildClassicalWindow(world, chunkBox, x, baseY + 10, centerZ - 24, 3, 4, true, quartz, glass, Direction.NORTH);
            buildClassicalWindow(world, chunkBox, x, baseY + 10, centerZ + 24, 3, 4, true, quartz, glass, Direction.SOUTH);
            if (!(side < 0 && x >= centerX - 96 && x <= centerX - 68)) {
                buildClassicalWindow(world, chunkBox, x, baseY + 2, centerZ + 24, 3, 4, true, quartz, glass, Direction.SOUTH);
            }
        }
        int outerX = side < 0 ? wMinX : wMaxX;
        for (int dz = -16; dz <= 16; dz += 8) {
            buildClassicalWindow(world, chunkBox, outerX, baseY + 2, centerZ + dz, 3, 4, false, quartz, glass, side < 0 ? Direction.WEST : Direction.EAST);
            buildClassicalWindow(world, chunkBox, outerX, baseY + 10, centerZ + dz, 3, 4, false, quartz, glass, side < 0 ? Direction.WEST : Direction.EAST);
        }
        // Puerta exterior al jardín en el extremo del ala
        placeDoubleDoorPair(world, chunkBox, outerX, baseY + 1, centerZ, false, side < 0 ? Direction.EAST : Direction.WEST);
    }

    private static void buildOvalOfficeSouthBay(StructureWorldAccess world, BlockBox chunkBox, int ox, int baseY, int oz) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
        BlockState diorite = Blocks.POLISHED_DIORITE.getDefaultState();

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -6; dz <= 8; dz++) {
                double norm = (double) (dx * dx) / (12.0D * 12.0D) + (double) (dz * dz) / (8.0D * 8.0D);
                if (norm <= 1.02D) {
                    setBlock(world, chunkBox, ox + dx, baseY, oz + dz, diorite);
                    if (norm >= 0.78D && dz > 0) {
                        for (int ry = 1; ry <= 7; ry++) {
                            boolean isWindow = (Math.abs(dx) <= 1 || Math.abs(Math.abs(dx) - 6) <= 1) && ry >= 2 && ry <= 5;
                            BlockState st = isWindow ? glass : (ry == 7 ? chiseled : ((Math.abs(dx) % 4 == 0) ? pillar : quartz));
                            setBlock(world, chunkBox, ox + dx, baseY + ry, oz + dz, st);
                        }
                    } else if (dz >= 0 && norm < 0.78D) {
                        for (int ry = 1; ry <= 7; ry++) {
                            setBlock(world, chunkBox, ox + dx, baseY + ry, oz + dz, Blocks.AIR.getDefaultState());
                        }
                    }
                    setBlock(world, chunkBox, ox + dx, baseY + 8, oz + dz, chiseled);
                    if (norm >= 0.78D && dz > 0) {
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
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

        // ---------------------------------------------------------------------
        // C. GRAN ZONA PRINCIPAL DE CONFERENCIAS (dx = -38..+38, dz = -28..+20)
        //    Arena principal amplia y despejada donde aparece Charlie Kirk
        // ---------------------------------------------------------------------
        // 1. Suelo de mármol con el Gran Sello Presidencial incrustado en el centro (dx=0, dz=2)
        for (int dx = -38; dx <= 38; dx++) {
            for (int dz = -27; dz <= 20; dz++) {
                double dist = Math.sqrt(dx * dx + (dz - 2) * (dz - 2));
                BlockState floorTile;
                if (dist <= 11.5D) {
                    if (dist >= 10.2D) {
                        floorTile = Blocks.GOLD_BLOCK.getDefaultState();
                    } else if (dist >= 8.2D) {
                        floorTile = Blocks.BLUE_CONCRETE.getDefaultState();
                    } else if (dist >= 4.2D) {
                        floorTile = ((dx + dz) % 2 == 0) ? Blocks.WHITE_CONCRETE.getDefaultState() : Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
                    } else {
                        floorTile = (Math.abs(dx) <= 1 && Math.abs(dz - 2) <= 1) ? gold : Blocks.RED_CONCRETE.getDefaultState();
                    }
                } else {
                    floorTile = ((dx + dz) % 2 == 0) ? quartz : diorite;
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
        // Iluminación de estudio sobre el escenario
        for (int lx = -14; lx <= 14; lx += 7) {
            setBlock(world, chunkBox, centerX + lx, baseY + 14, centerZ - 22, seaLantern);
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

        // 5. Cuatro Grandes Candelabros Colgantes en el techo abovedado del Salón de Conferencias
        for (int cx : new int[]{-16, 16}) {
            for (int cz : new int[]{-6, 10}) {
                placeChandelier(world, chunkBox, centerX + cx, baseY + 14, centerZ + cz, 3, gold, goldGlow);
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

        // Iluminación general de pasillos laterales y suites
        for (int side : new int[]{-1, 1}) {
            for (int lz : new int[]{-22, -6, 12, 26}) {
                setBlock(world, chunkBox, centerX + side * 45, baseY + 7, centerZ + lz, lanternHang);
                setBlock(world, chunkBox, centerX + side * 45, baseY + 14, centerZ + lz, lanternHang);
            }
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
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
        setBlock(world, chunkBox, centerX - 87, baseY + 14, centerZ, lanternHang);
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
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
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
        setBlock(world, chunkBox, centerX + 87, baseY + 14, centerZ, lanternHang);
    }

    // =========================================================================
    // 5. ÁRBOLES MONUMENTALES DE LOS JARDINES DE LA CASA BLANCA
    // =========================================================================

    private static void buildWhiteHouseTrees(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        // Robles y magnolios frondosos flanqueando los jardines sin tapar las fachadas ni el Buzón
        int[][] oakCoords = {
                {-46, 130}, {46, 130}, {-92, 138}, {92, 138},
                {-140, 128}, {140, 128}, {-142, 48}, {142, 48},
                {-142, -48}, {142, -48}, {-138, -130}, {138, -130},
                {-48, -132}, {48, -132}, {-112, -136}, {112, -136}
        };
        for (int[] c : oakCoords) {
            buildMonumentalOakTree(world, chunkBox, centerX + c[0], baseY + 1, centerZ + c[1], 13, false);
        }

        // Magnolios históricos (hojas de azalea florecida) cerca de los pórticos
        int[][] magnoliaCoords = {
                {-34, 46}, {34, 46}, {-56, -42}, {56, -42},
                {-64, 62}, {64, 62}
        };
        for (int[] m : magnoliaCoords) {
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
        fillBox(world, chunkBox, cx - 1, midY, cz, cx + 1, midY, cz, frame);
        fillBox(world, chunkBox, cx, midY, cz - 1, cx, midY, cz + 1, frame);
        setBlock(world, chunkBox, cx, midY - 1, cz, light);
        setBlock(world, chunkBox, cx - 1, midY - 1, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx + 1, midY - 1, cz, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY - 1, cz - 1, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, cx, midY - 1, cz + 1, Blocks.END_ROD.getDefaultState());
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
}
