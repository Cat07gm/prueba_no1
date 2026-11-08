package pruebano1.modid;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {

    public static final SimpleParticleType HELLO_KITTY = Registry.register(
            Registries.PARTICLE_TYPE,
            Identifier.of(Prueba_no1.MOD_ID, "hello_kitty"),
            FabricParticleTypes.simple(true)
    );

    public static void registerModParticles() {
        Prueba_no1.LOGGER.info("Registrando partículas personalizadas para " + Prueba_no1.MOD_ID);
    }
}
