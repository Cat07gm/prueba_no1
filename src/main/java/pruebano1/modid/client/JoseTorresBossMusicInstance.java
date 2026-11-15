package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import pruebano1.modid.ModSounds;
import pruebano1.modid.entity.JoseTorresEntity;

/**
 * Música de fondo para el Boss José Torres (Fase 1) y Jhoseph towers (Fase 2).
 * - Fase 1: suena desde la cinemática de entrada de 5s hasta que baja al 50% de vida.
 * - Fase 2: al transformarse en Jhoseph towers (blanco), suena la segunda canción recortada desde el segundo 9.
 * - Mientras José Torres realiza un ataque especial con voz ("Zombies" o "Dialecto"), atenúa temporalmente
 *   el volumen de la música de fondo para que el audio del ataque especial se escuche con total claridad.
 */
public class JoseTorresBossMusicInstance extends MovingSoundInstance {

    private static final float NORMAL_VOLUME = 0.85F;
    private static final float DUCKED_VOLUME = 0.16F;

    private final JoseTorresEntity boss;
    private final boolean phase2Track;

    public JoseTorresBossMusicInstance(JoseTorresEntity boss, boolean phase2Track) {
        super(
                phase2Track ? ModSounds.JHOSEPH_TOWERS_BOSS_MUSIC : ModSounds.JOSE_TORRES_BOSS_MUSIC,
                SoundCategory.RECORDS,
                SoundInstance.createRandom()
        );
        this.boss = boss;
        this.phase2Track = phase2Track;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = NORMAL_VOLUME;
        this.pitch = 1.0F;
    }

    public boolean isPhase2Track() {
        return this.phase2Track;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldKeepPlaying = this.boss != null
                && this.boss.isAlive()
                && !this.boss.isRemoved()
                && (this.phase2Track ? this.boss.shouldPlayPhase2Music() : this.boss.shouldPlayPhase1Music());

        if (!shouldKeepPlaying
                || client.player == null
                || !client.player.isAlive()
                || this.boss.squaredDistanceTo(client.player) > 6400.0D) {
            this.setDone();
            return;
        }

        // Bajar el volumen de la música de fondo mientras suena el audio de "Zombies" o "Dialecto"
        float targetVolume = (this.boss.getActiveSpecialType() != JoseTorresEntity.SPECIAL_NONE)
                ? DUCKED_VOLUME
                : NORMAL_VOLUME;
        if (this.volume > targetVolume) {
            this.volume = Math.max(targetVolume, this.volume - 0.12F);
        } else if (this.volume < targetVolume) {
            this.volume = Math.min(targetVolume, this.volume + 0.05F);
        }

        client.getMusicTracker().stop();
    }
}
