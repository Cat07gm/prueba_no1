package pruebano1.modid.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * Mueble/decoración simple del casino que no gira (mesas, macetas): solo tiene su forma.
 */
public class CasinoDecorBlock extends Block {

    private final VoxelShape shape;

    public CasinoDecorBlock(VoxelShape shape, Settings settings) {
        super(settings);
        this.shape = shape;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }
}
