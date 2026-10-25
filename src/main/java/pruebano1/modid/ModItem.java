package pruebano1.modid;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItem {

    // 1. Llave y creación del Lingote
    public static final RegistryKey<Item> SCARLET_INGOT_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_ingot"));
    public static final Item SCARLET_INGOT = new Item(new Item.Settings().registryKey(SCARLET_INGOT_KEY));

    // 2. Llaves para las Herramientas y Armas
    public static final RegistryKey<Item> SCARLET_SWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_sword"));
    public static final RegistryKey<Item> SCARLET_PICKAXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_pickaxe"));
    public static final RegistryKey<Item> SCARLET_AXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_axe"));
    public static final RegistryKey<Item> SCARLET_SHOVEL_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_shovel"));
    public static final RegistryKey<Item> SCARLET_HOE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_hoe"));

    // 3. Creación de las Herramientas
    public static final Item SCARLET_SWORD = new SwordItem(
            ModToolMaterials.SCARLET,
            3,
            -2.4F,
            new Item.Settings().registryKey(SCARLET_SWORD_KEY)
    );

    public static final Item SCARLET_PICKAXE = new PickaxeItem(
            ModToolMaterials.SCARLET,
            1,
            -2.8F,
            new Item.Settings().registryKey(SCARLET_PICKAXE_KEY)
    );

    public static final Item SCARLET_AXE = new AxeItem(
            ModToolMaterials.SCARLET,
            5.0F,
            -3.0F,
            new Item.Settings().registryKey(SCARLET_AXE_KEY)
    );

    public static final Item SCARLET_SHOVEL = new ShovelItem(
            ModToolMaterials.SCARLET,
            1.5F,
            -3.0F,
            new Item.Settings().registryKey(SCARLET_SHOVEL_KEY)
    );

    public static final Item SCARLET_HOE = new HoeItem(
            ModToolMaterials.SCARLET,
            -3.0F,
            0.0F,
            // CORRECCIÓN: Aquí tenía SCARLET_SHOVEL_KEY por error
            new Item.Settings().registryKey(SCARLET_HOE_KEY)
    );

    // 4. Método ayudante de registro
    private static Item registerItem(RegistryKey<Item> key, Item item) {
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        Prueba_no1.LOGGER.info("Registrando ítems para " + Prueba_no1.MOD_ID);

        // Registrar todos los ítems en la base de datos
        registerItem(SCARLET_INGOT_KEY, SCARLET_INGOT);
        registerItem(SCARLET_SWORD_KEY, SCARLET_SWORD);
        registerItem(SCARLET_PICKAXE_KEY, SCARLET_PICKAXE);
        registerItem(SCARLET_AXE_KEY, SCARLET_AXE);
        registerItem(SCARLET_SHOVEL_KEY, SCARLET_SHOVEL);
        registerItem(SCARLET_HOE_KEY, SCARLET_HOE);

        // Pestaña Ingredientes
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SCARLET_INGOT);
        });

        // Pestaña Combate
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SCARLET_SWORD);
        });

        // Pestaña Herramientas
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLET_PICKAXE);
            entries.add(SCARLET_AXE);
            entries.add(SCARLET_SHOVEL);
            entries.add(SCARLET_HOE);
        });
    }
}