package pruebano1.modid.world;

import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.nbt.NbtCompound;
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
import pruebano1.modid.block.PodcastCassetteBlock;

/**
 * Generador colosal (420x420 bloques, casi del tamaño de Isla Penstein) de la segunda gran Dungeon:
 * "La Casa de José Torres", basada fielmente en las 3 fotografías de referencia:
 * - Foto 1 y 3 (Frente y Explanada Sur): Enorme explanada y calzada de concreto claro con juntas diagonales,
 *   jardines verdes con arbustos y rocas decorativas, gran seto verde recortado con reja blanca metálica,
 *   cipreses altos y esbeltos, y el Pórtico Principal abovedado sostenido por altas columnas clásicas blancas.
 * - Mansión de 2 Pisos en estuco crema/beige con marcos blancos, ventanales arqueados, tejados escalonados
 *   de teja café oscura, alta chimenea crema en el ala izquierda trasera y un interior de lujo completamente
 *   amueblado y detallado (Vestíbulo imperial de doble altura, Sala con chimenea y cine en casa, Estudio de
 *   Grabación/Podcast, Cocina Gourmet, Gran Comedor, Salón de Billar y Bar VIP, Garaje Triple con autos,
 *   Master Suite con jacuzzi y 3 Suites de invitados).
 * - Foto 2 (Patio Trasero Norte): Porche trasero techado con columnas blancas y asador de piedra rústica,
 *   jardín con camino de losas, la enorme Piscina Curva Turquesa con Jacuzzi circular integrado, barda de
 *   madera rodeada de pinos gigantes y el bloque indestructible "Cassete de Podcast" al lado de la piscina.
 */
public class CasaJoseTorresPiece extends StructurePiece {

    /** Radio interior de la propiedad (176 bloques = 352x352 bloques de mansión, explanadas, jardines y piscina). */
    public static final int ESTATE_RADIUS = 176;
    /** Franja perimetral de 34 bloques donde el terreno nevado se suaviza en rampa sin cortes bruscos (total 420x420). */
    public static final int GRADE_RANGE = 34;
    public static final int TOTAL_RADIUS = ESTATE_RADIUS + GRADE_RANGE; // 210 -> 420x420 bloques

    /** Posición relativa del Cassete de Podcast en el patio trasero justo al lado de la piscina. */
    public static final int CASSETTE_REL_X = 0;
    public static final int CASSETTE_REL_Z = -128;

    private final int centerX;
    private final int baseY;
    private final int centerZ;

    public CasaJoseTorresPiece(int centerX, int baseY, int centerZ) {
        super(
                ModStructures.CASA_JOSE_TORRES_PIECE,
                0,
                new BlockBox(
                        centerX - 120,
                        baseY - 24,
                        centerZ - 120,
                        centerX + 120,
                        baseY + 45,
                        centerZ + 120
                )
        );
        this.centerX = centerX;
        this.baseY = baseY;
        this.centerZ = centerZ;
        this.setOrientation(Direction.SOUTH);
    }

    public CasaJoseTorresPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.CASA_JOSE_TORRES_PIECE, nbt);
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
        // Generado íntegramente por generateChunkForHouse desde CasaJoseTorresFeature
    }

    public static void generateChunkForHouse(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        // 1. Nivelación de los 420x420 bloques y transición suave sin cortes bruscos con el terreno exterior
        prepareEstateTerrainAndBlending(world, chunkBox, centerX, baseY, centerZ);

        // 2. Barda perimetral de madera, avenidas exteriores, jardines, setos, rocas, reja blanca y árboles
        buildEstateGroundsAndFrontDriveway(world, chunkBox, centerX, baseY, centerZ);

        // 3. Patio trasero (Foto 2): porche techado con asador de piedra, piscina curva con jacuzzi y Cassete de Podcast
        buildBackyardPoolAndPodcastAltar(world, chunkBox, centerX, baseY, centerZ);

        // 4. Casco arquitectónico de La Casa de José Torres (2 pisos en estuco crema, ventanales arqueados, chimenea y techos)
        buildMansionShellAndRoofs(world, chunkBox, centerX, baseY, centerZ);

        // 5. Pórtico Principal Monumental ("La nueva casa de José Torres", Foto 3)
        buildGrandFrontPortico(world, chunkBox, centerX, baseY, centerZ);

        // 6. Decoración interior detallada de Planta Baja y Planta Alta
        buildMansionInteriorDetails(world, chunkBox, centerX, baseY, centerZ, random);

        // 7. Pinos gigantes del patio trasero (Foto 2) y cipreses esbeltos frontales (Foto 1)
        buildEstateTrees(world, chunkBox, centerX, baseY, centerZ);

        // 8. Aplicar cualquier modificación personalizada guardada con "/casajosetorres guardar"
        CasaJoseTorresCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
    }

    // =========================================================================
    // 1. TERRENO Y TRANSICIÓN SUAVE SIN CORTES BRUSCOS EN BIOMAS TERRESTRES
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
        int topClearY = Math.min(world.getTopYInclusive(), baseY + 46);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        BlockState air = Blocks.AIR.getDefaultState();
        BlockState stoneFoundation = Blocks.STONE.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;

                // Distancia radial suavizada (esquinas redondeadas para evitar cortes cuadrados en el terreno)
                double chebyshev = Math.max(Math.abs(dx), Math.abs(dz));
                double euclidean = Math.sqrt((double) dx * dx + (double) dz * dz);
                double effectiveDist = chebyshev * 0.55D + euclidean * 0.45D;

                if (effectiveDist <= ESTATE_RADIUS) {
                    // Cimiento sólido hasta abajo para que ninguna parte de la propiedad quede flotando
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

                    // Superficie base de pasto verde en toda la propiedad (sin nieve)
                    pos.set(wx, baseY, wz);
                    world.setBlockState(pos, grass, Block.NOTIFY_LISTENERS);

                    // Limpiar todo el aire superior
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

                // Franja de transición suave (distOut de 1 a 34 bloques):
                double distOut = effectiveDist - ESTATE_RADIUS;

                // A) Si la colina/montaña natural es más alta que la propiedad, rebajarla en talud curvo suave
                //    y cubrir la nueva superficie con pasto y tierra para evitar cualquier pared cortada.
                int capY = baseY + (int) Math.floor((distOut * distOut) / 14.0D + distOut * 0.55D);
                boolean trimmedHighTerrain = false;
                for (int y = topClearY; y > capY; y--) {
                    pos.set(wx, y, wz);
                    BlockState st = world.getBlockState(pos);
                    if (!st.isAir()) {
                        if (!st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)
                                && !st.isIn(net.minecraft.registry.tag.BlockTags.LOGS)
                                && !st.isOf(Blocks.SNOW)) {
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

                // B) Si el terreno natural es más bajo que la propiedad, bajar un talud suave de pasto y tierra
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
    // 2. EXPLANADA FRONTAL, CALZADA CON JUNTAS, SETOS, REJA BLANCA Y JARDINES
    // =========================================================================

    private static void buildEstateGroundsAndFrontDriveway(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState smoothStone = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState polishedAndesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState lightGrayConcrete = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
        BlockState quartzBlock = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState strippedSpruce = Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();
        BlockState darkOakSlab = Blocks.DARK_OAK_SLAB.getDefaultState();
        BlockState oakLeaves = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState azaleaLeaves = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState floweringAzalea = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
        BlockState lantern = Blocks.LANTERN.getDefaultState();

        int fenceRadius = 156;

        for (int wx = chunkBox.getMinX(); wx <= chunkBox.getMaxX(); wx++) {
            int dx = wx - centerX;
            for (int wz = chunkBox.getMinZ(); wz <= chunkBox.getMaxZ(); wz++) {
                int dz = wz - centerZ;

                // 1. Barda perimetral de madera estilo californiano (Foto 2) con pilares cada 8 bloques
                boolean onFenceX = Math.abs(dx) == fenceRadius && Math.abs(dz) <= fenceRadius;
                boolean onFenceZ = Math.abs(dz) == fenceRadius && Math.abs(dx) <= fenceRadius;
                boolean isSouthGateOpening = dz == fenceRadius && Math.abs(dx) <= 14;

                if ((onFenceX || onFenceZ) && !isSouthGateOpening) {
                    boolean isPost = (dx % 8 == 0) || (dz % 8 == 0);
                    for (int ry = 1; ry <= 4; ry++) {
                        setBlock(world, chunkBox, wx, baseY + ry, wz, isPost ? strippedSpruce : sprucePlanks);
                    }
                    setBlock(world, chunkBox, wx, baseY + 5, wz, darkOakSlab);
                    if (isPost && ((dx + dz) % 16 == 0)) {
                        setBlock(world, chunkBox, wx, baseY + 5, wz, lantern);
                    }
                }

                // 2. Gran Calzada y Explanada Frontal de Concreto Claro con juntas diagonales y rectas (Fotos 1 y 3)
                boolean inMainEntryRoad = dz >= 96 && dz <= 162 && Math.abs(dx) <= 14;
                boolean inFrontConcretePlaza = dz >= 26 && dz <= 96 && dx >= -78 && dx <= 42
                        && !(dx >= -36 && dx <= 36 && dz >= 46 && dz <= 76 && Math.abs(dx) > 16); // Dejar jardines de césped a los lados
                boolean inWestGarageApron = dz >= -28 && dz <= 36 && dx >= -92 && dx <= -67;
                boolean inSideWalkwayEast = dz >= -42 && dz <= 42 && dx >= 69 && dx <= 76;

                if (inMainEntryRoad || inFrontConcretePlaza || inWestGarageApron || inSideWalkwayEast) {
                    // Juntas de dilatación diagonales y ortogonales como en el pavimento de las Fotos 1 y 3
                    boolean expansionJoint = ((dx + dz) % 14 == 0) || (dx % 12 == 0) || (dz % 12 == 0);
                    BlockState paver = expansionJoint ? polishedAndesite
                            : (((dx * 31 + dz * 17) & 3) == 0 ? lightGrayConcrete : smoothStone);
                    setBlock(world, chunkBox, wx, baseY, wz, paver);
                }

                // 3. Sendero perimetral de jardín que conecta el frente con el gran patio trasero
                boolean inGardenLoop = (Math.abs(dx) >= 112 && Math.abs(dx) <= 116 && dz >= -135 && dz <= 125)
                        || (dz >= 121 && dz <= 125 && Math.abs(dx) <= 116);
                if (inGardenLoop) {
                    setBlock(world, chunkBox, wx, baseY, wz, (dx + dz) % 5 == 0 ? polishedAndesite : smoothStone);
                }
            }
        }

        // 4. Gran Portal de Entrada Sur (dz = 156, dx = -16..16)
        for (int side : new int[]{-15, 15}) {
            fillBox(world, chunkBox, centerX + side - 1, baseY + 1, centerZ + 155,
                    centerX + side + 1, baseY + 8, centerZ + 157, quartzBlock);
            fillBox(world, chunkBox, centerX + side - 2, baseY + 9, centerZ + 154,
                    centerX + side + 2, baseY + 9, centerZ + 158, chiseledQuartz);
            setBlock(world, chunkBox, centerX + side, baseY + 10, centerZ + 156, Blocks.SEA_LANTERN.getDefaultState());
        }
        // Arco superior del portal de bienvenida
        fillBox(world, chunkBox, centerX - 16, baseY + 8, centerZ + 155,
                centerX + 16, baseY + 9, centerZ + 157, quartzBlock);

        // 5. Gran Seto Verde Recortado con la Reja Metálica Blanca al frente-izquierda (exactamente como en la Foto 1)
        // Seto denso de 4 bloques de alto en dx = -56..-38, dz = 28..38
        for (int x = -56; x <= -38; x++) {
            for (int z = 28; z <= 38; z++) {
                for (int y = 1; y <= 5; y++) {
                    if (y == 5 && (x == -56 || x == -38 || z == 28 || z == 38)) {
                        continue; // Borde redondeado del seto recortado
                    }
                    setBlock(world, chunkBox, centerX + x, baseY + y, centerZ + z,
                            ((x + y + z) % 3 == 0) ? azaleaLeaves : oakLeaves);
                }
            }
        }
        // Reja blanca / metálica junto al seto (Foto 1: dx = -58, dz = 28..38)
        for (int z = 28; z <= 38; z++) {
            for (int y = 1; y <= 3; y++) {
                setBlock(world, chunkBox, centerX - 58, baseY + y, centerZ + z,
                        (z == 28 || z == 33 || z == 38) ? quartzBlock : Blocks.IRON_BARS.getDefaultState());
            }
        }

        // 6. Camas de Arbustos Frondosos y Rocas Decorativas frente a la fachada y el pórtico (Fotos 1 y 3)
        buildShrubBedWithBoulders(world, chunkBox, centerX - 28, baseY, centerZ + 34, 8, 5, oakLeaves, floweringAzalea);
        buildShrubBedWithBoulders(world, chunkBox, centerX + 28, baseY, centerZ + 30, 10, 4, oakLeaves, azaleaLeaves);
        buildShrubBedWithBoulders(world, chunkBox, centerX + 48, baseY, centerZ + 28, 9, 4, oakLeaves, floweringAzalea);
        // Roca decorativa suelta en el césped como en la Foto 1 (derecha) y Foto 3 (izquierda)
        buildDecorativeBoulder(world, chunkBox, centerX + 34, baseY + 1, centerZ + 38);
        buildDecorativeBoulder(world, chunkBox, centerX - 33, baseY + 1, centerZ + 37);

        // 7. Fuentes y glorietas ornamentales en los jardines laterales (Este y Oeste) para enriquecer la gran propiedad
        buildGardenFountainPlaza(world, chunkBox, centerX - 96, baseY, centerZ + 72);
        buildGardenFountainPlaza(world, chunkBox, centerX + 96, baseY, centerZ + 72);
        buildOutdoorGazeboLounge(world, chunkBox, centerX + 96, baseY, centerZ - 48);
        buildOutdoorBasketballCourt(world, chunkBox, centerX - 98, baseY, centerZ - 52);
    }

    private static void buildShrubBedWithBoulders(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int baseY,
            int cz,
            int rx,
            int rz,
            BlockState mainLeaf,
            BlockState accentLeaf
    ) {
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double norm = (double) (dx * dx) / (rx * rx) + (double) (dz * dz) / (rz * rz);
                if (norm <= 1.0D) {
                    setBlock(world, chunkBox, cx + dx, baseY, cz + dz, Blocks.COARSE_DIRT.getDefaultState());
                    int maxH = norm < 0.45D ? 3 : (norm < 0.8D ? 2 : 1);
                    for (int y = 1; y <= maxH; y++) {
                        BlockState leaf = ((dx + dz + y) % 3 == 0) ? accentLeaf : mainLeaf;
                        setBlock(world, chunkBox, cx + dx, baseY + y, cz + dz, leaf);
                    }
                }
            }
        }
    }

    private static void buildDecorativeBoulder(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int bx,
            int by,
            int bz
    ) {
        setBlock(world, chunkBox, bx, by, bz, Blocks.ANDESITE.getDefaultState());
        setBlock(world, chunkBox, bx + 1, by, bz, Blocks.STONE.getDefaultState());
        setBlock(world, chunkBox, bx, by, bz + 1, Blocks.COBBLESTONE_SLAB.getDefaultState());
        setBlock(world, chunkBox, bx, by + 1, bz, Blocks.ANDESITE_SLAB.getDefaultState());
    }

    private static void buildGardenFountainPlaza(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int fx,
            int baseY,
            int fz
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseled = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();

        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 12.2D) {
                    setBlock(world, chunkBox, fx + dx, baseY, fz + dz,
                            d > 10.5D ? chiseled : Blocks.POLISHED_ANDESITE.getDefaultState());
                }
                if (d <= 7.2D) {
                    if (d >= 6.0D) {
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, quartz);
                    } else {
                        setBlock(world, chunkBox, fx + dx, baseY, fz + dz,
                                (dx % 2 == 0 && dz % 2 == 0) ? seaLantern : Blocks.PRISMARINE_BRICKS.getDefaultState());
                        setBlock(world, chunkBox, fx + dx, baseY + 1, fz + dz, water);
                    }
                }
            }
        }
        // Columna central de agua de la fuente
        for (int y = 1; y <= 5; y++) {
            setBlock(world, chunkBox, fx, baseY + y, fz, chiseled);
        }
        setBlock(world, chunkBox, fx, baseY + 6, fz, seaLantern);
        setBlock(world, chunkBox, fx, baseY + 7, fz, water);
    }

    private static void buildOutdoorGazeboLounge(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int gx,
            int baseY,
            int gz
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState darkOakSlab = Blocks.DARK_OAK_SLAB.getDefaultState();
        BlockState darkOakPlanks = Blocks.DARK_OAK_PLANKS.getDefaultState();

        fillBox(world, chunkBox, gx - 10, baseY, gz - 10, gx + 10, baseY, gz + 10, quartz);
        for (int sx : new int[]{-9, 9}) {
            for (int sz : new int[]{-9, 9}) {
                fillBox(world, chunkBox, gx + sx, baseY + 1, gz + sz, gx + sx, baseY + 5, gz + sz, pillar);
            }
        }
        fillBox(world, chunkBox, gx - 10, baseY + 6, gz - 10, gx + 10, baseY + 6, gz + 10, darkOakPlanks);
        fillBox(world, chunkBox, gx - 8, baseY + 7, gz - 8, gx + 8, baseY + 7, gz + 8, darkOakSlab);
        setBlock(world, chunkBox, gx, baseY + 5, gz, Blocks.SEA_LANTERN.getDefaultState());
        // Fogata lounge central con sillones alrededor
        setBlock(world, chunkBox, gx, baseY + 1, gz, Blocks.CAMPFIRE.getDefaultState());
        fillBox(world, chunkBox, gx - 4, baseY + 1, gz - 4, gx + 4, baseY + 1, gz - 4,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        fillBox(world, chunkBox, gx - 4, baseY + 1, gz + 4, gx + 4, baseY + 1, gz + 4,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
    }

    private static void buildOutdoorBasketballCourt(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int baseY,
            int cz
    ) {
        BlockState courtBlue = Blocks.CYAN_TERRACOTTA.getDefaultState();
        BlockState courtRed = Blocks.RED_TERRACOTTA.getDefaultState();
        BlockState lineWhite = Blocks.SMOOTH_QUARTZ.getDefaultState();

        for (int dx = -14; dx <= 14; dx++) {
            for (int dz = -22; dz <= 22; dz++) {
                boolean borderLine = Math.abs(dx) == 14 || Math.abs(dz) == 22 || dz == 0
                        || (Math.abs(dx) <= 4 && Math.abs(dz) >= 16);
                boolean centerCircle = (dx * dx + dz * dz) >= 12 && (dx * dx + dz * dz) <= 18;
                BlockState floor = (borderLine || centerCircle) ? lineWhite
                        : ((Math.abs(dx) <= 4 && Math.abs(dz) >= 16) ? courtRed : courtBlue);
                setBlock(world, chunkBox, cx + dx, baseY, cz + dz, floor);
            }
        }
        // Tableros de básquetbol Norte y Sur (aro naranja y red decorativa sin tolvas)
        for (int zSide : new int[]{-22, 22}) {
            int dirOffset = zSide < 0 ? zSide + 1 : zSide - 1;
            int hoopOffset = zSide < 0 ? zSide + 2 : zSide - 2;
            fillBox(world, chunkBox, cx, baseY + 1, cz + zSide, cx, baseY + 5, cz + zSide, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
            fillBox(world, chunkBox, cx - 2, baseY + 5, cz + dirOffset,
                    cx + 2, baseY + 7, cz + dirOffset, lineWhite);
            setBlock(world, chunkBox, cx, baseY + 5, cz + hoopOffset, Blocks.ACACIA_TRAPDOOR.getDefaultState());
            setBlock(world, chunkBox, cx, baseY + 4, cz + hoopOffset, Blocks.COBWEB.getDefaultState());
        }
    }

    // =========================================================================
    // 3. PATIO TRASERO (FOTO 2): PORCHE CON ASADOR, PISCINA CURVA CON JACUZZI
    //    Y EL BLOQUE INDESTRUCTIBLE "CASSETE DE PODCAST" AL LADO DE LA PISCINA
    // =========================================================================

    private static void buildBackyardPoolAndPodcastAltar(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState smoothStone = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState polishedAndesite = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState prismarineBricks = Blocks.PRISMARINE_BRICKS.getDefaultState();
        BlockState blueTile = Blocks.BLUE_TERRACOTTA.getDefaultState();
        BlockState cyanPoolBottom = Blocks.CYAN_TERRACOTTA.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState quartzPillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState quartzSlab = Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();
        BlockState cobble = Blocks.COBBLESTONE.getDefaultState();
        BlockState stoneBricks = Blocks.STONE_BRICKS.getDefaultState();

        // 1. Porche Trasero Techado con Comedor Exterior y Asador (Foto 2: dx = -34..+28, dz = -44..-32)
        fillBox(world, chunkBox, centerX - 34, baseY, centerZ - 44, centerX + 28, baseY, centerZ - 32, smoothStone);
        // Techo del porche trasero a altura Y+5 con moldura de madera oscura
        fillBox(world, chunkBox, centerX - 34, baseY + 5, centerZ - 44, centerX + 28, baseY + 5, centerZ - 32, quartzSlab);
        fillBox(world, chunkBox, centerX - 34, baseY + 6, centerZ - 44, centerX + 28, baseY + 6, centerZ - 44, Blocks.DARK_OAK_SLAB.getDefaultState());
        // Columnas blancas con basa y capitel del porche trasero (Foto 2) + faroles colgantes
        for (int px : new int[]{-33, -18, -4, 12, 27}) {
            setBlock(world, chunkBox, centerX + px, baseY + 1, centerZ - 43, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
            fillBox(world, chunkBox, centerX + px, baseY + 2, centerZ - 43, centerX + px, baseY + 4, centerZ - 43, quartzPillar);
            setBlock(world, chunkBox, centerX + px, baseY + 4, centerZ - 44, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        }
        // Isla de Asador / BBQ de piedra rústica a la izquierda del porche trasero (Foto 2: dx = -32..-20, dz = -43..-39)
        fillBox(world, chunkBox, centerX - 32, baseY + 1, centerZ - 43, centerX - 20, baseY + 1, centerZ - 40, cobble);
        fillBox(world, chunkBox, centerX - 32, baseY + 2, centerZ - 43, centerX - 20, baseY + 2, centerZ - 40,
                Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 30, baseY + 1, centerZ - 41, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 28, baseY + 1, centerZ - 41, Blocks.SMOKER.getDefaultState().with(SmokerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 26, baseY + 1, centerZ - 41, Blocks.CAMPFIRE.getDefaultState());
        setBlock(world, chunkBox, centerX - 26, baseY + 2, centerZ - 41, Blocks.AIR.getDefaultState());
        setBlock(world, chunkBox, centerX - 23, baseY + 1, centerZ - 41,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 23, baseY + 2, centerZ - 42, Blocks.TRIPWIRE_HOOK.getDefaultState());
        setBlock(world, chunkBox, centerX - 21, baseY + 2, centerZ - 41, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX - 29, baseY + 2, centerZ - 41, Blocks.DECORATED_POT.getDefaultState());

        // Mesa de comedor exterior y sillas bajo el porche trasero (dx = 4..10, dz = -39..-35)
        fillBox(world, chunkBox, centerX + 5, baseY + 1, centerZ - 39, centerX + 9, baseY + 1, centerZ - 36, Blocks.DARK_OAK_FENCE.getDefaultState());
        fillBox(world, chunkBox, centerX + 5, baseY + 2, centerZ - 39, centerX + 9, baseY + 2, centerZ - 36, Blocks.BROWN_CARPET.getDefaultState());
        setBlock(world, chunkBox, centerX + 7, baseY + 2, centerZ - 37, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        for (int tx = 5; tx <= 9; tx += 2) {
            setBlock(world, chunkBox, centerX + tx, baseY + 1, centerZ - 40, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            setBlock(world, chunkBox, centerX + tx, baseY + 1, centerZ - 35, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        }

        // Banca exterior blanca y macetas a la derecha del porche (Foto 2: dx = 14..24, dz = -35)
        fillBox(world, chunkBox, centerX + 14, baseY + 1, centerZ - 35, centerX + 24, baseY + 1, centerZ - 35,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 13, baseY + 1, centerZ - 35, Blocks.POTTED_FERN.getDefaultState());
        setBlock(world, chunkBox, centerX + 25, baseY + 1, centerZ - 35, Blocks.POTTED_RED_TULIP.getDefaultState());

        // 2. Camino de losas de piedra sobre el césped desde el porche hasta la piscina (Foto 2: dz = -45..-60)
        for (int z = -59; z <= -45; z += 3) {
            fillBox(world, chunkBox, centerX - 16, baseY, centerZ + z, centerX - 12, baseY, centerZ + z + 1, smoothStone);
        }

        // 3. La Enorme Piscina Curva Turquesa con Jacuzzi Circular Integrado (Foto 2: dx = -56..+56, dz = -142..-54)
        for (int wx = chunkBox.getMinX(); wx <= chunkBox.getMaxX(); wx++) {
            int dx = wx - centerX;
            for (int wz = chunkBox.getMinZ(); wz <= chunkBox.getMaxZ(); wz++) {
                int dz = wz - centerZ;
                if (dx < -64 || dx > 64 || dz < -150 || dz > -50) {
                    continue;
                }

                // Silueta curva orgánica de tres lóbulos inspirada directamente en la Foto 2
                double lobeMain = 1.0D - (Math.pow(dx / 34.0D, 2) + Math.pow((dz + 92.0D) / 22.0D, 2));
                double lobeWest = 1.0D - (Math.pow((dx + 22.0D) / 20.0D, 2) + Math.pow((dz + 84.0D) / 16.0D, 2));
                double lobeEast = 1.0D - (Math.pow((dx - 24.0D) / 21.0D, 2) + Math.pow((dz + 98.0D) / 18.0D, 2));
                double poolShape = Math.max(lobeMain, Math.max(lobeWest, lobeEast));

                // Jacuzzi circular integrado en el borde sur de la piscina (Foto 2: centro en dx = -4, dz = -68, radio 6.5)
                double spaDist = Math.sqrt(Math.pow(dx + 4.0D, 2) + Math.pow(dz + 68.0D, 2));

                if (spaDist <= 6.8D) {
                    if (spaDist >= 5.3D) {
                        // Borde elevado redondeado del jacuzzi
                        setBlock(world, chunkBox, wx, baseY, wz, stoneBricks);
                        setBlock(world, chunkBox, wx, baseY + 1, wz, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
                    } else {
                        // Interior del jacuzzi con agua templada iluminada
                        for (int y = -3; y <= -2; y++) {
                            setBlock(world, chunkBox, wx, baseY + y, wz,
                                    (dx + dz) % 2 == 0 ? seaLantern : cyanPoolBottom);
                        }
                        setBlock(world, chunkBox, wx, baseY - 1, wz, water);
                        setBlock(world, chunkBox, wx, baseY, wz, water);
                    }
                    continue;
                }

                if (poolShape > 0.08D) {
                    // Interior de la gran piscina curva turquesa (profundidad de 4 bloques)
                    for (int y = -4; y <= -1; y++) {
                        setBlock(world, chunkBox, wx, baseY + y, wz, water);
                    }
                    setBlock(world, chunkBox, wx, baseY, wz, water);
                    // Fondo turquesa luminoso
                    boolean lightSpot = (Math.abs(dx) % 6 == 0) && (Math.abs(dz) % 6 == 0);
                    setBlock(world, chunkBox, wx, baseY - 5, wz, lightSpot ? seaLantern : cyanPoolBottom);
                } else if (poolShape > -0.06D) {
                    // Pared de la piscina con cenefa superior de azulejo azul (como en la Foto 2)
                    for (int y = -5; y <= -1; y++) {
                        setBlock(world, chunkBox, wx, baseY + y, wz, y == -1 ? blueTile : prismarineBricks);
                    }
                    setBlock(world, chunkBox, wx, baseY, wz, blueTile);
                } else if (poolShape > -0.48D || (Math.abs(dx) <= 44 && dz >= -142 && dz <= -114)) {
                    // Amplio deck / terraza de concreto alrededor de la piscina y en la zona del Cassete de Podcast
                    boolean joint = ((dx + dz) % 9 == 0);
                    setBlock(world, chunkBox, wx, baseY, wz, joint ? polishedAndesite : smoothStone);
                }
            }
        }

        // 4. Camastros y sombrillas en el deck lateral de la piscina
        for (int cx : new int[]{-44, -36, 36, 44}) {
            setBlock(world, chunkBox, centerX + cx, baseY + 1, centerZ - 92, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            setBlock(world, chunkBox, centerX + cx, baseY + 1, centerZ - 91,
                    Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        }

        // 5. ALTAR DEL CASSETE DE PODCAST AL LADO DE LA PISCINA (en el patio trasero: dx = 0, dz = -128)
        int ax = centerX + CASSETTE_REL_X;
        int az = centerZ + CASSETTE_REL_Z;

        // Plataforma circular de cuarzo, oro y hielo compacto junto al borde norte de la piscina
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 6.2D) {
                    BlockState ringState = d > 5.0D
                            ? Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState()
                            : (d > 3.2D ? Blocks.PACKED_ICE.getDefaultState() : quartz);
                    setBlock(world, chunkBox, ax + dx, baseY, az + dz, ringState);
                }
            }
        }

        // Pedestal central decorativo iluminado con el bloque indestructible "Cassete de Podcast"
        fillBox(world, chunkBox, ax - 1, baseY + 1, az - 1, ax + 1, baseY + 1, az + 1, quartz);
        setBlock(world, chunkBox, ax, baseY + 1, az, Blocks.OCHRE_FROGLIGHT.getDefaultState());
        setBlock(world, chunkBox, ax - 1, baseY + 1, az, seaLantern);
        setBlock(world, chunkBox, ax + 1, baseY + 1, az, seaLantern);
        setBlock(world, chunkBox, ax, baseY + 1, az - 1, seaLantern);
        setBlock(world, chunkBox, ax, baseY + 1, az + 1, seaLantern);

        // El bloque indestructible Cassete de Podcast mirando hacia la piscina y la casa (Sur)
        setBlock(
                world,
                chunkBox,
                ax,
                baseY + 2,
                az,
                ModBlocks.PODCAST_CASSETTE.getDefaultState().with(PodcastCassetteBlock.FACING, Direction.SOUTH)
        );

        // Dos torres de bocinas de podcast flanqueando el altar junto a la piscina
        for (int side : new int[]{-8, 8}) {
            fillBox(world, chunkBox, ax + side - 1, baseY + 1, az - 1,
                    ax + side + 1, baseY + 5, az + 1, Blocks.BLACK_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, ax + side, baseY + 2, az + 1, Blocks.NOTE_BLOCK.getDefaultState());
            setBlock(world, chunkBox, ax + side, baseY + 4, az + 1, Blocks.NOTE_BLOCK.getDefaultState());
            setBlock(world, chunkBox, ax + side, baseY + 6, az, Blocks.SEA_LANTERN.getDefaultState());
        }
    }

    // =========================================================================
    // 4. CASCO PRINCIPAL DE LA MANSIÓN DE JOSÉ TORRES (FOTOS 1, 2 Y 3)
    // =========================================================================

    private static void buildMansionShellAndRoofs(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState creamStucco = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState cutSandstone = Blocks.CUT_SANDSTONE.getDefaultState();
        BlockState whiteTrim = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState baseStone = Blocks.STONE_BRICKS.getDefaultState();
        BlockState windowGlass = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        BlockState darkRoofPlank = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState darkRoofSlab = Blocks.DARK_OAK_SLAB.getDefaultState();
        BlockState marbleFloor = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState darkTile = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        // 1. Pisos, entrepiso y muros exteriores del cuerpo principal de 2 pisos (dx = -44..+46, dz = -31..+25)
        for (int dx = -44; dx <= 46; dx++) {
            for (int dz = -31; dz <= 25; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                if (!chunkBox.contains(wx, baseY, wz)) {
                    continue;
                }

                // Piso de Planta Baja (Y = baseY): mármol ajedrezado en Vestíbulo/Cocina/Baños y madera noble en cuartos
                boolean foyerZone = dx >= -18 && dx <= 18 && dz >= -30 && dz <= 25;
                boolean kitchenOrBath = (dx >= 19 && dx <= 45 && dz >= -30 && dz <= -10);
                BlockState groundFloorBlock;
                if (foyerZone || kitchenOrBath) {
                    groundFloorBlock = ((dx + dz) & 1) == 0 ? marbleFloor : darkTile;
                } else {
                    groundFloorBlock = Blocks.BIRCH_PLANKS.getDefaultState();
                }
                setBlock(world, chunkBox, wx, baseY, wz, groundFloorBlock);

                boolean isOuterWall = dx == -44 || dx == 46 || dz == -31 || dz == 25;
                boolean isCornerPilaster = (Math.abs(dx + 44) <= 1 || Math.abs(dx - 46) <= 1)
                        && (Math.abs(dz + 31) <= 1 || Math.abs(dz - 25) <= 1);
                for (int ry = 1; ry <= 18; ry++) {
                    if (isOuterWall) {
                        BlockState wallMat;
                        if (ry == 1) {
                            wallMat = baseStone;
                        } else if (ry == 9 || ry == 10 || ry == 18) {
                            wallMat = whiteTrim;
                        } else if (isCornerPilaster || Math.abs(dx) % 15 == 0 || Math.abs(dz) % 14 == 0) {
                            wallMat = cutSandstone;
                        } else {
                            wallMat = creamStucco;
                        }
                        setBlock(world, chunkBox, wx, baseY + ry, wz, wallMat);
                    } else if (ry == 9) {
                        // Entrepiso del Segundo Piso (con gran atrio de doble altura frente al Foyer: dx = -12..+12, dz = +1..+18)
                        boolean foyerVoid = dx >= -12 && dx <= 12 && dz >= 1 && dz <= 18;
                        if (!foyerVoid) {
                            setBlock(world, chunkBox, wx, baseY + ry, wz,
                                    (Math.abs(dx) >= 19 && dz >= -30 && dz <= -7 && dx >= 19 && dx <= 33)
                                            ? (((dx + dz) & 1) == 0 ? marbleFloor : darkTile)
                                            : Blocks.SPRUCE_PLANKS.getDefaultState());
                        } else {
                            setBlock(world, chunkBox, wx, baseY + ry, wz, air);
                        }
                    } else if (ry == 18) {
                        setBlock(world, chunkBox, wx, baseY + ry, wz, whiteTrim);
                    } else {
                        setBlock(world, chunkBox, wx, baseY + ry, wz, air);
                    }
                }
            }
        }

        // 2. Cornisa de alero saliente alrededor del cuerpo principal (ry = 18, evita cortes rectos en bordes)
        for (int dx = -45; dx <= 47; dx++) {
            setBlock(world, chunkBox, centerX + dx, baseY + 18, centerZ + 26,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            setBlock(world, chunkBox, centerX + dx, baseY + 18, centerZ - 32,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        }
        for (int dz = -32; dz <= 26; dz++) {
            setBlock(world, chunkBox, centerX - 45, baseY + 18, centerZ + dz,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            setBlock(world, chunkBox, centerX + 47, baseY + 18, centerZ + dz,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        }

        // 3. Tejado piramidal sólido a cuatro aguas (1 bloque por escalón para 0 huecos entre capas, ry = 19..28)
        for (int layer = 0; layer <= 9; layer++) {
            int ry = 19 + layer;
            int minX = -45 + layer * 2;
            int maxX = 47 - layer * 2;
            int minZ = -32 + layer * 2;
            int maxZ = 26 - layer * 2;
            if (minX > maxX || minZ > maxZ) {
                break;
            }
            for (int dx = minX; dx <= maxX; dx++) {
                for (int dz = minZ; dz <= maxZ; dz++) {
                    boolean outerBand = dx <= minX + 1 || dx >= maxX - 1 || dz <= minZ + 1 || dz >= maxZ - 1 || layer == 9;
                    if (!outerBand) {
                        continue;
                    }
                    boolean veryEdge = dx == minX || dx == maxX || dz == minZ || dz == maxZ;
                    if (veryEdge && layer < 9) {
                        Direction stairDir;
                        if (dz == maxZ && dx > minX && dx < maxX) {
                            stairDir = Direction.NORTH;
                        } else if (dz == minZ && dx > minX && dx < maxX) {
                            stairDir = Direction.SOUTH;
                        } else if (dx == minX && dz > minZ && dz < maxZ) {
                            stairDir = Direction.EAST;
                        } else if (dx == maxX && dz > minZ && dz < maxZ) {
                            stairDir = Direction.WEST;
                        } else {
                            stairDir = null;
                        }
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz,
                                stairDir != null ? Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, stairDir) : darkRoofSlab);
                    } else {
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz,
                                layer == 9 ? darkRoofSlab : darkRoofPlank);
                    }
                }
            }
        }

        // 4. Pabellón Frontal Derecho Sobresaliente de 2 Pisos con Frontón y Gran Ventanal Arqueado (Foto 1 derecha: dx = +21..+41, dz = +25..+29)
        for (int dx = 21; dx <= 41; dx++) {
            for (int dz = 25; dz <= 29; dz++) {
                setBlock(world, chunkBox, centerX + dx, baseY, centerZ + dz,
                        (dz == 29) ? whiteTrim : Blocks.SMOOTH_STONE.getDefaultState());
                boolean outerBay = dx == 21 || dx == 41 || dz == 29;
                for (int ry = 1; ry <= 18; ry++) {
                    if (outerBay) {
                        BlockState m = (ry == 1) ? baseStone : ((ry == 9 || ry == 10 || ry == 18) ? whiteTrim : creamStucco);
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz, m);
                    } else if (ry == 9 || ry == 18) {
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz, whiteTrim);
                    } else {
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz, air);
                    }
                }
            }
            // Tejado a dos aguas del Pabellón Derecho (ry = 19..24)
            int gableH = 5 - Math.abs(dx - 31) / 2;
            for (int h = 0; h <= gableH; h++) {
                setBlock(world, chunkBox, centerX + dx, baseY + 19 + h, centerZ + 29,
                        h == gableH ? whiteTrim : creamStucco);
                if (h == gableH) {
                    for (int dz = 21; dz <= 30; dz++) {
                        setBlock(world, chunkBox, centerX + dx, baseY + 19 + h + 1, centerZ + dz,
                                dz == 30 ? darkRoofSlab : darkRoofPlank);
                    }
                }
            }
        }
        // Quitar pared interior entre Pabellón Derecho y cuerpo principal (dx = 22..40, dz = 25) para ampliar las habitaciones de abajo y arriba
        fillBox(world, chunkBox, centerX + 22, baseY + 1, centerZ + 25, centerX + 40, baseY + 8, centerZ + 25, air);
        fillBox(world, chunkBox, centerX + 22, baseY + 10, centerZ + 25, centerX + 40, baseY + 17, centerZ + 25, air);
        // Gran Ventanal Arqueado del 2.º piso y Ventanal triple de Planta Baja en la fachada del Pabellón Derecho (Foto 1)
        buildFramedArchedWindow(world, chunkBox, centerX + 31, baseY + 11, centerZ + 29, 7, 7, true, whiteTrim, windowGlass, false, Direction.SOUTH);
        buildFramedArchedWindow(world, chunkBox, centerX + 31, baseY + 2, centerZ + 29, 9, 6, true, whiteTrim, windowGlass, true, Direction.SOUTH);

        // 5. Alas Laterales: Ala Oeste (Garaje Triple, dx = -66..-45, dz = -28..+22) y Ala Este (Billar/Bar VIP, dx = +47..+64, dz = -28..+20)
        buildSideWing(world, chunkBox, centerX, baseY, centerZ, -66, -45, -28, 22, creamStucco, whiteTrim, baseStone, darkRoofPlank, darkRoofSlab);
        buildSideWing(world, chunkBox, centerX, baseY, centerZ, 47, 64, -28, 20, creamStucco, whiteTrim, baseStone, darkRoofPlank, darkRoofSlab);

        // 6. Faldón continuo de teja café oscura entre el 1.er y 2.º piso en la fachada frontal y trasera (como en la Foto 1)
        for (int dx = -45; dx <= 47; dx++) {
            if (dx >= 21 && dx <= 41) {
                setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ + 30,
                        Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
                setBlock(world, chunkBox, centerX + dx, baseY + 9, centerZ + 31, darkRoofSlab);
            } else {
                setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ + 26,
                        Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
                setBlock(world, chunkBox, centerX + dx, baseY + 9, centerZ + 27, darkRoofSlab);
            }
        }

        // 7. LA ALTA CHIMENEA CREMA EN EL ALA TRASERA IZQUIERDA (Foto 2: dx = -38..-34, dz = -34..-30, sube hasta Y+29)
        fillBox(world, chunkBox, centerX - 38, baseY + 1, centerZ - 34,
                centerX - 34, baseY + 28, centerZ - 30, creamStucco);
        fillBox(world, chunkBox, centerX - 39, baseY + 28, centerZ - 35,
                centerX - 33, baseY + 28, centerZ - 29, whiteTrim);
        fillBox(world, chunkBox, centerX - 38, baseY + 29, centerZ - 34,
                centerX - 34, baseY + 29, centerZ - 30, whiteTrim);
        setBlock(world, chunkBox, centerX - 36, baseY + 28, centerZ - 32, Blocks.CAMPFIRE.getDefaultState());

        // 8. Ventanales arqueados con alféizares y jardineras en fachadas Frontal, Trasera y Laterales
        // Frente Oeste (Sala / Cine / Invitados):
        buildFramedArchedWindow(world, chunkBox, centerX - 37, baseY + 2, centerZ + 25, 7, 6, true, whiteTrim, windowGlass, true, Direction.SOUTH);
        buildFramedArchedWindow(world, chunkBox, centerX - 24, baseY + 2, centerZ + 25, 5, 5, true, whiteTrim, windowGlass, true, Direction.SOUTH);
        buildFramedArchedWindow(world, chunkBox, centerX - 37, baseY + 11, centerZ + 25, 5, 6, true, whiteTrim, windowGlass, false, Direction.SOUTH);
        buildFramedArchedWindow(world, chunkBox, centerX - 24, baseY + 11, centerZ + 25, 5, 6, true, whiteTrim, windowGlass, false, Direction.SOUTH);
        // Tres ventanitas superiores estilo Foto 1 sobre el ala izquierda:
        for (int cx : new int[]{-15, -9, 15}) {
            buildFramedArchedWindow(world, chunkBox, centerX + cx, baseY + 12, centerZ + 25, 3, 4, true, whiteTrim, windowGlass, false, Direction.SOUTH);
        }
        // Fachada Trasera hacia la Piscina (Foto 2):
        for (int wxPos : new int[]{-36, -24, 24, 36}) {
            buildFramedArchedWindow(world, chunkBox, centerX + wxPos, baseY + 2, centerZ - 31, 5, 6, true, whiteTrim, windowGlass, true, Direction.NORTH);
            buildFramedArchedWindow(world, chunkBox, centerX + wxPos, baseY + 11, centerZ - 31, 5, 6, true, whiteTrim, windowGlass, false, Direction.NORTH);
        }
        buildFramedArchedWindow(world, chunkBox, centerX, baseY + 11, centerZ - 31, 9, 6, true, whiteTrim, windowGlass, false, Direction.NORTH);
        // Ventanales laterales del 2.º piso (Este y Oeste) y del Salón VIP Este:
        for (int wzPos : new int[]{-18, 0, 14}) {
            buildFramedArchedWindow(world, chunkBox, centerX - 44, baseY + 11, centerZ + wzPos, 5, 6, false, whiteTrim, windowGlass, false, Direction.WEST);
            buildFramedArchedWindow(world, chunkBox, centerX + 46, baseY + 11, centerZ + wzPos, 5, 6, false, whiteTrim, windowGlass, false, Direction.EAST);
        }
        buildFramedArchedWindow(world, chunkBox, centerX + 64, baseY + 2, centerZ - 15, 5, 5, false, whiteTrim, windowGlass, true, Direction.EAST);
        buildFramedArchedWindow(world, chunkBox, centerX + 64, baseY + 2, centerZ + 8, 5, 5, false, whiteTrim, windowGlass, true, Direction.EAST);
        buildFramedArchedWindow(world, chunkBox, centerX + 55, baseY + 2, centerZ + 20, 5, 5, true, whiteTrim, windowGlass, true, Direction.SOUTH);

        // Puertas corredizas de cristal hacia el Porche Trasero y la Piscina (Foto 2: dz = -31, dx = -9..+9)
        fillBox(world, chunkBox, centerX - 9, baseY + 1, centerZ - 31, centerX + 9, baseY + 6, centerZ - 31, whiteTrim);
        fillBox(world, chunkBox, centerX - 8, baseY + 1, centerZ - 31, centerX + 8, baseY + 5, centerZ - 31, windowGlass);
        fillBox(world, chunkBox, centerX - 2, baseY + 1, centerZ - 31, centerX + 2, baseY + 4, centerZ - 31, air);

        // 3 Portones seccionales blancos con marco e iluminación en el Garaje Oeste (dx = -66)
        for (int gzCenter : new int[]{-16, -2, 12}) {
            fillBox(world, chunkBox, centerX - 66, baseY + 1, centerZ + gzCenter - 3,
                    centerX - 66, baseY + 5, centerZ + gzCenter + 3, whiteTrim);
            for (int z = gzCenter - 2; z <= gzCenter + 2; z++) {
                setBlock(world, chunkBox, centerX - 66, baseY + 4, centerZ + z, windowGlass);
            }
            setBlock(world, chunkBox, centerX - 67, baseY + 5, centerZ + gzCenter - 4, Blocks.LANTERN.getDefaultState());
            setBlock(world, chunkBox, centerX - 67, baseY + 5, centerZ + gzCenter + 4, Blocks.LANTERN.getDefaultState());
        }
    }

    private static void buildSideWing(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int minDx,
            int maxDx,
            int minDz,
            int maxDz,
            BlockState stucco,
            BlockState whiteTrim,
            BlockState baseStone,
            BlockState roofPlank,
            BlockState roofSlab
    ) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState floor = minDx < 0 ? Blocks.POLISHED_ANDESITE.getDefaultState() : Blocks.DARK_OAK_PLANKS.getDefaultState();

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                int wx = centerX + dx;
                int wz = centerZ + dz;
                if (!chunkBox.contains(wx, baseY, wz)) {
                    continue;
                }
                setBlock(world, chunkBox, wx, baseY, wz, floor);
                boolean outer = dx == minDx || dx == maxDx || dz == minDz || dz == maxDz;
                for (int ry = 1; ry <= 9; ry++) {
                    if (outer) {
                        setBlock(world, chunkBox, wx, baseY + ry, wz,
                                ry == 1 ? baseStone : (ry == 9 ? whiteTrim : stucco));
                    } else if (ry == 9) {
                        setBlock(world, chunkBox, wx, baseY + ry, wz, whiteTrim);
                    } else {
                        setBlock(world, chunkBox, wx, baseY + ry, wz, air);
                    }
                }
            }
        }
        // Tejado sólido a cuatro aguas escalonado 1 bloque por capa con alero saliente
        for (int layer = 0; layer <= 5; layer++) {
            int ry = 10 + layer;
            int x0 = minDx - 1 + layer * 2;
            int x1 = maxDx + 1 - layer * 2;
            int z0 = minDz - 1 + layer * 2;
            int z1 = maxDz + 1 - layer * 2;
            if (x0 > x1 || z0 > z1) {
                break;
            }
            for (int dx = x0; dx <= x1; dx++) {
                for (int dz = z0; dz <= z1; dz++) {
                    boolean band = dx <= x0 + 1 || dx >= x1 - 1 || dz <= z0 + 1 || dz >= z1 - 1 || layer == 5;
                    if (band) {
                        boolean veryEdge = dx == x0 || dx == x1 || dz == z0 || dz == z1;
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + dz,
                                (veryEdge || layer == 5) ? roofSlab : roofPlank);
                    }
                }
            }
        }
    }

    private static void buildFramedArchedWindow(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int bottomY,
            int cz,
            int width,
            int height,
            boolean alongX,
            BlockState frame,
            BlockState glass,
            boolean withPlanterBox,
            Direction outwardDir
    ) {
        int halfW = width / 2;
        for (int w = -halfW; w <= halfW; w++) {
            for (int h = 0; h < height; h++) {
                int wx = alongX ? cx + w : cx;
                int wz = alongX ? cz : cz + w;
                int wy = bottomY + h;
                boolean isBorder = Math.abs(w) == halfW || h == 0 || h == height - 1
                        || (h == height - 2 && Math.abs(w) == halfW - 1);
                setBlock(world, chunkBox, wx, wy, wz, isBorder ? frame : glass);
            }
            // Alféizar saliente y jardinera exterior bajo las ventanas de planta baja
            int outX = alongX ? cx + w + outwardDir.getOffsetX() : cx + outwardDir.getOffsetX();
            int outZ = alongX ? cz + outwardDir.getOffsetZ() : cz + w + outwardDir.getOffsetZ();
            setBlock(world, chunkBox, outX, bottomY - 1, outZ,
                    Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
            if (withPlanterBox && Math.abs(w) < halfW && (w % 2 == 0)) {
                setBlock(world, chunkBox, outX, bottomY, outZ, Blocks.POTTED_RED_TULIP.getDefaultState());
            }
        }
    }

    // =========================================================================
    // 5. PÓRTICO PRINCIPAL MONUMENTAL ("LA NUEVA CASA DE JOSÉ TORRES", FOTO 3)
    // =========================================================================

    private static void buildGrandFrontPortico(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState creamStucco = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState whiteTrim = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState darkRoofPlank = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState darkRoofSlab = Blocks.DARK_OAK_SLAB.getDefaultState();
        BlockState blackFrame = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        // Piso pavimentado con escalón de entrada y borde de mármol
        fillBox(world, chunkBox, centerX - 16, baseY, centerZ + 25,
                centerX + 16, baseY, centerZ + 42, Blocks.SMOOTH_STONE.getDefaultState());
        fillBox(world, chunkBox, centerX - 5, baseY, centerZ + 25,
                centerX + 5, baseY, centerZ + 42, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillBox(world, chunkBox, centerX - 16, baseY + 1, centerZ + 26,
                centerX + 16, baseY + 10, centerZ + 42, air);

        // Las grandes columnas clásicas blancas con basa y capitel (Foto 3) + faroles de cálida bienvenida
        int[][] columnCoords = {
                {-14, 41}, {14, 41},
                {-14, 33}, {14, 33},
                {-6, 41}, {6, 41}
        };
        for (int[] col : columnCoords) {
            int cx = centerX + col[0];
            int cz = centerZ + col[1];
            fillBox(world, chunkBox, cx - 1, baseY + 1, cz - 1, cx + 1, baseY + 1, cz + 1, chiseledQuartz);
            fillBox(world, chunkBox, cx, baseY + 2, cz, cx, baseY + 8, cz, pillar);
            fillBox(world, chunkBox, cx - 1, baseY + 9, cz - 1, cx + 1, baseY + 9, cz + 1, whiteTrim);
            if (col[1] == 41) {
                setBlock(world, chunkBox, cx, baseY + 4, cz + 1, Blocks.LANTERN.getDefaultState());
            }
        }

        // Frontón en estuco crema con el Gran Arco Central Abovedado y tejado escalonado sin huecos (Foto 3)
        for (int dx = -16; dx <= 16; dx++) {
            int gableTop = 15 - (Math.abs(dx) / 3);
            if (Math.abs(dx) <= 15) {
                for (int ry = 10; ry <= gableTop; ry++) {
                    boolean inCentralArch = Math.abs(dx) <= 5 && ry <= (12 - (Math.abs(dx) / 3));
                    if (!inCentralArch) {
                        setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ + 41, creamStucco);
                    }
                }
            }
            // Moldura y teja oscura continua en toda la pendiente del pórtico
            for (int dz = 25; dz <= 43; dz++) {
                setBlock(world, chunkBox, centerX + dx, baseY + gableTop, centerZ + dz, darkRoofPlank);
                setBlock(world, chunkBox, centerX + dx, baseY + gableTop + 1, centerZ + dz, darkRoofSlab);
                if (Math.abs(dx) <= 14 && dz >= 26 && dz <= 40) {
                    boolean ceilingLight = (dx == 0 && (dz == 29 || dz == 35));
                    setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ + dz,
                            ceilingLight ? Blocks.SEA_LANTERN.getDefaultState() : creamStucco);
                }
            }
        }
        // Lámpara colgante central del pórtico
        setBlock(world, chunkBox, centerX, baseY + 9, centerZ + 33, Blocks.CHAIN.getDefaultState());
        setBlock(world, chunkBox, centerX, baseY + 8, centerZ + 33, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

        // Entrada Principal de Lujo (Foto 3: gran arco negro acristalado con puertas dobles y faroles laterales)
        fillBox(world, chunkBox, centerX - 4, baseY + 1, centerZ + 25, centerX + 4, baseY + 7, centerZ + 25, blackFrame);
        fillBox(world, chunkBox, centerX - 3, baseY + 4, centerZ + 25, centerX + 3, baseY + 6, centerZ + 25, glass);
        fillBox(world, chunkBox, centerX - 2, baseY + 1, centerZ + 25, centerX + 2, baseY + 3, centerZ + 25, glass);
        // Puerta doble funcional centrada en dx = -1..0
        placeDoubleWoodDoor(world, chunkBox, centerX - 1, baseY + 1, centerZ + 25, Direction.NORTH, false);
        placeDoubleWoodDoor(world, chunkBox, centerX, baseY + 1, centerZ + 25, Direction.NORTH, true);
        setBlock(world, chunkBox, centerX - 4, baseY + 3, centerZ + 26, Blocks.LANTERN.getDefaultState());
        setBlock(world, chunkBox, centerX + 4, baseY + 3, centerZ + 26, Blocks.LANTERN.getDefaultState());

        // Ventanales arqueados con macetas flanqueando la entrada bajo el pórtico (Foto 3)
        buildFramedArchedWindow(world, chunkBox, centerX - 10, baseY + 2, centerZ + 25, 5, 6, true, whiteTrim, glass, true, Direction.SOUTH);
        buildFramedArchedWindow(world, chunkBox, centerX + 10, baseY + 2, centerZ + 25, 5, 6, true, whiteTrim, glass, true, Direction.SOUTH);
    }

    // =========================================================================
    // 6. DECORACIÓN INTERIOR MUY DETALLADA (PLANTA BAJA Y SEGUNDO PISO)
    // =========================================================================

    private static void buildMansionInteriorDetails(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        BlockState creamWall = Blocks.SMOOTH_SANDSTONE.getDefaultState();
        BlockState trimWall = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState warmGoldTrim = Blocks.YELLOW_TERRACOTTA.getDefaultState();
        BlockState goldGlow = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState blueCarpet = Blocks.LIGHT_BLUE_CARPET.getDefaultState();
        BlockState cyanCarpet = Blocks.CYAN_CARPET.getDefaultState();
        BlockState whiteCarpet = Blocks.WHITE_CARPET.getDefaultState();
        BlockState darkOakPlanks = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState bookshelf = Blocks.BOOKSHELF.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState lanternHang = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
        BlockState lanternStand = Blocks.LANTERN.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        // =====================================================================
        // A. DIVISIONES DE HABITACIONES Y PASILLOS EN PLANTA BAJA (Y+1 .. Y+8)
        // =====================================================================
        for (int dz = -30; dz <= 24; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                boolean isHallwayArch = (dz >= -9 && dz <= -5 && ry <= 4);
                boolean isSouthDoor = (dz == 9 || dz == 10) && ry <= 2;
                boolean isNorthDoor = (dz == -19 || dz == -18) && ry <= 2;
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                if (!isHallwayArch && !isSouthDoor && !isNorthDoor) {
                    setBlock(world, chunkBox, centerX - 18, baseY + ry, centerZ + dz, mat);
                    setBlock(world, chunkBox, centerX + 18, baseY + ry, centerZ + dz, mat);
                }
            }
        }
        // Pasillo Oeste Planta Baja (dz = -10 y -4, dx = -43..-19)
        for (int dx = -43; dx <= -19; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                boolean doorNorth = (dx == -38 || dx == -37 || dx == -25 || dx == -24) && ry <= 2;
                boolean doorSouth = (dx == -38 || dx == -37 || dx == -25 || dx == -24) && ry <= 2;
                if (!doorNorth) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 10, mat);
                }
                if (!doorSouth) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 4, mat);
                }
            }
        }
        // Muros divisorios Oeste
        for (int dz = -30; dz <= -11; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                setBlock(world, chunkBox, centerX - 31, baseY + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
            }
        }
        for (int dz = -3; dz <= 24; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                boolean archConnect = (dz >= 9 && dz <= 12 && ry <= 4);
                if (!archConnect) {
                    setBlock(world, chunkBox, centerX - 31, baseY + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
                }
            }
        }
        // Pasillo Este Planta Baja (dz = -10 y -4, dx = +19..+45)
        for (int dx = 19; dx <= 45; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                boolean doorNorth = (dx == 26 || dx == 27 || dx == 40 || dx == 41) && ry <= 2;
                boolean doorSouth = (dx == 25 || dx == 26 || dx == 39 || dx == 40) && ry <= 2;
                if (!doorNorth) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 10, mat);
                }
                if (!doorSouth) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 4, mat);
                }
            }
        }
        // Muros divisorios Este
        for (int dz = -3; dz <= 24; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                boolean archConnect = (dz >= 8 && dz <= 12 && ry <= 4);
                if (!archConnect) {
                    setBlock(world, chunkBox, centerX + 34, baseY + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
                }
            }
        }
        for (int dz = -30; dz <= -11; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                setBlock(world, chunkBox, centerX + 36, baseY + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
            }
        }
        for (int dx = 37; dx <= 45; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                boolean bathDoor = (dx == 40 || dx == 41) && ry <= 2;
                if (!bathDoor) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 20, (ry == 1 || ry == 8) ? trimWall : creamWall);
                }
            }
        }

        // Conexiones a Garaje Oeste y Salón VIP Este
        fillBox(world, chunkBox, centerX - 44, baseY + 1, centerZ - 8, centerX - 44, baseY + 4, centerZ - 6, air);
        fillBox(world, chunkBox, centerX + 46, baseY + 1, centerZ - 8, centerX + 46, baseY + 4, centerZ - 6, air);

        // Puertas dobles de madera en todos los cuartos de Planta Baja
        placeDoubleDoorPair(world, chunkBox, centerX - 18, baseY + 1, centerZ + 9, false, Direction.EAST);
        placeDoubleDoorPair(world, chunkBox, centerX + 18, baseY + 1, centerZ + 9, false, Direction.WEST);
        placeDoubleDoorPair(world, chunkBox, centerX - 18, baseY + 1, centerZ - 19, false, Direction.EAST);
        placeDoubleDoorPair(world, chunkBox, centerX + 18, baseY + 1, centerZ - 19, false, Direction.WEST);
        placeDoubleDoorPair(world, chunkBox, centerX - 38, baseY + 1, centerZ - 10, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 25, baseY + 1, centerZ - 10, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 38, baseY + 1, centerZ - 4, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 25, baseY + 1, centerZ - 4, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 26, baseY + 1, centerZ - 10, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 40, baseY + 1, centerZ - 10, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 25, baseY + 1, centerZ - 4, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 39, baseY + 1, centerZ - 4, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 40, baseY + 1, centerZ - 20, true, Direction.NORTH);

        // Pasillos Oeste y Este muy decorados (alfombra, consolas, vasijas, espejos, cuadros y faroles)
        for (int dx = -43; dx <= -19; dx++) {
            setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 7, yellowCarpet);
            if (dx % 6 == 0) {
                setBlock(world, chunkBox, centerX + dx, baseY + 8, centerZ - 7, lanternHang);
            }
            if (dx == -31 || dx == -21) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 9, Blocks.DECORATED_POT.getDefaultState());
            } else if (dx == -42 || dx == -28) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 9, Blocks.POTTED_FERN.getDefaultState());
            }
        }
        placeWallArt(world, chunkBox, centerX - 31, baseY + 3, centerZ - 9, Direction.SOUTH, Blocks.CYAN_GLAZED_TERRACOTTA);
        placeWallArt(world, chunkBox, centerX - 31, baseY + 3, centerZ - 5, Direction.NORTH, Blocks.YELLOW_GLAZED_TERRACOTTA);

        for (int dx = 19; dx <= 45; dx++) {
            setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 7, yellowCarpet);
            if (dx % 6 == 0) {
                setBlock(world, chunkBox, centerX + dx, baseY + 8, centerZ - 7, lanternHang);
            }
            if (dx == 33 || dx == 44) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 9, Blocks.DECORATED_POT.getDefaultState());
            } else if (dx == 22 || dx == 36) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ - 9, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
            }
        }
        placeWallArt(world, chunkBox, centerX + 33, baseY + 3, centerZ - 9, Direction.SOUTH, Blocks.BLUE_GLAZED_TERRACOTTA);
        placeWallArt(world, chunkBox, centerX + 33, baseY + 3, centerZ - 5, Direction.NORTH, Blocks.MAGENTA_GLAZED_TERRACOTTA);

        // =====================================================================
        // B. GRAN VESTÍBULO IMPERIAL Y GALERÍA TRASERA HABITADA
        // =====================================================================
        for (int dz = -29; dz <= 24; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                setBlock(world, chunkBox, centerX + dx, baseY + 1, centerZ + dz,
                        Math.abs(dx) == 2 ? yellowCarpet : redCarpet);
            }
        }
        // Medallón ornamental en el centro del Vestíbulo + consolas con vasijas y velas
        placeRug(world, chunkBox, centerX - 4, baseY + 1, centerZ + 8, centerX + 4, centerZ + 14, redCarpet, yellowCarpet);
        for (int side : new int[]{-1, 1}) {
            int cx = side * 11;
            fillBox(world, chunkBox, centerX + cx - 2, baseY + 1, centerZ + 23, centerX + cx + 2, baseY + 1, centerZ + 23, darkOakPlanks);
            setBlock(world, chunkBox, centerX + cx - 2, baseY + 2, centerZ + 23, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
            setBlock(world, chunkBox, centerX + cx - 1, baseY + 2, centerZ + 23, Blocks.DECORATED_POT.getDefaultState());
            setBlock(world, chunkBox, centerX + cx, baseY + 2, centerZ + 23, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
            setBlock(world, chunkBox, centerX + cx + 1, baseY + 2, centerZ + 23, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
            setBlock(world, chunkBox, centerX + cx + 2, baseY + 2, centerZ + 23, lanternStand);
            // Cortinas a los lados de los ventanales del pórtico
            setBlock(world, chunkBox, centerX + side * 7, baseY + 5, centerZ + 24, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
            setBlock(world, chunkBox, centerX + side * 13, baseY + 5, centerZ + 24, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
            // Percheros, espejos de cuerpo entero y bancas de espera
            fillBox(world, chunkBox, centerX + side * 17, baseY + 1, centerZ + 14, centerX + side * 17, baseY + 1, centerZ + 20,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, side < 0 ? Direction.WEST : Direction.EAST));
            setBlock(world, chunkBox, centerX + side * 17, baseY + 1, centerZ + 13, Blocks.POTTED_BAMBOO.getDefaultState());
            setBlock(world, chunkBox, centerX + side * 17, baseY + 1, centerZ + 21, Blocks.DECORATED_POT.getDefaultState());
            fillBox(world, chunkBox, centerX + side * 17, baseY + 2, centerZ + 3, centerX + side * 17, baseY + 5, centerZ + 5,
                    Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
            // Rincón de lectura acogedor bajo cada escalera (dz = -2..-1)
            setBlock(world, chunkBox, centerX + side * 14, baseY + 1, centerZ - 1, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, side < 0 ? Direction.WEST : Direction.EAST));
            setBlock(world, chunkBox, centerX + side * 14, baseY + 2, centerZ - 1, lanternStand);
        }

        // Escalinata Imperial Doble hacia el Segundo Piso
        for (int step = 0; step < 9; step++) {
            int sz = -3 - step;
            int sy = baseY + 1 + step;
            for (int side : new int[]{-1, 1}) {
                int x0 = side < 0 ? -10 : 6;
                int x1 = side < 0 ? -6 : 10;
                fillBox(world, chunkBox, centerX + x0, baseY + 1, centerZ + sz,
                        centerX + x1, sy - 1, centerZ + sz, quartz);
                fillBox(world, chunkBox, centerX + x0, sy, centerZ + sz,
                        centerX + x1, sy, centerZ + sz,
                        Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            }
        }
        fillBox(world, chunkBox, centerX - 10, baseY + 9, centerZ - 14, centerX + 10, baseY + 9, centerZ - 12, quartz);
        for (int dx = -12; dx <= 12; dx++) {
            setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ + 18, Blocks.GLASS_PANE.getDefaultState());
            if (Math.abs(dx) < 6) {
                setBlock(world, chunkBox, centerX + dx, baseY + 10, centerZ, Blocks.GLASS_PANE.getDefaultState());
            }
        }
        for (int dz = 0; dz <= 18; dz++) {
            setBlock(world, chunkBox, centerX - 12, baseY + 10, centerZ + dz, Blocks.GLASS_PANE.getDefaultState());
            setBlock(world, chunkBox, centerX + 12, baseY + 10, centerZ + dz, Blocks.GLASS_PANE.getDefaultState());
        }
        for (int cx : new int[]{-12, 12}) {
            for (int cz : new int[]{0, 18}) {
                setBlock(world, chunkBox, centerX + cx, baseY + 10, centerZ + cz, chiseledQuartz);
                setBlock(world, chunkBox, centerX + cx, baseY + 11, centerZ + cz, lanternStand);
            }
        }
        placeChandelier(world, chunkBox, centerX, baseY + 17, centerZ + 9, 4, goldGlow, seaLantern);

        // Piano de Cola Negro detallado con atril, partituras y banqueta en la Galería Trasera (dx = -12..-6, dz = -25..-18)
        placeRug(world, chunkBox, centerX - 13, baseY + 1, centerZ - 26, centerX - 5, centerZ - 17, redCarpet, yellowCarpet);
        fillBox(world, chunkBox, centerX - 12, baseY + 1, centerZ - 25, centerX - 6, baseY + 1, centerZ - 21, Blocks.BLACK_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 12, baseY + 2, centerZ - 25, centerX - 6, baseY + 2, centerZ - 21, Blocks.BLACK_CARPET.getDefaultState());
        fillBox(world, chunkBox, centerX - 11, baseY + 1, centerZ - 20, centerX - 7, baseY + 1, centerZ - 20, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 12, baseY + 2, centerZ - 21, Blocks.NOTE_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 9, baseY + 2, centerZ - 22, Blocks.LECTERN.getDefaultState());
        fillBox(world, chunkBox, centerX - 10, baseY + 1, centerZ - 18, centerX - 8, baseY + 1, centerZ - 18,
                Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 15, baseY + 1, centerZ - 29, Blocks.POTTED_CHERRY_SAPLING.getDefaultState());

        // Barra de té, librería baja y sala lounge frente a la piscina (dx = +6..+15, dz = -28..-19)
        placeRug(world, chunkBox, centerX + 6, baseY + 1, centerZ - 27, centerX + 15, centerZ - 19, blueCarpet, whiteCarpet);
        fillBox(world, chunkBox, centerX + 7, baseY + 1, centerZ - 20, centerX + 14, baseY + 1, centerZ - 20,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        fillBox(world, chunkBox, centerX + 15, baseY + 1, centerZ - 26, centerX + 15, baseY + 1, centerZ - 21,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        fillBox(world, chunkBox, centerX + 9, baseY + 1, centerZ - 24, centerX + 12, baseY + 1, centerZ - 23, Blocks.GLASS.getDefaultState());
        setBlock(world, chunkBox, centerX + 10, baseY + 2, centerZ - 24, Blocks.POTTED_RED_TULIP.getDefaultState());
        setBlock(world, chunkBox, centerX + 11, baseY + 2, centerZ - 23, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        // Reloj de abuelo + vitrina de vasijas (dx = +16)
        fillBox(world, chunkBox, centerX + 16, baseY + 1, centerZ - 15, centerX + 16, baseY + 3, centerZ - 15, darkOakPlanks);
        setBlock(world, chunkBox, centerX + 16, baseY + 4, centerZ - 15, goldGlow);
        setBlock(world, chunkBox, centerX + 16, baseY + 5, centerZ - 15, darkOakPlanks);
        setBlock(world, chunkBox, centerX + 15, baseY + 1, centerZ - 29, Blocks.DECORATED_POT.getDefaultState());

        // =====================================================================
        // C. HABITACIÓN 1: GRAN SALA DE ESTAR FAMILIAR (dx = -43..-32, dz = -3..+24)
        // =====================================================================
        placeRug(world, chunkBox, centerX - 41, baseY + 1, centerZ + 3, centerX - 33, centerZ + 17, blueCarpet, whiteCarpet);
        // Gran Chimenea con leñera de troncos oscuros, repisa, vasijas y velas
        fillBox(world, chunkBox, centerX - 43, baseY + 1, centerZ + 7, centerX - 42, baseY + 8, centerZ + 13, chiseledQuartz);
        fillBox(world, chunkBox, centerX - 42, baseY + 1, centerZ + 9, centerX - 42, baseY + 2, centerZ + 11, Blocks.CAMPFIRE.getDefaultState());
        fillBox(world, chunkBox, centerX - 41, baseY + 1, centerZ + 9, centerX - 41, baseY + 2, centerZ + 11, Blocks.IRON_BARS.getDefaultState());
        fillBox(world, chunkBox, centerX - 42, baseY + 1, centerZ + 5, centerX - 42, baseY + 2, centerZ + 6, Blocks.DARK_OAK_LOG.getDefaultState());
        setBlock(world, chunkBox, centerX - 41, baseY + 4, centerZ + 8, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX - 41, baseY + 4, centerZ + 10, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX - 41, baseY + 4, centerZ + 12, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        // Sofá en L, 2 sillones individuales y mesas laterales con lámparas
        fillBox(world, chunkBox, centerX - 34, baseY + 1, centerZ + 6, centerX - 34, baseY + 1, centerZ + 14,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        fillBox(world, chunkBox, centerX - 39, baseY + 1, centerZ + 15, centerX - 34, baseY + 1, centerZ + 15,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 39, baseY + 1, centerZ + 4, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 36, baseY + 1, centerZ + 4, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 34, baseY + 1, centerZ + 5, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 34, baseY + 2, centerZ + 5, lanternStand);
        setBlock(world, chunkBox, centerX - 34, baseY + 1, centerZ + 15, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 34, baseY + 2, centerZ + 15, Blocks.POTTED_FERN.getDefaultState());
        // Mesa de centro grande con pastel, juego de té y velas
        fillBox(world, chunkBox, centerX - 39, baseY + 1, centerZ + 8, centerX - 36, baseY + 1, centerZ + 12, Blocks.DARK_OAK_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 38, baseY + 2, centerZ + 9, Blocks.CAKE.getDefaultState());
        setBlock(world, chunkBox, centerX - 37, baseY + 2, centerZ + 10, Blocks.POTTED_FERN.getDefaultState());
        setBlock(world, chunkBox, centerX - 38, baseY + 2, centerZ + 11, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        // Credenza baja + libreros altos + cortinas junto a la ventana frontal
        fillBox(world, chunkBox, centerX - 43, baseY + 1, centerZ + 17, centerX - 43, baseY + 5, centerZ + 22, bookshelf);
        setBlock(world, chunkBox, centerX - 43, baseY + 2, centerZ + 19, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 42, baseY + 1, centerZ + 23, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(world, chunkBox, centerX - 41, baseY + 5, centerZ + 24, Blocks.CYAN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 33, baseY + 5, centerZ + 24, Blocks.CYAN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 33, baseY + 1, centerZ + 22, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, centerX - 33, baseY + 2, centerZ + 22, lanternStand);
        placeChandelier(world, chunkBox, centerX - 37, baseY + 8, centerZ + 10, 2, darkOakPlanks, lanternHang);

        // =====================================================================
        // D. HABITACIÓN 2: CINE EN CASA VIP (dx = -30..-19, dz = -3..+24)
        // =====================================================================
        placeRug(world, chunkBox, centerX - 29, baseY + 1, centerZ + 2, centerX - 20, centerZ + 19, redCarpet, yellowCarpet);
        // Cortinas acústicas rojas flanqueando la gran pantalla de cine + bocinas
        fillBox(world, chunkBox, centerX - 28, baseY + 2, centerZ - 3, centerX - 21, baseY + 6, centerZ - 3, Blocks.BLACK_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 28, baseY + 1, centerZ - 3, centerX - 21, baseY + 1, centerZ - 3, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        setBlock(world, chunkBox, centerX - 29, baseY + 5, centerZ - 2, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 20, baseY + 5, centerZ - 2, Blocks.RED_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        for (int by = 1; by <= 4; by++) {
            setBlock(world, chunkBox, centerX - 29, baseY + by, centerZ - 3, by <= 2 ? Blocks.NOTE_BLOCK.getDefaultState() : Blocks.RED_WOOL.getDefaultState());
            setBlock(world, chunkBox, centerX - 20, baseY + by, centerZ - 3, by <= 2 ? Blocks.NOTE_BLOCK.getDefaultState() : Blocks.RED_WOOL.getDefaultState());
        }
        // 3 Filas escalonadas de butacas VIP con mesitas laterales y velas de pasillo
        fillBox(world, chunkBox, centerX - 28, baseY + 1, centerZ + 4, centerX - 21, baseY + 1, centerZ + 4,
                Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 29, baseY + 1, centerZ + 8, centerX - 20, baseY + 1, centerZ + 11, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX - 28, baseY + 2, centerZ + 9, centerX - 21, baseY + 2, centerZ + 9,
                Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 29, baseY + 1, centerZ + 13, centerX - 20, baseY + 2, centerZ + 16, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX - 28, baseY + 3, centerZ + 14, centerX - 21, baseY + 3, centerZ + 14,
                Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 29, baseY + 2, centerZ + 8, Blocks.RED_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX - 20, baseY + 3, centerZ + 13, Blocks.RED_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        // Dulcería completa al fondo: máquina de palomitas, bebidas, pasteles y barril
        fillBox(world, chunkBox, centerX - 28, baseY + 1, centerZ + 22, centerX - 21, baseY + 1, centerZ + 22, quartz);
        setBlock(world, chunkBox, centerX - 28, baseY + 2, centerZ + 22, Blocks.YELLOW_TERRACOTTA.getDefaultState());
        setBlock(world, chunkBox, centerX - 28, baseY + 3, centerZ + 22, Blocks.GLASS.getDefaultState());
        setBlock(world, chunkBox, centerX - 26, baseY + 2, centerZ + 22, Blocks.CAKE.getDefaultState());
        setBlock(world, chunkBox, centerX - 24, baseY + 2, centerZ + 22, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX - 22, baseY + 1, centerZ + 22, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 25, baseY + 8, centerZ + 9, lanternHang);

        // =====================================================================
        // E. HABITACIÓN 3: ESTUDIO DE MÚSICA Y PODCAST (dx = -43..-32, dz = -30..-11)
        // =====================================================================
        fillBox(world, chunkBox, centerX - 43, baseY + 2, centerZ - 27, centerX - 43, baseY + 6, centerZ - 13, Blocks.GRAY_WOOL.getDefaultState());
        fillBox(world, chunkBox, centerX - 32, baseY + 2, centerZ - 27, centerX - 32, baseY + 6, centerZ - 13, Blocks.BLACK_WOOL.getDefaultState());
        // Mesa de podcast central con 4 micrófonos, atril y sillas
        fillBox(world, chunkBox, centerX - 40, baseY + 1, centerZ - 22, centerX - 35, baseY + 1, centerZ - 17, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 39, baseY + 2, centerZ - 21, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, centerX - 36, baseY + 2, centerZ - 21, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, centerX - 39, baseY + 2, centerZ - 18, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, centerX - 36, baseY + 2, centerZ - 18, Blocks.END_ROD.getDefaultState());
        setBlock(world, chunkBox, centerX - 38, baseY + 2, centerZ - 19, Blocks.LOOM.getDefaultState());
        setBlock(world, chunkBox, centerX - 37, baseY + 2, centerZ - 19, Blocks.DAYLIGHT_DETECTOR.getDefaultState());
        for (int px : new int[]{-39, -36}) {
            setBlock(world, chunkBox, centerX + px, baseY + 1, centerZ - 23, Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
            setBlock(world, chunkBox, centerX + px, baseY + 1, centerZ - 16, Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        }
        // Estación de edición con doble monitor + cabina vocal acristalada + placas decorativas de "discos"
        placeComputerDesk(world, chunkBox, centerX - 35, baseY + 1, centerZ - 29, Direction.SOUTH);
        setBlock(world, chunkBox, centerX - 42, baseY + 1, centerZ - 28, Blocks.NOTE_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 41, baseY + 1, centerZ - 28, Blocks.LECTERN.getDefaultState());
        fillBox(world, chunkBox, centerX - 40, baseY + 1, centerZ - 28, centerX - 40, baseY + 4, centerZ - 27, Blocks.GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX - 32, baseY + 4, centerZ - 20, Blocks.YELLOW_GLAZED_TERRACOTTA.getDefaultState().with(GlazedTerracottaBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 32, baseY + 4, centerZ - 18, Blocks.YELLOW_GLAZED_TERRACOTTA.getDefaultState().with(GlazedTerracottaBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 42, baseY + 1, centerZ - 12, Blocks.POTTED_FERN.getDefaultState());
        setBlock(world, chunkBox, centerX - 37, baseY + 8, centerZ - 20, lanternHang);

        // =====================================================================
        // F. HABITACIÓN 4: DESPACHO PRIVADO Y BIBLIOTECA (dx = -30..-19, dz = -30..-11)
        // =====================================================================
        placeRug(world, chunkBox, centerX - 28, baseY + 1, centerZ - 27, centerX - 21, centerZ - 14, redCarpet, yellowCarpet);
        // Libreros de piso a techo con escalera de madera y estanterías talladas
        fillBox(world, chunkBox, centerX - 19, baseY + 1, centerZ - 28, centerX - 19, baseY + 6, centerZ - 19, bookshelf);
        setBlock(world, chunkBox, centerX - 19, baseY + 2, centerZ - 22, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        fillBox(world, chunkBox, centerX - 20, baseY + 1, centerZ - 24, centerX - 20, baseY + 6, centerZ - 24,
                Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
        // Escritorio ejecutivo con computadora, archiveros y mesa de cartografía
        placeComputerDesk(world, chunkBox, centerX - 26, baseY + 1, centerZ - 27, Direction.SOUTH);
        setBlock(world, chunkBox, centerX - 29, baseY + 1, centerZ - 27, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        fillBox(world, chunkBox, centerX - 29, baseY + 1, centerZ - 25, centerX - 29, baseY + 2, centerZ - 24,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 21, baseY + 1, centerZ - 14, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX - 29, baseY + 1, centerZ - 13, Blocks.DECORATED_POT.getDefaultState());
        placeWallArt(world, chunkBox, centerX - 25, baseY + 3, centerZ - 30, Direction.SOUTH, Blocks.BLUE_GLAZED_TERRACOTTA);
        setBlock(world, chunkBox, centerX - 25, baseY + 8, centerZ - 20, lanternHang);

        // =====================================================================
        // G. HABITACIÓN 5: GRAN COMEDOR FORMAL (dx = +19..+33, dz = -3..+24)
        // =====================================================================
        placeRug(world, chunkBox, centerX + 20, baseY + 1, centerZ + 1, centerX + 32, centerZ + 20, redCarpet, yellowCarpet);
        // Mesa para 12 con platos individuales, velas, flores y carrito de servicio
        fillBox(world, chunkBox, centerX + 24, baseY + 1, centerZ + 3, centerX + 28, baseY + 1, centerZ + 17, darkOakPlanks);
        fillBox(world, chunkBox, centerX + 25, baseY + 2, centerZ + 4, centerX + 27, baseY + 2, centerZ + 16, yellowCarpet);
        for (int czChair = 4; czChair <= 16; czChair += 3) {
            setBlock(world, chunkBox, centerX + 23, baseY + 1, centerZ + czChair,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            setBlock(world, chunkBox, centerX + 29, baseY + 1, centerZ + czChair,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            setBlock(world, chunkBox, centerX + 24, baseY + 2, centerZ + czChair, Blocks.BIRCH_PRESSURE_PLATE.getDefaultState());
            setBlock(world, chunkBox, centerX + 28, baseY + 2, centerZ + czChair, Blocks.BIRCH_PRESSURE_PLATE.getDefaultState());
        }
        setBlock(world, chunkBox, centerX + 26, baseY + 1, centerZ + 2, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 26, baseY + 1, centerZ + 18, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX + 26, baseY + 2, centerZ + 7, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 26, baseY + 2, centerZ + 10, Blocks.POTTED_RED_TULIP.getDefaultState());
        setBlock(world, chunkBox, centerX + 26, baseY + 2, centerZ + 13, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        // Gran vitrina de cristal con vajilla y vasijas + carrito auxiliar
        fillBox(world, chunkBox, centerX + 19, baseY + 1, centerZ + 15, centerX + 19, baseY + 3, centerZ + 22,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.EAST));
        fillBox(world, chunkBox, centerX + 19, baseY + 4, centerZ + 16, centerX + 19, baseY + 5, centerZ + 21, Blocks.GLASS.getDefaultState());
        setBlock(world, chunkBox, centerX + 21, baseY + 1, centerZ + 2, Blocks.DECORATED_POT.getDefaultState());
        placeChandelier(world, chunkBox, centerX + 26, baseY + 8, centerZ + 10, 2, goldGlow, lanternHang);

        // =====================================================================
        // H. HABITACIÓN 6: SALÓN DE CAFÉ Y DESAYUNADOR (dx = +35..+45, dz = -3..+24)
        // =====================================================================
        // 2 Mesas de desayuno junto al ventanal frontal + rincón botánico
        fillBox(world, chunkBox, centerX + 39, baseY + 1, centerZ + 16, centerX + 41, baseY + 1, centerZ + 18, quartz);
        setBlock(world, chunkBox, centerX + 40, baseY + 2, centerZ + 17, Blocks.CAKE.getDefaultState());
        setBlock(world, chunkBox, centerX + 38, baseY + 1, centerZ + 17, Blocks.BIRCH_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX + 42, baseY + 1, centerZ + 17, Blocks.BIRCH_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 40, baseY + 1, centerZ + 15, Blocks.BIRCH_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 40, baseY + 1, centerZ + 19, Blocks.BIRCH_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX + 37, baseY + 1, centerZ + 23, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(world, chunkBox, centerX + 43, baseY + 1, centerZ + 23, Blocks.POTTED_CHERRY_SAPLING.getDefaultState());
        // Barra de café expreso y vitrina pastelera sin soportes de pociones
        fillBox(world, chunkBox, centerX + 44, baseY + 1, centerZ + 2, centerX + 45, baseY + 1, centerZ + 11, darkOakPlanks);
        fillBox(world, chunkBox, centerX + 44, baseY + 2, centerZ + 2, centerX + 44, baseY + 3, centerZ + 4, Blocks.GLASS.getDefaultState());
        setBlock(world, chunkBox, centerX + 44, baseY + 2, centerZ + 6, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 44, baseY + 2, centerZ + 8, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX + 44, baseY + 2, centerZ + 10, lanternStand);
        setBlock(world, chunkBox, centerX + 40, baseY + 8, centerZ + 10, lanternHang);

        // =====================================================================
        // I. HABITACIÓN 7: COCINA GOURMET COMPLETA (dx = +19..+35, dz = -30..-11)
        // =====================================================================
        // Isla central con bancos, tabla de picar y rack colgante de utensilios
        fillBox(world, chunkBox, centerX + 25, baseY + 1, centerZ - 22, centerX + 30, baseY + 1, centerZ - 19, quartz);
        setBlock(world, chunkBox, centerX + 26, baseY + 2, centerZ - 21, Blocks.BIRCH_PRESSURE_PLATE.getDefaultState());
        setBlock(world, chunkBox, centerX + 28, baseY + 2, centerZ - 20, Blocks.CAKE.getDefaultState());
        setBlock(world, chunkBox, centerX + 29, baseY + 2, centerZ - 21, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 26, baseY + 1, centerZ - 18, Blocks.DARK_OAK_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX + 29, baseY + 1, centerZ - 18, Blocks.DARK_OAK_SLAB.getDefaultState());
        // Encimera norte con alacenas inferiores y superiores, estufas, campana de cuarzo, tarja y refrigerador de cuarzo/concreto blanco
        fillBox(world, chunkBox, centerX + 20, baseY + 1, centerZ - 30, centerX + 32, baseY + 1, centerZ - 29, chiseledQuartz);
        fillBox(world, chunkBox, centerX + 20, baseY + 4, centerZ - 30, centerX + 23, baseY + 5, centerZ - 30, darkOakPlanks);
        fillBox(world, chunkBox, centerX + 28, baseY + 4, centerZ - 30, centerX + 31, baseY + 5, centerZ - 30, darkOakPlanks);
        setBlock(world, chunkBox, centerX + 25, baseY + 1, centerZ - 29, Blocks.SMOKER.getDefaultState().with(SmokerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 26, baseY + 1, centerZ - 29, Blocks.SMOKER.getDefaultState().with(SmokerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 27, baseY + 1, centerZ - 29, Blocks.BLAST_FURNACE.getDefaultState().with(BlastFurnaceBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX + 25, baseY + 5, centerZ - 30, centerX + 27, baseY + 6, centerZ - 29, quartz);
        // Fregadero doble sin caldero
        setBlock(world, chunkBox, centerX + 29, baseY + 1, centerZ - 29, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 30, baseY + 1, centerZ - 29, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 29, baseY + 2, centerZ - 30, Blocks.TRIPWIRE_HOOK.getDefaultState());
        // Refrigerador doble blanco decorativo con dispensador de hielo
        fillBox(world, chunkBox, centerX + 33, baseY + 1, centerZ - 30, centerX + 35, baseY + 3, centerZ - 29, Blocks.WHITE_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX + 34, baseY + 2, centerZ - 29, Blocks.PACKED_ICE.getDefaultState());
        setBlock(world, chunkBox, centerX + 21, baseY + 1, centerZ - 29, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        placeChandelier(world, chunkBox, centerX + 27, baseY + 8, centerZ - 20, 2, Blocks.IRON_BARS.getDefaultState(), lanternHang);

        // =====================================================================
        // J. HABITACIONES 8 & 9: LAVANDERÍA + BAÑO DE VISITAS (dx = +37..+45, dz = -30..-11)
        // =====================================================================
        // Lavandería con lavadora, secadora, tabla de planchar, canasto y jabones
        fillBox(world, chunkBox, centerX + 38, baseY + 1, centerZ - 30, centerX + 39, baseY + 2, centerZ - 29, Blocks.WHITE_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX + 38, baseY + 2, centerZ - 28, Blocks.GLASS_PANE.getDefaultState());
        fillBox(world, chunkBox, centerX + 41, baseY + 1, centerZ - 30, centerX + 42, baseY + 2, centerZ - 29, Blocks.WHITE_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX + 41, baseY + 2, centerZ - 28, Blocks.GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX + 44, baseY + 1, centerZ - 29, Blocks.COMPOSTER.getDefaultState());
        setBlock(world, chunkBox, centerX + 38, baseY + 1, centerZ - 24, Blocks.BIRCH_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX + 41, baseY + 3, centerZ - 29, Blocks.CYAN_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, false));
        setBlock(world, chunkBox, centerX + 41, baseY + 8, centerZ - 25, lanternHang);
        // Medio Baño de visitas
        placeBathroomModule(world, chunkBox, centerX + 41, baseY + 1, centerZ - 15, false);
        setBlock(world, chunkBox, centerX + 41, baseY + 8, centerZ - 15, lanternHang);

        // =====================================================================
        // K. ALA ESTE EXTERIOR: BILLAR, ARCADE, FUTBOLITO & BAR VIP (dx = +47..+63)
        // =====================================================================
        for (int dx = 47; dx <= 63; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                if (!(dx >= 53 && dx <= 57 && ry <= 4)) {
                    setBlock(world, chunkBox, centerX + dx, baseY + ry, centerZ - 4, (ry == 1 || ry == 8) ? trimWall : creamWall);
                }
            }
        }
        // Zona Sur (dz = -3..+19): Mesa de Billar, Futbolito, 5 Arcades, Diana de Dardos y Sofá Lounge
        placeRug(world, chunkBox, centerX + 49, baseY + 1, centerZ + 1, centerX + 59, centerZ + 15, redCarpet, yellowCarpet);
        fillBox(world, chunkBox, centerX + 52, baseY + 1, centerZ + 3, centerX + 57, baseY + 1, centerZ + 11, darkOakPlanks);
        fillBox(world, chunkBox, centerX + 53, baseY + 1, centerZ + 4, centerX + 56, baseY + 1, centerZ + 10, seaLantern);
        fillBox(world, chunkBox, centerX + 53, baseY + 2, centerZ + 4, centerX + 56, baseY + 2, centerZ + 10, Blocks.GREEN_CARPET.getDefaultState());
        placeChandelier(world, chunkBox, centerX + 55, baseY + 8, centerZ + 7, 2, Blocks.IRON_BARS.getDefaultState(), lanternHang);
        // Fila de 5 máquinas Arcade coloridas con palancas de control
        Block[] arcadeColors = {Blocks.MAGENTA_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.RED_CONCRETE, Blocks.LIME_CONCRETE, Blocks.YELLOW_CONCRETE};
        int[] arcadeZs = {-1, 2, 5, 11, 14};
        for (int i = 0; i < arcadeZs.length; i++) {
            int az = arcadeZs[i];
            setBlock(world, chunkBox, centerX + 62, baseY + 1, centerZ + az, arcadeColors[i].getDefaultState());
            setBlock(world, chunkBox, centerX + 62, baseY + 2, centerZ + az, seaLantern);
            setBlock(world, chunkBox, centerX + 62, baseY + 3, centerZ + az, arcadeColors[i].getDefaultState());
            setBlock(world, chunkBox, centerX + 61, baseY + 1, centerZ + az, Blocks.DARK_OAK_SLAB.getDefaultState());
        }
        // Diana de dardos en pared sur + mesa lounge
        setBlock(world, chunkBox, centerX + 61, baseY + 3, centerZ + 19, Blocks.TARGET.getDefaultState());
        fillBox(world, chunkBox, centerX + 50, baseY + 1, centerZ + 17, centerX + 58, baseY + 1, centerZ + 17,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        // Zona Norte (dz = -27..-5): Bar VIP en L con contra-barra de botellas y cava
        fillBox(world, chunkBox, centerX + 50, baseY + 1, centerZ - 21, centerX + 59, baseY + 1, centerZ - 19, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        fillBox(world, chunkBox, centerX + 50, baseY + 2, centerZ - 20, centerX + 59, baseY + 2, centerZ - 20, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        for (int bx = 51; bx <= 58; bx += 2) {
            setBlock(world, chunkBox, centerX + bx, baseY + 1, centerZ - 17, Blocks.DARK_OAK_SLAB.getDefaultState());
        }
        setBlock(world, chunkBox, centerX + 52, baseY + 2, centerZ - 20, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 54, baseY + 2, centerZ - 20, Blocks.GREEN_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, false));
        setBlock(world, chunkBox, centerX + 57, baseY + 2, centerZ - 20, lanternStand);
        fillBox(world, chunkBox, centerX + 51, baseY + 1, centerZ - 26, centerX + 58, baseY + 3, centerZ - 26, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 61, baseY + 1, centerZ - 26, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX + 55, baseY + 8, centerZ - 15, lanternHang);

        // =====================================================================
        // L. ALA OESTE EXTERIOR: GARAJE TRIPLE CON 3 AUTOS Y TALLER (dx = -65..-45)
        // =====================================================================
        buildLuxuryCarInGarage(world, chunkBox, centerX - 56, baseY + 1, centerZ - 16, Blocks.RED_CONCRETE.getDefaultState());
        buildLuxuryCarInGarage(world, chunkBox, centerX - 56, baseY + 1, centerZ - 2, Blocks.WHITE_CONCRETE.getDefaultState());
        buildLuxuryCarInGarage(world, chunkBox, centerX - 56, baseY + 1, centerZ + 12, Blocks.YELLOW_CONCRETE.getDefaultState());
        // Taller mecánico sin yunque: mesa de trabajo, herrería, afiladora, cortapiedras y pilas de llantas
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ - 24, Blocks.CRAFTING_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ - 23, Blocks.SMITHING_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ - 22, Blocks.GRINDSTONE.getDefaultState());
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ - 21, Blocks.STONECUTTER.getDefaultState());
        setBlock(world, chunkBox, centerX - 46, baseY + 1, centerZ - 20, Blocks.LOOM.getDefaultState());
        // Pilas de llantas y estantes de cascos
        fillBox(world, chunkBox, centerX - 64, baseY + 1, centerZ - 26, centerX - 64, baseY + 3, centerZ - 25, Blocks.BLACK_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 46, baseY + 1, centerZ + 2, centerX - 46, baseY + 2, centerZ + 6, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.WEST));
        for (int gz : new int[]{-16, -2, 12}) {
            setBlock(world, chunkBox, centerX - 56, baseY + 8, centerZ + gz, seaLantern);
        }

        // =====================================================================
        // M. SEGUNDO PISO COMPLETO (Y = baseY + 10 .. baseY + 17): PASILLOS Y 7 HABITACIONES
        // =====================================================================
        int f2 = baseY + 9; // Altura del suelo del 2.º piso

        // 1. Muros del Mezzanine hacia las Alas Este y Oeste del 2.º piso (dx = -18 y +18, dz = -30..+24)
        for (int dz = -30; dz <= 24; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                boolean hallArch = (dz >= -6 && dz <= -2 && ry <= 4);
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                if (!hallArch) {
                    setBlock(world, chunkBox, centerX - 18, f2 + ry, centerZ + dz, mat);
                    setBlock(world, chunkBox, centerX + 18, f2 + ry, centerZ + dz, mat);
                }
            }
        }
        // Pasillo Superior Oeste (dz = -7 y dz = -1, dx = -43..-19)
        for (int dx = -43; dx <= -19; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                boolean doorNorth = (dx == -38 || dx == -37 || dx == -28 || dx == -27 || dx == -22 || dx == -21) && ry <= 2;
                boolean doorSouth = (dx == -38 || dx == -37 || dx == -25 || dx == -24) && ry <= 2;
                if (!doorNorth) {
                    setBlock(world, chunkBox, centerX + dx, f2 + ry, centerZ - 7, mat);
                }
                if (!doorSouth) {
                    setBlock(world, chunkBox, centerX + dx, f2 + ry, centerZ - 1, mat);
                }
            }
        }
        // Pasillo Superior Este (dz = -7 y dz = -1, dx = +19..+45)
        for (int dx = 19; dx <= 45; dx++) {
            for (int ry = 1; ry <= 8; ry++) {
                BlockState mat = (ry == 1 || ry == 8) ? trimWall : creamWall;
                boolean doorNorth = (dx == 25 || dx == 26 || dx == 39 || dx == 40) && ry <= 2;
                boolean doorSouth = (dx == 26 || dx == 27) && ry <= 2;
                if (!doorNorth) {
                    setBlock(world, chunkBox, centerX + dx, f2 + ry, centerZ - 7, mat);
                }
                if (!doorSouth) {
                    setBlock(world, chunkBox, centerX + dx, f2 + ry, centerZ - 1, mat);
                }
            }
        }
        // Muros divisorios de cuartos del 2.º piso
        for (int dz = 0; dz <= 24; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                setBlock(world, chunkBox, centerX - 31, f2 + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
            }
        }
        for (int dz = -30; dz <= -8; dz++) {
            for (int ry = 1; ry <= 8; ry++) {
                setBlock(world, chunkBox, centerX - 31, f2 + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
                setBlock(world, chunkBox, centerX - 25, f2 + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
                setBlock(world, chunkBox, centerX + 34, f2 + ry, centerZ + dz, (ry == 1 || ry == 8) ? trimWall : creamWall);
            }
        }
        // Puertas de madera del 2.º piso
        placeDoubleDoorPair(world, chunkBox, centerX - 38, f2 + 1, centerZ - 1, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 25, f2 + 1, centerZ - 1, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 38, f2 + 1, centerZ - 7, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 28, f2 + 1, centerZ - 7, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX - 22, f2 + 1, centerZ - 7, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 26, f2 + 1, centerZ - 1, true, Direction.NORTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 25, f2 + 1, centerZ - 7, true, Direction.SOUTH);
        placeDoubleDoorPair(world, chunkBox, centerX + 39, f2 + 1, centerZ - 7, true, Direction.SOUTH);

        // Alfombra e iluminación de ambos pasillos superiores + consolas, arte y plantas
        for (int dx = -43; dx <= -19; dx++) {
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 5, blueCarpet);
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 4, whiteCarpet);
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 3, blueCarpet);
            if (dx % 6 == 0) {
                setBlock(world, chunkBox, centerX + dx, f2 + 8, centerZ - 4, lanternHang);
            }
        }
        for (int cx : new int[]{-42, -33}) {
            setBlock(world, chunkBox, centerX + cx, f2 + 1, centerZ - 6, darkOakPlanks);
            setBlock(world, chunkBox, centerX + cx, f2 + 2, centerZ - 6, Blocks.POTTED_CHERRY_SAPLING.getDefaultState());
            setBlock(world, chunkBox, centerX + cx, f2 + 4, centerZ - 6, Blocks.BLUE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        }
        setBlock(world, chunkBox, centerX - 31, f2 + 1, centerZ - 2, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 31, f2 + 2, centerZ - 2, Blocks.DECORATED_POT.getDefaultState());
        placeWallArt(world, chunkBox, centerX - 34, f2 + 3, centerZ - 7, Direction.SOUTH, Blocks.CYAN_GLAZED_TERRACOTTA);
        placeWallArt(world, chunkBox, centerX - 31, f2 + 3, centerZ - 1, Direction.NORTH, Blocks.BLUE_GLAZED_TERRACOTTA);

        for (int dx = 19; dx <= 45; dx++) {
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 5, redCarpet);
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 4, yellowCarpet);
            setBlock(world, chunkBox, centerX + dx, f2 + 1, centerZ - 3, redCarpet);
            if (dx % 6 == 0) {
                setBlock(world, chunkBox, centerX + dx, f2 + 8, centerZ - 4, lanternHang);
            }
        }
        for (int cx : new int[]{21, 32, 44}) {
            setBlock(world, chunkBox, centerX + cx, f2 + 1, centerZ - 6, darkOakPlanks);
            setBlock(world, chunkBox, centerX + cx, f2 + 2, centerZ - 6, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
            setBlock(world, chunkBox, centerX + cx, f2 + 4, centerZ - 6, Blocks.YELLOW_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        }
        setBlock(world, chunkBox, centerX + 35, f2 + 1, centerZ - 2, darkOakPlanks);
        setBlock(world, chunkBox, centerX + 35, f2 + 2, centerZ - 2, Blocks.DECORATED_POT.getDefaultState());
        placeWallArt(world, chunkBox, centerX + 30, f2 + 3, centerZ - 7, Direction.SOUTH, Blocks.YELLOW_GLAZED_TERRACOTTA);
        placeWallArt(world, chunkBox, centerX + 36, f2 + 3, centerZ - 1, Direction.NORTH, Blocks.MAGENTA_GLAZED_TERRACOTTA);

        // 2. Mezzanine Balcón Sur y Galería Norte de Trofeos (dx = -17..+17)
        fillBox(world, chunkBox, centerX - 5, f2 + 1, centerZ + 22, centerX + 5, f2 + 1, centerZ + 22,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 2, f2 + 1, centerZ + 20, centerX + 2, f2 + 1, centerZ + 20, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 1, f2 + 2, centerZ + 20, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 1, f2 + 2, centerZ + 20, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
        setBlock(world, chunkBox, centerX - 7, f2 + 1, centerZ + 22, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX + 7, f2 + 1, centerZ + 22, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX - 12, f2 + 1, centerZ + 21, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX + 12, f2 + 1, centerZ + 21, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 9, f2 + 1, centerZ + 23, Blocks.DARK_OAK_FENCE.getDefaultState());
        setBlock(world, chunkBox, centerX - 9, f2 + 2, centerZ + 23, Blocks.END_ROD.getDefaultState().with(EndRodBlock.FACING, Direction.SOUTH));
        for (int mx : new int[]{-15, 15}) {
            setBlock(world, chunkBox, centerX + mx, f2 + 1, centerZ + 22, Blocks.COMPOSTER.getDefaultState());
            setBlock(world, chunkBox, centerX + mx, f2 + 2, centerZ + 22, Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState());
            setBlock(world, chunkBox, centerX + mx, f2 + 3, centerZ + 22, Blocks.AZALEA_LEAVES.getDefaultState());
        }
        setBlock(world, chunkBox, centerX - 13, f2 + 1, centerZ + 23, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 13, f2 + 1, centerZ + 23, Blocks.DECORATED_POT.getDefaultState());

        // Galería Norte del 2.º Piso con vista a la piscina (dx = -16..+16, dz = -30..-16)
        placeRug(world, chunkBox, centerX - 10, f2 + 1, centerZ - 28, centerX + 10, centerZ - 18, redCarpet, yellowCarpet);
        fillBox(world, chunkBox, centerX - 15, f2 + 1, centerZ - 29, centerX - 11, f2 + 4, centerZ - 29, bookshelf);
        fillBox(world, chunkBox, centerX + 11, f2 + 1, centerZ - 29, centerX + 15, f2 + 4, centerZ - 29, bookshelf);
        fillBox(world, chunkBox, centerX - 14, f2 + 1, centerZ - 29, centerX - 12, f2 + 3, centerZ - 29,
                Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX + 12, f2 + 1, centerZ - 29, centerX + 14, f2 + 3, centerZ - 29,
                Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 6, f2 + 1, centerZ - 28, goldGlow);
        setBlock(world, chunkBox, centerX - 6, f2 + 2, centerZ - 28, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 6, f2 + 1, centerZ - 28, goldGlow);
        setBlock(world, chunkBox, centerX + 6, f2 + 2, centerZ - 28, Blocks.NOTE_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 2, f2 + 1, centerZ - 29, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX, f2 + 1, centerZ - 29, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 2, f2 + 1, centerZ - 29, Blocks.LOOM.getDefaultState().with(LoomBlock.FACING, Direction.SOUTH));
        // Piano de cola decorativo (Oeste de la galería)
        fillBox(world, chunkBox, centerX - 13, f2 + 1, centerZ - 24, centerX - 11, f2 + 1, centerZ - 22, Blocks.BLACK_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, centerX - 12, f2 + 2, centerZ - 22, Blocks.BIRCH_TRAPDOOR.getDefaultState().with(TrapdoorBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX - 12, f2 + 1, centerZ - 20, Blocks.DARK_OAK_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 12, f2 + 2, centerZ - 20, whiteCarpet);
        // Rincón de café y lectura (Este de la galería)
        setBlock(world, chunkBox, centerX + 13, f2 + 1, centerZ - 24, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX + 11, f2 + 1, centerZ - 23, Blocks.DARK_OAK_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX + 11, f2 + 2, centerZ - 23, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(world, chunkBox, centerX + 13, f2 + 1, centerZ - 21, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 4, f2 + 1, centerZ - 22, centerX + 4, f2 + 1, centerZ - 22,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        placeChandelier(world, chunkBox, centerX, f2 + 8, centerZ - 23, 2, goldGlow, lanternHang);

        // 3. MASTER BEDROOM DE JOSÉ TORRES (dx = +19..+45, dz = 0..+28 con el pabellón frontal)
        placeRug(world, chunkBox, centerX + 23, f2 + 1, centerZ + 5, centerX + 40, centerZ + 17, redCarpet, yellowCarpet);
        // Cabecera monumental decorativa con iluminación cálida y mosaico (sin bloques de minerales)
        fillBox(world, chunkBox, centerX + 45, f2 + 1, centerZ + 7, centerX + 45, f2 + 5, centerZ + 13, warmGoldTrim);
        fillBox(world, chunkBox, centerX + 45, f2 + 2, centerZ + 9, centerX + 45, f2 + 4, centerZ + 11,
                Blocks.YELLOW_GLAZED_TERRACOTTA.getDefaultState().with(GlazedTerracottaBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 45, f2 + 4, centerZ + 8, goldGlow);
        setBlock(world, chunkBox, centerX + 45, f2 + 4, centerZ + 12, goldGlow);
        placeDoubleBed(world, chunkBox, centerX + 42, f2 + 1, centerZ + 9, Direction.EAST, Blocks.RED_BED);
        placeDoubleBed(world, chunkBox, centerX + 42, f2 + 1, centerZ + 11, Direction.EAST, Blocks.RED_BED);
        // Dosel de cama king size con postes y cortinas laterales
        for (int px : new int[]{41, 44}) {
            for (int pz : new int[]{8, 12}) {
                fillBox(world, chunkBox, centerX + px, f2 + 1, centerZ + pz, centerX + px, f2 + 4, centerZ + pz, Blocks.DARK_OAK_FENCE.getDefaultState());
            }
        }
        fillBox(world, chunkBox, centerX + 41, f2 + 5, centerZ + 8, centerX + 44, f2 + 5, centerZ + 12, redCarpet);
        setBlock(world, chunkBox, centerX + 44, f2 + 3, centerZ + 8, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 44, f2 + 3, centerZ + 12, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.NORTH));
        // Mesitas de noche + banco al pie de la cama
        setBlock(world, chunkBox, centerX + 43, f2 + 1, centerZ + 7, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 43, f2 + 2, centerZ + 7, lanternStand);
        setBlock(world, chunkBox, centerX + 43, f2 + 1, centerZ + 13, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 43, f2 + 2, centerZ + 13, Blocks.POTTED_CHERRY_SAPLING.getDefaultState());
        fillBox(world, chunkBox, centerX + 40, f2 + 1, centerZ + 9, centerX + 40, f2 + 1, centerZ + 11, Blocks.DARK_OAK_SLAB.getDefaultState());
        fillBox(world, chunkBox, centerX + 40, f2 + 2, centerZ + 9, centerX + 40, f2 + 2, centerZ + 11, whiteCarpet);
        setBlock(world, chunkBox, centerX + 40, f2 + 1, centerZ + 7, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX + 40, f2 + 1, centerZ + 13, Blocks.DECORATED_POT.getDefaultState());
        // Televisor panorámico, torres de audio y sala de estar frente a la cama
        fillBox(world, chunkBox, centerX + 19, f2 + 2, centerZ + 8, centerX + 19, f2 + 4, centerZ + 12, Blocks.BLACK_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX + 19, f2 + 1, centerZ + 7, centerX + 19, f2 + 3, centerZ + 7, Blocks.NOTE_BLOCK.getDefaultState());
        fillBox(world, chunkBox, centerX + 19, f2 + 1, centerZ + 13, centerX + 19, f2 + 3, centerZ + 13, Blocks.NOTE_BLOCK.getDefaultState());
        fillBox(world, chunkBox, centerX + 20, f2 + 1, centerZ + 7, centerX + 20, f2 + 1, centerZ + 13, darkOakPlanks);
        setBlock(world, chunkBox, centerX + 20, f2 + 2, centerZ + 7, Blocks.POTTED_FERN.getDefaultState());
        setBlock(world, chunkBox, centerX + 20, f2 + 2, centerZ + 10, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 20, f2 + 2, centerZ + 13, Blocks.DECORATED_POT.getDefaultState());
        fillBox(world, chunkBox, centerX + 25, f2 + 1, centerZ + 8, centerX + 25, f2 + 1, centerZ + 12,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        fillBox(world, chunkBox, centerX + 23, f2 + 1, centerZ + 9, centerX + 23, f2 + 1, centerZ + 11, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX + 23, f2 + 2, centerZ + 10, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
        // Escritorio ejecutivo, biblioteca y tocador en la pared norte (evitando puerta en dx=+26..+27)
        fillBox(world, chunkBox, centerX + 20, f2 + 1, centerZ, centerX + 23, f2 + 4, centerZ, bookshelf);
        setBlock(world, chunkBox, centerX + 21, f2 + 2, centerZ, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, centerX + 22, f2 + 2, centerZ, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.SOUTH));
        placeComputerDesk(world, chunkBox, centerX + 35, f2 + 1, centerZ, Direction.SOUTH);
        fillBox(world, chunkBox, centerX + 41, f2 + 1, centerZ, centerX + 44, f2 + 1, centerZ, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX + 42, f2 + 2, centerZ, centerX + 43, f2 + 3, centerZ, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX + 41, f2 + 2, centerZ, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 44, f2 + 2, centerZ, Blocks.POTTED_BAMBOO.getDefaultState());
        placeWallArt(world, chunkBox, centerX + 19, f2 + 3, centerZ + 17, Direction.EAST, Blocks.YELLOW_GLAZED_TERRACOTTA);
        // Mirador frontal sur del Master Bedroom
        fillBox(world, chunkBox, centerX + 27, f2 + 1, centerZ + 22, centerX + 35, f2 + 1, centerZ + 22,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX + 29, f2 + 1, centerZ + 24, centerX + 33, f2 + 1, centerZ + 24, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX + 31, f2 + 2, centerZ + 24, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 26, f2 + 1, centerZ + 24, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX + 36, f2 + 1, centerZ + 24, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 25, f2 + 1, centerZ + 26, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        setBlock(world, chunkBox, centerX + 37, f2 + 1, centerZ + 26, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        placeChandelier(world, chunkBox, centerX + 32, f2 + 8, centerZ + 11, 2, goldGlow, lanternHang);

        // 4. BAÑO SPA PRINCIPAL (dx = +19..+33, dz = -30..-8)
        fillBox(world, chunkBox, centerX + 21, f2 + 1, centerZ - 28, centerX + 27, f2 + 2, centerZ - 22, chiseledQuartz);
        fillBox(world, chunkBox, centerX + 22, f2 + 1, centerZ - 27, centerX + 26, f2 + 1, centerZ - 23, seaLantern);
        fillBox(world, chunkBox, centerX + 22, f2 + 2, centerZ - 27, centerX + 26, f2 + 2, centerZ - 23, Blocks.WATER.getDefaultState());
        setBlock(world, chunkBox, centerX + 21, f2 + 3, centerZ - 28, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 27, f2 + 3, centerZ - 28, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(world, chunkBox, centerX + 21, f2 + 3, centerZ - 22, Blocks.DECORATED_POT.getDefaultState());
        placeBathroomModule(world, chunkBox, centerX + 30, f2 + 1, centerZ - 25, true);
        // Sauna privada de madera de abeto + doble lavabo con espejo iluminado
        fillBox(world, chunkBox, centerX + 20, f2 + 1, centerZ - 14, centerX + 21, f2 + 1, centerZ - 9,
                Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 20, f2 + 1, centerZ - 15, Blocks.SMOOTH_BASALT.getDefaultState());
        setBlock(world, chunkBox, centerX + 20, f2 + 2, centerZ - 15, goldGlow);
        fillBox(world, chunkBox, centerX + 32, f2 + 1, centerZ - 15, centerX + 32, f2 + 1, centerZ - 11, quartz);
        setBlock(world, chunkBox, centerX + 32, f2 + 1, centerZ - 14, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX + 32, f2 + 1, centerZ - 12, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX + 32, f2 + 2, centerZ - 13, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        fillBox(world, chunkBox, centerX + 33, f2 + 2, centerZ - 15, centerX + 33, f2 + 3, centerZ - 11, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX + 33, f2 + 3, centerZ - 17, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 33, f2 + 3, centerZ - 10, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX + 26, f2 + 8, centerZ - 18, lanternHang);

        // 5. VESTIDOR DE LUJO / WALK-IN CLOSET (dx = +35..+45, dz = -30..-8)
        placeRug(world, chunkBox, centerX + 37, f2 + 1, centerZ - 26, centerX + 43, centerZ - 12, whiteCarpet, cyanCarpet);
        fillBox(world, chunkBox, centerX + 35, f2 + 1, centerZ - 28, centerX + 35, f2 + 4, centerZ - 12,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.EAST));
        for (int wz : new int[]{-26, -22, -18, -14}) {
            setBlock(world, chunkBox, centerX + 35, f2 + 2, centerZ + wz, Blocks.BLUE_WOOL.getDefaultState());
            setBlock(world, chunkBox, centerX + 35, f2 + 3, centerZ + wz, Blocks.WHITE_WOOL.getDefaultState());
            setBlock(world, chunkBox, centerX + 36, f2 + 3, centerZ + wz, Blocks.BLACK_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.EAST));
        }
        fillBox(world, chunkBox, centerX + 45, f2 + 1, centerZ - 28, centerX + 45, f2 + 4, centerZ - 12, darkOakPlanks);
        for (int sz = -27; sz <= -13; sz += 2) {
            setBlock(world, chunkBox, centerX + 45, f2 + 1, centerZ + sz, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            setBlock(world, chunkBox, centerX + 45, f2 + 3, centerZ + sz, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        }
        setBlock(world, chunkBox, centerX + 44, f2 + 1, centerZ - 11, Blocks.DECORATED_POT.getDefaultState());
        fillBox(world, chunkBox, centerX + 39, f2 + 1, centerZ - 29, centerX + 41, f2 + 4, centerZ - 29, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX + 38, f2 + 2, centerZ - 29, goldGlow);
        setBlock(world, chunkBox, centerX + 42, f2 + 2, centerZ - 29, goldGlow);
        // Isla central de accesorios, relojes y fragancias
        fillBox(world, chunkBox, centerX + 39, f2 + 1, centerZ - 21, centerX + 41, f2 + 1, centerZ - 18, chiseledQuartz);
        setBlock(world, chunkBox, centerX + 40, f2 + 2, centerZ - 19, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));
        setBlock(world, chunkBox, centerX + 40, f2 + 2, centerZ - 20, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE.getDefaultState());
        setBlock(world, chunkBox, centerX + 39, f2 + 2, centerZ - 18, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
        placeChandelier(world, chunkBox, centerX + 40, f2 + 8, centerZ - 18, 1, goldGlow, lanternHang);

        // 6. RECÁMARA DE INVITADOS 1 (dx = -43..-32, dz = 0..+24)
        placeRug(world, chunkBox, centerX - 41, f2 + 1, centerZ + 6, centerX - 34, centerZ + 17, cyanCarpet, whiteCarpet);
        placeDoubleBed(world, chunkBox, centerX - 40, f2 + 1, centerZ + 11, Direction.WEST, Blocks.CYAN_BED);
        fillBox(world, chunkBox, centerX - 43, f2 + 1, centerZ + 10, centerX - 43, f2 + 4, centerZ + 12, warmGoldTrim);
        setBlock(world, chunkBox, centerX - 43, f2 + 3, centerZ + 8, Blocks.CYAN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 43, f2 + 3, centerZ + 14, Blocks.CYAN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 42, f2 + 1, centerZ + 9, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 42, f2 + 2, centerZ + 9, lanternStand);
        setBlock(world, chunkBox, centerX - 42, f2 + 1, centerZ + 13, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 42, f2 + 2, centerZ + 13, Blocks.POTTED_FERN.getDefaultState());
        fillBox(world, chunkBox, centerX - 37, f2 + 1, centerZ + 10, centerX - 37, f2 + 1, centerZ + 12, Blocks.BIRCH_SLAB.getDefaultState());
        fillBox(world, chunkBox, centerX - 32, f2 + 1, centerZ + 4, centerX - 32, f2 + 4, centerZ + 8,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.WEST));
        fillBox(world, chunkBox, centerX - 32, f2 + 1, centerZ + 14, centerX - 32, f2 + 4, centerZ + 17,
                Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        placeWallArt(world, chunkBox, centerX - 32, f2 + 3, centerZ + 11, Direction.WEST, Blocks.CYAN_GLAZED_TERRACOTTA);
        placeComputerDesk(world, chunkBox, centerX - 36, f2 + 1, centerZ + 22, Direction.NORTH);
        setBlock(world, chunkBox, centerX - 33, f2 + 1, centerZ + 22, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, centerX - 33, f2 + 1, centerZ + 4, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX - 37, f2 + 8, centerZ + 11, lanternHang);

        // 7. RECÁMARA DE INVITADOS 2 (dx = -30..-19, dz = 0..+24)
        placeRug(world, chunkBox, centerX - 28, f2 + 1, centerZ + 6, centerX - 21, centerZ + 17, yellowCarpet, whiteCarpet);
        placeDoubleBed(world, chunkBox, centerX - 22, f2 + 1, centerZ + 11, Direction.EAST, Blocks.WHITE_BED);
        setBlock(world, chunkBox, centerX - 19, f2 + 3, centerZ + 8, Blocks.YELLOW_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 19, f2 + 3, centerZ + 14, Blocks.YELLOW_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 20, f2 + 1, centerZ + 9, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 20, f2 + 2, centerZ + 9, lanternStand);
        setBlock(world, chunkBox, centerX - 20, f2 + 1, centerZ + 13, darkOakPlanks);
        setBlock(world, chunkBox, centerX - 20, f2 + 2, centerZ + 13, Blocks.POTTED_OXEYE_DAISY.getDefaultState());
        fillBox(world, chunkBox, centerX - 30, f2 + 1, centerZ + 4, centerX - 30, f2 + 4, centerZ + 8,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.EAST));
        fillBox(world, chunkBox, centerX - 30, f2 + 2, centerZ + 9, centerX - 30, f2 + 3, centerZ + 11, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        fillBox(world, chunkBox, centerX - 30, f2 + 1, centerZ + 15, centerX - 30, f2 + 4, centerZ + 20, bookshelf);
        setBlock(world, chunkBox, centerX - 30, f2 + 2, centerZ + 17, Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 28, f2 + 1, centerZ + 18, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 28, f2 + 1, centerZ + 16, Blocks.DARK_OAK_SLAB.getDefaultState());
        setBlock(world, chunkBox, centerX - 28, f2 + 2, centerZ + 16, Blocks.WHITE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 2).with(CandleBlock.LIT, true));
        placeComputerDesk(world, chunkBox, centerX - 24, f2 + 1, centerZ + 22, Direction.NORTH);
        placeWallArt(world, chunkBox, centerX - 19, f2 + 3, centerZ + 18, Direction.WEST, Blocks.YELLOW_GLAZED_TERRACOTTA);
        setBlock(world, chunkBox, centerX - 20, f2 + 1, centerZ + 4, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX - 25, f2 + 8, centerZ + 11, lanternHang);

        // 8. GIMNASIO PRIVADO (dx = -43..-32, dz = -30..-8)
        fillBox(world, chunkBox, centerX - 43, f2 + 2, centerZ - 26, centerX - 43, f2 + 5, centerZ - 12, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        fillBox(world, chunkBox, centerX - 43, f2 + 1, centerZ - 26, centerX - 43, f2 + 1, centerZ - 12, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
        // Dos caminadoras (treadmills) detalladas con tablero y barras
        for (int tz : new int[]{-24, -19}) {
            fillBox(world, chunkBox, centerX - 40, f2 + 1, centerZ + tz, centerX - 37, f2 + 1, centerZ + tz + 1, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
            setBlock(world, chunkBox, centerX - 41, f2 + 2, centerZ + tz, Blocks.IRON_BARS.getDefaultState());
            setBlock(world, chunkBox, centerX - 41, f2 + 2, centerZ + tz + 1, Blocks.IRON_BARS.getDefaultState());
            setBlock(world, chunkBox, centerX - 41, f2 + 3, centerZ + tz, Blocks.BLACK_CONCRETE.getDefaultState());
            setBlock(world, chunkBox, centerX - 41, f2 + 3, centerZ + tz + 1, Blocks.BLACK_CONCRETE.getDefaultState());
        }
        // Banco de pesas decorativo, mancuernas, saco de boxeo y barra de hidratación (sin yunques)
        setBlock(world, chunkBox, centerX - 35, f2 + 1, centerZ - 26, Blocks.GRINDSTONE.getDefaultState());
        setBlock(world, chunkBox, centerX - 34, f2 + 1, centerZ - 26, Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState());
        setBlock(world, chunkBox, centerX - 33, f2 + 1, centerZ - 26, Blocks.GRINDSTONE.getDefaultState());
        fillBox(world, chunkBox, centerX - 35, f2 + 1, centerZ - 24, centerX - 33, f2 + 1, centerZ - 24, Blocks.DARK_OAK_SLAB.getDefaultState());
        fillBox(world, chunkBox, centerX - 34, f2 + 6, centerZ - 19, centerX - 34, f2 + 8, centerZ - 19, Blocks.CHAIN.getDefaultState());
        fillBox(world, chunkBox, centerX - 34, f2 + 3, centerZ - 19, centerX - 34, f2 + 5, centerZ - 19, Blocks.RED_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, centerX - 37, f2 + 1, centerZ - 29, centerX - 37, f2 + 6, centerZ - 29,
                Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH));
        fillBox(world, chunkBox, centerX - 32, f2 + 1, centerZ - 15, centerX - 32, f2 + 3, centerZ - 13,
                Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 32, f2 + 1, centerZ - 11, quartz);
        setBlock(world, chunkBox, centerX - 32, f2 + 2, centerZ - 11, Blocks.TRIPWIRE_HOOK.getDefaultState().with(TripwireHookBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 33, f2 + 3, centerZ - 14, Blocks.WHITE_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        placeRug(world, chunkBox, centerX - 38, f2 + 1, centerZ - 14, centerX - 34, centerZ - 12, Blocks.LIME_CARPET.getDefaultState(), cyanCarpet);
        setBlock(world, chunkBox, centerX - 37, f2 + 8, centerZ - 19, lanternHang);

        // 9. BAÑO DE INVITADOS (dx = -30..-26, dz = -30..-8) & SALÓN DE ESTUDIO / BIBLIOTECA (dx = -24..-19, dz = -30..-8)
        placeBathroomModule(world, chunkBox, centerX - 28, f2 + 1, centerZ - 21, true);
        placeRug(world, chunkBox, centerX - 29, f2 + 1, centerZ - 16, centerX - 27, centerZ - 11, whiteCarpet, cyanCarpet);
        setBlock(world, chunkBox, centerX - 26, f2 + 3, centerZ - 14, Blocks.CYAN_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 30, f2 + 2, centerZ - 14, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        setBlock(world, chunkBox, centerX - 30, f2 + 1, centerZ - 11, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(world, chunkBox, centerX - 28, f2 + 8, centerZ - 16, lanternHang);

        fillBox(world, chunkBox, centerX - 19, f2 + 1, centerZ - 28, centerX - 19, f2 + 5, centerZ - 12, bookshelf);
        fillBox(world, chunkBox, centerX - 19, f2 + 2, centerZ - 22, centerX - 19, f2 + 3, centerZ - 16,
                Blocks.CHISELED_BOOKSHELF.getDefaultState().with(HorizontalFacingBlock.FACING, Direction.WEST));
        fillBox(world, chunkBox, centerX - 20, f2 + 1, centerZ - 15, centerX - 20, f2 + 5, centerZ - 15,
                Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST));
        placeComputerDesk(world, chunkBox, centerX - 22, f2 + 1, centerZ - 26, Direction.SOUTH);
        setBlock(world, chunkBox, centerX - 24, f2 + 1, centerZ - 20, Blocks.CARTOGRAPHY_TABLE.getDefaultState());
        setBlock(world, chunkBox, centerX - 24, f2 + 1, centerZ - 17, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.EAST));
        setBlock(world, chunkBox, centerX - 21, f2 + 1, centerZ - 20, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        setBlock(world, chunkBox, centerX - 23, f2 + 1, centerZ - 12, goldGlow);
        setBlock(world, chunkBox, centerX - 23, f2 + 2, centerZ - 12, Blocks.NOTE_BLOCK.getDefaultState());
        setBlock(world, chunkBox, centerX - 21, f2 + 1, centerZ - 11, Blocks.DECORATED_POT.getDefaultState());
        setBlock(world, chunkBox, centerX - 22, f2 + 8, centerZ - 19, lanternHang);
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

    private static void placeBathroomModule(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int floorY,
            int cz,
            boolean withShower
    ) {
        // Lavabo decorativo de cuarzo con grifo, espejo, vela aromática y maceta (sin bloques de minerales)
        setBlock(world, chunkBox, cx, floorY, cz - 2,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, cx, floorY + 1, cz - 2,
                Blocks.TRIPWIRE_HOOK.getDefaultState().with(TripwireHookBlock.FACING, Direction.SOUTH));
        setBlock(world, chunkBox, cx + 1, floorY, cz - 2, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setBlock(world, chunkBox, cx + 1, floorY + 1, cz - 2, Blocks.POTTED_FERN.getDefaultState());
        fillBox(world, chunkBox, cx, floorY + 2, cz - 3, cx + 1, floorY + 3, cz - 3, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        // Inodoro elegante con tapa de madera clara y tapete de baño
        setBlock(world, chunkBox, cx - 1, floorY, cz + 2,
                Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        setBlock(world, chunkBox, cx - 1, floorY + 1, cz + 2, Blocks.BIRCH_TRAPDOOR.getDefaultState());
        setBlock(world, chunkBox, cx, floorY, cz, Blocks.WHITE_CARPET.getDefaultState());
        if (withShower) {
            // Regadera con mampara de cristal alta y llave mezcladora
            fillBox(world, chunkBox, cx + 1, floorY, cz + 1, cx + 2, floorY + 3, cz + 2, Blocks.AIR.getDefaultState());
            fillBox(world, chunkBox, cx, floorY, cz + 1, cx, floorY + 2, cz + 1, Blocks.GLASS_PANE.getDefaultState());
            setBlock(world, chunkBox, cx + 2, floorY + 3, cz + 2,
                    Blocks.TRIPWIRE_HOOK.getDefaultState().with(TripwireHookBlock.FACING, Direction.WEST));
        }
    }

    private static void buildLuxuryCarInGarage(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int cx,
            int by,
            int cz,
            BlockState bodyColor
    ) {
        BlockState wheel = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState cabinGlass = Blocks.BLACK_STAINED_GLASS.getDefaultState();
        fillBox(world, chunkBox, cx - 4, by, cz - 2, cx + 4, by, cz + 2, bodyColor);
        fillBox(world, chunkBox, cx - 2, by + 1, cz - 1, cx + 1, by + 1, cz + 1, cabinGlass);
        fillBox(world, chunkBox, cx - 3, by + 1, cz - 1, cx - 3, by + 1, cz + 1, Blocks.BLACK_CARPET.getDefaultState());
        setBlock(world, chunkBox, cx - 3, by, cz - 2, wheel);
        setBlock(world, chunkBox, cx + 3, by, cz - 2, wheel);
        setBlock(world, chunkBox, cx - 3, by, cz + 2, wheel);
        setBlock(world, chunkBox, cx + 3, by, cz + 2, wheel);
        // Parrilla, faros delanteros, calaveras traseras y alerón deportivo
        setBlock(world, chunkBox, cx - 4, by, cz, Blocks.IRON_BARS.getDefaultState());
        setBlock(world, chunkBox, cx - 4, by, cz - 1, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, cx - 4, by, cz + 1, Blocks.SEA_LANTERN.getDefaultState());
        setBlock(world, chunkBox, cx + 4, by, cz - 1, Blocks.RED_CONCRETE.getDefaultState());
        setBlock(world, chunkBox, cx + 4, by, cz + 1, Blocks.RED_CONCRETE.getDefaultState());
        fillBox(world, chunkBox, cx + 4, by + 1, cz - 2, cx + 4, by + 1, cz + 2, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
    }

    private static void placeWallArt(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int wx,
            int wy,
            int wz,
            Direction facing,
            Block glazedBlock
    ) {
        Direction span = facing.rotateYClockwise();
        int dx = span.getOffsetX();
        int dz = span.getOffsetZ();
        boolean hasFacing = glazedBlock.getDefaultState().contains(GlazedTerracottaBlock.FACING);
        for (int row = 0; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                Direction tileFacing = ((row + col) & 1) == 0 ? facing : facing.rotateYClockwise();
                BlockState state = hasFacing
                        ? glazedBlock.getDefaultState().with(GlazedTerracottaBlock.FACING, tileFacing)
                        : glazedBlock.getDefaultState();
                setBlock(world, chunkBox, wx + col * dx, wy + row, wz + col * dz, state);
            }
        }
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
                Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, facing.getOpposite()));
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

    // =========================================================================
    // 7. CIPRESES ESBELTOS FRONTALES (FOTO 1) Y PINOS GIGANTES TRASEROS (FOTO 2)
    // =========================================================================

    private static void buildEstateTrees(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Cipreses altos y esbeltos junto a la fachada frontal (Foto 1)
        int[][] cypressCoords = {
                {-36, 29}, {20, 29}, {48, 26}, {-68, 28},
                {-20, 95}, {20, 95}, {-20, 130}, {20, 130}
        };
        for (int[] c : cypressCoords) {
            buildSlenderCypressTree(world, chunkBox, centerX + c[0], baseY + 1, centerZ + c[1], 19);
        }

        // Grandes Pinos y Secuoyas alrededor del patio trasero, la piscina y la barda de madera (Foto 2)
        int[][] pineCoords = {
                {-75, -60}, {-82, -95}, {-70, -135}, {-38, -148},
                {38, -148}, {70, -135}, {82, -95}, {75, -60},
                {-128, -120}, {-135, -40}, {-130, 50}, {-125, 120},
                {128, -120}, {135, -40}, {130, 50}, {125, 120},
                {-65, 135}, {65, 135}
        };
        for (int[] p : pineCoords) {
            buildTallPineTree(world, chunkBox, centerX + p[0], baseY + 1, centerZ + p[1], 22);
        }
    }

    private static void buildSlenderCypressTree(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int tx,
            int ty,
            int tz,
            int height
    ) {
        BlockState log = Blocks.SPRUCE_LOG.getDefaultState();
        BlockState leaf = Blocks.SPRUCE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        for (int y = 0; y < height - 2; y++) {
            setBlock(world, chunkBox, tx, ty + y, tz, log);
        }
        for (int y = 2; y <= height; y++) {
            int radius = (y < height - 3) ? 1 : 0;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && y < height - 2) {
                        continue;
                    }
                    if (Math.abs(dx) + Math.abs(dz) <= 1 || (y % 2 == 0 && y < height - 5)) {
                        setBlock(world, chunkBox, tx + dx, ty + y, tz + dz, leaf);
                    }
                }
            }
        }
    }

    private static void buildTallPineTree(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int tx,
            int ty,
            int tz,
            int height
    ) {
        BlockState log = Blocks.SPRUCE_LOG.getDefaultState();
        BlockState leaf = Blocks.SPRUCE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        // Tronco de 2x2 en la base y 1x1 arriba
        for (int y = 0; y < height - 2; y++) {
            setBlock(world, chunkBox, tx, ty + y, tz, log);
            if (y < height / 2) {
                setBlock(world, chunkBox, tx + 1, ty + y, tz, log);
                setBlock(world, chunkBox, tx, ty + y, tz + 1, log);
                setBlock(world, chunkBox, tx + 1, ty + y, tz + 1, log);
            }
        }
        // Copa frondosa escalonada
        for (int y = 5; y <= height; y++) {
            int maxR = Math.max(1, (height - y) / 3 + ((y % 2 == 0) ? 1 : 0));
            if (y >= height - 1) {
                maxR = 0;
            }
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
    // UTILIDADES DE COLOCACIÓN POR CHUNK
    // =========================================================================

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
