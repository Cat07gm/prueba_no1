package pruebano1.modid.datagen;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.BlockStateModelGenerator;
import net.minecraft.client.data.ItemModelGenerator;
import net.minecraft.client.data.Models;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.ModItem;

/**
 * Generador de Modelos (Blockstates, Modelos de Bloques, Modelos de Ítems y Definiciones de Ítems 1.21.4).
 * Cada registro aquí reemplaza entre 2 y 3 archivos JSON manuales en assets/prueba_no1/.
 */
public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // Bloques de cubo simple (misma textura en las 6 caras):
        // Genera automáticamente: blockstates/<bloque>.json, models/block/<bloque>.json e items/<bloque>.json
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.SCARLET_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.DEEPSLATE_SCARLET_ORE);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.SCARLET_BLOCK);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        // Ítems normales e ingredientes (Models.GENERATED = item/generated)
        itemModelGenerator.register(ModItem.SCARLET_INGOT, Models.GENERATED);

        // Herramientas y armas (Models.HANDHELD = item/handheld, para que se sujeten bien en 3ra persona)
        itemModelGenerator.register(ModItem.SCARLET_SWORD, Models.HANDHELD);
        itemModelGenerator.register(ModItem.SCARLET_PICKAXE, Models.HANDHELD);
        itemModelGenerator.register(ModItem.SCARLET_AXE, Models.HANDHELD);
        itemModelGenerator.register(ModItem.SCARLET_SHOVEL, Models.HANDHELD);
        itemModelGenerator.register(ModItem.SCARLET_HOE, Models.HANDHELD);

        // Piezas de armadura en el inventario
        itemModelGenerator.register(ModItem.SCARLET_HELMET, Models.GENERATED);
        itemModelGenerator.register(ModItem.SCARLET_CHESTPLATE, Models.GENERATED);
        itemModelGenerator.register(ModItem.SCARLET_LEGGINGS, Models.GENERATED);
        itemModelGenerator.register(ModItem.SCARLET_BOOTS, Models.GENERATED);
    }
}
