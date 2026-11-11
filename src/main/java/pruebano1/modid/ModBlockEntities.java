package pruebano1.modid;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import pruebano1.modid.block.DrafteaBlockEntity;

public class ModBlockEntities {

    // Pantalla de la Draftea (guarda los ítems de los rodillos)
    public static final BlockEntityType<DrafteaBlockEntity> DRAFTEA = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "draftea"),
            FabricBlockEntityTypeBuilder.create(DrafteaBlockEntity::new, ModBlocks.DRAFTEA).build()
    );

    public static void registerModBlockEntities() {
        Prueba_no1.LOGGER.info("Registrando entidades de bloque para " + Prueba_no1.MOD_ID);
    }
}
