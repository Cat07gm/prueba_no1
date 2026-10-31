package pruebano1.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.registry.RegistryWrapper;
import pruebano1.modid.ModBlocks;

import java.util.concurrent.CompletableFuture;

/**
 * Generador de Tablas de Botín (Loot Tables) para bloques.
 * Crea automáticamente los JSON en data/prueba_no1/loot_table/blocks/.
 */
public class ModLootTableProvider extends FabricBlockLootTableProvider {

    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // addDrop hace que el bloque se suelte a sí mismo al picarlo (con condición survives_explosion)
        addDrop(ModBlocks.SCARLET_ORE);
        addDrop(ModBlocks.DEEPSLATE_SCARLET_ORE);
        addDrop(ModBlocks.SCARLET_BLOCK);

        // NOTA: Si en el futuro creas un mineral en bruto (ej. RAW_SCARLET) que funcione con Fortuna y Toque Sedoso,
        // simplemente usarías:
        // addDrop(ModBlocks.SCARLET_ORE, oreDrops(ModBlocks.SCARLET_ORE, ModItem.RAW_SCARLET));
    }
}
