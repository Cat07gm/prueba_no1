package pruebano1.modid.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * Partícula personalizada que muestra aleatoriamente una de las 19 armas de fuego
 * soltadas por Charlie Kirk durante su aparición y mientras levita en la Fase 1.
 */
public class CharlieWeaponParticle extends SpriteBillboardParticle {

    protected CharlieWeaponParticle(
            ClientWorld world,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityMultiplier = 0.92F;
        this.gravityStrength = -0.01F;
        this.velocityX = velocityX;
        this.velocityY = velocityY + 0.025D;
        this.velocityZ = velocityZ;
        this.scale = 0.48F + (this.random.nextFloat() * 0.22F);
        this.maxAge = 40 + this.random.nextInt(25);
        // Cada partícula elige una de las 19 texturas de armas y la conserva fija durante su vida
        this.setSprite(spriteProvider.getSprite(this.random));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.maxAge - 10) {
            this.alpha = Math.max(0.0F, (this.maxAge - this.age) / 10.0F);
        }
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientWorld world,
                double x, double y, double z,
                double velocityX, double velocityY, double velocityZ
        ) {
            return new CharlieWeaponParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
