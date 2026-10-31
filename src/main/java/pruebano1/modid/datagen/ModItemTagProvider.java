package pruebano1.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import pruebano1.modid.ModItem;
import pruebano1.modid.ModToolMaterials;

import java.util.concurrent.CompletableFuture;

/**
 * Generador de Tags de Ítems (reparación en yunque, encantamientos de herramientas y armaduras).
 * Crea los archivos JSON en data/minecraft/tags/item/ y data/prueba_no1/tags/item/.
 */
public class ModItemTagProvider extends FabricTagProvider.ItemTagProvider {

    public ModItemTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        // Tag personalizado para reparar herramientas y armaduras Scarlet en el yunque
        getOrCreateTagBuilder(ModToolMaterials.SCARLET_REPAIR)
                .add(ModItem.SCARLET_INGOT);

        // Tags de herramientas (necesarios en 1.21+ para que reciban sus encantamientos correspondientes)
        getOrCreateTagBuilder(ItemTags.SWORDS).add(ModItem.SCARLET_SWORD);
        getOrCreateTagBuilder(ItemTags.PICKAXES).add(ModItem.SCARLET_PICKAXE);
        getOrCreateTagBuilder(ItemTags.AXES).add(ModItem.SCARLET_AXE);
        getOrCreateTagBuilder(ItemTags.SHOVELS).add(ModItem.SCARLET_SHOVEL);
        getOrCreateTagBuilder(ItemTags.HOES).add(ModItem.SCARLET_HOE);

        // Tags de armaduras (necesarios para encantamientos como Protección, Caída de Pluma, etc.)
        getOrCreateTagBuilder(ItemTags.HEAD_ARMOR).add(ModItem.SCARLET_HELMET);
        getOrCreateTagBuilder(ItemTags.CHEST_ARMOR).add(ModItem.SCARLET_CHESTPLATE);
        getOrCreateTagBuilder(ItemTags.LEG_ARMOR).add(ModItem.SCARLET_LEGGINGS);
        getOrCreateTagBuilder(ItemTags.FOOT_ARMOR).add(ModItem.SCARLET_BOOTS);
    }
}
