package pruebano1.modid.client.encyclopedia;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
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
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.DropInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EffectInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EntityPreviewConfig;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.ObtainingMethod;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.ObtainingType;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.SlotRef;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StatInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureFeatureInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureStepInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaTheme.ButtonStyle;
import pruebano1.modid.entity.JefriPensteinEntity;
import pruebano1.modid.entity.JoseTorresEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enciclopedia interactiva de Lon Terry rediseñada con estética de Libro / Pergamino de Atril
 * de Minecraft, marcos de madera oscura biselada, ranuras clásicas de inventario, lomo cosido
 * y Broches de Escarlata y Oro propios del mod, además de acceso directo al Mapa de Progresión de Dungeons.
 */
public class EncyclopediaScreen extends Screen {

    private static final int SIDEBAR_CARD_GAP = 3;
    private static final int SIDEBAR_SECTION_DIVIDER_H = 14;

    private static EncyclopediaCategory lastCategory = EncyclopediaCategory.ALL;
    @Nullable
    private static String lastSelectedId = null;

    @Nullable
    private String pendingInitialEntryId;

    private EncyclopediaCategory selectedCategory = lastCategory;
    private String searchQuery = "";
    private List<EncyclopediaEntry> filteredEntries = List.of();
    @Nullable
    private EncyclopediaEntry selectedEntry;
    @Nullable
    private EncyclopediaEntry hoveredGridEntry;

    private TextFieldWidget searchField;

    // Geometría general del libro abierto
    private int winX, winY, winW, winH;
    private final int topBarH = 30;
    private final int bottomBarH = 20;
    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int centerX, centerY, centerW, centerH;
    private int spineX;
    private int gridX, gridY, gridW, gridH;
    private int detailX, detailY, detailW, detailH;

    // Controles superiores y búsqueda
    private int progressionBtnX, progressionBtnY, progressionBtnW, progressionBtnH;
    private int closeBtnX, closeBtnY, closeBtnW, closeBtnH;
    private int searchBoxX, searchBoxY, searchBoxW, searchBoxH;
    private int clearSearchX, clearSearchY, clearSearchSize;

    // Estado de desplazamiento (scroll)
    private int sidebarScroll = 0;
    private int sidebarMaxScroll = 0;
    private int gridScroll = 0;
    private int gridMaxScroll = 0;
    private int detailScroll = 0;
    private int detailMaxScroll = 0;

    // Animaciones suaves al pasar el cursor
    private final Map<String, Float> gridHoverAnim = new HashMap<>();
    private final Map<EncyclopediaCategory, Float> sidebarHoverAnim = new HashMap<>();
    private float itemShowcaseHoverAnim = 0.0F;

    // Vista previa 3D de entidades
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
    private final List<HitTarget> socialTargets = new ArrayList<>();

    private record SocialLink(String label, String url, int accentColor) {
    }

    private static final List<SocialLink> SOCIAL_LINKS = List.of(
            new SocialLink("Instagram", "https://www.instagram.com/milokgms?srtk=MXNlOHRmMjlqNWltaA==", 0xFFC2255C),
            new SocialLink("Canal Lon Terry", "https://whatsapp.com/channel/0029Vaz9S5VIiRowWW7Zwy2b", 0xFF1E8E48),
            new SocialLink("Canal Milofans", "https://whatsapp.com/channel/0029Vb7yf8e8fewuDpPhlO1b", 0xFF1E8E48)
    );

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

    private record GridSlotDraw(
            EncyclopediaEntry entry,
            int x,
            int y,
            int size,
            float hoverAnim,
            boolean selected,
            boolean spoilerLocked
    ) {
    }

    public EncyclopediaScreen() {
        this(null);
    }

    public EncyclopediaScreen(@Nullable String initialEntryId) {
        super(Text.literal("Enciclopedia Lon Terry"));
        this.pendingInitialEntryId = initialEntryId;
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
                Text.literal("Buscar en el códice")
        );
        this.searchField.setMaxLength(48);
        this.searchField.setDrawsBackground(false);
        this.searchField.setEditableColor(EncyclopediaTheme.INK_TITLE);
        this.searchField.setPlaceholder(Text.literal("Buscar en el códice...").withColor(EncyclopediaTheme.INK_MUTED));
        this.searchField.setText(prevText);
        this.searchField.setChangedListener(text -> {
            this.searchQuery = text;
            this.gridScroll = 0;
            refreshFilteredEntries(false);
        });
        addSelectableChild(this.searchField);

        if (this.pendingInitialEntryId != null) {
            String target = this.pendingInitialEntryId;
            this.pendingInitialEntryId = null;
            navigateToEntryId(target);
        } else {
            refreshFilteredEntries(true);
        }
    }

    private void computeLayout() {
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

        this.progressionBtnW = winW >= 620 ? 172 : 124;
        this.progressionBtnH = 18;
        this.progressionBtnX = closeBtnX - progressionBtnW - 8;
        this.progressionBtnY = closeBtnY;

        int bodyY = winY + topBarH + 2;
        int bodyH = winH - topBarH - bottomBarH - 6;

        this.sidebarW = MathHelper.clamp((int) (winW * 0.12F), 70, 90);
        this.sidebarX = winX + 8;
        this.sidebarY = bodyY;
        this.sidebarH = bodyH;

        int pagesLeft = sidebarX + sidebarW + 4;
        int pagesRight = winX + winW - 9;
        int pagesTotalW = pagesRight - pagesLeft;

        this.centerW = MathHelper.clamp((int) (pagesTotalW * 0.44F), 172, 420);
        this.centerX = pagesLeft;
        this.centerY = bodyY;
        this.centerH = bodyH;

        this.spineX = centerX + centerW + 3;

        this.detailX = spineX + 4;
        this.detailY = bodyY;
        this.detailW = pagesRight - detailX;
        this.detailH = bodyH;

        this.searchBoxX = centerX + 10;
        this.searchBoxY = centerY + 24;
        this.searchBoxW = centerW - 20;
        this.searchBoxH = 18;
        this.clearSearchSize = 14;
        this.clearSearchX = searchBoxX + searchBoxW - clearSearchSize - 2;
        this.clearSearchY = searchBoxY + (searchBoxH - clearSearchSize) / 2;

        this.gridX = centerX + 10;
        this.gridY = searchBoxY + searchBoxH + 7;
        this.gridW = centerW - 20;
        this.gridH = centerY + centerH - gridY - 8;
    }

    private void openDungeonProgressionMap() {
        playBookTurnSound();
        if (this.client != null) {
            this.client.setScreen(new DungeonProgressionScreen(this));
        }
    }

    private void playBookTurnSound() {
        if (this.client != null) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
        }
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
        if (this.selectedCategory != category) {
            playBookTurnSound();
        }
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
        context.fillGradient(0, 0, this.width, this.height, 0xCC120A06, 0xE60B0604);
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

        this.renderBackground(context, mouseX, mouseY, delta);

        // Cubierta exterior de madera oscura con los 4 Broches de Escarlata y Oro
        EncyclopediaTheme.drawBookOuterFrame(context, winX, winY, winW, winH);

        renderTopBar(context, mouseX, mouseY);
        renderSidebar(context, mouseX, mouseY, delta);
        renderCenterPage(context, mouseX, mouseY, delta);
        EncyclopediaTheme.drawBookSpine(context, spineX, centerY, centerH);
        renderDetailPage(context, mouseX, mouseY, delta);
        renderBottomBar(context);

        if (pendingTooltipStack != null && !pendingTooltipStack.isEmpty()) {
            context.drawItemTooltip(this.textRenderer, pendingTooltipStack, mouseX, mouseY);
        } else if (pendingTooltipLines != null && !pendingTooltipLines.isEmpty()) {
            context.drawTooltip(this.textRenderer, pendingTooltipLines, mouseX, mouseY);
        } else if (hoveredGridEntry != null) {
            renderGridEntryTooltip(context, hoveredGridEntry, mouseX, mouseY);
        }
    }

    private void renderTopBar(DrawContext context, int mouseX, int mouseY) {
        EncyclopediaTheme.drawScaledItem(context, new ItemStack(Items.WRITABLE_BOOK), winX + 22, winY + 17.0F, 1.05F);
        Text brandText = Text.literal("LON TERRY").formatted(Formatting.BOLD);
        int brandX = winX + 35;
        context.drawTextWithShadow(this.textRenderer, brandText, brandX, winY + 11, EncyclopediaTheme.GOLD_BRIGHT);
        int brandW = this.textRenderer.getWidth(brandText);
        String topSub = selectedCategory == EncyclopediaCategory.STRUCTURES
                ? "· Códice de Estructuras"
                : "· Códice & Enciclopedia";
        if (brandX + brandW + 8 + this.textRenderer.getWidth(topSub) < progressionBtnX - 10) {
            context.drawText(this.textRenderer, topSub, brandX + brandW + 6, winY + 11, EncyclopediaTheme.CREAM_SECONDARY, false);
        }

        // Botón destacado: Progresión de Dungeons
        boolean hoverProg = isOver(mouseX, mouseY, progressionBtnX, progressionBtnY, progressionBtnW, progressionBtnH);
        EncyclopediaTheme.drawMinecraftButton(
                context, progressionBtnX, progressionBtnY, progressionBtnW, progressionBtnH,
                hoverProg, ButtonStyle.SCARLET_GOLD
        );
        int comp = ClientDungeonProgression.getCompletedCount();
        int tot = ClientDungeonProgression.getTotalImplementedDungeons();
        String progLabel = progressionBtnW >= 165
                ? "⚔ PROGRESIÓN DUNGEONS (" + comp + "/" + tot + ")"
                : "⚔ PROGRESIÓN (" + comp + "/" + tot + ")";
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, progLabel,
                progressionBtnX + progressionBtnW / 2.0F, progressionBtnY + 5.0F, 0.86F,
                hoverProg ? EncyclopediaTheme.GOLD_BRIGHT : 0xFFFFFFFF, true
        );
        if (hoverProg) {
            this.pendingTooltipLines = List.of(
                    Text.literal("⚔ Mapa de Progresión de Dungeons").formatted(Formatting.GOLD, Formatting.BOLD),
                    Text.literal("Visualiza de forma dinámica el orden de jefes y tu avance").formatted(Formatting.YELLOW)
            );
        }

        // Botón Cerrar
        boolean hoverClose = isOver(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
        EncyclopediaTheme.drawMinecraftButton(context, closeBtnX, closeBtnY, closeBtnW, closeBtnH, hoverClose, ButtonStyle.WOOD);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "✕ Cerrar",
                closeBtnX + closeBtnW / 2.0F, closeBtnY + 5.0F, 0.90F,
                hoverClose ? EncyclopediaTheme.GOLD_BRIGHT : EncyclopediaTheme.CREAM_PRIMARY, true
        );
    }

    private void renderBottomBar(DrawContext context) {
        int barY = winY + winH - bottomBarH + 1;
        this.socialTargets.clear();
        int rightCursor = winX + winW - 14;

        if (winW > 460) {
            for (int i = SOCIAL_LINKS.size() - 1; i >= 0; i--) {
                SocialLink link = SOCIAL_LINKS.get(i);
                int btnW = this.textRenderer.getWidth(link.label()) + 12;
                int btnH = 13;
                int btnX = rightCursor - btnW;
                int btnY = barY + 2;
                boolean hovered = isOver(this.currentMouseX, this.currentMouseY, btnX, btnY, btnW, btnH);
                EncyclopediaTheme.drawMinecraftButton(context, btnX, btnY, btnW, btnH, hovered, ButtonStyle.PARCHMENT);
                context.drawText(
                        this.textRenderer, link.label(), btnX + 6, btnY + 3,
                        hovered ? EncyclopediaTheme.INK_SCARLET : link.accentColor(), false
                );
                this.socialTargets.add(new HitTarget(
                        btnX, btnY, btnW, btnH,
                        () -> ConfirmLinkScreen.open(this, link.url()),
                        null,
                        List.of(Text.literal(link.label()).formatted(Formatting.BOLD), Text.literal("Clic para abrir enlace").formatted(Formatting.GRAY))
                ));
                if (hovered) {
                    this.pendingTooltipLines = List.of(
                            Text.literal(link.label()).formatted(Formatting.GOLD, Formatting.BOLD),
                            Text.literal("Clic para abrir en el navegador").formatted(Formatting.YELLOW)
                    );
                }
                rightCursor = btnX - 5;
            }
        }

        StringBuilder hints = new StringBuilder("[G] Cerrar   [P] Progresión   [↑↓←→] Navegar");
        if (selectedEntry != null && selectedEntry.isEntityEntry()) {
            hints.append("   [Arrastrar] Rotar 3D");
        }
        String hintStr = hints.toString();
        int maxHintW = Math.max(80, rightCursor - (winX + 18));
        if (this.textRenderer.getWidth(hintStr) > maxHintW) {
            hintStr = "[G] Cerrar · [P] Progresión";
        }
        context.drawTextWithShadow(this.textRenderer, Text.literal(hintStr), winX + 14, barY + 4, EncyclopediaTheme.CREAM_SECONDARY);
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY, float delta) {
        // Canal interior de cuero/madera oscura para los separadores del libro
        context.fill(sidebarX, sidebarY, sidebarX + sidebarW, sidebarY + sidebarH, EncyclopediaTheme.WOOD_SHADOW);
        context.drawBorder(sidebarX, sidebarY, sidebarW, sidebarH, EncyclopediaTheme.GOLD_DARK);

        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "ÍNDICE",
                sidebarX + sidebarW / 2.0F, sidebarY + 5, 0.84F, EncyclopediaTheme.GOLD_BRIGHT, true
        );

        int listTop = sidebarY + 17;
        int listBottom = sidebarY + sidebarH - 4;
        EncyclopediaCategory[] cats = EncyclopediaCategory.values();
        int cardH = getSidebarCardHeight();
        int totalH = getSidebarTotalHeight(cats, cardH);
        this.sidebarMaxScroll = Math.max(0, totalH - (listBottom - listTop));
        this.sidebarScroll = MathHelper.clamp(this.sidebarScroll, 0, this.sidebarMaxScroll);

        context.enableScissor(sidebarX + 1, listTop, sidebarX + sidebarW - 1, listBottom);
        int cardW = sidebarW - 10;
        int cardX = sidebarX + 5;
        float hoverStep = Math.min(1.0F, delta * 0.45F);

        for (int i = 0; i < cats.length; i++) {
            EncyclopediaCategory cat = cats[i];
            int cy = listTop + getSidebarCardTop(cats, i, cardH) - sidebarScroll;

            if (cat.isSeparateSection() && i > 0 && !cats[i - 1].isSeparateSection()) {
                int divY = cy - SIDEBAR_SECTION_DIVIDER_H;
                context.fill(cardX + 2, divY + 3, cardX + cardW - 2, divY + 4, EncyclopediaTheme.GOLD_DARK);
                EncyclopediaTheme.drawCenteredScaledText(
                        context, this.textRenderer, "MUNDOS",
                        cardX + cardW / 2.0F, divY + 6, 0.72F, EncyclopediaTheme.GOLD_BRIGHT, false
                );
            }

            if (cy + cardH < listTop || cy > listBottom) {
                continue;
            }
            boolean active = (cat == selectedCategory);
            boolean hover = mouseY >= listTop && mouseY < listBottom && isOver(mouseX, mouseY, cardX, cy, cardW, cardH);

            float prevAnim = sidebarHoverAnim.getOrDefault(cat, 0.0F);
            float hoverAnim = MathHelper.lerp(hoverStep, prevAnim, hover ? 1.0F : 0.0F);
            sidebarHoverAnim.put(cat, hoverAnim);

            EncyclopediaTheme.drawBookmarkTab(context, cardX, cy, cardW, cardH, active, hover, cat.isSeparateSection());

            float baseIconScale = cardH >= 30 ? 0.90F : 0.74F;
            float iconScale = baseIconScale * (1.0F + 0.20F * hoverAnim);
            EncyclopediaTheme.drawScaledItem(context, cat.getIcon(), cardX + cardW / 2.0F + 1.0F, cy + cardH * 0.36F, iconScale);

            float labelScale = Math.min(cardH >= 30 ? 0.74F : 0.66F, (cardW - 8.0F) / Math.max(1.0F, textRenderer.getWidth(cat.getShortLabel())));
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer,
                    cat.getShortLabel(),
                    cardX + cardW / 2.0F + 1.0F,
                    cy + cardH * 0.68F,
                    Math.max(0.54F, labelScale),
                    active ? EncyclopediaTheme.GOLD_BRIGHT : (hover ? EncyclopediaTheme.CREAM_PRIMARY : EncyclopediaTheme.CREAM_SECONDARY),
                    active
            );
        }
        context.disableScissor();

        if (sidebarMaxScroll > 0) {
            EncyclopediaTheme.drawParchmentScrollbar(context, sidebarX + sidebarW - 5, listTop, listBottom - listTop, sidebarScroll, sidebarMaxScroll, listBottom - listTop);
        }
    }

    private int getSidebarCardHeight() {
        int listH = Math.max(120, (sidebarY + sidebarH - 4) - (sidebarY + 17));
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

    private void renderCenterPage(DrawContext context, int mouseX, int mouseY, float delta) {
        EncyclopediaTheme.drawParchmentPage(context, centerX, centerY, centerW, centerH);

        context.drawText(this.textRenderer, "❖", centerX + 10, centerY + 8, EncyclopediaTheme.INK_SCARLET, false);
        context.drawText(
                this.textRenderer,
                Text.literal(selectedCategory.getHeaderTitle().toUpperCase()).formatted(Formatting.BOLD),
                centerX + 20, centerY + 8, EncyclopediaTheme.INK_TITLE, false
        );
        String countText = filteredEntries.size() + (selectedCategory == EncyclopediaCategory.STRUCTURES
                ? (filteredEntries.size() == 1 ? " lugar" : " lugares")
                : " entradas");
        int cw = this.textRenderer.getWidth(countText);
        if (centerX + 28 + this.textRenderer.getWidth(selectedCategory.getHeaderTitle().toUpperCase()) < centerX + centerW - cw - 12) {
            context.drawText(this.textRenderer, countText, centerX + centerW - cw - 10, centerY + 8, EncyclopediaTheme.INK_MUTED, false);
        }

        // Caja de búsqueda estilo ranura de pergamino hundida
        boolean searchFocused = this.searchField != null && this.searchField.isFocused();
        context.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxW, searchBoxY + searchBoxH,
                searchFocused ? EncyclopediaTheme.PARCHMENT_BRIGHT : EncyclopediaTheme.PARCHMENT_SLOT_BG);
        context.drawBorder(searchBoxX, searchBoxY, searchBoxW, searchBoxH,
                searchFocused ? EncyclopediaTheme.SCARLET_MAIN : EncyclopediaTheme.PARCHMENT_SHADOW);
        EncyclopediaTheme.drawScaledItem(context, new ItemStack(Items.SPYGLASS), searchBoxX + 9.0F, searchBoxY + searchBoxH / 2.0F, 0.68F);
        if (this.searchField != null) {
            this.searchField.render(context, mouseX, mouseY, delta);
        }

        if (!this.searchQuery.isEmpty()) {
            boolean hoverClear = isOver(mouseX, mouseY, clearSearchX, clearSearchY, clearSearchSize, clearSearchSize);
            context.fill(clearSearchX, clearSearchY, clearSearchX + clearSearchSize, clearSearchY + clearSearchSize,
                    hoverClear ? EncyclopediaTheme.SCARLET_MAIN : EncyclopediaTheme.PARCHMENT_AGED);
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, "×",
                    clearSearchX + clearSearchSize / 2.0F, clearSearchY + 3.0F, 1.0F,
                    hoverClear ? 0xFFFFFFFF : EncyclopediaTheme.INK_TITLE, false
            );
        }

        if (filteredEntries.isEmpty()) {
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, "Sin resultados en el códice",
                    centerX + centerW / 2.0F, gridY + 36, 0.95F, EncyclopediaTheme.INK_MUTED, false
            );
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

        context.enableScissor(gridX - 2, gridY - 2, gridX + gridW + 2, gridBottom + 2);

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

            boolean spoilerLocked = ClientDungeonProgression.isStructureEntrySpoilerLocked(entry.getId());
            GridSlotDraw slotDraw = new GridSlotDraw(entry, sx, sy, slotSize, hoverAnim, isSelected, spoilerLocked);
            if (hoverAnim > 0.01F) {
                drawGridSlotBackground(context, slotDraw);
                deferredHoveredDraws.add(slotDraw);
            } else {
                drawGridSlotBackground(context, slotDraw);
                drawGridSlotContent(context, slotDraw);
            }
        }

        for (GridSlotDraw slotDraw : deferredHoveredDraws) {
            drawGridSlotBackground(context, slotDraw);
            drawGridSlotContent(context, slotDraw);
        }

        context.disableScissor();

        if (gridMaxScroll > 0) {
            EncyclopediaTheme.drawParchmentScrollbar(context, centerX + centerW - 7, gridY, gridH, gridScroll, gridMaxScroll, gridH);
        }
    }

    private void drawGridSlotBackground(DrawContext context, GridSlotDraw slot) {
        int rarityBorder = slot.spoilerLocked()
                ? EncyclopediaTheme.PARCHMENT_SHADOW
                : slot.entry().getRarity().getDynamicBorderColor(slot.selected() || slot.hoverAnim() > 0.2F, animTimeSec);
        EncyclopediaTheme.drawMinecraftSlot(
                context, slot.x(), slot.y(), slot.size(),
                slot.hoverAnim() > 0.2F, slot.selected(), rarityBorder
        );
    }

    private void drawGridSlotContent(DrawContext context, GridSlotDraw slot) {
        float cx = slot.x() + slot.size() / 2.0F;
        float cy = slot.y() + slot.size() / 2.0F;

        if (slot.spoilerLocked()) {
            EncyclopediaTheme.drawPixelPadlock(context, Math.round(cx), Math.round(cy - 1));
            return;
        }

        float zoom = 1.0F + 0.26F * slot.hoverAnim();
        if (slot.entry().isEntityEntry() && client != null && client.world != null) {
            EntityPreviewConfig preview = slot.entry().getEntityPreview();
            LivingEntity entity = getOrCreatePreviewEntity(preview.entityTypeSupplier().get());
            if (entity != null) {
                float miniScale = preview.gridScale() * (slot.size() / 34.0F) * zoom;
                EncyclopediaTheme.renderEntityViewport(
                        context,
                        slot.x() + 1, slot.y() + 1, slot.x() + slot.size() - 1, slot.y() + slot.size() - 1,
                        cx, cy + 2.0F, miniScale,
                        new Vector3f(0.0F, preview.offsetY(), 0.0F),
                        preview.defaultYawDeg(), preview.defaultPitchDeg(),
                        entity, false
                );
            }
        } else {
            float baseScale = (slot.size() - 8) / 16.0F * 0.85F;
            EncyclopediaTheme.drawScaledItem(context, slot.entry().getIcon(), cx, cy, baseScale * zoom);
        }
    }

    private void renderDetailPage(DrawContext context, int mouseX, int mouseY, float delta) {
        this.detailTargets.clear();
        EncyclopediaTheme.drawParchmentPage(context, detailX, detailY, detailW, detailH);

        if (selectedEntry == null) {
            EncyclopediaTheme.drawCenteredScaledText(
                    context, this.textRenderer, "Selecciona una página del códice",
                    detailX + detailW / 2.0F, detailY + detailH / 2.0F, 1.0F, EncyclopediaTheme.INK_MUTED, false
            );
            return;
        }

        // Si es una estructura de dungeon bloqueada por Modo Sin Spoilers, mostrar página sellada sin spoilers
        if (ClientDungeonProgression.isStructureEntrySpoilerLocked(selectedEntry.getId())) {
            renderSpoilerLockedDetailPage(context, mouseX, mouseY);
            return;
        }

        int pad = 12;
        int contentX = detailX + pad;
        int contentW = detailW - pad * 2 - 4;
        int viewTop = detailY + 7;
        int viewBottom = detailY + detailH - 7;
        int viewH = viewBottom - viewTop;

        context.enableScissor(detailX + 4, viewTop, detailX + detailW - 4, viewBottom);

        int y = viewTop - detailScroll;
        EncyclopediaRarity rarity = selectedEntry.getRarity();

        if (selectedEntry.isEntityEntry()) {
            y = renderEntityDetailHeader(context, selectedEntry, rarity, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
        } else {
            y = renderItemDetailHeader(context, selectedEntry, rarity, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom, delta);
        }
        y += 8;

        // Caja de descripción sobre pergamino con listón izquierdo Escarlata
        List<OrderedText> descLines = this.textRenderer.wrapLines(Text.literal(selectedEntry.getDescription()), contentW - 18);
        int descH = Math.max(24, descLines.size() * 10 + 12);
        EncyclopediaTheme.drawParchmentCard(context, contentX, y, contentW, descH, false, false);
        context.fill(contentX + 1, y + 1, contentX + 4, y + descH - 1, EncyclopediaTheme.SCARLET_MAIN);
        int lineY = y + 6;
        for (OrderedText line : descLines) {
            context.drawText(this.textRenderer, line, contentX + 10, lineY, EncyclopediaTheme.INK_BODY, false);
            lineY += 10;
        }
        y += descH + 10;

        // Estadísticas (2 columnas con placa oscura de alto contraste para los valores)
        List<StatInfo> stats = selectedEntry.getStats();
        if (!stats.isEmpty()) {
            y = drawSectionHeader(context, "ESTADÍSTICAS", contentX, y, contentW);
            int colGap = 6;
            int cardW = (contentW - colGap) / 2;
            int cardH = 30;
            for (int i = 0; i < stats.size(); i += 2) {
                drawStatCard(context, stats.get(i), contentX, y, cardW, cardH);
                if (i + 1 < stats.size()) {
                    drawStatCard(context, stats.get(i + 1), contentX + cardW + colGap, y, cardW, cardH);
                }
                y += cardH + 5;
            }
            y += 5;
        }

        // Contenido de Estructuras
        List<StructureFeatureInfo> structureContents = selectedEntry.getStructureContents();
        if (!structureContents.isEmpty()) {
            y = drawSectionHeader(context, "QUÉ CONTIENE LA ESTRUCTURA", contentX, y, contentW);
            for (StructureFeatureInfo feature : structureContents) {
                y = drawStructureFeatureCard(context, feature, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // Pasos de Conquista de Estructuras
        List<StructureStepInfo> structureSteps = selectedEntry.getStructureSteps();
        if (!structureSteps.isEmpty()) {
            y = drawSectionHeader(context, "CÓMO EXPLORARLA Y VENCERLA", contentX, y, contentW);
            for (StructureStepInfo step : structureSteps) {
                y = drawStructureStepCard(context, step, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // Efectos y Habilidades
        List<EffectInfo> effects = selectedEntry.getEffects();
        if (!effects.isEmpty()) {
            y = drawSectionHeader(context, "EFECTOS / HABILIDADES", contentX, y, contentW);
            for (EffectInfo effect : effects) {
                y = drawEffectCard(context, effect, contentX, y, contentW);
            }
            y += 5;
        }

        // Drops / Premios
        List<DropInfo> drops = selectedEntry.getDrops();
        if (!drops.isEmpty()) {
            y = drawSectionHeader(context, "DROPS / PREMIOS", contentX, y, contentW);
            for (DropInfo drop : drops) {
                y = drawDropCard(context, drop, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
            y += 5;
        }

        // Crafteo / Obtención con ranuras clásicas de Minecraft
        List<ObtainingMethod> methods = selectedEntry.getObtainingMethods();
        if (!methods.isEmpty()) {
            String sectionTitle = selectedEntry.getCategory() == EncyclopediaCategory.STRUCTURES
                    ? "LOCALIZACIÓN EN EL MUNDO"
                    : "CRAFTEO / OBTENCIÓN";
            y = drawSectionHeader(context, sectionTitle, contentX, y, contentW);
            for (ObtainingMethod method : methods) {
                y = drawObtainingCard(context, method, contentX, y, contentW, mouseX, mouseY, viewTop, viewBottom);
            }
        }

        context.disableScissor();

        int totalContentH = (y + detailScroll) - viewTop + 8;
        this.detailMaxScroll = Math.max(0, totalContentH - viewH);
        this.detailScroll = MathHelper.clamp(this.detailScroll, 0, this.detailMaxScroll);

        if (detailMaxScroll > 0) {
            EncyclopediaTheme.drawParchmentScrollbar(context, detailX + detailW - 7, viewTop, viewH, detailScroll, detailMaxScroll, viewH);
        }
    }

    private void renderSpoilerLockedDetailPage(DrawContext context, int mouseX, int mouseY) {
        int pad = 16;
        int x = detailX + pad;
        int w = detailW - pad * 2;
        int cy = detailY + detailH / 2 - 58;

        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "🔒 DUNGEON OCULTA · ???",
                detailX + detailW / 2.0F, cy, 1.05F, EncyclopediaTheme.INK_SCARLET, false
        );

        int boxH = 74;
        int boxY = cy + 18;
        EncyclopediaTheme.drawParchmentCard(context, x, boxY, w, boxH, false, false);
        EncyclopediaTheme.drawMysteryBossEmblem(context, this.textRenderer, x + w / 2, boxY + 28, 1.65F, false);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer,
                "Derrota primero al jefe de la dungeon anterior",
                x + w / 2.0F, boxY + 50, 0.84F, EncyclopediaTheme.INK_TITLE, false
        );
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer,
                "para revelar los secretos de este lugar.",
                x + w / 2.0F, boxY + 60, 0.82F, EncyclopediaTheme.INK_MUTED, false
        );

        int btnW = Math.min(w, 210);
        int btnH = 20;
        int btnX = detailX + (detailW - btnW) / 2;
        int btnY = boxY + boxH + 12;
        boolean hover = isOver(mouseX, mouseY, btnX, btnY, btnW, btnH);
        EncyclopediaTheme.drawMinecraftButton(context, btnX, btnY, btnW, btnH, hover, ButtonStyle.SCARLET_GOLD);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, "⚔ Ver Mapa de Progresión →",
                btnX + btnW / 2.0F, btnY + 6.0F, 0.90F, 0xFFFFFFFF, true
        );
        registerDetailTarget(btnX, btnY, btnW, btnH, this::openDungeonProgressionMap, null, null, detailY, detailY + detailH);
        this.detailMaxScroll = 0;
    }

    private int renderItemDetailHeader(
            DrawContext context, EncyclopediaEntry entry, EncyclopediaRarity rarity,
            int x, int y, int width, int mouseX, int mouseY, int viewTop, int viewBottom, float delta
    ) {
        context.drawText(this.textRenderer, Text.literal(entry.getName()).formatted(Formatting.BOLD), x, y + 1, EncyclopediaTheme.INK_TITLE, false);
        int badgeW = drawRarityBadge(context, rarity, x, y + 14);
        context.drawText(this.textRenderer, entry.getTypeLabel(), x + badgeW + 6, y + 16, EncyclopediaTheme.INK_MUTED, false);

        if (entry.getRelatedEntryId() != null && entry.getRelatedButtonLabel() != null) {
            String btnLabel = entry.getRelatedButtonLabel();
            int btnW = this.textRenderer.getWidth(btnLabel) + 12;
            drawActionPill(
                    context, x + width - btnW, y + 12, btnLabel,
                    ButtonStyle.PARCHMENT, EncyclopediaTheme.INK_SCARLET,
                    () -> navigateToEntryId(entry.getRelatedEntryId()),
                    mouseX, mouseY, viewTop, viewBottom
            );
        }

        int showcaseY = y + 31;
        int showcaseH = MathHelper.clamp((int) (detailH * 0.29F), 84, 118);
        boolean hoverShowcase = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, showcaseY, width, showcaseH);
        this.itemShowcaseHoverAnim = MathHelper.lerp(Math.min(1.0F, delta * 0.45F), this.itemShowcaseHoverAnim, hoverShowcase ? 1.0F : 0.0F);

        int border = rarity.getDynamicBorderColor(hoverShowcase, animTimeSec);
        EncyclopediaTheme.drawDarkShowcaseAlcove(context, x, showcaseY, width, showcaseH, border);

        int pedestalY = showcaseY + showcaseH - 16;
        context.fill(x + 28, pedestalY, x + width - 28, pedestalY + 1, EncyclopediaTheme.GOLD_DEEP);

        EncyclopediaTheme.drawScaledText(context, this.textRenderer, "✦ RELIQUIA / DETALLE", x + 8, showcaseY + 6, 0.78F, EncyclopediaTheme.CREAM_MUTED, false);

        float largeScale = MathHelper.clamp(showcaseH * 0.045F, 3.5F, 5.0F) * (1.0F + 0.12F * itemShowcaseHoverAnim);
        EncyclopediaTheme.drawScaledItem(context, entry.getIcon(), x + width / 2.0F, showcaseY + showcaseH / 2.0F - 1.0F, largeScale);
        registerDetailTarget(x, showcaseY, width, showcaseH, null, entry.getIcon(), null, viewTop, viewBottom);

        return showcaseY + showcaseH;
    }

    private int renderEntityDetailHeader(
            DrawContext context, EncyclopediaEntry entry, EncyclopediaRarity rarity,
            int x, int y, int width, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        context.drawText(this.textRenderer, Text.literal(entry.getName()).formatted(Formatting.BOLD), x, y + 1, EncyclopediaTheme.INK_TITLE, false);
        int badgeW = drawRarityBadge(context, rarity, x, y + 14);
        context.drawText(this.textRenderer, entry.getTypeLabel(), x + badgeW + 6, y + 16, EncyclopediaTheme.INK_MUTED, false);

        if (entry.getRelatedEntryId() != null && entry.getRelatedButtonLabel() != null) {
            String btnLabel = entry.getRelatedButtonLabel();
            int btnW = this.textRenderer.getWidth(btnLabel) + 12;
            drawActionPill(
                    context, x + width - btnW, y + 12, btnLabel,
                    ButtonStyle.PARCHMENT, EncyclopediaTheme.INK_SCARLET,
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
        EncyclopediaTheme.drawDarkShowcaseAlcove(context, x, stageY, width, stageH, border);

        int floorY = stageY + stageH - 22;
        context.fill(x + 18, floorY, x + width - 18, floorY + 1, 0x88E6AF35);

        EntityPreviewConfig preview = entry.getEntityPreview();
        if (preview != null && client != null && client.world != null) {
            LivingEntity entity = getOrCreatePreviewEntity(preview.entityTypeSupplier().get());
            if (entity != null) {
                boolean devilMode = preview.supportsDevilMode() && this.jefriDevilModePreview;
                if (entity instanceof JefriPensteinEntity jefri) {
                    jefri.setClientPreviewDevilMode(devilMode);
                } else if (entity instanceof JoseTorresEntity jose) {
                    jose.setClientPreviewPhase2(devilMode);
                }
                float dynamicScale = preview.showcaseScale() * (stageH / 128.0F);
                EncyclopediaTheme.renderEntityViewport(
                        context,
                        x + 3, stageY + 3, x + width - 3, stageY + stageH - 3,
                        x + width / 2.0F, stageY + stageH / 2.0F + 2.0F,
                        dynamicScale,
                        new Vector3f(0.0F, preview.offsetY(), 0.0F),
                        this.previewYawDeg, this.previewPitchDeg,
                        entity, false
                );
            }
        }

        EncyclopediaTheme.drawScaledText(
                context, this.textRenderer, "✦ VISTA 3D · Arrastra para rotar",
                x + 8, stageY + 6, 0.78F, EncyclopediaTheme.CREAM_MUTED, false
        );

        int btnY = stageY + stageH - 18;
        int bx = x + 6;
        String autoLabel = autoRotate3D ? "Auto: ON" : "Auto: OFF";
        int autoW = drawActionPill(
                context, bx, btnY, autoLabel,
                autoRotate3D ? ButtonStyle.EMERALD : ButtonStyle.WOOD,
                0xFFFFFFFF,
                () -> this.autoRotate3D = !this.autoRotate3D,
                mouseX, mouseY, viewTop, viewBottom
        );
        bx += autoW + 4;

        drawActionPill(
                context, bx, btnY, "Reset",
                ButtonStyle.WOOD, EncyclopediaTheme.CREAM_PRIMARY,
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
            boolean isJose = "entity_jose_torres".equals(entry.getId());
            String phaseName = isJose ? "Jhoseph Towers" : "Modo Diablo";
            String devilLabel = phaseName + (jefriDevilModePreview ? ": ON" : ": OFF");
            int dw = this.textRenderer.getWidth(devilLabel) + 12;
            drawActionPill(
                    context, x + width - dw - 6, btnY, devilLabel,
                    jefriDevilModePreview ? ButtonStyle.SCARLET_GOLD : ButtonStyle.WOOD,
                    0xFFFFFFFF,
                    () -> this.jefriDevilModePreview = !this.jefriDevilModePreview,
                    mouseX, mouseY, viewTop, viewBottom
            );
        }

        return stageY + stageH;
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

    private int drawSectionHeader(DrawContext context, String title, int x, int y, int w) {
        context.drawText(this.textRenderer, "❖", x, y, EncyclopediaTheme.INK_SCARLET, false);
        context.drawText(this.textRenderer, Text.literal(title).formatted(Formatting.BOLD), x + 9, y, EncyclopediaTheme.INK_TITLE, false);
        int tw = this.textRenderer.getWidth(title) + 14;
        if (tw < w - 8) {
            context.fill(x + tw, y + 4, x + w, y + 5, EncyclopediaTheme.PARCHMENT_RULE);
        }
        return y + 13;
    }

    private void drawStatCard(DrawContext context, StatInfo stat, int x, int y, int w, int h) {
        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, false, false);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, stat.label(), x + 6, y + 4, 0.80F, EncyclopediaTheme.INK_MUTED, false);
        // Franja inferior en roble oscuro para que cualquier color brillante de stat resalte al 100%
        context.fill(x + 3, y + 14, x + w - 3, y + h - 3, EncyclopediaTheme.WOOD_DARK);
        EncyclopediaTheme.drawScaledText(context, this.textRenderer, stat.value(), x + 6, y + 16.5F, 0.86F, stat.valueColor(), true);
    }

    private int drawStructureFeatureCard(
            DrawContext context, StructureFeatureInfo feature,
            int x, int y, int w, int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        List<OrderedText> detailLines = this.textRenderer.wrapLines(Text.literal(feature.detail()), w - 38);
        int h = Math.max(30, 18 + detailLines.size() * 9);
        boolean clickable = feature.targetEntryId() != null;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);

        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, hover && clickable, false);
        context.fill(x + 1, y + 1, x + 3, y + h - 1, EncyclopediaTheme.GOLD_DEEP);

        ItemStack icon = feature.getIcon();
        EncyclopediaTheme.drawScaledItem(context, icon, x + 16, y + 15.0F, hover ? 1.12F : 0.95F);

        context.drawText(this.textRenderer, Text.literal(feature.title()).formatted(Formatting.BOLD), x + 30, y + 5, EncyclopediaTheme.INK_TITLE, false);
        if (clickable) {
            String linkTag = "Ver →";
            int lw = this.textRenderer.getWidth(linkTag);
            context.drawText(this.textRenderer, linkTag, x + w - lw - 6, y + 5, EncyclopediaTheme.INK_SCARLET, false);
        }

        int dy = y + 16;
        for (OrderedText line : detailLines) {
            context.drawText(this.textRenderer, line, x + 30, dy, EncyclopediaTheme.INK_BODY, false);
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

        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, hover && clickable, false);

        int badgeSize = 14;
        int bx = x + 6;
        int by = y + 6;
        context.fill(bx, by, bx + badgeSize, by + badgeSize, EncyclopediaTheme.SCARLET_MAIN);
        context.drawBorder(bx, by, badgeSize, badgeSize, EncyclopediaTheme.GOLD_MAIN);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, String.valueOf(step.stepNumber()),
                bx + badgeSize / 2.0F, by + 3.0F, 0.95F, 0xFFFFFFFF, true
        );

        if (!icon.isEmpty()) {
            EncyclopediaTheme.drawScaledItem(context, icon, bx + badgeSize / 2.0F, by + badgeSize + 10.0F, hover ? 1.05F : 0.85F);
        }

        context.drawText(this.textRenderer, Text.literal(step.title()).formatted(Formatting.BOLD), x + 28, y + 6, EncyclopediaTheme.INK_SCARLET, false);
        int ty = y + 17;
        for (OrderedText line : lines) {
            context.drawText(this.textRenderer, line, x + 28, ty, EncyclopediaTheme.INK_BODY, false);
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
        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, false, false);
        context.fill(x + 1, y + 1, x + 4, y + h - 1, effect.accentColor());

        context.drawText(this.textRenderer, Text.literal(effect.name()).formatted(Formatting.BOLD), x + 8, y + 4, EncyclopediaTheme.INK_TITLE, false);
        int dy = y + 14;
        for (OrderedText line : detailLines) {
            context.drawText(this.textRenderer, line, x + 8, dy, EncyclopediaTheme.INK_BODY, false);
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

        EncyclopediaTheme.drawParchmentCard(context, x, y, w, h, hover && clickable, false);

        ItemStack icon = drop.getIcon();
        EncyclopediaTheme.drawScaledItem(context, icon, x + 13, y + h / 2.0F, hover ? 1.12F : 0.90F);
        context.drawText(this.textRenderer, drop.name(), x + 27, y + 8, EncyclopediaTheme.INK_TITLE, false);

        String right = drop.detail() + (clickable ? " →" : "");
        int rw = this.textRenderer.getWidth(right);
        context.drawText(
                this.textRenderer, right, x + w - rw - 6, y + 8,
                clickable ? EncyclopediaTheme.INK_LINK : EncyclopediaTheme.INK_SCARLET, false
        );

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

            EncyclopediaTheme.drawParchmentCard(context, x, y, w, cardH, false, false);
            context.drawText(this.textRenderer, Text.literal(method.title()).formatted(Formatting.BOLD), x + innerPad, y + 5, EncyclopediaTheme.INK_MUTED, false);

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
            context.drawText(this.textRenderer, Text.literal("⟹").formatted(Formatting.BOLD), arrowX, arrowY, EncyclopediaTheme.INK_SCARLET, false);

            int outSize = 26;
            int outX = arrowX + 18;
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
                        context.drawText(this.textRenderer, ol, labelX, ly, EncyclopediaTheme.INK_TITLE, false);
                        ly += 10;
                    }
                    if (outStack.getCount() > 1) {
                        context.drawText(this.textRenderer, "x" + outStack.getCount(), labelX, ly, EncyclopediaTheme.INK_SCARLET, false);
                    }
                }
            }

            int ny = gy + contentH + 4;
            for (OrderedText nl : noteLines) {
                context.drawText(this.textRenderer, nl, x + innerPad, ny, EncyclopediaTheme.INK_MUTED, false);
                ny += 9;
            }
            return y + cardH + 5;
        }

        if (method.type() == ObtainingType.SMELTING || method.type() == ObtainingType.BLASTING) {
            int slot = 24;
            int noteH = noteLines.isEmpty() ? 0 : (noteLines.size() * 9 + 4);
            int cardH = 18 + slot + 6 + noteH + 6;
            EncyclopediaTheme.drawParchmentCard(context, x, y, w, cardH, false, false);
            context.drawText(this.textRenderer, Text.literal(method.title()).formatted(Formatting.BOLD), x + innerPad, y + 5, EncyclopediaTheme.INK_MUTED, false);

            int rowY = y + 17;
            SlotRef input = method.inputs().isEmpty() ? SlotRef.empty() : method.inputs().get(0);
            drawRecipeSlot(context, input, x + innerPad, rowY, slot, mouseX, mouseY, viewTop, viewBottom);
            context.drawText(this.textRenderer, "🔥 ⟹", x + innerPad + slot + 7, rowY + 8, EncyclopediaTheme.INK_SCARLET, false);
            drawRecipeSlot(context, method.output(), x + innerPad + slot + 36, rowY, slot, mouseX, mouseY, viewTop, viewBottom);

            ItemStack outStack = method.output().getStack();
            if (!outStack.isEmpty()) {
                context.drawText(this.textRenderer, outStack.getName(), x + innerPad + slot + 66, rowY + 8, EncyclopediaTheme.INK_TITLE, false);
            }

            int ny = rowY + slot + 5;
            for (OrderedText nl : noteLines) {
                context.drawText(this.textRenderer, nl, x + innerPad, ny, EncyclopediaTheme.INK_MUTED, false);
                ny += 9;
            }
            return y + cardH + 5;
        }

        int slot = 24;
        List<OrderedText> infoWrapped = this.textRenderer.wrapLines(Text.literal(method.note()), w - innerPad * 2 - slot - 8);
        int textH = Math.max(slot, infoWrapped.size() * 9 + 4);
        int cardH = 18 + textH + 8;
        EncyclopediaTheme.drawParchmentCard(context, x, y, w, cardH, false, false);
        context.drawText(this.textRenderer, Text.literal(method.title()).formatted(Formatting.BOLD), x + innerPad, y + 5, EncyclopediaTheme.INK_MUTED, false);

        SlotRef iconRef = method.inputs().isEmpty() ? SlotRef.empty() : method.inputs().get(0);
        drawRecipeSlot(context, iconRef, x + innerPad, y + 18, slot, mouseX, mouseY, viewTop, viewBottom);
        int ty = y + 19;
        for (OrderedText line : infoWrapped) {
            context.drawText(this.textRenderer, line, x + innerPad + slot + 8, ty, EncyclopediaTheme.INK_BODY, false);
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

        EncyclopediaTheme.drawMinecraftSlot(
                context, x, y, size, hover, false,
                hasLink ? EncyclopediaTheme.SCARLET_MAIN : EncyclopediaTheme.GOLD_DEEP
        );

        if (!stack.isEmpty()) {
            float zoom = hover ? 1.22F : 1.0F;
            EncyclopediaTheme.drawScaledItem(context, stack, x + size / 2.0F, y + size / 2.0F, ((size - 4) / 16.0F) * zoom);
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
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, label,
                x + w / 2.0F, y + 2.5F, 0.85F, rarity.getTextColor(), true
        );
        return w;
    }

    private int drawActionPill(
            DrawContext context, int x, int y, String label,
            ButtonStyle style, int textColor, Runnable onClick,
            int mouseX, int mouseY, int viewTop, int viewBottom
    ) {
        int w = this.textRenderer.getWidth(label) + 12;
        int h = 14;
        boolean hover = mouseY >= viewTop && mouseY < viewBottom && isOver(mouseX, mouseY, x, y, w, h);
        EncyclopediaTheme.drawMinecraftButton(context, x, y, w, h, hover, style);
        boolean shadow = (style != ButtonStyle.PARCHMENT);
        EncyclopediaTheme.drawCenteredScaledText(
                context, this.textRenderer, label,
                x + w / 2.0F, y + 3.5F, 0.90F, textColor, shadow
        );
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
        if (ClientDungeonProgression.isStructureEntrySpoilerLocked(entry.getId())) {
            context.drawTooltip(this.textRenderer, List.of(
                    Text.literal("🔒 ???").formatted(Formatting.RED, Formatting.BOLD),
                    Text.literal("Derrota al jefe anterior para revelar esta dungeon").formatted(Formatting.GRAY)
            ), mouseX, mouseY);
            return;
        }

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

        if (isOver(mouseX, mouseY, progressionBtnX, progressionBtnY, progressionBtnW, progressionBtnH)) {
            openDungeonProgressionMap();
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

        int listTop = sidebarY + 17;
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
                    playBookTurnSound();
                    selectEntry(filteredEntries.get(i), true);
                    return true;
                }
            }
        }

        for (int i = socialTargets.size() - 1; i >= 0; i--) {
            HitTarget target = socialTargets.get(i);
            if (target.onClick() != null && target.contains(mouseX, mouseY)) {
                target.onClick().run();
                return true;
            }
        }

        for (int i = detailTargets.size() - 1; i >= 0; i--) {
            HitTarget target = detailTargets.get(i);
            if (target.onClick() != null && target.contains(mouseX, mouseY)) {
                playBookTurnSound();
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

        if (keyCode == GLFW.GLFW_KEY_P) {
            openDungeonProgressionMap();
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
        if (nextIdx != idx) {
            playBookTurnSound();
        }
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

    private static boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
