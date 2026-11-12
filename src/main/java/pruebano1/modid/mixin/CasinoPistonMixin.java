package pruebano1.modid.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pruebano1.modid.world.DrafteaCasinoManager;

/**
 * Impide que pistones empujen o jalen bloques dentro o hacia los límites protegidos del casino.
 */
@Mixin(PistonBlock.class)
public abstract class CasinoPistonMixin {

    @Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
    private static void preventPistonInsideCasino(
            BlockState state,
            World world,
            BlockPos pos,
            Direction direction,
            boolean canBreak,
            Direction pistonDir,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (DrafteaCasinoManager.isProtectedBlock(world, pos)
                || DrafteaCasinoManager.isProtectedBlock(world, pos.offset(direction))) {
            cir.setReturnValue(false);
        }
    }
}
