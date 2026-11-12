package pruebano1.modid.casino;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import pruebano1.modid.Prueba_no1;

import java.util.List;

/**
 * LISTA DE CANCIONES DEL DJ DEL CASINO.
 *
 * Cada canción necesita su archivo .ogg (OGG Vorbis, NO mp3) en:
 *     src/main/resources/assets/prueba_no1/sounds/casino/<id>.ogg
 * y su entrada en assets/prueba_no1/sounds.json con el nombre "casino_music_<id>".
 *
 * Para agregar una canción: convierte el audio a .ogg, ponle un id sin espacios ni mayúsculas,
 * cópialo a la carpeta de arriba, añade una línea aquí y su entrada en sounds.json.
 */
public final class CasinoSongs {

    public record Song(String id, String title, SoundEvent sound) {
    }

    public static final List<Song> SONGS = List.of(
            song("brazino_777", "Brazino 777 El Juego De Esta Era - CruzXO"),
            song("ebb_tide", "Ebb Tide"),
            song("glide_with_me", "Glide with me - David Luong"),
            song("hit_the_road_jack", "Hit The Road Jack - Ray Charles"),
            song("voy_buscando", "Voy Buscando un Travesti - Cionibe")
    );

    private CasinoSongs() {
    }

    private static Song song(String id, String title) {
        Identifier soundId = Identifier.of(Prueba_no1.MOD_ID, "casino_music_" + id);
        SoundEvent sound = Registry.register(Registries.SOUND_EVENT, soundId, SoundEvent.of(soundId));
        return new Song(id, title, sound);
    }

    public static boolean isValid(int index) {
        return index >= 0 && index < SONGS.size();
    }

    /** Fuerza la carga de la clase (y el registro de los sonidos) durante la inicialización del mod. */
    public static void registerCasinoSongs() {
        Prueba_no1.LOGGER.info("Registrando {} canciones del casino para {}", SONGS.size(), Prueba_no1.MOD_ID);
    }
}
