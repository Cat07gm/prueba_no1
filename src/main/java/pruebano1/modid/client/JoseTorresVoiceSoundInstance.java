package pruebano1.modid.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import pruebano1.modid.entity.JoseTorresEntity;

/**
 * Reproduce los audios de voz de los ataques especiales de José Torres / Jhoseph towers
 * ("Zombies" y "Dialecto") sin atenuación por distancia para que se escuchen siempre fuertes
 * y claros por encima de la música del boss.
 */
public class JoseTorresVoiceSoundInstance extends MovingSoundInstance {

    private final JoseTorresEntity boss;

    public JoseTorresVoiceSoundInstance(JoseTorresEntity boss, SoundEvent voiceSound) {
        super(voiceSound, SoundCategory.RECORDS, SoundInstance.createRandom());
        this.boss = boss;
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        this.relative = true;
        this.repeat = false;
        this.repeatDelay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
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
                || client.player == null
                || !client.player.isAlive()
                || this.boss.squaredDistanceTo(client.player) > 6400.0D) {
            this.setDone();
        }
    }
}
