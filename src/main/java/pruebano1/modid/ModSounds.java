package pruebano1.modid;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final Identifier PI_DIDI_BOSS_MUSIC_ID = Identifier.of(Prueba_no1.MOD_ID, "pi_didi_boss_music");
    public static final SoundEvent PI_DIDI_BOSS_MUSIC = Registry.register(
            Registries.SOUND_EVENT,
            PI_DIDI_BOSS_MUSIC_ID,
            SoundEvent.of(PI_DIDI_BOSS_MUSIC_ID)
    );

    public static final Identifier FER_OVILLA_BANANA_HIT_ID = Identifier.of(Prueba_no1.MOD_ID, "fer_ovilla_banana_hit");
    public static final SoundEvent FER_OVILLA_BANANA_HIT = Registry.register(
            Registries.SOUND_EVENT,
            FER_OVILLA_BANANA_HIT_ID,
            SoundEvent.of(FER_OVILLA_BANANA_HIT_ID)
    );

    public static final Identifier PLAYER_DEATH_KO_ID = Identifier.of(Prueba_no1.MOD_ID, "player_death_ko");
    public static final SoundEvent PLAYER_DEATH_KO = Registry.register(
            Registries.SOUND_EVENT,
            PLAYER_DEATH_KO_ID,
            SoundEvent.of(PLAYER_DEATH_KO_ID)
    );

    public static final Identifier VALENTINA_EXPLOSION_ID = Identifier.of(Prueba_no1.MOD_ID, "valentina_explosion");
    public static final SoundEvent VALENTINA_EXPLOSION = Registry.register(
            Registries.SOUND_EVENT,
            VALENTINA_EXPLOSION_ID,
            SoundEvent.of(VALENTINA_EXPLOSION_ID)
    );

    public static final Identifier JEFRI_BOSS_MUSIC_ID = Identifier.of(Prueba_no1.MOD_ID, "jefri_boss_music");
    public static final SoundEvent JEFRI_BOSS_MUSIC = Registry.register(
            Registries.SOUND_EVENT,
            JEFRI_BOSS_MUSIC_ID,
            SoundEvent.of(JEFRI_BOSS_MUSIC_ID)
    );

    public static final Identifier SONIDO_ATAQUE_ESPECIAL_1_ID = Identifier.of(Prueba_no1.MOD_ID, "sonido_ataqueespecial1");
    public static final SoundEvent SONIDO_ATAQUE_ESPECIAL_1 = Registry.register(
            Registries.SOUND_EVENT,
            SONIDO_ATAQUE_ESPECIAL_1_ID,
            SoundEvent.of(SONIDO_ATAQUE_ESPECIAL_1_ID)
    );

    public static final Identifier JEFRI_DEATH_FART_ID = Identifier.of(Prueba_no1.MOD_ID, "jefri_death_fart");
    public static final SoundEvent JEFRI_DEATH_FART = Registry.register(
            Registries.SOUND_EVENT,
            JEFRI_DEATH_FART_ID,
            SoundEvent.of(JEFRI_DEATH_FART_ID)
    );

    // SONIDOS DE LA DRAFTEA (en sounds.json apuntan a sonidos de vanilla; se pueden cambiar por .ogg propios)
    public static final Identifier DRAFTEA_COIN_ID = Identifier.of(Prueba_no1.MOD_ID, "draftea_coin");
    public static final SoundEvent DRAFTEA_COIN = Registry.register(
            Registries.SOUND_EVENT,
            DRAFTEA_COIN_ID,
            SoundEvent.of(DRAFTEA_COIN_ID)
    );

    public static final Identifier DRAFTEA_SPIN_ID = Identifier.of(Prueba_no1.MOD_ID, "draftea_spin");
    public static final SoundEvent DRAFTEA_SPIN = Registry.register(
            Registries.SOUND_EVENT,
            DRAFTEA_SPIN_ID,
            SoundEvent.of(DRAFTEA_SPIN_ID)
    );

    public static final Identifier DRAFTEA_WIN_ID = Identifier.of(Prueba_no1.MOD_ID, "draftea_win");
    public static final SoundEvent DRAFTEA_WIN = Registry.register(
            Registries.SOUND_EVENT,
            DRAFTEA_WIN_ID,
            SoundEvent.of(DRAFTEA_WIN_ID)
    );

    public static void registerModSounds() {
        Prueba_no1.LOGGER.info("Registrando sonidos personalizados para " + Prueba_no1.MOD_ID);
    }
}
