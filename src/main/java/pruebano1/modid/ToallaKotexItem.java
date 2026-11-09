package pruebano1.modid;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.entity.KotexProjectileEntity;

/**
 * Ítem proyectil "Toalla Kotex":
 * - Funciona como munición de Arco y Ballesta (al extender ArrowItem y estar en el tag minecraft:arrows).
 * - También se puede lanzar directamente con clic derecho en la mano como un proyectil arrojadizo.
 * - Hace 4 corazones de daño base (8.0F) y se queda clavada en el suelo como una flecha.
 */
public class ToallaKotexItem extends ArrowItem {

    public ToallaKotexItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter, @Nullable ItemStack shotFrom) {
        return new KotexProjectileEntity(world, shooter, stack.copyWithCount(1), shotFrom);
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        KotexProjectileEntity projectile = new KotexProjectileEntity(
                world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1), null
        );
        projectile.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        return projectile;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        // Si tiene un arco o ballesta en la otra mano, dejar que el arco/ballesta use la Toalla Kotex como munición
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (user.getStackInHand(otherHand).getItem() instanceof RangedWeaponItem) {
            return ActionResult.PASS;
        }

        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(
                null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
                0.85F, 0.9F + (world.getRandom().nextFloat() * 0.2F)
        );

        user.getItemCooldownManager().set(itemStack, 8);

        if (!world.isClient()) {
            KotexProjectileEntity projectile = new KotexProjectileEntity(
                    world, user, itemStack.copyWithCount(1), null
            );
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.85F, 1.0F);
            if (user.isInCreativeMode()) {
                projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            } else {
                projectile.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            }
            world.spawnEntity(projectile);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.isInCreativeMode()) {
            itemStack.decrement(1);
        }

        return ActionResult.SUCCESS;
    }
}
