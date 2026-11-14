package pruebano1.modid.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import pruebano1.modid.ModBlocks;

import java.util.concurrent.CompletableFuture;

/**
 * Generador de Tags de Bloques (con qué herramienta se pica y qué nivel de pico requiere).
 * Crea los archivos JSON en data/minecraft/tags/block/.
 */
public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        // Bloques que se pican rápido con el pico
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.SCARLET_ORE)
                .add(ModBlocks.DEEPSLATE_SCARLET_ORE)
                .add(ModBlocks.SCARLET_BLOCK)
                .add(ModBlocks.CASINO_LIGHT)
                .add(ModBlocks.DRAFTEA_LIGHT)
                .add(ModBlocks.CASINO_CHAIR)
                .add(ModBlocks.DJ_TURNTABLE)
                .add(ModBlocks.DJ_MIXER)
                .add(ModBlocks.DJ_CONTROLLER)
                .add(ModBlocks.CASINO_COFFEE_TABLE)
                .add(ModBlocks.CASINO_COCKTAIL_TABLE)
                .add(ModBlocks.CASINO_BAR_STOOL)
                .add(ModBlocks.NEON_SIGN_CARDS)
                .add(ModBlocks.NEON_SIGN_CHIP)
                .add(ModBlocks.NEON_SIGN_DICE)
                .add(ModBlocks.NEON_SIGN_777);

        // La Mesa Draftea Diaz es de madera: se rompe rápido con hacha
        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.MESA_DRAFTEA_DIAZ);

        // Bloques que requieren al menos pico de hierro (o puedes usar NEEDS_DIAMOND_TOOL)
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.SCARLET_ORE)
                .add(ModBlocks.DEEPSLATE_SCARLET_ORE)
                .add(ModBlocks.SCARLET_BLOCK);

        // Bloques que el Wither y el Ender Dragon no pueden destruir (como la Bedrock)
        getOrCreateTagBuilder(BlockTags.WITHER_IMMUNE)
                .add(ModBlocks.DRAFTEA);
        getOrCreateTagBuilder(BlockTags.DRAGON_IMMUNE)
                .add(ModBlocks.DRAFTEA);
    }
}
