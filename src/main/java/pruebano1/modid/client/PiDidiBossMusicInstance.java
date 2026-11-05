package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import pruebano1.modid.ModSounds;
import pruebano1.modid.entity.PiDidiEntity;

/**
 * Soundtrack de fondo para la batalla contra el mini-jefe Pi Didi.
 * - Se reproduce en estéreo como música de fondo (AttenuationType.NONE) mientras Pi Didi esté en combate.
 * - Se repite en bucle si la pelea dura más que la canción.
 * - Se detiene automáticamente en cuanto Pi Didi muere, deja de estar en combate o el jugador se aleja.
 */
public class PiDidiBossMusicInstance extends MovingSoundInstance {

    private final PiDidiEntity boss;

    public PiDidiBossMusicInstance(PiDidiEntity boss) {
        super(ModSounds.PI_DIDI_BOSS_MUSIC, SoundCategory.RECORDS, SoundInstance.createRandom());
        this.boss = boss;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.85F;
        this.pitch = 1.0F;
    }

    @Override
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.boss == null
                || !this.boss.isAlive()
                || this.boss.isRemoved()
                || !this.boss.isInCombat()
                || client.player == null
                || !client.player.isAlive()
                || this.boss.squaredDistanceTo(client.player) > 2304.0D) {
            this.setDone();
            return;
        }

        // Silenciar la música ambiental normal de Minecraft mientras suene el tema de Pi Didi
        client.getMusicTracker().stop();
    }

    public boolean isForBoss(PiDidiEntity entity) {
        return !this.isDone() && this.boss == entity;
    }
}
