package pruebano1.modid;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.util.shape.VoxelShapes;
import pruebano1.modid.block.CasinoBarStoolBlock;
import pruebano1.modid.block.CasinoChairBlock;
import pruebano1.modid.block.CasinoDecorBlock;
import pruebano1.modid.block.CasinoSofaBlock;
import pruebano1.modid.block.MesaDrafteaDiazBlock;
import pruebano1.modid.block.NeonSignBlock;
import pruebano1.modid.block.DjComponentBlock;
import pruebano1.modid.block.DjControllerBlock;
import pruebano1.modid.block.DrafteaBlock;
import pruebano1.modid.block.JefriMailboxBlock;

public class ModBlocks {

    //Llaves para Scarlet ore Stone
    public static final RegistryKey<Block> SCARLET_ORE_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Prueba_no1.MOD_ID, "scarlet_ore"));
    public static final RegistryKey<Item> SCARLET_ORE_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_ore"));

    //definir el bloque
    public static final Block SCARLET_ORE = new Block(AbstractBlock.Settings.create()
            .registryKey(SCARLET_ORE_KEY)
            .requiresTool()
            .strength(3.0f, 3.0f));

    //Dar de alta un nuevo bloque
    private static Block registerBlock(RegistryKey<Block> blockKey, RegistryKey<Item> itemKey, Block block) {
        Registry.register(Registries.BLOCK, blockKey, block);
        Registry.register(Registries.ITEM, itemKey, new BlockItem(block, new Item.Settings().registryKey(itemKey)));
        return block;
    }

    //Llaves para la deepslate scarlet ore
    public static final RegistryKey<Block> DEEPSLATE_SCARLET_ORE_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Prueba_no1.MOD_ID, "deepslate_scarlet_ore"));
    public static final RegistryKey<Item> DEEPSLATE_SCARLET_ORE_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "deepslate_scarlet_ore"));

    public static final Block DEEPSLATE_SCARLET_ORE = new Block(AbstractBlock.Settings.create()
            .registryKey(DEEPSLATE_SCARLET_ORE_KEY)
            .requiresTool()
            .strength(4.5f, 3.0f));

    //Llaves para el bloque de Scarlet Puro
    public static final RegistryKey<Block> SCARLET_BLOCK_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Prueba_no1.MOD_ID, "scarlet_block"));
    public static final RegistryKey<Item> SCARLET_BLOCK_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_block"));

    public static final Block SCARLET_BLOCK = new Block(AbstractBlock.Settings.create()
            .registryKey(SCARLET_BLOCK_KEY)
            .requiresTool()
            .strength(5.0f, 6.0f));

    // BUZÓN DE JEFRI PENSTEIN (Bloque indestructible de invocación en la Isla Penstein)
    public static final RegistryKey<Block> JEFRI_MAILBOX_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "jefri_mailbox")
    );
    public static final RegistryKey<Item> JEFRI_MAILBOX_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "jefri_mailbox")
    );

    public static final Block JEFRI_MAILBOX = new JefriMailboxBlock(AbstractBlock.Settings.create()
            .registryKey(JEFRI_MAILBOX_KEY)
            .strength(-1.0f, 3600000.0f) // Indestructible como la Bedrock
            .dropsNothing()
            .nonOpaque()
            .sounds(BlockSoundGroup.LANTERN));

    // DRAFTEA (Máquina tragamonedas de 2 bloques de alto que funciona con Tony Coins)
    public static final RegistryKey<Block> DRAFTEA_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "draftea")
    );
    public static final RegistryKey<Item> DRAFTEA_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "draftea")
    );

    // Indestructible en supervivencia como la Bedrock (en creativo sí se rompe). Solo se obtiene en creativo o con /give.
    public static final Block DRAFTEA = new DrafteaBlock(AbstractBlock.Settings.create()
            .registryKey(DRAFTEA_KEY)
            .strength(-1.0f, 3600000.0f)
            .dropsNothing()
            .nonOpaque()
            .luminance(state -> 6) // La pantalla brilla un poco
            .pistonBehavior(PistonBehavior.BLOCK)
            .sounds(BlockSoundGroup.METAL));

    // LUZ DE CASINO (Alterna entre morado, rojo, amarillo y verde con destello llamativo)
    public static final RegistryKey<Block> CASINO_LIGHT_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "casino_light")
    );
    public static final RegistryKey<Item> CASINO_LIGHT_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "casino_light")
    );

    public static final Block CASINO_LIGHT = new Block(AbstractBlock.Settings.create()
            .registryKey(CASINO_LIGHT_KEY)
            .mapColor(MapColor.PURPLE)
            .strength(0.8f, 6.0f)
            .luminance(state -> 15)
            .emissiveLighting(Blocks::always)
            .sounds(BlockSoundGroup.LANTERN));

    // LUZ DRAFTEA (Iluminación fija en el tono morado característico de las máquinas Draftea)
    public static final RegistryKey<Block> DRAFTEA_LIGHT_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "draftea_light")
    );
    public static final RegistryKey<Item> DRAFTEA_LIGHT_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "draftea_light")
    );

    public static final Block DRAFTEA_LIGHT = new Block(AbstractBlock.Settings.create()
            .registryKey(DRAFTEA_LIGHT_KEY)
            .mapColor(MapColor.PURPLE)
            .strength(0.8f, 6.0f)
            .luminance(state -> 15)
            .emissiveLighting(Blocks::always)
            .sounds(BlockSoundGroup.LANTERN));

    // SILLA NEGRA DE CASINO (Banquito giratorio acolchado para sentarse)
    public static final RegistryKey<Block> CASINO_CHAIR_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "casino_chair")
    );
    public static final RegistryKey<Item> CASINO_CHAIR_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "casino_chair")
    );

    public static final Block CASINO_CHAIR = new CasinoChairBlock(AbstractBlock.Settings.create()
            .registryKey(CASINO_CHAIR_KEY)
            .mapColor(MapColor.BLACK)
            .strength(1.5f, 4.0f)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.DESTROY)
            .sounds(BlockSoundGroup.METAL));

    // CONTROLADOR DJ 1: TORNAMESAS (Decorativo)
    public static final RegistryKey<Block> DJ_TURNTABLE_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "dj_turntable")
    );
    public static final RegistryKey<Item> DJ_TURNTABLE_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "dj_turntable")
    );

    public static final Block DJ_TURNTABLE = new DjComponentBlock(6.5, AbstractBlock.Settings.create()
            .registryKey(DJ_TURNTABLE_KEY)
            .mapColor(MapColor.BLACK)
            .strength(1.8f, 5.0f)
            .nonOpaque()
            .luminance(state -> 6)
            .pistonBehavior(PistonBehavior.BLOCK)
            .sounds(BlockSoundGroup.METAL));

    // CONTROLADOR DJ 2: MEZCLADORA DE AUDIO (Decorativo)
    public static final RegistryKey<Block> DJ_MIXER_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "dj_mixer")
    );
    public static final RegistryKey<Item> DJ_MIXER_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "dj_mixer")
    );

    public static final Block DJ_MIXER = new DjComponentBlock(7.2, AbstractBlock.Settings.create()
            .registryKey(DJ_MIXER_KEY)
            .mapColor(MapColor.BLACK)
            .strength(1.8f, 5.0f)
            .nonOpaque()
            .luminance(state -> 7)
            .pistonBehavior(PistonBehavior.BLOCK)
            .sounds(BlockSoundGroup.METAL));

    // CONTROLADOR DJ 3: CONSOLA PRINCIPAL (Con función interactiva próximamente)
    public static final RegistryKey<Block> DJ_CONTROLLER_KEY = RegistryKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(Prueba_no1.MOD_ID, "dj_controller")
    );
    public static final RegistryKey<Item> DJ_CONTROLLER_ITEM_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "dj_controller")
    );

    public static final Block DJ_CONTROLLER = new DjControllerBlock(AbstractBlock.Settings.create()
            .registryKey(DJ_CONTROLLER_KEY)
            .mapColor(MapColor.BLACK)
            .strength(2.0f, 6.0f)
            .nonOpaque()
            .luminance(state -> 10)
            .pistonBehavior(PistonBehavior.BLOCK)
            .sounds(BlockSoundGroup.METAL));

    // =====================================================================
    // DECORACIÓN Y JUEGOS NUEVOS DEL CASINO DRAFTEA
    // =====================================================================

    private static RegistryKey<Block> blockKey(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(Prueba_no1.MOD_ID, name));
    }

    private static RegistryKey<Item> itemKey(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, name));
    }

    // MESA DRAFTEA DIAZ (mesa de apuestas de cartas 5x3: se apuestan lingotes y gemas). Solo creativo/comandos.
    public static final RegistryKey<Block> MESA_DRAFTEA_DIAZ_KEY = blockKey("mesa_draftea_diaz");
    public static final Block MESA_DRAFTEA_DIAZ = new MesaDrafteaDiazBlock(AbstractBlock.Settings.create()
            .registryKey(MESA_DRAFTEA_DIAZ_KEY)
            .mapColor(MapColor.PURPLE)
            .strength(2.5f, 1200.0f) // Muy resistente a explosiones para que no se parta en pedazos
            .nonOpaque()
            .pistonBehavior(PistonBehavior.BLOCK)
            .sounds(BlockSoundGroup.WOOD));

    // SOFÁ MODULAR (se une con los de al lado: puntas, piezas centrales y esquinas)
    public static final RegistryKey<Block> CASINO_SOFA_KEY = blockKey("casino_sofa");
    public static final Block CASINO_SOFA = new CasinoSofaBlock(AbstractBlock.Settings.create()
            .registryKey(CASINO_SOFA_KEY)
            .mapColor(MapColor.BLACK)
            .strength(1.0f, 3.0f)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.DESTROY)
            .sounds(BlockSoundGroup.WOOL));

    // MESA DE CENTRO (baja, para las salas con sofás)
    public static final RegistryKey<Block> CASINO_COFFEE_TABLE_KEY = blockKey("casino_coffee_table");
    public static final Block CASINO_COFFEE_TABLE = new CasinoDecorBlock(
            Block.createCuboidShape(0, 0, 0, 16, 8, 16),
            AbstractBlock.Settings.create()
                    .registryKey(CASINO_COFFEE_TABLE_KEY)
                    .mapColor(MapColor.WHITE)
                    .strength(1.5f, 4.0f)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)
                    .sounds(BlockSoundGroup.STONE));

    // MESA ALTA DE COCTEL (pie cromado)
    public static final RegistryKey<Block> CASINO_COCKTAIL_TABLE_KEY = blockKey("casino_cocktail_table");
    public static final Block CASINO_COCKTAIL_TABLE = new CasinoDecorBlock(
            VoxelShapes.union(
                    Block.createCuboidShape(3, 0, 3, 13, 1, 13),
                    Block.createCuboidShape(7, 1, 7, 9, 14, 9),
                    Block.createCuboidShape(1, 14, 1, 15, 16, 15)),
            AbstractBlock.Settings.create()
                    .registryKey(CASINO_COCKTAIL_TABLE_KEY)
                    .mapColor(MapColor.WHITE)
                    .strength(1.5f, 4.0f)
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)
                    .sounds(BlockSoundGroup.METAL));

    // SILLA DE BAR ALTA
    public static final RegistryKey<Block> CASINO_BAR_STOOL_KEY = blockKey("casino_bar_stool");
    public static final Block CASINO_BAR_STOOL = new CasinoBarStoolBlock(AbstractBlock.Settings.create()
            .registryKey(CASINO_BAR_STOOL_KEY)
            .mapColor(MapColor.BLACK)
            .strength(1.5f, 4.0f)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.DESTROY)
            .sounds(BlockSoundGroup.METAL));

    // PLANTAS EN MACETA MODERNA
    public static final RegistryKey<Block> CASINO_PALM_POT_KEY = blockKey("casino_palm_pot");
    public static final Block CASINO_PALM_POT = createPlant(CASINO_PALM_POT_KEY);
    public static final RegistryKey<Block> CASINO_MONSTERA_POT_KEY = blockKey("casino_monstera_pot");
    public static final Block CASINO_MONSTERA_POT = createPlant(CASINO_MONSTERA_POT_KEY);

    private static Block createPlant(RegistryKey<Block> key) {
        return new CasinoDecorBlock(Block.createCuboidShape(3, 0, 3, 13, 16, 13), AbstractBlock.Settings.create()
                .registryKey(key)
                .mapColor(MapColor.BLACK)
                .strength(0.8f, 2.0f)
                .nonOpaque()
                .pistonBehavior(PistonBehavior.DESTROY)
                .sounds(BlockSoundGroup.DECORATED_POT));
    }

    // CUADROS / LETREROS NEÓN DE PARED
    public static final RegistryKey<Block> NEON_SIGN_CARDS_KEY = blockKey("neon_sign_cards");
    public static final Block NEON_SIGN_CARDS = createNeonSign(NEON_SIGN_CARDS_KEY);
    public static final RegistryKey<Block> NEON_SIGN_CHIP_KEY = blockKey("neon_sign_chip");
    public static final Block NEON_SIGN_CHIP = createNeonSign(NEON_SIGN_CHIP_KEY);
    public static final RegistryKey<Block> NEON_SIGN_DICE_KEY = blockKey("neon_sign_dice");
    public static final Block NEON_SIGN_DICE = createNeonSign(NEON_SIGN_DICE_KEY);
    public static final RegistryKey<Block> NEON_SIGN_777_KEY = blockKey("neon_sign_777");
    public static final Block NEON_SIGN_777 = createNeonSign(NEON_SIGN_777_KEY);

    private static Block createNeonSign(RegistryKey<Block> key) {
        return new NeonSignBlock(AbstractBlock.Settings.create()
                .registryKey(key)
                .mapColor(MapColor.PURPLE)
                .strength(0.8f, 2.0f)
                .nonOpaque()
                .noCollision()
                .luminance(state -> 10)
                .pistonBehavior(PistonBehavior.DESTROY)
                .sounds(BlockSoundGroup.GLASS));
    }

    /** Todos los bloques nuevos de decoración del casino, en el orden en que salen en el inventario creativo. */
    public static final Block[] CASINO_DECOR = {
            CASINO_SOFA, CASINO_COFFEE_TABLE, CASINO_COCKTAIL_TABLE, CASINO_BAR_STOOL,
            CASINO_PALM_POT, CASINO_MONSTERA_POT,
            NEON_SIGN_CARDS, NEON_SIGN_CHIP, NEON_SIGN_DICE, NEON_SIGN_777
    };

    private static void registerSimple(Block block, RegistryKey<Block> key) {
        registerBlock(key, itemKey(key.getValue().getPath()), block);
    }

    //REGISTERS
    public static void registerModBlocks() {
        registerSimple(MESA_DRAFTEA_DIAZ, MESA_DRAFTEA_DIAZ_KEY);
        registerSimple(CASINO_SOFA, CASINO_SOFA_KEY);
        registerSimple(CASINO_COFFEE_TABLE, CASINO_COFFEE_TABLE_KEY);
        registerSimple(CASINO_COCKTAIL_TABLE, CASINO_COCKTAIL_TABLE_KEY);
        registerSimple(CASINO_BAR_STOOL, CASINO_BAR_STOOL_KEY);
        registerSimple(CASINO_PALM_POT, CASINO_PALM_POT_KEY);
        registerSimple(CASINO_MONSTERA_POT, CASINO_MONSTERA_POT_KEY);
        registerSimple(NEON_SIGN_CARDS, NEON_SIGN_CARDS_KEY);
        registerSimple(NEON_SIGN_CHIP, NEON_SIGN_CHIP_KEY);
        registerSimple(NEON_SIGN_DICE, NEON_SIGN_DICE_KEY);
        registerSimple(NEON_SIGN_777, NEON_SIGN_777_KEY);

        Prueba_no1.LOGGER.info("Registrando bloques para " + Prueba_no1.MOD_ID);

        registerBlock(SCARLET_ORE_KEY, SCARLET_ORE_ITEM_KEY, SCARLET_ORE);
        registerBlock(DEEPSLATE_SCARLET_ORE_KEY, DEEPSLATE_SCARLET_ORE_ITEM_KEY, DEEPSLATE_SCARLET_ORE);
        registerBlock(SCARLET_BLOCK_KEY, SCARLET_BLOCK_ITEM_KEY, SCARLET_BLOCK);
        registerBlock(JEFRI_MAILBOX_KEY, JEFRI_MAILBOX_ITEM_KEY, JEFRI_MAILBOX);
        registerBlock(DRAFTEA_KEY, DRAFTEA_ITEM_KEY, DRAFTEA);
        registerBlock(CASINO_LIGHT_KEY, CASINO_LIGHT_ITEM_KEY, CASINO_LIGHT);
        registerBlock(DRAFTEA_LIGHT_KEY, DRAFTEA_LIGHT_ITEM_KEY, DRAFTEA_LIGHT);
        registerBlock(CASINO_CHAIR_KEY, CASINO_CHAIR_ITEM_KEY, CASINO_CHAIR);
        registerBlock(DJ_TURNTABLE_KEY, DJ_TURNTABLE_ITEM_KEY, DJ_TURNTABLE);
        registerBlock(DJ_MIXER_KEY, DJ_MIXER_ITEM_KEY, DJ_MIXER);
        registerBlock(DJ_CONTROLLER_KEY, DJ_CONTROLLER_ITEM_KEY, DJ_CONTROLLER);

        //Inyectar al inventario del creativo
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SCARLET_ORE);
            entries.add(DEEPSLATE_SCARLET_ORE);
            entries.add(SCARLET_BLOCK);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(JEFRI_MAILBOX);
            entries.add(DRAFTEA);
            entries.add(CASINO_LIGHT);
            entries.add(DRAFTEA_LIGHT);
            entries.add(CASINO_CHAIR);
            entries.add(DJ_TURNTABLE);
            entries.add(DJ_MIXER);
            entries.add(DJ_CONTROLLER);
            entries.add(MESA_DRAFTEA_DIAZ);
            for (Block decor : CASINO_DECOR) {
                entries.add(decor);
            }
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(CASINO_LIGHT);
            entries.add(DRAFTEA_LIGHT);
        });
    }
}
