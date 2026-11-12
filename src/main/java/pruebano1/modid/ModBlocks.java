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
import pruebano1.modid.block.CasinoChairBlock;
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

    //REGISTERS
    public static void registerModBlocks() {
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
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(CASINO_LIGHT);
            entries.add(DRAFTEA_LIGHT);
        });
    }
}
