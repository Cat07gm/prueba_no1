package pruebano1.modid;

import net.minecraft.item.HoeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.Item;
import net.minecraft.item.AxeItem;
import net.minecraft.item.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItem {

    //llaves para lingote escarlata
    public static final RegistryKey<Item> SCARLET_INGOT_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_ingot"));
    //creacion lingote
    public static final Item SCARLET_INGOT = new Item(new Item.Settings().registryKey(SCARLET_INGOT_KEY));

    private static Item registerItem(RegistryKey<Item> key, Item item) {
        return Registry.register(Registries.ITEM, key, item);
    }
    //llaves para espada
    public static final RegistryKey<Item> SCARLET_SWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_sword"));
    //Creación espada
    public static final Item SCARLET_SWORD = new SwordItem(
            ModToolMaterials.SCARLET,
            3,
            -2.4F,
            new Item.Settings().registryKey(SCARLET_SWORD_KEY)
    );
    //llave para pico
    public static final RegistryKey<Item> SCARLET_PICKAXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_pickaxe"));
    //definicion y creacion del pico
    public static final Item SCARLET_PICKAXE = new PickaxeItem(
            ModToolMaterials.SCARLET,
            1,
            -2.8F,
            new Item.Settings().registryKey(SCARLET_PICKAXE_KEY)
    );
    //llave para hacha
    public static final RegistryKey<Item> SCARLET_AXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_axe"));
    //definicion y creacion del hacha
    public static final Item SCARLET_AXE = new AxeItem(
            ModToolMaterials.SCARLET,
            5.0F,
            -3.0F,
            new Item.Settings().registryKey(SCARLET_AXE_KEY)
    );
    //llave para pala
    public static final RegistryKey<Item> SCARLET_SHOVEL_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_shovel"));
    //definicion y creacion de pala
    public static final Item SCARLET_SHOVEL = new ShovelItem(
            ModToolMaterials.SCARLET,
            1.5F,
            -3.0F,
            new Item.Settings().registryKey(SCARLET_SHOVEL_KEY)
    );
    //llave para azada
    public static final RegistryKey<Item> SCARLET_HOE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_hoe"));
    //defincion y creacion de azada
    public static final Item SCARLT_HOE = new HoeItem(
            ModToolMaterials.SCARLET,
            -2.0F,
            0.0F,
            new Item.Settings().registryKey(SCARLET_HOE_KEY)
    );
    //registro de items
    public static void registerModItems() {
        Prueba_no1.LOGGER.info("Registrando ítems para " + Prueba_no1.MOD_ID);

        registerItem(SCARLET_INGOT_KEY, SCARLET_INGOT);
        registerItem(SCARLET_SWORD_KEY, SCARLET_SWORD);
        registerItem(SCARLET_PICKAXE_KEY, SCARLET_PICKAXE);
        registerItem(SCARLET_AXE_KEY, SCARLET_AXE);
        registerItem(SCARLET_SHOVEL_KEY, SCARLET_SHOVEL);
        registerItem(SCARLET_HOE_KEY, SCARLT_HOE);

        //inyectar al inventario del creativo
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SCARLET_INGOT);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SCARLET_SWORD);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLET_PICKAXE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLET_AXE);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLET_SHOVEL);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLT_HOE);
        });
    }
}
