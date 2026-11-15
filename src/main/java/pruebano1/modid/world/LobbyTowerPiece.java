package pruebano1.modid.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;

/**
 * Torre del Lobby principal: fuste de piedra lisa escalonado (~60 bloques) envuelto por cintas
 * helicoidales carmesí y turquesa que se despegan en la cima, hueco interior con escalera de roble
 * en espiral, cascada y vegetación, y azotea ajardinada con el punto de aparición del mundo.
 *
 * Coordenadas relativas: (0, 0, 0) es el centro de la torre a nivel del suelo de la plaza.
 */
public final class LobbyTowerPiece {

    /** Radio de la plaza circular; aquí termina la construcción y la zona protegida. */
    public static final int SITE_RADIUS = 24;
    /** Bloques fuera de la plaza donde el terreno se suaviza en talud en vez de cortarse. */
    private static final int GRADE_RANGE = 10;

    /** Altura del piso de la azotea (se camina en ROOF_Y + 1). */
    public static final int ROOF_Y = 58;
    public static final int MIN_REL_Y = -6;
    public static final int MAX_REL_Y = 80;

    /** Medio ancho del hueco interior (11x11) y del anillo donde va la escalera. */
    private static final int SHAFT = 5;
    private static final int LAST_STEP = 115;
    private static final int TENDRIL_TOP_Y = 71;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState STONE = Blocks.SMOOTH_STONE.getDefaultState();
    private static final BlockState STONE_SLAB = Blocks.SMOOTH_STONE_SLAB.getDefaultState();
    private static final BlockState BRICKS = Blocks.STONE_BRICKS.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();
    private static final BlockState FALLING_WATER = Blocks.WATER.getDefaultState().with(FluidBlock.LEVEL, 8);
    private static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.getDefaultState();
    private static final BlockState SHROOMLIGHT = Blocks.SHROOMLIGHT.getDefaultState();
    private static final BlockState WARM_LIGHT = Blocks.OCHRE_FROGLIGHT.getDefaultState();
    private static final BlockState MOSS = Blocks.MOSS_BLOCK.getDefaultState();
    private static final BlockState OAK_LEAVES = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    private static final BlockState AZALEA_LEAVES = Blocks.AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    private static final BlockState FLOWER_LEAVES = Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
    private static final BlockState GLASS = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
    private static final BlockState LANTERN = Blocks.LANTERN.getDefaultState();

    private static final BlockState RED_RIBBON = Blocks.CRIMSON_PLANKS.getDefaultState();
    private static final BlockState RED_GLOW = Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
    private static final BlockState TEAL_RIBBON = Blocks.WARPED_PLANKS.getDefaultState();
    private static final BlockState TEAL_GLOW = Blocks.VERDANT_FROGLIGHT.getDefaultState();

    private LobbyTowerPiece() {
    }

    /** Porción de la obra que cae dentro de un chunk. */
    private record Ctx(StructureWorldAccess world, BlockBox box, int cx, int by, int cz) {
        void set(int dx, int y, int dz, BlockState state) {
            int wx = cx + dx;
            int wy = by + y;
            int wz = cz + dz;
            if (box.contains(wx, wy, wz)) {
                world.setBlockState(new BlockPos(wx, wy, wz), state, Block.NOTIFY_LISTENERS);
            }
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        set(x, y, z, state);
                    }
                }
            }
        }
    }

    public static void generateChunk(StructureWorldAccess world, BlockBox chunkBox, int centerX, int baseY, int centerZ) {
        Ctx c = new Ctx(world, chunkBox, centerX, baseY, centerZ);
        prepareSite(c);
        buildPlaza(c);
        buildShell(c);
        buildInterior(c);
        buildOpenings(c);
        buildRoof(c);
        buildStairs(c);
        buildRibbons(c);
        LobbyTowerCustomEdits.applyIfPresent(world, chunkBox, centerX, baseY, centerZ);
    }

    /** Medio ancho exterior de la torre a cada altura: base ancha, tres cuerpos, cornisas y capitel. */
    static int halfWidth(int y) {
        if (y <= 1) return 12;
        if (y <= 5) return 11;
        if (y <= 13) return 10;
        if (y <= 21) return 9;
        if (y == 22) return 10;
        if (y <= 33) return 8;
        if (y <= 35) return 9;
        if (y <= 47) return 7;
        if (y <= 49) return 8;
        if (y <= 55) return 7;
        if (y == 56) return 8;
        return 9;
    }

    /** ¿Columna dentro de la plaza circular? Fuera de ella nunca se pega nada guardado. */
    static boolean isInsideSite(int dx, int dz) {
        return dx * dx + dz * dz <= SITE_RADIUS * SITE_RADIUS;
    }

    private static int hash(int x, int y, int z) {
        int h = x * 73428767 ^ y * 912931 ^ z * 19349663;
        h ^= h >>> 13;
        h *= 1274126177;
        return Math.floorMod(h ^ (h >>> 16), 100);
    }

    private static boolean isSoft(BlockState state) {
        return state.isAir() || state.isLiquid() || state.isReplaceable();
    }

    // =========================================================================
    // 1. TERRENO: plaza nivelada, relleno bajo ella y talud de transición
    // =========================================================================

    private static void prepareSite(Ctx c) {
        StructureWorldAccess world = c.world();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int by = c.by();
        int top = world.getTopYInclusive();
        int bottom = world.getBottomY() + 1;
        int reach = SITE_RADIUS + GRADE_RANGE;

        for (int wx = c.box().getMinX(); wx <= c.box().getMaxX(); wx++) {
            int dx = wx - c.cx();
            for (int wz = c.box().getMinZ(); wz <= c.box().getMaxZ(); wz++) {
                int dz = wz - c.cz();
                int distSq = dx * dx + dz * dz;
                if (distSq > reach * reach) {
                    continue;
                }

                if (isInsideSite(dx, dz)) {
                    for (int y = top; y > by; y--) {
                        pos.set(wx, y, wz);
                        if (!world.getBlockState(pos).isAir()) {
                            world.setBlockState(pos, AIR, Block.NOTIFY_LISTENERS);
                        }
                    }
                    boolean lawn = distSq > 21 * 21;
                    world.setBlockState(pos.set(wx, by, wz), lawn ? Blocks.GRASS_BLOCK.getDefaultState() : pavement(dx, dz), Block.NOTIFY_LISTENERS);
                    for (int y = by - 1; y >= by - 3; y--) {
                        world.setBlockState(pos.set(wx, y, wz), lawn ? Blocks.DIRT.getDefaultState() : Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                    // Relleno bajo la plaza solo donde hay hueco, para que nada quede flotando
                    for (int y = by - 4; y >= Math.max(bottom, by - 24); y--) {
                        pos.set(wx, y, wz);
                        if (isSoft(world.getBlockState(pos))) {
                            world.setBlockState(pos, Blocks.STONE.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                    continue;
                }

                int d = (int) Math.ceil(Math.sqrt(distSq) - SITE_RADIUS);
                // Terreno más alto: talud con pasto en vez de pared vertical
                DrafteaCasinoPiece.gradeTerrainAroundPlaza(world, pos, wx, wz, by + d * 2 - 1);

                // Terreno más bajo: falda de tierra que desciende un bloque por bloque de distancia
                if (d <= 8) {
                    int target = by - d;
                    for (int y = target; y >= Math.max(bottom, target - 24); y--) {
                        pos.set(wx, y, wz);
                        if (!isSoft(world.getBlockState(pos))) {
                            break;
                        }
                        world.setBlockState(
                                pos,
                                y == target ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.DIRT.getDefaultState(),
                                Block.NOTIFY_LISTENERS
                        );
                    }
                }
            }
        }
    }

    private static BlockState pavement(int dx, int dz) {
        int h = hash(dx, 0, dz);
        if (h < 22) return Blocks.MOSSY_STONE_BRICKS.getDefaultState();
        if (h < 32) return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        return BRICKS;
    }

    // =========================================================================
    // 2. PLAZA: camino de acceso, faroles, arbustos y estanque para el salto desde la cima
    // =========================================================================

    private static void buildPlaza(Ctx c) {
        // Camino desde la puerta sur
        c.fill(-1, 0, 13, 1, 0, 21, Blocks.POLISHED_ANDESITE.getDefaultState());

        // Faroles
        int[][] lamps = {{13, 13}, {-13, 13}, {13, -13}, {-13, -13}, {0, -18}, {-18, 0}, {4, 20}, {-4, 20}};
        for (int[] lamp : lamps) {
            c.fill(lamp[0], 1, lamp[1], lamp[0], 3, lamp[1], Blocks.STONE_BRICK_WALL.getDefaultState());
            c.set(lamp[0], 4, lamp[1], LANTERN);
        }

        // Arbustos
        int[][] bushes = {{16, 9}, {16, -9}, {-16, 8}, {-16, -8}, {8, 17}, {-8, 17}, {8, -17}, {-8, -17}};
        for (int[] bush : bushes) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    boolean corner = ox != 0 && oz != 0;
                    c.set(bush[0] + ox, 1, bush[1] + oz, leafAt(bush[0] + ox, 1, bush[1] + oz));
                    if (!corner) {
                        c.set(bush[0] + ox, 2, bush[1] + oz, leafAt(bush[0] + ox, 2, bush[1] + oz));
                    }
                }
            }
        }

        // Estanque al este, justo bajo el trampolín de la azotea
        for (int x = 13; x <= 23; x++) {
            for (int z = -5; z <= 5; z++) {
                boolean rim = x == 13 || x == 23 || Math.abs(z) == 5;
                if (rim) {
                    c.set(x, 0, z, BRICKS);
                    c.fill(x, -4, z, x, -1, z, STONE);
                } else {
                    c.fill(x, -3, z, x, 0, z, WATER);
                    c.set(x, -4, z, (x % 2 == 0 && z % 2 == 0) ? SEA_LANTERN : STONE);
                }
            }
        }
    }

    private static BlockState leafAt(int x, int y, int z) {
        int h = hash(x, y, z);
        if (h < 18) return FLOWER_LEAVES;
        if (h < 45) return AZALEA_LEAVES;
        return OAK_LEAVES;
    }

    // =========================================================================
    // 3. FUSTE: muros de piedra lisa, hueco interior, estanque del fondo y cornisas
    // =========================================================================

    private static void buildShell(Ctx c) {
        for (int y = -4; y <= ROOF_Y; y++) {
            int hw = halfWidth(y);
            for (int x = -hw; x <= hw; x++) {
                for (int z = -hw; z <= hw; z++) {
                    int r = Math.max(Math.abs(x), Math.abs(z));
                    BlockState state;
                    if (y == ROOF_Y) {
                        state = STONE;
                    } else if (r > SHAFT) {
                        // La cara interior del muro lleva manchas de musgo
                        state = (r == SHAFT + 1 && y >= 1 && hash(x, y, z) < 24) ? MOSS : STONE;
                    } else if (y == -4) {
                        state = (r <= 3 && x % 2 == 0 && z % 2 == 0) ? SEA_LANTERN : STONE;
                    } else if (y <= 0) {
                        state = r <= 3 ? WATER : (y == 0 ? BRICKS : STONE);
                    } else {
                        state = AIR;
                    }
                    c.set(x, y, z, state);
                }
            }
        }

        // Losas sobre cada retranqueo para que el escalonado no quede en arista viva
        for (int y = 1; y < ROOF_Y; y++) {
            int below = halfWidth(y - 1);
            int here = halfWidth(y);
            if (below <= here) {
                continue;
            }
            for (int x = -below; x <= below; x++) {
                for (int z = -below; z <= below; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) > here) {
                        c.set(x, y, z, STONE_SLAB);
                    }
                }
            }
        }

        // Luz cálida en esquinas y centros de cada cornisa
        for (int y : new int[]{22, 35, 49, 57}) {
            int hw = halfWidth(y);
            for (int[] p : new int[][]{{hw, hw}, {hw, -hw}, {-hw, hw}, {-hw, -hw}, {hw, 0}, {-hw, 0}, {0, hw}, {0, -hw}}) {
                c.set(p[0], y, p[1], WARM_LIGHT);
            }
        }
    }

    // =========================================================================
    // 4. INTERIOR: cascada y columnas de follaje iluminadas
    // =========================================================================

    private static void buildInterior(Ctx c) {
        // Cascada por el lado oeste del hueco, cayendo al estanque del fondo
        for (int z = -1; z <= 1; z++) {
            c.set(-3, ROOF_Y - 1, z, WATER);
            c.fill(-3, 1, z, -3, ROOF_Y - 2, z, FALLING_WATER);
        }

        // Follaje colgante en las esquinas del hueco, con luz escondida cada pocos bloques
        for (int sx : new int[]{-3, 3}) {
            for (int sz : new int[]{-3, 3}) {
                for (int y = 1; y < ROOF_Y; y++) {
                    c.set(sx, y, sz, y % 6 == 2 ? SHROOMLIGHT : leafAt(sx, y, sz));
                }
            }
        }
        // Hojas sueltas enmarcando la cascada
        for (int z : new int[]{-2, 2}) {
            for (int y = 1; y < ROOF_Y; y++) {
                if (hash(-3, y, z) < 55) {
                    c.set(-3, y, z, leafAt(-3, y, z));
                }
            }
        }
    }

    // =========================================================================
    // 5. PUERTA Y VENTANAS
    // =========================================================================

    private static void buildOpenings(Ctx c) {
        // Puerta sur: pasillo a través de la base hasta el hueco interior
        c.fill(-1, 1, SHAFT + 1, 1, 3, 12, AIR);
        c.set(0, 3, 9, LANTERN.with(LanternBlock.HANGING, true));
        c.set(-2, 3, 11, WARM_LIGHT);
        c.set(2, 3, 11, WARM_LIGHT);

        // Ventanal sobre la puerta
        window(c, 0, 1, 6, 9);
        // Ventanas en los dos cuerpos superiores
        for (int[] dir : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
            if (dir[1] != 1) {
                window(c, dir[0], dir[1], 26, 28);
            }
            window(c, dir[0], dir[1], 40, 42);
        }
    }

    /** Hueco de 3 de ancho que atraviesa el muro, cerrado con vidrio en la cara exterior. */
    private static void window(Ctx c, int dirX, int dirZ, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            int hw = halfWidth(y);
            for (int o = -1; o <= 1; o++) {
                for (int r = SHAFT + 1; r <= hw; r++) {
                    int x = dirX * r + dirZ * o;
                    int z = dirZ * r + dirX * o;
                    c.set(x, y, z, r == hw ? GLASS : AIR);
                }
            }
        }
    }

    // =========================================================================
    // 6. AZOTEA: waystone central, jardín, pretil, pozo de caída y trampolín
    // =========================================================================

    private static void buildRoof(Ctx c) {
        int floor = ROOF_Y;
        int deck = ROOF_Y + 1;

        // Jardín: musgo y plantas en la franja exterior
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                int r = Math.max(Math.abs(x), Math.abs(z));
                if (r < 6 || (x >= 6 && Math.abs(z) <= 1) || hash(x, floor, z) >= 60) {
                    continue;
                }
                c.set(x, floor, z, MOSS);
                int h = hash(x, deck, z);
                if (h < 18) {
                    c.set(x, deck, z, Blocks.MOSS_CARPET.getDefaultState());
                } else if (h < 34) {
                    c.set(x, deck, z, Blocks.SHORT_GRASS.getDefaultState());
                } else if (h < 42) {
                    c.set(x, deck, z, Blocks.FERN.getDefaultState());
                } else if (h < 49) {
                    c.set(x, deck, z, Blocks.AZALEA.getDefaultState());
                } else if (h < 54) {
                    c.set(x, deck, z, Blocks.FLOWERING_AZALEA.getDefaultState());
                } else if (h < 59) {
                    c.set(x, deck, z, Blocks.OXEYE_DAISY.getDefaultState());
                }
            }
        }

        // Pretil con hojas que se desbordan y linternas; abierto al este para el trampolín
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != 9 || (x == 9 && Math.abs(z) <= 1)) {
                    continue;
                }
                int ax = Math.abs(x);
                int az = Math.abs(z);
                boolean lampSpot = (ax == 9 && (az == 9 || az == 5)) || (az == 9 && ax == 5);
                if (lampSpot) {
                    c.set(x, deck, z, STONE);
                    c.set(x, deck + 1, z, LANTERN);
                } else if (hash(x, deck, z) < 38) {
                    c.set(x, deck, z, leafAt(x, deck, z));
                } else {
                    c.set(x, deck, z, ((x + z) & 1) == 0 ? STONE : STONE_SLAB);
                }
            }
        }

        // Arbolitos en las cuatro esquinas
        for (int sx : new int[]{-7, 7}) {
            for (int sz : new int[]{-7, 7}) {
                c.set(sx, floor, sz, MOSS);
                for (int ox = -2; ox <= 2; ox++) {
                    for (int oz = -2; oz <= 2; oz++) {
                        boolean corner = Math.abs(ox) == 2 && Math.abs(oz) == 2;
                        if (!corner) {
                            c.set(sx + ox, deck + 2, sz + oz, leafAt(sx + ox, deck + 2, sz + oz));
                            c.set(sx + ox, deck + 3, sz + oz, leafAt(sx + ox, deck + 3, sz + oz));
                        }
                        if (Math.abs(ox) <= 1 && Math.abs(oz) <= 1) {
                            c.set(sx + ox, deck + 4, sz + oz, leafAt(sx + ox, deck + 4, sz + oz));
                        }
                    }
                }
                c.fill(sx, deck, sz, sx, deck + 2, sz, Blocks.OAK_LOG.getDefaultState());
                c.set(sx, deck + 3, sz, SHROOMLIGHT);
            }
        }

        // Plataforma del waystone y luces empotradas a su alrededor
        c.fill(-1, floor, -1, 1, floor, 1, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
        c.set(-2, floor, 0, SEA_LANTERN);
        c.set(0, floor, 2, SEA_LANTERN);
        c.set(0, floor, -2, SEA_LANTERN);

        // Pozo de caída hacia el estanque interior
        c.fill(2, floor, -2, 3, floor, 2, AIR);

        // Abertura por donde desemboca la escalera en espiral
        for (int x = -SHAFT; x <= SHAFT; x++) {
            for (int z = -SHAFT; z <= SHAFT; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) >= SHAFT - 1) {
                    int idx = stairIndex(x, z);
                    if (idx >= 10 && idx <= 18) {
                        c.set(x, floor, z, AIR);
                    }
                }
            }
        }

        // Trampolín al este, sobre el estanque exterior
        BlockState plank = Blocks.OAK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
        c.fill(10, floor, -1, 15, floor, 1, plank);
    }

    // =========================================================================
    // 7. ESCALERA EN ESPIRAL (roble, 2 de ancho, medio bloque por paso)
    // =========================================================================

    /**
     * Posición de una celda del anillo de escalera dentro de una vuelta (0..31).
     * El 0 queda justo al este de la puerta y la espiral sube en sentido este → norte → oeste → sur.
     */
    private static int stairIndex(int x, int z) {
        int ring = SHAFT - 1;
        int cx = Math.max(-ring, Math.min(ring, x));
        int cz = Math.max(-ring, Math.min(ring, z));
        int p;
        if (cz == ring && cx < ring) {
            p = cx + ring;
        } else if (cx == ring && cz > -ring) {
            p = 2 * ring + (ring - cz);
        } else if (cz == -ring && cx > -ring) {
            p = 4 * ring + (ring - cx);
        } else {
            p = 6 * ring + (cz + ring);
        }
        return Math.floorMod(p - 6, 8 * ring);
    }

    private static void buildStairs(Ctx c) {
        BlockState low = Blocks.OAK_SLAB.getDefaultState();
        BlockState high = Blocks.OAK_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP);
        for (int x = -SHAFT; x <= SHAFT; x++) {
            for (int z = -SHAFT; z <= SHAFT; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) < SHAFT - 1) {
                    continue;
                }
                int idx = stairIndex(x, z);
                for (int n = idx; n <= LAST_STEP; n += 32) {
                    c.set(x, 1 + n / 2, z, (n & 1) == 0 ? low : high);
                }
            }
        }
    }

    // =========================================================================
    // 8. CINTAS HELICOIDALES carmesí y turquesa, con luces, que se despegan en la cima
    // =========================================================================

    private static void buildRibbons(Ctx c) {
        ribbon(c, 0.30, 1, RED_RIBBON, RED_GLOW);
        ribbon(c, 0.80, 1, RED_RIBBON, RED_GLOW);
        ribbon(c, 0.05, -1, TEAL_RIBBON, TEAL_GLOW);
        ribbon(c, 0.55, -1, TEAL_RIBBON, TEAL_GLOW);
    }

    private static void ribbon(Ctx c, double start, int direction, BlockState body, BlockState glow) {
        for (int y = 1; y <= TENDRIL_TOP_Y; y++) {
            boolean tendril = y > ROOF_Y - 1;
            int r = tendril ? 10 + (y - ROOF_Y) / 2 : halfWidth(y) + 1;
            int perimeter = 8 * r;
            int q = (int) Math.round((start + direction * y / 72.0) * perimeter);
            for (int o = tendril ? 0 : -1; o <= 1; o++) {
                int p = Math.floorMod(q + o, perimeter);
                int x;
                int z;
                if (p < 2 * r) {
                    x = p - r;
                    z = r;
                } else if (p < 4 * r) {
                    x = r;
                    z = r - (p - 2 * r);
                } else if (p < 6 * r) {
                    x = r - (p - 4 * r);
                    z = -r;
                } else {
                    x = -r;
                    z = -r + (p - 6 * r);
                }
                // Se dejan libres la puerta sur y la trayectoria del salto desde el trampolín
                if ((y <= 4 && z > 0 && Math.abs(x) <= 2) || (tendril && x >= 9 && Math.abs(z) <= 4)) {
                    continue;
                }
                c.set(x, y, z, (o == 0 && y % 6 == 3) ? glow : body);
            }
        }
    }
}
