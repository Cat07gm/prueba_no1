package pruebano1.modid;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import pruebano1.modid.entity.IceBallProjectileEntity;

/**
 * Ítem "Bola de Hielo" (proyectil de José Torres / Jhoseph towers y también arrojadizo por el jugador):
 * - Parecido a una bola de nieve pero con textura de hielo cristalino.
 * - Al impactar produce sonidos de hielo rompiéndose, aplica Lentitud/Congelación y causa
 *   daño un poco más fuerte que la Toalla Kotex (11.0F = 5.5 corazones).
 */
public class IceBallItem extends Item implements ProjectileItem {

    public IceBallItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(
                null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS,
                0.7F, 0.85F + (world.getRandom().nextFloat() * 0.25F)
        );
        user.getItemCooldownManager().set(stack, 6);

        if (!world.isClient()) {
            IceBallProjectileEntity projectile = new IceBallProjectileEntity(
                    world, user, stack.copyWithCount(1), 11.0F, false
            );
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.65F, 0.8F);
            world.spawnEntity(projectile);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.isInCreativeMode()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        return new IceBallProjectileEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
    }
}
