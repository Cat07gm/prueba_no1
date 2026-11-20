package pruebano1.modid.client;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import pruebano1.modid.Prueba_no1;

/**
 * Renderizador 3D direccional para las balas y cohetes de los Agentes de Seguridad de La Casa Blanca.
 * Orienta cada proyectil horizontalmente ("acostado") apuntando exactamente hacia su vector de velocidad.
 */
public class GuardProjectileRenderer extends EntityRenderer<ThrownItemEntity, ProjectileEntityRenderState> {

    private static final Identifier WHITE_TEXTURE = Identifier.of(
            Prueba_no1.MOD_ID,
            "textures/entity/guard_projectile_white.png"
    );

    private final boolean isRocket;

    private GuardProjectileRenderer(EntityRendererFactory.Context context, boolean isRocket) {
        super(context);
        this.isRocket = isRocket;
        this.shadowRadius = isRocket ? 0.25F : 0.08F;
    }

    public static GuardProjectileRenderer bullet(EntityRendererFactory.Context context) {
        return new GuardProjectileRenderer(context, false);
    }

    public static GuardProjectileRenderer rocket(EntityRendererFactory.Context context) {
        return new GuardProjectileRenderer(context, true);
    }

    @Override
    public ProjectileEntityRenderState createRenderState() {
        return new ProjectileEntityRenderState();
    }

    @Override
    public void updateRenderState(ThrownItemEntity entity, ProjectileEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        Vec3d vel = entity.getVelocity();
        if (vel.lengthSquared() > 1.0E-5D) {
            double horiz = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            state.yaw = (float) (MathHelper.atan2(vel.x, vel.z) * (180.0D / Math.PI));
            state.pitch = (float) (-(MathHelper.atan2(vel.y, horiz) * (180.0D / Math.PI)));
        } else {
            state.yaw = entity.getYaw(tickDelta);
            state.pitch = -entity.getPitch(tickDelta);
        }
    }

    @Override
    public void render(
            ProjectileEntityRenderState state,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light
    ) {
        matrices.push();
        matrices.translate(0.0F, this.isRocket ? 0.22F : 0.125F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        if (this.isRocket) {
            renderRocketMesh(entry, vc, fullLight);
        } else {
            renderBulletMesh(entry, vc, fullLight);
        }

        matrices.pop();
        super.render(state, matrices, vertexConsumers, light);
    }

    private static void renderBulletMesh(MatrixStack.Entry entry, VertexConsumer vc, int light) {
        // Casquillo de latón trasero (acostado en eje Z)
        drawBox(entry, vc, light, -0.042F, -0.042F, -0.22F, 0.042F, 0.042F, 0.02F, 224, 178, 62);
        // Destello tracer en la parte posterior del casquillo
        drawQuad(entry, vc, light,
                -0.032F, -0.032F, -0.224F,
                -0.032F, 0.032F, -0.224F,
                0.032F, 0.032F, -0.224F,
                0.032F, -0.032F, -0.224F,
                0.0F, 0.0F, -1.0F, 255, 240, 150);
        // Cuerpo medio encamisado en cobre
        drawBox(entry, vc, light, -0.036F, -0.036F, 0.02F, 0.036F, 0.036F, 0.14F, 206, 118, 56);
        // Punta afilada apuntando al frente (+Z)
        drawBox(entry, vc, light, -0.022F, -0.022F, 0.14F, 0.022F, 0.022F, 0.21F, 242, 198, 128);
    }

    private static void renderRocketMesh(MatrixStack.Entry entry, VertexConsumer vc, int light) {
        // Tobera trasera de acero oscuro
        drawBox(entry, vc, light, -0.075F, -0.075F, -0.44F, 0.075F, 0.075F, -0.24F, 48, 52, 58);
        // Cuatro aletas estabilizadoras (horizontales y verticales)
        drawBox(entry, vc, light, -0.17F, -0.015F, -0.44F, 0.17F, 0.015F, -0.27F, 64, 68, 74);
        drawBox(entry, vc, light, -0.015F, -0.17F, -0.44F, 0.015F, 0.17F, -0.27F, 64, 68, 74);
        // Núcleo incandescente de propulsión trasero
        drawQuad(entry, vc, light,
                -0.055F, -0.055F, -0.444F,
                -0.055F, 0.055F, -0.444F,
                0.055F, 0.055F, -0.444F,
                0.055F, -0.055F, -0.444F,
                0.0F, 0.0F, -1.0F, 255, 164, 48);
        // Fuselaje principal verde oliva
        drawBox(entry, vc, light, -0.095F, -0.095F, -0.24F, 0.095F, 0.095F, 0.06F, 78, 102, 48);
        // Anillo amarillo de advertencia
        drawBox(entry, vc, light, -0.102F, -0.102F, 0.06F, 0.102F, 0.102F, 0.16F, 234, 182, 44);
        // Ojiva explosiva roja cónica apuntando hacia el frente (+Z)
        drawBox(entry, vc, light, -0.082F, -0.082F, 0.16F, 0.082F, 0.082F, 0.32F, 206, 46, 42);
        drawBox(entry, vc, light, -0.048F, -0.048F, 0.32F, 0.048F, 0.048F, 0.42F, 232, 68, 62);
    }

    private static void drawBox(
            MatrixStack.Entry entry,
            VertexConsumer vc,
            int light,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int r, int g, int b
    ) {
        // Sombreado sutil por cara para volumen 3D nítido
        int topR = Math.min(255, (int) (r * 1.08F));
        int topG = Math.min(255, (int) (g * 1.08F));
        int topB = Math.min(255, (int) (b * 1.08F));
        int sideR = (int) (r * 0.88F);
        int sideG = (int) (g * 0.88F);
        int sideB = (int) (b * 0.88F);
        int botR = (int) (r * 0.72F);
        int botG = (int) (g * 0.72F);
        int botB = (int) (b * 0.72F);

        // +Y (arriba)
        drawQuad(entry, vc, light,
                minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F, topR, topG, topB);
        // -Y (abajo)
        drawQuad(entry, vc, light,
                minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F, botR, botG, botB);
        // +Z (frente / nariz)
        drawQuad(entry, vc, light,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                0.0F, 0.0F, 1.0F, r, g, b);
        // -Z (atrás / cola)
        drawQuad(entry, vc, light,
                maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                0.0F, 0.0F, -1.0F, botR, botG, botB);
        // -X (lado oeste)
        drawQuad(entry, vc, light,
                minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                -1.0F, 0.0F, 0.0F, sideR, sideG, sideB);
        // +X (lado este)
        drawQuad(entry, vc, light,
                maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                1.0F, 0.0F, 0.0F, sideR, sideG, sideB);
    }

    private static void drawQuad(
            MatrixStack.Entry entry,
            VertexConsumer vc,
            int light,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float nx, float ny, float nz,
            int r, int g, int b
    ) {
        vc.vertex(entry, x1, y1, z1).color(r, g, b, 255).texture(0.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, nx, ny, nz);
        vc.vertex(entry, x2, y2, z2).color(r, g, b, 255).texture(0.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, nx, ny, nz);
        vc.vertex(entry, x3, y3, z3).color(r, g, b, 255).texture(1.0F, 1.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, nx, ny, nz);
        vc.vertex(entry, x4, y4, z4).color(r, g, b, 255).texture(1.0F, 0.0F)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, nx, ny, nz);
    }
}
