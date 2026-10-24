package pruebano1.modid;

import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModToolMaterials {

    public static final TagKey<Item> SCARLET_REPAIR = TagKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_repair"));

    //definir el material scarlet
    public static final ToolMaterial SCARLET = new ToolMaterial(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, // tier del diamante
            1561, // durabilidad
            8.0F, // velocidad de minería
            3.5F, // daño base
            22,   // nivel de encantado
            SCARLET_REPAIR
    );
}