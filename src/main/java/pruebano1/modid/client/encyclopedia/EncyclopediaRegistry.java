package pruebano1.modid.client.encyclopedia;

import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ConsumeEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pruebano1.modid.ModBlocks;
import pruebano1.modid.ModItem;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.DropInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EffectInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.EntityPreviewConfig;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.ObtainingMethod;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.SlotRef;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StatInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureFeatureInfo;
import pruebano1.modid.client.encyclopedia.EncyclopediaEntry.StructureStepInfo;
import pruebano1.modid.entity.ModEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Central catalog registering player-facing descriptions, stats, crafting recipes,
 * 3D entities, and structure guides for the Lon Terry mod.
 */
public final class EncyclopediaRegistry {

    private static List<EncyclopediaEntry> entries;
    private static Map<String, EncyclopediaEntry> byId;

    private EncyclopediaRegistry() {
    }

    public static synchronized List<EncyclopediaEntry> getEntries() {
        ensureInitialized();
        return entries;
    }

    @Nullable
    public static synchronized EncyclopediaEntry getById(@Nullable String id) {
        if (id == null) {
            return null;
        }
        ensureInitialized();
        return byId.get(id);
    }

    public static List<EncyclopediaEntry> filter(EncyclopediaCategory category, String rawQuery) {
        String query = EncyclopediaEntry.normalizeForSearch(rawQuery);
        List<EncyclopediaEntry> result = new ArrayList<>();
        for (EncyclopediaEntry entry : getEntries()) {
            if (entry.belongsTo(category) && entry.matchesQuery(query)) {
                result.add(entry);
            }
        }
        return result;
    }

    private static void ensureInitialized() {
        if (entries != null) {
            return;
        }
        List<EncyclopediaEntry> list = new ArrayList<>();
        registerResources(list);
        registerTools(list);
        registerArmor(list);
        registerAccessories(list);
        registerTotems(list);
        registerBlocks(list);
        registerEntities(list);
        registerFood(list);
        registerMisc(list);
        registerStructures(list);

        Map<String, EncyclopediaEntry> map = new LinkedHashMap<>();
        for (EncyclopediaEntry entry : list) {
            map.put(entry.getId(), entry);
        }
        entries = Collections.unmodifiableList(list);
        byId = Collections.unmodifiableMap(map);
    }

    private static void registerResources(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("scarlet_ingot", "Lingote de Escarlata")
                .type("Material / Lingote")
                .category(EncyclopediaCategory.RESOURCES)
                .icon(ModItem.SCARLET_INGOT)
                .description("Lingote carmesí forjado al fundir Mena de Escarlata. Es el material principal para fabricar armas, herramientas, armaduras, brazaletes y las alas Scarty.")
                .stats(
                        new StatInfo("Calidad", "Superior al Diamante", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Usos principales", "Armas, Armadura y Alas")
                )
                .obtaining(
                        ObtainingMethod.smelting(
                                "Horno",
                                SlotRef.link(ModBlocks.SCARLET_ORE, "scarlet_ore"),
                                SlotRef.of(ModItem.SCARLET_INGOT),
                                "Funde Mena de Escarlata para obtener lingotes."
                        ),
                        ObtainingMethod.blasting(
                                "Alto Horno (Más rápido)",
                                SlotRef.link(ModBlocks.SCARLET_ORE, "scarlet_ore"),
                                SlotRef.of(ModItem.SCARLET_INGOT),
                                "También funciona con la mena de pizarra profunda."
                        ),
                        ObtainingMethod.shapeless(
                                "Mesa de Crafteo",
                                List.of(SlotRef.link(ModBlocks.SCARLET_BLOCK, "scarlet_block")),
                                SlotRef.of(ModItem.SCARLET_INGOT, 9),
                                "Recupera 9 lingotes al deshacer un Bloque de Escarlata."
                        )
                )
                .keywords("scarlet ingot mineral lingote metal fundir")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_ore", "Mena de Escarlata")
                .type("Mineral Subterráneo")
                .category(EncyclopediaCategory.RESOURCES, EncyclopediaCategory.BLOCKS)
                .icon(ModBlocks.SCARLET_ORE)
                .description("Mineral rojizo poco común escondido bajo tierra. Al picarlo obtienes el bloque para fundirlo en un Horno o Alto Horno.")
                .stats(
                        new StatInfo("Altura recomendada", "Nivel 0 a 16", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Herramienta mínima", "Pico de Hierro")
                )
                .drops(
                        DropInfo.link(ModItem.SCARLET_INGOT, "Lingote de Escarlata", "Al fundir en horno", "scarlet_ingot")
                )
                .obtaining(ObtainingMethod.info(
                        "Minería",
                        SlotRef.of(Items.IRON_PICKAXE),
                        "Búscalo en cuevas y minas entre las capas 0 y 16. Necesitas al menos un Pico de Hierro."
                ))
                .keywords("ore mena piedra mineral picar")
                .build());

        list.add(EncyclopediaEntry.builder("deepslate_scarlet_ore", "Mena de Escarlata de Pizarra")
                .type("Mineral Profundo")
                .category(EncyclopediaCategory.RESOURCES, EncyclopediaCategory.BLOCKS)
                .icon(ModBlocks.DEEPSLATE_SCARLET_ORE)
                .description("Variante profunda de la Mena de Escarlata. Es más dura de picar, pero aparece con mayor frecuencia en las cavernas más bajas.")
                .stats(
                        new StatInfo("Altura recomendada", "Nivel -64 a -1", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Herramienta mínima", "Pico de Hierro")
                )
                .drops(
                        DropInfo.link(ModItem.SCARLET_INGOT, "Lingote de Escarlata", "Al fundir en horno", "scarlet_ingot")
                )
                .obtaining(ObtainingMethod.info(
                        "Minería Profunda",
                        SlotRef.of(Items.IRON_PICKAXE),
                        "Encuéntralo en las profundidades de pizarra entre -64 y -1. Requiere Pico de Hierro o superior."
                ))
                .keywords("deepslate ore mena pizarra profunda")
                .build());
    }

    private static void registerTools(List<EncyclopediaEntry> list) {
        SlotRef ingot = SlotRef.link(ModItem.SCARLET_INGOT, "scarlet_ingot");
        SlotRef handle = SlotRef.of(Blocks.COBBLED_DEEPSLATE);

        list.add(EncyclopediaEntry.builder("scarlet_sword", "Espada de Escarlata")
                .type("Arma / Espada")
                .category(EncyclopediaCategory.TOOLS)
                .icon(ModItem.SCARLET_SWORD)
                .description("Espada carmesí con empuñadura reforzada de pizarra profunda. Muy efectiva contra los enemigos y jefes de Isla Penstein.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_SWORD, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("#  ", "#  ", "x  ", '#', ingot, 'x', handle),
                        SlotRef.of(ModItem.SCARLET_SWORD),
                        "Usa Pizarra profunda labrada en lugar de palo para el mango."
                ))
                .keywords("sword espada arma combate")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_pickaxe", "Pico de Escarlata")
                .type("Herramienta / Pico")
                .category(EncyclopediaCategory.TOOLS)
                .icon(ModItem.SCARLET_PICKAXE)
                .description("Pico rápido y resistente capaz de minar obsidiana y cualquier mineral avanzado. Ideal para recibir buenos encantamientos.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_PICKAXE, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("###", " x ", " x ", '#', ingot, 'x', handle),
                        SlotRef.of(ModItem.SCARLET_PICKAXE),
                        "Mango hecho con Pizarra profunda labrada."
                ))
                .keywords("pickaxe pico minar")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_axe", "Hacha de Escarlata")
                .type("Herramienta / Hacha")
                .category(EncyclopediaCategory.TOOLS)
                .icon(ModItem.SCARLET_AXE)
                .description("Hacha pesada de gran filo que tala árboles al instante y asesta golpes muy potentes en combate.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_AXE, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("## ", "#x ", " x ", '#', ingot, 'x', handle),
                        SlotRef.of(ModItem.SCARLET_AXE),
                        "Mango hecho con Pizarra profunda labrada."
                ))
                .keywords("axe hacha madera combate")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_shovel", "Pala de Escarlata")
                .type("Herramienta / Pala")
                .category(EncyclopediaCategory.TOOLS)
                .icon(ModItem.SCARLET_SHOVEL)
                .description("Pala ligera y duradera para despejar tierra, arena, grava o nieve a gran velocidad.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_SHOVEL, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid(" # ", " x ", " x ", '#', ingot, 'x', handle),
                        SlotRef.of(ModItem.SCARLET_SHOVEL),
                        "Mango hecho con Pizarra profunda labrada."
                ))
                .keywords("shovel pala excavar")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_hoe", "Azada de Escarlata")
                .type("Herramienta / Azada")
                .category(EncyclopediaCategory.TOOLS)
                .icon(ModItem.SCARLET_HOE)
                .description("Azada muy resistente para preparar cultivos extensos y limpiar bloques vegetales rápidamente.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_HOE, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("## ", " x ", " x ", '#', ingot, 'x', handle),
                        SlotRef.of(ModItem.SCARLET_HOE),
                        "Mango hecho con Pizarra profunda labrada."
                ))
                .keywords("hoe azada cultivo")
                .build());
    }

    private static void registerArmor(List<EncyclopediaEntry> list) {
        SlotRef ingot = SlotRef.link(ModItem.SCARLET_INGOT, "scarlet_ingot");

        list.add(EncyclopediaEntry.builder("scarlet_helmet", "Casco de Escarlata")
                .type("Armadura / Cabeza")
                .category(EncyclopediaCategory.ARMOR)
                .icon(ModItem.SCARLET_HELMET)
                .description("Yelmo blindado de Escarlata que ofrece alta protección y admite encantamientos fuertes con facilidad.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_HELMET, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("###", "# #", "   ", '#', ingot),
                        SlotRef.of(ModItem.SCARLET_HELMET),
                        "Parte del conjunto completo de Escarlata."
                ))
                .keywords("helmet casco armadura")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_chestplate", "Pechera de Escarlata")
                .type("Armadura / Torso")
                .category(EncyclopediaCategory.ARMOR)
                .icon(ModItem.SCARLET_CHESTPLATE)
                .description("La pieza más protectora del conjunto Escarlata, pensada para resistir los golpes directos de Pi Didi y Jefri Penstein.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_CHESTPLATE, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("# #", "###", "###", '#', ingot),
                        SlotRef.of(ModItem.SCARLET_CHESTPLATE),
                        "Máxima defensa física para el torso."
                ))
                .keywords("chestplate pechera armadura")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_leggings", "Pantalones de Escarlata")
                .type("Armadura / Piernas")
                .category(EncyclopediaCategory.ARMOR)
                .icon(ModItem.SCARLET_LEGGINGS)
                .description("Grebas resistentes de Escarlata para aguantar largas batallas y protegerte contra ataques fuertes.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_LEGGINGS, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("###", "# #", "# #", '#', ingot),
                        SlotRef.of(ModItem.SCARLET_LEGGINGS),
                        "Reparable en yunque con Lingote de Escarlata."
                ))
                .keywords("leggings pantalones grebas armadura")
                .build());

        list.add(EncyclopediaEntry.builder("scarlet_boots", "Botas de Escarlata")
                .type("Armadura / Pies")
                .category(EncyclopediaCategory.ARMOR)
                .icon(ModItem.SCARLET_BOOTS)
                .description("Botas reforzadas de Escarlata que completan la armadura ideal para explorar Isla Penstein.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARLET_BOOTS, List.of(
                        new StatInfo("Reparación", "Lingote de Escarlata")
                )))
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("   ", "# #", "# #", '#', ingot),
                        SlotRef.of(ModItem.SCARLET_BOOTS),
                        "Reparable en yunque con Lingote de Escarlata."
                ))
                .keywords("boots botas armadura")
                .build());

        list.add(EncyclopediaEntry.builder("scarty", "Scarty")
                .type("Armadura / Élitros Blindados")
                .category(EncyclopediaCategory.ARMOR, EncyclopediaCategory.ACCESSORIES)
                .icon(ModItem.SCARTY)
                .description("Élitros evolucionados con blindaje de Escarlata, diamante y membranas de fantasma. Te permiten volar por el cielo mientras conservas armadura, fuerza extra y amortiguación contra caídas.")
                .statsSupplier(() -> buildGearStats(ModItem.SCARTY, List.of(
                        new StatInfo("Daño por caída", "-75%", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Altura segura", "+4 bloques", EncyclopediaEntry.CYAN_STAT_COLOR)
                )))
                .effects(
                        new EffectInfo("Vuelo en Planeo", "Funciona igual que unos Élitros usando cohetes.", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new EffectInfo("Furia Blindada", "Otorga defensa extra en el pecho y +10% de fuerza de ataque.", EncyclopediaEntry.PURPLE_STAT_COLOR),
                        new EffectInfo("Aterrizaje Suave", "Reduce drásticamente el daño al caer desde las alturas.", EncyclopediaEntry.GREEN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("#D#", "#E#", "M M",
                                '#', ingot,
                                'D', SlotRef.of(Items.DIAMOND),
                                'E', SlotRef.of(Items.ELYTRA),
                                'M', SlotRef.of(Items.PHANTOM_MEMBRANE)),
                        SlotRef.of(ModItem.SCARTY),
                        "Combina tus Élitros con Escarlata, un diamante y membranas de fantasma."
                ))
                .keywords("scarty elytra elitros alas volar")
                .build());
    }

    private static void registerAccessories(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("scarlet_bracelet", "Brazalete de Escarlata")
                .type("Accesorio de Curación")
                .category(EncyclopediaCategory.ACCESSORIES)
                .icon(ModItem.SCARLET_BRACELET)
                .description("Reliquia portátil que cura salud al instante cuando haces clic derecho. Muy útil en medio de combates intensos.")
                .stats(
                        new StatInfo("Curación", "4 corazones", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Recarga", "25 seg", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Usos", "100 cargas")
                )
                .effects(
                        new EffectInfo("Curación Instantánea", "Clic derecho recupera 4 corazones al momento.", EncyclopediaEntry.GREEN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("###", "# #", "#D#",
                                '#', SlotRef.link(ModItem.SCARLET_INGOT, "scarlet_ingot"),
                                'D', SlotRef.of(Items.DIAMOND)),
                        SlotRef.of(ModItem.SCARLET_BRACELET),
                        "También puedes mejorarlo rodeándolo de oro."
                ))
                .related("golden_scarlet_bracelet", "Ver Mejora Dorada")
                .keywords("bracelet brazalete curacion vida")
                .build());

        list.add(EncyclopediaEntry.builder("golden_scarlet_bracelet", "Brazalete de Escarlata Dorado")
                .type("Accesorio Mejorado")
                .category(EncyclopediaCategory.ACCESSORIES)
                .icon(ModItem.GOLDEN_SCARLET_BRACELET)
                .description("Versión mejorada del Brazalete de Escarlata. Restaura más vida en menos tiempo de espera y te da Prisa Minera al usarlo.")
                .stats(
                        new StatInfo("Curación", "6 corazones", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Recarga", "22 seg", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Usos", "80 cargas"),
                        new StatInfo("Bono extra", "Prisa Minera I (10s)")
                )
                .effects(
                        new EffectInfo("Curación Mayor", "Clic derecho recupera 6 corazones al instante.", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new EffectInfo("Prisa Minera I", "Picas más rápido durante 10 segundos al activarlo.", EncyclopediaEntry.GOLD_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("GGG", "GBG", "GGG",
                                'G', SlotRef.of(Items.GOLD_INGOT),
                                'B', SlotRef.link(ModItem.SCARLET_BRACELET, "scarlet_bracelet")),
                        SlotRef.of(ModItem.GOLDEN_SCARLET_BRACELET),
                        "Mejora un Brazalete de Escarlata rodeándolo con 8 lingotes de oro."
                ))
                .keywords("bracelet brazalete dorado oro curar haste")
                .build());
    }

    private static void registerTotems(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("nazitotem", "Nazitotem")
                .type("Tótem Especial")
                .category(EncyclopediaCategory.TOTEMS)
                .icon(ModItem.NAZITOTEM)
                .description("Tótem especial que evita que mueras cuando lo llevas en la mano y avisa a todo el mundo con un estruendo al salvar tu vida.")
                .stats(
                        new StatInfo("Protección", "Evita 1 muerte"),
                        new StatInfo("Uso", "En cualquier mano")
                )
                .effectsSupplier(() -> buildDeathProtectionEffects(ModItem.NAZITOTEM, List.of(
                        new EffectInfo("Segunda Oportunidad", "Se consume automáticamente ante un golpe mortal.", EncyclopediaEntry.RED_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Cómo conseguirlo",
                        SlotRef.link(ModItem.NAZITOTEM, "isla_penstein"),
                        "Se consigue explorando Isla Penstein y enfrentando sus desafíos."
                ))
                .keywords("totem nazitotem inmortal muerte")
                .build());

        list.add(EncyclopediaEntry.builder("isratotem", "Isratotem")
                .type("Tótem Protector")
                .category(EncyclopediaCategory.TOTEMS)
                .icon(ModItem.ISRATOTEM)
                .description("Tótem avanzado que te salva de morir, otorga 5 corazones dorados extra de protección y anuncia tu salvación con un estruendo.")
                .stats(
                        new StatInfo("Escudo extra", "5 corazones dorados", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Uso", "En cualquier mano")
                )
                .effectsSupplier(() -> buildDeathProtectionEffects(ModItem.ISRATOTEM, List.of(
                        new EffectInfo("Escudo Dorado", "Garantiza 5 corazones de Absorción justo al revivir.", EncyclopediaEntry.GOLD_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Cómo conseguirlo",
                        SlotRef.link(ModItem.ISRATOTEM, "isla_penstein"),
                        "Recompensa especial ligada a Isla Penstein y los jefes del mod."
                ))
                .keywords("totem isratotem absorcion")
                .build());

        list.add(EncyclopediaEntry.builder("lontotem", "Lontotem")
                .type("Tótem Supremo")
                .category(EncyclopediaCategory.TOTEMS)
                .icon(ModItem.LONTOTEM)
                .description("El tótem más poderoso del mod. Si recibes un golpe letal, te mantiene con vida, te rodea con 8 corazones dorados extra y desata poderosas bendiciones.")
                .stats(
                        new StatInfo("Escudo máximo", "8 corazones dorados", EncyclopediaEntry.PURPLE_STAT_COLOR),
                        new StatInfo("Uso", "En cualquier mano")
                )
                .effectsSupplier(() -> buildDeathProtectionEffects(ModItem.LONTOTEM, List.of(
                        new EffectInfo("Doble Vida", "Te protege con 8 corazones de Absorción y fuertes mejoras temporales.", EncyclopediaEntry.PURPLE_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Reliquia Épica",
                        SlotRef.link(ModItem.LONTOTEM, "isla_penstein"),
                        "Tesoro supremo ligado a los combates más difíciles de Lon Terry."
                ))
                .keywords("totem lontotem epico doble vida")
                .build());
    }

    private static void registerBlocks(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("scarlet_block", "Bloque de Escarlata")
                .type("Bloque / Almacenamiento")
                .category(EncyclopediaCategory.BLOCKS, EncyclopediaCategory.RESOURCES)
                .icon(ModBlocks.SCARLET_BLOCK)
                .description("Bloque metálico compacto ideal para ahorrar espacio en tus cofres o decorar construcciones. Puedes volver a convertirlo en lingotes en cualquier momento.")
                .stats(
                        new StatInfo("Equivalente", "9 Lingotes"),
                        new StatInfo("Herramienta", "Pico de Hierro+")
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        patternGrid("###", "###", "###", '#', SlotRef.link(ModItem.SCARLET_INGOT, "scarlet_ingot")),
                        SlotRef.of(ModBlocks.SCARLET_BLOCK),
                        "Desarmable nuevamente en 9 Lingotes de Escarlata."
                ))
                .keywords("block bloque escarlata almacenamiento")
                .build());

        list.add(EncyclopediaEntry.builder("jefri_mailbox", "Buzón de Jefri")
                .type("Altar de Invocación")
                .category(EncyclopediaCategory.BLOCKS, EncyclopediaCategory.MISC)
                .rarity(EncyclopediaRarity.RARE)
                .icon(ModBlocks.JEFRI_MAILBOX)
                .description("Buzón misterioso situado frente a la entrada de la mansión en Isla Penstein. Entrega una Orden de Arresto aquí para despertar al jefe final.")
                .stats(
                        new StatInfo("Resistencia", "Indestructible"),
                        new StatInfo("Llave", "Orden de Arresto", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Despierta a", "Jefri Penstein", EncyclopediaEntry.RED_STAT_COLOR)
                )
                .effects(
                        new EffectInfo("Invocar Jefe", "Haz clic derecho con la Orden de Arresto en la mano.", EncyclopediaEntry.RED_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.info(
                        "Dónde encontrarlo",
                        SlotRef.link(ModItem.ARREST_WARRANT, "arrest_warrant"),
                        "Aparece frente a la entrada Norte de la Residencia Principal en Isla Penstein."
                ))
                .related("entity_jefri_penstein", "Ver Jefe Jefri 3D")
                .keywords("mailbox buzon jefri altar invocar jefe")
                .build());

        list.add(EncyclopediaEntry.builder("draftea", "Máquina Draftea")
                .type("Máquina de Premios")
                .category(EncyclopediaCategory.BLOCKS, EncyclopediaCategory.MISC)
                .rarity(EncyclopediaRarity.RARE)
                .icon(ModBlocks.DRAFTEA)
                .description("Máquina recreativa de 3 rodillos. Inserta un Tony Coin haciendo clic derecho para ponerla a girar y llevarte recompensas al azar.")
                .stats(
                        new StatInfo("Costo por giro", "1 Tony Coin", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Premio", "Garantizado al girar", EncyclopediaEntry.GREEN_STAT_COLOR)
                )
                .drops(
                        DropInfo.of(Items.EXPERIENCE_BOTTLE, "Frasco de Experiencia", "x8 (25%)"),
                        DropInfo.of(Items.IRON_INGOT, "Lingote de Hierro", "x8 (25%)"),
                        DropInfo.of(Items.STRING, "Hilo", "x8 (25%)"),
                        DropInfo.of(Items.GOLD_INGOT, "Lingote de Oro", "x5 (15%)"),
                        DropInfo.of(Items.DIAMOND, "Diamante", "x2 (5%)"),
                        DropInfo.of(Items.GOLDEN_APPLE, "Manzana Dorada", "x1 (5%)")
                )
                .obtaining(ObtainingMethod.info(
                        "Cómo usarla",
                        SlotRef.link(ModItem.TONY_COIN, "tony_coin"),
                        "Ten un Tony Coin en la mano y haz clic derecho en la máquina para hacerla girar."
                ))
                .related("tony_coin", "Ver Tony Coin")
                .keywords("draftea casino tragamonedas maquina premio")
                .build());

        list.add(EncyclopediaEntry.builder("draftea_light", "Luz Morada Draftea")
                .type("Bloque de Iluminación Neón")
                .category(EncyclopediaCategory.BLOCKS)
                .rarity(EncyclopediaRarity.UNCOMMON)
                .icon(ModBlocks.DRAFTEA_LIGHT)
                .description("Bloque luminoso de color morado característico de las máquinas Draftea con un suave pulso de luz. Perfecto para combinar con decoración negra y blanca en el casino.")
                .stats(
                        new StatInfo("Iluminación", "Nivel 15 (Máxima)", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Color principal", "Morado Neón Draftea", EncyclopediaEntry.PURPLE_STAT_COLOR),
                        new StatInfo("Brillo propio", "Visible en oscuridad")
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        List.of(
                                SlotRef.of(Items.PURPLE_DYE), SlotRef.of(Items.QUARTZ), SlotRef.of(Items.PURPLE_DYE),
                                SlotRef.of(Items.QUARTZ), SlotRef.of(Items.GLOWSTONE), SlotRef.of(Items.QUARTZ),
                                SlotRef.of(Items.PURPLE_DYE), SlotRef.of(Items.QUARTZ), SlotRef.of(Items.PURPLE_DYE)
                        ),
                        SlotRef.of(ModBlocks.DRAFTEA_LIGHT, 4),
                        "Receta de iluminación característica del casino Draftea Tony Diaz."
                ))
                .related("casino_light", "Ver Luz de Casino")
                .keywords("luz draftea morado neon iluminacion casino bloque")
                .build());

        list.add(EncyclopediaEntry.builder("casino_light", "Luz Animada de Casino")
                .type("Bloque de Iluminación Animado")
                .category(EncyclopediaCategory.BLOCKS)
                .rarity(EncyclopediaRarity.RARE)
                .icon(ModBlocks.CASINO_LIGHT)
                .description("Bloque de neón animado para letreros, fachadas y carruseles que alterna entre morado, rojo, amarillo y verde con destellos de transición al estilo de un casino clásico.")
                .stats(
                        new StatInfo("Iluminación", "Nivel 15 (Máxima)", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Colores", "Morado, Rojo, Amarillo y Verde", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Animación", "Transición brillante", EncyclopediaEntry.GREEN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        List.of(
                                SlotRef.of(Items.GLOWSTONE_DUST), SlotRef.of(Items.RED_DYE), SlotRef.of(Items.GLOWSTONE_DUST),
                                SlotRef.of(Items.PURPLE_DYE), SlotRef.link(ModBlocks.DRAFTEA_LIGHT, "draftea_light"), SlotRef.of(Items.GREEN_DYE),
                                SlotRef.of(Items.GLOWSTONE_DUST), SlotRef.of(Items.YELLOW_DYE), SlotRef.of(Items.GLOWSTONE_DUST)
                        ),
                        SlotRef.of(ModBlocks.CASINO_LIGHT, 4),
                        "Combina tinta de los 4 colores con Polvo de Piedra Luminosa y una Luz Morada Draftea."
                ))
                .related("draftea_tony_diaz", "Ver Casino Draftea Tony Diaz")
                .keywords("luz casino animada colores morado rojo amarillo verde neon iluminacion")
                .build());

        list.add(EncyclopediaEntry.builder("casino_chair", "Silla Negra de Casino")
                .type("Mobiliario · Asiento Funcional")
                .category(EncyclopediaCategory.BLOCKS)
                .rarity(EncyclopediaRarity.COMMON)
                .icon(ModBlocks.CASINO_CHAIR)
                .description("Elegante banquito negro de casino con pedestal metálico, reposapiés cromado y respaldo acolchado. Haz clic derecho sobre la silla para sentarte frente a las máquinas Draftea, mesas de juego o en la barra del bar.")
                .stats(
                        new StatInfo("Interacción", "Clic derecho para sentarse", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Acabado", "Cuero negro y acero cromado", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Uso", "Salón Draftea y Barra VIP")
                )
                .obtaining(ObtainingMethod.shaped(
                        "Mesa de Crafteo",
                        List.of(
                                SlotRef.empty(), SlotRef.of(Items.BLACK_WOOL), SlotRef.empty(),
                                SlotRef.empty(), SlotRef.of(Items.IRON_INGOT), SlotRef.empty(),
                                SlotRef.empty(), SlotRef.of(Items.POLISHED_BLACKSTONE), SlotRef.empty()
                        ),
                        SlotRef.of(ModBlocks.CASINO_CHAIR, 2),
                        "Fabricable con Lana Negra, Lingote de Hierro y Piedra Negra Pulida."
                ))
                .related("draftea", "Ver Máquina Draftea")
                .keywords("silla negra casino banquito taburete asiento sentarse")
                .build());
    }

    private static void registerEntities(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("entity_jefri_penstein", "Jefri Penstein")
                .type("Jefe Final · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.EPIC)
                .icon(ModBlocks.JEFRI_MAILBOX)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.JEFRI_PENSTEIN,
                        50.0F, 13.0F, 1.05F, 22.0F, 8.0F, true
                ))
                .description("El gran jefe de Isla Penstein. Llega con una entrada espectacular y música propia; cuando pierde suficiente vida entra en su temible Modo Diablo de furia carmesí.")
                .statsSupplier(() -> buildEntityStats(ModEntities.JEFRI_PENSTEIN, List.of(
                        new StatInfo("Peligro", "Jefe Final", EncyclopediaEntry.RED_STAT_COLOR),
                        new StatInfo("Fases", "Normal / Modo Diablo", EncyclopediaEntry.PURPLE_STAT_COLOR)
                )))
                .effects(
                        new EffectInfo("Modo Diablo", "Segunda fase agresiva donde su cuerpo brilla en rojo. Usa el botón de arriba para ver cómo luce.", EncyclopediaEntry.RED_STAT_COLOR),
                        new EffectInfo("Cómo invocarlo", "Usa una Orden de Arresto en el Buzón de Jefri de la isla.", EncyclopediaEntry.CYAN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.info(
                        "Invocación",
                        SlotRef.link(ModItem.ARREST_WARRANT, "arrest_warrant"),
                        "Lleva una Orden de Arresto al Buzón de Jefri en la entrada de la mansión de Isla Penstein."
                ))
                .related("jefri_mailbox", "Ver Buzón de Jefri")
                .keywords("jefri penstein boss jefe 3d devil modo diablo")
                .build());

        list.add(EncyclopediaEntry.builder("entity_pi_didi", "Pi Didi")
                .type("Jefe de Mansión · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.EPIC)
                .icon(Items.GOLD_BLOCK)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.PI_DIDI,
                        50.0F, 13.0F, 1.05F, -20.0F, 8.0F, false
                ))
                .description("Poderoso jefe que custodia el salón del trono dentro de la Gran Residencia Principal en Isla Penstein. Golpea muy duro cuerpo a cuerpo, así que no vayas sin buena armadura.")
                .statsSupplier(() -> buildEntityStats(ModEntities.PI_DIDI, List.of(
                        new StatInfo("Peligro", "Jefe", EncyclopediaEntry.RED_STAT_COLOR),
                        new StatInfo("Ubicación", "Mansión de la Isla", EncyclopediaEntry.CYAN_STAT_COLOR)
                )))
                .effects(
                        new EffectInfo("Guardián del Trono", "Defiende el interior de la mansión donde se esconden los cofres de tesoro.", EncyclopediaEntry.GOLD_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.info(
                        "Dónde habita",
                        SlotRef.link(Items.FILLED_MAP, "isla_penstein"),
                        "Se encuentra en el salón central de la mansión en Isla Penstein."
                ))
                .related("isla_penstein", "Ir a Isla Penstein")
                .keywords("pi didi boss jefe 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_yostin_biber", "Yostin Biber")
                .type("Enemigo · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.RARE)
                .icon(Items.IRON_SWORD)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.YOSTIN_BIBER,
                        82.0F, 22.0F, 0.52F, 22.0F, 8.0F, false
                ))
                .description("El más común de los niños hostiles que protegen Isla Penstein. Suele atacar en grupos pequeños por toda la explanada.")
                .statsSupplier(() -> buildEntityStats(ModEntities.YOSTIN_BIBER, List.of(
                        new StatInfo("Frecuencia", "Muy común en la isla"),
                        new StatInfo("Comportamiento", "Ataca en grupo")
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde aparece",
                        SlotRef.link(Items.FILLED_MAP, "isla_penstein"),
                        "Patrulla las calles, jardines y villas de Isla Penstein."
                ))
                .related("isla_penstein", "Ir a Isla Penstein")
                .keywords("yostin biber nino enemigo isla 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_vitor_jugo", "Vitor Jugo")
                .type("Enemigo · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.UNCOMMON)
                .icon(Items.IRON_SWORD)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.VITOR_JUGO,
                        68.0F, 18.0F, 0.54F, 40.0F, 24.0F, false
                ))
                .description("Peculiar enemigo de Isla Penstein que flota horizontalmente mientras persigue a cualquier jugador que pise la isla.")
                .statsSupplier(() -> buildEntityStats(ModEntities.VITOR_JUGO, List.of(
                        new StatInfo("Frecuencia", "Poco común en la isla"),
                        new StatInfo("Rasgo", "Postura horizontal")
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde aparece",
                        SlotRef.link(Items.FILLED_MAP, "isla_penstein"),
                        "Aparece dentro de las zonas abiertas de Isla Penstein."
                ))
                .related("isla_penstein", "Ir a Isla Penstein")
                .keywords("vitor jugo nino enemigo isla 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_fer_ovilla", "Fer Ovilla")
                .type("Enemigo · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.UNCOMMON)
                .icon(Items.IRON_SWORD)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.FER_OVILLA,
                        82.0F, 22.0F, 0.52F, -18.0F, 8.0F, false
                ))
                .description("Centinela de Isla Penstein que acompaña a las patrullas exteriores para impedir que alcances la mansión.")
                .statsSupplier(() -> buildEntityStats(ModEntities.FER_OVILLA, List.of(
                        new StatInfo("Frecuencia", "Común en la isla"),
                        new StatInfo("Comportamiento", "Hostil al acercarse")
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde aparece",
                        SlotRef.link(Items.FILLED_MAP, "isla_penstein"),
                        "Patrulla alrededor de la explanada y alberca de Isla Penstein."
                ))
                .related("isla_penstein", "Ir a Isla Penstein")
                .keywords("fer ovilla nino enemigo isla 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_valentina_prieta", "Valentina Prieta")
                .type("Enemigo · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.RARE)
                .icon(Items.IRON_SWORD)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.VALENTINA_PRIETA,
                        82.0F, 22.0F, 0.52F, 18.0F, 8.0F, false
                ))
                .description("Integrante del grupo hostil de Isla Penstein que persigue sin descanso a los intrusos cerca de la residencia.")
                .statsSupplier(() -> buildEntityStats(ModEntities.VALENTINA_PRIETA, List.of(
                        new StatInfo("Frecuencia", "Común en la isla"),
                        new StatInfo("Comportamiento", "Combate cercano")
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde aparece",
                        SlotRef.link(Items.FILLED_MAP, "isla_penstein"),
                        "Ronda el complejo principal de Isla Penstein."
                ))
                .related("isla_penstein", "Ir a Isla Penstein")
                .keywords("valentina prieta nino enemigo isla 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_tony_fase1", "Tony Fase 1")
                .type("Personal del Casino · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.UNCOMMON)
                .icon(Items.BREWING_STAND)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.TONY_FASE1,
                        50.0F, 13.0F, 1.05F, -20.0F, 8.0F, false
                ))
                .description("Bartender oficial del casino Draftea Tony Diaz con su característica sudadera verde. Atiende fijo detrás de la barra batiendo bebidas con coctelera y puliendo vasos.")
                .statsSupplier(() -> buildEntityStats(ModEntities.TONY_FASE1, List.of(
                        new StatInfo("Actitud", "Pasivo", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Rol", "Bartender Fijo", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Ubicación", "Barra Norte del Casino", EncyclopediaEntry.PURPLE_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde habita",
                        SlotRef.link(ModBlocks.CASINO_LIGHT, "draftea_tony_diaz"),
                        "Se encuentra atendiendo el Lounge Bar dentro del casino Draftea Tony Diaz."
                ))
                .related("draftea_tony_diaz", "Ver Casino Draftea Tony Diaz")
                .keywords("tony fase 1 bartender bar sudadera verde pasivo casino 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_tony_fase2", "Tony Fase 2")
                .type("Cliente del Casino · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.COMMON)
                .icon(ModBlocks.DRAFTEA)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.TONY_FASE2,
                        50.0F, 13.0F, 1.05F, 22.0F, 8.0F, false
                ))
                .description("Cliente habitual del casino Draftea Tony Diaz con sudadera blanca. Varios de ellos recorren las salas del casino o se detienen a jugar frente a las máquinas Draftea.")
                .statsSupplier(() -> buildEntityStats(ModEntities.TONY_FASE2, List.of(
                        new StatInfo("Actitud", "Pasivo", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Rol", "Cliente del Casino", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Actividad", "Pasea y juega Drafteas", EncyclopediaEntry.CYAN_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde habita",
                        SlotRef.link(ModBlocks.DRAFTEA, "draftea_tony_diaz"),
                        "Un grupo de clientes pasea y juega por la planta baja y el Mezzanine VIP del casino."
                ))
                .related("draftea_tony_diaz", "Ver Casino Draftea Tony Diaz")
                .keywords("tony fase 2 cliente sudadera blanca pasivo draftea casino 3d")
                .build());

        list.add(EncyclopediaEntry.builder("entity_tony_fase3", "Tony Fase 3")
                .type("Guardia del Casino · Entidad 3D")
                .category(EncyclopediaCategory.ENTITIES)
                .rarity(EncyclopediaRarity.RARE)
                .icon(ModBlocks.CASINO_CHAIR)
                .entityPreview(new EntityPreviewConfig(
                        () -> ModEntities.TONY_FASE3,
                        50.0F, 13.0F, 1.05F, -18.0F, 8.0F, false
                ))
                .description("Personal de seguridad y vigilancia del casino Draftea Tony Diaz con sudadera negra. Dos custodian la entrada principal y cuatro vigilan el interior con firmes posturas de guardia.")
                .statsSupplier(() -> buildEntityStats(ModEntities.TONY_FASE3, List.of(
                        new StatInfo("Actitud", "Pasivo", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Rol", "Guardia de Seguridad", EncyclopediaEntry.PURPLE_STAT_COLOR),
                        new StatInfo("Puestos", "2 en Entrada · 4 en Interior", EncyclopediaEntry.CYAN_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde habita",
                        SlotRef.link(ModBlocks.CASINO_LIGHT, "draftea_tony_diaz"),
                        "Vigila los arcos de entrada y las zonas clave dentro del casino Draftea Tony Diaz."
                ))
                .related("draftea_tony_diaz", "Ver Casino Draftea Tony Diaz")
                .keywords("tony fase 3 guardia seguridad sudadera negra pasivo casino 3d")
                .build());
    }

    private static void registerFood(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("banana", "Banana")
                .type("Comida / Fruta")
                .category(EncyclopediaCategory.FOOD)
                .icon(ModItem.BANANA)
                .description("Fruta tropical dulce ideal para recuperar energía rápido mientras combates o exploras Isla Penstein.")
                .statsSupplier(() -> buildConsumableStats(ModItem.BANANA, List.of(
                        new StatInfo("Tipo", "Alimento rápido")
                )))
                .effectsSupplier(() -> buildConsumableEffects(ModItem.BANANA, Collections.emptyList()))
                .obtaining(ObtainingMethod.info(
                        "Dónde conseguirla",
                        SlotRef.link(ModItem.BANANA, "isla_penstein"),
                        "Se consigue como botín al explorar Isla Penstein."
                ))
                .keywords("banana platano comida alimento")
                .build());
    }

    private static void registerMisc(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("arrest_warrant", "Orden de Arresto")
                .type("Llave de Jefe")
                .category(EncyclopediaCategory.MISC)
                .icon(ModItem.ARREST_WARRANT)
                .description("Documento especial necesario para retar al jefe final. Úsalo con clic derecho frente al Buzón de Jefri en Isla Penstein.")
                .stats(
                        new StatInfo("Cómo usar", "Clic derecho en Buzón"),
                        new StatInfo("Invoca a", "Jefri Penstein", EncyclopediaEntry.RED_STAT_COLOR)
                )
                .effects(
                        new EffectInfo("Despertar al Jefe", "Activa la batalla contra Jefri Penstein al entregarla en su buzón.", EncyclopediaEntry.CYAN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.info(
                        "Uso en la Isla",
                        SlotRef.link(ModBlocks.JEFRI_MAILBOX, "jefri_mailbox"),
                        "Llave ligada al Buzón de Jefri frente a la mansión de Isla Penstein."
                ))
                .related("jefri_mailbox", "Ver Buzón de Jefri")
                .keywords("orden arresto warrant invocar jefri")
                .build());

        list.add(EncyclopediaEntry.builder("toalla_kotex", "Toalla Kotex")
                .type("Proyectil / Munición")
                .category(EncyclopediaCategory.MISC, EncyclopediaCategory.TOOLS)
                .icon(ModItem.TOALLA_KOTEX)
                .description("Proyectil arrojadizo muy rápido que inflige fuerte daño al impactar y también sirve como munición alternativa para arcos.")
                .stats(
                        new StatInfo("Daño al golpear", "4 corazones", EncyclopediaEntry.RED_STAT_COLOR),
                        new StatInfo("Cadencia", "Disparo muy rápido", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Sirve en arcos", "Sí", EncyclopediaEntry.GREEN_STAT_COLOR)
                )
                .effects(
                        new EffectInfo("Lanzamiento Directo", "Haz clic derecho para arrojarla al instante sin necesitar arco.", EncyclopediaEntry.CYAN_STAT_COLOR)
                )
                .obtaining(ObtainingMethod.info(
                        "Dónde conseguirla",
                        SlotRef.link(ModItem.TOALLA_KOTEX, "isla_penstein"),
                        "Botín especial vinculado a las batallas en Isla Penstein."
                ))
                .keywords("toalla kotex proyectil flecha arrojadizo")
                .build());

        list.add(EncyclopediaEntry.builder("marijuana", "Marihuana")
                .type("Consumible Especial")
                .category(EncyclopediaCategory.MISC, EncyclopediaCategory.FOOD)
                .icon(ModItem.MARIJUANA)
                .description("Hierba exótica característica de Isla Penstein que desprende volutas de humo verde al utilizarse.")
                .statsSupplier(() -> buildConsumableStats(ModItem.MARIJUANA, List.of(
                        new StatInfo("Origen", "Isla Penstein", EncyclopediaEntry.GREEN_STAT_COLOR)
                )))
                .effectsSupplier(() -> buildConsumableEffects(ModItem.MARIJUANA, List.of(
                        new EffectInfo("Humo Verde", "Libera nubes de humo aromático verde.", EncyclopediaEntry.GREEN_STAT_COLOR)
                )))
                .obtaining(ObtainingMethod.info(
                        "Dónde conseguirla",
                        SlotRef.link(ModItem.MARIJUANA, "isla_penstein"),
                        "Se encuentra ligada al botín temático de Isla Penstein."
                ))
                .keywords("marijuana marihuana hierba weed consumible")
                .build());

        list.add(EncyclopediaEntry.builder("tony_coin", "Tony Coin")
                .type("Ficha de Premios")
                .category(EncyclopediaCategory.MISC)
                .icon(ModItem.TONY_COIN)
                .description("Moneda dorada especial que acepta la máquina Draftea. Cada moneda garantiza un premio al azar entre experiencia, hierro, oro, diamantes o manzanas doradas.")
                .stats(
                        new StatInfo("Sirve para", "Girar la Draftea", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Mejores premios", "Diamantes y Manzana Dorada", EncyclopediaEntry.CYAN_STAT_COLOR)
                )
                .drops(
                        DropInfo.link(ModBlocks.DRAFTEA, "Ir a Máquina Draftea", "Ver todos los premios", "draftea")
                )
                .obtaining(ObtainingMethod.info(
                        "Dónde conseguirla",
                        SlotRef.link(ModBlocks.DRAFTEA, "draftea"),
                        "Consíguela explorando Isla Penstein y derrotando enemigos del mod."
                ))
                .related("draftea", "Ver Máquina Draftea")
                .keywords("tony coin moneda ficha casino draftea")
                .build());
    }

    private static void registerStructures(List<EncyclopediaEntry> list) {
        list.add(EncyclopediaEntry.builder("isla_penstein", "Isla Penstein")
                .type("Estructura / Isla Dungeon")
                .category(EncyclopediaCategory.STRUCTURES)
                .rarity(EncyclopediaRarity.EPIC)
                .icon(Items.FILLED_MAP)
                .description("Una enorme isla privada en medio del océano profundo llena de lujos y peligros. Alberga villas de huéspedes, una alberca olímpica, un yate atracado y la mansión principal donde te esperan tesoros y dos jefes temibles.")
                .stats(
                        new StatInfo("Tamaño", "Isla Gigante", EncyclopediaEntry.GOLD_STAT_COLOR),
                        new StatInfo("Bioma", "Océano Profundo", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Cofres de tesoro", "4 en la Mansión", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Jefes", "Pi Didi y Jefri", EncyclopediaEntry.RED_STAT_COLOR)
                )
                .structureContents(
                        StructureFeatureInfo.link(
                                "Gran Residencia Principal y Trono",
                                "Mansión blanca de cuarzo custodiada por Pi Didi en su trono dorado. En sus alas laterales esconde 4 cofres con recompensas de alto nivel.",
                                Items.CHEST,
                                "entity_pi_didi"
                        ),
                        StructureFeatureInfo.link(
                                "Altar y Buzón de Jefri",
                                "Frente a la entrada Norte de la mansión encontrarás el Buzón de Jefri sobre un pedestal de oro. Entrega ahí una Orden de Arresto para invocar al jefe final.",
                                ModBlocks.JEFRI_MAILBOX,
                                "jefri_mailbox"
                        ),
                        StructureFeatureInfo.of(
                                "Explanada, Alberca y 4 Villas",
                                "Amplio patio pavimentado con alberca olímpica, fuente de cuarzo, jardines de palmeras, 4 villas decoradas y 2 helipuertos al Sur.",
                                Items.QUARTZ_BLOCK
                        ),
                        StructureFeatureInfo.of(
                                "Muelle Este y Yate de Lujo",
                                "Gran embarcadero de madera en la costa Este con un yate blanco amarrado y escalinata amurallada directo al patio principal.",
                                Items.OAK_BOAT
                        ),
                        StructureFeatureInfo.link(
                                "Patrullas Hostiles",
                                "Toda la isla está vigilada por Yostin Biber, Fer Ovilla, Valentina Prieta y Vitor Jugo, listos para atacar al verte.",
                                Items.IRON_SWORD,
                                "entity_yostin_biber"
                        )
                )
                .structureSteps(
                        StructureStepInfo.link(
                                1,
                                "Prepárate antes de zarpar",
                                "Lleva armadura de Escarlata (o alas Scarty para moverte rápido), buena espada, un Brazalete curativo, comida y un tótem (Isratotem o Lontotem), además de una Orden de Arresto.",
                                ModItem.SCARLET_CHESTPLATE,
                                "scarlet_chestplate"
                        ),
                        StructureStepInfo.link(
                                2,
                                "Limpia la explanada primero",
                                "Llega por el muelle Este o planeando con Scarty y derrota primero a los guardias exteriores (Yostin Biber, Fer Ovilla, Valentina Prieta y Vitor Jugo) para evitar que te acorralen.",
                                ModItem.SCARTY,
                                "scarty"
                        ),
                        StructureStepInfo.link(
                                3,
                                "Asalta la mansión y vence a Pi Didi",
                                "Cruza los arcos principales, derrota a Pi Didi en el centro del salón y saquea los 4 cofres repartidos dentro de la mansión.",
                                ModItem.SCARLET_SWORD,
                                "entity_pi_didi"
                        ),
                        StructureStepInfo.link(
                                4,
                                "Desafía a Jefri Penstein en su buzón",
                                "Cuando estés listo, acércate al Buzón de Jefri frente a la mansión y usa una Orden de Arresto con clic derecho para iniciar la pelea.",
                                ModItem.ARREST_WARRANT,
                                "arrest_warrant"
                        ),
                        StructureStepInfo.link(
                                5,
                                "Sobrevive a su Modo Diablo",
                                "En la segunda mitad del combate, Jefri se torna rojo y ataca con furia. Muévete sin parar, cúrate con tu Brazalete entre golpes y guarda tu Tótem por emergencia.",
                                ModItem.LONTOTEM,
                                "entity_jefri_penstein"
                        )
                )
                .drops(
                        DropInfo.link(ModBlocks.JEFRI_MAILBOX, "Buzón de Jefri", "Altar del jefe", "jefri_mailbox"),
                        DropInfo.link(ModItem.ARREST_WARRANT, "Orden de Arresto", "Llave de invocación", "arrest_warrant"),
                        DropInfo.link(ModItem.LONTOTEM, "Tótems del Mod", "Protección mortal", "lontotem"),
                        DropInfo.link(ModItem.TONY_COIN, "Tony Coin", "Fichas Draftea", "tony_coin")
                )
                .obtaining(ObtainingMethod.info(
                        "Cómo encontrarla",
                        SlotRef.of(Items.FILLED_MAP),
                        "Navega o vuela sobre océanos profundos hasta avistar la gran isla con muelles, palmeras y edificios blancos de cuarzo."
                ))
                .keywords("isla penstein estructura dungeon oceano mansion jefri pi didi como vencer")
                .build());

        list.add(EncyclopediaEntry.builder("draftea_tony_diaz", "Casino Draftea Tony Diaz")
                .type("Estructura Única · Zona Segura")
                .category(EncyclopediaCategory.STRUCTURES)
                .rarity(EncyclopediaRarity.EPIC)
                .icon(ModBlocks.CASINO_LIGHT)
                .description("Monumental casino único por mundo que se genera cerca del spawn inicial. Destaca por su fachada Art-Deco con letrero luminoso CASINO, palmeras, fuentes y dos pisos decorados con carruseles Draftea, mesas de juego, barra de bar y zona VIP. Dentro ningún jugador recibe daño y el recinto está protegido contra destrucción.")
                .stats(
                        new StatInfo("Aparición", "1 única vez cerca del spawn", EncyclopediaEntry.CYAN_STAT_COLOR),
                        new StatInfo("Protección", "Bloques indestructibles", EncyclopediaEntry.GREEN_STAT_COLOR),
                        new StatInfo("Zona Segura", "Invulnerabilidad total (PvP activo)", EncyclopediaEntry.PURPLE_STAT_COLOR),
                        new StatInfo("Entrada", "Subtítulo Draftea Tony Diaz", EncyclopediaEntry.GOLD_STAT_COLOR)
                )
                .structureContents(
                        StructureFeatureInfo.link(
                                "Carruseles y Máquinas Draftea",
                                "Dos carruseles centrales gigantes y múltiples hileras de tragamonedas Draftea con sillas giratorias listas para apostar tus Tony Coins.",
                                ModBlocks.DRAFTEA,
                                "draftea"
                        ),
                        StructureFeatureInfo.link(
                                "Letrero Neón y Luces del Casino",
                                "Fachada escalonada iluminada con luces animadas de casino y bloques de luz morada Draftea en arcos, coronas y marquesina.",
                                ModBlocks.CASINO_LIGHT,
                                "casino_light"
                        ),
                        StructureFeatureInfo.link(
                                "Mesas de Blackjack, Bar y Mezzanine VIP",
                                "Ala este con mesas de juego, zona norte con barra de bar y escalinatas hacia el balcón VIP y escenario musical del segundo piso.",
                                ModBlocks.DRAFTEA_LIGHT,
                                "draftea_light"
                        ),
                        StructureFeatureInfo.link(
                                "Refugio Seguro de Jugadores",
                                "Al cruzar la entrada recibes invulnerabilidad completa contra todo daño mientras juegas o convives con otros jugadores.",
                                ModItem.TONY_COIN,
                                "tony_coin"
                        )
                )
                .structureSteps(
                        StructureStepInfo.link(
                                1,
                                "Ubica el casino saliendo del spawn",
                                "Camina unos 175–220 bloques desde el punto de aparición (normalmente hacia el Norte) buscando la gran fachada negra, blanca y morada con luces de neón y palmeras.",
                                ModBlocks.CASINO_LIGHT,
                                "casino_light"
                        ),
                        StructureStepInfo.link(
                                2,
                                "Entra y juega tus Tony Coins",
                                "Al entrar aparecerá el subtítulo 'Draftea Tony Diaz'. No podrás romper ni colocar bloques en supervivencia y serás invulnerable mientras usas las máquinas Draftea.",
                                ModItem.TONY_COIN,
                                "tony_coin"
                        ),
                        StructureStepInfo.of(
                                3,
                                "Personalización en modo Creativo",
                                "Los administradores en modo Creativo pueden modificar detalles del edificio y guardar la plantilla para futuros mundos con el comando /drafteacasino guardar.",
                                Items.COMMAND_BLOCK
                        )
                )
                .related("draftea", "Ver Máquina Draftea")
                .keywords("casino draftea tony diaz estructura spawn seguro invulnerable tragamonedas")
                .build());
    }

    private static List<StatInfo> buildGearStats(ItemConvertible item, List<StatInfo> extraStats) {
        ItemStack stack = new ItemStack(item);
        List<StatInfo> stats = new ArrayList<>();

        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
                RegistryEntry<EntityAttribute> attr = entry.attribute();
                double value = entry.modifier().value();
                EntityAttributeModifier.Operation op = entry.modifier().operation();

                if (attr.equals(EntityAttributes.ATTACK_DAMAGE)) {
                    if (op == EntityAttributeModifier.Operation.ADD_VALUE) {
                        stats.add(new StatInfo("Daño", formatHeartsOrPts(1.0 + value), EncyclopediaEntry.RED_STAT_COLOR));
                    } else {
                        stats.add(new StatInfo("Fuerza extra", formatPercent(value), EncyclopediaEntry.RED_STAT_COLOR));
                    }
                } else if (attr.equals(EntityAttributes.ARMOR) && op == EntityAttributeModifier.Operation.ADD_VALUE) {
                    stats.add(new StatInfo("Defensa", "+" + formatNumber(value), EncyclopediaEntry.CYAN_STAT_COLOR));
                }
            }
        }

        if (stack.getMaxDamage() > 0) {
            stats.add(new StatInfo("Durabilidad", stack.getMaxDamage() + " usos", EncyclopediaEntry.GREEN_STAT_COLOR));
        }

        stats.addAll(extraStats);
        return stats;
    }

    private static List<StatInfo> buildConsumableStats(ItemConvertible item, List<StatInfo> extraStats) {
        ItemStack stack = new ItemStack(item);
        List<StatInfo> stats = new ArrayList<>();
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            stats.add(new StatInfo("Alimento", "+" + formatNumber(food.nutrition() / 2.0) + " muslos", EncyclopediaEntry.GREEN_STAT_COLOR));
        }
        stats.addAll(extraStats);
        return stats;
    }

    private static List<EffectInfo> buildConsumableEffects(ItemConvertible item, List<EffectInfo> extraEffects) {
        ItemStack stack = new ItemStack(item);
        List<EffectInfo> effects = new ArrayList<>();
        ConsumableComponent consumable = stack.get(DataComponentTypes.CONSUMABLE);
        if (consumable != null) {
            collectConsumeEffects(consumable.onConsumeEffects(), effects);
        }
        effects.addAll(extraEffects);
        return effects;
    }

    private static List<EffectInfo> buildDeathProtectionEffects(ItemConvertible item, List<EffectInfo> extraEffects) {
        ItemStack stack = new ItemStack(item);
        List<EffectInfo> effects = new ArrayList<>();
        DeathProtectionComponent protection = stack.get(DataComponentTypes.DEATH_PROTECTION);
        if (protection != null) {
            collectConsumeEffects(protection.deathEffects(), effects);
        }
        effects.addAll(extraEffects);
        return effects;
    }

    private static void collectConsumeEffects(List<ConsumeEffect> consumeEffects, List<EffectInfo> target) {
        for (ConsumeEffect effect : consumeEffects) {
            if (effect instanceof ApplyEffectsConsumeEffect applyEffects) {
                float chance = applyEffects.probability();
                for (StatusEffectInstance instance : applyEffects.effects()) {
                    String name = Text.translatable(instance.getTranslationKey()).getString();
                    String roman = toRoman(instance.getAmplifier() + 1);
                    int seconds = Math.max(1, instance.getDuration() / 20);
                    String detail = seconds + " seg";
                    if (chance < 0.999F) {
                        detail += " (" + Math.round(chance * 100.0F) + "%)";
                    }
                    target.add(new EffectInfo(name + " " + roman, detail, EncyclopediaEntry.CYAN_STAT_COLOR));
                }
            }
        }
    }

    private static List<StatInfo> buildEntityStats(
            EntityType<? extends LivingEntity> entityType,
            List<StatInfo> extraStats
    ) {
        List<StatInfo> stats = new ArrayList<>();
        if (DefaultAttributeRegistry.hasDefinitionFor(entityType)) {
            DefaultAttributeContainer container = DefaultAttributeRegistry.get(entityType);
            if (container.has(EntityAttributes.MAX_HEALTH)) {
                double hp = container.getValue(EntityAttributes.MAX_HEALTH);
                stats.add(new StatInfo(
                        "Vida",
                        formatNumber(hp / 2.0) + " ❤ (" + formatNumber(hp) + " HP)",
                        EncyclopediaEntry.RED_STAT_COLOR
                ));
            }
            if (container.has(EntityAttributes.ATTACK_DAMAGE)) {
                double dmg = container.getValue(EntityAttributes.ATTACK_DAMAGE);
                stats.add(new StatInfo("Daño", formatHeartsOrPts(dmg), EncyclopediaEntry.GOLD_STAT_COLOR));
            }
            if (container.has(EntityAttributes.ARMOR)) {
                double armor = container.getValue(EntityAttributes.ARMOR);
                if (armor > 0.0) {
                    stats.add(new StatInfo("Defensa", formatNumber(armor), EncyclopediaEntry.CYAN_STAT_COLOR));
                }
            }
        }
        stats.addAll(extraStats);
        return stats;
    }

    private static List<SlotRef> patternGrid(
            String r0, String r1, String r2,
            Object... keyPairs
    ) {
        Map<Character, SlotRef> lookup = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyPairs.length; i += 2) {
            lookup.put((Character) keyPairs[i], (SlotRef) keyPairs[i + 1]);
        }
        String[] rows = {r0, r1, r2};
        List<SlotRef> grid = new ArrayList<>(9);
        for (int row = 0; row < 3; row++) {
            String line = row < rows.length ? rows[row] : "   ";
            for (int col = 0; col < 3; col++) {
                char c = col < line.length() ? line.charAt(col) : ' ';
                grid.add(lookup.getOrDefault(c, SlotRef.empty()));
            }
        }
        return grid;
    }

    private static String formatHeartsOrPts(double hp) {
        return formatNumber(hp / 2.0) + " ❤ (" + formatNumber(hp) + " HP)";
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.01) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String formatPercent(double ratio) {
        long pct = Math.round(ratio * 100.0);
        return (pct >= 0 ? "+" : "") + pct + "%";
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
