package pruebano1.modid.client.encyclopedia;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.ModItem;

import java.util.function.Supplier;

/**
 * Navigation categories shown in the left sidebar of the encyclopedia.
 */
public enum EncyclopediaCategory {
    ALL("TODOS", "Todos los Elementos", () -> new ItemStack(Items.KNOWLEDGE_BOOK), false),
    RESOURCES("RECURSOS", "Recursos y Minerales", () -> new ItemStack(ModItem.SCARLET_INGOT), false),
    TOOLS("HERRAMIENTAS", "Armas y Herramientas", () -> new ItemStack(ModItem.SCARLET_SWORD), false),
    ARMOR("ARMADURA", "Armadura y Alas", () -> new ItemStack(ModItem.SCARLET_CHESTPLATE), false),
    ACCESSORIES("ACCESORIOS", "Accesorios", () -> new ItemStack(ModItem.GOLDEN_SCARLET_BRACELET), false),
    TOTEMS("TÓTEMS", "Tótems Especiales", () -> new ItemStack(ModItem.LONTOTEM), false),
    BLOCKS("BLOQUES", "Bloques del Mod", () -> new ItemStack(ModBlocks.SCARLET_BLOCK), false),
    ENTITIES("ENTIDADES", "Entidades 3D del Mod", () -> new ItemStack(Items.CREEPER_HEAD), false),
    FOOD("COMIDA", "Comida y Consumibles", () -> new ItemStack(ModItem.BANANA), false),
    MISC("MISC", "Misceláneos y Dungeon", () -> new ItemStack(ModItem.TONY_COIN), false),
    STRUCTURES("ESTRUCTURAS", "Apartado de Estructuras", () -> new ItemStack(Items.FILLED_MAP), true);

    private final String shortLabel;
    private final String headerTitle;
    private final Supplier<ItemStack> iconSupplier;
    private final boolean separateSection;
    private ItemStack cachedIcon;

    EncyclopediaCategory(String shortLabel, String headerTitle, Supplier<ItemStack> iconSupplier, boolean separateSection) {
        this.shortLabel = shortLabel;
        this.headerTitle = headerTitle;
        this.iconSupplier = iconSupplier;
        this.separateSection = separateSection;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    /** Returns true for categories rendered as a separate standalone section in the sidebar. */
    public boolean isSeparateSection() {
        return separateSection;
    }

    public ItemStack getIcon() {
        if (cachedIcon == null) {
            cachedIcon = iconSupplier.get();
        }
        return cachedIcon;
    }
}
