package pruebano1.modid.client.encyclopedia;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import pruebano1.modid.Prueba_no1Client;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.DropInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EffectInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EntityPreviewConfig;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.ObtainingMethod;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.ObtainingType;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.SlotRef;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StatInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureFeatureInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureStepInfo;
import pruebano1.modid.entity.JefriPensteinEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interactive opaque dark-slate encyclopedia covering all Lon Terry mod items,
 * recipes, 3D animated entities, and a standalone Structures guide section.
 */
public class EncyclopediaScreen extends Screen {

    // High-contrast, solid/rich obsidian-slate palette (no washed-out transparency)
    private static final int BG_WINDOW = 0xFF0E141F;
    private static final int BG_TOP_TOP = 0xFF1C283C;
    private static final int BG_TOP_BOT = 0xFF131C2B;
    private static final int BG_SIDEBAR = 0xFF121B2A;
    private static final int BG_PANEL = 0xFF141D2D;
    private static final int BG_CARD = 0xFF1B273A;
    private static final int BG_CARD_HOVER = 0xFF263752;
    private static final int BG_SLOT = 0xFF182336;
    private static final int BG_INPUT = 0xFF0C121D;

    private static final int BORDER_SUBTLE = 0xFF38516E;
    private static final int BORDER_OUTER = 0xFF4A6B8F;
    private static final int BORDER_ACTIVE = 0xFF2CE6D0;
    private static final int ACCENT_CRIMSON = 0xFFE53E2D;
    private static final int ACCENT_CRIMSON_BRIGHT = 0xFFFF5A4A;
    private static final int ACCENT_TEAL = 0xFF2CE6C8;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFE2ECF6;
    private static final int TEXT_MUTED = 0xFFB0C4DA;

    private static final int SIDEBAR_CARD_GAP = 3;
    private static final int SIDEBAR_SECTION_DIVIDER_H = 14;

    private static EncyclopediaCategory lastCategory = EncyclopediaCategory.ALL;
    @Nullable
    private static String lastSelectedId = null;

    private EncyclopediaCategory selectedCategory = lastCategory;
    private String searchQuery = "";
    private List<EncyclopediaEntry> filteredEntries = List.of();
    @Nullable
    private EncyclopediaEntry selectedEntry;
    @Nullable
    private EncyclopediaEntry hoveredGridEntry;

    private TextFieldWidget searchField;

    // Layout geometry
    private int winX, winY, winW, winH;
    private final int topBarH = 26;
    private final int bottomBarH = 18;
    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int centerX, centerY, centerW, centerH;
    private int gridX, gridY, gridW, gridH;
    private int detailX, detailY, detailW, detailH;

    // Close & clear hitboxes
    private int closeBtnX, closeBtnY, closeBtnW, closeBtnH;
    private int searchBoxX, searchBoxY, searchBoxW, searchBoxH;
    private int clearSearchX, clearSearchY, clearSearchSize;

    // Scroll state
    private int sidebarScroll = 0;
    private int sidebarMaxScroll = 0;
    private int gridScroll = 0;
    private int gridMaxScroll = 0;
    private int detailScroll = 0;
    private int detailMaxScroll = 0;

    // Smooth hover magnification state
    private final Map<String, Float> gridHoverAnim = new HashMap<>();
    private final Map<EncyclopediaCategory, Float> sidebarHoverAnim = new HashMap<>();
    private float itemShowcaseHoverAnim = 0.0F;

    // 3D entity preview state
    private final Map<EntityType<?>, LivingEntity> entityCache = new HashMap<>();
    private float previewYawDeg = 20.0F;
    private float previewPitchDeg = 8.0F;
    private boolean autoRotate3D = true;
    private boolean jefriDevilModePreview = false;
    private boolean draggingStage3D = false;
    private int stageBoxX, stageBoxY, stageBoxW, stageBoxH;

    private float animTimeSec = 0.0F;
    private int currentMouseX = 0;
    private int currentMouseY = 0;
    @Nullable
    private ItemStack pendingTooltipStack = null;
    @Nullable
    private List<Text> pendingTooltipLines = null;

    private final List<HitTarget> detailTargets = new ArrayList<>();

    private record HitTarget(
            int x, int y, int w, int h,
            @Nullable Runnable onClick,
            @Nullable ItemStack tooltipStack,
            @Nullable List<Text> customTooltip
    ) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record GridSlotDraw(EncyclopediaEntry entry, int x, int y, int size, float hoverAnim, boolean selected) {
    }

    public EncyclopediaScreen() {
        super(Text.literal("Enciclopedia Lon Terry"));
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();

        String prevText = this.searchField != null ? this.searchField.getText() : this.searchQuery;
        this.searchField = new TextFieldWidget(
                this.textRenderer,
                searchBoxX + 18,
                searchBoxY + (searchBoxH - 8) / 2,
                Math.max(40, searchBoxW - 34),
                12,
                Text.literal("Buscar elemento")
        );
        this.searchField.setMaxLength(48);
        this.searchField.setDrawsBackground(false);
        this.searchField.setEditableColor(TEXT_PRIMARY);
        this.searchField.setPlaceholder(Text.literal("Buscar...").formatted(Formatting.GRAY));
        this.searchField.setText(prevText);
        this.searchField.setChangedListener(text -> {
            this.searchQuery = text;
            this.gridScroll = 0;
            refreshFilteredEntries(false);
        });
        addSelectableChild(this.searchField);

        refreshFilteredEntries(true);
    }

    private void computeLayout() {
        int marginX = width >= 680 ? 12 : 6;
        int marginY = height >= 400 ? 10 : 6;
        this.winW = Math.min(width - marginX * 2, 1080);
        this.winH = Math.min(height - marginY * 2, 640);
        this.winX = (width - winW) / 2;
        this.winY = (height - winH) / 2;

        this.closeBtnW = 62;
        this.closeBtnH = 16;
        this.closeBtnX = winX + winW - closeBtnW - 8;
        this.closeBtnY = winY + (topBarH - closeBtnH) / 2;

        int bodyY = winY + topBarH;
        int bodyH = winH - topBarH - bottomBarH;

        this.sidebarW = MathHelper.clamp((int) (winW * 0.12F), 68, 88);
        this.sidebarX = winX + 1;
        this.sidebarY = bodyY;
        this.sidebarH = bodyH;

        int remainingW = winW - sidebarW - 2;
        this.centerW = MathHelper.clamp((int) (remainingW * 0.43F), 175, 420);
        this.centerX = sidebarX + sidebarW;
        this.centerY = bodyY;
        this.centerH = bodyH;

        this.searchBoxX = centerX + 8;
        this.searchBoxY = centerY + 24;
        this.searchBoxW = centerW - 16;
        this.searchBoxH = 18;
        this.clearSearchSize = 14;
        this.clearSearchX = searchBoxX + searchBoxW - clearSearchSize - 2;
        this.clearSearchY = searchBoxY + (searchBoxH - clearSearchSize) / 2;

        this.gridX = centerX + 8;
        this.gridY = searchBoxY + searchBoxH + 8;
        this.gridW = centerW - 16;
        this.gridH = centerY + centerH - gridY - 6;

        this.detailX = centerX + centerW;
        this.detailY = bodyY;
        this.detailW = winX + winW - 1 - detailX;
        this.detailH = bodyH;
    }

    private void refreshFilteredEntries(boolean tryRestoreLast) {
        this.filteredEntries = EncyclopediaRegistry.filter(this.selectedCategory, this.searchQuery);
        if (this.filteredEntries.isEmpty()) {
            this.selectedEntry = null;
            return;
        }

        if (tryRestoreLast && lastSelectedId != null) {
            for (EncyclopediaEntry entry : filteredEntries) {
                if (entry.getId().equals(lastSelectedId)) {
                    selectEntry(entry, false);
                    return;
                }
            }
        }

        if (this.selectedEntry != null && this.filteredEntries.contains(this.selectedEntry)) {
            return;
        }
        selectEntry(this.filteredEntries.get(0), false);
    }

    private void selectCategory(EncyclopediaCategory category) {
        this.selectedCategory = category;
        lastCategory = category;
        this.gridScroll = 0;
        refreshFilteredEntries(false);
    }

    private void selectEntry(@Nullable EncyclopediaEntry entry, boolean resetDetailScroll) {
        if (entry == null) {
            return;
        }
        this.selectedEntry = entry;
        lastSelectedId = entry.getId();
        if (resetDetailScroll) {
            this.detailScroll = 0;
        }
        EntityPreviewConfig preview = entry.getEntityPreview();
        if (preview != null) {
            this.previewYawDeg = preview.defaultYawDeg();
            this.previewPitchDeg = preview.defaultPitchDeg();
            if (!preview.supportsDevilMode()) {
                this.jefriDevilModePreview = false;
            }
        }
    }

    private void navigateToEntryId(@Nullable String targetId) {
        if (targetId == null) {
            return;
        }
        EncyclopediaEntry target = EncyclopediaRegistry.getById(targetId);
        if (target == null) {
            return;
        }
        if (target.getCategory() == EncyclopediaCategory.STRUCTURES) {
            this.selectedCategory = EncyclopediaCategory.STRUCTURES;
            lastCategory = EncyclopediaCategory.STRUCTURES;
        } else if (!target.belongsTo(this.selectedCategory)
                || !target.matchesQuery(EncyclopediaEntry.normalizeForSearch(this.searchQuery))) {
            this.selectedCategory = EncyclopediaCategory.ALL;
            lastCategory = EncyclopediaCategory.ALL;
        }
        if (this.searchField != null && !this.searchQuery.isEmpty()) {
            this.searchQuery = "";
            this.searchField.setText("");
        }
        this.filteredEntries = EncyclopediaRegistry.filter(this.selectedCategory, "");
        selectEntry(target, true);
        ensureSelectedVisibleInGrid();
    }

    private void ensureSelectedVisibleInGrid() {
        if (selectedEntry == null || filteredEntries.isEmpty()) {
            return;
        }
        int idx = filteredEntries.indexOf(selectedEntry);
        if (idx < 0) {
            return;
        }
        int slotStep = getSlotSize() + getSlotGap();
        int cols = getGridColumns();
        int row = idx / cols;
        int rowTop = row * slotStep;
        int rowBottom = rowTop + getSlotSize();
        if (rowTop < gridScroll) {
            gridScroll = rowTop;
        } else if (rowBottom > gridScroll + gridH) {
            gridScroll = rowBottom - gridH;
        }
        gridScroll = MathHelper.clamp(gridScroll, 0, Math.max(0, gridMaxScroll));
    }

    @Override
    public void tick() {
        super.tick();
        if (autoRotate3D && !draggingStage3D && selectedEntry != null && selectedEntry.isEntityEntry()) {
            previewYawDeg = (previewYawDeg + 1.15F) % 360.0F;
        }
        for (LivingEntity cached : entityCache.values()) {
            if (cached != null) {
                cached.age++;
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0xF406080E, 0xFA090D16);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.animTimeSec += delta / 20.0F;
        this.currentMouseX = mouseX;
        this.currentMouseY = mouseY;
        this.pendingTooltipStack = null;
        this.pendingTooltipLines = null;
        this.hoveredGridEntry = null;
        this.stageBoxW = 0;

        // Render background ONCE before our panels (calling super.render at the end would re-run renderBackground on top!)
        this.renderBackground(context, mouseX, mouseY, delta);

        // Main window container
        context.fill(winX, winY, winX + winW, winY + winH, BG_WINDOW);
        context.drawBorder(winX, winY, winW, winH, BORDER_OUTER);

        renderTopBar(context, mouseX, mouseY);
        renderSidebar(context, mouseX, mouseY, delta);
        renderCenterPanel(context, mouseX, mouseY, delta);
        renderDetailPanel(context, mouseX, mouseY, delta);
        renderBottomBar(context);

        // Render hovered tooltip above everything
        if (pendingTooltipStack != null && !pendingTooltipStack.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, pendingTooltipStack, mouseX, mouseY);
        } else if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
            context.drawTooltip(this.textRenderer, pendingTooltipLines, mouseX, mouseY);
        } else if (hoveredGridEntry != null) {
            renderGridEntryTooltip(context, hoveredGridEntry, mouseX, mouseY);
        }
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY) {
        context.fillGradient(winX + 1, winY + 1, winX + winW - 1, winY + topBarH, BG_TOP_TOP, BG_TOP_BOT);
        context.fill(winX + 1, winY + topBarH - 1, winX + winW - 1, winY + topBarH, BORDER_SUBTLE);
        context.fill(winX + 1, winY + topBarH - 2, winX + 165, winY + topBarH - 1, ACCENT_CRIMSON);

        drawScaledItem(context, new ItemStack(Items.KNOWLEDGE_BOOK), winX + 16, winY + topBarH / 2.0F, 1.0F);
        Text brandText = Text.literal("LON TERRY").formatted(Formatting.BOLD);
        int brandX = winX + 29;
        context.drawTextWithShadow(this.textRenderer, brandText, brandX, winY + 9, TEXT_PRIMARY);
        int brandW = this.textRenderer.getWidth(brandText);
        String topSub = selectedCategory == EncyclopediaCategory.STRUCTURES
                ? "· Apartado de Estructuras"
                : "· Enciclopedia del Mod";
        context.drawText(this.textRenderer, topSub, brandX + brandW + 6, winY + 9, TEXT_MUTED, false);

        int mainTotal = EncyclopediaRegistry.filter(EncyclopediaCategory.ALL, "").size();
        String countStr = selectedCategory == EncyclopediaCategory.STRUCTURES
                ? "Estructuras: " + filteredEntries.size()
                : (filteredEntries.size() == mainTotal ? "Items: " + mainTotal : "Mostrando: " + filteredEntries.size() + " / " + mainTotal);
        int subW = this.textRenderer.getWidth(countStr);
        context.drawText(this.textRenderer, countStr, closeBtnX - subW - 10, winY + 9, TEXT_SECONDARY, false);

        boolean hoverClose = isOver(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        int closeBg = hoverClose ? 0xFF4F1D27 : 0xFF1D273A;
        int closeBorder = hoverClose ? ACCENT_CRIMSON_BRIGHT : BORDER_SUBTLE;
        context.fill(closeBtnX, closeBtnY, closeBtnX + closeBtnW, closeBtnY + closeBtnH, closeBg);
        context.drawBorder(closeBtnX, closeBtnY, closeBtnW, closeBtnH, closeBorder);
        drawCenteredScaledText(context, "✕ CERRAR", closeBtnX + closeBtnW / 2.0F, closeBtnY + 4.5F, 0.95F, hoverClose ? 0xFFFFFFFF : TEXT_PRIMARY);
    }

    private void renderBottomBar(DrawContext context) {
        int barY = winY + winH - bottomBarH;
        context.fill(winX + 1, barY, winX + winW - 1, winY + winH - 1, 0xFF101622);
        context.fill(winX + 1, barY, winX + winW - 1, barY + 1, BORDER_SUBTLE);

        StringBuilder hints = new StringBuilder("[G] Abrir/Cerrar   [↑↓←→] Navegar   [ESC] Cerrar");
        if (selectedEntry != null && selectedEntry.isEntityEntry()) {
            hints.append("   [Arrastrar] Rotar 3D");
        }
        context.drawText(this.textRenderer, hints.toString(), winX + 10, barY + 5, TEXT_MUTED, false);

        String rightLabel = "Mod Lon Terry · Fabric 1.21.4";
        int rw = this.textRenderer.getWidth(rightLabel);
        if (winW > 420) {
            context.drawText(this.textRenderer, rightLabel, winX + winW - rw - 10, barY + 5, TEXT_MUTED, false);
        }
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, BG_SIDEBAR);
        context.fill(sidebarX + sidebarW - 1, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, BORDER_SUBTLE);

        drawCenteredScaledText(context, "CATEGORÍAS", sidebarX + sidebarW / 2.0F, sidebarY + 6, 0.85F, TEXT_SECONDARY);

        int listTop = sidebarY + 18;
        int listBottom = sidebarY + sidebarH - 4;
        EncyclopediaCategory[] cats = EncyclopediaCategory.values();
        int cardH = getSidebarCardHeight();
        int totalH = getSidebarTotalHeight(cats, cardH);
        this.sidebarMaxScroll = Math.max(0, totalH - (listBottom - listTop));
        this.sidebarScroll = MathHelper.clamp(this.sidebarScroll, 0, this.sidebarMaxScroll);

        context.enableScissor(sidebarX, listTop, sidebarX + sidebarW - 1, listBottom);
        int cardW = sidebarW - 10;
        int cardX = sidebarX + 5;
        float hoverStep = Math.min(1.0F, delta * 0.45F);

        for (int i = 0; i < cats.length; i++) {
            EncyclopediaCategory cat = cats[i];
            int cy = listTop + getSidebarCardTop(cats, i, cardH) - sidebarScroll;

            if (cat.isSeparateSection() && i > 0 && !cats[i - 1].isSeparateSection()) {
                int divY = cy - SIDEBAR_SECTION_DIVIDER_H;
                context.fill(cardX + 2, divY + 3, cardX + cardW - 2, divY + 4, BORDER_SUBTLE);
                drawCenteredScaledText(context, "APARTADO", cardX + cardW / 2.0F, divY + 6, 0.72F, ACCENT_TEAL);
            }

            if (cy + cardH < listTop || cy > listBottom) {
                continue;
            }
            boolean active = (cat == selectedCategory);
            boolean hover = mouseY >= listTop && mouseY < listBottom && isOver(mouseX, mouseY, cardX, cy, cardW, cardH);

            float prevAnim = sidebarHoverAnim.getOrDefault(cat, 0.0F);
            float hoverAnim = MathHelper.lerp(hoverStep, prevAnim, hover ? 1.0F : 0.0F);
            sidebarHoverAnim.put(cat, hoverAnim);

            int bg = active ? 0xFF1D354D : (hover ? BG_CARD_HOVER : BG_CARD);
            int border = active ? BORDER_ACTIVE : (hover ? 0xFF5D87B3 : BORDER_SUBTLE);
            context.fill(cardX, cy, cardX + cardW, cy + cardH, bg);
            context.drawBorder(cardX, cy, cardW, cardH, border);
            if (active) {
                context.fill(cardX + 1, cy + 1, cardX + 3, cy + cardH - 1, cat.isSeparateSection() ? EncyclopediaRarity.EPIC.getHighlightBorderColor() : ACCENT_TEAL);
            }

            float baseIconScale = cardH >= 30 ? 0.90F : 0.74F;
            float iconScale = baseIconScale * (1.0F + 0.22F * hoverAnim);
            drawScaledItem(context, cat.getIcon(), cardX + cardW / 2.0F, cy + cardH * 0.36F, iconScale);
            float labelScale = Math.min(cardH >= 30 ? 0.76F : 0.68F, (cardW - 6.0F) / Math.max(1.0F, textRenderer.getWidth(cat.getShortLabel())));
            drawCenteredScaledText(
                    context,
                    cat.getShortLabel(),
                    cardX + cardW / 2.0F,
                    cy + cardH * 0.67F,
                    Math.max(0.55F, labelScale),
                    active ? 0xFFFFFFFF : (hover ? TEXT_PRIMARY : TEXT_SECONDARY)
            );
        }
        context.disableScissor();

        if (sidebarMaxScroll > 0) {
            drawScrollbar(context, sidebarX + sidebarW - 4, listTop, listBottom - listTop, sidebarScroll, sidebarMaxScroll, listBottom - listTop);
        }
    }

    private int getSidebarCardHeight() {
        int listH = Math.max(120, (sidebarY + sidebarH - 4) - (sidebarY + 18));
        EncyclopediaCategory[] cats = EncyclopediaCategory.values();
        int fitH = (listH - SIDEBAR_SECTION_DIVIDER_H - (cats.length - 1) * SIDEBAR_CARD_GAP) / cats.length;
        return MathHelper.clamp(fitH, 25, 34);
    }

    private int getSidebarCardTop(EncyclopediaCategory[] cats, int index, int cardH) {
        int y = 0;
        for (int i = 0; i < index; i++) {
            y += cardH + SIDEBAR_CARD_GAP;
        }
        if (cats[index].isSeparateSection()) {
            y += SIDEBAR_SECTION_DIVIDER_H;
        }
        return y;
    }

    private int getSidebarTotalHeight(EncyclopediaCategory[] cats, int cardH) {
        if (cats.length == 0) {
            return 0;
        }
        return getSidebarCardTop(cats, cats.length - 1, cardH) + cardH;
    }

    private void renderCenterPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(centerX, centerY, centerX + centerW, centerY + centerH, BG_PANEL);
        context.fill(centerX + centerW - 1, centerY, centerX + centerW, centerY + centerH, BORDER_SUBTLE);

        context.drawText(this.textRenderer, "✦", centerX + 8, centerY + 7, ACCENT_TEAL, false);
        context.drawText(this.textRenderer, selectedCategory.getHeaderTitle().toUpperCase(), centerX + 17, centerY + 7, TEXT_PRIMARY, false);
        String countText = filteredEntries.size() + (selectedCategory == EncyclopediaCategory.STRUCTURES ? " zona" : " items");
        int cw = this.textRenderer.getWidth(countText);
        context.drawText(this.textRenderer, countText, centerX + centerW - cw - 10, centerY + 7, TEXT_MUTED, false);

        boolean searchFocused = this.searchField != null && this.searchField.isFocused();
        context.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH, BG_INPUT);
        context.drawBorder(searchBoxX, searchBoxY, searchBoxW, searchBoxH, searchFocused ? BORDER_ACTIVE : BORDER_SUBTLE);
        drawScaledItem(context, new ItemStack(Items.SPYGLASS), searchBoxX + 9.0F, searchBoxY + searchBoxH / 2.0F, 0.65F);
        if (this.searchField != null) {
            this.searchField.render(context, mouseX, mouseY, delta);
        }

        if (!this.searchQuery.isEmpty()) {
            boolean hoverClear = isOver(mouseX, mouseY, clearSearchX, clearSearchY, clearSearchSize, clearSearchSize);
            context.fill(clearSearchX, clearSearchY, clearSearchX + clearSearchSize, clearSearchY + clearSearchSize, hoverClear ? 0xFF3E1E28 : 0xFF1A2538);
            drawCenteredScaledText(context, "×", clearSearchX + clearSearchSize / 2.0F, clearSearchY + 3.0F, 1.0F, hoverClear ? 0xFFFFFFFF : TEXT_SECONDARY);
        }

        if (filteredEntries.isEmpty()) {
            drawCenteredScaledText(context, "Sin coincidencias", centerX + centerW / 2.0F, gridY + 36, 1.0F, TEXT_MUTED);
            this.gridMaxScroll = 0;
            return;
        }

        int slotSize = getSlotSize();
        int slotGap = getSlotGap();
        int cols = getGridColumns();
        int rows = (filteredEntries.size() + cols - 1) / cols;
        int totalH = rows * slotSize + Math.max(0, rows - 1) * slotGap;
        this.gridMaxScroll = Math.max(0, totalH - gridH);
        this.gridScroll = MathHelper.clamp(this.gridScroll, 0, this.gridMaxScroll);

        int gridBottom = gridY + gridH;
        float hoverStep = Math.min(1.0F, delta * 0.45F);
        List<GridSlotDraw> deferredHoveredDraws = new ArrayList<>();

        context.enableScissor(gridX - 3, gridY - 2, gridX + gridW + 3, gridBottom + 2);

        for (int i = 0; i < filteredEntries.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int sx = gridX + col * (slotSize + slotGap);
            int sy = gridY + row * (slotSize + slotGap) - gridScroll;

            if (sy + slotSize < gridY || sy > gridBottom) {
                continue;
            }

            EncyclopediaEntry entry = filteredEntries.get(i);
            boolean isSelected = entry.equals(this.selectedEntry);
            boolean isHovered = mouseY >= gridY && mouseY < gridBottom && isOver(mouseX, mouseY, sx, sy, slotSize, slotSize);
            if (isHovered) {
                this.hoveredGridEntry = entry;
            }

            float prevAnim = gridHoverAnim.getOrDefault(entry.getId(), 0.0F);
            float hoverAnim = MathHelper.lerp(hoverStep, prevAnim, isHovered ? 1.0F : 0.0F);
            if (hoverAnim < 0.005F && !isHovered) {
                hoverAnim = 0.0F;
            }
            gridHoverAnim.put(entry.getId(), hoverAnim);

            GridSlotDraw slotDraw = new GridSlotDraw(entry, sx, sy, slotSize, hoverAnim, isSelected);
            if (hoverAnim > 0.01F) {
                drawGridSlotBackground(context, slotDraw);
                deferredHoveredDraws.add(slotDraw);
            } else {
                drawGridSlotBackground(context, slotDraw);
                drawGridSlotContent(context, slotDraw);
            }
        }

        // Draw magnified hovered slots last so enlarged items sit cleanly above neighboring slots
        for (GridSlotDraw slotDraw : deferredHoveredDraws) {
            drawGridSlotBackground(context, slotDraw);
            drawGridSlotContent(context, slotDraw);
        }

        context.disableScissor();

        if (gridMaxScroll > 0) {
            drawScrollbar(context, gridX + gridW - 4, gridY, gridH, gridScroll, gridMaxScroll, gridH);
        }
    }

    private void drawGridSlotBackground(DrawContext context, GridSlotDraw slot) {
        EncyclopediaRarity rarity = slot.entry().getRarity();
        int expand = slot.hoverAnim() > 0.35F ? 1 : 0;
        int x1 = slot.x() - expand;
        int y1 = slot.y() - expand;
        int x2 = slot.x() + slot.size() + expand;
        int y2 = slot.y() + slot.size() + expand;

        int bg = slot.selected() ? 0xFF21334E : (slot.hoverAnim() > 0.2F ? BG_CARD_HOVER : BG_SLOT);
        int border = rarity.getDynamicBorderColor(slot.selected() || slot.hoverAnim() > 0.2F, animTimeSec);

        context.fill(x1, y1, x2, y2, bg);
        context.drawBorder(x1, y1, x2 - x1, y2 - y1, border);
        if (slot.selected()) {
            context.drawBorder(x1 + 1, y1 + 1, x2 - x1 - 2, y2 - y1 - 2, (border & 0x00FFFFFF) | 0x99000000);
        }
    }

    private void drawGridSlotContent(DrawContext context, GridSlotDraw slot) {
        float zoom = 1.0F + 0.30F * slot.hoverAnim();
        float cx = slot.x() + slot.size() / 2.0F;
        float cy = slot.y() + slot.size() / 2.0F;

        if (slot.entry().isEntityEntry() && client != null && client.world != null) {
            EntityPreviewConfig preview = slot.entry().getEntityPreview();
            float miniScale = preview.gridScale() * (slot.size() / 34.0F) * zoom;
            renderEntityViewport(
                    context,
                    slot.x(), slot.y(), slot.x() + slot.size(), slot.y() + slot.size(),
                    cx, cy + 2.0F,
                    miniScale, preview,
                    preview.defaultYawDeg(), preview.defaultPitchDeg(), false
            );
        } else {
            float baseScale = (slot.size() - 8) / 16.0F * 0.85F;
            drawScaledItem(context, slot.entry().getIcon(), cx, cy, baseScale * zoom);
        }
    }

    private void renderDetailPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        this.detailTargets.clear();
        context.fill(detailX, detailY, detailX + detailW, detailY + detailH, BG_PANEL);

        if (selectedEntry == null) {
            drawCenteredScaledText(context, "Selecciona un elemento para ver sus detalles", detailX + detailW / 2.0F, detailY + detailH / 2.0F, 1.0F, TEXT_MUTED);
            return;
        }

        int pad = 10;
        int contentX = detailX + pad;
        int contentW = detailW - pad * 2 - 6;
        int viewTop = detailY + 6;
        int viewBottom = detailY + detailH - 6;
        int viewH = viewBottom - viewTop;

        context.enableScissor(detailX + 1, viewTop, detailX + detailW - 1, viewBottom);

        int y = viewTop - detailScroll;
        EncyclopediaRarity rarity = selectedEntry.getRarity();

        // 1. Header & Prominent Visual Stage (3D Entity OR Large Enlarged Item Showcase)
        if (selectedEntry.isEntityEntry()) {
            y = renderEntityDetailHeader(context, selectedEntry, rarity, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
        } else {
            y = renderItemDetailHeader(context, selectedEntry, rarity, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom, delta);
        }
        y += 8;

        // 2. Description box with strong crimson left accent
        List<OrderedText> descLines = this.textRenderer.wrapLines(Text.literal(selectedEntry.getDescription()), contentW - 16);
        int descH = Math.max(24, descLines.size() * 10 + 12);
        context.fill(contentX, y, contentX + contentW, y + descH, BG_CARD);
        context.drawBorder(contentX, y, contentW, descH, BORDER_SUBTLE);
        context.fill(contentX + 1, y + 1, contentX + 4, y + descH - 1, ACCENT_CRIMSON);
        int lineY = y + 6;
        for (OrderedText line : descLines) {
            context.drawText(this.textRenderer, line, contentX + 10, lineY, TEXT_SECONDARY, false);
            lineY += 10;
        }
        y += descH + 10;

        // 3. Stats grid (2 columns)
        List<StatInfo> stats = selectedEntry.getStats();
        if (!stats.isEmpty()) {
            y = drawSectionHeader(context, "ESTADÍSTICAS", contentX, y);
            int colGap = 6;
            int cardW = (contentW - colGap) / 2;
            int cardH = 26;
            for (int i = 0; i < stats.size(); i += 2) {
                drawStatCard(context, stats.get(i), contentX, y, cardW, cardH);
                if (i + 1 < stats.size()) {
                    drawStatCard(context, stats.get(i + 1), contentX + cardW + colGap, y, cardW, cardH);
                }
                y += cardH + 5;
            }
            y += 5;
        }

        // 4. Dedicated Structures Content Section
        List<StructureFeatureInfo> structureContents = selectedEntry.getStructureContents();
        if (!structureContents.isEmpty()) {
            y = drawSectionHeader(context, "QUÉ CONTIENE LA ESTRUCTURA", contentX, y);
            for (StructureFeatureInfo feature : structureContents) {
                y = drawStructureFeatureCard(context, feature, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // 5. Dedicated Structures Conquest / Victory Strategy Section
        List<StructureStepInfo> structureSteps = selectedEntry.getStructureSteps();
        if (!structureSteps.isEmpty()) {
            y = drawSectionHeader(context, "CÓMO EXPLORARLA Y VENCERLA", contentX, y);
            for (StructureStepInfo step : structureSteps) {
                y = drawStructureStepCard(context, step, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // 6. Effects / abilities
        List<EffectInfo> effects = selectedEntry.getEffects();
        if (!effects.isEmpty()) {
            y = drawSectionHeader(context, "EFECTOS / HABILIDADES", contentX, y);
            for (EffectInfo effect : effects) {
                y = drawEffectCard(context, effect, contentX, y, contentW);
            }
            y += 5;
        }

        // 7. Drops / Draftea rewards
        List<DropInfo> drops = selectedEntry.getDrops();
        if (!drops.isEmpty()) {
            y = drawSectionHeader(context, "DROPS / PREMIOS", contentX, y);
            for (DropInfo drop : drops) {
                y = drawDropCard(context, drop, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // 8. Crafting / smelting / obtaining
        List<ObtainingMethod> methods = selectedEntry.getObtainingMethods();
        if (!methods.isEmpty()) {
            String sectionTitle = selectedEntry.getCategory() == EncyclopediaCategory.STRUCTURES
                    ? "LOCALIZACIÓN EN EL MUNDO"
                    : "CRAFTEO / OBTENCIÓN";
            y = drawSectionHeader(context, sectionTitle, contentX, y);
            for (ObtainingMethod method : methods) {
                y = drawObtainingCard(context, method, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
        }

        context.disableScissor();

        int totalContentH = (y + detailScroll) - viewTop + 8;
        this.detailMaxScroll = Math.max(0, totalContentH - viewH);
        this.detailScroll = MathHelper.clamp(this.detailScroll, 0, this.detailMaxScroll);

        if (detailMaxScroll > 0) {
            drawScrollbar(context, detailX + detailW - 5, viewTop, viewH, detailScroll, detailMaxScroll, viewH);
        }
    }

    /**
     * Renders a prominent large item showcase on the right detail panel so the selected item can be inspected clearly.
     */
    private int renderItemDetailHeader(
            DrawContext context, EncyclopediaEntry entry, EncyclopediaRarity rarity,
            int x, int y, int width, int mouseX, int mouseY, int viewTop, int viewBottom, float delta
    ) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(entry.getName()).formatted(Formatting.BOLD), x, y + 1, TEXT_PRIMARY);
        int badgeW = drawRarityBadge(context, rarity, x, y + 14);
        context.drawText(this.textRenderer, entry.getTypeLabel(), x + badgeW + 6, y + 16, TEXT_MUTED, false);

        if (entry.getRelatedEntryId() != null && entry.getRelatedButtonLabel() != null) {
            String btnLabel = entry.getRelatedButtonLabel();
            int btnW = this.textRenderer.getWidth(btnLabel) + 12;
            drawActionPill(
                    context, x + width - btnW, y + 12, btnLabel,
                    0xFF1A2D42, BORDER_ACTIVE, 0xFF8FF5FF,
                    () -> navigateToEntryId(entry.getRelatedEntryId()),
                    mouseX, mouseY, viewTop, viewBottom
            );
        }

        int showcaseY = y + 31;
        int showcaseH = MathHelper.clamp((int) (detailH * 0.30F), 86, 120);
        boolean hoverShowcase = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, showcaseY, width, showcaseH);
        this.itemShowcaseHoverAnim = MathHelper.lerp(Math.min(1.0F, delta * 0.45F), this.itemShowcaseHoverAnim, hoverShowcase ? 1.0F : 0.0F);

        int border = rarity.getDynamicBorderColor(hoverShowcase, animTimeSec);
        context.fillGradient(x, showcaseY, x + width, showcaseY + showcaseH, 0xFF0D1422, 0xFF1A273E);
        context.drawBorder(x, showcaseY, width, showcaseH, border);
        context.drawBorder(x + 1, showcaseY + 1, width - 2, showcaseH - 2, (border & 0x00FFFFFF) | 0x44000000);

        // Glowing showcase pedestal line below the large item
        int pedestalY = showcaseY + showcaseH - 16;
        int pedestalColor = (rarity.getHighlightBorderColor() & 0x00FFFFFF) | 0x66000000;
        context.fill(x + 28, pedestalY, x + width - 28, pedestalY + 1, pedestalColor);
        context.fill(x + 48, pedestalY + 4, x + width - 48, pedestalY + 5, (pedestalColor & 0x00FFFFFF) | 0x33000000);

        drawScaledText(context, "✦ VISTA DETALLADA", x + 6, showcaseY + 5, 0.80F, TEXT_MUTED);

        float largeScale = MathHelper.clamp(showcaseH * 0.046F, 3.6F, 5.2F) * (1.0F + 0.12F * itemShowcaseHoverAnim);
        drawScaledItem(context, entry.getIcon(), x + width / 2.0F, showcaseY + showcaseH / 2.0F - 2.0F, largeScale);
        registerDetailTarget(x, showcaseY, width, showcaseH, null, entry.getIcon(), null, viewTop, viewBottom);

        return showcaseY + showcaseH;
    }

    private int renderEntityDetailHeader(
            DrawContext context, EncyclopediaEntry entry, EncyclopediaRarity rarity,
            int x, int y, int width, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        context.drawTextWithShadow(this.textRenderer, Text.literal(entry.getName()).formatted(Formatting.BOLD), x, y + 1, TEXT_PRIMARY);
        int badgeW = drawRarityBadge(context, rarity, x, y + 14);
        context.drawText(this.textRenderer, entry.getTypeLabel(), x + badgeW + 6, y + 16, TEXT_MUTED, false);

        if (entry.getRelatedEntryId() != null && entry.getRelatedButtonLabel() != null) {
            String btnLabel = entry.getRelatedButtonLabel();
            int btnW = this.textRenderer.getWidth(btnLabel) + 12;
            drawActionPill(
                    context, x + width - btnW, y + 12, btnLabel,
                    0xFF1A2D42, BORDER_SUBTLE, TEXT_SECONDARY,
                    () -> navigateToEntryId(entry.getRelatedEntryId()),
                    mouseX, mouseY, viewTop, viewBottom
            );
        }

        int stageY = y + 32;
        int stageH = MathHelper.clamp((int) (detailH * 0.36F), 112, 152);
        this.stageBoxX = x;
        this.stageBoxY = stageY;
        this.stageBoxW = width;
        this.stageBoxH = stageH;

        int border = rarity.getDynamicBorderColor(draggingStage3D, animTimeSec);
        context.fillGradient(x, stageY, x + width, stageY + stageH, 0xFF0C121E, 0xFF1A273D);
        context.drawBorder(x, stageY, width, stageH, border);

        int floorY = stageY + stageH - 22;
        context.fill(x + 14, floorY, x + width - 14, floorY + 1, 0x662CE6C8);
        context.fill(x + 28, floorY + 7, x + width - 28, floorY + 8, 0x332CE6C8);

        EntityPreviewConfig preview = entry.getEntityPreview();
        if (preview != null && client != null && client.world != null) {
            float dynamicScale = preview.showcaseScale() * (stageH / 128.0F);
            renderEntityViewport(
                    context,
                    x + 2, stageY + 2, x + width - 2, stageY + stageH - 2,
                    x + width / 2.0F, stageY + stageH / 2.0F + 2.0F,
                    dynamicScale, preview,
                    this.previewYawDeg, this.previewPitchDeg,
                    preview.supportsDevilMode() && this.jefriDevilModePreview
            );
        }

        drawScaledText(context, "✦ VISTA 3D INTERACTIVA · Arrastra para rotar", x + 6, stageY + 5, 0.80F, TEXT_MUTED);

        int btnY = stageY + stageH - 18;
        int bx = x + 6;
        String autoLabel = autoRotate3D ? "Auto: ON" : "Auto: OFF";
        int autoW = drawActionPill(
                context, bx, btnY, autoLabel,
                autoRotate3D ? 0xFF143C34 : 0xFF1A2538,
                autoRotate3D ? ACCENT_TEAL : BORDER_SUBTLE,
                autoRotate3D ? 0xFF7DFFE8 : TEXT_MUTED,
                () -> this.autoRotate3D = !this.autoRotate3D,
                mouseX, mouseY, viewTop, viewBottom
        );
        bx += autoW + 4;

        drawActionPill(
                context, bx, btnY, "Reset",
                0xFF1A2538, BORDER_SUBTLE, TEXT_SECONDARY,
                () -> {
                    if (preview != null) {
                        this.previewYawDeg = preview.defaultYawDeg();
                        this.previewPitchDeg = preview.defaultPitchDeg();
                    }
                    this.autoRotate3D = true;
                },
                mouseX, mouseY, viewTop, viewBottom
        );

        if (preview != null && preview.supportsDevilMode()) {
            String devilLabel = jefriDevilModePreview ? "Modo Diablo: ON" : "Modo Diablo: OFF";
            int dw = this.textRenderer.getWidth(devilLabel) + 12;
            drawActionPill(
                    context, x + width - dw - 6, btnY, devilLabel,
                    jefriDevilModePreview ? 0xFF4D1720 : 0xFF1A2538,
                    jefriDevilModePreview ? ACCENT_CRIMSON_BRIGHT : BORDER_SUBTLE,
                    jefriDevilModePreview ? 0xFFFF8888 : TEXT_MUTED,
                    () -> this.jefriDevilModePreview = !this.jefriDevilModePreview,
                    mouseX, mouseY, viewTop, viewBottom
            );
        }

        return stageY + stageH;
    }

    private void renderEntityViewport(
            DrawContext context,
            int scissorLeft, int scissorTop, int scissorRight, int scissorBottom,
            float centerX, float centerY, float scale,
            EntityPreviewConfig config,
            float yawDeg, float pitchDeg, boolean devilMode
    ) {
        if (scissorRight <= scissorLeft || scissorBottom <= scissorTop) {
            return;
        }
        LivingEntity entity = getOrCreatePreviewEntity(config.entityTypeSupplier().get());
        if (entity == null) {
            return;
        }

        entity.setPosition(0.0, 0.0, 0.0);
        entity.prevBodyYaw = 180.0F;
        entity.bodyYaw = 180.0F;
        entity.prevHeadYaw = 180.0F;
        entity.headYaw = 180.0F;
        entity.prevYaw = 180.0F;
        entity.setYaw(180.0F);
        entity.prevPitch = 0.0F;
        entity.setPitch(0.0F);

        if (entity instanceof JefriPensteinEntity jefri) {
            jefri.setClientPreviewDevilMode(devilMode);
        }

        float pitchRad = (float) Math.toRadians(pitchDeg);
        float yawRad = (float) Math.toRadians(yawDeg);
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateX(pitchRad)
                .rotateY(yawRad);
        Quaternionf cameraOverride = new Quaternionf().rotateX(pitchRad);
        Vector3f translation = new Vector3f(0.0F, config.offsetY(), 0.0F);

        context.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
        try {
            InventoryScreen.drawEntity(context, centerX, centerY, scale, translation, rotation, cameraOverride, entity);
        } catch (Throwable ignored) {
            // Protect UI from any single-frame third-party model exception
        } finally {
            context.disableScissor();
        }
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

    private int drawSectionHeader(DrawContext context, String title, int x, int y) {
        context.drawText(this.textRenderer, "✦", x, y, ACCENT_TEAL, false);
        context.drawText(this.textRenderer, title, x + 9, y, TEXT_SECONDARY, false);
        return y + 13;
    }

    private void drawStatCard(DrawContext context, StatInfo stat, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, BG_CARD);
        context.drawBorder(x, y, w, h, BORDER_SUBTLE);
        drawScaledText(context, stat.label(), x + 6, y + 4, 0.85F, TEXT_MUTED);
        drawScaledText(context, stat.value(), x + 6, y + 14, 0.92F, stat.valueColor());
    }

    private int drawStructureFeatureCard(
            DrawContext context, StructureFeatureInfo feature,
            int x, int y, int w, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        List<OrderedText> detailLines = this.textRenderer.wrapLines(Text.literal(feature.detail()), w - 38);
        int h = Math.max(30, 18 + detailLines.size() * 9);
        boolean clickable = feature.targetEntryId() != null;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);

        context.fill(x, y, x + w, y + h, hover && clickable ? BG_CARD_HOVER : BG_CARD);
        context.drawBorder(x, y, w, h, hover && clickable ? BORDER_ACTIVE : BORDER_SUBTLE);
        context.fill(x + 1, y + 1, x + 3, y + h - 1, ACCENT_TEAL);

        ItemStack icon = feature.getIcon();
        float iconScale = hover ? 1.15F : 0.95F;
        drawScaledItem(context, icon, x + 16, y + 15.0F, iconScale);

        context.drawTextWithShadow(this.textRenderer, Text.literal(feature.title()), x + 30, y + 5, TEXT_PRIMARY);
        if (clickable) {
            String linkTag = "Ver →";
            int lw = this.textRenderer.getWidth(linkTag);
            context.drawText(this.textRenderer, linkTag, x + w - lw - 6, y + 5, 0xFF7BE8FF, false);
        }

        int dy = y + 16;
        for (OrderedText line : detailLines) {
            context.drawText(this.textRenderer, line, x + 30, dy, TEXT_MUTED, false);
            dy += 9;
        }

        registerDetailTarget(
                x, y, w, h,
                clickable ? () -> navigateToEntryId(feature.targetEntryId()) : null,
                icon,
                null,
                viewTop, viewBottom
        );
        return y + h + 5;
    }

    private int drawStructureStepCard(
            DrawContext context, StructureStepInfo step,
            int x, int y, int w, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        ItemStack icon = step.getIcon();
        List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(step.instruction()), w - 42);
        int h = Math.max(icon.isEmpty() ? 32 : 44, 22 + lines.size() * 9);
        boolean clickable = step.targetEntryId() != null;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);

        context.fill(x, y, x + w, y + h, hover && clickable ? BG_CARD_HOVER : BG_CARD);
        context.drawBorder(x, y, w, h, hover && clickable ? BORDER_ACTIVE : BORDER_SUBTLE);

        // Numbered step badge
        int badgeSize = 14;
        int bx = x + 6;
        int by = y + 6;
        context.fill(bx, by, bx + badgeSize, by + badgeSize, ACCENT_CRIMSON);
        context.drawBorder(bx, by, badgeSize, badgeSize, 0xFFFF8A7A);
        drawCenteredScaledText(context, String.valueOf(step.stepNumber()), bx + badgeSize / 2.0F, by + 3.0F, 0.95F, 0xFFFFFFFF);

        if (!icon.isEmpty()) {
            drawScaledItem(context, icon, bx + badgeSize / 2.0F, by + badgeSize + 10.0F, hover ? 1.05F : 0.85F);
        }

        context.drawTextWithShadow(this.textRenderer, Text.literal(step.title()), x + 28, y + 6, EncyclopediaEntry.GOLD_STAT_COLOR);
        int ty = y + 17;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, x + 28, ty, TEXT_SECONDARY, false);
            ty += 9;
        }

        registerDetailTarget(
                x, y, w, h,
                clickable ? () -> navigateToEntryId(step.targetEntryId()) : null,
                icon,
                null,
                viewTop, viewBottom
        );
        return y + h + 5;
    }

    private int drawEffectCard(DrawContext context, EffectInfo effect, int x, int y, int w) {
        List<OrderedText> detailLines = this.textRenderer.wrapLines(Text.literal(effect.detail()), w - 16);
        int h = 16 + detailLines.size() * 9;
        context.fill(x, y, x + w, y + h, BG_CARD);
        context.drawBorder(x, y, w, h, BORDER_SUBTLE);
        context.fill(x + 1, y + 1, x + 3, y + h - 1, effect.accentColor());

        context.drawText(this.textRenderer, effect.name(), x + 8, y + 4, TEXT_PRIMARY, false);
        int dy = y + 14;
        for (OrderedText line : detailLines) {
            context.drawText(this.textRenderer, line, x + 8, dy, TEXT_MUTED, false);
            dy += 9;
        }
        return y + h + 4;
    }

    private int drawDropCard(
            DrawContext context, DropInfo drop,
            int x, int y, int w, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        int h = 24;
        boolean clickable = drop.targetEntryId() != null;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);

        context.fill(x, y, x + w, y + h, hover && clickable ? BG_CARD_HOVER : BG_CARD);
        context.drawBorder(x, y, w, h, hover && clickable ? BORDER_ACTIVE : BORDER_SUBTLE);

        ItemStack icon = drop.getIcon();
        drawScaledItem(context, icon, x + 13, y + h / 2.0F, hover ? 1.12F : 0.90F);
        context.drawText(this.textRenderer, drop.name(), x + 27, y + 8, TEXT_PRIMARY, false);

        String right = drop.detail() + (clickable ? " →" : "");
        int rw = this.textRenderer.getWidth(right);
        context.drawText(this.textRenderer, right, x + w - rw - 6, y + 8, clickable ? 0xFF7BE8FF : EncyclopediaEntry.GOLD_STAT_COLOR, false);

        registerDetailTarget(
                x, y, w, h,
                clickable ? () -> navigateToEntryId(drop.targetEntryId()) : null,
                icon,
                null,
                viewTop, viewBottom
        );
        return y + h + 4;
    }

    private int drawObtainingCard(
            DrawContext context, ObtainingMethod method,
            int x, int y, int w, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        int innerPad = 8;
        List<OrderedText> noteLines = method.note().isEmpty()
                ? List.of()
                : this.textRenderer.wrapLines(Text.literal(method.note()), w - innerPad * 2);

        if (method.type() == ObtainingType.CRAFTING_SHAPED || method.type() == ObtainingType.CRAFTING_SHAPELESS) {
            int slot = 22;
            int gap = 2;
            int gridSize = slot * 3 + gap * 2;
            boolean compact = w < 220;
            int contentH = compact ? (gridSize + 36) : Math.max(gridSize, 32);
            int noteH = noteLines.isEmpty() ? 0 : (noteLines.size() * 9 + 6);
            int cardH = 18 + contentH + noteH + 6;

            context.fill(x, y, x + w, y + cardH, BG_CARD);
            context.drawBorder(x, y, w, cardH, BORDER_SUBTLE);
            context.drawText(this.textRenderer, method.title(), x + innerPad, y + 5, TEXT_MUTED, false);

            int gx = x + innerPad;
            int gy = y + 17;
            for (int idx = 0; idx < 9; idx++) {
                SlotRef ref = idx < method.inputs().size() ? method.inputs().get(idx) : SlotRef.empty();
                int col = idx % 3;
                int row = idx / 3;
                drawRecipeSlot(context, ref, gx + col * (slot + gap), gy + row * (slot + gap), slot, mouseX, mouseY, viewTop, viewBottom);
            }

            int arrowX = compact ? (gx + 10) : (gx + gridSize + 10);
            int arrowY = compact ? (gy + gridSize + 10) : (gy + gridSize / 2 - 4);
            context.drawTextWithShadow(this.textRenderer, Text.literal("→"), arrowX, arrowY, EncyclopediaEntry.GOLD_STAT_COLOR);

            int outSize = 26;
            int outX = arrowX + 16;
            int outY = compact ? (gy + gridSize + 2) : (gy + (gridSize - outSize) / 2);
            drawRecipeSlot(context, method.output(), outX, outY, outSize, mouseX, mouseY, viewTop, viewBottom);

            if (!compact) {
                ItemStack outStack = method.output().getStack();
                if (!outStack.isEmpty()) {
                    int labelX = outX + outSize + 8;
                    int labelMaxW = Math.max(30, x + w - labelX - innerPad);
                    List<OrderedText> outLines = this.textRenderer.wrapLines(outStack.getName(), labelMaxW);
                    int ly = outY + 4;
                    for (OrderedText ol : outLines) {
                        context.drawText(this.textRenderer, ol, labelX, ly, TEXT_PRIMARY, false);
                        ly += 10;
                    }
                    if (outStack.getCount() > 1) {
                        context.drawText(this.textRenderer, "x" + outStack.getCount(), labelX, ly, EncyclopediaEntry.GOLD_STAT_COLOR, false);
                    }
                }
            }

            int ny = gy + contentH + 4;
            for (OrderedText nl : noteLines) {
                context.drawText(this.textRenderer, nl, x + innerPad, ny, TEXT_MUTED, false);
                ny += 9;
            }
            return y + cardH + 5;
        }

        if (method.type() == ObtainingType.SMELTING || method.type() == ObtainingType.BLASTING) {
            int slot = 24;
            int noteH = noteLines.isEmpty() ? 0 : (noteLines.size() * 9 + 4);
            int cardH = 18 + slot + 6 + noteH + 6;
            context.fill(x, y, x + w, y + cardH, BG_CARD);
            context.drawBorder(x, y, w, cardH, BORDER_SUBTLE);
            context.drawText(this.textRenderer, method.title(), x + innerPad, y + 5, TEXT_MUTED, false);

            int rowY = y + 17;
            SlotRef input = method.inputs().isEmpty() ? SlotRef.empty() : method.inputs().get(0);
            drawRecipeSlot(context, input, x + innerPad, rowY, slot, mouseX, mouseY, viewTop, viewBottom);
            context.drawText(this.textRenderer, "» →", x + innerPad + slot + 8, rowY + 8, 0xFFFFAE42, false);
            drawRecipeSlot(context, method.output(), x + innerPad + slot + 34, rowY, slot, mouseX, mouseY, viewTop, viewBottom);

            ItemStack outStack = method.output().getStack();
            if (!outStack.isEmpty()) {
                context.drawText(this.textRenderer, outStack.getName(), x + innerPad + slot + 64, rowY + 8, TEXT_PRIMARY, false);
            }

            int ny = rowY + slot + 5;
            for (OrderedText nl : noteLines) {
                context.drawText(this.textRenderer, nl, x + innerPad, ny, TEXT_MUTED, false);
                ny += 9;
            }
            return y + cardH + 5;
        }

        int slot = 24;
        List<OrderedText> infoWrapped = this.textRenderer.wrapLines(Text.literal(method.note()), w - innerPad * 2 - slot - 8);
        int textH = Math.max(slot, infoWrapped.size() * 9 + 4);
        int cardH = 18 + textH + 8;
        context.fill(x, y, x + w, y + cardH, BG_CARD);
        context.drawBorder(x, y, w, cardH, BORDER_SUBTLE);
        context.drawText(this.textRenderer, method.title(), x + innerPad, y + 5, TEXT_MUTED, false);

        SlotRef iconRef = method.inputs().isEmpty() ? SlotRef.empty() : method.inputs().get(0);
        drawRecipeSlot(context, iconRef, x + innerPad, y + 18, slot, mouseX, mouseY, viewTop, viewBottom);
        int ty = y + 19;
        for (OrderedText line : infoWrapped) {
            context.drawText(this.textRenderer, line, x + innerPad + slot + 8, ty, TEXT_SECONDARY, false);
            ty += 9;
        }
        return y + cardH + 5;
    }

    private void drawRecipeSlot(
            DrawContext context, SlotRef ref,
            int x, int y, int size, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        ItemStack stack = ref.getStack();
        boolean hasLink = ref.targetEntryId() != null && !stack.isEmpty();
        boolean hover = !stack.isEmpty() && mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, size, size);

        context.fill(x, y, x + size, y + size, hover ? BG_CARD_HOVER : BG_INPUT);
        int border = hover ? (hasLink ? BORDER_ACTIVE : 0xFF6B8FB8) : 0xFF334761;
        context.drawBorder(x, y, size, size, border);

        if (!stack.isEmpty()) {
            float zoom = hover ? 1.24F : 1.0F;
            drawScaledItem(context, stack, x + size / 2.0F, y + size / 2.0F, ((size - 4) / 16.0F) * zoom);
            if (stack.getCount() > 1) {
                String cnt = String.valueOf(stack.getCount());
                int cw = this.textRenderer.getWidth(cnt);
                context.drawTextWithShadow(this.textRenderer, cnt, x + size - cw - 1, y + size - 9, 0xFFFFFFFF);
            }
            registerDetailTarget(
                    x, y, size, size,
                    hasLink ? () -> navigateToEntryId(ref.targetEntryId()) : null,
                    stack,
                    null,
                    viewTop, viewBottom
            );
        }
    }

    private int drawRarityBadge(DrawContext context, EncyclopediaRarity rarity, int x, int y) {
        String label = rarity.getDisplayName();
        int w = Math.round(this.textRenderer.getWidth(label) * 0.85F) + 8;
        int h = 11;
        context.fill(x, y, x + w, y + h, rarity.getBadgeBackground());
        context.drawBorder(x, y, w, h, rarity.getBorderColor());
        drawCenteredScaledText(context, label, x + w / 2.0F, y + 2.5F, 0.85F, rarity.getTextColor());
        return w;
    }

    private int drawActionPill(
            DrawContext context, int x, int y, String label,
            int bgColor, int borderColor, int textColor, Runnable onClick,
            int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        int w = this.textRenderer.getWidth(label) + 12;
        int h = 14;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);
        context.fill(x, y, x + w, y + h, hover ? shiftBrightness(bgColor, 20) : bgColor);
        context.drawBorder(x, y, w, h, hover ? 0xFFFFFFFF : borderColor);
        drawCenteredScaledText(context, label, x + w / 2.0F, y + 3.5F, 0.92F, hover ? 0xFFFFFFFF : textColor);
        registerDetailTarget(x, y, w, h, onClick, null, null, viewTop, viewBottom);
        return w;
    }

    private void registerDetailTarget(
            int x, int y, int w, int h,
            @Nullable Runnable onClick,
            @Nullable ItemStack tooltipStack,
            @Nullable List<Text> customTooltip,
            int viewTop, int viewBottom
    ) {
        int cy1 = Math.max(y, viewTop);
        int cy2 = Math.min(y + h, viewBottom);
        if (cy2 <= cy1) {
            return;
        }
        HitTarget target = new HitTarget(x, cy1, w, cy2 - cy1, onClick, tooltipStack, customTooltip);
        this.detailTargets.add(target);
        if (target.contains(this.currentMouseX, this.currentMouseY)) {
            if (tooltipStack != null && !tooltipStack.isEmpty()) {
                this.pendingTooltipStack = tooltipStack;
            } else if (customTooltip != null) {
                this.pendingTooltipLines = customTooltip;
            }
        }
    }

    private void renderGridEntryTooltip(DrawContext context, EncyclopediaEntry entry, int mouseX, int mouseY) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(entry.getName()).formatted(Formatting.BOLD));
        lines.add(Text.literal(entry.getRarity().getDisplayName() + " · " + entry.getTypeLabel()).withColor(entry.getRarity().getTextColor()));
        if (entry.isEntityEntry()) {
            lines.add(Text.literal("Clic para inspeccionar en 3D").formatted(Formatting.AQUA));
        } else if (entry.getCategory() == EncyclopediaCategory.STRUCTURES) {
            lines.add(Text.literal("Clic para ver guía y contenido").formatted(Formatting.GOLD));
        }
        context.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
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

        if (!searchQuery.isEmpty() && isOver(mouseX, mouseY, clearSearchX, clearSearchY, clearSearchSize, clearSearchSize)) {
            this.searchQuery = "";
            if (this.searchField != null) {
                this.searchField.setText("");
                this.searchField.setFocused(true);
            }
            return true;
        }

        if (isOver(mouseX, mouseY, searchBoxX, searchBoxY, searchBoxW, searchBoxH)) {
            if (this.searchField != null) {
                this.searchField.setFocused(true);
                this.setFocused(this.searchField);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        } else if (this.searchField != null && this.searchField.isFocused()) {
            this.searchField.setFocused(false);
        }

        int listTop = sidebarY + 18;
        int listBottom = sidebarY + sidebarH - 4;
        if (mouseX >= sidebarX && mouseX < sidebarX + sidebarW && mouseY >= listTop && mouseY < listBottom) {
            EncyclopediaCategory[] cats = EncyclopediaCategory.values();
            int cardH = getSidebarCardHeight();
            int cardW = sidebarW - 10;
            int cardX = sidebarX + 5;
            for (int i = 0; i < cats.length; i++) {
                int cy = listTop + getSidebarCardTop(cats, i, cardH) - sidebarScroll;
                if (isOver(mouseX, mouseY, cardX, cy, cardW, cardH)) {
                    selectCategory(cats[i]);
                    return true;
                }
            }
        }

        if (mouseX >= gridX && mouseX < gridX + gridW && mouseY >= gridY && mouseY < gridY + gridH) {
            int slotSize = getSlotSize();
            int slotGap = getSlotGap();
            int cols = getGridColumns();
            for (int i = 0; i < filteredEntries.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int sx = gridX + col * (slotSize + slotGap);
                int sy = gridY + row * (slotSize + slotGap) - gridScroll;
                if (isOver(mouseX, mouseY, sx, sy, slotSize, slotSize)) {
                    selectEntry(filteredEntries.get(i), true);
                    return true;
                }
            }
        }

        for (int i = detailTargets.size() - 1; i >= 0; i--) {
            HitTarget target = detailTargets.get(i);
            if (target.onClick() != null && target.contains(mouseX, mouseY)) {
                target.onClick().run();
                return true;
            }
        }

        if (stageBoxW > 0 && isOver(mouseX, mouseY, stageBoxX, stageBoxY, stageBoxW, stageBoxH)) {
            this.draggingStage3D = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.draggingStage3D = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingStage3D && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.autoRotate3D = false;
            this.previewYawDeg = (this.previewYawDeg + (float) deltaX * 1.25F) % 360.0F;
            this.previewPitchDeg = MathHelper.clamp(this.previewPitchDeg - (float) deltaY * 0.70F, -45.0F, 55.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int step = (int) Math.round(-verticalAmount * 24.0);
        if (mouseX >= sidebarX && mouseX < sidebarX + sidebarW) {
            sidebarScroll = MathHelper.clamp(sidebarScroll + step, 0, sidebarMaxScroll);
            return true;
        }
        if (mouseX >= centerX && mouseX < centerX + centerW) {
            gridScroll = MathHelper.clamp(gridScroll + step, 0, gridMaxScroll);
            return true;
        }
        if (mouseX >= detailX && mouseX < detailX + detailW) {
            detailScroll = MathHelper.clamp(detailScroll + step, 0, detailMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.searchField.setFocused(false);
                this.setFocused(null);
                return true;
            }
            if (this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }

        if (Prueba_no1Client.getEncyclopediaKey() != null && Prueba_no1Client.getEncyclopediaKey().matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_SLASH || (keyCode == GLFW.GLFW_KEY_F && hasControlDown())) {
            if (this.searchField != null) {
                this.searchField.setFocused(true);
                this.setFocused(this.searchField);
                return true;
            }
        }

        if (!filteredEntries.isEmpty() && handleArrowNavigation(keyCode)) {
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleArrowNavigation(int keyCode) {
        int cols = getGridColumns();
        int idx = selectedEntry != null ? filteredEntries.indexOf(selectedEntry) : 0;
        if (idx < 0) {
            idx = 0;
        }
        int nextIdx = switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> idx - 1;
            case GLFW.GLFW_KEY_RIGHT -> idx + 1;
            case GLFW.GLFW_KEY_UP -> idx - cols;
            case GLFW.GLFW_KEY_DOWN -> idx + cols;
            default -> -1;
        };
        if (keyCode != GLFW.GLFW_KEY_LEFT && keyCode != GLFW.GLFW_KEY_RIGHT
                && keyCode != GLFW.GLFW_KEY_UP && keyCode != GLFW.GLFW_KEY_DOWN) {
            return false;
        }
        nextIdx = MathHelper.clamp(nextIdx, 0, filteredEntries.size() - 1);
        selectEntry(filteredEntries.get(nextIdx), true);
        ensureSelectedVisibleInGrid();
        return true;
    }

    private int getSlotSize() {
        return gridW >= 260 ? 36 : 32;
    }

    private int getSlotGap() {
        return 4;
    }

    private int getGridColumns() {
        int available = Math.max(32, gridW - 8);
        int step = getSlotSize() + getSlotGap();
        return Math.max(1, (available + getSlotGap()) / step);
    }

    private void drawScaledItem(DrawContext context, ItemStack stack, float centerX, float centerY, float scale) {
        if (stack.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 150.0F * (1.0F - scale));
        matrices.scale(scale, scale, scale);
        context.drawItem(stack, 0, 0);
        matrices.pop();
    }

    private void drawScaledText(DrawContext context, String text, float x, float y, float scale, int color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(this.textRenderer, text, 0, 0, color, false);
        matrices.pop();
    }

    private void drawCenteredScaledText(DrawContext context, String text, float centerX, float y, float scale, int color) {
        float w = this.textRenderer.getWidth(text) * scale;
        drawScaledText(context, text, centerX - w / 2.0F, y, scale, color);
    }

    private void drawScrollbar(DrawContext context, int x, int y, int trackH, int scroll, int maxScroll, int viewH) {
        if (maxScroll <= 0 || trackH <= 8) {
            return;
        }
        context.fill(x, y, x + 3, y + trackH, 0x881A2538);
        int thumbH = Math.max(14, (int) (trackH * (viewH / (float) (viewH + maxScroll))));
        int thumbY = y + (int) ((trackH - thumbH) * (scroll / (float) maxScroll));
        context.fill(x, thumbY, x + 3, thumbY + thumbH, 0xFF53759B);
    }

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int shiftBrightness(int argb, int delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = MathHelper.clamp(((argb >>> 16) & 0xFF) + delta, 0, 255);
        int g = MathHelper.clamp(((argb >>> 8) & 0xFF) + delta, 0, 255);
        int b = MathHelper.clamp((argb & 0xFF) + delta, 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
