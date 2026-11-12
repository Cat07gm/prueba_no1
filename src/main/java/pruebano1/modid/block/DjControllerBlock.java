package pruebano1.modid.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Consola controladora principal de DJ con pantalla de ondas y pads RGB.
 * Clic derecho: abre la interfaz para elegir la música del casino y su volumen.
 */
public class DjControllerBlock extends DjComponentBlock {

    public static final MapCodec<DjControllerBlock> CONTROLLER_CODEC = createCodec(DjControllerBlock::new);

    /**
     * Abre la pantalla del DJ en el cliente. Lo asigna Prueba_no1Client al iniciar,
     * así este bloque no depende de clases que solo existen en el cliente (y no rompe un servidor dedicado).
     */
    public static Consumer<BlockPos> clientScreenOpener = pos -> {
    };

    public DjControllerBlock(Settings settings) {
        super(10.0, settings);
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CONTROLLER_CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            clientScreenOpener.accept(pos);
        }
        return ActionResult.SUCCESS;
    }
}
