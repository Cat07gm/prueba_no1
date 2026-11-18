package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import pruebano1.modid.ModSounds;
import pruebano1.modid.entity.CharlieKirkEntity;

/**
 * Música de fondo para toda la pelea contra Charlie Kirk (incluyendo Fase 1 y Fase 2).
 * - Primera reproducción (loopFrom45s = false): reproduce la canción completa desde el segundo 0 hasta el final sin repetir.
 * - Repeticiones siguientes (loopFrom45s = true): reproduce en bucle infinito la pista recortada a partir del segundo 45.
 * - Se detiene automáticamente al iniciar su cinemática de muerte o si el jugador se aleja.
 */
public class CharlieKirkBossMusicInstance extends MovingSoundInstance {

    // Duración de charlie_kirk_boss_music.ogg = 224.5 s (~4490 ticks a 20 TPS; margen de seguridad 4500)
    private static final int INTRO_FULL_DURATION_TICKS = 4500;

    private final CharlieKirkEntity boss;
    private final boolean loopFrom45s;
    private int ticksPlayed = 0;
    private boolean startedPlaying = false;

    public CharlieKirkBossMusicInstance(CharlieKirkEntity boss, boolean loopFrom45s) {
        super(
                loopFrom45s ? ModSounds.CHARLIE_KIRK_BOSS_MUSIC_LOOP : ModSounds.CHARLIE_KIRK_BOSS_MUSIC,
                SoundCategory.RECORDS,
                SoundInstance.createRandom()
        );
        this.boss = boss;
        this.loopFrom45s = loopFrom45s;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.repeat = loopFrom45s;
        this.repeatDelay = 0;
        this.volume = 0.88F;
        this.pitch = 1.0F;
    }

    public boolean isLoopTrack() {
        return this.loopFrom45s;
    }

    public int getTicksPlayed() {
        return this.ticksPlayed;
    }

    public boolean hasStartedPlaying() {
        return this.startedPlaying;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.boss == null
                || !this.boss.isAlive()
                || this.boss.isRemoved()
                || !this.boss.shouldPlayBossMusic()
                || client.player == null
                || !client.player.isAlive()
                || this.boss.squaredDistanceTo(client.player) > 90000.0D) {
            this.setDone();
            return;
        }

        if (client.getSoundManager().isPlaying(this)) {
            this.startedPlaying = true;
        }

        this.ticksPlayed++;
        if (!this.loopFrom45s && this.ticksPlayed >= INTRO_FULL_DURATION_TICKS) {
            this.setDone();
            return;
        }

        client.getMusicTracker().stop();
    }
}
