package pruebano1.modid;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.item.equipment.EquipmentType;
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
    public static final Item SCARLET_SWORD = new SwordItem(ModToolMaterials.SCARLET, 3, -2.4F, new Item.Settings().registryKey(SCARLET_SWORD_KEY));
    public static final Item SCARLET_PICKAXE = new PickaxeItem(ModToolMaterials.SCARLET, 1, -2.8F, new Item.Settings().registryKey(SCARLET_PICKAXE_KEY));
    public static final Item SCARLET_AXE = new AxeItem(ModToolMaterials.SCARLET, 5.0F, -3.0F, new Item.Settings().registryKey(SCARLET_AXE_KEY));
    public static final Item SCARLET_SHOVEL = new ShovelItem(ModToolMaterials.SCARLET, 1.5F, -3.0F, new Item.Settings().registryKey(SCARLET_SHOVEL_KEY));
    public static final Item SCARLET_HOE = new HoeItem(ModToolMaterials.SCARLET, -3.0F, 0.0F, new Item.Settings().registryKey(SCARLET_HOE_KEY));

    // --- ARMADURA SCARLET: 1. Llaves de Registro ---
    public static final RegistryKey<Item> SCARLET_HELMET_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_helmet"));
    public static final RegistryKey<Item> SCARLET_CHESTPLATE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_chestplate"));
    public static final RegistryKey<Item> SCARLET_LEGGINGS_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_leggings"));
    public static final RegistryKey<Item> SCARLET_BOOTS_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_boots"));

    // --- ARMADURA SCARLET: 2. Creación de las 4 piezas ---
    public static final Item SCARLET_HELMET = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.HELMET,
            new Item.Settings().registryKey(SCARLET_HELMET_KEY)
    );
    public static final Item SCARLET_CHESTPLATE = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.CHESTPLATE,
            new Item.Settings().registryKey(SCARLET_CHESTPLATE_KEY)
    );
    public static final Item SCARLET_LEGGINGS = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.LEGGINGS,
            new Item.Settings().registryKey(SCARLET_LEGGINGS_KEY)
    );
    public static final Item SCARLET_BOOTS = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.BOOTS,
            new Item.Settings().registryKey(SCARLET_BOOTS_KEY)
    );

    // 4. Método ayudante de registro
    private static Item registerItem(RegistryKey<Item> key, Item item) {
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        Prueba_no1.LOGGER.info("Registrando ítems para " + Prueba_no1.MOD_ID);

        // Registrar lingote y herramientas
        registerItem(SCARLET_INGOT_KEY, SCARLET_INGOT);
        registerItem(SCARLET_SWORD_KEY, SCARLET_SWORD);
        registerItem(SCARLET_PICKAXE_KEY, SCARLET_PICKAXE);
        registerItem(SCARLET_AXE_KEY, SCARLET_AXE);
        registerItem(SCARLET_SHOVEL_KEY, SCARLET_SHOVEL);
        registerItem(SCARLET_HOE_KEY, SCARLET_HOE);

        // --- ARMADURA SCARLET: 3. Registrar en la base de datos ---
        registerItem(SCARLET_HELMET_KEY, SCARLET_HELMET);
        registerItem(SCARLET_CHESTPLATE_KEY, SCARLET_CHESTPLATE);
        registerItem(SCARLET_LEGGINGS_KEY, SCARLET_LEGGINGS);
        registerItem(SCARLET_BOOTS_KEY, SCARLET_BOOTS);

        // Pestaña Ingredientes
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SCARLET_INGOT);
        });

        // Pestaña Combate (Espada + Set completo de Armadura)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SCARLET_SWORD);
            entries.add(SCARLET_HELMET);
            entries.add(SCARLET_CHESTPLATE);
            entries.add(SCARLET_LEGGINGS);
            entries.add(SCARLET_BOOTS);
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