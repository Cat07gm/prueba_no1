package pruebano1.modid.entity;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import pruebano1.modid.ModItem;

/**
 * MENÚ DEL LOUNGE BAR: lo que vende Tony Fase 1 (el bartender) a cambio de Tony Coins.
 *
 * Para cambiar precios o productos edita {@link #create()}:
 *   sell(monedas, objeto)  -> cuántas Tony Coins cuesta y qué entrega.
 * Las ofertas no se agotan nunca.
 */
public final class TonyBarTrades {

    /** Usos por oferta antes de "agotarse": tan alto que en la práctica es ilimitado. */
    private static final int UNLIMITED = 99999;

    private TonyBarTrades() {
    }

    public static TradeOfferList create() {
        TradeOfferList offers = new TradeOfferList();
        // ---- Botana ----
        offers.add(sell(1, new ItemStack(Items.COOKIE, 6)));
        offers.add(sell(1, new ItemStack(Items.BREAD, 4)));
        offers.add(sell(1, new ItemStack(Items.SWEET_BERRIES, 8)));
        offers.add(sell(2, new ItemStack(Items.COOKED_BEEF, 4)));
        offers.add(sell(2, new ItemStack(Items.PUMPKIN_PIE, 3)));
        offers.add(sell(3, new ItemStack(Items.CAKE, 1)));
        offers.add(sell(4, new ItemStack(Items.GOLDEN_CARROT, 4)));
        // ---- Bebidas ----
        offers.add(sell(1, named(new ItemStack(Items.HONEY_BOTTLE, 2), "Miel Draftea", Formatting.GOLD)));
        offers.add(sell(2, named(new ItemStack(Items.MILK_BUCKET), "Malteada de la Casa", Formatting.WHITE)));
        offers.add(sell(3, named(PotionContentsComponent.createStack(Items.POTION, Potions.SWIFTNESS),
                "Energético Draftea", Formatting.AQUA)));
        offers.add(sell(4, named(PotionContentsComponent.createStack(Items.POTION, Potions.REGENERATION),
                "Coctel Morado", Formatting.LIGHT_PURPLE)));
        // Fuerza II (1:30 min)
        offers.add(sell(5, named(PotionContentsComponent.createStack(Items.POTION, Potions.STRONG_STRENGTH),
                "Shot del Tony", Formatting.RED)));
        return offers;
    }

    private static TradeOffer sell(int coins, ItemStack result) {
        // priceMultiplier 0: el precio no sube aunque se compre mucho
        return new TradeOffer(new TradedItem(ModItem.TONY_COIN, coins), result, UNLIMITED, 0, 0.0F);
    }

    /** Nombre de bebida del bar (sin cursiva, como un objeto normal). */
    private static ItemStack named(ItemStack stack, String name, Formatting color) {
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(name).formatted(color));
        return stack;
    }
}
