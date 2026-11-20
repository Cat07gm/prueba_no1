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
import pruebano1.modid.world.PalacioNacionalStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Evita que TNT, creepers, camas u otras explosiones destruyan bloques dentro del casino "Draftea Tony Diaz"
 * o dentro de la dungeon "El Palacio Nacional de México".
 */
@Mixin(ExplosionImpl.class)
public abstract class CasinoExplosionMixin {

    @Shadow
    @Final
    private ServerWorld world;

    private static boolean isExplosionProtected(ServerWorld world, BlockPos pos) {
        return DrafteaCasinoManager.isProtectedBlock(world, pos)
                || PalacioNacionalStructure.isPosInPalacioNacional(world, pos);
    }

    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"), cancellable = true)
    private void filterCasinoProtectedBlocks(CallbackInfoReturnable<List<BlockPos>> cir) {
        List<BlockPos> blocks = cir.getReturnValue();
        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        boolean hasProtected = false;
        for (BlockPos pos : blocks) {
            if (isExplosionProtected(this.world, pos)) {
                hasProtected = true;
                break;
            }
        }
        if (!hasProtected) {
            return;
        }

        List<BlockPos> filtered = new ArrayList<>(blocks.size());
        for (BlockPos pos : blocks) {
            if (!isExplosionProtected(this.world, pos)) {
                filtered.add(pos);
            }
        }
        cir.setReturnValue(filtered);
    }
}
