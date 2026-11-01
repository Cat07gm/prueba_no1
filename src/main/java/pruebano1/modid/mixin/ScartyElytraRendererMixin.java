package pruebano1.modid.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pruebano1.modid.ModItem;
import pruebano1.modid.client.ScartyEntityModel;

@Mixin(ElytraFeatureRenderer.class)
public abstract class ScartyElytraRendererMixin<S extends BipedEntityRenderState, M extends EntityModel<S>> {

    @Shadow
    @Final
    private EquipmentRenderer equipmentRenderer;

    @Unique
    private ScartyEntityModel scartyModel;

    @Unique
    private ScartyEntityModel scartyBabyModel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initScartyModels(
            FeatureRendererContext<S, M> context,
            LoadedEntityModels loader,
            EquipmentRenderer equipmentRenderer,
            CallbackInfo ci
    ) {
        this.scartyModel = new ScartyEntityModel(false);
        this.scartyBabyModel = new ScartyEntityModel(true);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderScarty3DModel(
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider,
            int light,
            S state,
            float limbAngle,
            float limbDistance,
            CallbackInfo ci
    ) {
        ItemStack chestStack = state.equippedChestStack;
        if (!chestStack.isOf(ModItem.SCARTY)) {
            return;
        }

        EquippableComponent equippableComponent = chestStack.get(DataComponentTypes.EQUIPPABLE);
        if (equippableComponent == null || equippableComponent.assetId().isEmpty()) {
            return;
        }

        ScartyEntityModel modelToRender = state.baby ? this.scartyBabyModel : this.scartyModel;

        matrixStack.push();
        matrixStack.translate(0.0F, 0.0F, 0.125F);
        modelToRender.setAngles(state);
        this.equipmentRenderer.render(
                EquipmentModel.LayerType.WINGS,
                equippableComponent.assetId().get(),
                modelToRender,
                chestStack,
                matrixStack,
                vertexConsumerProvider,
                light,
                null
        );
        matrixStack.pop();

        // Cancelamos el renderizado cuadrado por defecto porque ya dibujamos el modelo 3D de Scarty
        ci.cancel();
    }
}
