package pruebano1.modid.client;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * Partícula personalizada de Hello Kitty que sale disparada cuando Valentina Prieta explota al morir.
 */
public class HelloKittyParticle extends SpriteBillboardParticle {

    protected HelloKittyParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityMultiplier = 0.90F;
        this.gravityStrength = 0.04F;
        this.velocityX = velocityX;
        this.velocityY = velocityY + 0.05D;
        this.velocityZ = velocityZ;
        this.scale = 0.40F + (this.random.nextFloat() * 0.20F);
        this.maxAge = 35 + this.random.nextInt(20);
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getBrightness(float tint) {
        // Que las caritas de Hello Kitty brillen con luz máxima en la explosión
        return 15728880;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new HelloKittyParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
