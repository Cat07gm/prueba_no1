package pruebano1.modid.client.encyclopedia;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Paleta y primitivas gráficas compartidas para el rediseño de la Enciclopedia Lon Terry
 * y el Mapa de Progresión de Dungeons:
 * - Estética Libro / Pergamino de Atril de Minecraft (madera de roble oscuro biselada en pixel-art,
 *   páginas cálidas de pergamino antiguo, ranuras hundidas clásicas de inventario y marcos de Logros).
 * - Identidad única del mod mediante Broches de Escarlata y Oro en las esquinas, remaches y sellos.
 */
public final class EncyclopediaTheme {

    // Cubierta y marcos de Madera Oscura (estilo Atril / Roble Oscuro de Minecraft)
    public static final int WOOD_OUTER_BORDER = 0xFF140B06;
    public static final int WOOD_SHADOW = 0xFF22130B;
    public static final int WOOD_DARK = 0xFF311D11;
    public static final int WOOD_BASE = 0xFF422818;
    public static final int WOOD_MID = 0xFF563520;
    public static final int WOOD_HIGHLIGHT = 0xFF754B30;

    // Pergamino cálido estilo Libro Escrito / Mapa de Minecraft
    public static final int PARCHMENT_BRIGHT = 0xFFF7EEDB;
    public static final int PARCHMENT_LIGHT = 0xFFF1E4CA;
    public static final int PARCHMENT_BASE = 0xFFE9D8B8;
    public static final int PARCHMENT_AGED = 0xFFDCC6A0;
    public static final int PARCHMENT_CARD = 0xFFE2CFA9;
    public static final int PARCHMENT_CARD_HOVER = 0xFFEEE0C2;
    public static final int PARCHMENT_SLOT_BG = 0xFFCDB58C;
    public static final int PARCHMENT_SLOT_HOVER = 0xFFDFC9A4;
    public static final int PARCHMENT_RULE = 0xFFB59366;
    public static final int PARCHMENT_BORDER = 0xFF8F6D43;
    public static final int PARCHMENT_SHADOW = 0xFF694D2C;

    // Broches y acentos de Escarlata + Oro del mod Lon Terry
    public static final int SCARLET_BRIGHT = 0xFFFF5959;
    public static final int SCARLET_MAIN = 0xFFD11F2A;
    public static final int SCARLET_DEEP = 0xFF8F121B;
    public static final int SCARLET_DARK = 0xFF54090F;

    public static final int GOLD_BRIGHT = 0xFFFFE588;
    public static final int GOLD_MAIN = 0xFFE6AF35;
    public static final int GOLD_DEEP = 0xFFB07B16;
    public static final int GOLD_DARK = 0xFF6E4909;

    public static final int EMERALD_BRIGHT = 0xFF6EFF9A;
    public static final int EMERALD_MAIN = 0xFF28A854;
    public static final int EMERALD_DARK = 0xFF13592B;

    // Tintas de alta legibilidad sobre pergamino y madera
    public static final int INK_TITLE = 0xFF241308;
    public static final int INK_BODY = 0xFF3B2312;
    public static final int INK_MUTED = 0xFF6B4C33;
    public static final int INK_SCARLET = 0xFF9E1620;
    public static final int INK_LINK = 0xFF195873;

    public static final int CREAM_PRIMARY = 0xFFFFF5DF;
    public static final int CREAM_SECONDARY = 0xFFE8D5B7;
    public static final int CREAM_MUTED = 0xFFC2A885;

    public enum ButtonStyle {
        SCARLET_GOLD,
        WOOD,
        PARCHMENT,
        EMERALD
    }

    public enum NodeFrameKind {
        TASK,       // Marco cuadrado biselado clásico de logro
        GOAL,       // Marco arqueado superior/inferior
        CHALLENGE   // Marco coronado con puntas para jefes mayores / final
    }

    public enum NodeVisualState {
        COMPLETED,
        UNLOCKED_NEXT,
        LOCKED_SPOILER,
        FUTURE_DEV
    }

    private EncyclopediaTheme() {
    }

    /**
     * Dibuja la cubierta exterior completa de madera oscura biselada estilo Minecraft
     * con los 4 Broches de Escarlata y Oro característicos del mod en las esquinas.
     */
    public static void drawBookOuterFrame(DrawContext context, int x, int y, int w, int h) {
        // Sombra proyectada suave detrás del tomo
        context.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x66000000);

        // Marco exterior negro-caoba de 1px y cuerpo de roble oscuro
        context.fill(x, y, x + w, y + h, WOOD_OUTER_BORDER);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, WOOD_BASE);

        // Bisel 3D pixelado estilo GUI de Minecraft en el borde de madera
        context.fill(x + 1, y + 1, x + w - 1, y + 3, WOOD_HIGHLIGHT);
        context.fill(x + 1, y + 1, x + 3, y + h - 1, WOOD_HIGHLIGHT);
        context.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, WOOD_SHADOW);
        context.fill(x + w - 3, y + 1, x + w - 1, y + h - 1, WOOD_SHADOW);

        // Filete dorado/escarlata empotrado dentro de la cubierta
        context.drawBorder(x + 4, y + 4, w - 8, h - 8, GOLD_DARK);

        // Los 4 Broches de Escarlata y Oro en las esquinas del códice
        drawCornerScarletClasp(context, x, y, false, false);
        drawCornerScarletClasp(context, x + w - 12, y, true, false);
        drawCornerScarletClasp(context, x, y + h - 12, false, true);
        drawCornerScarletClasp(context, x + w - 12, y + h - 12, true, true);
    }

    /**
     * Broche esquinero metálico de Oro con gema facetada de Escarlata (12x12 px).
     */
    public static void drawCornerScarletClasp(DrawContext context, int x, int y, boolean right, boolean bottom) {
        // Placa base dorada en L
        context.fill(x, y, x + 12, y + 12, WOOD_OUTER_BORDER);
        context.fill(x + 1, y + 1, x + 11, y + 11, GOLD_DEEP);
        context.fill(x + 1, y + 1, x + 10, y + 2, GOLD_BRIGHT);
        context.fill(x + 1, y + 1, x + 2, y + 10, GOLD_BRIGHT);

        // Recorte interior para darle silueta de herraje de libro de Minecraft
        int cutX = right ? x + 1 : x + 7;
        int cutY = bottom ? y + 1 : y + 7;
        context.fill(cutX, cutY, cutX + 4, cutY + 4, WOOD_BASE);

        // Gema de Escarlata engastada en la esquina exterior
        int gemX = right ? x + 5 : x + 2;
        int gemY = bottom ? y + 5 : y + 2;
        context.fill(gemX, gemY, gemX + 5, gemY + 5, SCARLET_DARK);
        context.fill(gemX + 1, gemY + 1, gemX + 4, gemY + 4, SCARLET_MAIN);
        context.fill(gemX + 1, gemY + 1, gemX + 2, gemY + 2, SCARLET_BRIGHT);
    }

    /**
     * Dibuja una página interior de pergamino cálido con bordes sombreados de papel antiguo.
     */
    public static void drawParchmentPage(DrawContext context, int x, int y, int w, int h) {
        if (w <= 4 || h <= 4) {
            return;
        }
        context.fill(x, y, x + w, y + h, PARCHMENT_SHADOW);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, PARCHMENT_AGED);
        context.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, PARCHMENT_LIGHT, PARCHMENT_BASE);

        // Doble filete fino de tinta sepia adentro de la hoja
        context.drawBorder(x + 3, y + 3, w - 6, h - 6, PARCHMENT_RULE);
    }

    /**
     * Dibuja el lomo central encuadernado que separa dos páginas del libro abierto,
     * con costuras doradas y abrazaderas de Escarlata.
     */
    public static void drawBookSpine(DrawContext context, int spineX, int y, int h) {
        context.fill(spineX - 3, y, spineX + 3, y + h, WOOD_DARK);
        context.fill(spineX - 1, y, spineX + 1, y + h, WOOD_SHADOW);
        context.fill(spineX - 3, y, spineX - 2, y + h, PARCHMENT_SHADOW);
        context.fill(spineX + 2, y, spineX + 3, y + h, PARCHMENT_SHADOW);

        // Costuras/nervios de encuadernación cada ~46 px
        for (int bandY = y + 20; bandY < y + h - 16; bandY += 46) {
            context.fill(spineX - 4, bandY, spineX + 4, bandY + 5, GOLD_DARK);
            context.fill(spineX - 3, bandY + 1, spineX + 3, bandY + 4, SCARLET_MAIN);
            context.fill(spineX - 2, bandY + 1, spineX + 2, bandY + 2, SCARLET_BRIGHT);
        }
    }

    /**
     * Tarjeta sobre pergamino con bisel sutil estilo Minecraft.
     */
    public static void drawParchmentCard(DrawContext context, int x, int y, int w, int h, boolean hovered, boolean active) {
        int bg = active ? PARCHMENT_BRIGHT : (hovered ? PARCHMENT_CARD_HOVER : PARCHMENT_CARD);
        int border = active ? SCARLET_MAIN : (hovered ? GOLD_DEEP : PARCHMENT_BORDER);
        context.fill(x, y, x + w, y + h, bg);
        context.drawBorder(x, y, w, h, border);
        // Luz superior interna estilo bisel Minecraft
        context.fill(x + 1, y + 1, x + w - 1, y + 2, PARCHMENT_BRIGHT);
        if (active) {
            context.fill(x + 1, y + 1, x + 3, y + h - 1, SCARLET_MAIN);
        }
    }

    /**
     * Escenario oscuro empotrado (madera noble + remaches dorados/escarlata) para que modelos 3D
     * e ítems brillantes resalten nítidamente sobre la página de pergamino.
     */
    public static void drawDarkShowcaseAlcove(DrawContext context, int x, int y, int w, int h, int accentBorder) {
        context.fill(x, y, x + w, y + h, WOOD_OUTER_BORDER);
        context.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1D110A, 0xFF332014);
        context.drawBorder(x + 1, y + 1, w - 2, h - 2, accentBorder);
        // Sombra hundida superior
        context.fill(x + 2, y + 2, x + w - 2, y + 4, 0x88000000);

        // Pequeños remaches de oro en las 4 esquinas del nicho
        drawGoldStud(context, x + 3, y + 3);
        drawGoldStud(context, x + w - 6, y + 3);
        drawGoldStud(context, x + 3, y + h - 6);
        drawGoldStud(context, x + w - 6, y + h - 6);
    }

    public static void drawGoldStud(DrawContext context, int x, int y) {
        context.fill(x, y, x + 3, y + 3, GOLD_DARK);
        context.fill(x, y, x + 2, y + 2, GOLD_BRIGHT);
    }

    /**
     * Ranura hundida clásica estilo Inventario / Mesa de Crafteo de Minecraft adaptada al pergamino.
     */
    public static void drawMinecraftSlot(
            DrawContext context,
            int x,
            int y,
            int size,
            boolean hovered,
            boolean selected,
            int rarityBorderColor
    ) {
        int bg = selected ? 0xFFF2DFBA : (hovered ? PARCHMENT_SLOT_HOVER : PARCHMENT_SLOT_BG);
        context.fill(x, y, x + size, y + size, bg);

        // Bisel hundido tradicional de slot de Minecraft: oscuro arriba/izquierda, claro abajo/derecha
        context.fill(x, y, x + size - 1, y + 1, PARCHMENT_SHADOW);
        context.fill(x, y, x + 1, y + size - 1, PARCHMENT_SHADOW);
        context.fill(x + 1, y + size - 1, x + size, y + size, PARCHMENT_BRIGHT);
        context.fill(x + size - 1, y + 1, x + size, y + size, PARCHMENT_BRIGHT);
        context.fill(x + size - 1, y, x + size, y + 1, PARCHMENT_AGED);
        context.fill(x, y + size - 1, x + 1, y + size, PARCHMENT_AGED);

        if (selected) {
            context.drawBorder(x - 1, y - 1, size + 2, size + 2, SCARLET_MAIN);
            context.drawBorder(x, y, size, size, GOLD_BRIGHT);
        } else if (hovered) {
            context.drawBorder(x, y, size, size, rarityBorderColor);
        } else if (rarityBorderColor != 0) {
            // Pequeña muesca esquinera del color de rareza para no saturar la ranura clásica
            context.fill(x + 1, y + size - 3, x + 3, y + size - 1, rarityBorderColor);
        }
    }

    /**
     * Botón táctil biselado en pixel-art estilo Minecraft con variantes del mod.
     */
    public static void drawMinecraftButton(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            boolean hovered,
            ButtonStyle style
    ) {
        int outer = WOOD_OUTER_BORDER;
        int fill;
        int topHighlight;
        int botShadow;

        switch (style) {
            case SCARLET_GOLD -> {
                outer = hovered ? GOLD_BRIGHT : GOLD_DARK;
                fill = hovered ? 0xFFE22B37 : SCARLET_MAIN;
                topHighlight = hovered ? 0xFFFF7A7A : SCARLET_BRIGHT;
                botShadow = SCARLET_DARK;
            }
            case EMERALD -> {
                outer = hovered ? GOLD_BRIGHT : WOOD_OUTER_BORDER;
                fill = hovered ? 0xFF32BF62 : EMERALD_MAIN;
                topHighlight = EMERALD_BRIGHT;
                botShadow = EMERALD_DARK;
            }
            case PARCHMENT -> {
                outer = hovered ? SCARLET_MAIN : PARCHMENT_SHADOW;
                fill = hovered ? PARCHMENT_BRIGHT : PARCHMENT_AGED;
                topHighlight = 0xFFFFF9EB;
                botShadow = PARCHMENT_BORDER;
            }
            default -> {
                outer = hovered ? GOLD_MAIN : WOOD_OUTER_BORDER;
                fill = hovered ? WOOD_MID : WOOD_BASE;
                topHighlight = hovered ? 0xFF8C5B3B : WOOD_HIGHLIGHT;
                botShadow = WOOD_SHADOW;
            }
        }

        context.fill(x, y, x + w, y + h, outer);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        context.fill(x + 1, y + 1, x + w - 2, y + 2, topHighlight);
        context.fill(x + 1, y + 1, x + 2, y + h - 2, topHighlight);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, botShadow);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, botShadow);
    }

    /**
     * Cinta / separador de categoría estilo marcapáginas de cuero y Escarlata.
     */
    public static void drawBookmarkTab(
            DrawContext context,
            int x,
            int y,
            int w,
            int h,
            boolean active,
            boolean hovered,
            boolean specialSection
    ) {
        int bg = active ? (specialSection ? SCARLET_DEEP : WOOD_MID) : (hovered ? WOOD_BASE : WOOD_DARK);
        int border = active ? GOLD_MAIN : (hovered ? WOOD_HIGHLIGHT : WOOD_OUTER_BORDER);
        context.fill(x, y, x + w, y + h, border);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
        context.fill(x + 1, y + 1, x + w - 2, y + 2, active ? GOLD_BRIGHT : WOOD_HIGHLIGHT);

        // Listón izquierdo de Escarlata o Dorado cuando está activo
        int ribbonColor = active ? (specialSection ? GOLD_BRIGHT : SCARLET_BRIGHT) : (specialSection ? GOLD_DEEP : SCARLET_DEEP);
        context.fill(x + 1, y + 1, x + 4, y + h - 1, ribbonColor);
    }

    /**
     * Marco pixelado estilo Advancements (Logros) de Minecraft con muescas/puntas en las esquinas
     * según el tipo de nodo y su estado de conquista.
     */
    public static void drawAdvancementNodeFrame(
            DrawContext context,
            int x,
            int y,
            int size,
            NodeFrameKind kind,
            NodeVisualState state,
            boolean hovered,
            boolean selected,
            float animTimeSec
    ) {
        float pulse = 0.5F + 0.5F * MathHelper.sin(animTimeSec * 4.2F);

        int outerRim;
        int frameMain;
        int frameHighlight;
        int frameShadow;
        int innerWellTop;
        int innerWellBot;

        switch (state) {
            case COMPLETED -> {
                outerRim = selected ? 0xFFFFFFFF : WOOD_OUTER_BORDER;
                frameMain = GOLD_MAIN;
                frameHighlight = GOLD_BRIGHT;
                frameShadow = GOLD_DARK;
                innerWellTop = 0xFF1E3824;
                innerWellBot = 0xFF31593A;
            }
            case UNLOCKED_NEXT -> {
                outerRim = selected ? GOLD_BRIGHT : WOOD_OUTER_BORDER;
                frameMain = lerpArgb(SCARLET_MAIN, GOLD_MAIN, pulse * 0.45F);
                frameHighlight = GOLD_BRIGHT;
                frameShadow = SCARLET_DARK;
                innerWellTop = 0xFF2B1712;
                innerWellBot = 0xFF47251B;
            }
            case LOCKED_SPOILER -> {
                outerRim = selected ? GOLD_MAIN : 0xFF1A120C;
                frameMain = hovered ? 0xFF5E4A3A : 0xFF4A392C;
                frameHighlight = 0xFF755E4B;
                frameShadow = 0xFF291F17;
                innerWellTop = 0xFFBCA582;
                innerWellBot = 0xFFA38B68;
            }
            default -> {
                outerRim = selected ? GOLD_MAIN : 0xFF1A1412;
                frameMain = 0xFF3E3530;
                frameHighlight = 0xFF5C5049;
                frameShadow = 0xFF211C19;
                innerWellTop = 0xFF9E8B70;
                innerWellBot = 0xFF87745B;
            }
        }

        // Sombra sobre el mapa de pergamino
        context.fill(x + 2, y + 3, x + size + 2, y + size + 3, 0x44000000);

        // Puntas ornamentales estilo CHALLENGE / GOAL de Minecraft Advancements
        if (kind == NodeFrameKind.CHALLENGE) {
            // Corona/puntas superiores e inferiores de jefe mayor
            context.fill(x - 3, y + 4, x + size + 3, y + 10, outerRim);
            context.fill(x - 2, y + 5, x + size + 2, y + 9, frameMain);
            context.fill(x - 3, y + size - 10, x + size + 3, y + size - 4, outerRim);
            context.fill(x - 2, y + size - 9, x + size + 2, y + size - 5, frameMain);
            context.fill(x + size / 2 - 4, y - 4, x + size / 2 + 4, y + 2, outerRim);
            context.fill(x + size / 2 - 3, y - 3, x + size / 2 + 3, y + 1, frameHighlight);
        } else if (kind == NodeFrameKind.GOAL) {
            context.fill(x + 6, y - 2, x + size - 6, y + 2, outerRim);
            context.fill(x + 7, y - 1, x + size - 7, y + 2, frameHighlight);
            context.fill(x + 6, y + size - 2, x + size - 6, y + size + 2, outerRim);
            context.fill(x + 7, y + size - 2, x + size - 7, y + size + 1, frameShadow);
        }

        // Cuerpo con esquinas recortadas pixel-art estilo nodo de Logro de Minecraft
        context.fill(x + 2, y, x + size - 2, y + size, outerRim);
        context.fill(x, y + 2, x + size, y + size - 2, outerRim);

        context.fill(x + 2, y + 1, x + size - 2, y + size - 1, frameMain);
        context.fill(x + 1, y + 2, x + size - 1, y + size - 2, frameMain);

        // Biseles de luz y sombra del marco
        context.fill(x + 2, y + 1, x + size - 2, y + 3, frameHighlight);
        context.fill(x + 1, y + 2, x + 3, y + size - 2, frameHighlight);
        context.fill(x + 2, y + size - 3, x + size - 2, y + size - 1, frameShadow);
        context.fill(x + size - 3, y + 2, x + size - 1, y + size - 2, frameShadow);

        // Fondo interior del retrato
        int pad = 5;
        context.fill(x + pad - 1, y + pad - 1, x + size - pad + 1, y + size - pad + 1, WOOD_OUTER_BORDER);
        context.fillGradient(x + pad, y + pad, x + size - pad, y + size - pad, innerWellTop, innerWellBot);

        // Halo pulsante exterior cuando está seleccionado o es el siguiente objetivo
        if (selected) {
            context.drawBorder(x - 3, y - 3, size + 6, size + 6, GOLD_BRIGHT);
        } else if (hovered) {
            context.drawBorder(x - 2, y - 2, size + 4, size + 4, PARCHMENT_BRIGHT);
        }
    }

    /**
     * Dibuja un pequeño candado pixel-art en la esquina de un nodo bloqueado.
     */
    public static void drawPixelPadlock(DrawContext context, int cx, int cy) {
        // Arco metálico superior
        context.fill(cx - 3, cy - 6, cx + 3, cy - 2, WOOD_OUTER_BORDER);
        context.fill(cx - 2, cy - 5, cx + 2, cy - 2, 0xFFC7CFD6);
        context.fill(cx - 1, cy - 4, cx + 1, cy - 2, WOOD_DARK);
        // Cuerpo del candado en hierro/oro viejo
        context.fill(cx - 5, cy - 2, cx + 5, cy + 6, WOOD_OUTER_BORDER);
        context.fill(cx - 4, cy - 1, cx + 4, cy + 5, GOLD_DEEP);
        context.fill(cx - 4, cy - 1, cx + 3, cy, GOLD_BRIGHT);
        // Ojo de cerradura
        context.fill(cx - 1, cy + 1, cx + 1, cy + 4, WOOD_OUTER_BORDER);
    }

    /**
     * Dibuja una silueta pixel-art misteriosa con signo "?" para jefes aún no revelados / en desarrollo.
     */
    public static void drawMysteryBossEmblem(DrawContext context, TextRenderer textRenderer, int cx, int cy, float scale, boolean twinBosses) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(cx, cy, 0.0F);
        matrices.scale(scale, scale, 1.0F);

        int silColor = 0xFF170E09;
        int rimColor = 0x44000000;

        if (twinBosses) {
            // Silueta menor frontal (Mini-Jefe) + Silueta colosal trasera (Final Boss) para Palacio Nacional
            drawHumanoidSilhouettePixels(context, -8, 2, 0.78F, 0xFF26170F);
            drawHumanoidSilhouettePixels(context, 5, -2, 1.05F, silColor);
        } else {
            drawHumanoidSilhouettePixels(context, 0, 0, 1.0F, silColor);
        }

        // Aura sutil en la base
        context.fill(-12, 13, 12, 14, rimColor);

        // Signo "?" central en dorado/escarlata
        String q = "?";
        int qw = textRenderer.getWidth(q);
        context.drawText(textRenderer, q, -qw / 2 + 1, -3, 0xFF000000, false);
        context.drawText(textRenderer, q, -qw / 2, -4, GOLD_BRIGHT, false);

        matrices.pop();
    }

    private static void drawHumanoidSilhouettePixels(DrawContext context, int ox, int oy, float s, int color) {
        int headR = Math.max(3, Math.round(4 * s));
        int shoulderW = Math.max(6, Math.round(9 * s));
        int torsoH = Math.max(8, Math.round(12 * s));
        // Cabeza
        context.fill(ox - headR, oy - torsoH - headR * 2 + 4, ox + headR, oy - torsoH + 4, color);
        // Hombros y capa/torso
        context.fill(ox - shoulderW, oy - torsoH + 4, ox + shoulderW, oy + 4, color);
        context.fill(ox - shoulderW + 2, oy + 4, ox + shoulderW - 2, oy + 12, color);
    }

    /**
     * Renderiza una entidad 3D viva ya sea a todo color o como una silueta oscura impenetrable (Modo Sin Spoilers).
     */
    public static void renderEntityViewport(
            DrawContext context,
            int scissorLeft,
            int scissorTop,
            int scissorRight,
            int scissorBottom,
            float centerX,
            float centerY,
            float scale,
            Vector3f translation,
            float yawDeg,
            float pitchDeg,
            LivingEntity entity,
            boolean silhouetteMode
    ) {
        if (scissorRight <= scissorLeft || scissorBottom <= scissorTop || entity == null) {
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

        float pitchRad = (float) Math.toRadians(pitchDeg);
        float yawRad = (float) Math.toRadians(yawDeg);
        Quaternionf rotation = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateX(pitchRad)
                .rotateY(yawRad);
        Quaternionf cameraOverride = new Quaternionf().rotateX(pitchRad);

        context.enableScissor(scissorLeft, scissorTop, scissorRight, scissorBottom);
        try {
            if (!silhouetteMode) {
                InventoryScreen.drawEntity(context, centerX, centerY, scale, translation, rotation, cameraOverride, entity);
            } else {
                drawSilhouetteEntity(context, centerX, centerY, scale, translation, rotation, cameraOverride, entity);
            }
        } catch (Throwable ignored) {
            // Proteger la interfaz ante cualquier excepción puntual de renderizado de modelo
        } finally {
            context.disableScissor();
        }
    }

    private static void drawSilhouetteEntity(
            DrawContext context,
            float x,
            float y,
            float scale,
            Vector3f translation,
            Quaternionf rotation,
            @Nullable Quaternionf overrideCameraAngle,
            LivingEntity entity
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.draw();

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 50.0D);
        matrices.scale(scale, scale, -scale);
        matrices.translate(translation.x, translation.y, translation.z);
        matrices.multiply(rotation);

        DiffuseLighting.enableForLevel();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        if (overrideCameraAngle != null) {
            overrideCameraAngle.conjugate();
            dispatcher.setRotation(overrideCameraAngle);
        }
        dispatcher.setRenderShadows(false);

        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        SilhouetteVertexConsumerProvider silhouetteProvider = new SilhouetteVertexConsumerProvider(immediate, 20, 12, 8);
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 1.0F, matrices, silhouetteProvider, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        immediate.draw();

        dispatcher.setRenderShadows(true);
        matrices.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }

    public static void drawScaledItem(DrawContext context, ItemStack stack, float centerX, float centerY, float scale) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(centerX - 8.0F * scale, centerY - 8.0F * scale, 150.0F * (1.0F - scale));
        matrices.scale(scale, scale, scale);
        context.drawItem(stack, 0, 0);
        matrices.pop();
    }

    public static void drawScaledText(DrawContext context, TextRenderer textRenderer, String text, float x, float y, float scale, int color, boolean shadow) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, text, 0, 0, color, shadow);
        matrices.pop();
    }

    public static void drawCenteredScaledText(DrawContext context, TextRenderer textRenderer, String text, float centerX, float y, float scale, int color, boolean shadow) {
        float w = textRenderer.getWidth(text) * scale;
        drawScaledText(context, textRenderer, text, centerX - w / 2.0F, y, scale, color, shadow);
    }

    public static void drawParchmentScrollbar(DrawContext context, int x, int y, int trackH, int scroll, int maxScroll, int viewH) {
        if (maxScroll <= 0 || trackH <= 8) {
            return;
        }
        context.fill(x, y, x + 4, y + trackH, 0x55694D2C);
        int thumbH = Math.max(16, (int) (trackH * (viewH / (float) (viewH + maxScroll))));
        int thumbY = y + (int) ((trackH - thumbH) * (scroll / (float) maxScroll));
        context.fill(x, thumbY, x + 4, thumbY + thumbH, WOOD_DARK);
        context.fill(x + 1, thumbY + 1, x + 3, thumbY + thumbH - 1, SCARLET_MAIN);
    }

    public static int lerpArgb(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        int ra = MathHelper.lerp(t, aa, ba);
        int rr = MathHelper.lerp(t, ar, br);
        int rg = MathHelper.lerp(t, ag, bg);
        int rb = MathHelper.lerp(t, ab, bb);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    /**
     * Wrapper de VertexConsumerProvider que transforma cualquier modelo 3D (GeckoLib o Vanilla)
     * en una silueta pura de tinta oscura respetando el canal alfa de su textura.
     */
    private static final class SilhouetteVertexConsumerProvider implements VertexConsumerProvider {
        private final VertexConsumerProvider delegate;
        private final int silR;
        private final int silG;
        private final int silB;

        SilhouetteVertexConsumerProvider(VertexConsumerProvider delegate, int silR, int silG, int silB) {
            this.delegate = delegate;
            this.silR = silR;
            this.silG = silG;
            this.silB = silB;
        }

        @Override
        public VertexConsumer getBuffer(RenderLayer layer) {
            VertexConsumer base = delegate.getBuffer(layer);
            return new VertexConsumer() {
                @Override
                public VertexConsumer vertex(float x, float y, float z) {
                    base.vertex(x, y, z);
                    return this;
                }

                @Override
                public VertexConsumer color(int red, int green, int blue, int alpha) {
                    base.color(silR, silG, silB, alpha);
                    return this;
                }

                @Override
                public VertexConsumer texture(float u, float v) {
                    base.texture(u, v);
                    return this;
                }

                @Override
                public VertexConsumer overlay(int u, int v) {
                    base.overlay(u, v);
                    return this;
                }

                @Override
                public VertexConsumer light(int u, int v) {
                    base.light(u, v);
                    return this;
                }

                @Override
                public VertexConsumer normal(float x, float y, float z) {
                    base.normal(x, y, z);
                    return this;
                }
            };
        }
    }
}
