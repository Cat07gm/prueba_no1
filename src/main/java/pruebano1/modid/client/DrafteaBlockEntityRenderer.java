package pruebano1.modid.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import pruebano1.modid.block.DrafteaBlock;
import pruebano1.modid.block.DrafteaBlockEntity;

/**
 * Dibuja en la pantalla de la Draftea los ítems de los 3 rodillos
 * (los que van pasando mientras gira y, al final, el premio que salió).
 */
public class DrafteaBlockEntityRenderer implements BlockEntityRenderer<DrafteaBlockEntity> {

    /** Centro horizontal de cada rodillo en la cara del modelo (en píxeles de bloque, 0-16), de izquierda a derecha. */
    private static final float[] REEL_X = {12.0F, 8.0F, 4.0F};
    /** Altura del centro de los rodillos dentro de la mitad superior. */
    private static final float REEL_Y = 7.0F;
    /** La cara de la pantalla está en z = 3 del modelo; dibujamos un poquito por delante. */
    private static final float SCREEN_Z = 2.9F;
    private static final float ITEM_SCALE = 0.17F;

    private final ItemRenderer itemRenderer;

    public DrafteaBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(DrafteaBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        if (!(state.getBlock() instanceof DrafteaBlock) || state.get(DrafteaBlock.HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        Direction facing = state.get(DrafteaBlock.FACING);

        matrices.push();
        // Girar todo según hacia dónde mira la máquina (el modelo base mira al norte)
        matrices.translate(0.5D, 0.0D, 0.5D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - facing.getPositiveHorizontalDegrees()));
        matrices.translate(-0.5D, 0.0D, -0.5D);

        for (int i = 0; i < DrafteaBlockEntity.REELS; i++) {
            Item item = entity.getReel(i);
            if (item == null || item == net.minecraft.item.Items.AIR) {
                continue;
            }
            matrices.push();
            matrices.translate(REEL_X[i] / 16.0F, REEL_Y / 16.0F, SCREEN_Z / 16.0F);
            // Los ítems planos miran hacia +Z; los volteamos para que miren al frente de la máquina (-Z)
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
            matrices.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            // Brillo máximo: la pantalla está "encendida"
            itemRenderer.renderItem(new ItemStack(item), ModelTransformationMode.FIXED,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
                    matrices, vertexConsumers, entity.getWorld(), 0);
            matrices.pop();
        }
        matrices.pop();
    }
}
