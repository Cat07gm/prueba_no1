package pruebano1.modid.world;

import net.minecraft.block.*;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
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
import pruebano1.modid.block.JefriMailboxBlock;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.entity.PiDidiEntity;

/**
 * Generador colosal (520x520 bloques) de "La Isla de Albert Penstein" (Isla Penstein),
 * con topografía 100% continua y suave (sin cortes verticales de tierra ni bloques flotantes),
 * foso oceánico perimetral para no chocar jamás con islas normales, cimientos sólidos bajo cada estructura y detalles completos.
 */
public class IslaPensteinPiece extends StructurePiece {

    public static final int SCALE = 5;
    public static final int ISLAND_RADIUS = 52 * SCALE; // 260 bloques de radio = 520x520 bloques
    public static final int SEA_LEVEL = 62;
    public static final int PLATEAU_Y = 74;

    private final int centerX;
    private final int centerZ;

    public IslaPensteinPiece(int centerX, int centerZ) {
        super(
                ModStructures.ISLA_PENSTEIN_PIECE,
                0,
                new BlockBox(
                        centerX - 120,
                        34,
                        centerZ - 120,
                        centerX + 120,
                        115,
                        centerZ + 120
                )
        );
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.setOrientation(Direction.SOUTH);
    }

    public IslaPensteinPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.ISLA_PENSTEIN_PIECE, nbt);
        this.centerX = nbt.getInt("CenterX");
        this.centerZ = nbt.getInt("CenterZ");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("CenterX", this.centerX);
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
        // Generado íntegramente por generateChunkForIsland desde IslaPensteinFeature
    }

    public static void generateChunkForIsland(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            Random random
    ) {
        // 1. Terreno suave y continuo + foso oceánico exterior para borrar cualquier colisión con islas vanilla
        generateIslandTerrain(world, chunkBox, centerX, centerZ, random);

        // 2. Explanada principal con cimientos sólidos, alberca olímpica, camastros, fuente central y jardines
        generateCourtyardAndPool(world, chunkBox, centerX, centerZ);

        // 3. Muelle gigante techado, yate de lujo y gran escalinata amurallada (sin tierra expuesta ni techos flotantes)
        generateBoatDock(world, chunkBox, centerX, centerZ);

        // 4. Las 4 villas blancas de invitados con techos azules, ventanas, camas y decoración interior
        generateGuestCabin(world, chunkBox, centerX, centerZ, -116, PLATEAU_Y, -100, 26, 26, Direction.SOUTH);
        generateGuestCabin(world, chunkBox, centerX, centerZ, -74, PLATEAU_Y, -104, 26, 26, Direction.SOUTH);
        generateGuestCabin(world, chunkBox, centerX, centerZ, -32, PLATEAU_Y, -104, 26, 26, Direction.SOUTH);
        generateGuestCabin(world, chunkBox, centerX, centerZ, 60, PLATEAU_Y, -24, 28, 36, Direction.WEST);

        // 5. Gran Residencia Principal sobre podio escalonado + Buzón de Jefri Penstein
        generateMainResidence(world, chunkBox, centerX, centerZ, random);

        // 6. Terraza Sur con cimientos sólidos hasta el suelo, escalinata central y los 2 helipuertos con "H"
        generateLowerHelipadTerrace(world, chunkBox, centerX, centerZ);

        // 7. Carretera costera y muralla del acantilado Oeste adaptada suavemente al terreno
        generateWestPerimeterRoad(world, chunkBox, centerX, centerZ);

        // 8. Más de 160 palmeras tropicales gigantes (plantadas únicamente en pasto o arena natural)
        generatePalmTrees(world, chunkBox, centerX, centerZ);

        // 9. Aplicar cualquier construcción o cambio personalizado que hayas guardado con "/islapenstein guardar"
        IslaPensteinCustomEdits.applyCustomChunkIfPresent(world, chunkBox, centerX, centerZ);

        // 10. Spawnear a Pi Didi y patrullas iniciales de los 4 niños
        spawnIslandInhabitants(world, chunkBox, centerX, centerZ, random);
    }

    /**
     * Calcula el factor de forma (0.0 en la orilla del mar -> 1.0+ en el centro) de la isla.
     */
    public static double getIslandShapeFactor(int dx, int dz) {
        double sx = dx / (double) SCALE;
        double sz = dz / (double) SCALE;

        double mainBody = 1.0 - (Math.pow(sx / 34.0, 2) + Math.pow(sz / 31.0, 2));
        double northPoint = 1.0 - (Math.pow((sx - 6.0) / 15.0, 2) + Math.pow((sz + 27.0) / 18.0, 2));
        double southWestPoint = 1.0 - (Math.pow((sx + 19.0) / 19.0, 2) + Math.pow((sz - 21.0) / 17.0, 2));
        double southEastPoint = 1.0 - (Math.pow((sx - 10.0) / 20.0, 2) + Math.pow((sz - 24.0) / 17.0, 2));
        double northWestCape = 1.0 - (Math.pow((sx + 21.0) / 16.0, 2) + Math.pow((sz + 13.0) / 16.0, 2));

        double shape = Math.max(
                mainBody,
                Math.max(
                        Math.max(northPoint * 0.86, southWestPoint * 0.90),
                        Math.max(southEastPoint * 0.92, northWestCape * 0.86)
                )
        );

        // Ensenada suave de la bahía Este
        double eastBayCut = Math.max(0.0, 1.0 - (Math.pow((sx - 28.0) / 13.0, 2) + Math.pow((sz + 11.0) / 11.0, 2)));
        shape -= eastBayCut * 0.44;

        // Ondulación suave de la costa
        double coastWave = 0.045 * Math.sin(sx * 0.20) * Math.cos(sz * 0.20)
                + 0.018 * Math.sin(dx * 0.07) * Math.cos(dz * 0.07);
        return shape + coastWave;
    }

    /**
     * Altura superficial 100% continua y sin saltos bruscos:
     * Sube suavemente con curva smoothstep desde SEA_LEVEL (62) hasta la meseta central en PLATEAU_Y (74).
     */
    public static int getIslandSurfaceY(int dx, int dz) {
        double shape = getIslandShapeFactor(dx, dz);
        if (shape <= 0.0) {
            return SEA_LEVEL - 4;
        }

        // Interpolación suave (smoothstep) entre shape=0.0 (Y=62) y shape=0.38 (Y=74)
        double t = Math.min(1.0, shape / 0.38);
        double smooth = t * t * (3.0 - 2.0 * t);
        return SEA_LEVEL + (int) Math.round(smooth * (PLATEAU_Y - SEA_LEVEL));
    }

    private static void generateIslandTerrain(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            Random random
    ) {
        int margin = 24;
        int minX = Math.max(centerX - (ISLAND_RADIUS + margin), chunkBox.getMinX());
        int maxX = Math.min(centerX + (ISLAND_RADIUS + margin), chunkBox.getMaxX());
        int minZ = Math.max(centerZ - (ISLAND_RADIUS + margin), chunkBox.getMinZ());
        int maxZ = Math.min(centerZ + (ISLAND_RADIUS + margin), chunkBox.getMaxZ());

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int x = minX; x <= maxX; x++) {
            int dx = x - centerX;
            for (int z = minZ; z <= maxZ; z++) {
                int dz = z - centerZ;
                double shape = getIslandShapeFactor(dx, dz);

                if (shape <= 0.0) {
                    // Foso oceánico perimetral: si había alguna isla vanilla cerca, sumergirla suavemente bajo el mar
                    // para que jamás haya cortes bruscos ni choques con tierra normal
                    int floorDepth = SEA_LEVEL - 1 - (int) Math.min(10.0, Math.round(-shape * 28.0));
                    for (int y = floorDepth + 1; y <= 105; y++) {
                        pos.set(x, y, z);
                        BlockState cur = world.getBlockState(pos);
                        if (y <= SEA_LEVEL) {
                            if (!cur.isOf(Blocks.WATER)) {
                                world.setBlockState(pos, Blocks.WATER.getDefaultState(), Block.NOTIFY_LISTENERS);
                            }
                        } else if (!cur.isAir()) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        }
                    }
                    pos.set(x, floorDepth, z);
                    if (!world.getBlockState(pos).isSolidBlock(world, pos)) {
                        world.setBlockState(pos, Blocks.SAND.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                    continue;
                }

                int surfaceY = getIslandSurfaceY(dx, dz);

                // Determinar si es playa de arena (costa Este/Noreste o toda la orilla baja Y<=64) o acantilado rocoso (costa Oeste/Sur media)
                boolean isBeach = (surfaceY <= SEA_LEVEL + 2) || (dx > 25 && dz < 35 && surfaceY <= SEA_LEVEL + 5);
                boolean isRockySlope = !isBeach && (dx < -40 || dz > 95) && (surfaceY <= SEA_LEVEL + 7);

                // Limpiar bloques superiores hasta Y=110
                for (int y = surfaceY + 1; y <= 110; y++) {
                    pos.set(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }

                // Rellenar cimiento submarino desde Y=36 hasta la superficie de la isla
                for (int y = 36; y <= surfaceY; y++) {
                    pos.set(x, y, z);
                    BlockState stateToPlace;
                    if (y == surfaceY) {
                        if (isBeach) {
                            stateToPlace = Blocks.SAND.getDefaultState();
                        } else if (isRockySlope) {
                            stateToPlace = (random.nextInt(3) == 0)
                                    ? Blocks.MOSSY_COBBLESTONE.getDefaultState()
                                    : Blocks.ANDESITE.getDefaultState();
                        } else {
                            stateToPlace = Blocks.GRASS_BLOCK.getDefaultState();
                        }
                    } else if (y >= surfaceY - 4) {
                        stateToPlace = isBeach
                                ? Blocks.SANDSTONE.getDefaultState()
                                : (isRockySlope ? Blocks.STONE.getDefaultState() : Blocks.DIRT.getDefaultState());
                    } else {
                        stateToPlace = Blocks.STONE.getDefaultState();
                    }
                    world.setBlockState(pos, stateToPlace, Block.NOTIFY_LISTENERS);
                }

                // Vegetación y flores tropicales en las colinas verdes fuera de los edificios
                if (!isBeach && !isRockySlope && surfaceY >= 68) {
                    boolean inCompoundZone = (dx >= -124 && dx <= 96 && dz >= -108 && dz <= 146);
                    if (!inCompoundZone && random.nextInt(8) == 0) {
                        pos.set(x, surfaceY + 1, z);
                        BlockState flora = (random.nextInt(6) == 0)
                                ? Blocks.BLUE_ORCHID.getDefaultState()
                                : (random.nextInt(3) == 0 ? Blocks.FERN.getDefaultState() : Blocks.SHORT_GRASS.getDefaultState());
                        world.setBlockState(pos, flora, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    /**
     * Construye la explanada con cimientos sólidos de ladrillo de piedra, la alberca olímpica con camastros y sombrillas,
     * una fuente central de cuarzo y los jardines simétricos.
     */
    private static void generateCourtyardAndPool(StructureWorldAccess world, BlockBox chunkBox, int centerX, int centerZ) {
        int courtY = PLATEAU_Y; // Y = 74
        int minCourtDx = -120;
        int maxCourtDx = 48;
        int minCourtDz = -104;
        int maxCourtDz = 14;

        int x0 = Math.max(minCourtDx, chunkBox.getMinX() - centerX);
        int x1 = Math.min(maxCourtDx, chunkBox.getMaxX() - centerX);
        int z0 = Math.max(minCourtDz, chunkBox.getMinZ() - centerZ);
        int z1 = Math.min(maxCourtDz, chunkBox.getMaxZ() - centerZ);

        // 1. Pavimento de la explanada CON cimiento sólido hasta el terreno natural para que NUNCA flote ni deje tierra expuesta
        for (int dx = x0; dx <= x1; dx++) {
            for (int dz = z0; dz <= z1; dz++) {
                if (getIslandShapeFactor(dx, dz) < 0.18) {
                    continue;
                }
                int natY = Math.max(SEA_LEVEL, getIslandSurfaceY(dx, dz));
                boolean isBorder = (dx == minCourtDx || dx == maxCourtDx || dz == minCourtDz || dz == maxCourtDz);

                for (int y = Math.min(natY, courtY - 4); y < courtY; y++) {
                    setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.STONE_BRICKS.getDefaultState());
                }

                if (isBorder) {
                    setRel(world, chunkBox, centerX, centerZ, dx, courtY, dz, Blocks.STONE_BRICKS.getDefaultState());
                    // Evitar bloquear la entrada de la escalinata del muelle (dz = -66..-52 en dx = 48) y el paso Sur a la mansión
                    boolean isDockEntrance = (dx == maxCourtDx && dz >= -66 && dz <= -52);
                    boolean isSouthWalkway = (dz == maxCourtDz && dx >= -20 && dx <= 35);
                    if (!isDockEntrance && !isSouthWalkway) {
                        setRel(world, chunkBox, centerX, centerZ, dx, courtY + 1, dz, Blocks.STONE_BRICK_WALL.getDefaultState());
                        if ((dx + dz) % 12 == 0) {
                            setRel(world, chunkBox, centerX, centerZ, dx, courtY + 2, dz, Blocks.LANTERN.getDefaultState());
                        }
                    }
                } else {
                    // Senderos decorativos de ladrillos de cuarzo cada 16 bloques
                    boolean isPathway = (Math.abs(dx + 36) <= 2) || (Math.abs(dz + 44) <= 2);
                    setRel(
                            world,
                            chunkBox,
                            centerX,
                            centerZ,
                            dx,
                            courtY,
                            dz,
                            isPathway ? Blocks.QUARTZ_BRICKS.getDefaultState() : Blocks.SMOOTH_SANDSTONE.getDefaultState()
                    );
                }
            }
        }

        // 2. Alberca Olímpica Turquesa (dx = -106..-52, dz = -58..-8)
        fillClipped(world, chunkBox, centerX, centerZ, -108, courtY, -60, -50, courtY, -6, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, -106, courtY - 5, -58, -52, courtY - 4, -8, Blocks.PRISMARINE_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, -106, courtY - 3, -58, -52, courtY, -8, Blocks.WATER.getDefaultState());

        for (int dx = -100; dx <= -58; dx += 10) {
            fillClipped(world, chunkBox, centerX, centerZ, dx, courtY - 4, -54, dx, courtY - 4, -12, Blocks.SEA_LANTERN.getDefaultState());
        }

        // Trampolín de cuarzo en el borde Norte de la alberca (dx = -79, dz = -60..-55)
        fillClipped(world, chunkBox, centerX, centerZ, -80, courtY + 1, -60, -78, courtY + 1, -55, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());

        // Camastros y sombrillas playeras junto a la alberca (dx = -46, dz = -52..-14)
        for (int dz = -50; dz <= -16; dz += 11) {
            // Camastro blanco y azul
            setRel(world, chunkBox, centerX, centerZ, -46, courtY + 1, dz, Blocks.QUARTZ_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            setRel(world, chunkBox, centerX, centerZ, -45, courtY + 1, dz, Blocks.LIGHT_BLUE_CARPET.getDefaultState());
            setRel(world, chunkBox, centerX, centerZ, -44, courtY + 1, dz, Blocks.WHITE_CARPET.getDefaultState());
            // Sombrilla de playa
            for (int h = 1; h <= 3; h++) {
                setRel(world, chunkBox, centerX, centerZ, -45, courtY + h, dz + 3, Blocks.OAK_FENCE.getDefaultState());
            }
            fillClipped(world, chunkBox, centerX, centerZ, -47, courtY + 4, dz + 1, -43, courtY + 4, dz + 5, Blocks.CYAN_CARPET.getDefaultState());
            setRel(world, chunkBox, centerX, centerZ, -45, courtY + 4, dz + 3, Blocks.WHITE_CARPET.getDefaultState());
        }

        // 3. Dos grandes jardines simétricos con bordillo de ladrillo de piedra y flores (Foto 3)
        int[][] gardens = {{-36, -66, -10, -24}, {6, -66, 32, -24}};
        for (int[] g : gardens) {
            fillClipped(world, chunkBox, centerX, centerZ, g[0] - 1, courtY, g[1] - 1, g[2] + 1, courtY, g[3] + 1, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, g[0], courtY, g[1], g[2], courtY, g[3], Blocks.GRASS_BLOCK.getDefaultState());
            // Setos decorativos en las esquinas del jardín
            fillClipped(world, chunkBox, centerX, centerZ, g[0] + 1, courtY + 1, g[1] + 1, g[2] - 1, courtY + 1, g[1] + 1, Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
            fillClipped(world, chunkBox, centerX, centerZ, g[0] + 1, courtY + 1, g[3] - 1, g[2] - 1, courtY + 1, g[3] - 1, Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
        }

        // 4. Fuente monumental de cuarzo en el centro entre ambos jardines (dx = -5..1, dz = -48..-42)
        fillClipped(world, chunkBox, centerX, centerZ, -6, courtY + 1, -49, 2, courtY + 1, -41, Blocks.QUARTZ_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, -5, courtY + 1, -48, 1, courtY + 1, -42, Blocks.WATER.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, -3, courtY + 1, -46, -1, courtY + 4, -44, Blocks.QUARTZ_PILLAR.getDefaultState());
        setRel(world, chunkBox, centerX, centerZ, -2, courtY + 5, -45, Blocks.SEA_LANTERN.getDefaultState());
        setRel(world, chunkBox, centerX, centerZ, -2, courtY + 6, -45, Blocks.WATER.getDefaultState());
    }

    /**
     * Construye el muelle monumental techado, el yate blanco y la gran escalinata amurallada hacia la explanada
     * (limpiando el cielo hasta Y=110 y poniendo muros laterales de piedra para que NUNCA quede pasto flotando ni tierra expuesta).
     */
    private static void generateBoatDock(StructureWorldAccess world, BlockBox chunkBox, int centerX, int centerZ) {
        int dockY = SEA_LEVEL + 2; // Y = 64

        // Pasarela del muelle (dx = 78..225, dz = -66..-52)
        fillClipped(world, chunkBox, centerX, centerZ, 78, dockY, -66, 225, dockY, -52, Blocks.SPRUCE_PLANKS.getDefaultState());

        int minDx = Math.max(78, chunkBox.getMinX() - centerX);
        int maxDx = Math.min(225, chunkBox.getMaxX() - centerX);
        int minDz = Math.max(-66, chunkBox.getMinZ() - centerZ);
        int maxDz = Math.min(-52, chunkBox.getMaxZ() - centerZ);

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                if ((dz == -66 || dz == -52) && dx >= 84 && dx <= 188) {
                    setRel(world, chunkBox, centerX, centerZ, dx, dockY + 1, dz, Blocks.OAK_FENCE.getDefaultState());
                    if (dx % 12 == 0) {
                        setRel(world, chunkBox, centerX, centerZ, dx, dockY + 2, dz, Blocks.OAK_FENCE.getDefaultState());
                        setRel(world, chunkBox, centerX, centerZ, dx, dockY + 3, dz, Blocks.LANTERN.getDefaultState());
                    }
                }
                if ((dz == -66 || dz == -52) && dx % 10 == 0) {
                    for (int y = 40; y < dockY; y++) {
                        setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.SPRUCE_LOG.getDefaultState());
                    }
                }
            }
        }

        // Pabellón techado al final del muelle (dx = 190..225, dz = -70..-48)
        fillClipped(world, chunkBox, centerX, centerZ, 190, dockY, -70, 225, dockY, -48, Blocks.DARK_OAK_PLANKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 189, dockY + 6, -71, 226, dockY + 6, -47, Blocks.SPRUCE_SLAB.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 192, dockY + 7, -68, 223, dockY + 7, -50, Blocks.SPRUCE_PLANKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 195, dockY + 8, -65, 220, dockY + 8, -53, Blocks.SPRUCE_SLAB.getDefaultState());

        int[][] dockPillars = {
                {191, -69}, {191, -49}, {202, -69}, {202, -49},
                {213, -69}, {213, -49}, {224, -69}, {224, -49}
        };
        for (int[] p : dockPillars) {
            fillClipped(world, chunkBox, centerX, centerZ, p[0], dockY + 1, p[1], p[0], dockY + 5, p[1], Blocks.SPRUCE_LOG.getDefaultState());
        }
        setRel(world, chunkBox, centerX, centerZ, 207, dockY + 5, -59, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));

        // Yate blanco de lujo amarrado junto al muelle (dx = 130..168, dz = -80..-70)
        fillClipped(world, chunkBox, centerX, centerZ, 130, SEA_LEVEL, -80, 168, SEA_LEVEL + 1, -70, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 136, SEA_LEVEL + 2, -79, 162, SEA_LEVEL + 4, -71, Blocks.QUARTZ_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 138, SEA_LEVEL + 3, -79, 160, SEA_LEVEL + 3, -71, Blocks.BLACK_STAINED_GLASS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 135, SEA_LEVEL + 5, -79, 163, SEA_LEVEL + 5, -71, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());

        // GRAN ESCALINATA AMURALLADA desde el muelle/playa (dx = 48..78, dz = -66..-52) subiendo de Y=64 a Y=74
        for (int step = 0; step <= 10; step++) {
            int dxStart = 78 - step * 3;
            int dxEnd = dxStart + 2;
            int y = dockY + step;

            // Cimiento sólido debajo de cada escalón hasta Y=58
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, 58, -68, dxEnd, y - 1, -50, Blocks.STONE_BRICKS.getDefaultState());
            // Escalón de arenisca lisa
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, y, -66, dxEnd, y, -52, Blocks.SMOOTH_SANDSTONE.getDefaultState());
            // Limpiar AIRE hasta Y=110 para que NUNCA quede una capa de pasto flotando encima (corrección de Foto 2)
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, y + 1, -68, dxEnd, 110, -50, Blocks.AIR.getDefaultState());

            // Muros de contención laterales de ladrillo de piedra y cuarzo (dz = -68..-67 y dz = -51..-50)
            // para cubrir toda la ladera lateral y que nunca se vea tierra cortada
            int wallTop = Math.max(y + 2, PLATEAU_Y + 1);
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, y, -68, dxEnd, wallTop, -67, Blocks.STONE_BRICKS.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, y, -51, dxEnd, wallTop, -50, Blocks.STONE_BRICKS.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, wallTop + 1, -68, dxEnd, wallTop + 1, -67, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, dxStart, wallTop + 1, -51, dxEnd, wallTop + 1, -50, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        }
    }

    /**
     * Construye una de las 4 grandes villas blancas con cimiento sólido, techo azul, muebles interiores y porche.
     */
    private static void generateGuestCabin(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            int startDx,
            int baseY,
            int startDz,
            int sizeX,
            int sizeZ,
            Direction doorSide
    ) {
        int endDx = startDx + sizeX - 1;
        int endDz = startDz + sizeZ - 1;
        int wallTopY = baseY + 7;

        if (chunkBox.getMaxX() < centerX + startDx - 4 || chunkBox.getMinX() > centerX + endDx + 4
                || chunkBox.getMaxZ() < centerZ + startDz - 4 || chunkBox.getMinZ() > centerZ + endDz + 4) {
            return;
        }

        // Cimiento sólido debajo de la villa hasta Y=62 para que jamás flote en las laderas
        fillClipped(world, chunkBox, centerX, centerZ, startDx - 1, 62, startDz - 1, endDx + 1, baseY - 1, endDz + 1, Blocks.STONE_BRICKS.getDefaultState());

        // Piso de madera de roble y vaciado interior
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY, startDz, endDx, baseY, endDz, Blocks.OAK_PLANKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx + 1, baseY + 1, startDz + 1, endDx - 1, wallTopY, endDz - 1, Blocks.AIR.getDefaultState());

        // Alfombra interior y decoración (cama grande, libreros y mesas)
        fillClipped(world, chunkBox, centerX, centerZ, startDx + 5, baseY + 1, startDz + 5, endDx - 5, baseY + 1, endDz - 5, Blocks.CYAN_CARPET.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx + 2, baseY + 1, startDz + 2, startDx + 6, baseY + 3, startDz + 2, Blocks.BOOKSHELF.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx - 6, baseY + 1, startDz + 3, endDx - 3, baseY + 1, startDz + 6, Blocks.RED_WOOL.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx - 6, baseY + 1, startDz + 2, endDx - 3, baseY + 1, startDz + 2, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());

        // Muros exteriores blancos de cuarzo liso
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, startDz, endDx, wallTopY, startDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, endDz, endDx, wallTopY, endDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, startDz, startDx, wallTopY, endDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx, baseY + 1, startDz, endDx, wallTopY, endDz, Blocks.SMOOTH_QUARTZ.getDefaultState());

        // Pilares de ladrillo de piedra en las 4 esquinas
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, startDz, startDx + 1, wallTopY, startDz + 1, Blocks.STONE_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx - 1, baseY + 1, startDz, endDx, wallTopY, startDz + 1, Blocks.STONE_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, endDz - 1, startDx + 1, wallTopY, endDz, Blocks.STONE_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx - 1, baseY + 1, endDz - 1, endDx, wallTopY, endDz, Blocks.STONE_BRICKS.getDefaultState());

        // Ventanales amplios
        int midX = startDx + sizeX / 2;
        int midZ = startDz + sizeZ / 2;
        fillClipped(world, chunkBox, centerX, centerZ, midX - 4, baseY + 3, startDz, midX + 4, baseY + 5, startDz, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, midX - 4, baseY + 3, endDz, midX + 4, baseY + 5, endDz, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 3, midZ - 4, startDx, baseY + 5, midZ + 4, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, endDx, baseY + 3, midZ - 4, endDx, baseY + 5, midZ + 4, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());

        // Entrada arqueada de 3x4 bloques
        if (doorSide == Direction.SOUTH) {
            fillClipped(world, chunkBox, centerX, centerZ, midX - 1, baseY + 1, endDz, midX + 1, baseY + 4, endDz, Blocks.AIR.getDefaultState());
        } else {
            fillClipped(world, chunkBox, centerX, centerZ, startDx, baseY + 1, midZ - 1, startDx, baseY + 4, midZ + 1, Blocks.AIR.getDefaultState());
        }

        // Iluminación interior
        setRel(world, chunkBox, centerX, centerZ, midX, wallTopY, midZ, Blocks.SEA_LANTERN.getDefaultState());

        // Techo azul celeste piramidal escalonado
        for (int layer = 0; layer <= 5; layer++) {
            int roofY = wallTopY + 1 + layer;
            int inset = layer * 2;
            BlockState roofBlock = (layer % 2 == 0)
                    ? Blocks.LIGHT_BLUE_CONCRETE.getDefaultState()
                    : Blocks.CYAN_TERRACOTTA.getDefaultState();
            fillClipped(
                    world,
                    chunkBox,
                    centerX,
                    centerZ,
                    startDx - 1 + inset,
                    roofY,
                    startDz - 1 + inset,
                    endDx + 1 - inset,
                    roofY,
                    endDz + 1 - inset,
                    roofBlock
            );
        }
    }

    /**
     * Construye la Gran Residencia Principal sobre un podio sólido escalonado con cimientos profundos,
     * pórtico de arcos, salón real, cofres, Pi Didi y el Buzón indestructible de Jefri.
     */
    private static void generateMainResidence(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            Random random
    ) {
        int baseY = PLATEAU_Y + 1; // Y = 75 (1 bloque por encima de la explanada Y=74, sin saltos de tierra)
        int minDx = -44;
        int maxDx = 58;
        int minDz = 18;
        int maxDz = 86;
        int wallTopY = baseY + 11;

        if (chunkBox.getMaxX() < centerX + minDx - 8 || chunkBox.getMinX() > centerX + maxDx + 8
                || chunkBox.getMaxZ() < centerZ + minDz - 16 || chunkBox.getMinZ() > centerZ + maxDz + 8) {
            return;
        }

        // Cimiento sólido debajo de toda la plataforma de la mansión desde Y=62 hasta baseY
        fillClipped(world, chunkBox, centerX, centerZ, minDx - 5, 62, minDz - 5, maxDx + 5, baseY - 1, maxDz + 5, Blocks.STONE_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx - 5, baseY, minDz - 5, maxDx + 5, baseY, maxDz + 5, Blocks.SMOOTH_QUARTZ.getDefaultState());

        // Piso interior con gran alfombra real roja y dorada
        fillClipped(world, chunkBox, centerX, centerZ, minDx + 1, baseY, minDz + 1, maxDx - 1, baseY, maxDz - 1, Blocks.QUARTZ_BRICKS.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx + 10, baseY, minDz + 10, maxDx - 10, baseY, maxDz - 10, Blocks.RED_WOOL.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx + 14, baseY, minDz + 14, maxDx - 14, baseY, maxDz - 14, Blocks.GOLD_BLOCK.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx + 16, baseY, minDz + 16, maxDx - 16, baseY, maxDz - 16, Blocks.RED_WOOL.getDefaultState());

        // Vaciado del gran salón interior
        fillClipped(world, chunkBox, centerX, centerZ, minDx + 1, baseY + 1, minDz + 1, maxDx - 1, wallTopY, maxDz - 1, Blocks.AIR.getDefaultState());

        // Muros exteriores blancos de cuarzo liso
        fillClipped(world, chunkBox, centerX, centerZ, minDx, baseY + 1, minDz, maxDx, wallTopY, minDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx, baseY + 1, maxDz, maxDx, wallTopY, maxDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, minDx, baseY + 1, minDz, minDx, wallTopY, maxDz, Blocks.SMOOTH_QUARTZ.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, maxDx, baseY + 1, minDz, maxDx, wallTopY, maxDz, Blocks.SMOOTH_QUARTZ.getDefaultState());

        // Arcos monumentales abiertos en las fachadas Norte (minDz) y Sur (maxDz)
        for (int archCenterDx = minDx + 12; archCenterDx <= maxDx - 12; archCenterDx += 14) {
            fillClipped(world, chunkBox, centerX, centerZ, archCenterDx - 3, baseY + 1, minDz, archCenterDx + 3, baseY + 6, minDz, Blocks.AIR.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, archCenterDx - 3, baseY + 1, maxDz, archCenterDx + 3, baseY + 6, maxDz, Blocks.AIR.getDefaultState());
        }

        // Ventanales laterales de cristal azul claro
        for (int winCenterDz = minDz + 12; winCenterDz <= maxDz - 12; winCenterDz += 14) {
            fillClipped(world, chunkBox, centerX, centerZ, minDx, baseY + 3, winCenterDz - 3, minDx, baseY + 8, winCenterDz + 3, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
            fillClipped(world, chunkBox, centerX, centerZ, maxDx, baseY + 3, winCenterDz - 3, maxDx, baseY + 8, winCenterDz + 3, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState());
        }

        // Columnas interiores de cuarzo cincelado, candelabros y trono central para Pi Didi
        for (int colDx = minDx + 18; colDx <= maxDx - 18; colDx += 22) {
            for (int colDz = minDz + 18; colDz <= maxDz - 18; colDz += 24) {
                fillClipped(world, chunkBox, centerX, centerZ, colDx, baseY + 1, colDz, colDx + 1, wallTopY, colDz + 1, Blocks.QUARTZ_PILLAR.getDefaultState());
                setRel(world, chunkBox, centerX, centerZ, colDx, wallTopY, colDz + 2, Blocks.SEA_LANTERN.getDefaultState());
            }
        }

        // Podio / Trono interior de Pi Didi en el centro del salón (dx = 4..10, dz = 50..56)
        fillClipped(world, chunkBox, centerX, centerZ, 4, baseY + 1, 50, 10, baseY + 1, 56, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        fillClipped(world, chunkBox, centerX, centerZ, 6, baseY + 2, 54, 8, baseY + 5, 55, Blocks.GOLD_BLOCK.getDefaultState());

        // Gran techo azul celeste escalonado
        for (int layer = 0; layer <= 7; layer++) {
            int roofY = wallTopY + 1 + layer;
            int inset = layer * 3;
            BlockState roofState = (layer % 2 == 0)
                    ? Blocks.LIGHT_BLUE_CONCRETE.getDefaultState()
                    : Blocks.CYAN_TERRACOTTA.getDefaultState();
            fillClipped(
                    world,
                    chunkBox,
                    centerX,
                    centerZ,
                    minDx - 2 + inset,
                    roofY,
                    minDz - 2 + inset,
                    maxDx + 2 - inset,
                    roofY,
                    maxDz + 2 - inset,
                    roofState
            );
        }

        // 4 Cofres de botín dentro de la Residencia Principal
        placeLootChest(world, chunkBox, new BlockPos(centerX - 30, baseY + 1, centerZ + 52), LootTables.END_CITY_TREASURE_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 44, baseY + 1, centerZ + 52), LootTables.END_CITY_TREASURE_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX - 30, baseY + 1, centerZ + 34), LootTables.WOODLAND_MANSION_CHEST, random);
        placeLootChest(world, chunkBox, new BlockPos(centerX + 44, baseY + 1, centerZ + 34), LootTables.WOODLAND_MANSION_CHEST, random);

        // PEDESTAL Y BUZÓN INDESTRUCTIBLE DE JEFRI PENSTEIN frente a la entrada Norte de la Residencia Principal (dx = 7, dz = 8)
        fillClipped(world, chunkBox, centerX, centerZ, 5, PLATEAU_Y, 6, 9, PLATEAU_Y, 10, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        setRel(world, chunkBox, centerX, centerZ, 7, PLATEAU_Y + 1, 8, Blocks.GOLD_BLOCK.getDefaultState());
        setRel(
                world,
                chunkBox,
                centerX,
                centerZ,
                7,
                PLATEAU_Y + 2,
                8,
                ModBlocks.JEFRI_MAILBOX.getDefaultState().with(JefriMailboxBlock.FACING, Direction.NORTH)
        );
    }

    private static void placeLootChest(
            StructureWorldAccess world,
            BlockBox chunkBox,
            BlockPos pos,
            RegistryKey<LootTable> lootTable,
            Random random
    ) {
        if (chunkBox.contains(pos)) {
            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_LISTENERS);
            if (world.getBlockEntity(pos) instanceof LootableContainerBlockEntity chest) {
                chest.setLootTable(lootTable, random.nextLong());
            }
        }
    }

    /**
     * Construye la terraza inferior Sur con cimientos sólidos hasta el suelo (para que NUNCA flote sobre la playa como en la Foto 3),
     * escalinata central desde el jardín y los 2 helipuertos circulares con letra "H".
     */
    private static void generateLowerHelipadTerrace(StructureWorldAccess world, BlockBox chunkBox, int centerX, int centerZ) {
        int terraceY = 70;

        if (chunkBox.getMaxX() < centerX - 20 || chunkBox.getMinX() > centerX + 95
                || chunkBox.getMaxZ() < centerZ + 90 || chunkBox.getMinZ() > centerZ + 148) {
            return;
        }

        int minDx = Math.max(-14, chunkBox.getMinX() - centerX);
        int maxDx = Math.min(82, chunkBox.getMaxX() - centerX);
        int minDz = Math.max(92, chunkBox.getMinZ() - centerZ);
        int maxDz = Math.min(142, chunkBox.getMaxZ() - centerZ);

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                // Recortar siguiendo la curva natural de la isla (shape >= 0.22) para que JAMÁS sobresalga flotando hacia la playa
                if (getIslandShapeFactor(dx, dz) < 0.22) {
                    continue;
                }

                // Muro de contención Norte (dz = 92..93) con escalinata central en dx = 22..34
                if (dz <= 93) {
                    if (dx >= 22 && dx <= 34) {
                        int stepY = (dz == 92) ? 73 : 71;
                        for (int y = 62; y <= stepY; y++) {
                            setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.SMOOTH_SANDSTONE.getDefaultState());
                        }
                        for (int y = stepY + 1; y <= 85; y++) {
                            setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.AIR.getDefaultState());
                        }
                    } else {
                        for (int y = 62; y <= PLATEAU_Y; y++) {
                            setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
                        }
                        setRel(world, chunkBox, centerX, centerZ, dx, PLATEAU_Y + 1, dz, Blocks.STONE_BRICK_WALL.getDefaultState());
                    }
                    continue;
                }

                // Cimiento sólido de ladrillos de piedra musgosos desde Y=60 hasta terraceY-1 para que nunca haya huecos debajo
                for (int y = 60; y < terraceY; y++) {
                    setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
                }
                // Limpiar bloques por encima de la terraza (ya que el terreno natural era más alto en el centro)
                for (int y = terraceY + 1; y <= 85; y++) {
                    setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.AIR.getDefaultState());
                }

                // Dos helipuertos circulares (centrados en (12, 118) y (52, 118), radio 13) con pista gris, anillo amarillo y letra "H" blanca
                double dist1 = Math.hypot(dx - 12, dz - 118);
                double dist2 = Math.hypot(dx - 52, dz - 118);

                BlockState topBlock = Blocks.SMOOTH_SANDSTONE.getDefaultState();
                if (dist1 <= 13.5 || dist2 <= 13.5) {
                    int cx = (dist1 <= 13.5) ? 12 : 52;
                    int cz = 118;
                    double d = Math.min(dist1, dist2);
                    int rdx = dx - cx;
                    int rdz = dz - cz;

                    // Letra "H" blanca en el centro del helipuerto
                    boolean isLetterH = (Math.abs(rdz) <= 5 && (Math.abs(rdx + 3) <= 1 || Math.abs(rdx - 3) <= 1))
                            || (Math.abs(rdz) <= 1 && Math.abs(rdx) <= 3);

                    if (isLetterH) {
                        topBlock = Blocks.SMOOTH_QUARTZ.getDefaultState();
                    } else if (d >= 11.2 && d <= 12.6) {
                        topBlock = Blocks.YELLOW_CONCRETE.getDefaultState();
                    } else {
                        topBlock = Blocks.GRAY_CONCRETE.getDefaultState();
                    }
                }
                setRel(world, chunkBox, centerX, centerZ, dx, terraceY, dz, topBlock);

                // Muro perimetral en el borde exterior de la terraza
                if (getIslandShapeFactor(dx + 2, dz) < 0.22 || getIslandShapeFactor(dx - 2, dz) < 0.22 || getIslandShapeFactor(dx, dz + 2) < 0.22) {
                    setRel(world, chunkBox, centerX, centerZ, dx, terraceY, dz, Blocks.MOSSY_STONE_BRICKS.getDefaultState());
                    setRel(world, chunkBox, centerX, centerZ, dx, terraceY + 1, dz, Blocks.MOSSY_STONE_BRICK_WALL.getDefaultState());
                }
            }
        }
    }

    /**
     * Construye la carretera costera y el muro blanco del acantilado Oeste con cimiento sólido en la ladera.
     */
    private static void generateWestPerimeterRoad(StructureWorldAccess world, BlockBox chunkBox, int centerX, int centerZ) {
        if (chunkBox.getMaxX() < centerX - 155 || chunkBox.getMinX() > centerX - 125
                || chunkBox.getMaxZ() < centerZ - 95 || chunkBox.getMinZ() > centerZ + 95) {
            return;
        }

        int minDx = Math.max(-148, chunkBox.getMinX() - centerX);
        int maxDx = Math.min(-132, chunkBox.getMaxX() - centerX);
        int minDz = Math.max(-88, chunkBox.getMinZ() - centerZ);
        int maxDz = Math.min(88, chunkBox.getMaxZ() - centerZ);

        for (int dz = minDz; dz <= maxDz; dz++) {
            int roadY = getIslandSurfaceY(-140, dz);
            if (roadY < SEA_LEVEL + 3) continue;

            for (int dx = minDx; dx <= maxDx; dx++) {
                // Cimiento sólido debajo de la carretera hasta Y=60
                for (int y = 60; y < roadY; y++) {
                    setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.STONE_BRICKS.getDefaultState());
                }
                for (int y = roadY + 1; y <= roadY + 6; y++) {
                    setRel(world, chunkBox, centerX, centerZ, dx, y, dz, Blocks.AIR.getDefaultState());
                }

                if (dx <= -146) {
                    setRel(world, chunkBox, centerX, centerZ, dx, roadY, dz, Blocks.STONE_BRICKS.getDefaultState());
                    setRel(world, chunkBox, centerX, centerZ, dx, roadY + 1, dz, Blocks.QUARTZ_BRICKS.getDefaultState());
                    setRel(world, chunkBox, centerX, centerZ, dx, roadY + 2, dz, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
                } else {
                    setRel(world, chunkBox, centerX, centerZ, dx, roadY, dz, Blocks.POLISHED_ANDESITE.getDefaultState());
                }
            }
        }
    }

    /**
     * Genera las palmeras tropicales gigantes asegurándose de que solo crezcan sobre pasto o arena natural
     * (sin atravesar edificios, escalinatas ni helipuertos).
     */
    private static void generatePalmTrees(StructureWorldAccess world, BlockBox chunkBox, int centerX, int centerZ) {
        int[][] basePalmCoords = {
                {-4, -9}, {4, -9},
                {-23, -11}, {-23, -3}, {-19, 1}, {-14, 1}, {-9, 1},
                {-24, 6}, {-20, 8}, {-16, 6}, {-12, 9}, {-22, 13}, {-17, 14}, {-13, 16},
                {-11, 5}, {-11, 12}, {14, 6}, {14, 13}, {-6, 18}, {12, 18},
                {12, -16}, {16, -18}, {20, -8}, {22, -2}, {21, 8}, {18, 17},
                {4, -26}, {9, -23}, {-18, 22}, {15, 29}
        };

        int[][] offsets = {{0, 0}, {-11, 9}, {12, -8}, {-8, -12}, {10, 11}};

        BlockState log = Blocks.JUNGLE_LOG.getDefaultState();
        BlockState leaves = Blocks.JUNGLE_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);

        for (int i = 0; i < basePalmCoords.length; i++) {
            for (int j = 0; j < offsets.length; j++) {
                int dx = basePalmCoords[i][0] * SCALE + offsets[j][0];
                int dz = basePalmCoords[i][1] * SCALE + offsets[j][1];

                // Excluir zonas de alberca, villas, mansión, escalinata del muelle y helipuertos
                if (dx >= -112 && dx <= -44 && dz >= -64 && dz <= -2) continue;
                if (dx >= -120 && dx <= 0 && dz >= -108 && dz <= -72) continue;
                if (dx >= 56 && dx <= 92 && dz >= -28 && dz <= 16) continue;
                if (dx >= 44 && dx <= 84 && dz >= -72 && dz <= -46) continue;
                if (dx >= -50 && dx <= 64 && dz >= 12 && dz <= 92) continue;
                if (dx >= -16 && dx <= 84 && dz >= 90 && dz <= 144) continue;

                if (chunkBox.getMaxX() < centerX + dx - 8 || chunkBox.getMinX() > centerX + dx + 8
                        || chunkBox.getMaxZ() < centerZ + dz - 8 || chunkBox.getMinZ() > centerZ + dz + 8) {
                    continue;
                }

                if (getIslandShapeFactor(dx, dz) < 0.08) continue;
                int baseY = getIslandSurfaceY(dx, dz);
                if (baseY < SEA_LEVEL + 1) continue;

                int trunkHeight = 11 + ((i + j) % 5);
                int leanX = ((i + j) % 2 == 0) ? 2 : -2;
                int topDx = dx + leanX;
                int topY = baseY + trunkHeight;

                for (int h = 1; h <= trunkHeight; h++) {
                    int curDx = dx + (leanX * h) / trunkHeight;
                    setRel(world, chunkBox, centerX, centerZ, curDx, baseY + h, dz, log);
                }

                setRel(world, chunkBox, centerX, centerZ, topDx, topY + 1, dz, leaves);
                for (int r = 1; r <= 5; r++) {
                    int drop = (r >= 4) ? -1 : 0;
                    setRel(world, chunkBox, centerX, centerZ, topDx + r, topY + drop, dz, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx - r, topY + drop, dz, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx, topY + drop, dz + r, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx, topY + drop, dz - r, leaves);
                }
                for (int d = 1; d <= 3; d++) {
                    int drop = (d == 3) ? -1 : 0;
                    setRel(world, chunkBox, centerX, centerZ, topDx + d, topY + drop, dz + d, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx - d, topY + drop, dz + d, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx + d, topY + drop, dz - d, leaves);
                    setRel(world, chunkBox, centerX, centerZ, topDx - d, topY + drop, dz - d, leaves);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void spawnIslandInhabitants(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            Random random
    ) {
        // 1. Mini-Jefe Pi Didi frente a su trono dentro de la Residencia Principal (dx = 7, y = 77, dz = 48)
        BlockPos bossPos = new BlockPos(centerX + 7, PLATEAU_Y + 3, centerZ + 48);
        if (chunkBox.contains(bossPos)) {
            PiDidiEntity piDidi = ModEntities.PI_DIDI.create(world.toServerWorld(), SpawnReason.STRUCTURE);
            if (piDidi != null) {
                piDidi.refreshPositionAndAngles(bossPos.getX() + 0.5D, bossPos.getY(), bossPos.getZ() + 0.5D, 180.0F, 0.0F);
                piDidi.setPersistent();
                world.spawnEntityAndPassengers(piDidi);
            }
        }

        // 2. Patrullas iniciales distribuidas por la isla
        int[][] patrolSpots = {
                {-80, -30},  // Alberca olímpica
                {-45, -70},  // Explanada frente a las villas
                {-10, -45},  // Jardines centrales
                {30, -40},   // Entrada del muelle
                {-90, 50},   // Bosque de palmeras Suroeste
                {-30, 90},   // Jardín Sur Oeste
                {25, 90},    // Jardín Sur Este
                {32, 118},   // Terraza de los helipuertos
                {75, -10},   // Villa Este
                {140, -59},  // Mitad del gran muelle
                {35, -120},  // Península Norte
                {-95, 110}   // Acantilado Suroeste
        };

        EntityType<? extends MobEntity>[] childTypes = new EntityType[]{
                ModEntities.YOSTIN_BIBER,
                ModEntities.VITOR_JUGO,
                ModEntities.FER_OVILLA,
                ModEntities.VALENTINA_PRIETA
        };

        for (int i = 0; i < patrolSpots.length; i++) {
            int dx = patrolSpots[i][0];
            int dz = patrolSpots[i][1];
            int y = (dx >= 78 && dz >= -66 && dz <= -52)
                    ? (SEA_LEVEL + 3)
                    : (dz >= 94 && dz <= 140 && dx >= -12 && dx <= 80 ? 71 : (getIslandSurfaceY(dx, dz) + 1));
            BlockPos spawnPos = new BlockPos(centerX + dx, y, centerZ + dz);

            if (chunkBox.contains(spawnPos)) {
                for (int c = 0; c < 2; c++) {
                    EntityType<? extends MobEntity> chosenType = childTypes[(i + c + random.nextInt(childTypes.length)) % childTypes.length];
                    MobEntity child = chosenType.create(world.toServerWorld(), SpawnReason.STRUCTURE);
                    if (child != null) {
                        child.refreshPositionAndAngles(
                                spawnPos.getX() + 0.5D + (c * 1.5D),
                                spawnPos.getY(),
                                spawnPos.getZ() + 0.5D,
                                random.nextFloat() * 360.0F,
                                0.0F
                        );
                        child.setPersistent();
                        world.spawnEntityAndPassengers(child);
                    }
                }
            }
        }
    }

    private static void fillClipped(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            int minDx,
            int minY,
            int minDz,
            int maxDx,
            int maxY,
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
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    private static void setRel(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int centerZ,
            int dx,
            int y,
            int dz,
            BlockState state
    ) {
        int wx = centerX + dx;
        int wz = centerZ + dz;
        if (chunkBox.contains(wx, y, wz)) {
            world.setBlockState(new BlockPos(wx, y, wz), state, Block.NOTIFY_LISTENERS);
        }
    }
}
