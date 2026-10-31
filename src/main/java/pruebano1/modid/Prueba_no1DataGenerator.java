package pruebano1.modid;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import pruebano1.modid.datagen.ModBlockTagProvider;
import pruebano1.modid.datagen.ModItemTagProvider;
import pruebano1.modid.datagen.ModLootTableProvider;
import pruebano1.modid.datagen.ModModelProvider;
import pruebano1.modid.datagen.ModRecipeProvider;

public class Prueba_no1DataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		// Registramos cada proveedor (Provider) para que genere los JSON automáticamente al correr runDatagen
		pack.addProvider(ModBlockTagProvider::new);
		pack.addProvider(ModItemTagProvider::new);
		pack.addProvider(ModLootTableProvider::new);
		pack.addProvider(ModModelProvider::new);
		pack.addProvider(ModRecipeProvider::new);
	}
}
