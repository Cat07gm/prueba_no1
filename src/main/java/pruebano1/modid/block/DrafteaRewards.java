package pruebano1.modid.block;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * TABLA DE PREMIOS DE LA MÁQUINA "DRAFTEA".
 *
 * Cada premio tiene: (item, cantidad, peso).
 * El "peso" es la probabilidad relativa: la probabilidad real de cada premio es
 *      peso / (suma de todos los pesos)
 * Como los pesos de abajo suman 100, cada peso equivale directamente a su porcentaje.
 *
 * Para cambiar probabilidades solo modifica el último número de cada línea.
 * Para agregar un premio nuevo, añade otra línea: new Reward(Items.ALGO, cantidad, peso)
 * (para ítems del mod usa ModItem.ALGO, importando pruebano1.modid.ModItem).
 */
public final class DrafteaRewards {

    public record Reward(Item item, int count, int weight) {
        public ItemStack toStack() {
            return new ItemStack(item, count);
        }
    }

    public static final List<Reward> REWARDS = List.of(
            //          ítem                       cantidad  peso (%)
            new Reward(Items.EXPERIENCE_BOTTLE,    8,        25),
            new Reward(Items.IRON_INGOT,           8,        25),
            new Reward(Items.STRING,               8,        25),
            new Reward(Items.GOLD_INGOT,           5,        15),
            new Reward(Items.DIAMOND,              2,         5),
            new Reward(Items.GOLDEN_APPLE,         1,         5)
    );

    private DrafteaRewards() {
    }

    /** Elige un premio al azar respetando los pesos y devuelve su posición en REWARDS. */
    public static int pickIndex(Random random) {
        int total = 0;
        for (Reward reward : REWARDS) {
            total += reward.weight();
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < REWARDS.size(); i++) {
            roll -= REWARDS.get(i).weight();
            if (roll < 0) {
                return i;
            }
        }
        return 0;
    }

    /** Un ítem cualquiera de la tabla (sin pesos), para animar los rodillos mientras giran. */
    public static Item randomDisplayItem(Random random, Item avoid) {
        Item item = REWARDS.get(random.nextInt(REWARDS.size())).item();
        if (item == avoid && REWARDS.size() > 1) {
            item = REWARDS.get(random.nextInt(REWARDS.size())).item();
        }
        return item;
    }
}
