package pruebano1.modid.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.block.PodcastCassetteBlock;

/**
 * Construye el "Escenario del Podcast" (Arena y Escenario de José Torres / Jhoseph towers):
 * - Explanada / Arena circular amplia (radio 19 bloques) despejada para el combate contra José Torres.
 * - Escenario elevado al Norte con alfombra roja/dorada, torres de bocinas laterales, luces de escenario
 *   y mesa de podcast.
 * - Pedestal central con el bloque indestructible "Cassete de Podcast" listo para invocar a José Torres
 *   con el "Acordeon de José Torres".
 */
public final class PodcastStagePiece {

    public static final int ARENA_RADIUS = 19;
    public static final int TOTAL_RADIUS = 23;

    private PodcastStagePiece() {
    }

    public static void generateChunk(
            StructureWorldAccess world,
            BlockBox chunkBox,
            int centerX,
            int baseY,
            int centerZ
    ) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState foundation = Blocks.STONE_BRICKS.getDefaultState();
        BlockState darkFloor = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState deepslateTile = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState quartzFloor = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState chiseledQuartz = Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState();
        BlockState goldBlock = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState packedIce = Blocks.PACKED_ICE.getDefaultState();
        BlockState cyanGlass = Blocks.CYAN_STAINED_GLASS.getDefaultState();
        BlockState blackConcrete = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState noteBlock = Blocks.NOTE_BLOCK.getDefaultState();
        BlockState seaLantern = Blocks.SEA_LANTERN.getDefaultState();
        BlockState froglight = Blocks.OCHRE_FROGLIGHT.getDefaultState();
        BlockState sprucePlanks = Blocks.SPRUCE_PLANKS.getDefaultState();
        BlockState darkOakPlanks = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState redCarpet = Blocks.RED_CARPET.getDefaultState();
        BlockState yellowCarpet = Blocks.YELLOW_CARPET.getDefaultState();
        BlockState wallBorder = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();

        BlockPos.Mutable mpos = new BlockPos.Mutable();

        for (int x = chunkBox.getMinX(); x <= chunkBox.getMaxX(); x++) {
            for (int z = chunkBox.getMinZ(); z <= chunkBox.getMaxZ(); z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 23.5D) {
                    continue;
                }

                // 1. Limpiar vegetación/bloques superiores sobre la arena para que el combate sea libre
                for (int y = baseY + 1; y <= baseY + 24; y++) {
                    mpos.set(x, y, z);
                    if (!world.getBlockState(mpos).isOf(ModBlocks.PODCAST_CASSETTE)) {
                        world.setBlockState(mpos, air, 2);
                    }
                }

                // 2. Cimentación inferior hasta encontrar suelo firme
                for (int y = baseY - 1; y >= baseY - 10; y--) {
                    mpos.set(x, y, z);
                    if (dist <= ARENA_RADIUS + 2.0D) {
                        world.setBlockState(mpos, foundation, 2);
                    }
                }

                // 3. Talud exterior suave (entre radio 19.5 y 23.5)
                if (dist > ARENA_RADIUS) {
                    int stepDown = (int) Math.floor((dist - ARENA_RADIUS) * 0.75D);
                    mpos.set(x, baseY - stepDown, z);
                    world.setBlockState(mpos, Blocks.GRASS_BLOCK.getDefaultState(), 2);
                    continue;
                }

                // 4. Piso principal de la Arena (y = baseY)
                mpos.set(x, baseY, z);
                int absDx = Math.abs(dx);
                int absDz = Math.abs(dz);

                if (absDx <= 1 && absDz <= 1) {
                    // Base dorada y de cuarzo bajo el pedestal del Cassete de Podcast
                    world.setBlockState(mpos, (absDx == 0 && absDz == 0) ? goldBlock : chiseledQuartz, 2);
                } else if (Math.abs(dist - 6.0D) < 0.75D || Math.abs(dist - 12.0D) < 0.75D) {
                    // Anillos luminosos de hielo bajo cristal cian (temática de "Una Bola de Hielo" / Jhoseph towers)
                    mpos.set(x, baseY - 1, z);
                    world.setBlockState(mpos, packedIce, 2);
                    mpos.set(x, baseY, z);
                    world.setBlockState(mpos, cyanGlass, 2);
                } else if (absDx <= 2 && dz >= 2 && dz <= ARENA_RADIUS) {
                    // Pasillo central de entrada desde el Sur hacia el altar
                    world.setBlockState(mpos, absDx == 2 ? goldBlock : quartzFloor, 2);
                } else if (((dx + dz) & 1) == 0 && dist <= 15.0D) {
                    world.setBlockState(mpos, darkFloor, 2);
                } else {
                    world.setBlockState(mpos, deepslateTile, 2);
                }

                // 5. Borde perimetral y faroles de la Arena (en el anillo exterior ~18.5..19.0)
                if (dist >= 18.0D && dist <= ARENA_RADIUS && dz > -8) {
                    // Dejar abierta la entrada Sur (absDx <= 3 && dz > 15)
                    if (!(absDx <= 3 && dz > 15)) {
                        mpos.set(x, baseY + 1, z);
                        world.setBlockState(mpos, wallBorder, 2);
                        if ((absDx % 6 == 0 || absDz % 6 == 0)) {
                            mpos.set(x, baseY + 2, z);
                            world.setBlockState(mpos, froglight, 2);
                        }
                    }
                }

                // 6. ESCENARIO DEL PODCAST AL NORTE (dz entre -16 y -8, dx entre -9 y +9)
                if (dz >= -16 && dz <= -8 && absDx <= 9) {
                    // Plataforma elevada del escenario (altura baseY + 1)
                    mpos.set(x, baseY + 1, z);
                    if (dz == -8 && absDx <= 3) {
                        // Escalón frontal de subida al escenario
                        world.setBlockState(mpos, Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState(), 2);
                    } else {
                        world.setBlockState(mpos, (absDx == 9 || dz == -16) ? chiseledQuartz : darkOakPlanks, 2);
                        // Alfombra roja y dorada sobre el escenario
                        if (dz <= -9 && dz >= -15 && absDx <= 8) {
                            mpos.set(x, baseY + 2, z);
                            world.setBlockState(mpos, (absDx == 0 || dz == -12) ? yellowCarpet : redCarpet, 2);
                        }
                    }

                    // Pared trasera del escenario (dz == -16) con diseño negro/dorado y blanco/cian
                    if (dz == -16 && absDx <= 8) {
                        for (int sy = 2; sy <= 6; sy++) {
                            mpos.set(x, baseY + sy, z);
                            if (sy == 6 || absDx == 8) {
                                world.setBlockState(mpos, chiseledQuartz, 2);
                            } else if (sy == 4 && absDx % 3 == 0) {
                                world.setBlockState(mpos, seaLantern, 2);
                            } else {
                                world.setBlockState(mpos, blackConcrete, 2);
                            }
                        }
                    }

                    // Torres de bocinas / parlantes gigantes a ambos lados del escenario (absDx >= 7 && dz >= -14 && dz <= -12)
                    if ((absDx == 7 || absDx == 8) && (dz == -13 || dz == -12)) {
                        for (int sy = 2; sy <= 5; sy++) {
                            mpos.set(x, baseY + sy, z);
                            world.setBlockState(mpos, (dz == -12 && (sy == 3 || sy == 4)) ? noteBlock : blackConcrete, 2);
                        }
                        mpos.set(x, baseY + 6, z);
                        world.setBlockState(mpos, froglight, 2);
                    }

                    // Mesa del Podcast en el escenario (dz == -12, absDx <= 2)
                    if (dz == -12 && absDx <= 2) {
                        mpos.set(x, baseY + 2, z);
                        world.setBlockState(mpos, sprucePlanks, 2);
                        if (absDx == 1) {
                            // Micrófonos decorativos sobre la mesa del podcast
                            mpos.set(x, baseY + 3, z);
                            world.setBlockState(mpos, Blocks.END_ROD.getDefaultState(), 2);
                        }
                    }
                    // Asientos detrás de la mesa del podcast (dz == -14, absDx <= 1)
                    if (dz == -14 && absDx <= 1) {
                        mpos.set(x, baseY + 2, z);
                        world.setBlockState(mpos, ModBlocks.CASINO_CHAIR.getDefaultState(), 2);
                    }

                    // Techo / marquesina de luces sobre el escenario
                    if (dz >= -16 && dz <= -9 && absDx <= 8) {
                        mpos.set(x, baseY + 7, z);
                        if (dz == -9 && absDx % 2 == 0) {
                            world.setBlockState(mpos, froglight, 2);
                        } else {
                            world.setBlockState(mpos, Blocks.DARK_OAK_SLAB.getDefaultState(), 2);
                        }
                    }
                }

                // 7. PEDESTAL CENTRAL Y BLOQUE INDESTRUCTIBLE "CASSETE DE PODCAST" (dx == 0, dz == 0)
                if (dx == 0 && dz == 0) {
                    mpos.set(x, baseY + 1, z);
                    world.setBlockState(mpos, chiseledQuartz, 2);
                    mpos.set(x, baseY + 2, z);
                    world.setBlockState(
                            mpos,
                            ModBlocks.PODCAST_CASSETTE.getDefaultState()
                                    .with(PodcastCassetteBlock.FACING, Direction.SOUTH),
                            3
                    );
                }
            }
        }
    }
}
