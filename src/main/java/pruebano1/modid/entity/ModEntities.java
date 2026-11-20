package pruebano1.modid.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import pruebano1.modid.Prueba_no1;
import pruebano1.modid.world.CasaBlancaStructure;
import pruebano1.modid.world.IslaPensteinStructure;

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

    public static final RegistryKey<EntityType<?>> VALENTINA_PRIETA_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "valentina_prieta")
    );

    public static final EntityType<ValentinaPrietaEntity> VALENTINA_PRIETA = Registry.register(
            Registries.ENTITY_TYPE,
            VALENTINA_PRIETA_KEY,
            EntityType.Builder.create(ValentinaPrietaEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.45F, 0.95F) // Mitad del tamaño del jugador
                    .maxTrackingRange(64)
                    .build(VALENTINA_PRIETA_KEY)
    );

    public static final RegistryKey<EntityType<?>> JEFRI_PENSTEIN_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "jefri_penstein")
    );

    public static final EntityType<JefriPensteinEntity> JEFRI_PENSTEIN = Registry.register(
            Registries.ENTITY_TYPE,
            JEFRI_PENSTEIN_KEY,
            EntityType.Builder.create(JefriPensteinEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8F, 2.2F) // Tamaño normal de jefe igual a Pi Didi
                    .maxTrackingRange(64)
                    .build(JEFRI_PENSTEIN_KEY)
    );

    public static final RegistryKey<EntityType<?>> JOSE_TORRES_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "jose_torres")
    );

    public static final EntityType<JoseTorresEntity> JOSE_TORRES = Registry.register(
            Registries.ENTITY_TYPE,
            JOSE_TORRES_KEY,
            EntityType.Builder.create(JoseTorresEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8F, 2.2F)
                    .maxTrackingRange(64)
                    .build(JOSE_TORRES_KEY)
    );

    public static final RegistryKey<EntityType<?>> KOTEX_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "kotex_projectile")
    );

    public static final EntityType<KotexProjectileEntity> KOTEX_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            KOTEX_PROJECTILE_KEY,
            EntityType.Builder.<KotexProjectileEntity>create(KotexProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.45F, 0.25F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(10)
                    .build(KOTEX_PROJECTILE_KEY)
    );

    public static final RegistryKey<EntityType<?>> ICE_BALL_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "ice_ball_projectile")
    );

    public static final EntityType<IceBallProjectileEntity> ICE_BALL_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            ICE_BALL_PROJECTILE_KEY,
            EntityType.Builder.<IceBallProjectileEntity>create(IceBallProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.35F, 0.35F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(10)
                    .build(ICE_BALL_PROJECTILE_KEY)
    );

    public static final RegistryKey<EntityType<?>> GIANT_ICE_BALL_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "giant_ice_ball_projectile")
    );

    public static final EntityType<IceBallProjectileEntity> GIANT_ICE_BALL_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            GIANT_ICE_BALL_PROJECTILE_KEY,
            EntityType.Builder.<IceBallProjectileEntity>create(IceBallProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(1.15F, 1.15F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(10)
                    .build(GIANT_ICE_BALL_PROJECTILE_KEY)
    );

    public static final RegistryKey<EntityType<?>> SEAT_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "seat")
    );

    public static final EntityType<SeatEntity> SEAT = Registry.register(
            Registries.ENTITY_TYPE,
            SEAT_KEY,
            EntityType.Builder.<SeatEntity>create(SeatEntity::new, SpawnGroup.MISC)
                    .dimensions(0.0F, 0.0F)
                    .maxTrackingRange(32)
                    .trackingTickInterval(10)
                    .build(SEAT_KEY)
    );

    public static final RegistryKey<EntityType<?>> TONY_FASE1_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "tony_fase1")
    );

    public static final EntityType<TonyFase1Entity> TONY_FASE1 = Registry.register(
            Registries.ENTITY_TYPE,
            TONY_FASE1_KEY,
            EntityType.Builder.<TonyFase1Entity>create(TonyFase1Entity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.9F)
                    .maxTrackingRange(64)
                    .build(TONY_FASE1_KEY)
    );

    public static final RegistryKey<EntityType<?>> TONY_FASE2_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "tony_fase2")
    );

    public static final EntityType<TonyFase2Entity> TONY_FASE2 = Registry.register(
            Registries.ENTITY_TYPE,
            TONY_FASE2_KEY,
            EntityType.Builder.<TonyFase2Entity>create(TonyFase2Entity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.9F)
                    .maxTrackingRange(64)
                    .build(TONY_FASE2_KEY)
    );

    public static final RegistryKey<EntityType<?>> TONY_FASE3_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "tony_fase3")
    );

    public static final EntityType<TonyFase3Entity> TONY_FASE3 = Registry.register(
            Registries.ENTITY_TYPE,
            TONY_FASE3_KEY,
            EntityType.Builder.<TonyFase3Entity>create(TonyFase3Entity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.9F)
                    .maxTrackingRange(64)
                    .build(TONY_FASE3_KEY)
    );

    public static final RegistryKey<EntityType<?>> CHARLIE_KIRK_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "charlie_kirk")
    );

    public static final EntityType<CharlieKirkEntity> CHARLIE_KIRK = Registry.register(
            Registries.ENTITY_TYPE,
            CHARLIE_KIRK_KEY,
            EntityType.Builder.create(CharlieKirkEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8F, 2.2F)
                    .maxTrackingRange(200)
                    .build(CHARLIE_KIRK_KEY)
    );

    public static final RegistryKey<EntityType<?>> WHITE_HOUSE_GUARD_PISTOL_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "white_house_guard_pistol")
    );

    public static final EntityType<WhiteHouseGuardEntity> WHITE_HOUSE_GUARD_PISTOL = Registry.register(
            Registries.ENTITY_TYPE,
            WHITE_HOUSE_GUARD_PISTOL_KEY,
            EntityType.Builder.create(WhiteHouseGuardEntity::createPistolGuard, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 1.95F)
                    .maxTrackingRange(64)
                    .build(WHITE_HOUSE_GUARD_PISTOL_KEY)
    );

    public static final RegistryKey<EntityType<?>> WHITE_HOUSE_GUARD_RIFLE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "white_house_guard_rifle")
    );

    public static final EntityType<WhiteHouseGuardEntity> WHITE_HOUSE_GUARD_RIFLE = Registry.register(
            Registries.ENTITY_TYPE,
            WHITE_HOUSE_GUARD_RIFLE_KEY,
            EntityType.Builder.create(WhiteHouseGuardEntity::createRifleGuard, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 1.95F)
                    .maxTrackingRange(64)
                    .build(WHITE_HOUSE_GUARD_RIFLE_KEY)
    );

    public static final RegistryKey<EntityType<?>> WHITE_HOUSE_GUARD_BAZOOKA_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "white_house_guard_bazooka")
    );

    public static final EntityType<WhiteHouseGuardEntity> WHITE_HOUSE_GUARD_BAZOOKA = Registry.register(
            Registries.ENTITY_TYPE,
            WHITE_HOUSE_GUARD_BAZOOKA_KEY,
            EntityType.Builder.create(WhiteHouseGuardEntity::createBazookaGuard, SpawnGroup.MONSTER)
                    .dimensions(0.6F, 1.95F)
                    .maxTrackingRange(64)
                    .build(WHITE_HOUSE_GUARD_BAZOOKA_KEY)
    );

    public static final RegistryKey<EntityType<?>> GUARD_BULLET_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "guard_bullet_projectile")
    );

    public static final EntityType<GuardBulletProjectileEntity> GUARD_BULLET_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            GUARD_BULLET_PROJECTILE_KEY,
            EntityType.Builder.<GuardBulletProjectileEntity>create(GuardBulletProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(5)
                    .build(GUARD_BULLET_PROJECTILE_KEY)
    );

    public static final RegistryKey<EntityType<?>> GUARD_ROCKET_PROJECTILE_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "guard_rocket_projectile")
    );

    public static final EntityType<GuardRocketProjectileEntity> GUARD_ROCKET_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            GUARD_ROCKET_PROJECTILE_KEY,
            EntityType.Builder.<GuardRocketProjectileEntity>create(GuardRocketProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.45F, 0.45F)
                    .maxTrackingRange(64)
                    .trackingTickInterval(5)
                    .build(GUARD_ROCKET_PROJECTILE_KEY)
    );

    public static void registerModEntities() {
        Prueba_no1.LOGGER.info("Registrando entidades para " + Prueba_no1.MOD_ID);
        FabricDefaultAttributeRegistry.register(PI_DIDI, PiDidiEntity.createPiDidiAttributes());
        FabricDefaultAttributeRegistry.register(YOSTIN_BIBER, YostinBiberEntity.createYostinBiberAttributes());
        FabricDefaultAttributeRegistry.register(VITOR_JUGO, VitorJugoEntity.createVitorJugoAttributes());
        FabricDefaultAttributeRegistry.register(FER_OVILLA, FerOvillaEntity.createFerOvillaAttributes());
        FabricDefaultAttributeRegistry.register(VALENTINA_PRIETA, ValentinaPrietaEntity.createValentinaPrietaAttributes());
        FabricDefaultAttributeRegistry.register(JEFRI_PENSTEIN, JefriPensteinEntity.createJefriPensteinAttributes());
        FabricDefaultAttributeRegistry.register(JOSE_TORRES, JoseTorresEntity.createJoseTorresAttributes());
        FabricDefaultAttributeRegistry.register(TONY_FASE1, TonyFase1Entity.createTonyFase1Attributes());
        FabricDefaultAttributeRegistry.register(TONY_FASE2, TonyFase2Entity.createTonyFase2Attributes());
        FabricDefaultAttributeRegistry.register(TONY_FASE3, TonyFase3Entity.createTonyFase3Attributes());
        FabricDefaultAttributeRegistry.register(CHARLIE_KIRK, CharlieKirkEntity.createCharlieKirkAttributes());
        FabricDefaultAttributeRegistry.register(WHITE_HOUSE_GUARD_PISTOL, WhiteHouseGuardEntity.createGuardAttributes());
        FabricDefaultAttributeRegistry.register(WHITE_HOUSE_GUARD_RIFLE, WhiteHouseGuardEntity.createGuardAttributes());
        FabricDefaultAttributeRegistry.register(WHITE_HOUSE_GUARD_BAZOOKA, WhiteHouseGuardEntity.createGuardAttributes());

        // Permitir spawneo natural de los 4 niños en la Isla Penstein tanto de día como de noche,
        // manteniendo un límite local (máximo 12 niños en un radio de 28 bloques) para que sea abundante pero controlable para 2 jugadores full diamante.
        SpawnRestriction.register(YOSTIN_BIBER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canIslandChildSpawn);
        SpawnRestriction.register(VITOR_JUGO, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canIslandChildSpawn);
        SpawnRestriction.register(FER_OVILLA, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canIslandChildSpawn);
        SpawnRestriction.register(VALENTINA_PRIETA, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canIslandChildSpawn);

        // Solo permitir agentes de Seguridad de la Casa Blanca dentro de La Casa Blanca, bien espaciados y sin grupos grandes
        SpawnRestriction.register(WHITE_HOUSE_GUARD_PISTOL, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canWhiteHouseGuardSpawn);
        SpawnRestriction.register(WHITE_HOUSE_GUARD_RIFLE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canWhiteHouseGuardSpawn);
        SpawnRestriction.register(WHITE_HOUSE_GUARD_BAZOOKA, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntities::canWhiteHouseGuardSpawn);
    }

    public static boolean canWhiteHouseGuardSpawn(
            EntityType<? extends HostileEntity> type,
            ServerWorldAccess world,
            SpawnReason spawnReason,
            BlockPos pos,
            Random random
    ) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }
        if (!CasaBlancaStructure.isPosInCasaBlanca(world, pos)) {
            return false;
        }
        // Lógica equivalente a Isla Penstein (< 12 en radio 28) aumentada un +10% (< 13 en radio 28), evitando amontonamientos
        if (world.getEntitiesByClass(WhiteHouseGuardEntity.class, new Box(pos).expand(7.0D), LivingEntity::isAlive).size() >= 2) {
            return false;
        }
        int nearby28 = world.getEntitiesByClass(WhiteHouseGuardEntity.class, new Box(pos).expand(28.0D), LivingEntity::isAlive).size();
        if (nearby28 >= 13) {
            return false;
        }
        int totalDungeonGuards = world.getEntitiesByClass(WhiteHouseGuardEntity.class, new Box(pos).expand(140.0D), LivingEntity::isAlive).size();
        return totalDungeonGuards < 28;
    }

    private static boolean canIslandChildSpawn(
            EntityType<? extends HostileEntity> type,
            ServerWorldAccess world,
            SpawnReason spawnReason,
            BlockPos pos,
            Random random
    ) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (pos.getY() < 62 || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
            return false;
        }
        // Asegurar que el spawneo natural ocurra únicamente dentro de los 520x520 bloques de la Isla Penstein
        if (!IslaPensteinStructure.isPosOnIsland(world, pos)) {
            return false;
        }
        // Controlar cantidad local para que haya muchos niños en toda la isla sin saturar el combate
        int nearbyCount = world.getEntitiesByClass(HostileEntity.class, new Box(pos).expand(28.0D), Mob -> true).size();
        return nearbyCount < 12;
    }
}
