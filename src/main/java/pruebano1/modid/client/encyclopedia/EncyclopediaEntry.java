package pruebano1.modid.client.encyclopedia;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Immutable catalog entry describing an item, tool, armor, block, structure, or 3D entity in the Lon Terry mod.
 */
public final class EncyclopediaEntry {

    public static final int GOLD_STAT_COLOR = 0xFFF1C40F;
    public static final int CYAN_STAT_COLOR = 0xFF55E6FF;
    public static final int GREEN_STAT_COLOR = 0xFF55FF88;
    public static final int RED_STAT_COLOR = 0xFFFF6B6B;
    public static final int PURPLE_STAT_COLOR = 0xFFD980FA;

    public record StatInfo(String label, String value, int valueColor) {
        public StatInfo(String label, String value) {
            this(label, value, GOLD_STAT_COLOR);
        }
    }

    public record EffectInfo(String name, String detail, int accentColor) {
        public EffectInfo(String name, String detail) {
            this(name, detail, CYAN_STAT_COLOR);
        }
    }

    public record SlotRef(Supplier<ItemStack> stackSupplier, @Nullable String targetEntryId) {
        public static SlotRef empty() {
            return new SlotRef(() -> ItemStack.EMPTY, null);
        }

        public static SlotRef of(ItemConvertible item) {
            return new SlotRef(() -> new ItemStack(item), null);
        }

        public static SlotRef of(ItemConvertible item, int count) {
            return new SlotRef(() -> new ItemStack(item, count), null);
        }

        public static SlotRef link(ItemConvertible item, String targetEntryId) {
            return new SlotRef(() -> new ItemStack(item), targetEntryId);
        }

        public static SlotRef link(ItemConvertible item, int count, String targetEntryId) {
            return new SlotRef(() -> new ItemStack(item, count), targetEntryId);
        }

        public ItemStack getStack() {
            ItemStack stack = stackSupplier.get();
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }

    public record DropInfo(Supplier<ItemStack> iconSupplier, String name, String detail, @Nullable String targetEntryId) {
        public static DropInfo of(ItemConvertible item, String name, String detail) {
            return new DropInfo(() -> new ItemStack(item), name, detail, null);
        }

        public static DropInfo link(ItemConvertible item, String name, String detail, String targetEntryId) {
            return new DropInfo(() -> new ItemStack(item), name, detail, targetEntryId);
        }

        public ItemStack getIcon() {
            ItemStack stack = iconSupplier.get();
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }

    public enum ObtainingType {
        CRAFTING_SHAPED,
        CRAFTING_SHAPELESS,
        SMELTING,
        BLASTING,
        INFO
    }

    public record ObtainingMethod(
            ObtainingType type,
            String title,
            List<SlotRef> inputs,
            SlotRef output,
            String note
    ) {
        public static ObtainingMethod shaped(String title, List<SlotRef> grid9, SlotRef output, String note) {
            return new ObtainingMethod(ObtainingType.CRAFTING_SHAPED, title, grid9, output, note);
        }

        public static ObtainingMethod shapeless(String title, List<SlotRef> inputs, SlotRef output, String note) {
            return new ObtainingMethod(ObtainingType.CRAFTING_SHAPELESS, title, inputs, output, note);
        }

        public static ObtainingMethod smelting(String title, SlotRef input, SlotRef output, String note) {
            return new ObtainingMethod(ObtainingType.SMELTING, title, List.of(input), output, note);
        }

        public static ObtainingMethod blasting(String title, SlotRef input, SlotRef output, String note) {
            return new ObtainingMethod(ObtainingType.BLASTING, title, List.of(input), output, note);
        }

        public static ObtainingMethod info(String title, SlotRef icon, String note) {
            return new ObtainingMethod(ObtainingType.INFO, title, List.of(icon), SlotRef.empty(), note);
        }
    }

    /**
     * Configuration for rendering a live 3D entity in the grid slot and showcase stage.
     */
    public record EntityPreviewConfig(
            Supplier<? extends EntityType<? extends LivingEntity>> entityTypeSupplier,
            float showcaseScale,
            float gridScale,
            float offsetY,
            float defaultYawDeg,
            float defaultPitchDeg,
            boolean supportsDevilMode
    ) {
    }

    /** Describes a notable zone, chest pool, or boss room inside a structure entry. */
    public record StructureFeatureInfo(
            String title,
            String detail,
            @Nullable Supplier<ItemStack> iconSupplier,
            @Nullable String targetEntryId
    ) {
        public static StructureFeatureInfo of(String title, String detail, ItemConvertible icon) {
            return new StructureFeatureInfo(title, detail, () -> new ItemStack(icon), null);
        }

        public static StructureFeatureInfo link(String title, String detail, ItemConvertible icon, String targetEntryId) {
            return new StructureFeatureInfo(title, detail, () -> new ItemStack(icon), targetEntryId);
        }

        public ItemStack getIcon() {
            if (iconSupplier == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = iconSupplier.get();
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }

    /** Describes one tactical step to prepare for, explore, and conquer a structure. */
    public record StructureStepInfo(
            int stepNumber,
            String title,
            String instruction,
            @Nullable Supplier<ItemStack> iconSupplier,
            @Nullable String targetEntryId
    ) {
        public static StructureStepInfo of(int step, String title, String instruction, ItemConvertible icon) {
            return new StructureStepInfo(step, title, instruction, () -> new ItemStack(icon), null);
        }

        public static StructureStepInfo link(int step, String title, String instruction, ItemConvertible icon, String targetEntryId) {
            return new StructureStepInfo(step, title, instruction, () -> new ItemStack(icon), targetEntryId);
        }

        public ItemStack getIcon() {
            if (iconSupplier == null) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = iconSupplier.get();
            return stack == null ? ItemStack.EMPTY : stack;
        }
    }

    private final String id;
    private final String name;
    private final String typeLabel;
    private final EncyclopediaCategory category;
    private final EnumSet<EncyclopediaCategory> extraCategories;
    @Nullable
    private final EncyclopediaRarity explicitRarity;
    private final Supplier<ItemStack> iconSupplier;
    @Nullable
    private final EntityPreviewConfig entityPreview;
    private final String description;
    private final Supplier<List<StatInfo>> statsSupplier;
    private final Supplier<List<EffectInfo>> effectsSupplier;
    private final List<DropInfo> drops;
    private final List<ObtainingMethod> obtainingMethods;
    private final List<StructureFeatureInfo> structureContents;
    private final List<StructureStepInfo> structureSteps;
    private final String normalizedSearchBlob;
    @Nullable
    private final String relatedEntryId;
    @Nullable
    private final String relatedButtonLabel;

    private ItemStack cachedIcon;
    private List<StatInfo> cachedStats;
    private List<EffectInfo> cachedEffects;

    private EncyclopediaEntry(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.typeLabel = builder.typeLabel;
        this.category = builder.category;
        this.extraCategories = builder.extraCategories.isEmpty()
                ? EnumSet.noneOf(EncyclopediaCategory.class)
                : EnumSet.copyOf(builder.extraCategories);
        this.explicitRarity = builder.explicitRarity;
        this.iconSupplier = builder.iconSupplier;
        this.entityPreview = builder.entityPreview;
        this.description = builder.description;
        this.statsSupplier = builder.statsSupplier;
        this.effectsSupplier = builder.effectsSupplier;
        this.drops = Collections.unmodifiableList(new ArrayList<>(builder.drops));
        this.obtainingMethods = Collections.unmodifiableList(new ArrayList<>(builder.obtainingMethods));
        this.structureContents = Collections.unmodifiableList(new ArrayList<>(builder.structureContents));
        this.structureSteps = Collections.unmodifiableList(new ArrayList<>(builder.structureSteps));
        this.relatedEntryId = builder.relatedEntryId;
        this.relatedButtonLabel = builder.relatedButtonLabel;

        String rawSearch = builder.name + " " + builder.typeLabel + " " + builder.id + " " + builder.keywords + " " + builder.description;
        this.normalizedSearchBlob = normalizeForSearch(rawSearch);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public EncyclopediaCategory getCategory() {
        return category;
    }

    public boolean belongsTo(EncyclopediaCategory cat) {
        if (this.category == EncyclopediaCategory.STRUCTURES) {
            return cat == EncyclopediaCategory.STRUCTURES;
        }
        return cat == EncyclopediaCategory.ALL || this.category == cat || this.extraCategories.contains(cat);
    }

    public ItemStack getIcon() {
        if (cachedIcon == null) {
            ItemStack stack = iconSupplier.get();
            cachedIcon = stack == null ? ItemStack.EMPTY : stack;
        }
        return cachedIcon;
    }

    public EncyclopediaRarity getRarity() {
        if (explicitRarity != null) {
            return explicitRarity;
        }
        Rarity vanillaRarity = getIcon().getRarity();
        return switch (vanillaRarity) {
            case UNCOMMON -> EncyclopediaRarity.UNCOMMON;
            case RARE -> EncyclopediaRarity.RARE;
            case EPIC -> EncyclopediaRarity.EPIC;
            default -> EncyclopediaRarity.COMMON;
        };
    }

    public boolean isEntityEntry() {
        return entityPreview != null;
    }

    @Nullable
    public EntityPreviewConfig getEntityPreview() {
        return entityPreview;
    }

    public String getDescription() {
        return description;
    }

    public List<StatInfo> getStats() {
        if (cachedStats == null) {
            cachedStats = Collections.unmodifiableList(new ArrayList<>(statsSupplier.get()));
        }
        return cachedStats;
    }

    public List<EffectInfo> getEffects() {
        if (cachedEffects == null) {
            cachedEffects = Collections.unmodifiableList(new ArrayList<>(effectsSupplier.get()));
        }
        return cachedEffects;
    }

    public List<DropInfo> getDrops() {
        return drops;
    }

    public List<ObtainingMethod> getObtainingMethods() {
        return obtainingMethods;
    }

    public List<StructureFeatureInfo> getStructureContents() {
        return structureContents;
    }

    public List<StructureStepInfo> getStructureSteps() {
        return structureSteps;
    }

    @Nullable
    public String getRelatedEntryId() {
        return relatedEntryId;
    }

    @Nullable
    public String getRelatedButtonLabel() {
        return relatedButtonLabel;
    }

    public boolean matchesQuery(String normalizedQuery) {
        return normalizedQuery.isEmpty() || normalizedSearchBlob.contains(normalizedQuery);
    }

    public static String normalizeForSearch(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private String typeLabel = "";
        private EncyclopediaCategory category = EncyclopediaCategory.MISC;
        private final EnumSet<EncyclopediaCategory> extraCategories = EnumSet.noneOf(EncyclopediaCategory.class);
        @Nullable
        private EncyclopediaRarity explicitRarity = null;
        private Supplier<ItemStack> iconSupplier = () -> ItemStack.EMPTY;
        @Nullable
        private EntityPreviewConfig entityPreview = null;
        private String description = "";
        private Supplier<List<StatInfo>> statsSupplier = Collections::emptyList;
        private Supplier<List<EffectInfo>> effectsSupplier = Collections::emptyList;
        private final List<DropInfo> drops = new ArrayList<>();
        private final List<ObtainingMethod> obtainingMethods = new ArrayList<>();
        private final List<StructureFeatureInfo> structureContents = new ArrayList<>();
        private final List<StructureStepInfo> structureSteps = new ArrayList<>();
        private String keywords = "";
        @Nullable
        private String relatedEntryId = null;
        @Nullable
        private String relatedButtonLabel = null;

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder type(String typeLabel) {
            this.typeLabel = typeLabel;
            return this;
        }

        public Builder category(EncyclopediaCategory category, EncyclopediaCategory... also) {
            this.category = category;
            for (EncyclopediaCategory extra : also) {
                if (extra != category) {
                    this.extraCategories.add(extra);
                }
            }
            return this;
        }

        public Builder rarity(EncyclopediaRarity rarity) {
            this.explicitRarity = rarity;
            return this;
        }

        public Builder icon(ItemConvertible item) {
            this.iconSupplier = () -> new ItemStack(item);
            return this;
        }

        public Builder entityPreview(EntityPreviewConfig config) {
            this.entityPreview = config;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder statsSupplier(Supplier<List<StatInfo>> supplier) {
            this.statsSupplier = supplier;
            return this;
        }

        public Builder stats(StatInfo... stats) {
            List<StatInfo> list = List.of(stats);
            this.statsSupplier = () -> list;
            return this;
        }

        public Builder effectsSupplier(Supplier<List<EffectInfo>> supplier) {
            this.effectsSupplier = supplier;
            return this;
        }

        public Builder effects(EffectInfo... effects) {
            List<EffectInfo> list = List.of(effects);
            this.effectsSupplier = () -> list;
            return this;
        }

        public Builder drops(DropInfo... drops) {
            Collections.addAll(this.drops, drops);
            return this;
        }

        public Builder drops(List<DropInfo> drops) {
            this.drops.addAll(drops);
            return this;
        }

        public Builder obtaining(ObtainingMethod... methods) {
            Collections.addAll(this.obtainingMethods, methods);
            return this;
        }

        public Builder structureContents(StructureFeatureInfo... features) {
            Collections.addAll(this.structureContents, features);
            return this;
        }

        public Builder structureSteps(StructureStepInfo... steps) {
            Collections.addAll(this.structureSteps, steps);
            return this;
        }

        public Builder keywords(String keywords) {
            this.keywords = keywords;
            return this;
        }

        public Builder related(String entryId, String buttonLabel) {
            this.relatedEntryId = entryId;
            this.relatedButtonLabel = buttonLabel;
            return this;
        }

        public EncyclopediaEntry build() {
            return new EncyclopediaEntry(this);
        }
    }
}
