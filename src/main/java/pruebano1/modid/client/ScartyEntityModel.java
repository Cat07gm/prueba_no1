package pruebano1.modid.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.ElytraEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.util.math.MathHelper;

/**
 * Modelo 3D personalizado de Scarty para la espalda del jugador.
 * Reproduce en 3D
 */
public class ScartyEntityModel extends ElytraEntityModel {

    private final ModelPart harness;
    private final ModelPart leftWingPart;
    private final ModelPart rightWingPart;

    public ScartyEntityModel(boolean baby) {
        super(getTexturedModelData(baby).createModel());
        ModelPart root = this.getRootPart();
        this.harness = root.getChild("harness");
        this.leftWingPart = root.getChild("left_wing");
        this.rightWingPart = root.getChild("right_wing");
    }

    public static TexturedModelData getTexturedModelData(boolean baby) {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // ARNÉS CENTRAL EN FORMA DE "H" (Fijo en el centro de la espalda)

        ModelPartBuilder harnessBuilder = ModelPartBuilder.create()
                // Columna vertical izquierda de la "H" (Escarlata)
                .uv(16, 12).cuboid(1.5F, 1.5F, 0.4F, 2.0F, 8.5F, 2.0F)
                // Columna vertical derecha de la "H" (Escarlata)
                .uv(16, 12).cuboid(-3.5F, 1.5F, 0.4F, 2.0F, 8.5F, 2.0F)
                // Barra horizontal superior (Núcleo de Diamante en relieve 3D)
                .uv(0, 0).cuboid(-1.5F, 2.5F, 0.7F, 3.0F, 1.8F, 2.2F)
                // Barra horizontal intermedia (Escarlata)
                .uv(16, 14).cuboid(-1.5F, 4.6F, 0.5F, 3.0F, 1.5F, 2.0F)
                // Barra horizontal inferior (Diamante oscuro en relieve 3D)
                .uv(0, 6).cuboid(-1.5F, 6.4F, 0.7F, 3.0F, 1.6F, 2.2F);

        root.addChild("harness", harnessBuilder, ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        //ALA IZQUIERDA (+X) Y ALA DERECHA (-X) CON LAS 5 PUNTAS EN 3D

        addWingSide(root, "left_wing", 1.0F);
        addWingSide(root, "right_wing", -1.0F);

        TexturedModelData texturedModelData = TexturedModelData.of(modelData, 64, 32);
        if (baby) {
            return texturedModelData.transform(ElytraEntityModel.BABY_TRANSFORMER);
        }
        return texturedModelData;
    }

    /**
     * Construye las 5 puntas/alas en 3D para un lado (sign = +1.0F izquierdo, -1.0F derecho).
     * ajustar aquí el largo, grosor o ángulo de cualquiera de las 5 piezas.
     */
    private static void addWingSide(ModelPartData root, String name, float sign) {
        // Nodo raíz del ala (pivote junto al pilar de la "H" en la espalda)
        ModelPartData wing = root.addChild(
                name,
                ModelPartBuilder.create(),
                ModelTransform.pivot(sign * 3.0F, 5.0F, 1.4F)
        );

        // PUNTA 1: Cuerno superior interno (sube junto al borde superior de la "H") ---
        float hornX = sign > 0 ? -0.5F : -1.5F;
        float hornTipX = sign > 0 ? 0.0F : -1.5F;
        wing.addChild(
                "inner_horn",
                ModelPartBuilder.create()
                        .uv(18, 14).cuboid(hornX, -6.5F, -0.5F, 2.0F, 5.0F, 1.5F)
                        .uv(20, 14).cuboid(hornTipX, -8.0F, -0.4F, 1.5F, 2.0F, 1.3F),
                ModelTransform.of(0.0F, -1.5F, 0.0F, 0.0F, 0.0F, sign * -0.15F)
        );

        //PUNTA 2: Ala diagonal superior con rejilla (Escarlata + Membrana de Phantom) ---
        float upperX = sign > 0 ? 0.0F : -3.0F;
        float upperTipX = sign > 0 ? 0.5F : -2.5F;
        wing.addChild(
                "upper_grid_wing",
                ModelPartBuilder.create()
                        // Cuerpo principal cuadriculado (usa la región UV de rejilla en x=32, y=0)
                        .uv(32, 0).cuboid(upperX, -9.5F, -0.6F, 3.0F, 9.5F, 1.5F)
                        // Punta escalonada superior
                        .uv(36, 2).cuboid(upperTipX, -11.5F, -0.5F, 2.0F, 2.0F, 1.3F),
                // Inclinada en diagonal hacia arriba y afuera (~45 grados)
                ModelTransform.of(sign * 1.0F, -1.5F, 0.0F, 0.0F, 0.0F, sign * 0.78F)
        );

        // PUNTA 3: Cuchilla central larga (sale horizontalmente hacia afuera) ---
        float midMainX = sign > 0 ? 0.0F : -10.5F;
        float midTipX = sign > 0 ? 10.5F : -12.5F;
        float midStepX = sign > 0 ? 0.0F : -6.5F;
        wing.addChild(
                "middle_long_blade",
                ModelPartBuilder.create()
                        // Cuchilla larga principal
                        .uv(16, 16).cuboid(midMainX, -1.5F, -0.8F, 10.5F, 2.5F, 1.8F)
                        // Punta exterior escalonada
                        .uv(16, 20).cuboid(midTipX, -1.0F, -0.6F, 2.0F, 1.8F, 1.4F)
                        // Escalón inferior de la cuchilla central
                        .uv(20, 22).cuboid(midStepX, 1.0F, -0.6F, 6.5F, 1.2F, 1.5F),
                ModelTransform.of(sign * 0.5F, -0.5F, 0.1F, 0.0F, 0.0F, sign * 0.04F)
        );

        // PUNTA 4: Cuchilla media-baja (sale debajo de la central hacia afuera) ---
        float lowMidX = sign > 0 ? 0.0F : -7.5F;
        float lowMidTipX = sign > 0 ? 7.5F : -9.0F;
        wing.addChild(
                "lower_mid_blade",
                ModelPartBuilder.create()
                        .uv(34, 4).cuboid(lowMidX, -1.0F, -0.6F, 7.5F, 2.4F, 1.5F)
                        .uv(18, 18).cuboid(lowMidTipX, -0.5F, -0.5F, 1.5F, 1.8F, 1.3F),
                // Inclinada hacia afuera y un poco hacia abajo (~20 grados)
                ModelTransform.of(sign * 0.5F, 1.8F, 0.0F, 0.0F, 0.0F, sign * -0.32F)
        );

        // PUNTA 5: Aleta inferior en zigzag (baja en diagonal con 3 escalones 3D) ---
        float tail1X = sign > 0 ? -0.5F : -2.0F;
        float tail2X = sign > 0 ? 0.3F : -2.8F;
        float tail3X = sign > 0 ? 1.1F : -3.1F;
        wing.addChild(
                "bottom_tail_blade",
                ModelPartBuilder.create()
                        // Escalón 1 (superior)
                        .uv(16, 14).cuboid(tail1X, 0.0F, -0.5F, 2.5F, 3.2F, 1.5F)
                        // Escalón 2 (medio, con detalle de membrana)
                        .uv(36, 4).cuboid(tail2X, 2.8F, -0.5F, 2.5F, 3.2F, 1.4F)
                        // Escalón 3 (punta inferior del zigzag)
                        .uv(18, 20).cuboid(tail3X, 5.6F, -0.4F, 2.0F, 2.6F, 1.3F),
                // Inclinada hacia abajo y afuera
                ModelTransform.of(sign * 0.2F, 3.2F, 0.0F, 0.0F, 0.0F, sign * -0.35F)
        );
    }

    @Override
    public void setAngles(BipedEntityRenderState state) {
        super.setAngles(state);

        float sneakOffset = state.isInSneakingPose ? 3.0F : 0.0F;
        this.harness.pivotY = sneakOffset;
        this.harness.pitch = state.isInSneakingPose ? 0.35F : 0.0F;

        // Calculamos cuánto están abiertas las alas (0.0 quieto en el suelo -> 1.0 planeando en el aire)
        float openFactor = MathHelper.clamp((Math.abs(state.leftWingRoll) - 0.2618F) / (1.5708F - 0.2618F), 0.0F, 1.0F);

        // Posición del pivote de cada ala para que encaje con el arnés central "H"
        this.leftWingPart.pivotX = 3.0F;
        this.leftWingPart.pivotY = 5.0F + sneakOffset;
        this.leftWingPart.pivotZ = 1.4F;

        this.rightWingPart.pivotX = -3.0F;
        this.rightWingPart.pivotY = 5.0F + sneakOffset;
        this.rightWingPart.pivotZ = 1.4F;

        // Mantener el diseño del boceto erguido en la espalda y desplegar las alas en 3D al volar
        float wingPitch = state.isInSneakingPose ? 0.35F : openFactor * 0.25F;
        float wingRoll = openFactor * 0.32F;
        float wingYaw = 0.18F - (openFactor * 0.14F);

        this.leftWingPart.pitch = wingPitch;
        this.leftWingPart.roll = -wingRoll;
        this.leftWingPart.yaw = wingYaw;

        this.rightWingPart.pitch = wingPitch;
        this.rightWingPart.roll = wingRoll;
        this.rightWingPart.yaw = -wingYaw;
    }
}
