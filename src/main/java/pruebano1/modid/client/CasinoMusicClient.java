package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import pruebano1.modid.casino.CasinoSongs;
import pruebano1.modid.network.CasinoMusicStatePayload;
import pruebano1.modid.world.DrafteaCasinoManager;

/**
 * Cliente: reproduce la canción elegida por el DJ SOLO mientras el jugador está dentro del casino.
 * Al salir se desvanece en ~1 segundo; al entrar vuelve a sonar.
 * Usa la categoría "Tocadiscos/Bloques musicales", así que también respeta ese control de volumen de las opciones.
 */
public final class CasinoMusicClient {

    /** Cuánto cambia el volumen por tick al entrar/salir o al cambiar de canción (20 ticks = 1 segundo). */
    private static final float FADE_PER_TICK = 0.05F;

    private static int songIndex = -1;
    private static int djVolume = 70; // No llamarlo "volume": choca con el campo volume de los sonidos
    private static int playSerial = 0;

    private static CasinoSongInstance current = null;

    private CasinoMusicClient() {
    }

    public static void onState(CasinoMusicStatePayload payload) {
        songIndex = payload.songIndex();
        djVolume = payload.volume();
        playSerial = payload.playSerial();
    }

    /** Para la interfaz: ¿el jugador está dentro del casino (donde se escucha la música)? */
    public static boolean isLocalPlayerInsideCasino() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.world != null && isPlayerInsideCasino(client);
    }

    public static int getSongIndex() {
        return songIndex;
    }

    public static int getVolume() {
        return djVolume;
    }

    public static void reset() {
        songIndex = -1;
        playSerial = 0;
        if (current != null) {
            MinecraftClient.getInstance().getSoundManager().stop(current);
            current = null;
        }
    }

    /** Llamado cada tick del cliente. */
    public static void tick(MinecraftClient client) {
        if (current != null && current.isDone()) {
            current = null;
        }
        if (client.player == null || client.world == null) {
            return;
        }
        boolean wanted = CasinoSongs.isValid(songIndex) && djVolume > 0 && isPlayerInsideCasino(client);
        if (wanted && current == null) {
            current = new CasinoSongInstance(songIndex, playSerial);
            client.getSoundManager().play(current);
        }
    }

    private static boolean isPlayerInsideCasino(MinecraftClient client) {
        return client.world.getRegistryKey() == World.OVERWORLD
                && DrafteaCasinoManager.isInsideCasinoClient(client.player.getBlockPos());
    }

    private static class CasinoSongInstance extends MovingSoundInstance {

        private final int mySong;
        private final int mySerial;

        CasinoSongInstance(int song, int serial) {
            super(CasinoSongs.SONGS.get(song).sound(), SoundCategory.RECORDS, SoundInstance.createRandom());
            this.mySong = song;
            this.mySerial = serial;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.relative = true;
            this.repeat = true; // Al terminar, la canción vuelve a empezar
            this.repeatDelay = 0;
            this.volume = 0.01F; // Empieza casi en silencio y sube suavemente
            this.pitch = 1.0F;
        }

        @Override
        public boolean shouldAlwaysPlay() {
            return true; // Que no se descarte por empezar con volumen muy bajo
        }

        @Override
        public void tick() {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean sameSong = mySong == songIndex && mySerial == playSerial;
            boolean inside = client.player != null && client.world != null && isPlayerInsideCasino(client);

            float target = (sameSong && inside) ? djVolume / 100.0F : 0.0F;
            if (this.volume < target) {
                this.volume = Math.min(target, this.volume + FADE_PER_TICK);
            } else if (this.volume > target) {
                // Cambio de canción: se corta más rápido para que entre la nueva
                float step = sameSong ? FADE_PER_TICK : FADE_PER_TICK * 3.0F;
                this.volume = Math.max(target, this.volume - step);
            }

            if (target == 0.0F && this.volume <= 0.0F) {
                this.setDone();
                return;
            }

            // Mientras suena la música del casino, silenciar la música normal de Minecraft
            if (inside && sameSong) {
                client.getMusicTracker().stop();
            }
        }
    }
}
