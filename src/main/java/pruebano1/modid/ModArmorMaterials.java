package pruebano1.modid;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;

public class ModArmorMaterials {

    // 1. Llave Visual (En Yarn 1.21.4 se usa EquipmentAssetKeys.REGISTRY_KEY)
    public static final RegistryKey<EquipmentAsset> SCARLET_ASSET_KEY = RegistryKey.of(
            EquipmentAssetKeys.REGISTRY_KEY,
            Identifier.of(Prueba_no1.MOD_ID, "scarlet")
    );

    // 2. El Material Cerrado (Record)
    public static final ArmorMaterial SCARLET = new ArmorMaterial(
            33, // Multiplicador de durabilidad base
            Util.make(new EnumMap<>(EquipmentType.class), map -> {
                map.put(EquipmentType.BOOTS, 3);
                map.put(EquipmentType.LEGGINGS, 6);
                map.put(EquipmentType.CHESTPLATE, 8);
                map.put(EquipmentType.HELMET, 3);
            }), // Puntos de defensa exactos por pieza
            22, // Encantabilidad (Nivel Oro)
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, // Sonido al equipar
            2.5F, // Dureza (Toughness) - Nivel Diamante 2.0F
            0.0F, // Resistencia al empuje
            ModToolMaterials.SCARLET_REPAIR, // Reutilizamos la etiqueta de reparación
            SCARLET_ASSET_KEY // Inyectamos la conexión visual
    );
}