package pruebano1.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.block.Blocks;
import net.minecraft.data.recipe.RecipeExporter;
import net.minecraft.data.recipe.RecipeGenerator;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.ModItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Generador de Recetas (Mesa de crafteo, Horno y Alto Horno).
 * Crea las recetas en data/prueba_no1/recipe/ y sus avances de desbloqueo para el libro de recetas.
 */
public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeGenerator getRecipeGenerator(RegistryWrapper.WrapperLookup registryLookup, RecipeExporter exporter) {
        return new RecipeGenerator(registryLookup, exporter) {
            @Override
            public void generate() {
                // 1. Fundición en Horno y Alto Horno (Smelting & Blasting)
                List<ItemConvertible> scarletOres = List.of(ModBlocks.SCARLET_ORE, ModBlocks.DEEPSLATE_SCARLET_ORE);
                offerSmelting(scarletOres, RecipeCategory.MISC, ModItem.SCARLET_INGOT, 1.0f, 200, "scarlet_ingot");
                offerBlasting(scarletOres, RecipeCategory.MISC, ModItem.SCARLET_INGOT, 1.0f, 100, "scarlet_ingot");

                // 2. Compactación reversible: 9 Lingotes <-> 1 Bloque de Scarlet (crea ambas recetas de golpe)
                offerReversibleCompactingRecipes(
                        RecipeCategory.MISC,
                        ModItem.SCARLET_INGOT,
                        RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.SCARLET_BLOCK
                );

                // 3. Herramientas Scarlet (usando cobbled_deepslate como mango como en tus recetas actuales)
                createShaped(RecipeCategory.COMBAT, ModItem.SCARLET_SWORD)
                        .pattern("#")
                        .pattern("#")
                        .pattern("x")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('x', Blocks.COBBLED_DEEPSLATE)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItem.SCARLET_PICKAXE)
                        .pattern("###")
                        .pattern(" x ")
                        .pattern(" x ")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('x', Blocks.COBBLED_DEEPSLATE)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItem.SCARLET_AXE)
                        .pattern("##")
                        .pattern("#x")
                        .pattern(" x")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('x', Blocks.COBBLED_DEEPSLATE)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItem.SCARLET_SHOVEL)
                        .pattern("#")
                        .pattern("x")
                        .pattern("x")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('x', Blocks.COBBLED_DEEPSLATE)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.TOOLS, ModItem.SCARLET_HOE)
                        .pattern("##")
                        .pattern(" x")
                        .pattern(" x")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('x', Blocks.COBBLED_DEEPSLATE)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                // 4. Armadura Scarlet
                createShaped(RecipeCategory.COMBAT, ModItem.SCARLET_HELMET)
                        .pattern("###")
                        .pattern("# #")
                        .input('#', ModItem.SCARLET_INGOT)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.COMBAT, ModItem.SCARLET_CHESTPLATE)
                        .pattern("# #")
                        .pattern("###")
                        .pattern("###")
                        .input('#', ModItem.SCARLET_INGOT)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.COMBAT, ModItem.SCARLET_LEGGINGS)
                        .pattern("###")
                        .pattern("# #")
                        .pattern("# #")
                        .input('#', ModItem.SCARLET_INGOT)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                createShaped(RecipeCategory.COMBAT, ModItem.SCARLET_BOOTS)
                        .pattern("# #")
                        .pattern("# #")
                        .input('#', ModItem.SCARLET_INGOT)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                // 5. Scarty (Elytra de Escarlata: Membrana de Phantom + Lingote de Escarlata + Diamante)
                createShaped(RecipeCategory.COMBAT, ModItem.SCARTY)
                        .pattern("M#M")
                        .pattern("#D#")
                        .pattern("M M")
                        .input('M', Items.PHANTOM_MEMBRANE)
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('D', Items.DIAMOND)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                // 6. Brazalete de Escarlata (Versión 1: Lingote de Escarlata + Diamante)
                createShaped(RecipeCategory.TOOLS, ModItem.SCARLET_BRACELET)
                        .pattern(" # ")
                        .pattern("# #")
                        .pattern(" D ")
                        .input('#', ModItem.SCARLET_INGOT)
                        .input('D', Items.DIAMOND)
                        .criterion(hasItem(ModItem.SCARLET_INGOT), conditionsFromItem(ModItem.SCARLET_INGOT))
                        .offerTo(exporter);

                // 7. Brazalete de Escarlata Dorado (Versión 2: Brazalete V1 rodeado de Lingotes de Oro)
                createShaped(RecipeCategory.TOOLS, ModItem.GOLDEN_SCARLET_BRACELET)
                        .pattern("GGG")
                        .pattern("GBG")
                        .pattern("GGG")
                        .input('G', Items.GOLD_INGOT)
                        .input('B', ModItem.SCARLET_BRACELET)
                        .criterion(hasItem(ModItem.SCARLET_BRACELET), conditionsFromItem(ModItem.SCARLET_BRACELET))
                        .offerTo(exporter);
            }
        };
    }

    @Override
    public String getName() {
        return "Prueba_no1 Recipes";
    }
}
