package pruebano1.modid.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * Partícula personalizada de "Virus Encapsulado" (cápsula cilíndrica con doble hélice azul)
 * que libera José Torres al invocar oleadas de entre 4 y 8 zombies.
 */
public class EncapsulatedVirusParticle extends SpriteBillboardParticle {

    protected EncapsulatedVirusParticle(
            ClientWorld world,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            SpriteProvider spriteProvider
    ) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityMultiplier = 0.93F;
        this.gravityStrength = -0.015F; // Flota suavemente hacia arriba con brillo biológico
        this.velocityX = velocityX * 0.35D;
        this.velocityY = velocityY * 0.35D + 0.035D;
        this.velocityZ = velocityZ * 0.35D;
        this.scale = 0.42F + (this.random.nextFloat() * 0.24F);
        this.maxAge = 38 + this.random.nextInt(22);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        return 15728880; // Brillo máximo para que resalte la doble hélice azul
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
            return new EncapsulatedVirusParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
