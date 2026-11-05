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

    public static final RegistryKey<EntityType<?>> YOSTIN_BIBER_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "yostin_biber")
    );

    public static final EntityType<YostinBiberEntity> YOSTIN_BIBER = Registry.register(
            Registries.ENTITY_TYPE,
            YOSTIN_BIBER_KEY,
            EntityType.Builder.create(YostinBiberEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.45F, 0.95F) // Mitad del tamaño del jugador
                    .maxTrackingRange(64)
                    .build(YOSTIN_BIBER_KEY)
    );

    public static final RegistryKey<EntityType<?>> VITOR_JUGO_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "vitor_jugo")
    );

    public static final EntityType<VitorJugoEntity> VITOR_JUGO = Registry.register(
            Registries.ENTITY_TYPE,
            VITOR_JUGO_KEY,
            EntityType.Builder.create(VitorJugoEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7F, 0.5F) // Acostado: más largo que alto
                    .maxTrackingRange(64)
                    .build(VITOR_JUGO_KEY)
    );

    public static final RegistryKey<EntityType<?>> FER_OVILLA_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "fer_ovilla")
    );

    public static final EntityType<FerOvillaEntity> FER_OVILLA = Registry.register(
            Registries.ENTITY_TYPE,
            FER_OVILLA_KEY,
            EntityType.Builder.create(FerOvillaEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.45F, 0.95F) // Mitad del tamaño del jugador
                    .maxTrackingRange(64)
                    .build(FER_OVILLA_KEY)
    );

    public static final RegistryKey<EntityType<?>> BANANA_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "banana_projectile")
    );

    public static final EntityType<BananaProjectileEntity> BANANA_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            BANANA_PROJECTILE_KEY,
            EntityType.Builder.<BananaProjectileEntity>create(BananaProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.4F, 0.25F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(10)
                    .build(BANANA_PROJECTILE_KEY)
    );

    public static void registerModEntities() {
        Prueba_no1.LOGGER.info("Registrando entidades para " + Prueba_no1.MOD_ID);
        FabricDefaultAttributeRegistry.register(PI_DIDI, PiDidiEntity.createPiDidiAttributes());
        FabricDefaultAttributeRegistry.register(YOSTIN_BIBER, YostinBiberEntity.createYostinBiberAttributes());
        FabricDefaultAttributeRegistry.register(VITOR_JUGO, VitorJugoEntity.createVitorJugoAttributes());
        FabricDefaultAttributeRegistry.register(FER_OVILLA, FerOvillaEntity.createFerOvillaAttributes());
    }
}
