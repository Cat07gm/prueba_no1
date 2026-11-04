package pruebano1.modid.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

public class ModEntities {

    public static final RegistryKey<EntityType<?>> PI_DIDI_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "pi_didi")
    );

    public static final EntityType<PiDidiEntity> PI_DIDI = Registry.register(
            Registries.ENTITY_TYPE,
            PI_DIDI_KEY,
            EntityType.Builder.create(PiDidiEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8F, 2.2F)
                    .maxTrackingRange(64)
                    .build(PI_DIDI_KEY)
    );

    public static void registerModEntities() {
        Prueba_no1.LOGGER.info("Registrando entidades para " + Prueba_no1.MOD_ID);
        FabricDefaultAttributeRegistry.register(PI_DIDI, PiDidiEntity.createPiDidiAttributes());
    }
}
