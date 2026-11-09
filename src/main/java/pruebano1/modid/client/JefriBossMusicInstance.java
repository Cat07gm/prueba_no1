package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import pruebano1.modid.ModSounds;
import pruebano1.modid.entity.JefriPensteinEntity;

/**
 * Soundtrack de fondo para el Jefe Final Jefri penstein.
 * - Suena desde que inicia la cinemática de invocación hasta que logran derrotar a Jefri.
 * - Se detiene inmediatamente en cuanto inicia la última cinemática de muerte (jaula).
 */
public class JefriBossMusicInstance extends MovingSoundInstance {

    private final JefriPensteinEntity boss;

    public JefriBossMusicInstance(JefriPensteinEntity boss) {
        super(ModSounds.JEFRI_BOSS_MUSIC, SoundCategory.RECORDS, SoundInstance.createRandom());
        this.boss = boss;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.90F;
        this.pitch = 1.0F;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        // Detener música inmediatamente si empieza la cinemática final de muerte (jaula) o si muere
        if (this.boss == null
                || !this.boss.isAlive()
                || this.boss.isRemoved()
                || !this.boss.shouldPlayBossMusic()
                || client.player == null
                || !client.player.isAlive()
                || this.boss.squaredDistanceTo(client.player) > 6400.0D) {
            this.setDone();
            return;
        }

        client.getMusicTracker().stop();
    }
}
