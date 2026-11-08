package pruebano1.modid;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final RegistryKey<ItemGroup> SCARLET_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(Prueba_no1.MOD_ID, "scarlet_group")
    );

    public static final ItemGroup SCARLET_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItem.SCARLET_INGOT))
            .displayName(Text.translatable("itemgroup.prueba_no1.scarlet_group"))
            .entries((displayContext, entries) -> {
                // 1. Bloques y Minerales
                entries.add(ModBlocks.SCARLET_ORE);
                entries.add(ModBlocks.DEEPSLATE_SCARLET_ORE);
                entries.add(ModBlocks.SCARLET_BLOCK);

                // 2. Materiales
                entries.add(ModItem.SCARLET_INGOT);

                // 3. Armas y Herramientas
                entries.add(ModItem.SCARLET_SWORD);
                entries.add(ModItem.SCARLET_PICKAXE);
                entries.add(ModItem.SCARLET_AXE);
                entries.add(ModItem.SCARLET_SHOVEL);
                entries.add(ModItem.SCARLET_HOE);

                // 4. Armadura y Alas (Scarty)
                entries.add(ModItem.SCARLET_HELMET);
                entries.add(ModItem.SCARLET_CHESTPLATE);
                entries.add(ModItem.SCARLET_LEGGINGS);
                entries.add(ModItem.SCARLET_BOOTS);
                entries.add(ModItem.SCARTY);

                // 5. Brazaletes y Tótems Especiales
                entries.add(ModItem.SCARLET_BRACELET);
                entries.add(ModItem.GOLDEN_SCARLET_BRACELET);
                entries.add(ModItem.NAZITOTEM);
                entries.add(ModItem.ISRATOTEM);
                entries.add(ModItem.LONTOTEM);

                // 6. Ítems, Bloques y Drops de la Dungeon (Isla Penstein)
                entries.add(ModBlocks.JEFRI_MAILBOX);
                entries.add(ModItem.ARREST_WARRANT);
                entries.add(ModItem.BANANA);
                entries.add(ModItem.MARIJUANA);

                // 7. Huevos Generadores (Jefe y Familia de Niños)
                entries.add(ModItem.PI_DIDI_SPAWN_EGG);
                entries.add(ModItem.YOSTIN_BIBER_SPAWN_EGG);
                entries.add(ModItem.VITOR_JUGO_SPAWN_EGG);
                entries.add(ModItem.FER_OVILLA_SPAWN_EGG);
                entries.add(ModItem.VALENTINA_PRIETA_SPAWN_EGG);
            })
            .build();

    public static void registerItemGroups() {
        Prueba_no1.LOGGER.info("Registrando pestaña creativa para " + Prueba_no1.MOD_ID);
        Registry.register(Registries.ITEM_GROUP, SCARLET_GROUP_KEY, SCARLET_GROUP);
    }
}
