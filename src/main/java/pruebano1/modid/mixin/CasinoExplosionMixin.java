package pruebano1.modid.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pruebano1.modid.world.DrafteaCasinoManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Evita que TNT, creepers, camas u otras explosiones destruyan bloques dentro del casino "Draftea Tony Diaz".
 */
@Mixin(ExplosionImpl.class)
public abstract class CasinoExplosionMixin {

    @Shadow
    @Final
    private ServerWorld world;

    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"), cancellable = true)
    private void filterCasinoProtectedBlocks(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> blocks = cir.getReturnValue();
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        boolean hasProtected = false;
        for (BlockPos pos : blocks) {
            if (DrafteaCasinoManager.isProtectedBlock(this.world, pos)) {
                hasProtected = true;
                break;
            }
        }
        if (!hasProtected) {
            return;
        }

        List<BlockPos> filtered = new ArrayList<>(blocks.size());
        for (BlockPos pos : blocks) {
            if (!DrafteaCasinoManager.isProtectedBlock(this.world, pos)) {
                filtered.add(pos);
            }
        }
        cir.setReturnValue(filtered);
    }
}
