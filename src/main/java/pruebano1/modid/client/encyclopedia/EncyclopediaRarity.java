package pruebano1.modid.client.encyclopedia;

import net.minecraft.util.math.MathHelper;

/**
 * Rarity tiers used by the Lon Terry Encyclopedia UI for slot borders,
 * detail headers, and badges.
 */
public enum EncyclopediaRarity {
    COMMON("COMÚN", 0xFF6B8199, 0xFFA4BCD4, 0xFFD2E0EE, 0xFF202C3D),
    UNCOMMON("POCO COMÚN", 0xFF32D674, 0xFF66FF99, 0xFF70FFA3, 0xFF143825),
    RARE("RARO", 0xFF28BEFF, 0xFF6BE8FF, 0xFF70EEFF, 0xFF123347),
    EPIC("ÉPICO", 0xFFE64DFF, 0xFFFF85FF, 0xFFFF8FFF, 0xFF3B164A);

    private final String displayName;
    private final int borderColor;
    private final int highlightBorderColor;
    private final int textColor;
    private final int badgeBackground;

    EncyclopediaRarity(String displayName, int borderColor, int highlightBorderColor, int textColor, int badgeBackground) {
        this.displayName = displayName;
        this.borderColor = borderColor;
        this.highlightBorderColor = highlightBorderColor;
        this.textColor = textColor;
        this.badgeBackground = badgeBackground;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int getHighlightBorderColor() {
        return highlightBorderColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBadgeBackground() {
        return badgeBackground;
    }

    /**
     * Computes a dynamic border color (pulsing magenta-gold cycle for EPIC,
     * brighter highlight for selected/hovered lower tiers).
     */
    public int getDynamicBorderColor(boolean selectedOrHovered, float animTimeSeconds) {
        if (this == EPIC) {
            float wave = 0.5F + 0.5F * MathHelper.sin(animTimeSeconds * 3.2F);
            int c1 = selectedOrHovered ? 0xFFFF80FF : 0xFFDF4AF5;
            int c2 = selectedOrHovered ? 0xFFFFDF66 : 0xFFF2B632;
            return lerpColor(c1, c2, wave);
        }
        return selectedOrHovered ? highlightBorderColor : borderColor;
    }

    private static int lerpColor(int a, int b, float t) {
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
}
