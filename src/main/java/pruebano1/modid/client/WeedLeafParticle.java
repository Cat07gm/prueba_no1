package pruebano1.modid.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

/**
 * Partícula personalizada con la hoja verde pixel-art que libera Jefri penstein
 * mientras levita e invoca niños a su alrededor.
 */
public class WeedLeafParticle extends SpriteBillboardParticle {

    protected WeedLeafParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);
        this.velocityMultiplier = 0.92F;
        this.gravityStrength = 0.02F;
        this.velocityX = velocityX;
        this.velocityY = velocityY + 0.03D;
        this.velocityZ = velocityZ;
        this.scale = 0.38F + (this.random.nextFloat() * 0.22F);
        this.maxAge = 35 + this.random.nextInt(20);
        this.setSpriteForAge(spriteProvider);
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
        public Particle createParticle(SimpleParticleType type, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new WeedLeafParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
