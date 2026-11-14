package pruebano1.modid.world;

import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.ModItem;
import pruebano1.modid.block.CasinoChairBlock;
import pruebano1.modid.block.DjComponentBlock;
import pruebano1.modid.block.DrafteaBlock;
import pruebano1.modid.block.DrafteaBlockEntity;

/**
 * Generador monumental del casino único "Draftea Tony Diaz".
 * Estilo Art-Deco nocturno con fachadas escalonadas, marquesina curva, palmeras tropicales,
 * letrero luminoso "CASINO" + luces moradas Draftea, negras, blancas y luces animadas de casino;
 * e interior de dos pisos con carruseles centrales de Drafteas, múltiples islas de tragamonedas,
 * mesas de juego, barra de bar, escenario VIP, candelabros y balcón panorámico.
 */
public final class DrafteaCasinoPiece {

    // Dimensiones del edificio principal (73x55 bloques) y plaza exterior (89x74 + escalinatas)
    private static final int BLDG_MIN_X = -36;
    private static final int BLDG_MAX_X = 36;
    private static final int BLDG_MIN_Z = -31;
    private static final int BLDG_MAX_Z = 23;

    static final int PLAZA_MIN_X = -44;
    static final int PLAZA_MAX_X = 44;
    static final int PLAZA_MIN_Z = -35;
    static final int PLAZA_MAX_Z = 38;

    /** Bloques alrededor de la plaza donde el terreno más alto se rebaja en talud en vez de cortarse en vertical. */
    private static final int GRADE_RANGE = 8;

    private static final int MEZZ_Y = 9;      // Suelo del segundo piso VIP
    private static final int WING_TOP_Y = 17; // Techo de las alas laterales
    private static final int ATRIUM_TOP_Y = 20;

    private static final Item[] SHOWCASE_REEL_ITEMS = {
            ModItem.TONY_COIN,
            Items.DIAMOND,
            Items.GOLDEN_APPLE,
            Items.GOLD_INGOT,
            Items.EMERALD,
            ModItem.SCARLET_INGOT
    };

    private DrafteaCasinoPiece() {
    }

    public static void generateChunkForCasino(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        // 1. Nivelación, cimiento sólido subterráneo, vaciado del recinto y escalinatas perimetrales
        prepareSiteAndFoundation(world, chunkBox, centerX, baseY, centerZ);

        // 2. Plaza exterior pavimentada, fuentes simétricas y palmeras tropicales iluminadas
        buildExteriorPlazaAndPalms(world, chunkBox, centerX, baseY, centerZ);

        // 3. Casco principal negro/blanco/morado, muros decorados, techos abovedados y tragaluz
        buildMainShellAndRoof(world, chunkBox, centerX, baseY, centerZ);

        // 4. Fachada Art-Deco frontal, torre escalonada con aros neón, marquesina curva y letrero CASINO
        buildArtDecoFacadeAndMarquee(world, chunkBox, centerX, baseY, centerZ);

        // 5. Alfombras geométricas y pasillos del salón principal
        buildFloorCarpets(world, chunkBox, centerX, baseY, centerZ);

        // 6. Planta baja: recepción/caja, monumento lobby, 2 carruseles altos, islas Draftea, mesas, bar y escaleras
        buildGroundFloorZones(world, chunkBox, centerX, baseY, centerZ, random);

        // 7. Segundo piso Mezzanine VIP, escenario DJ, máquinas exclusivas y candelabros centrales
        buildMezzanineAndVIP(world, chunkBox, centerX, baseY, centerZ, random);

        // 8. Aplicar tus modificaciones personalizadas guardadas con "/drafteacasino guardar"
        DrafteaCasinoCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, baseY, centerZ);
    }

    // =========================================================================
    // 1. CIMIENTO, NIVELACIÓN Y ESCALINATAS DE ACCESO
    // =========================================================================

    private static void prepareSiteAndFoundation(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        int minX = Math.max(centerX + PLAZA_MIN_X - GRADE_RANGE, chunkBox.getMinX());
        int maxX = Math.min(centerX + PLAZA_MAX_X + GRADE_RANGE, chunkBox.getMaxX());
        int minZ = Math.max(centerZ + PLAZA_MIN_Z - GRADE_RANGE, chunkBox.getMinZ());
        int maxZ = Math.min(centerZ + PLAZA_MAX_Z + GRADE_RANGE, chunkBox.getMaxZ());

        if (minX > maxX || minZ > maxZ) {
            return;
        }

        int bottomY = Math.max(world.getBottomY() + 1, baseY - 14);
        int topClearY = Math.min(world.getTopYInclusive(), baseY + 38);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        BlockState foundation = Blocks.DEEPSLATE_BRICKS.getDefaultState();
        BlockState floorBase = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        for (int wx = minX; wx <= maxX; wx++) {
            int dx = wx - centerX;
            for (int wz = minZ; wz <= maxZ; wz++) {
                int dz = wz - centerZ;
                boolean insidePlaza = dx >= PLAZA_MIN_X && dx <= PLAZA_MAX_X
                        && dz >= PLAZA_MIN_Z && dz <= PLAZA_MAX_Z;

                if (insidePlaza) {
                    // Cimiento firme hasta debajo del suelo natural para que ninguna esquina flote
                    for (int y = bottomY; y < baseY; y++) {
                        pos.set(wx, y, wz);
                        BlockState cur = world.getBlockState(pos);
                        if (y >= baseY - 2 || cur.isAir() || cur.isLiquid() || cur.isReplaceable()) {
                            world.setBlockState(pos, foundation, Block.NOTIFY_LISTENERS);
                        }
                    }

                    // Pavimento base (se decora encima en fases siguientes)
                    pos.set(wx, baseY, wz);
                    world.setBlockState(pos, floorBase, Block.NOTIFY_LISTENERS);

                    // Despejar completamente el volumen aéreo del casino y su plaza
                    for (int y = baseY + 1; y <= topClearY; y++) {
                        pos.set(wx, y, wz);
                        world.setBlockState(pos, air, Block.NOTIFY_LISTENERS);
                    }
                    continue;
                }

                // Anillo exterior (escalinatas de transición suave al terreno)
                int distOut = Math.max(
                        Math.max(PLAZA_MIN_X - dx, dx - PLAZA_MAX_X),
                        Math.max(PLAZA_MIN_Z - dz, dz - PLAZA_MAX_Z)
                );
                if (distOut < 1) {
                    continue;
                }

                // Terreno más alto que la plaza: en vez de un corte vertical se rebaja en talud
                // (2 de alto por bloque de distancia) y se vuelve a cubrir de pasto.
                gradeTerrainAroundPlaza(world, pos, wx, wz, baseY + distOut * 2 - 1);

                if (distOut <= 4) {
                    int stepY = baseY - distOut;
                    // Solo rellena huecos: el terreno natural que ya existe no se toca
                    for (int y = bottomY; y < stepY; y++) {
                        pos.set(wx, y, wz);
                        BlockState cur = world.getBlockState(pos);
                        if (cur.isAir() || cur.isLiquid() || cur.isReplaceable()) {
                            world.setBlockState(pos, foundation, Block.NOTIFY_LISTENERS);
                        }
                    }

                    // Escalinatas frontales amplias y bordes escalonados laterales/traseros
                    boolean isFrontApproach = dz > PLAZA_MAX_Z && Math.abs(dx) <= 44;
                    boolean isSideStair = Math.abs(dx) > PLAZA_MAX_X && Math.abs(dz - 12) <= 10;
                    if (isFrontApproach || isSideStair || distOut <= 2) {
                        BlockState stepState = pickPerimeterStepBlock(dx, dz, distOut);
                        pos.set(wx, stepY, wz);
                        BlockState existingAtStep = world.getBlockState(pos);
                        if (existingAtStep.isAir() || existingAtStep.isLiquid() || existingAtStep.isReplaceable() || distOut == 1) {
                            world.setBlockState(pos, stepState, Block.NOTIFY_LISTENERS);
                        }
                    }
                }
            }
        }
    }

    /**
     * Quita todo lo que sobresalga por encima de capY en la columna y, si se recortó suelo,
     * deja la nueva superficie con pasto/tierra para que no quede piedra o tierra expuesta.
     */
    private static void gradeTerrainAroundPlaza(
            StructureWorldAccess world,
            BlockPos.Mutable pos,
            int wx,
            int wz,
            int capY
    ) {
        BlockState air = Blocks.AIR.getDefaultState();
        boolean removedGround = false;
        for (int y = world.getTopYInclusive(); y > capY; y--) {
            pos.set(wx, y, wz);
            BlockState cur = world.getBlockState(pos);
            if (cur.isAir()) {
                continue;
            }
            if (cur.isIn(BlockTags.DIRT) || cur.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                removedGround = true;
            }
            world.setBlockState(pos, air, Block.NOTIFY_LISTENERS);
        }
        if (!removedGround) {
            return;
        }
        for (int depth = 0; depth < 3; depth++) {
            pos.set(wx, capY - depth, wz);
            BlockState cur = world.getBlockState(pos);
            if (!cur.isIn(BlockTags.DIRT) && !cur.isIn(BlockTags.BASE_STONE_OVERWORLD)) {
                break;
            }
            world.setBlockState(
                    pos,
                    depth == 0 ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.DIRT.getDefaultState(),
                    Block.NOTIFY_LISTENERS
            );
        }
    }

    private static BlockState pickPerimeterStepBlock(int dx, int dz, int distOut) {
        if (dz > PLAZA_MAX_Z) {
            if (Math.abs(dx) <= 4) {
                return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                        .with(StairsBlock.FACING, Direction.NORTH);
            }
            return Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, Direction.NORTH);
        }
        if (dx > PLAZA_MAX_X) {
            return Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, Direction.WEST);
        }
        if (dx < PLAZA_MIN_X) {
            return Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState()
                    .with(StairsBlock.FACING, Direction.EAST);
        }
        return (distOut % 2 == 1) ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.DEEPSLATE_TILES.getDefaultState();
    }

    // =========================================================================
    // 2. PLAZA EXTERIOR, FUENTES Y PALMERAS TROPICALES
    // =========================================================================

    private static void buildExteriorPlazaAndPalms(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState darkTile = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState blackStone = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState purpleWool = Blocks.PURPLE_WOOL.getDefaultState();
        BlockState redWool = Blocks.RED_WOOL.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();

        // Patrón ajedrezado elegante en toda la explanada frontal
        int x0 = Math.max(centerX + PLAZA_MIN_X, chunkBox.getMinX());
        int x1 = Math.min(centerX + PLAZA_MAX_X, chunkBox.getMaxX());
        int z0 = Math.max(centerZ + PLAZA_MIN_Z, chunkBox.getMinZ());
        int z1 = Math.min(centerZ + PLAZA_MAX_Z, chunkBox.getMaxZ());

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int wx = x0; wx <= x1; wx++) {
            int dx = wx - centerX;
            for (int wz = z0; wz <= z1; wz++) {
                int dz = wz - centerZ;
                if (dx >= BLDG_MIN_X && dx <= BLDG_MAX_X && dz >= BLDG_MIN_Z && dz <= BLDG_MAX_Z) {
                    continue; // El interior tiene su propia alfombra
                }

                BlockState top = ((Math.floorMod(dx, 2) ^ Math.floorMod(dz, 2)) == 0) ? blackStone : darkTile;

                // Borde perimetral blanco
                if (dx <= PLAZA_MIN_X + 1 || dx >= PLAZA_MAX_X - 1 || dz <= PLAZA_MIN_Z + 1 || dz >= PLAZA_MAX_Z - 1) {
                    top = quartz;
                } else if (dz >= 24 && Math.abs(dx) <= 4) {
                    // Gran alfombra roja y morada de acceso frontal
                    if (Math.abs(dx) == 4) {
                        top = quartz;
                    } else if (Math.abs(dx) == 3) {
                        top = purpleWool;
                    } else {
                        top = redWool;
                    }
                } else if (dz >= 24 && Math.abs(dx) == 5 && (dz % 2 == 0)) {
                    top = drafteaLight;
                } else if ((Math.abs(dx) == 39 || Math.abs(dx) == 40) && Math.floorMod(dz, 6) == 0) {
                    top = drafteaLight;
                }

                pos.set(wx, baseY, wz);
                world.setBlockState(pos, top, Block.NOTIFY_LISTENERS);
            }
        }

        // Luces de pavimento animadas bordeando la acera frontal
        for (int dx = -40; dx <= 40; dx += 4) {
            if (Math.abs(dx) <= 5) continue;
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 0, 36, casinoLight);
        }

        // Bolardos / Faroles modernos Art-Deco en el bulevar frontal
        for (int dx : new int[]{-34, -24, -14, -6, 6, 14, 24, 34}) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 1, 37, Blocks.POLISHED_BLACKSTONE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 2, 37, Blocks.QUARTZ_PILLAR.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 3, 37, Blocks.QUARTZ_PILLAR.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 4, 37, drafteaLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 5, 37, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
        }

        // Fuentes simétricas con luz morada en la plaza izquierda y derecha
        buildPlazaFountain(world, chunkBox, centerX, baseY, centerZ, -22, 31);
        buildPlazaFountain(world, chunkBox, centerX, baseY, centerZ, 22, 31);

        // 6 Palmeras tropicales gigantes flanqueando las alas izquierda y derecha (igual que la foto 1)
        int[][] palmCoords = {
                {-41, 33, 1},
                {-40, 15, 1},
                {-40, -10, 1},
                {41, 33, -1},
                {40, 15, -1},
                {40, -10, -1}
        };
        for (int[] palm : palmCoords) {
            buildPalmTree(world, chunkBox, centerX, baseY, centerZ, palm[0], palm[1], palm[2]);
        }
    }

    private static void buildPlazaFountain(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int fDx,
            int fDz
    ) {
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState slab = Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState();
        BlockState water = Blocks.WATER.getDefaultState();

        for (int ox = -3; ox <= 3; ox++) {
            for (int oz = -3; oz <= 3; oz++) {
                double dist = Math.hypot(ox, oz);
                if (dist > 3.6) continue;
                if (dist >= 2.7) {
                    setRel(world, chunkBox, centerX, baseY, centerZ, fDx + ox, 1, fDz + oz, quartz);
                    setRel(world, chunkBox, centerX, baseY, centerZ, fDx + ox, 2, fDz + oz, slab);
                } else {
                    setRel(world, chunkBox, centerX, baseY, centerZ, fDx + ox, 0, fDz + oz, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
                    setRel(world, chunkBox, centerX, baseY, centerZ, fDx + ox, 1, fDz + oz, water);
                }
            }
        }
        // Obelisco central luminoso de la fuente
        setRel(world, chunkBox, centerX, baseY, centerZ, fDx, 1, fDz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, fDx, 2, fDz, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, fDx, 3, fDz, Blocks.AMETHYST_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, fDx, 4, fDz, ModBlocks.CASINO_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, fDx, 5, fDz, Blocks.AMETHYST_CLUSTER.getDefaultState());
    }

    private static void buildPalmTree(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int pDx,
            int pDz,
            int leanDir
    ) {
        // Jardinera iluminada morada y blanca
        fillClipped(world, chunkBox, centerX, baseY, centerZ, pDx - 1, 1, pDz - 1, pDx + 1, 1, pDz + 1, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx - 1, 1, pDz - 1, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx + 1, 1, pDz - 1, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx - 1, 1, pDz + 1, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx + 1, 1, pDz + 1, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx, 0, pDz, Blocks.GRASS_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, pDx, 1, pDz, Blocks.JUNGLE_LOG.getDefaultState());

        int height = 11;
        BlockState log = Blocks.JUNGLE_LOG.getDefaultState();
        BlockState leaves = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        int topDx = pDx;
        for (int h = 2; h <= height; h++) {
            int leanOffset = (h >= 7) ? leanDir * 2 : (h >= 4 ? leanDir : 0);
            topDx = pDx + leanOffset;
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx, h, pDz, log);
        }

        int topY = height + 1;
        setRel(world, chunkBox, centerX, baseY, centerZ, topDx, topY, pDz, leaves);
        setRel(world, chunkBox, centerX, baseY, centerZ, topDx, topY + 1, pDz, leaves);
        setRel(world, chunkBox, centerX, baseY, centerZ, topDx, topY - 1, pDz, Blocks.OCHRE_FROGLIGHT.getDefaultState());

        for (int r = 1; r <= 4; r++) {
            int drop = (r >= 3) ? -1 : 0;
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx + r, topY + drop, pDz, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx - r, topY + drop, pDz, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx, topY + drop, pDz + r, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx, topY + drop, pDz - r, leaves);
        }
        for (int d = 1; d <= 3; d++) {
            int drop = (d == 3) ? -1 : 0;
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx + d, topY + drop, pDz + d, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx - d, topY + drop, pDz + d, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx + d, topY + drop, pDz - d, leaves);
            setRel(world, chunkBox, centerX, baseY, centerZ, topDx - d, topY + drop, pDz - d, leaves);
        }
    }

    // =========================================================================
    // 3. ESTRUCTURA PRINCIPAL DEL EDIFICIO, MUROS Y TECHOS
    // =========================================================================

    private static void buildMainShellAndRoof(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState wallBlack = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState wallTile = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState trimWhite = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pillarWhite = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState glass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.getDefaultState();
        BlockState purpleGlassPane = Blocks.PURPLE_STAINED_GLASS_PANE.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();
        BlockState frogWarm = Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();

        // Muros laterales Oeste/Este (dx = -36 y +36) y trasero Norte (dz = -31) hasta relY = 16
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MIN_Z, BLDG_MIN_X, 16, BLDG_MAX_Z, wallBlack);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 1, BLDG_MIN_Z, BLDG_MAX_X, 16, BLDG_MAX_Z, wallBlack);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MIN_Z, BLDG_MAX_X, 16, BLDG_MIN_Z, wallBlack);

        // Zócalos de piedra negra pulida y cenefas horizontales morada y blanca en muros laterales y trasero
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MIN_Z, BLDG_MIN_X, 2, BLDG_MAX_Z, wallTile);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 1, BLDG_MIN_Z, BLDG_MAX_X, 2, BLDG_MAX_Z, wallTile);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MIN_Z, BLDG_MAX_X, 2, BLDG_MIN_Z, wallTile);

        // Franja luminosa morada y blanca recorriendo el perímetro exterior (relY = 8 y relY = 15..16)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 8, BLDG_MIN_Z, BLDG_MIN_X, 8, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 8, BLDG_MIN_Z, BLDG_MAX_X, 8, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 8, BLDG_MIN_Z, BLDG_MAX_X, 8, BLDG_MIN_Z, trimWhite);

        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 15, BLDG_MIN_Z, BLDG_MIN_X, 15, BLDG_MAX_Z, drafteaLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 15, BLDG_MIN_Z, BLDG_MAX_X, 15, BLDG_MAX_Z, drafteaLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 15, BLDG_MIN_Z, BLDG_MAX_X, 15, BLDG_MIN_Z, drafteaLight);

        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 16, BLDG_MIN_Z, BLDG_MIN_X, 16, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 16, BLDG_MIN_Z, BLDG_MAX_X, 16, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 16, BLDG_MIN_Z, BLDG_MAX_X, 16, BLDG_MIN_Z, trimWhite);

        // Ventanales panorámicos en muros laterales Oeste/Este
        for (int wz = -22; wz <= 14; wz += 12) {
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 3, wz - 3, BLDG_MIN_X, 6, wz + 3, glass);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 3, wz - 3, BLDG_MAX_X, 6, wz + 3, glass);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 10, wz - 3, BLDG_MIN_X, 13, wz + 3, purpleGlass);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 10, wz - 3, BLDG_MAX_X, 13, wz + 3, purpleGlass);

            // Pilares luminosos entre ventanas
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, wz + 5, BLDG_MIN_X, 14, wz + 5, pillarWhite);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, 1, wz + 5, BLDG_MAX_X, 14, wz + 5, pillarWhite);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X - 1, 2, wz + 5, BLDG_MIN_X - 1, 14, wz + 5, casinoLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X + 1, 2, wz + 5, BLDG_MAX_X + 1, 14, wz + 5, casinoLight);
        }

        // Ventanales amplios y tiras de neón en la pared trasera Norte (dz = -31)
        for (int wx = -26; wx <= 26; wx += 13) {
            fillClipped(world, chunkBox, centerX, baseY, centerZ, wx - 3, 10, BLDG_MIN_Z, wx + 3, 13, BLDG_MIN_Z, purpleGlass);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, wx - 4, 2, BLDG_MIN_Z - 1, wx - 4, 14, BLDG_MIN_Z - 1, drafteaLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, wx + 4, 2, BLDG_MIN_Z - 1, wx + 4, 14, BLDG_MIN_Z - 1, drafteaLight);
        }

        // Techos de alas laterales (dx = -36..-17 y +17..+36) en relY = 17
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, WING_TOP_Y, BLDG_MIN_Z, -17, WING_TOP_Y, BLDG_MAX_Z, wallTile);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 17, WING_TOP_Y, BLDG_MIN_Z, BLDG_MAX_X, WING_TOP_Y, BLDG_MAX_Z, wallTile);

        // Pretil superior con cenefa de luz en los techos laterales
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, WING_TOP_Y + 1, BLDG_MIN_Z, BLDG_MIN_X, WING_TOP_Y + 1, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MAX_X, WING_TOP_Y + 1, BLDG_MIN_Z, BLDG_MAX_X, WING_TOP_Y + 1, BLDG_MAX_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, WING_TOP_Y + 1, BLDG_MIN_Z, -17, WING_TOP_Y + 1, BLDG_MIN_Z, trimWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 17, WING_TOP_Y + 1, BLDG_MIN_Z, BLDG_MAX_X, WING_TOP_Y + 1, BLDG_MIN_Z, trimWhite);

        // Clerestorio / Muros elevados de la bóveda central (dx = -17 y +17, y = 17..20)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -17, WING_TOP_Y, BLDG_MIN_Z, -17, ATRIUM_TOP_Y, BLDG_MAX_Z, wallBlack);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 17, WING_TOP_Y, BLDG_MIN_Z, 17, ATRIUM_TOP_Y, BLDG_MAX_Z, wallBlack);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -17, ATRIUM_TOP_Y, BLDG_MIN_Z, 17, ATRIUM_TOP_Y, BLDG_MIN_Z, wallBlack);
        // Franjas luminosas en la bóveda central y cristal alto
        for (int wz = BLDG_MIN_Z + 4; wz <= BLDG_MAX_Z - 4; wz += 6) {
            fillClipped(world, chunkBox, centerX, baseY, centerZ, -17, 18, wz, -17, 19, wz + 2, purpleGlassPane);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, 17, 18, wz, 17, 19, wz + 2, purpleGlassPane);
        }

        // Techo artesonado de la bóveda central (relY = 20) con vigas y cúpula de tragaluz neón (relY = 21)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -16, ATRIUM_TOP_Y, BLDG_MIN_Z, 16, ATRIUM_TOP_Y, BLDG_MAX_Z, Blocks.DEEPSLATE_TILES.getDefaultState());

        // Cuadrícula iluminada en el techo del atrio (como los techos con focos empotrados de las fotos 2 y 3)
        for (int wx = -14; wx <= 14; wx += 4) {
            for (int wz = -26; wz <= 18; wz += 4) {
                setRel(world, chunkBox, centerX, baseY, centerZ, wx, ATRIUM_TOP_Y, wz, frogWarm);
            }
        }
        // Tiras moradas en los bordes del techo alto
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -15, ATRIUM_TOP_Y, -28, -15, ATRIUM_TOP_Y, 20, drafteaLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 15, ATRIUM_TOP_Y, -28, 15, ATRIUM_TOP_Y, 20, drafteaLight);

        // Tragaluz central elevado del atrio (dx = -6..+6, dz = -14..+12)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -6, ATRIUM_TOP_Y, -14, 6, ATRIUM_TOP_Y, 12, purpleGlass);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -7, ATRIUM_TOP_Y, -15, 7, ATRIUM_TOP_Y, -15, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -7, ATRIUM_TOP_Y, 13, 7, ATRIUM_TOP_Y, 13, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -7, ATRIUM_TOP_Y, -14, -7, ATRIUM_TOP_Y, 12, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 7, ATRIUM_TOP_Y, -14, 7, ATRIUM_TOP_Y, 12, casinoLight);

        // Iluminación interior en alas laterales bajo mezzanine (y = MEZZ_Y - 1 = 8) y piso 2 (y = 16)
        for (int wx = -33; wx <= 33; wx += 5) {
            for (int wz = -27; wz <= 19; wz += 5) {
                boolean inAtriumOpening = (wx >= -15 && wx <= 15 && wz >= -15 && wz <= 14);
                if (!inAtriumOpening) {
                    setRel(world, chunkBox, centerX, baseY, centerZ, wx, 16, wz, frogWarm);
                    if (wz <= 19) {
                        setRel(world, chunkBox, centerX, baseY, centerZ, wx, MEZZ_Y - 1, wz, frogWarm);
                    }
                }
            }
        }

        // Columnas arquitectónicas blancas y negras con anillo morado en el atrio
        for (int cx : new int[]{-16, 16}) {
            for (int cz : new int[]{-16, -6, 4, 14}) {
                fillClipped(world, chunkBox, centerX, baseY, centerZ, cx, 1, cz, cx, 19, cz, pillarWhite);
                setRel(world, chunkBox, centerX, baseY, centerZ, cx, 1, cz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, cx, 8, cz, drafteaLight);
                setRel(world, chunkBox, centerX, baseY, centerZ, cx, 9, cz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, cx, 16, cz, drafteaLight);
            }
        }
    }

    // =========================================================================
    // 4. FACHADA ART-DECO, MARQUESINA CURVA Y LETRERO "CASINO / DRAFTEA"
    // =========================================================================

    private static void buildArtDecoFacadeAndMarquee(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState blackConcrete = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState blackTile = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState whiteQuartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pillarWhite = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState purpleConcrete = Blocks.PURPLE_CONCRETE.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();
        BlockState warmLight = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState emeraldNeon = Blocks.VERDANT_FROGLIGHT.getDefaultState();
        BlockState tintedGlass = Blocks.TINTED_GLASS.getDefaultState();
        BlockState mullion = Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState();

        // Pared frontal principal en dz = +23
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MAX_Z, BLDG_MAX_X, 16, BLDG_MAX_Z, blackConcrete);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, BLDG_MIN_X, 1, BLDG_MAX_Z, BLDG_MAX_X, 2, BLDG_MAX_Z, blackTile);

        // Alas laterales frontales: grandes ventanales inferiores y superiores enmarcados en neón (foto 1)
        for (int side : new int[]{-1, 1}) {
            int winMinX = side < 0 ? -31 : 21;
            int winMaxX = side < 0 ? -21 : 31;
            int outerRimX = side < 0 ? -35 : 35;

            // Marco neón exterior en L en cada ala
            fillClipped(world, chunkBox, centerX, baseY, centerZ, outerRimX, 8, BLDG_MAX_Z + 1, outerRimX, 15, BLDG_MAX_Z + 1, casinoLight);
            int bandMinX = Math.min(outerRimX, side < 0 ? -16 : 16);
            int bandMaxX = Math.max(outerRimX, side < 0 ? -16 : 16);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, bandMinX, 15, BLDG_MAX_Z + 1, bandMaxX, 15, BLDG_MAX_Z + 1, casinoLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, bandMinX, 16, BLDG_MAX_Z + 1, bandMaxX, 16, BLDG_MAX_Z + 1, drafteaLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, bandMinX, 17, BLDG_MAX_Z, bandMaxX, 17, BLDG_MAX_Z, whiteQuartz);

            // Ventanales inferiores (3 paños iluminados de vitrina)
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX, 2, BLDG_MAX_Z, winMaxX, 5, BLDG_MAX_Z, tintedGlass);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMinX + 3, 2, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMinX + 3, 3, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMinX + 3, 4, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMinX + 3, 5, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMaxX - 3, 2, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMaxX - 3, 3, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMaxX - 3, 4, BLDG_MAX_Z, mullion);
            setRel(world, chunkBox, centerX, baseY, centerZ, winMaxX - 3, 5, BLDG_MAX_Z, mullion);

            // Gran ventanal Art-Deco superior en cuadricula 3x2 con contorno luminoso
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX, 9, BLDG_MAX_Z, winMaxX, 13, BLDG_MAX_Z, tintedGlass);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX, 11, BLDG_MAX_Z, winMaxX, 11, BLDG_MAX_Z, blackConcrete);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX + 3, 9, BLDG_MAX_Z, winMinX + 3, 13, BLDG_MAX_Z, blackConcrete);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMaxX - 3, 9, BLDG_MAX_Z, winMaxX - 3, 13, BLDG_MAX_Z, blackConcrete);
            // Borde luminoso del ventanal superior
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX - 1, 9, BLDG_MAX_Z + 1, winMinX - 1, 14, BLDG_MAX_Z + 1, drafteaLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMaxX + 1, 9, BLDG_MAX_Z + 1, winMaxX + 1, 14, BLDG_MAX_Z + 1, drafteaLight);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, winMinX - 1, 14, BLDG_MAX_Z + 1, winMaxX + 1, 14, BLDG_MAX_Z + 1, casinoLight);
        }

        // Torre escalonada central Art-Deco (dx = -16..+16, alzándose hasta relY = 32)
        int[][] tiers = {
                {-16, 16, 18},
                {-13, 13, 21},
                {-10, 10, 24},
                {-7, 7, 27},
                {-4, 4, 30}
        };
        for (int[] tier : tiers) {
            fillClipped(world, chunkBox, centerX, baseY, centerZ, tier[0], 10, BLDG_MAX_Z - 1, tier[1], tier[2], BLDG_MAX_Z, blackConcrete);
            // Remate blanco en la corona de cada escalón
            fillClipped(world, chunkBox, centerX, baseY, centerZ, tier[0], tier[2], BLDG_MAX_Z, tier[1], tier[2], BLDG_MAX_Z + 1, whiteQuartz);
            fillClipped(world, chunkBox, centerX, baseY, centerZ, tier[0], tier[2] - 1, BLDG_MAX_Z + 1, tier[1], tier[2] - 1, BLDG_MAX_Z + 1, casinoLight);
        }

        // Costillas verticales luminosas Art-Deco subiendo por la fachada central
        int[][] verticalFins = {
                {-15, 17}, {15, 17},
                {-13, 20}, {13, 20},
                {-11, 20}, {11, 20},
                {-9, 23}, {9, 23},
                {-7, 26}, {7, 26},
                {-5, 26}, {5, 26}
        };
        for (int[] fin : verticalFins) {
            int fx = fin[0];
            int fTop = fin[1];
            BlockState finLight = (Math.abs(fx) == 13 || Math.abs(fx) == 9 || Math.abs(fx) == 5) ? drafteaLight : casinoLight;
            fillClipped(world, chunkBox, centerX, baseY, centerZ, fx, 9, BLDG_MAX_Z + 1, fx, fTop, BLDG_MAX_Z + 1, finLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, fx, fTop + 1, BLDG_MAX_Z + 1, whiteQuartz);
        }

        // Rejillas neón verdes verticales icónicas en el centro superior (homenaje directo a la imagen 1)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -4, 17, BLDG_MAX_Z + 1, -3, 22, BLDG_MAX_Z + 1, emeraldNeon);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 3, 17, BLDG_MAX_Z + 1, 4, 22, BLDG_MAX_Z + 1, emeraldNeon);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -1, 18, BLDG_MAX_Z + 1, 1, 22, BLDG_MAX_Z + 1, drafteaLight);

        // Emblema circular neón doble en lo alto de la torre (relY = 26..33)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 26, BLDG_MAX_Z + 1, 2, 30, BLDG_MAX_Z + 1, blackConcrete);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 26, BLDG_MAX_Z + 1, 2, 26, BLDG_MAX_Z + 1, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 30, BLDG_MAX_Z + 1, 2, 30, BLDG_MAX_Z + 1, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 27, BLDG_MAX_Z + 1, -2, 29, BLDG_MAX_Z + 1, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 2, 27, BLDG_MAX_Z + 1, 2, 29, BLDG_MAX_Z + 1, casinoLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -1, 27, BLDG_MAX_Z + 1, 1, 29, BLDG_MAX_Z + 1, drafteaLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 28, BLDG_MAX_Z + 1, Blocks.AMETHYST_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 31, BLDG_MAX_Z + 1, casinoLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 32, BLDG_MAX_Z + 1, whiteQuartz);
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 33, BLDG_MAX_Z + 1, drafteaLight);

        // Panel oscuro del letrero principal encima de la marquesina y letras "CASINO" + corona "DRAFTEA"
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -13, 9, BLDG_MAX_Z + 1, 13, 16, BLDG_MAX_Z + 1, blackConcrete);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -13, 9, BLDG_MAX_Z + 1, 13, 9, BLDG_MAX_Z + 1, drafteaLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -13, 16, BLDG_MAX_Z + 1, 13, 16, BLDG_MAX_Z + 1, drafteaLight);
        buildCasinoLettering(world, chunkBox, centerX, baseY, centerZ);

        // Gran marquesina frontal saliente con proa curva y triple cinta luminosa (relY = 6..8)
        for (int dx = -36; dx <= 36; dx++) {
            int maxMarqueeZ = BLDG_MAX_Z + 3; // Saliente normal en alas: dz = 26
            if (Math.abs(dx) <= 15) {
                double norm = dx / 15.0;
                int curveExtra = (int) Math.round(3.4 * Math.sqrt(Math.max(0.0, 1.0 - norm * norm)));
                maxMarqueeZ = BLDG_MAX_Z + 3 + curveExtra; // Alcanza dz = 29 en el centro
            }
            for (int dz = BLDG_MAX_Z + 1; dz <= maxMarqueeZ; dz++) {
                boolean outerEdge = (dz == maxMarqueeZ) || (Math.abs(dx) == 36);
                // Base de la marquesina (relY = 6): focos cálidos estilo teatro/casino
                BlockState underBulb = outerEdge
                        ? casinoLight
                        : (((Math.floorMod(dx, 2) ^ Math.floorMod(dz, 2)) == 0) ? warmLight : blackConcrete);
                setRel(world, chunkBox, centerX, baseY, centerZ, dx, 6, dz, underBulb);

                // Franja media morada Draftea (relY = 7) y franja superior animada (relY = 8)
                setRel(world, chunkBox, centerX, baseY, centerZ, dx, 7, dz, outerEdge ? drafteaLight : blackTile);
                setRel(world, chunkBox, centerX, baseY, centerZ, dx, 8, dz, outerEdge ? casinoLight : blackConcrete);
                setRel(world, chunkBox, centerX, baseY, centerZ, dx, 9, dz,
                        outerEdge ? Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState() : blackConcrete);
                if (!outerEdge && dz == maxMarqueeZ - 1) {
                    setRel(world, chunkBox, centerX, baseY, centerZ, dx, 9, dz, drafteaLight);
                }
            }
        }

        // Gran entrada acristalada y pórtico abierto triple bajo la marquesina (dx = -11..+11, dz = +23)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -10, 1, BLDG_MAX_Z, 10, 5, BLDG_MAX_Z, purpleConcrete);
        // Vitrinas laterales de acceso
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -9, 2, BLDG_MAX_Z, -6, 4, BLDG_MAX_Z, tintedGlass);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 6, 2, BLDG_MAX_Z, 9, 4, BLDG_MAX_Z, tintedGlass);
        // Tres arcos de entrada amplios sin obstáculos (centro de 5 bloques + dos accesos laterales de 2 bloques)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 1, BLDG_MAX_Z, 2, 4, BLDG_MAX_Z, Blocks.AIR.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -4, 1, BLDG_MAX_Z, -4, 4, BLDG_MAX_Z, Blocks.AIR.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 4, 1, BLDG_MAX_Z, 4, 4, BLDG_MAX_Z, Blocks.AIR.getDefaultState());
        // Columnas doradas/blancas del vestíbulo de entrada
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -5, 1, BLDG_MAX_Z, -5, 5, BLDG_MAX_Z, pillarWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -3, 1, BLDG_MAX_Z, -3, 5, BLDG_MAX_Z, pillarWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 3, 1, BLDG_MAX_Z, 3, 5, BLDG_MAX_Z, pillarWhite);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 5, 1, BLDG_MAX_Z, 5, 5, BLDG_MAX_Z, pillarWhite);
        setRel(world, chunkBox, centerX, baseY, centerZ, -3, 5, BLDG_MAX_Z, Blocks.GOLD_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 3, 5, BLDG_MAX_Z, Blocks.GOLD_BLOCK.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, 5, BLDG_MAX_Z, 2, 5, BLDG_MAX_Z, drafteaLight);

        // Letrero oficial luminoso sobre las puertas
        placeWallSign(world, chunkBox, centerX, baseY, centerZ, 0, 5, BLDG_MAX_Z + 1, Direction.SOUTH,
                DyeColor.WHITE, "§d★ DRAFTEA ★", "§f§lTONY DIAZ", "§aZona Segura", "§e¡Bienvenidos!");
    }

    /**
     * Dibuja en píxeles de luz las letras "CASINO" centradas en el letrero frontal (y = +10..+14, z = +25)
     * más un cintillo neón superior "DRAFTEA TONY DIAZ".
     */
    private static void buildCasinoLettering(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        String[][] letters = {
                { // C
                        "###",
                        "#  ",
                        "#  ",
                        "#  ",
                        "###"
                },
                { // A
                        "###",
                        "# #",
                        "###",
                        "# #",
                        "# #"
                },
                { // S
                        "###",
                        "#  ",
                        "###",
                        "  #",
                        "###"
                },
                { // I
                        "###",
                        " # ",
                        " # ",
                        " # ",
                        "###"
                },
                { // N
                        "# #",
                        "###",
                        "###",
                        "###",
                        "# #"
                },
                { // O
                        "###",
                        "# #",
                        "# #",
                        "# #",
                        "###"
                }
        };

        BlockState letterLight = Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
        BlockState letterGold = Blocks.GOLD_BLOCK.getDefaultState();

        // Ancho total = 6*3 + 5*1 = 23 -> desde dx = -11 hasta +11
        int startX = -11;
        int topY = 14;
        int zSign = BLDG_MAX_Z + 2;

        for (int li = 0; li < letters.length; li++) {
            String[] glyph = letters[li];
            int gx = startX + li * 4;
            for (int row = 0; row < 5; row++) {
                String line = glyph[row];
                int ry = topY - row;
                for (int col = 0; col < 3; col++) {
                    if (line.charAt(col) == '#') {
                        setRel(world, chunkBox, centerX, baseY, centerZ,
                                gx + col, ry, zSign, (row == 0 || row == 4) ? letterLight : letterGold);
                    }
                }
            }
        }

        // Barra neón superior sobre "CASINO" con puntos alternos blancos y morados
        for (int dx = -11; dx <= 11; dx++) {
            BlockState crest = (Math.floorMod(dx, 2) == 0)
                    ? ModBlocks.DRAFTEA_LIGHT.getDefaultState()
                    : Blocks.WHITE_CONCRETE.getDefaultState();
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 15, BLDG_MAX_Z + 2, crest);
        }
    }

    // =========================================================================
    // 5. ALFOMBRAS GEOMÉTRICAS Y SUELOS DECORADOS DEL SALÓN PRINCIPAL
    // =========================================================================

    private static void buildFloorCarpets(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        int x0 = Math.max(centerX + BLDG_MIN_X + 1, chunkBox.getMinX());
        int x1 = Math.min(centerX + BLDG_MAX_X - 1, chunkBox.getMaxX());
        int z0 = Math.max(centerZ + BLDG_MIN_Z + 1, chunkBox.getMinZ());
        int z1 = Math.min(centerZ + BLDG_MAX_Z - 1, chunkBox.getMaxZ());

        if (x0 > x1 || z0 > z1) {
            return;
        }

        BlockState blackTile = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState darkTile = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState purpleWool = Blocks.PURPLE_WOOL.getDefaultState();
        BlockState magentaWool = Blocks.MAGENTA_WOOL.getDefaultState();
        BlockState blackWool = Blocks.BLACK_WOOL.getDefaultState();
        BlockState redWool = Blocks.RED_WOOL.getDefaultState();
        BlockState whiteWool = Blocks.WHITE_WOOL.getDefaultState();
        BlockState greenWool = Blocks.GREEN_WOOL.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int wx = x0; wx <= x1; wx++) {
            int dx = wx - centerX;
            for (int wz = z0; wz <= z1; wz++) {
                int dz = wz - centerZ;
                BlockState floor = ((Math.floorMod(dx, 2) ^ Math.floorMod(dz, 2)) == 0) ? blackTile : darkTile;

                // Pasillo central de acceso en el vestíbulo (dx = -3..+3, dz = +12..+22)
                if (Math.abs(dx) <= 3 && dz >= 12) {
                    if (Math.abs(dx) == 3) floor = quartz;
                    else if (Math.abs(dx) == 2) floor = purpleWool;
                    else floor = redWool;
                }
                // Pasillos transversales limpios con líneas luminosas
                else if (dz == 13 || dz == -15 || Math.abs(dx) == 17) {
                    floor = (Math.floorMod(dx + dz, 4) == 0) ? drafteaLight : quartz;
                }
                // Zona central de los 2 Carruseles (dx = -16..+16, dz = -14..+12)
                else if (Math.abs(dx) <= 16 && dz >= -14 && dz <= 12) {
                    int ring = (Math.abs(dx) + Math.abs(dz - 1)) % 6;
                    if (Math.abs(dx) <= 2 && Math.abs(dz - 1) <= 2) {
                        floor = (Math.abs(dx) == 0 && dz == 1) ? ModBlocks.CASINO_LIGHT.getDefaultState()
                                : (Math.abs(dx) == 2 || Math.abs(dz - 1) == 2 ? gold : purpleWool);
                    } else if (ring == 0) {
                        floor = purpleWool;
                    } else if (ring == 1 || ring == 5) {
                        floor = redWool;
                    } else if (ring == 2 || ring == 4) {
                        floor = blackWool;
                    } else {
                        floor = magentaWool;
                    }
                }
                // Ala Oeste (zona de islas Draftea: alfombra morada, negra y roja con acentos)
                else if (dx <= -18 && dz >= -14 && dz <= 11) {
                    int pat = Math.floorMod(dx * 3 + dz * 2, 5);
                    if (pat == 0) floor = purpleWool;
                    else if (pat == 1) floor = redWool;
                    else if (pat == 2) floor = whiteWool;
                    else floor = blackWool;
                }
                // Ala Este (zona de mesas de juego: alfombra verde esmeralda, morada y negra)
                else if (dx >= 18 && dz >= -14 && dz <= 11) {
                    int pat = Math.floorMod(dx * 2 - dz * 3, 5);
                    if (pat == 0) floor = greenWool;
                    else if (pat == 1) floor = purpleWool;
                    else if (pat == 2) floor = gold;
                    else floor = blackWool;
                }
                // Zona Norte frente al bar y escalinatas (dz <= -16)
                else if (dz <= -16 && Math.abs(dx) <= 14) {
                    floor = ((Math.floorMod(dx, 2) == 0) && (Math.floorMod(dz, 2) == 0)) ? drafteaLight : blackTile;
                }

                pos.set(wx, baseY, wz);
                world.setBlockState(pos, floor, Block.NOTIFY_LISTENERS);
            }
        }
    }

    // =========================================================================
    // 6. PLANTA BAJA: RECEPCIÓN, CARRUSELES, TRAGAMONEDAS, MESAS, BAR Y ESCALERAS
    // =========================================================================

    private static void buildGroundFloorZones(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        // 6A. Monumento central de bienvenida en el vestíbulo (dx = 0, dz = 17)
        buildLobbySculpture(world, chunkBox, centerX, baseY, centerZ);

        // 6B. Taquilla "CAJA / FICHAS" a la izquierda del vestíbulo (dx = -28..-19, dz = 16..20)
        buildCashierBooth(world, chunkBox, centerX, baseY, centerZ);

        // 6C. Sala de espera / Vitrinas de premios a la derecha del vestíbulo (dx = 19..32, dz = 16..21)
        buildLobbyLoungeAndShowcases(world, chunkBox, centerX, baseY, centerZ);

        // 6D. Los 2 GRANDES CARRUSELES CENTRALES DE DRAFTEA (inspirados en las fotos 2 y 3)
        buildCentralSlotCarousel(world, chunkBox, centerX, baseY, centerZ, -9, 1, random);
        buildCentralSlotCarousel(world, chunkBox, centerX, baseY, centerZ, 9, 1, random);

        // 6E. Ala Oeste: 3 bancos dobles de máquinas Draftea con divisiones neón y sillas (24 máquinas)
        buildWestSlotArcade(world, chunkBox, centerX, baseY, centerZ, random);

        // 6F. Ala Este: mesas de Blackjack/Ruleta/Póker + fila de máquinas contra la pared Este
        buildEastGamingWing(world, chunkBox, centerX, baseY, centerZ, random);

        // 6G. Gran Bar de Lujo al Norte y dos escalinatas reales hacia el piso 2 VIP
        buildNorthLuxuryBarAndStairs(world, chunkBox, centerX, baseY, centerZ);
    }

    private static void buildLobbySculpture(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -1, 1, 16, 1, 1, 18, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 1, 17, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 2, 17, Blocks.AMETHYST_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 3, 17, ModBlocks.CASINO_LIGHT.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 4, 17, Blocks.AMETHYST_CLUSTER.getDefaultState());
    }

    private static void buildCashierBooth(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState counterBase = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState counterTop = Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();
        BlockState bars = Blocks.IRON_BARS.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();

        // Mostrador frontal y lateral de la Caja
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -31, 1, 16, -21, 1, 16, counterBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -31, 2, 16, -21, 2, 16, counterTop);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -21, 1, 17, -21, 1, 21, counterBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -21, 2, 17, -21, 2, 21, counterTop);

        // Rejillas de seguridad de caja
        for (int dx = -30; dx <= -22; dx += 2) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 3, 16, bars);
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 4, 16, Blocks.PURPLE_STAINED_GLASS_PANE.getDefaultState());
        }
        // Dintel luminoso sobre la caja
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -32, 5, 16, -20, 5, 16, Blocks.BLACK_CONCRETE.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -30, 5, 16, -22, 5, 16, drafteaLight);

        // Detalles internos: bóveda dorada, cofres, atril y taburetes
        setRel(world, chunkBox, centerX, baseY, centerZ, -29, 1, 20, Blocks.GOLD_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -28, 1, 20, Blocks.GOLD_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -26, 1, 20, Blocks.CHEST.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -24, 1, 20, Blocks.LECTERN.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -33, 1, 20, Blocks.POTTED_AZALEA_BUSH.getDefaultState());

        placeWallSign(world, chunkBox, centerX, baseY, centerZ, -26, 4, 15, Direction.NORTH,
                DyeColor.YELLOW, "§6§lCAJA CENTRAL", "§dFichas Draftea", "§fTony Coin = Premio", "§a¡Mucha Suerte!");
    }

    private static void buildLobbyLoungeAndShowcases(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Vitrina de trofeos detrás de vidrio morado contra la pared derecha del vestíbulo
        BlockState pedestal = Blocks.QUARTZ_PILLAR.getDefaultState();
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.getDefaultState();
        for (int i = 0; i < 3; i++) {
            int tx = 21 + i * 4;
            setRel(world, chunkBox, centerX, baseY, centerZ, tx, 1, 20, pedestal);
            BlockState trophy = switch (i) {
                case 0 -> ModBlocks.SCARLET_BLOCK.getDefaultState();
                case 1 -> Blocks.EMERALD_BLOCK.getDefaultState();
                default -> Blocks.DIAMOND_BLOCK.getDefaultState();
            };
            setRel(world, chunkBox, centerX, baseY, centerZ, tx, 2, 20, trophy);
            setRel(world, chunkBox, centerX, baseY, centerZ, tx, 3, 20, glass);
            setRel(world, chunkBox, centerX, baseY, centerZ, tx, 4, 20, ModBlocks.CASINO_LIGHT.getDefaultState());
        }

        // Sofás de espera elegantes y mesita con florero
        BlockState sofa = Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH);
        for (int dx = 22; dx <= 28; dx++) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 1, 16, sofa);
        }
        setRel(world, chunkBox, centerX, baseY, centerZ, 21, 1, 16, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 29, 1, 16, Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 21, 2, 16, Blocks.POTTED_ALLIUM.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 29, 2, 16, Blocks.LANTERN.getDefaultState());
    }

    /**
     * Construye uno de los dos monumentales carruseles de máquinas Draftea en el centro del salón,
     * inspirado en las torres centrales iluminadas de las imágenes de referencia.
     */
    private static void buildCentralSlotCarousel(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int cDx,
            int cDz,
            Random random
    ) {
        BlockState darkCore = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState quartz = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.getDefaultState();

        // Columna monumental de 3x3 subiendo desde el suelo hasta el techo (relY = 1..18)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, cDx - 1, 1, cDz - 1, cDx + 1, 18, cDz + 1, darkCore);
        for (int y = 1; y <= 17; y++) {
            BlockState rib = (y % 4 == 0) ? casinoLight : ((y % 2 == 0) ? drafteaLight : quartz);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx - 1, y, cDz - 1, rib);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx + 1, y, cDz - 1, rib);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx - 1, y, cDz + 1, rib);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx + 1, y, cDz + 1, rib);
        }

        // Pilares de esquina decorativos de 2 bloques de alto entre las máquinas
        for (int ox : new int[]{-2, 2}) {
            for (int oz : new int[]{-2, 2}) {
                setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 1, cDz + oz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 2, cDz + oz, drafteaLight);
                setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 3, cDz + oz, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            }
        }

        // Corona superior gigante del carrusel ("Wheel / Lightning Storm", relY = 6..11)
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                boolean rim = Math.abs(ox) == 2 || Math.abs(oz) == 2;
                if (rim) {
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 6, cDz + oz, gold);
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 7, cDz + oz,
                            (Math.abs(ox) == 2 && Math.abs(oz) == 2) ? casinoLight : drafteaLight);
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 8, cDz + oz, purpleGlass);
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 9, cDz + oz,
                            (Math.abs(ox + oz) % 2 == 0) ? casinoLight : drafteaLight);
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 10, cDz + oz, quartz);
                }
            }
        }
        // Aro exterior sobresaliente en relY = 8..9
        for (int offset = -1; offset <= 1; offset++) {
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx + offset, 8, cDz - 3, casinoLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx + offset, 8, cDz + 3, casinoLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx - 3, 8, cDz + offset, casinoLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, cDx + 3, 8, cDz + offset, casinoLight);
        }

        // 8 Máquinas Draftea operativas rodeando el carrusel con su silla en frente
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx - 1, 1, cDz - 2, Direction.NORTH, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx + 1, 1, cDz - 2, Direction.NORTH, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx - 1, 1, cDz + 2, Direction.SOUTH, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx + 1, 1, cDz + 2, Direction.SOUTH, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx - 2, 1, cDz - 1, Direction.WEST, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx - 2, 1, cDz + 1, Direction.WEST, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx + 2, 1, cDz - 1, Direction.EAST, random);
        placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, cDx + 2, 1, cDz + 1, Direction.EAST, random);
    }

    private static void buildWestSlotArcade(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        // 3 bancos dobles espalda con espalda en z = 8, z = 1 y z = -6 (4 máquinas Norte + 4 Sur por banco)
        int[] bankCentersZ = {8, 1, -6};
        int[] machineXs = {-30, -28, -24, -22};

        for (int bankZ : bankCentersZ) {
            // Divisor central luminoso de punta a punta del banco
            setRel(world, chunkBox, centerX, baseY, centerZ, -32, 1, bankZ, Blocks.POLISHED_BLACKSTONE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -32, 2, bankZ, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -32, 3, bankZ, ModBlocks.CASINO_LIGHT.getDefaultState());

            setRel(world, chunkBox, centerX, baseY, centerZ, -20, 1, bankZ, Blocks.POLISHED_BLACKSTONE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -20, 2, bankZ, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -20, 3, bankZ, ModBlocks.CASINO_LIGHT.getDefaultState());

            // Pilar medio del banco (dx = -26) y coronamiento neón sobre las 8 máquinas
            fillClipped(world, chunkBox, centerX, baseY, centerZ, -31, 3, bankZ, -21, 3, bankZ, Blocks.BLACK_CONCRETE.getDefaultState());
            fillClipped(world, chunkBox, centerX, baseY, centerZ, -31, 4, bankZ, -21, 4, bankZ, ModBlocks.CASINO_LIGHT.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -26, 1, bankZ, Blocks.QUARTZ_PILLAR.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -26, 2, bankZ, ModBlocks.DRAFTEA_LIGHT.getDefaultState());

            for (int mx : machineXs) {
                // Cara Sur (mirando a +Z) y cara Norte (mirando a -Z)
                setRel(world, chunkBox, centerX, baseY, centerZ, mx, 1, bankZ, Blocks.BLACK_CONCRETE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, mx, 2, bankZ, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
                placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, mx, 1, bankZ + 1, Direction.SOUTH, random);
                placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, mx, 1, bankZ - 1, Direction.NORTH, random);
            }
        }

        // Decoración junto a la pared Oeste (plantas y vitrinas luminosas entre ventanas)
        for (int wz : new int[]{-11, -3, 5, 11}) {
            setRel(world, chunkBox, centerX, baseY, centerZ, -35, 1, wz, Blocks.POLISHED_BLACKSTONE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -35, 2, wz, Blocks.POTTED_AZALEA_BUSH.getDefaultState());
        }
    }

    private static void buildEastGamingWing(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        // Mesa 1: Gran mesa de Blackjack verde y oro en (+24, +6)
        buildCardTable(world, chunkBox, centerX, baseY, centerZ, 24, 6, Blocks.GREEN_CARPET.getDefaultState(), Direction.WEST);

        // Mesa 2: Mesa VIP de Ruleta morada en (+24, -4)
        buildCardTable(world, chunkBox, centerX, baseY, centerZ, 24, -4, Blocks.PURPLE_CARPET.getDefaultState(), Direction.WEST);

        // Sofá circular / descanso en la esquina Noroeste del ala Este (+24, -12)
        for (int dx = 22; dx <= 26; dx++) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 1, -12,
                    Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        }
        setRel(world, chunkBox, centerX, baseY, centerZ, 21, 1, -12, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 21, 2, -12, Blocks.LANTERN.getDefaultState());

        // Fila de 6 máquinas Draftea alineadas contra la pared Este mirando al Oeste (+34)
        int[] wallSlotsZ = {9, 7, 3, 1, -3, -5};
        for (int wz : wallSlotsZ) {
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, 34, 1, wz, Direction.WEST, random);
        }
        // Letrero/marco luminoso sobre la fila de máquinas Este
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 34, 3, -6, 34, 3, 10, Blocks.BLACK_CONCRETE.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 34, 4, -6, 34, 4, 10, ModBlocks.DRAFTEA_LIGHT.getDefaultState());
    }

    private static void buildCardTable(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int tDx,
            int tDz,
            BlockState feltCarpet,
            Direction dealerSide
    ) {
        BlockState tableBase = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState tableTrim = Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);

        // Tablero 3x5 (relY = 1 mesa, relY = 2 tapete de juego y fichas/velas)
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                boolean center = (ox == 0 && Math.abs(oz) <= 1);
                setRel(world, chunkBox, centerX, baseY, centerZ, tDx + ox, 1, tDz + oz, center ? tableBase : tableTrim);
                setRel(world, chunkBox, centerX, baseY, centerZ, tDx + ox, 2, tDz + oz, feltCarpet);
            }
        }
        // Centro de mesa: vela morada iluminada y adorno
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx, 2, tDz,
                Blocks.PURPLE_CANDLE.getDefaultState().with(CandleBlock.CANDLES, 3).with(CandleBlock.LIT, true));

        // Lámpara colgante desde el techo sobre la mesa (bajo el Mezzanine en y = 8)
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx, 7, tDz, Blocks.CHAIN.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx, 6, tDz, ModBlocks.CASINO_LIGHT.getDefaultState());

        // Sillas negras de casino alrededor de la mesa mirando al centro
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx - 2, 1, tDz - 1,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.EAST));
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx - 2, 1, tDz + 1,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.EAST));
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx + 2, 1, tDz - 1,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.WEST));
        setRel(world, chunkBox, centerX, baseY, centerZ, tDx + 2, 1, tDz + 1,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.WEST));
        if (dealerSide != Direction.SOUTH) {
            setRel(world, chunkBox, centerX, baseY, centerZ, tDx, 1, tDz + 3,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.NORTH));
        }
        if (dealerSide != Direction.NORTH) {
            setRel(world, chunkBox, centerX, baseY, centerZ, tDx, 1, tDz - 3,
                    ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.SOUTH));
        }
    }

    private static void buildNorthLuxuryBarAndStairs(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        // Gran Bar VIP en la zona central Norte (dx = -11..+11, dz = -29..-21)
        BlockState barBase = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState barTop = Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();

        // Barra principal en dz = -22, alas laterales en dx = ±11 (-29..-23)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -11, 1, -22, 11, 1, -22, barBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -11, 2, -22, 11, 2, -22, barTop);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -11, 1, -28, -11, 1, -23, barBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -11, 2, -28, -11, 2, -23, barTop);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 11, 1, -28, 11, 1, -23, barBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, 11, 2, -28, 11, 2, -23, barTop);

        // Franja luminosa de fondo bajo la barra
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -10, 1, -23, 10, 1, -23, drafteaLight);

        // 7 Sillas negras de casino frente al bar (dz = -20) mirando hacia el Norte
        BlockState barChair = ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.NORTH);
        for (int dx = -9; dx <= 9; dx += 3) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, 1, -20, barChair);
        }

        // Estantería iluminada trasera del bar (dz = -29..-30)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -10, 1, -29, 10, 1, -29, barBase);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -10, 2, -30, 10, 6, -30, Blocks.BLACK_CONCRETE.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -8, 3, -30, 8, 5, -30, drafteaLight);
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -9, 2, -30, 9, 2, -30, Blocks.SMOOTH_QUARTZ.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -5, 2, -29, Blocks.BREWING_STAND.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 2, -29, Blocks.CAKE.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 5, 2, -29, Blocks.BREWING_STAND.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -2, 2, -29, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        setRel(world, chunkBox, centerX, baseY, centerZ, 2, 2, -29, Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.SOUTH));
        setRel(world, chunkBox, centerX, baseY, centerZ, -8, 6, -29, casinoLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, 6, -29, casinoLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, 8, 6, -29, casinoLight);

        placeWallSign(world, chunkBox, centerX, baseY, centerZ, 0, 4, -29, Direction.SOUTH,
                DyeColor.WHITE, "§d★ LOUNGE BAR ★", "§fDraftea Tony Diaz", "§eBebidas y Botana", "§aZona Segura");

        // Escalinatas gemelas majestuosas hacia el piso 2 VIP (Oeste dx=-24..-20, Este dx=+20..+24)
        for (int side : new int[]{-1, 1}) {
            int sMinX = side < 0 ? -24 : 20;
            int sMaxX = side < 0 ? -20 : 24;
            int outerWallX = sMinX - 1;
            int innerWallX = sMaxX + 1;

            // Sube 9 peldaños desde dz = -15 (relY = 1) hasta dz = -23 (relY = 9 = Mezzanine)
            for (int step = 0; step < 9; step++) {
                int stepY = 1 + step;
                int stepZ = -15 - step;

                BlockState stairState = Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState()
                        .with(StairsBlock.FACING, Direction.NORTH);
                fillClipped(world, chunkBox, centerX, baseY, centerZ, sMinX, stepY, stepZ, sMaxX, stepY, stepZ, stairState);
                // Relleno de soporte limpio bajo cada escalón
                if (stepY > 1) {
                    fillClipped(world, chunkBox, centerX, baseY, centerZ, sMinX, 1, stepZ, sMaxX, stepY - 1, stepZ, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                }

                // Balaustradas laterales elegantes
                setRel(world, chunkBox, centerX, baseY, centerZ, outerWallX, stepY, stepZ, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, outerWallX, stepY + 1, stepZ,
                        (step % 3 == 0) ? drafteaLight : Blocks.QUARTZ_PILLAR.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, innerWallX, stepY, stepZ, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                setRel(world, chunkBox, centerX, baseY, centerZ, innerWallX, stepY + 1, stepZ,
                        (step % 3 == 0) ? drafteaLight : Blocks.QUARTZ_PILLAR.getDefaultState());
            }
        }
    }

    // =========================================================================
    // 7. SEGUNDO PISO MEZZANINE VIP, ESCENARIO DJ Y CANDELABROS
    // =========================================================================

    private static void buildMezzanineAndVIP(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            Random random
    ) {
        BlockState deckBase = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState vipCarpet = Blocks.PURPLE_WOOL.getDefaultState();
        BlockState vipBorder = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState railingGlass = Blocks.PURPLE_STAINED_GLASS_PANE.getDefaultState();
        BlockState railingTop = Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();

        // Colocar el piso del Mezzanine en relY = MEZZ_Y (9), dejando hueco central del atrio y huecos de escaleras
        int x0 = Math.max(centerX + BLDG_MIN_X + 1, chunkBox.getMinX());
        int x1 = Math.min(centerX + BLDG_MAX_X - 1, chunkBox.getMaxX());
        int z0 = Math.max(centerZ + BLDG_MIN_Z + 1, chunkBox.getMinZ());
        int z1 = Math.min(centerZ + BLDG_MAX_Z - 1, chunkBox.getMaxZ());

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int wx = x0; wx <= x1; wx++) {
            int dx = wx - centerX;
            for (int wz = z0; wz <= z1; wz++) {
                int dz = wz - centerZ;

                // Atrio central abierto para ver hacia abajo los carruseles y desde abajo el techo/candelabros
                boolean isAtriumVoid = (dx >= -15 && dx <= 15 && dz >= -15 && dz <= 14);
                // Hueco libre sobre las dos escalinatas (dz = -22..-14, |dx| = 20..24)
                boolean isWestStairWell = (dx >= -25 && dx <= -19 && dz >= -22 && dz <= -14);
                boolean isEastStairWell = (dx >= 19 && dx <= 25 && dz >= -22 && dz <= -14);

                if (isAtriumVoid || isWestStairWell || isEastStairWell) {
                    continue;
                }

                // Piso estructural negro debajo + alfombra elegante arriba
                BlockState deck = ((Math.abs(dx) == 16 || dz == -16 || dz == 15) && Math.abs(dx) <= 17 && dz >= -17 && dz <= 16)
                        ? vipBorder
                        : ((Math.floorMod(dx + dz, 4) == 0) ? deckBase : vipCarpet);

                pos.set(wx, baseY + MEZZ_Y, wz);
                world.setBlockState(pos, deck, Block.NOTIFY_LISTENERS);
            }
        }

        // Barandales de cristal morado y cuarzo bordeando todo el atrio abierto (relY = 10)
        for (int dx = -16; dx <= 16; dx++) {
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, MEZZ_Y + 1, -16,
                    (Math.abs(dx) % 4 == 0) ? drafteaLight : railingGlass);
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, MEZZ_Y + 2, -16, railingTop);

            setRel(world, chunkBox, centerX, baseY, centerZ, dx, MEZZ_Y + 1, 15,
                    (Math.abs(dx) % 4 == 0) ? drafteaLight : railingGlass);
            setRel(world, chunkBox, centerX, baseY, centerZ, dx, MEZZ_Y + 2, 15, railingTop);
        }
        for (int dz = -15; dz <= 14; dz++) {
            setRel(world, chunkBox, centerX, baseY, centerZ, -16, MEZZ_Y + 1, dz,
                    (Math.floorMod(dz, 5) == 0) ? drafteaLight : railingGlass);
            setRel(world, chunkBox, centerX, baseY, centerZ, -16, MEZZ_Y + 2, dz, railingTop);

            setRel(world, chunkBox, centerX, baseY, centerZ, 16, MEZZ_Y + 1, dz,
                    (Math.floorMod(dz, 5) == 0) ? drafteaLight : railingGlass);
            setRel(world, chunkBox, centerX, baseY, centerZ, 16, MEZZ_Y + 2, dz, railingTop);
        }

        // Barandal de seguridad alrededor de los dos huecos de escalera (en el piso 2)
        for (int side : new int[]{-1, 1}) {
            int sMinX = side < 0 ? -25 : 19;
            int sMaxX = side < 0 ? -19 : 25;
            for (int dz = -22; dz <= -14; dz++) {
                setRel(world, chunkBox, centerX, baseY, centerZ, sMinX, MEZZ_Y + 1, dz, railingGlass);
                setRel(world, chunkBox, centerX, baseY, centerZ, sMaxX, MEZZ_Y + 1, dz, railingGlass);
            }
            for (int dx = sMinX; dx <= sMaxX; dx++) {
                setRel(world, chunkBox, centerX, baseY, centerZ, dx, MEZZ_Y + 1, -14, railingGlass);
            }
        }

        // 7A. Escenario / Cabina DJ VIP en la galería Norte (dx = -10..+10, dz = -29..-21, relY = 10..15)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -10, MEZZ_Y + 1, -29, 10, MEZZ_Y + 1, -22, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -9, MEZZ_Y + 1, -28, 9, MEZZ_Y + 1, -23, Blocks.PURPLE_WOOL.getDefaultState());
        // Consola/mesa de DJ (dz = -24, altura +2) y los 3 nuevos controladores DJ encima (altura +3)
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -2, MEZZ_Y + 2, -24, 2, MEZZ_Y + 2, -24, Blocks.POLISHED_BLACKSTONE.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -2, MEZZ_Y + 2, -24, drafteaLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, 2, MEZZ_Y + 2, -24, drafteaLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, -3, MEZZ_Y + 2, -24, Blocks.NOTE_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -3, MEZZ_Y + 3, -24, Blocks.NOTE_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 3, MEZZ_Y + 2, -24, Blocks.NOTE_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 3, MEZZ_Y + 3, -24, Blocks.NOTE_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -2, MEZZ_Y + 3, -24, Blocks.JUKEBOX.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, -1, MEZZ_Y + 3, -24,
                ModBlocks.DJ_TURNTABLE.getDefaultState().with(DjComponentBlock.FACING, Direction.NORTH));
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, MEZZ_Y + 3, -24,
                ModBlocks.DJ_CONTROLLER.getDefaultState().with(DjComponentBlock.FACING, Direction.NORTH));
        setRel(world, chunkBox, centerX, baseY, centerZ, 1, MEZZ_Y + 3, -24,
                ModBlocks.DJ_MIXER.getDefaultState().with(DjComponentBlock.FACING, Direction.NORTH));
        setRel(world, chunkBox, centerX, baseY, centerZ, 2, MEZZ_Y + 3, -24, Blocks.JUKEBOX.getDefaultState());
        setRel(world, chunkBox, centerX, baseY, centerZ, 0, MEZZ_Y + 2, -26,
                ModBlocks.CASINO_CHAIR.getDefaultState().with(CasinoChairBlock.FACING, Direction.SOUTH));
        // Columnas ecualizadoras de sonido e iluminación a ambos lados de la cabina DJ
        for (int eqX : new int[]{-8, -6, 6, 8}) {
            fillClipped(world, chunkBox, centerX, baseY, centerZ, eqX, MEZZ_Y + 2, -29, eqX, MEZZ_Y + 6, -29, Blocks.BLACK_CONCRETE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, eqX, MEZZ_Y + 3, -29, casinoLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, eqX, MEZZ_Y + 5, -29, drafteaLight);
        }
        fillClipped(world, chunkBox, centerX, baseY, centerZ, -4, MEZZ_Y + 3, -30, 4, MEZZ_Y + 5, -30, casinoLight);

        // 7B. Salón VIP de Drafteas Oeste (piso 2: dx = -34..-20, dz = -10..+12)
        int vipFloorY = MEZZ_Y + 1;
        for (int rowZ : new int[]{-6, 4}) {
            setRel(world, chunkBox, centerX, baseY, centerZ, -30, vipFloorY, rowZ, Blocks.GOLD_BLOCK.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, -27, vipFloorY, rowZ, drafteaLight);
            setRel(world, chunkBox, centerX, baseY, centerZ, -24, vipFloorY, rowZ, Blocks.GOLD_BLOCK.getDefaultState());
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, -29, vipFloorY, rowZ, Direction.SOUTH, random);
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, -25, vipFloorY, rowZ, Direction.SOUTH, random);
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, -29, vipFloorY, rowZ - 1, Direction.NORTH, random);
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, -25, vipFloorY, rowZ - 1, Direction.NORTH, random);
        }

        // 7C. Salón VIP High-Roller Este (piso 2: dx = 20..34, dz = -10..+12)
        buildCardTable(world, chunkBox, centerX, baseY, centerZ, 24, 5, Blocks.RED_CARPET.getDefaultState(), Direction.WEST);
        buildCardTable(world, chunkBox, centerX, baseY, centerZ, 24, -6, Blocks.PURPLE_CARPET.getDefaultState(), Direction.WEST);
        for (int wz : new int[]{8, 6, 0, -2, -6, -8}) {
            placeDrafteaWithStool(world, chunkBox, centerX, baseY, centerZ, 34, vipFloorY, wz, Direction.WEST, random);
        }

        // 7D. Puente mirador Sur con mesitas lounge mirando hacia los ventanales de la fachada (dz = 18..21)
        for (int lx : new int[]{-10, 0, 10}) {
            setRel(world, chunkBox, centerX, baseY, centerZ, lx, vipFloorY, 19, Blocks.DARK_OAK_FENCE.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, lx, vipFloorY + 1, 19, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, lx, vipFloorY + 2, 19, Blocks.LANTERN.getDefaultState());
            setRel(world, chunkBox, centerX, baseY, centerZ, lx - 2, vipFloorY, 19,
                    Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            setRel(world, chunkBox, centerX, baseY, centerZ, lx + 2, vipFloorY, 19,
                    Blocks.CRIMSON_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
        }

        // 7E. Gran Candelabro geométrico neón suspendido sobre el centro del atrio (dx = 0, dz = 1, relY = 14..19)
        buildAtriumChandelier(world, chunkBox, centerX, baseY, centerZ, 0, 1);
    }

    private static void buildAtriumChandelier(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int cDx,
            int cDz
    ) {
        BlockState chain = Blocks.CHAIN.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState casinoLight = ModBlocks.CASINO_LIGHT.getDefaultState();
        BlockState drafteaLight = ModBlocks.DRAFTEA_LIGHT.getDefaultState();
        BlockState glass = Blocks.PURPLE_STAINED_GLASS.getDefaultState();

        // Tirantes desde el techo (relY = 19) hasta la corona superior (relY = 17)
        for (int ox : new int[]{-2, 2}) {
            for (int oz : new int[]{-2, 2}) {
                setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 19, cDz + oz, chain);
                setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 18, cDz + oz, chain);
            }
        }
        setRel(world, chunkBox, centerX, baseY, centerZ, cDx, 19, cDz, chain);
        setRel(world, chunkBox, centerX, baseY, centerZ, cDx, 18, cDz, chain);

        // Anillo principal luminoso (5x5 en relY = 17, 3x3 en relY = 15..16, punta en relY = 14)
        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                if (Math.abs(ox) == 2 || Math.abs(oz) == 2) {
                    boolean corner = (Math.abs(ox) == 2 && Math.abs(oz) == 2);
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 17, cDz + oz, corner ? gold : drafteaLight);
                    if (!corner && (ox == 0 || oz == 0)) {
                        setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 16, cDz + oz, casinoLight);
                    }
                } else {
                    setRel(world, chunkBox, centerX, baseY, centerZ, cDx + ox, 16, cDz + oz, glass);
                }
            }
        }
        setRel(world, chunkBox, centerX, baseY, centerZ, cDx, 17, cDz, gold);
        setRel(world, chunkBox, centerX, baseY, centerZ, cDx, 15, cDz, casinoLight);
        setRel(world, chunkBox, centerX, baseY, centerZ, cDx, 14, cDz, Blocks.AMETHYST_CLUSTER.getDefaultState());
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Coloca una máquina tragamonedas Draftea completa (mitad inferior + superior + símbolos iniciales
     * en los 3 rodillos) junto con un taburete frente a ella mirando hacia la máquina.
     */
    private static void placeDrafteaWithStool(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int dx,
            int relY,
            int dz,
            Direction facing,
            Random random
    ) {
        BlockState lower = ModBlocks.DRAFTEA.getDefaultState()
                .with(DrafteaBlock.FACING, facing)
                .with(DrafteaBlock.HALF, DoubleBlockHalf.LOWER)
                .with(DrafteaBlock.SPIN, 0);
        BlockState upper = ModBlocks.DRAFTEA.getDefaultState()
                .with(DrafteaBlock.FACING, facing)
                .with(DrafteaBlock.HALF, DoubleBlockHalf.UPPER)
                .with(DrafteaBlock.SPIN, 0);

        setRel(world, chunkBox, centerX, baseY, centerZ, dx, relY, dz, lower);
        setRel(world, chunkBox, centerX, baseY, centerZ, dx, relY + 1, dz, upper);

        BlockPos upperPos = new BlockPos(centerX + dx, baseY + relY + 1, centerZ + dz);
        if (chunkBox.contains(upperPos) && world.getBlockEntity(upperPos) instanceof DrafteaBlockEntity screen) {
            int offset = random.nextInt(SHOWCASE_REEL_ITEMS.length);
            screen.setReel(0, SHOWCASE_REEL_ITEMS[offset]);
            screen.setReel(1, SHOWCASE_REEL_ITEMS[(offset + 1) % SHOWCASE_REEL_ITEMS.length]);
            screen.setReel(2, SHOWCASE_REEL_ITEMS[(offset + 2) % SHOWCASE_REEL_ITEMS.length]);
            screen.markDirty();
        }

        // Silla negra de casino directamente frente a la máquina mirando hacia ella
        int stoolDx = dx + facing.getOffsetX();
        int stoolDz = dz + facing.getOffsetZ();
        BlockState chair = ModBlocks.CASINO_CHAIR.getDefaultState()
                .with(CasinoChairBlock.FACING, facing.getOpposite());
        setRel(world, chunkBox, centerX, baseY, centerZ, stoolDx, relY, stoolDz, chair);
    }

    private static void placeWallSign(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int dx,
            int relY,
            int dz,
            Direction facing,
            DyeColor dyeColor,
            String line0,
            String line1,
            String line2,
            String line3
    ) {
        BlockPos pos = new BlockPos(centerX + dx, baseY + relY, centerZ + dz);
        if (!chunkBox.contains(pos)) {
            return;
        }
        world.setBlockState(
                pos,
                Blocks.CRIMSON_WALL_SIGN.getDefaultState().with(WallSignBlock.FACING, facing),
                Block.NOTIFY_LISTENERS
        );
        if (world.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            Text[] messages = {
                    Text.literal(line0),
                    Text.literal(line1),
                    Text.literal(line2),
                    Text.literal(line3)
            };
            SignText signText = new SignText(messages, messages, dyeColor, true);
            if (sign.getWorld() != null) {
                sign.setText(signText, true);
                sign.setWaxed(true);
            } else {
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("is_waxed", true);
                SignText.CODEC.encodeStart(NbtOps.INSTANCE, signText)
                        .result()
                        .ifPresent(encoded -> nbt.put("front_text", encoded));
                sign.read(nbt, world.toServerWorld().getRegistryManager());
            }
            sign.markDirty();
        }
    }

    private static void fillClipped(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int minDx,
            int minRelY,
            int minDz,
            int maxDx,
            int maxRelY,
            int maxDz,
            BlockState state
    ) {
        int x0 = Math.max(centerX + minDx, chunkBox.getMinX());
        int x1 = Math.min(centerX + maxDx, chunkBox.getMaxX());
        int z0 = Math.max(centerZ + minDz, chunkBox.getMinZ());
        int z1 = Math.min(centerZ + maxDz, chunkBox.getMaxZ());
        if (x0 > x1 || z0 > z1) {
            return;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int relY = minRelY; relY <= maxRelY; relY++) {
                    pos.set(x, baseY + relY, z);
                    world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    private static void setRel(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ,
            int dx,
            int relY,
            int dz,
            BlockState state
    ) {
        int wx = centerX + dx;
        int wy = baseY + relY;
        int wz = centerZ + dz;
        if (chunkBox.contains(wx, wy, wz)) {
            world.setBlockState(new BlockPos(wx, wy, wz), state, Block.NOTIFY_LISTENERS);
        }
    }
}
