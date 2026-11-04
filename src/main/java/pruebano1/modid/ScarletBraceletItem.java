package pruebano1.modid;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Clase para los Brazaletes de Escarlata (Versión 1 y Versión 2 Dorada).
 * Al hacer clic derecho:
 * - Cura de golpe la cantidad de vida configurada (1 corazón = 2.0F de vida).
 * - Si es la versión dorada (giveHaste = true), da Prisa Minera I por 10 segundos (200 ticks).
 * - Aplica el tiempo de espera (cooldown) y gasta 1 punto de durabilidad.
 */
public class ScarletBraceletItem extends Item {

    private final float healAmount;
    private final int cooldownTicks;
    private final boolean giveHaste;

    public ScarletBraceletItem(float healAmount, int cooldownSeconds, boolean giveHaste, Settings settings) {
        super(settings);
        this.healAmount = healAmount;
        this.cooldownTicks = cooldownSeconds * 20; // En Minecraft, 1 segundo = 20 ticks
        this.giveHaste = giveHaste;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient()) {
            // Curar de golpe al jugador (8.0F = 4 corazones / 12.0F = 6 corazones)
            user.heal(this.healAmount);

            // Si es la versión 2 (dorada), dar Prisa Minera I (amplificador 0) por 10 segundos (200 ticks)
            if (this.giveHaste) {
                user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 10 * 20, 0));
            }

            // Activar el tiempo de recarga (Cooldown)
            user.getItemCooldownManager().set(stack, this.cooldownTicks);

            // Gastar 1 uso de durabilidad
            stack.damage(1, user, LivingEntity.getSlotForHand(hand));

            // Sonido al activarlo
            world.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.2F
            );
        }

        return ActionResult.SUCCESS;
    }
}
