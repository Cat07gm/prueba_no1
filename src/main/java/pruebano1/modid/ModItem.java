package pruebano1.modid;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.ClearAllEffectsConsumeEffect;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.Unit;
import pruebano1.modid.entity.ModEntities;

import java.util.List;

public class ModItem {

    //Llave y creación del Lingote
    public static final RegistryKey<Item> SCARLET_INGOT_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_ingot"));
    public static final Item SCARLET_INGOT = new Item(new Item.Settings().registryKey(SCARLET_INGOT_KEY));

    //Llaves para las Herramientas y Armas
    public static final RegistryKey<Item> SCARLET_SWORD_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_sword"));
    public static final RegistryKey<Item> SCARLET_PICKAXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_pickaxe"));
    public static final RegistryKey<Item> SCARLET_AXE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_axe"));
    public static final RegistryKey<Item> SCARLET_SHOVEL_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_shovel"));
    public static final RegistryKey<Item> SCARLET_HOE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_hoe"));

    //Creación de las Herramientas
    public static final Item SCARLET_SWORD = new SwordItem(ModToolMaterials.SCARLET, 3, -2.4F, new Item.Settings().registryKey(SCARLET_SWORD_KEY));
    public static final Item SCARLET_PICKAXE = new PickaxeItem(ModToolMaterials.SCARLET, 1, -2.8F, new Item.Settings().registryKey(SCARLET_PICKAXE_KEY));
    public static final Item SCARLET_AXE = new AxeItem(ModToolMaterials.SCARLET, 5.0F, -3.0F, new Item.Settings().registryKey(SCARLET_AXE_KEY));
    public static final Item SCARLET_SHOVEL = new ShovelItem(ModToolMaterials.SCARLET, 1.5F, -3.0F, new Item.Settings().registryKey(SCARLET_SHOVEL_KEY));
    public static final Item SCARLET_HOE = new HoeItem(ModToolMaterials.SCARLET, -3.0F, 0.0F, new Item.Settings().registryKey(SCARLET_HOE_KEY));

    //ARMADURA SCARLET Llaves de Registro
    public static final RegistryKey<Item> SCARLET_HELMET_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_helmet"));
    public static final RegistryKey<Item> SCARLET_CHESTPLATE_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_chestplate"));
    public static final RegistryKey<Item> SCARLET_LEGGINGS_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_leggings"));
    public static final RegistryKey<Item> SCARLET_BOOTS_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarlet_boots"));

    //ARMADURA SCARLET Creación de las 4 piezas
    public static final Item SCARLET_HELMET = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.HELMET,
            new Item.Settings().registryKey(SCARLET_HELMET_KEY)
    );
    public static final Item SCARLET_CHESTPLATE = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.CHESTPLATE,
            new Item.Settings().registryKey(SCARLET_CHESTPLATE_KEY)
    );
    public static final Item SCARLET_LEGGINGS = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.LEGGINGS,
            new Item.Settings().registryKey(SCARLET_LEGGINGS_KEY)
    );
    public static final Item SCARLET_BOOTS = new ArmorItem(
            ModArmorMaterials.SCARLET,
            EquipmentType.BOOTS,
            new Item.Settings().registryKey(SCARLET_BOOTS_KEY)
    );

    //SCARTY (Scarlet + Elytra): Llave y Creación
    public static final RegistryKey<Item> SCARTY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "scarty"));

    public static final Item SCARTY = new Item(
            new Item.Settings()
                    .registryKey(SCARTY_KEY)
                    .maxDamage(532)
                    .rarity(Rarity.RARE)
                    .repairable(ModToolMaterials.SCARLET_REPAIR) // Se repara con Lingote de Escarlata
                    //Permite planear exactamente como una Elytra convencional y usar cohetes
                    .component(DataComponentTypes.GLIDER, Unit.INSTANCE)
                    //Se equipa en el pecho y muestra las alas 3D scarty
                    .component(
                            DataComponentTypes.EQUIPPABLE,
                            EquippableComponent.builder(EquipmentSlot.CHEST)
                                    .equipSound(SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA)
                                    .model(ModArmorMaterials.SCARTY_ASSET_KEY)
                                    .damageOnHurt(true)
                                    .build()
                    )
                    //Atributos especiales al llevarla equipada en el pecho
                    .attributeModifiers(
                            AttributeModifiersComponent.builder()
                                    //Protección de una pechera de hierro (+6 de Armadura)
                                    .add(
                                            EntityAttributes.ARMOR,
                                            new EntityAttributeModifier(
                                                    Identifier.of(Prueba_no1.MOD_ID, "scarty_armor"),
                                                    6.0,
                                                    EntityAttributeModifier.Operation.ADD_VALUE
                                            ),
                                            AttributeModifierSlot.CHEST
                                    )
                                    //+10% más de daño a los ataques
                                    .add(
                                            EntityAttributes.ATTACK_DAMAGE,
                                            new EntityAttributeModifier(
                                                    Identifier.of(Prueba_no1.MOD_ID, "scarty_attack_bonus"),
                                                    0.10,
                                                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                            ),
                                            AttributeModifierSlot.CHEST
                                    )
                                    //Resistencia al daño por caída (-75% de daño recibido al caer)
                                    .add(
                                            EntityAttributes.FALL_DAMAGE_MULTIPLIER,
                                            new EntityAttributeModifier(
                                                    Identifier.of(Prueba_no1.MOD_ID, "scarty_fall_resistance"),
                                                    -0.75,
                                                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                            ),
                                            AttributeModifierSlot.CHEST
                                    )
                                    //+4 bloques extra de caída segura sin recibir nada de daño
                                    .add(
                                            EntityAttributes.SAFE_FALL_DISTANCE,
                                            new EntityAttributeModifier(
                                                    Identifier.of(Prueba_no1.MOD_ID, "scarty_safe_fall"),
                                                    4.0,
                                                    EntityAttributeModifier.Operation.ADD_VALUE
                                            ),
                                            AttributeModifierSlot.CHEST
                                    )
                                    .build()
                    )
    );

    //BRAZALETES DE ESCARLATA Versión 1 y Versión 2 Dorada
    public static final RegistryKey<Item> SCARLET_BRACELET_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "scarlet_bracelet")
    );
    public static final RegistryKey<Item> GOLDEN_SCARLET_BRACELET_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "golden_scarlet_bracelet")
    );

    //Versión 1 Cura 4 corazones (8.0F), Cooldown 25s, 100 usos, reparable con lingote Scarlet y sin encantar
    public static final Item SCARLET_BRACELET = new ScarletBraceletItem(
            8.0F,
            25,
            false,
            new Item.Settings()
                    .registryKey(SCARLET_BRACELET_KEY)
                    .maxDamage(100)
                    .repairable(ModToolMaterials.SCARLET_REPAIR)
    );

    //Versión 2 Dorada Cura 6 corazones (12.0F), Prisa Minera I (10s), Cooldown 22s, 80 usos, reparable y sin encantar
    public static final Item GOLDEN_SCARLET_BRACELET = new ScarletBraceletItem(
            12.0F,
            22,
            true,
            new Item.Settings()
                    .registryKey(GOLDEN_SCARLET_BRACELET_KEY)
                    .maxDamage(80)
                    .rarity(Rarity.RARE)
                    .repairable(ModToolMaterials.SCARLET_REPAIR)
    );

    //TÓTEMS PERSONALIZADOS Solo en Creativo
    public static final RegistryKey<Item> NAZITOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "nazitotem"));
    public static final RegistryKey<Item> ISRATOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "isratotem"));
    public static final RegistryKey<Item> LONTOTEM_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Prueba_no1.MOD_ID, "lontotem"));

    //                                                   NAZITOTEM
    //1 segundo = 20 ticks, se multiplica en status effects - 0 amplifier es 1(ej. regeneracion 1).
    //Regeneración II (5s = 100 ticks), Resistencia I (10s), Resistencia al Fuego (10s),
    //Caída Lenta (10s) y Fuerza II (35s)
    public static final Item NAZITOTEM = new Item(
            new Item.Settings()
                    .registryKey(NAZITOTEM_KEY)
                    .maxCount(1)
                    .rarity(Rarity.UNCOMMON)
                    .component(
                            DataComponentTypes.DEATH_PROTECTION,
                            new DeathProtectionComponent(List.of(
                                    ClearAllEffectsConsumeEffect.INSTANCE,
                                    new ApplyEffectsConsumeEffect(List.of(
                                            new StatusEffectInstance(StatusEffects.REGENERATION, 5 * 20, 1),
                                            new StatusEffectInstance(StatusEffects.RESISTANCE, 10 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 10 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.STRENGTH, 35 * 20, 1)
                                    ))
                            ))
                    )
    );

    //                                      ISRATOTEM
    //Efectos del tótem tradicional (Regeneración II 45s, Resistencia al Fuego 40s) +
    //Absorción de 5 corazones (10.0F (2.0F = 1 corazón)) + Caida lenta por 8 segundos (160) Levitación I por 5 segundos (100 ticks).
    public static final Item ISRATOTEM = new Item(
            new Item.Settings()
                    .registryKey(ISRATOTEM_KEY)
                    .maxCount(1)
                    .rarity(Rarity.RARE)
                    .component(
                            DataComponentTypes.DEATH_PROTECTION,
                            new DeathProtectionComponent(List.of(
                                    ClearAllEffectsConsumeEffect.INSTANCE,
                                    new ApplyEffectsConsumeEffect(List.of(
                                            new StatusEffectInstance(StatusEffects.REGENERATION, 45 * 20, 1),
                                            new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.ABSORPTION, 5 * 20, 2),
                                            new StatusEffectInstance(StatusEffects.SLOW_FALLING, 8 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.LEVITATION, 5 * 20, 0)
                                    ))
                            ))
                    )
    );

    //                                                    LONTOTEM (el mas god)
    //El doble que el totem tradicional (Regeneración II 90s, Resistencia al Fuego 80s, Absorción de 8 corazones por 15 segundos)  +
    //Velocidad I por 25 segundos + Visión Nocturna por 1 minuto + luminiscencia por 15 segundos tambien.
    public static final Item LONTOTEM = new Item(
            new Item.Settings()
                    .registryKey(LONTOTEM_KEY)
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .component(
                            DataComponentTypes.DEATH_PROTECTION,
                            new DeathProtectionComponent(List.of(
                                    ClearAllEffectsConsumeEffect.INSTANCE,
                                    new ApplyEffectsConsumeEffect(List.of(
                                            new StatusEffectInstance(StatusEffects.REGENERATION, 90 * 20, 1),
                                            new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 80 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.ABSORPTION, 15 * 20, 3),
                                            new StatusEffectInstance(StatusEffects.SPEED, 25 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.NIGHT_VISION, 60 * 20, 0),
                                            new StatusEffectInstance(StatusEffects.GLOWING, 15 * 20, 0)
                                    ))
                            ))
                    )
    );

    //ORDEN DE ARRESTO (Trofeo del Mini Jefe Pi Didi para invocar al jefe final de la dungeon)
    public static final RegistryKey<Item> ARREST_WARRANT_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "arrest_warrant")
    );
    public static final Item ARREST_WARRANT = new Item(
            new Item.Settings()
                    .registryKey(ARREST_WARRANT_KEY)
                    .maxCount(1)
                    .rarity(Rarity.RARE)
    );

    //TONY COIN (Moneda para jugar en la máquina Draftea)
    public static final RegistryKey<Item> TONY_COIN_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "tony_coin")
    );
    public static final Item TONY_COIN = new Item(
            new Item.Settings()
                    .registryKey(TONY_COIN_KEY)
                    .maxCount(64)
                    .rarity(Rarity.UNCOMMON)
    );

    //HUEVO GENERADOR DE PI DIDI (Para invocarlo desde Creativo)
    public static final RegistryKey<Item> PI_DIDI_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "pi_didi_spawn_egg")
    );
    public static final Item PI_DIDI_SPAWN_EGG = new SpawnEggItem(
            ModEntities.PI_DIDI,
            new Item.Settings().registryKey(PI_DIDI_SPAWN_EGG_KEY)
    );

    //HUEVO GENERADOR DE YOSTIN BIBER (Secuaz de la Dungeon Isla Epstein)
    public static final RegistryKey<Item> YOSTIN_BIBER_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "yostin_biber_spawn_egg")
    );
    public static final Item YOSTIN_BIBER_SPAWN_EGG = new SpawnEggItem(
            ModEntities.YOSTIN_BIBER,
            new Item.Settings().registryKey(YOSTIN_BIBER_SPAWN_EGG_KEY)
    );

    //HUEVO GENERADOR DE VITOR JUGO (Secuaz flotante de la Dungeon Isla Epstein)
    public static final RegistryKey<Item> VITOR_JUGO_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "vitor_jugo_spawn_egg")
    );
    public static final Item VITOR_JUGO_SPAWN_EGG = new SpawnEggItem(
            ModEntities.VITOR_JUGO,
            new Item.Settings().registryKey(VITOR_JUGO_SPAWN_EGG_KEY)
    );

    //MARIJUANA (Drop de Vitor Jugo, por ahora sin utilidad)
    public static final RegistryKey<Item> MARIJUANA_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "marijuana")
    );
    public static final Item MARIJUANA = new Item(
            new Item.Settings().registryKey(MARIJUANA_KEY)
    );

    //HUEVO GENERADOR DE FER OVILLA (Francotiradora de la Dungeon Isla Epstein)
    public static final RegistryKey<Item> FER_OVILLA_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "fer_ovilla_spawn_egg")
    );
    public static final Item FER_OVILLA_SPAWN_EGG = new SpawnEggItem(
            ModEntities.FER_OVILLA,
            new Item.Settings().registryKey(FER_OVILLA_SPAWN_EGG_KEY)
    );

    //PLÁTANO (Drop comestible de Fer Ovilla: restaura 2 muslitos y medio de hambre)
    public static final RegistryKey<Item> BANANA_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "banana")
    );
    public static final Item BANANA = new Item(
            new Item.Settings()
                    .registryKey(BANANA_KEY)
                    .food(new FoodComponent.Builder().nutrition(5).saturationModifier(0.6F).build())
    );

    //HUEVO GENERADOR DE VALENTINA PRIETA (Secuaz de la Dungeon Isla Epstein)
    public static final RegistryKey<Item> VALENTINA_PRIETA_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "valentina_prieta_spawn_egg")
    );
    public static final Item VALENTINA_PRIETA_SPAWN_EGG = new SpawnEggItem(
            ModEntities.VALENTINA_PRIETA,
            new Item.Settings().registryKey(VALENTINA_PRIETA_SPAWN_EGG_KEY)
    );

    //HUEVO GENERADOR DE JEFRI PENSTEIN (Jefe Final de la Dungeon Isla Penstein)
    public static final RegistryKey<Item> JEFRI_PENSTEIN_SPAWN_EGG_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "jefri_penstein_spawn_egg")
    );
    public static final Item JEFRI_PENSTEIN_SPAWN_EGG = new SpawnEggItem(
            ModEntities.JEFRI_PENSTEIN,
            new Item.Settings().registryKey(JEFRI_PENSTEIN_SPAWN_EGG_KEY)
    );

    //TOALLA KOTEX (Proyectil tipo flecha de Jefri penstein y usable con Arco/Ballesta o lanzándola con la mano)
    public static final RegistryKey<Item> TOALLA_KOTEX_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            Identifier.of(Prueba_no1.MOD_ID, "toalla_kotex")
    );
    public static final Item TOALLA_KOTEX = new ToallaKotexItem(
            new Item.Settings().registryKey(TOALLA_KOTEX_KEY)
    );

    //Método ayudante de registro
    private static Item registerItem(RegistryKey<Item> key, Item item) {
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void registerModItems() {
        Prueba_no1.LOGGER.info("Registrando ítems para " + Prueba_no1.MOD_ID);

        // Registrar lingote y herramientas
        registerItem(SCARLET_INGOT_KEY, SCARLET_INGOT);
        registerItem(SCARLET_SWORD_KEY, SCARLET_SWORD);
        registerItem(SCARLET_PICKAXE_KEY, SCARLET_PICKAXE);
        registerItem(SCARLET_AXE_KEY, SCARLET_AXE);
        registerItem(SCARLET_SHOVEL_KEY, SCARLET_SHOVEL);
        registerItem(SCARLET_HOE_KEY, SCARLET_HOE);

        //ARMADURA SCARLET 3. Registrar en la base de datos
        registerItem(SCARLET_HELMET_KEY, SCARLET_HELMET);
        registerItem(SCARLET_CHESTPLATE_KEY, SCARLET_CHESTPLATE);
        registerItem(SCARLET_LEGGINGS_KEY, SCARLET_LEGGINGS);
        registerItem(SCARLET_BOOTS_KEY, SCARLET_BOOTS);

        //Registrar Scarty (Elytra Scarlet)
        registerItem(SCARTY_KEY, SCARTY);

        //Registrar Brazaletes de Escarlata (Versión 1 y 2)
        registerItem(SCARLET_BRACELET_KEY, SCARLET_BRACELET);
        registerItem(GOLDEN_SCARLET_BRACELET_KEY, GOLDEN_SCARLET_BRACELET);

        //Registrar los 3 Tótems personalizados
        registerItem(NAZITOTEM_KEY, NAZITOTEM);
        registerItem(ISRATOTEM_KEY, ISRATOTEM);
        registerItem(LONTOTEM_KEY, LONTOTEM);

        //Registrar Orden de Arresto y Huevos Generadores
        registerItem(ARREST_WARRANT_KEY, ARREST_WARRANT);
        registerItem(TONY_COIN_KEY, TONY_COIN);
        registerItem(PI_DIDI_SPAWN_EGG_KEY, PI_DIDI_SPAWN_EGG);
        registerItem(YOSTIN_BIBER_SPAWN_EGG_KEY, YOSTIN_BIBER_SPAWN_EGG);
        registerItem(VITOR_JUGO_SPAWN_EGG_KEY, VITOR_JUGO_SPAWN_EGG);
        registerItem(MARIJUANA_KEY, MARIJUANA);
        registerItem(FER_OVILLA_SPAWN_EGG_KEY, FER_OVILLA_SPAWN_EGG);
        registerItem(BANANA_KEY, BANANA);
        registerItem(VALENTINA_PRIETA_SPAWN_EGG_KEY, VALENTINA_PRIETA_SPAWN_EGG);
        registerItem(JEFRI_PENSTEIN_SPAWN_EGG_KEY, JEFRI_PENSTEIN_SPAWN_EGG);
        registerItem(TOALLA_KOTEX_KEY, TOALLA_KOTEX);

        //Pestaña Ingredientes
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(SCARLET_INGOT);
            entries.add(ARREST_WARRANT);
            entries.add(TONY_COIN);
            entries.add(MARIJUANA);
        });

        //Pestaña Comida y Bebida
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(BANANA);
        });

        //Pestaña Huevos Generadores (Spawn Eggs)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> {
            entries.add(JEFRI_PENSTEIN_SPAWN_EGG);
            entries.add(PI_DIDI_SPAWN_EGG);
            entries.add(YOSTIN_BIBER_SPAWN_EGG);
            entries.add(VITOR_JUGO_SPAWN_EGG);
            entries.add(FER_OVILLA_SPAWN_EGG);
            entries.add(VALENTINA_PRIETA_SPAWN_EGG);
        });

        //Pestaña Combate
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(SCARLET_SWORD);
            entries.add(SCARLET_HELMET);
            entries.add(SCARLET_CHESTPLATE);
            entries.add(SCARLET_LEGGINGS);
            entries.add(SCARLET_BOOTS);
            entries.add(SCARTY);
            entries.add(NAZITOTEM);
            entries.add(ISRATOTEM);
            entries.add(LONTOTEM);
            entries.add(TOALLA_KOTEX);
        });

        //Pestaña Herramientas
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(SCARLET_PICKAXE);
            entries.add(SCARLET_AXE);
            entries.add(SCARLET_SHOVEL);
            entries.add(SCARLET_HOE);
            entries.add(SCARTY);
            entries.add(SCARLET_BRACELET);
            entries.add(GOLDEN_SCARLET_BRACELET);
        });
    }
}
