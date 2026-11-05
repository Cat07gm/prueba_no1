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

    public static void registerModSounds() {
        Prueba_no1.LOGGER.info("Registrando sonidos personalizados para " + Prueba_no1.MOD_ID);
    }
}
