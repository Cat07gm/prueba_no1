package pruebano1.modid.mixin;

import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pruebano1.modid.world.DrafteaCasinoManager;

/**
 * Impide que el fuego se propague o queme estructuras del casino "Draftea Tony Diaz".
 */
@Mixin(FireBlock.class)
public abstract class CasinoFireMixin {

    @Inject(method = "trySpreadingFire", at = @At("HEAD"), cancellable = true)
    private void preventFireSpreadInsideCasino(
            World world,
            BlockPos pos,
            int spreadFactor,
            Random random,
            int currentAge,
            CallbackInfo ci
    ) {
        if (DrafteaCasinoManager.isProtectedBlock(world, pos)) {
            ci.cancel();
        }
    }
}
