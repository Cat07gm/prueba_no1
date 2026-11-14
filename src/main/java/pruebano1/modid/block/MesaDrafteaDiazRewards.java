package pruebano1.modid.block;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModItem;

import java.util.List;

/**
 * APUESTAS Y PREMIOS DE LA MESA DRAFTEA DIAZ.
 *
 * Para apostar se hace clic en la mesa con un lingote o gema en la mano.
 * Cada mineral pide una cantidad fija y pertenece a un nivel (tier): mientras más raro,
 * menos probabilidad de perder y más probabilidad de premios altos.
 *
 * - Cambiar cuánto cuesta apostar un mineral: segundo número de BETS.
 * - Cambiar la probabilidad de perder: LOSE_CHANCE (en %, una por nivel).
 * - Cambiar qué tan seguido sale cada tipo de premio: CATEGORY_WEIGHTS.
 * - Cambiar los premios: listas BASIC, MEDIUM y HIGH (objeto, cantidad, peso dentro de su lista).
 */
public final class MesaDrafteaDiazRewards {

    /** Apuesta: objeto que se apuesta, cuántos se consumen y su nivel (0 = común ... 3 = legendario). */
    public record Bet(Item item, int count, int tier) {
    }

    /** Premio: objeto, cantidad y peso (más peso = sale más dentro de su categoría). */
    public record Reward(Item item, int count, int weight) {
    }

    public static final List<Bet> BETS = List.of(
            // Nivel 0: comunes (60% de perder)
            new Bet(Items.COAL, 16, 0),
            new Bet(Items.COPPER_INGOT, 16, 0),
            new Bet(Items.REDSTONE, 16, 0),
            new Bet(Items.LAPIS_LAZULI, 16, 0),
            new Bet(Items.QUARTZ, 12, 0),
            // Nivel 1: metales preciosos (45% de perder)
            new Bet(Items.IRON_INGOT, 8, 1),
            new Bet(Items.GOLD_INGOT, 6, 1),
            // Nivel 2: gemas (30% de perder)
            new Bet(Items.EMERALD, 3, 2),
            new Bet(Items.DIAMOND, 2, 2),
            new Bet(ModItem.SCARLET_INGOT, 2, 2),
            // Nivel 3: legendario (15% de perder)
            new Bet(Items.NETHERITE_INGOT, 1, 3)
    );

    /** Probabilidad (%) de que la casa gane y no salga nada, por nivel de la apuesta. */
    public static final int[] LOSE_CHANCE = {60, 45, 30, 15};

    /**
     * Si ganas: peso de cada categoría de premio {básico, medio, alto} según el nivel de la apuesta.
     * Ej. con carbón (nivel 0) el 85% de los premios son básicos; con netherita el 40% son altos.
     */
    public static final int[][] CATEGORY_WEIGHTS = {
            {85, 14, 1},
            {60, 35, 5},
            {30, 55, 15},
            {10, 50, 40}
    };

    public static final List<Reward> BASIC = List.of(
            new Reward(Items.GOLDEN_CARROT, 8, 1),
            new Reward(Items.ENDER_PEARL, 4, 1),
            new Reward(Items.EXPERIENCE_BOTTLE, 8, 1),
            new Reward(Items.SPECTRAL_ARROW, 16, 1)
    );

    /** El libro encantado lleva un encantamiento aleatorio (sin maldiciones). */
    public static final List<Reward> MEDIUM = List.of(
            new Reward(Items.GOLDEN_APPLE, 2, 1),
            new Reward(Items.ENCHANTED_BOOK, 1, 1),
            new Reward(Items.SADDLE, 1, 1),
            new Reward(Items.NAME_TAG, 1, 1),
            new Reward(Items.FIREWORK_ROCKET, 16, 1)
    );

    public static final List<Reward> HIGH = List.of(
            new Reward(Items.TOTEM_OF_UNDYING, 1, 1),
            new Reward(Items.ENCHANTED_GOLDEN_APPLE, 1, 1),
            new Reward(Items.TRIDENT, 1, 1)
    );

    /** Categoría que devuelve {@link #roll}: -1 = perdiste, 0 = básico, 1 = medio, 2 = alto. */
    public static final int LOSE = -1;
    public static final int CATEGORY_HIGH = 2;

    private MesaDrafteaDiazRewards() {
    }

    public static @Nullable Bet findBet(ItemStack stack) {
        for (Bet bet : BETS) {
            if (stack.isOf(bet.item())) {
                return bet;
            }
        }
        return null;
    }

    /** Decide el resultado de una apuesta del nivel indicado: LOSE o la categoría del premio. */
    public static int roll(int tier, Random random) {
        tier = Math.clamp(tier, 0, LOSE_CHANCE.length - 1);
        if (random.nextInt(100) < LOSE_CHANCE[tier]) {
            return LOSE;
        }
        int[] weights = CATEGORY_WEIGHTS[tier];
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int pick = random.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick < 0) {
                return i;
            }
        }
        return 0;
    }

    /** Crea el objeto de premio de la categoría indicada. */
    public static ItemStack createPrize(int category, World world, Random random) {
        List<Reward> pool = switch (category) {
            case 2 -> HIGH;
            case 1 -> MEDIUM;
            default -> BASIC;
        };
        int total = 0;
        for (Reward r : pool) {
            total += r.weight();
        }
        int pick = random.nextInt(Math.max(1, total));
        Reward chosen = pool.get(0);
        for (Reward r : pool) {
            pick -= r.weight();
            if (pick < 0) {
                chosen = r;
                break;
            }
        }
        if (chosen.item() == Items.ENCHANTED_BOOK) {
            return randomEnchantedBook(world, random);
        }
        return new ItemStack(chosen.item(), chosen.count());
    }

    private static ItemStack randomEnchantedBook(World world, Random random) {
        Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        List<RegistryEntry.Reference<Enchantment>> options = registry.streamEntries()
                .filter(entry -> !entry.isIn(EnchantmentTags.CURSE))
                .toList();
        if (options.isEmpty()) {
            return new ItemStack(Items.BOOK);
        }
        RegistryEntry.Reference<Enchantment> entry = options.get(random.nextInt(options.size()));
        int level = 1 + random.nextInt(Math.max(1, entry.value().getMaxLevel()));
        return EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(entry, level));
    }
}
