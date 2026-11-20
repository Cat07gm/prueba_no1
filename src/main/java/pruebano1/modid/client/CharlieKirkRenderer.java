package pruebano1.modid.client;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.entity.CharlieKirkEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renderizador de GeckoLib para Charlie Kirk (jefe de La Casa Blanca).
 * - Oculta el modelo y la sombra mientras espera en el podio o antes del segundo 47.8 de la cinemática.
 * - Dibuja el círculo giratorio de 8 armas 3D (rifles/escopetas y 4 bazookas en Fase 2) durante el Ataque Especial 1.
 */
public class CharlieKirkRenderer extends GeoEntityRenderer<CharlieKirkEntity> {

    private static final Identifier WHITE_TEXTURE = Identifier.of(
            Prueba_no1.MOD_ID,
            "textures/entity/guard_projectile_white.png"
    );

    public CharlieKirkRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(Identifier.of(Prueba_no1.MOD_ID, "charlie_kirk")));
        this.shadowRadius = 0.6F;
    }

    @Override
    public boolean shouldRender(CharlieKirkEntity entity, Frustum frustum, double x, double y, double z) {
        if (entity.isWaitingForPlayer()
                || (entity.isInSpawnCinematic() && entity.getSpawnCinematicTicks() < CharlieKirkEntity.REVEAL_MODEL_TICK)) {
            return false;
        }
        if (entity.isWeaponCircleActive()) {
            return super.shouldRender(entity, frustum, x, y, z) || frustum.isVisible(entity.getBoundingBox().expand(4.5D));
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void renderFinal(
            MatrixStack poseStack,
            CharlieKirkEntity animatable,
            BakedGeoModel model,
            VertexConsumerProvider bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int renderColor
    ) {
        super.renderFinal(poseStack, animatable, model, bufferSource, buffer, partialTick, packedLight, packedOverlay, renderColor);

        if (!animatable.isWeaponCircleActive()) {
            return;
        }

        int ticks = animatable.getWeaponCircleTicks();
        float t = MathHelper.clamp(ticks + partialTick, 0.0F, (float) CharlieKirkEntity.WEAPON_CIRCLE_DURATION_TICKS);
        float spawnIn = MathHelper.clamp(t / 12.0F, 0.0F, 1.0F);
        float fadeOut = MathHelper.clamp((CharlieKirkEntity.WEAPON_CIRCLE_DURATION_TICKS - t) / 12.0F, 0.0F, 1.0F);
        float scale = Math.min(spawnIn, fadeOut);
        if (scale <= 0.02F) {
            return;
        }

        boolean phase2 = animatable.isPhase2();
        float baseAngle = animatable.getWeaponCircleAngle() + partialTick * CharlieKirkEntity.WEAPON_CIRCLE_ROT_SPEED;
        VertexConsumer vc = bufferSource.getBuffer(RenderLayer.getEntityCutoutNoCull(WHITE_TEXTURE));
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        for (int i = 0; i < CharlieKirkEntity.WEAPON_CIRCLE_COUNT; i++) {
            double slotAngle = baseAngle + i * ((Math.PI * 2.0D) / CharlieKirkEntity.WEAPON_CIRCLE_COUNT);
            double dirX = Math.cos(slotAngle);
            double dirZ = Math.sin(slotAngle);

            float bob = (float) Math.sin((t * 0.18D) + i * 0.85D) * 0.09F;
            float wx = (float) (dirX * CharlieKirkEntity.WEAPON_CIRCLE_RADIUS);
            float wy = 0.30F + bob;
            float wz = (float) (dirZ * CharlieKirkEntity.WEAPON_CIRCLE_RADIUS);

            // Retroceso visual breve en el momento de disparo
            float recoil = 0.0F;
            if (!CharlieKirkEntity.isBazookaSlot(i, phase2)) {
                int[] burstTicks = {22, 32, 42, 52};
                for (int bt : burstTicks) {
                    float dt = t - bt;
                    if (dt >= 0.0F && dt < 5.0F) {
                        recoil = (1.0F - dt / 5.0F) * 0.18F;
                        break;
                    }
                }
            } else {
                float dt = t - CharlieKirkEntity.WEAPON_CIRCLE_BAZOOKA_FIRE_TICK;
                if (dt >= 0.0F && dt < 8.0F) {
                    recoil = (1.0F - dt / 8.0F) * 0.28F;
                }
            }

            wx -= (float) (dirX * recoil);
            wz -= (float) (dirZ * recoil);

            float yawDeg = (float) (MathHelper.atan2(dirX, dirZ) * (180.0D / Math.PI));

            poseStack.push();
            poseStack.translate(wx, wy, wz);
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.5F));
            poseStack.scale(scale, scale, scale);

            MatrixStack.Entry entry = poseStack.peek();
            if (CharlieKirkEntity.isBazookaSlot(i, phase2)) {
                boolean loaded = ticks < CharlieKirkEntity.WEAPON_CIRCLE_BAZOOKA_FIRE_TICK;
                renderBazookaModel(entry, vc, fullLight, loaded);
            } else {
                renderGunModel(entry, vc, fullLight, i);
            }

            poseStack.pop();
        }
    }

    /**
     * Dibuja un modelo 3D de Bazooka verde oliva apuntando hacia +Z.
     */
    private static void renderBazookaModel(MatrixStack.Entry entry, VertexConsumer vc, int light, boolean loaded) {
        // Campana trasera ensanchada
        drawBox(entry, vc, light, -0.105F, -0.105F, -0.78F, 0.105F, 0.105F, -0.62F, 62, 82, 42);
        // Tubo principal verde oliva
        drawBox(entry, vc, light, -0.088F, -0.088F, -0.62F, 0.088F, 0.088F, 0.68F, 82, 108, 52);
        // Bandas amarillo/naranja características
        drawBox(entry, vc, light, -0.094F, -0.094F, -0.18F, 0.094F, 0.094F, -0.08F, 228, 174, 42);
        drawBox(entry, vc, light, -0.094F, -0.094F, 0.36F, 0.094F, 0.094F, 0.44F, 212, 114, 34);
        // Anillo delantero en la boca del tubo
        drawBox(entry, vc, light, -0.104F, -0.104F, 0.68F, 0.104F, 0.104F, 0.78F, 52, 68, 36);
        // Mira óptica superior
        drawBox(entry, vc, light, -0.032F, 0.088F, -0.02F, 0.032F, 0.165F, 0.18F, 36, 40, 46);
        drawBox(entry, vc, light, -0.042F, 0.12F, 0.18F, 0.042F, 0.17F, 0.25F, 72, 142, 198);
        // Empuñaduras inferiores
        drawBox(entry, vc, light, -0.028F, -0.25F, 0.04F, 0.028F, -0.088F, 0.13F, 38, 40, 44);
        drawBox(entry, vc, light, -0.026F, -0.22F, 0.34F, 0.026F, -0.088F, 0.41F, 38, 40, 44);
        // Ojiva roja visible dentro de la boca antes de disparar
        if (loaded) {
            drawBox(entry, vc, light, -0.062F, -0.062F, 0.78F, 0.062F, 0.062F, 0.94F, 206, 46, 42);
            drawBox(entry, vc, light, -0.036F, -0.036F, 0.94F, 0.036F, 0.036F, 1.02F, 236, 72, 64);
        }
    }

    /**
     * Dibuja un rifle/arma de fuego 3D apuntando hacia +Z con variantes visuales de la hoja de armas.
     */
    private static void renderGunModel(MatrixStack.Entry entry, VertexConsumer vc, int light, int slot) {
        int variant = slot % 4;
        switch (variant) {
            case 1 -> {
                // Variante SCAR-H beige táctico con mira
                drawBox(entry, vc, light, -0.048F, -0.095F, -0.64F, 0.048F, 0.055F, -0.30F, 196, 178, 138); // culata
                drawBox(entry, vc, light, -0.052F, -0.055F, -0.30F, 0.052F, 0.068F, 0.38F, 204, 186, 146);  // cajón/guardamanos
                drawBox(entry, vc, light, -0.034F, -0.22F, 0.02F, 0.034F, -0.055F, 0.16F, 172, 154, 118);   // cargador recto
                drawBox(entry, vc, light, -0.024F, -0.19F, -0.24F, 0.024F, -0.055F, -0.16F, 42, 44, 48);    // empuñadura
                drawBox(entry, vc, light, -0.022F, -0.008F, 0.38F, 0.022F, 0.036F, 0.72F, 48, 52, 58);      // cañón
                drawBox(entry, vc, light, -0.028F, 0.068F, -0.10F, 0.028F, 0.125F, 0.18F, 40, 42, 46);      // mira telescópica
            }
            case 2 -> {
                // Variante AK-47 con culata/guardamanos de madera
                drawBox(entry, vc, light, -0.044F, -0.10F, -0.62F, 0.044F, 0.042F, -0.28F, 148, 86, 38);   // culata madera
                drawBox(entry, vc, light, -0.048F, -0.048F, -0.28F, 0.048F, 0.062F, 0.12F, 44, 46, 52);    // cuerpo acero
                drawBox(entry, vc, light, -0.042F, -0.038F, 0.12F, 0.042F, 0.048F, 0.42F, 158, 94, 44);    // guardamanos madera
                drawBox(entry, vc, light, -0.028F, -0.22F, 0.00F, 0.028F, -0.048F, 0.12F, 36, 38, 42);     // cargador curvo sup.
                drawBox(entry, vc, light, -0.026F, -0.30F, 0.04F, 0.026F, -0.20F, 0.16F, 34, 36, 40);      // cargador curvo inf.
                drawBox(entry, vc, light, -0.024F, -0.19F, -0.24F, 0.024F, -0.048F, -0.16F, 138, 78, 34);  // empuñadura madera
                drawBox(entry, vc, light, -0.020F, -0.004F, 0.42F, 0.020F, 0.034F, 0.74F, 46, 48, 54);     // cañón
            }
            case 3 -> {
                // Variante ametralladora pesada M249 negra con bípode/caja de munición
                drawBox(entry, vc, light, -0.048F, -0.092F, -0.65F, 0.048F, 0.046F, -0.32F, 38, 40, 44);   // culata
                drawBox(entry, vc, light, -0.058F, -0.062F, -0.32F, 0.058F, 0.072F, 0.42F, 48, 52, 58);    // cajón pesado
                drawBox(entry, vc, light, -0.078F, -0.21F, -0.04F, 0.078F, -0.062F, 0.16F, 64, 72, 56);    // caja munición
                drawBox(entry, vc, light, -0.024F, -0.19F, -0.26F, 0.024F, -0.062F, -0.18F, 34, 36, 40);   // empuñadura
                drawBox(entry, vc, light, -0.024F, -0.008F, 0.42F, 0.024F, 0.036F, 0.80F, 40, 44, 50);     // cañón largo
                drawBox(entry, vc, light, -0.024F, 0.072F, -0.08F, 0.024F, 0.12F, 0.14F, 64, 68, 74);      // mira
            }
            default -> {
                // Variante M4/AR-15 negro táctico con mira holo
                drawBox(entry, vc, light, -0.044F, -0.088F, -0.62F, 0.044F, 0.046F, -0.28F, 36, 38, 42);   // culata
                drawBox(entry, vc, light, -0.048F, -0.052F, -0.28F, 0.048F, 0.064F, 0.40F, 44, 48, 54);    // cuerpo y rieles
                drawBox(entry, vc, light, -0.030F, -0.23F, 0.02F, 0.030F, -0.052F, 0.15F, 56, 60, 66);     // cargador
                drawBox(entry, vc, light, -0.024F, -0.19F, -0.24F, 0.024F, -0.052F, -0.16F, 32, 34, 38);   // empuñadura
                drawBox(entry, vc, light, -0.020F, -0.006F, 0.40F, 0.020F, 0.034F, 0.72F, 40, 44, 50);     // cañón
                drawBox(entry, vc, light, -0.024F, 0.064F, -0.06F, 0.024F, 0.118F, 0.12F, 32, 34, 38);     // asa/mira
            }
        }
    }

    private static void drawBox(
            MatrixStack.Entry entry,
            VertexConsumer vc,
            int light,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int r, int g, int b
    ) {
        int topR = Math.min(255, (int) (r * 1.10F));
        int topG = Math.min(255, (int) (g * 1.10F));
        int topB = Math.min(255, (int) (b * 1.10F));
        int sideR = (int) (r * 0.86F);
        int sideG = (int) (g * 0.86F);
        int sideB = (int) (b * 0.86F);
        int botR = (int) (r * 0.70F);
        int botG = (int) (g * 0.70F);
        int botB = (int) (b * 0.70F);

        // +Y
        drawQuad(entry, vc, light,
                minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F, topR, topG, topB);
        // -Y
        drawQuad(entry, vc, light,
                minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F, botR, botG, botB);
        // +Z
        drawQuad(entry, vc, light,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                0.0F, 0.0F, 1.0F, r, g, b);
        // -Z
        drawQuad(entry, vc, light,
                maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                0.0F, 0.0F, -1.0F, botR, botG, botB);
        // -X
        drawQuad(entry, vc, light,
                minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                -1.0F, 0.0F, 0.0F, sideR, sideG, sideB);
        // +X
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
