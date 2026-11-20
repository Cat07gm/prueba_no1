package pruebano1.modid.client.encyclopedia;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pruebano1.modid.ModItem;
import pruebano1.modid.Prueba_no1Client;
import pruebano1.modid.client.encyclopedia.EncyclopediaTheme.ButtonStyle;
import pruebano1.modid.client.encyclopedia.EncyclopediaTheme.NodeFrameKind;
import pruebano1.modid.client.encyclopedia.EncyclopediaTheme.NodeVisualState;
import pruebano1.modid.entity.ModEntities;
import pruebano1.modid.world.DungeonProgressionState.DungeonStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Pantalla interactiva de Progresión de Dungeons inspirada en los Logros (Advancements) de Minecraft
 * combinada con la estética de Libro / Pergamino y Broches de Escarlata de Lon Terry.
 *
 * Incluye:
 * - Lienzo cartográfico arrastrable y con zoom mediante rueda del ratón.
 * - Líneas ortogonales que se iluminan dinámicamente en orden al derrotar a cada jefe.
 * - Modo Sin Spoilers: cualquier dungeon cuyo jefe anterior aún no haya sido derrotado (así como
 *   las futuras por desarrollar) oculta su identidad con "???" y renderiza al jefe en silueta oscura.
 * - Panel lateral de inspección al hacer clic con Estado Global, Jefe, Llave de Invocación y acceso a su ficha.
 */
public class DungeonProgressionScreen extends Screen {

    private record ProgressionNode(
            int index,
            String romanStep,
            @Nullable DungeonStage stage,
            String revealedTitle,
            String revealedBossLabel,
            String revealedKeyName,
            Supplier<ItemStack> keyStackSupplier,
            @Nullable Supplier<EntityType<? extends LivingEntity>> bossEntitySupplier,
            float gridEntityScale,
            float showcaseEntityScale,
            float entityOffsetY,
            boolean twinMysteryBosses,
            @Nullable String encyclopediaEntryId,
            NodeFrameKind frameKind,
            int mapX,
            int mapY
    ) {
    }

    private static final List<ProgressionNode> NODES = List.of(
            new ProgressionNode(
                    0, "I",
                    DungeonStage.ISLA_PENSTEIN,
                    "Isla Penstein",
                    "Jefri Penstein",
                    "Orden de arresto",
                    () -> new ItemStack(ModItem.ARREST_WARRANT),
                    () -> ModEntities.JEFRI_PENSTEIN,
                    15.5F, 44.0F, 1.05F, false,
                    "isla_penstein",
                    NodeFrameKind.TASK,
                    -270, -24
            ),
            new ProgressionNode(
                    1, "II",
                    DungeonStage.CASA_JOSE_TORRES,
                    "Casa de José Torres",
                    "José Torres",
                    "Acordeon de José Torres",
                    () -> new ItemStack(ModItem.JOSE_TORRES_ACCORDION),
                    () -> ModEntities.JOSE_TORRES,
                    15.5F, 44.0F, 1.05F, false,
                    "casa_jose_torres",
                    NodeFrameKind.GOAL,
                    -135, 36
            ),
            new ProgressionNode(
                    2, "III",
                    DungeonStage.CASA_BLANCA,
                    "La Casa Blanca",
                    "Charlie Kirk",
                    "Permiso para portar armas",
                    () -> new ItemStack(ModItem.GUN_PERMIT),
                    () -> ModEntities.CHARLIE_KIRK,
                    15.5F, 44.0F, 1.05F, false,
                    "casa_blanca",
                    NodeFrameKind.GOAL,
                    0, -26
            ),
            new ProgressionNode(
                    3, "IV",
                    DungeonStage.PALACIO_NACIONAL,
                    "El Palacio Nacional",
                    "Mini-Jefe: ??? / Boss Final: ???",
                    "???",
                    () -> ItemStack.EMPTY,
                    null,
                    15.0F, 42.0F, 1.0F, true,
                    "palacio_nacional",
                    NodeFrameKind.CHALLENGE,
                    140, 34
            ),
            new ProgressionNode(
                    4, "V",
                    null,
                    "???",
                    "???",
                    "???",
                    () -> ItemStack.EMPTY,
                    null,
                    14.0F, 40.0F, 1.0F, false,
                    null,
                    NodeFrameKind.GOAL,
                    275, -20
            ),
            new ProgressionNode(
                    5, "VI",
                    null,
                    "???",
                    "???",
                    "???",
                    () -> ItemStack.EMPTY,
                    null,
                    14.0F, 40.0F, 1.0F, false,
                    null,
                    NodeFrameKind.CHALLENGE,
                    405, 22
            )
    );

    private static final int NODE_BASE_SIZE = 54;

    @Nullable
    private final Screen parentScreen;

    private int winX, winY, winW, winH;
    private final int topBarH = 30;
    private final int bottomBarH = 20;
    private int mapViewX, mapViewY, mapViewW, mapViewH;
    private int spineX;
    private int detailX, detailY, detailW, detailH;

    // Botones superiores
    private int backBtnX, backBtnY, backBtnW, backBtnH;
    private int centerBtnX, centerBtnY, centerBtnW, centerBtnH;
    private int closeBtnX, closeBtnY, closeBtnW, closeBtnH;

    // Botón de ir a ficha en el panel derecho
    private int guideBtnX, guideBtnY, guideBtnW, guideBtnH;
    private boolean guideBtnEnabled = false;

    // Estado de cámara del mapa arrastrable
    private float panX = 45.0F;
    private float panY = 0.0F;
    private float targetPanX = 45.0F;
    private float targetPanY = 0.0F;
    private float zoom = 1.0F;
    private float targetZoom = 1.0F;
    private boolean draggingMap = false;
    private double dragStartMouseX, dragStartMouseY;
    private boolean movedDuringDrag = false;

    // Selección y rotación 3D del jefe inspeccionado
    private int selectedNodeIndex = 0;
    private int hoveredNodeIndex = -1;
    private final Map<EntityType<?>, LivingEntity> entityCache = new HashMap<>();
    private float previewYawDeg = 22.0F;
    private float previewPitchDeg = 8.0F;
    private boolean autoRotateBoss3D = true;
    private boolean draggingBossStage = false;
    private int stageBoxX, stageBoxY, stageBoxW, stageBoxH;

    private float animTimeSec = 0.0F;
    @Nullable
    private ItemStack pendingTooltipStack = null;
    @Nullable
    private List<Text> pendingTooltipLines = null;

    public DungeonProgressionScreen(@Nullable Screen parentScreen) {
        super(Text.literal("Progresión de Dungeons"));
        this.parentScreen = parentScreen;
        // Seleccionar por defecto la siguiente dungeon activa por conquistar
        int initialIdx = 0;
        for (int i = 0; i < NODES.size(); i++) {
            ProgressionNode n = NODES.get(i);
            if (n.stage() != null && ClientDungeonProgression.isRevealed(n.stage())) {
                initialIdx = i;
                if (!ClientDungeonProgression.isCompleted(n.stage())) {
                    break;
                }
            }
        }
        this.selectedNodeIndex = initialIdx;
        ProgressionNode target = NODES.get(initialIdx);
        this.panX = -target.mapX() * 0.55F;
        this.targetPanX = this.panX;
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        int marginX = width >= 680 ? 12 : 6;
        int marginY = height >= 400 ? 10 : 6;
        this.winW = Math.min(width - marginX * 2, 1080);
        this.winH = Math.min(height - marginY * 2, 640);
        this.winX = (width - winW) / 2;
        this.winY = (height - winH) / 2;

        this.closeBtnW = 62;
        this.closeBtnH = 18;
        this.closeBtnX = winX + winW - closeBtnW - 12;
        this.closeBtnY = winY + (topBarH - closeBtnH) / 2 + 2;

        this.backBtnW = 114;
        this.backBtnH = 18;
        this.backBtnX = closeBtnX - backBtnW - 6;
        this.backBtnY = closeBtnY;

        this.centerBtnW = 74;
        this.centerBtnH = 18;
        this.centerBtnX = backBtnX - centerBtnW - 6;
        this.centerBtnY = closeBtnY;

        int bodyY = winY + topBarH + 2;
        int bodyH = winH - topBarH - bottomBarH - 6;

        int innerLeft = winX + 9;
        int innerRight = winX + winW - 9;
        int totalInnerW = innerRight - innerLeft;

        this.detailW = MathHelper.clamp((int) (totalInnerW * 0.34F), 200, 320);
        this.spineX = innerRight - detailW - 4;

        this.mapViewX = innerLeft;
        this.mapViewY = bodyY;
        this.mapViewW = (spineX - 3) - innerLeft;
        this.mapViewH = bodyH;

        this.detailX = spineX + 4;
        this.detailY = bodyY;
        this.detailH = bodyH;
    }

    @Override
    public void tick() {
        super.tick();
        if (autoRotateBoss3D && !draggingBossStage) {
            previewYawDeg = (previewYawDeg + 1.2F) % 360.0F;
        }
        for (LivingEntity cached : entityCache.values()) {
            if (cached != null) {
                cached.age++;
            }
        }
    }

    private NodeVisualState getNodeState(ProgressionNode node) {
        if (node.stage() == null) {
            return NodeVisualState.FUTURE_DEV;
        }
        if (ClientDungeonProgression.isCompleted(node.stage())) {
            return NodeVisualState.COMPLETED;
        }
        if (ClientDungeonProgression.isRevealed(node.stage())) {
            return NodeVisualState.UNLOCKED_NEXT;
        }
        return NodeVisualState.LOCKED_SPOILER;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xCC120A06, 0xE60B0604);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.animTimeSec += delta / 20.0F;
        this.pendingTooltipStack = null;
        this.pendingTooltipLines = null;
        this.hoveredNodeIndex = -1;
        this.stageBoxW = 0;
        this.guideBtnW = 0;

        // Suavizado de paneo y zoom
        float smooth = Math.min(1.0F, delta * 0.32F);
        if (!draggingMap) {
            this.panX = MathHelper.lerp(smooth, this.panX, this.targetPanX);
            this.panY = MathHelper.lerp(smooth, this.panY, this.targetPanY);
        }
        this.zoom = MathHelper.lerp(smooth, this.zoom, this.targetZoom);

        this.renderBackground(context, mouseX, mouseY, delta);

        // Cubierta exterior de madera oscura con Broches de Escarlata
        EncyclopediaTheme.drawBookOuterFrame(context, winX, winY, winW, winH);

        renderTopHeader(context, mouseX, mouseY);
        renderMapViewport(context, mouseX, mouseY);
        EncyclopediaTheme.drawBookSpine(context, spineX, mapViewY, mapViewH);
        renderInspectionPage(context, mouseX, mouseY);
        renderBottomFooter(context);

        if (pendingTooltipStack != null && !pendingTooltipStack.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, pendingTooltipStack, mouseX, mouseY);
        } else if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
            context.drawTooltip(this.textRenderer, pendingTooltipLines, mouseX, mouseY);
        }
    }

    private void renderTopHeader(DrawContext context, int mouseX, int mouseY) {
        EncyclopediaTheme.drawScaledItem(context, new ItemStack(ModItem.SCARLET_SWORD), winX + 22, winY + 17.0F, 1.05F);
        Text title = Text.literal("PROGRESIÓN DE DUNGEONS").formatted(Formatting.BOLD);
        context.drawTextWithShadow(this.textRenderer, title, winX + 36, winY + 11, EncyclopediaTheme.GOLD_BRIGHT);

        int completed = ClientDungeonProgression.getCompletedCount();
        int total = ClientDungeonProgression.getTotalImplementedDungeons();
        String progressBadge = "Conquistadas: " + completed + " / " + total;
        int titleW = this.textRenderer.getWidth(title);
        if (winX + 44 + titleW + this.textRenderer.getWidth(progressBadge) < centerBtnX - 8) {
            context.drawText(this.textRenderer, "· " + progressBadge, winX + 42 + titleW, winY + 11, EncyclopediaTheme.CREAM_SECONDARY, false);
        }

        // Botón Centrar Mapa
        boolean hoverCenter = isOver(mouseX, mouseY, centerBtnX, centerBtnY, centerBtnW, centerBtnH);
        EncyclopediaTheme.drawMinecraftButton(context, centerBtnX, centerBtnY, centerBtnW, centerBtnH, hoverCenter, ButtonStyle.WOOD);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "⌖ Centrar",
                centerBtnX + centerBtnW / 2.0F, centerBtnY + 5.0F, 0.90F,
                hoverCenter ? EncyclopediaTheme.GOLD_BRIGHT : EncyclopediaTheme.CREAM_PRIMARY, true
        );

        // Botón Volver a Enciclopedia
        boolean hoverBack = isOver(mouseX, mouseY, backBtnX, backBtnY, backBtnW, backBtnH);
        EncyclopediaTheme.drawMinecraftButton(context, backBtnX, backBtnY, backBtnW, backBtnH, hoverBack, ButtonStyle.PARCHMENT);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "← Enciclopedia",
                backBtnX + backBtnW / 2.0F, backBtnY + 5.0F, 0.92F,
                EncyclopediaTheme.INK_TITLE, false
        );

        // Botón Cerrar
        boolean hoverClose = isOver(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        EncyclopediaTheme.drawMinecraftButton(context, closeBtnX, closeBtnY, closeBtnW, closeBtnH, hoverClose, ButtonStyle.SCARLET_GOLD);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "✕ Cerrar",
                closeBtnX + closeBtnW / 2.0F, closeBtnY + 5.0F, 0.92F,
                0xFFFFFFFF, true
        );
    }

    private void renderMapViewport(DrawContext context, int mouseX, int mouseY) {
        EncyclopediaTheme.drawParchmentPage(context, mapViewX, mapViewY, mapViewW, mapViewH);

        int innerX = mapViewX + 4;
        int innerY = mapViewY + 4;
        int innerW = mapViewW - 8;
        int innerH = mapViewH - 8;

        context.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        float originX = innerX + innerW / 2.0F + panX;
        float originY = innerY + innerH / 2.0F + panY;

        // 1. Cuadrícula cartográfica sutil que se mueve con el arrastre del mapa
        drawCartographicGrid(context, innerX, innerY, innerW, innerH, originX, originY);

        // 2. Rosa de los vientos decorativa en la esquina del pergamino
        drawCompassWatermark(context, innerX + 26, innerY + innerH - 28);

        // 3. Líneas conectoras ortogonales estilo Advancements de Minecraft
        for (int i = 0; i < NODES.size() - 1; i++) {
            ProgressionNode from = NODES.get(i);
            ProgressionNode to = NODES.get(i + 1);
            int x1 = Math.round(originX + from.mapX() * zoom);
            int y1 = Math.round(originY + from.mapY() * zoom);
            int x2 = Math.round(originX + to.mapX() * zoom);
            int y2 = Math.round(originY + to.mapY() * zoom);

            NodeVisualState fromState = getNodeState(from);
            NodeVisualState toState = getNodeState(to);
            boolean lit = (fromState == NodeVisualState.COMPLETED);
            boolean futureDash = (toState == NodeVisualState.FUTURE_DEV);
            drawAdvancementConnection(context, x1, y1, x2, y2, lit, futureDash);
        }

        // 4. Nodos de cada Dungeon con retratos/siluetas 3D
        int scaledSize = Math.max(38, Math.round(NODE_BASE_SIZE * zoom));
        boolean mouseInsideMap = isOver(mouseX, mouseY, innerX, innerY, innerW, innerH);

        for (int i = 0; i < NODES.size(); i++) {
            ProgressionNode node = NODES.get(i);
            int cx = Math.round(originX + node.mapX() * zoom);
            int cy = Math.round(originY + node.mapY() * zoom);
            int nx = cx - scaledSize / 2;
            int ny = cy - scaledSize / 2;

            if (nx + scaledSize < innerX - 20 || nx > innerX + innerW + 20
                    || ny + scaledSize < innerY - 20 || ny > innerY + innerH + 20) {
                continue;
            }

            NodeVisualState state = getNodeState(node);
            boolean selected = (i == selectedNodeIndex);
            boolean hovered = mouseInsideMap && isOver(mouseX, mouseY, nx, ny, scaledSize, scaledSize);
            if (hovered) {
                this.hoveredNodeIndex = i;
                buildNodeHoverTooltip(node, state);
            }

            EncyclopediaTheme.drawAdvancementNodeFrame(
                    context, nx, ny, scaledSize,
                    node.frameKind(), state, hovered, selected, animTimeSec
            );

            // Contenido interior del nodo: Modelo 3D a color, Silueta 3D oscura o Emblema Misterioso
            renderNodePortraitContent(context, node, state, nx, ny, scaledSize, innerX, innerY, innerX + innerW, innerY + innerH);

            // Sello de número romano arriba del nodo
            String stepTag = "Etapa " + node.romanStep();
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, stepTag,
                    cx, ny - 10, 0.78F, EncyclopediaTheme.INK_MUTED, false
            );

            // Etiqueta debajo del nodo (respetando Modo Sin Spoilers)
            String displayTitle = (state == NodeVisualState.COMPLETED || state == NodeVisualState.UNLOCKED_NEXT)
                    ? node.revealedTitle()
                    : "???";
            int pillW = Math.round(this.textRenderer.getWidth(displayTitle) * 0.82F) + 10;
            int pillX = cx - pillW / 2;
            int pillY = ny + scaledSize + 5;
            int pillBg = selected ? EncyclopediaTheme.WOOD_DARK : 0xDDF3E6CA;
            int pillBorder = selected ? EncyclopediaTheme.GOLD_MAIN : EncyclopediaTheme.PARCHMENT_BORDER;
            int pillText = selected ? EncyclopediaTheme.GOLD_BRIGHT : EncyclopediaTheme.INK_TITLE;
            context.fill(pillX, pillY, pillX + pillW, pillY + 12, pillBg);
            context.drawBorder(pillX, pillY, pillW, 12, pillBorder);
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, displayTitle,
                    cx, pillY + 2.5F, 0.82F, pillText, selected
            );

            // Insignia de estado en la esquina inferior derecha del marco
            if (state == NodeVisualState.COMPLETED) {
                int bx = nx + scaledSize - 11;
                int by = ny + scaledSize - 11;
                context.fill(bx, by, bx + 12, by + 12, EncyclopediaTheme.WOOD_OUTER_BORDER);
                context.fill(bx + 1, by + 1, bx + 11, by + 11, EncyclopediaTheme.EMERALD_MAIN);
                EncyclopediaTheme.drawCenteredScaledText(context, this.textRenderer, "✔", bx + 6.0F, by + 2.0F, 0.90F, 0xFFFFFFFF, true);
            } else if (state == NodeVisualState.LOCKED_SPOILER || state == NodeVisualState.FUTURE_DEV) {
                EncyclopediaTheme.drawPixelPadlock(context, nx + scaledSize - 5, ny + scaledSize - 5);
            }
        }

        context.disableScissor();

        // Cinta informativa flotante en la esquina superior izquierda del mapa
        context.fill(innerX + 6, innerY + 6, innerX + 214, innerY + 20, 0xDD382214);
        context.drawBorder(innerX + 6, innerY + 6, 208, 14, EncyclopediaTheme.GOLD_DEEP);
        EncyclopediaTheme.drawScaledText(
                context, this.textRenderer,
                "✥ Arrastra el mapa · Clic en un nodo",
                innerX + 12, innerY + 9.5F, 0.82F, EncyclopediaTheme.CREAM_PRIMARY, false
        );

        // Leyenda rápida en la parte baja del mapa
        int legY = innerY + innerH - 18;
        int legW = Math.min(innerW - 12, 276);
        int legX = innerX + innerW - legW - 6;
        context.fill(legX, legY, legX + legW, legY + 14, 0xE6EFE1C4);
        context.drawBorder(legX, legY, legW, 14, EncyclopediaTheme.PARCHMENT_BORDER);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, "✔ Derrotado", legX + 6, legY + 3.5F, 0.78F, EncyclopediaTheme.EMERALD_DARK, false);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, "⚔ Pendiente", legX + 78, legY + 3.5F, 0.78F, EncyclopediaTheme.INK_SCARLET, false);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, "🔒 Sin Spoilers (???)", legX + 150, legY + 3.5F, 0.78F, EncyclopediaTheme.INK_MUTED, false);
    }

    private void drawCartographicGrid(DrawContext context, int vx, int vy, int vw, int vh, float originX, float originY) {
        int step = Math.max(24, Math.round(36 * zoom));
        int gridColor = 0x268A683E;
        int majorColor = 0x428A683E;

        int startX = vx + Math.floorMod(Math.round(originX) - vx, step);
        int idxX = 0;
        for (int gx = startX; gx < vx + vw; gx += step, idxX++) {
            context.fill(gx, vy, gx + 1, vy + vh, (idxX % 4 == 0) ? majorColor : gridColor);
        }

        int startY = vy + Math.floorMod(Math.round(originY) - vy, step);
        int idxY = 0;
        for (int gy = startY; gy < vy + vh; gy += step, idxY++) {
            context.fill(vx, gy, vx + vw, gy + 1, (idxY % 4 == 0) ? majorColor : gridColor);
        }
    }

    private void drawCompassWatermark(DrawContext context, int cx, int cy) {
        int ink = 0x55755432;
        context.fill(cx - 12, cy, cx + 13, cy + 1, ink);
        context.fill(cx, cy - 12, cx + 1, cy + 13, ink);
        context.drawBorder(cx - 7, cy - 7, 15, 15, ink);
        EncyclopediaTheme.drawCenteredScaledText(context, this.textRenderer, "N", cx + 0.5F, cy - 18, 0.75F, EncyclopediaTheme.INK_MUTED, false);
    }

    private void drawAdvancementConnection(DrawContext context, int x1, int y1, int x2, int y2, boolean illuminated, boolean dashed) {
        int midX = (x1 + x2) / 2;

        int borderCol = illuminated ? EncyclopediaTheme.WOOD_OUTER_BORDER : 0xAA5E462D;
        int coreCol = illuminated ? EncyclopediaTheme.GOLD_MAIN : 0xFF9E8360;

        if (dashed && !illuminated) {
            drawDashedHorizontal(context, x1, midX, y1, coreCol);
            drawDashedVertical(context, midX, Math.min(y1, y2), Math.max(y1, y2), coreCol);
            drawDashedHorizontal(context, midX, x2, y2, coreCol);
            return;
        }

        // Borde grueso de 5px estilo sendero de Advancements de Minecraft
        drawOrthogonalSegment(context, x1, y1, midX, y2, x2, 3, borderCol);
        // Canal interior de 3px
        drawOrthogonalSegment(context, x1, y1, midX, y2, x2, 1, coreCol);

        // Pulso luminoso viajando por la línea cuando el tramo ya fue conquistado
        if (illuminated) {
            float t = (animTimeSec * 0.65F) % 1.0F;
            float len1 = Math.abs(midX - x1);
            float len2 = Math.abs(y2 - y1);
            float len3 = Math.abs(x2 - midX);
            float total = Math.max(1.0F, len1 + len2 + len3);
            float dist = t * total;
            int px, py;
            if (dist <= len1) {
                px = MathHelper.lerp(dist / Math.max(1.0F, len1), x1, midX);
                py = y1;
            } else if (dist <= len1 + len2) {
                px = midX;
                py = MathHelper.lerp((dist - len1) / Math.max(1.0F, len2), y1, y2);
            } else {
                px = MathHelper.lerp((dist - len1 - len2) / Math.max(1.0F, len3), midX, x2);
                py = y2;
            }
            context.fill(px - 3, py - 3, px + 3, py + 3, EncyclopediaTheme.SCARLET_BRIGHT);
            context.fill(px - 2, py - 2, px + 2, py + 2, EncyclopediaTheme.GOLD_BRIGHT);
        }
    }

    private void drawOrthogonalSegment(DrawContext context, int x1, int y1, int midX, int y2, int x2, int halfThick, int color) {
        int xa = Math.min(x1, midX);
        int xb = Math.max(x1, midX);
        context.fill(xa, y1 - halfThick, xb + halfThick, y1 + halfThick + 1, color);

        int ya = Math.min(y1, y2);
        int yb = Math.max(y1, y2);
        context.fill(midX - halfThick, ya, midX + halfThick + 1, yb + halfThick + 1, color);

        int xc = Math.min(midX, x2);
        int xd = Math.max(midX, x2);
        context.fill(xc, y2 - halfThick, xd + halfThick, y2 + halfThick + 1, color);
    }

    private void drawDashedHorizontal(DrawContext context, int x1, int x2, int y, int color) {
        int a = Math.min(x1, x2);
        int b = Math.max(x1, x2);
        for (int x = a; x < b; x += 8) {
            context.fill(x, y - 1, Math.min(b, x + 4), y + 2, color);
        }
    }

    private void drawDashedVertical(DrawContext context, int x, int y1, int y2, int color) {
        for (int y = y1; y < y2; y += 8) {
            context.fill(x - 1, y, x + 2, Math.min(y2, y + 4), color);
        }
    }

    private void renderNodePortraitContent(
            DrawContext context,
            ProgressionNode node,
            NodeVisualState state,
            int nx,
            int ny,
            int size,
            int clipL,
            int clipT,
            int clipR,
            int clipB
    ) {
        int scL = Math.max(clipL, nx + 5);
        int scT = Math.max(clipT, ny + 5);
        int scR = Math.min(clipR, nx + size - 5);
        int scB = Math.min(clipB, ny + size - 5);
        if (scR <= scL || scB <= scT) {
            return;
        }

        boolean spoilerSilhouette = (state == NodeVisualState.LOCKED_SPOILER);
        if (node.bossEntitySupplier() != null && client != null && client.world != null) {
            LivingEntity entity = getOrCreatePreviewEntity(node.bossEntitySupplier().get());
            if (entity != null) {
                float entityScale = node.gridEntityScale() * (size / (float) NODE_BASE_SIZE);
                EncyclopediaTheme.renderEntityViewport(
                        context,
                        scL, scT, scR, scB,
                        nx + size / 2.0F, ny + size / 2.0F + 2.0F,
                        entityScale,
                        new Vector3f(0.0F, node.entityOffsetY(), 0.0F),
                        22.0F, 8.0F,
                        entity,
                        spoilerSilhouette
                );
                return;
            }
        }

        // Si el jefe aún no existe en código (Palacio Nacional o futuras dungeons), dibujar emblema de silueta misteriosa
        context.enableScissor(scL, scT, scR, scB);
        EncyclopediaTheme.drawMysteryBossEmblem(
                context, this.textRenderer,
                nx + size / 2, ny + size / 2 + 1,
                (size / (float) NODE_BASE_SIZE) * 1.10F,
                node.twinMysteryBosses() && state != NodeVisualState.LOCKED_SPOILER
        );
        context.disableScissor();
    }

    private void renderInspectionPage(DrawContext context, int mouseX, int mouseY) {
        EncyclopediaTheme.drawParchmentPage(context, detailX, detailY, detailW, detailH);

        ProgressionNode node = NODES.get(MathHelper.clamp(selectedNodeIndex, 0, NODES.size() - 1));
        NodeVisualState state = getNodeState(node);
        boolean revealed = (state == NodeVisualState.COMPLETED || state == NodeVisualState.UNLOCKED_NEXT);

        int pad = 12;
        int x = detailX + pad;
        int w = detailW - pad * 2;
        int y = detailY + 10;

        // 1. Cinta de Etapa y Título
        EncyclopediaTheme.drawScaledText(
                context, this.textRenderer,
                "ETAPA " + node.romanStep() + " DE PROGRESIÓN",
                x, y, 0.80F, EncyclopediaTheme.INK_MUTED, false
        );
        y += 11;

        String titleStr = revealed ? node.revealedTitle() : "???";
        context.drawText(this.textRenderer, Text.literal(titleStr).formatted(Formatting.BOLD), x, y, EncyclopediaTheme.INK_TITLE, false);
        y += 14;

        // 2. Sello de Estado Global
        y = drawGlobalStatusBanner(context, state, x, y, w);
        y += 8;

        // 3. Escenario de Retrato / Silueta 3D del Jefe
        int stageH = MathHelper.clamp((int) (detailH * 0.34F), 92, 138);
        this.stageBoxX = x;
        this.stageBoxY = y;
        this.stageBoxW = w;
        this.stageBoxH = stageH;

        if (revealed) {
            int alcBorder = state == NodeVisualState.COMPLETED ? EncyclopediaTheme.GOLD_MAIN : EncyclopediaTheme.SCARLET_MAIN;
            EncyclopediaTheme.drawDarkShowcaseAlcove(context, x, y, w, stageH, alcBorder);
        } else {
            // Fondo de pergamino sombreado para resaltar la silueta oscura en Modo Sin Spoilers
            context.fill(x, y, x + w, y + stageH, EncyclopediaTheme.WOOD_OUTER_BORDER);
            context.fillGradient(x + 2, y + 2, x + w - 2, y + stageH - 2, 0xFFD4BE98, 0xFFB0966E);
            context.drawBorder(x + 1, y + 1, w - 2, stageH - 2, EncyclopediaTheme.PARCHMENT_SHADOW);
        }

        if (node.bossEntitySupplier() != null && client != null && client.world != null) {
            LivingEntity entity = getOrCreatePreviewEntity(node.bossEntitySupplier().get());
            if (entity != null) {
                float sc = node.showcaseEntityScale() * (stageH / 122.0F);
                EncyclopediaTheme.renderEntityViewport(
                        context,
                        x + 3, y + 3, x + w - 3, y + stageH - 3,
                        x + w / 2.0F, y + stageH / 2.0F + 4.0F,
                        sc,
                        new Vector3f(0.0F, node.entityOffsetY(), 0.0F),
                        revealed ? previewYawDeg : 22.0F,
                        revealed ? previewPitchDeg : 8.0F,
                        entity,
                        !revealed
                );
            }
        } else {
            context.enableScissor(x + 3, y + 3, x + w - 3, y + stageH - 3);
            EncyclopediaTheme.drawMysteryBossEmblem(
                    context, this.textRenderer,
                    x + w / 2, y + stageH / 2 + 2,
                    2.25F,
                    revealed && node.twinMysteryBosses()
            );
            context.disableScissor();
        }

        String stageCaption = !revealed
                ? "🔒 Silueta Oculta (Modo Sin Spoilers)"
                : (node.bossEntitySupplier() != null ? "✦ Arrastra para rotar al jefe" : "✦ Jefes por revelar");
        EncyclopediaTheme.drawScaledText(
                context, this.textRenderer, stageCaption,
                x + 6, y + 5, 0.78F,
                revealed ? EncyclopediaTheme.CREAM_SECONDARY : EncyclopediaTheme.INK_TITLE, false
        );

        y += stageH + 10;

        // 4. Tarjeta de Jefe de la Dungeon
        String bossValue = revealed ? node.revealedBossLabel() : "???";
        y = drawInfoRowCard(context, "JEFE DE LA DUNGEON", bossValue, x, y, w,
                revealed ? EncyclopediaTheme.INK_SCARLET : EncyclopediaTheme.INK_MUTED);
        y += 6;

        // 5. Tarjeta de Llave de Invocación con ranura clásica de Minecraft
        y = drawSummonKeyCard(context, node, revealed, x, y, w, mouseX, mouseY);
        y += 10;

        // 6. Botón de Acceso a la Ficha en la Enciclopedia
        this.guideBtnH = 22;
        this.guideBtnW = w;
        this.guideBtnX = x;
        this.guideBtnY = Math.min(y, detailY + detailH - guideBtnH - 10);
        this.guideBtnEnabled = revealed && node.encyclopediaEntryId() != null;

        boolean hoverGuide = isOver(mouseX, mouseY, guideBtnX, guideBtnY, guideBtnW, guideBtnH);
        if (guideBtnEnabled) {
            EncyclopediaTheme.drawMinecraftButton(context, guideBtnX, guideBtnY, guideBtnW, guideBtnH, hoverGuide, ButtonStyle.SCARLET_GOLD);
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer,
                    "📖 Ver Ficha en la Enciclopedia →",
                    guideBtnX + guideBtnW / 2.0F, guideBtnY + 7.0F, 0.92F,
                    0xFFFFFFFF, true
            );
        } else {
            EncyclopediaTheme.drawMinecraftButton(context, guideBtnX, guideBtnY, guideBtnW, guideBtnH, false, ButtonStyle.WOOD);
            String lockedLabel = (state == NodeVisualState.FUTURE_DEV)
                    ? "⏳ Próximamente por desarrollar"
                    : "🔒 Derrota al jefe anterior para desbloquear";
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer,
                    lockedLabel,
                    guideBtnX + guideBtnW / 2.0F, guideBtnY + 7.0F, 0.80F,
                    EncyclopediaTheme.CREAM_MUTED, false
            );
        }
    }

    private int drawGlobalStatusBanner(DrawContext context, NodeVisualState state, int x, int y, int w) {
        int h = 18;
        int bg;
        int border;
        String text;
        int textColor;

        switch (state) {
            case COMPLETED -> {
                bg = EncyclopediaTheme.EMERALD_DARK;
                border = EncyclopediaTheme.GOLD_MAIN;
                text = "✔ ESTADO GLOBAL: DERROTADO";
                textColor = EncyclopediaTheme.EMERALD_BRIGHT;
            }
            case UNLOCKED_NEXT -> {
                bg = EncyclopediaTheme.SCARLET_DARK;
                border = EncyclopediaTheme.GOLD_MAIN;
                text = "⚔ ESTADO GLOBAL: PENDIENTE";
                textColor = EncyclopediaTheme.GOLD_BRIGHT;
            }
            case LOCKED_SPOILER -> {
                bg = EncyclopediaTheme.WOOD_DARK;
                border = EncyclopediaTheme.PARCHMENT_SHADOW;
                text = "🔒 ESTADO GLOBAL: PENDIENTE (OCULTO)";
                textColor = EncyclopediaTheme.CREAM_SECONDARY;
            }
            default -> {
                bg = 0xFF2B2420;
                border = 0xFF5C5049;
                text = "⏳ FUTURA DUNGEON EN DESARROLLO";
                textColor = EncyclopediaTheme.CREAM_MUTED;
            }
        }

        context.fill(x, y, x + w, y + h, bg);
        context.drawBorder(x, y, w, h, border);
        EncyclopediaTheme.drawCenteredScaledText(context, this.textRenderer, text, x + w / 2.0F, y + 5.0F, 0.85F, textColor, true);
        return y + h;
    }

    private int drawInfoRowCard(DrawContext context, String label, String value, int x, int y, int w, int valueColor) {
        List<OrderedText> wrapped = this.textRenderer.wrapLines(Text.literal(value), w - 16);
        int h = Math.max(30, 16 + wrapped.size() * 10);
        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, false, false);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, label, x + 7, y + 5, 0.76F, EncyclopediaTheme.INK_MUTED, false);
        int vy = y + 15;
        for (OrderedText line : wrapped) {
            context.drawText(this.textRenderer, line, x + 7, vy, valueColor, false);
            vy += 10;
        }
        return y + h;
    }

    private int drawSummonKeyCard(
            DrawContext context,
            ProgressionNode node,
            boolean revealed,
            int x,
            int y,
            int w,
            int mouseX,
            int mouseY
    ) {
        int h = 36;
        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, false, false);

        int slotSize = 24;
        int sx = x + 6;
        int sy = y + (h - slotSize) / 2;
        ItemStack keyStack = revealed ? node.keyStackSupplier().get() : ItemStack.EMPTY;
        boolean hoverSlot = !keyStack.isEmpty() && isOver(mouseX, mouseY, sx, sy, slotSize, slotSize);

        EncyclopediaTheme.drawMinecraftSlot(context, sx, sy, slotSize, hoverSlot, false, EncyclopediaTheme.GOLD_DEEP);
        if (!keyStack.isEmpty()) {
            EncyclopediaTheme.drawScaledItem(context, keyStack, sx + slotSize / 2.0F, sy + slotSize / 2.0F, hoverSlot ? 1.15F : 1.0F);
            if (hoverSlot) {
                this.pendingTooltipStack = keyStack;
            }
        } else {
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, "?",
                    sx + slotSize / 2.0F, sy + 7.0F, 1.0F, EncyclopediaTheme.INK_MUTED, false
            );
        }

        EncyclopediaTheme.drawScaledText(
                context, this.textRenderer, "LLAVE DE INVOCACIÓN",
                sx + slotSize + 8, y + 6, 0.76F, EncyclopediaTheme.INK_MUTED, false
        );
        String keyLabel = revealed ? node.revealedKeyName() : "???";
        List<OrderedText> keyLines = this.textRenderer.wrapLines(Text.literal(keyLabel), w - slotSize - 20);
        if (!keyLines.isEmpty()) {
            context.drawText(this.textRenderer, keyLines.get(0), sx + slotSize + 8, y + 18, EncyclopediaTheme.INK_TITLE, false);
        }
        return y + h;
    }

    private void renderBottomFooter(DrawContext context) {
        int fy = winY + winH - bottomBarH + 2;
        String hints = "[Arrastrar] Mover Mapa   [Rueda] Zoom   [← / →] Cambiar Etapa   [Backspace] Enciclopedia   [ESC] Cerrar";
        if (this.textRenderer.getWidth(hints) > winW - 28) {
            hints = "[Arrastrar] Mapa · [←/→] Etapa · [ESC] Cerrar";
        }
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, hints,
                winX + winW / 2.0F, fy + 3.0F, 0.84F, EncyclopediaTheme.CREAM_SECONDARY, true
        );
    }

    private void buildNodeHoverTooltip(ProgressionNode node, NodeVisualState state) {
        List<Text> lines = new ArrayList<>();
        boolean revealed = (state == NodeVisualState.COMPLETED || state == NodeVisualState.UNLOCKED_NEXT);
        lines.add(Text.literal("Etapa " + node.romanStep() + ": " + (revealed ? node.revealedTitle() : "???"))
                .formatted(Formatting.GOLD, Formatting.BOLD));
        switch (state) {
            case COMPLETED -> lines.add(Text.literal("✔ Jefe derrotado").formatted(Formatting.GREEN));
            case UNLOCKED_NEXT -> lines.add(Text.literal("⚔ Pendiente por derrotar").formatted(Formatting.RED));
            case LOCKED_SPOILER -> lines.add(Text.literal("🔒 Oculta hasta vencer la dungeon anterior").formatted(Formatting.GRAY));
            case FUTURE_DEV -> lines.add(Text.literal("⏳ Próximamente por desarrollar").formatted(Formatting.DARK_GRAY));
        }
        lines.add(Text.literal("Clic para inspeccionar").formatted(Formatting.YELLOW));
        this.pendingTooltipLines = lines;
    }

    @Nullable
    private LivingEntity getOrCreatePreviewEntity(EntityType<? extends LivingEntity> type) {
        if (client == null || client.world == null) {
            return null;
        }
        return entityCache.computeIfAbsent(type, t -> {
            Entity created = t.create(client.world, SpawnReason.LOAD);
            return created instanceof LivingEntity living ? living : null;
        });
    }

    private void selectAndFocusNode(int idx) {
        this.selectedNodeIndex = MathHelper.clamp(idx, 0, NODES.size() - 1);
        ProgressionNode node = NODES.get(this.selectedNodeIndex);
        this.targetPanX = -node.mapX() * this.targetZoom;
        this.targetPanY = -node.mapY() * this.targetZoom;
        this.previewYawDeg = 22.0F;
        this.previewPitchDeg = 8.0F;
        this.autoRotateBoss3D = true;
        playPageSound();
    }

    private void returnToEncyclopedia(@Nullable String focusEntryId) {
        playPageSound();
        if (this.client == null) {
            return;
        }
        if (focusEntryId != null) {
            this.client.setScreen(new EncyclopediaScreen(focusEntryId));
        } else if (this.parentScreen instanceof EncyclopediaScreen) {
            this.client.setScreen(this.parentScreen);
        } else {
            this.client.setScreen(new EncyclopediaScreen());
        }
    }

    private void playPageSound() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isOver(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH)) {
            this.close();
            return true;
        }

        if (isOver(mouseX, mouseY, backBtnX, backBtnY, backBtnW, backBtnH)) {
            returnToEncyclopedia(null);
            return true;
        }

        if (isOver(mouseX, mouseY, centerBtnX, centerBtnY, centerBtnW, centerBtnH)) {
            selectAndFocusNode(this.selectedNodeIndex);
            this.targetZoom = 1.0F;
            return true;
        }

        if (guideBtnEnabled && guideBtnW > 0 && isOver(mouseX, mouseY, guideBtnX, guideBtnY, guideBtnW, guideBtnH)) {
            ProgressionNode node = NODES.get(this.selectedNodeIndex);
            if (node.encyclopediaEntryId() != null) {
                returnToEncyclopedia(node.encyclopediaEntryId());
                return true;
            }
        }

        if (stageBoxW > 0 && isOver(mouseX, mouseY, stageBoxX, stageBoxY, stageBoxW, stageBoxH)) {
            NodeVisualState state = getNodeState(NODES.get(selectedNodeIndex));
            if (state == NodeVisualState.COMPLETED || state == NodeVisualState.UNLOCKED_NEXT) {
                this.draggingBossStage = true;
                return true;
            }
        }

        if (isOver(mouseX, mouseY, mapViewX + 4, mapViewY + 4, mapViewW - 8, mapViewH - 8)) {
            this.draggingMap = true;
            this.movedDuringDrag = false;
            this.dragStartMouseX = mouseX;
            this.dragStartMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        if (draggingBossStage) {
            this.autoRotateBoss3D = false;
            this.previewYawDeg = (this.previewYawDeg + (float) deltaX * 1.3F) % 360.0F;
            this.previewPitchDeg = MathHelper.clamp(this.previewPitchDeg - (float) deltaY * 0.7F, -45.0F, 55.0F);
            return true;
        }

        if (draggingMap) {
            if (Math.hypot(mouseX - dragStartMouseX, mouseY - dragStartMouseY) > 4.0D) {
                this.movedDuringDrag = true;
            }
            this.panX = MathHelper.clamp(this.panX + (float) deltaX, -520.0F, 520.0F);
            this.panY = MathHelper.clamp(this.panY + (float) deltaY, -220.0F, 220.0F);
            this.targetPanX = this.panX;
            this.targetPanY = this.panY;
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggingMap && !movedDuringDrag && hoveredNodeIndex >= 0) {
                selectAndFocusNode(hoveredNodeIndex);
            }
            this.draggingMap = false;
            this.draggingBossStage = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isOver(mouseX, mouseY, mapViewX, mapViewY, mapViewW, mapViewH)) {
            this.targetZoom = MathHelper.clamp(this.targetZoom + (float) verticalAmount * 0.10F, 0.75F, 1.35F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Prueba_no1Client.getEncyclopediaKey() != null
                && Prueba_no1Client.getEncyclopediaKey().matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            returnToEncyclopedia(null);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            selectAndFocusNode(Math.max(0, selectedNodeIndex - 1));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            selectAndFocusNode(Math.min(NODES.size() - 1, selectedNodeIndex + 1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
