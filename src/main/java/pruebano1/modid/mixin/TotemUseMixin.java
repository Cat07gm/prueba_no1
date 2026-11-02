package pruebano1.modid.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pruebano1.modid.ModItem;

@Mixin(LivingEntity.class)
public abstract class TotemUseMixin extends Entity {

    @Shadow
    public abstract ItemStack getStackInHand(Hand hand);

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    public abstract void setAbsorptionAmount(float absorptionAmount);

    @Unique
    private ItemStack pruebaNo1$consumedTotem = ItemStack.EMPTY;

    public TotemUseMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * Antes de que Minecraft consuma el tótem de la mano, guardamos una copia
     * para saber exactamente qué tótem (tradicional, Nazitotem, Isratotem o Lontotem) tenía el jugador.
     */
    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"))
    private void captureTotemBeforeUse(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        this.pruebaNo1$consumedTotem = ItemStack.EMPTY;
        for (Hand hand : Hand.values()) {
            ItemStack stack = this.getStackInHand(hand);
            if (stack.contains(DataComponentTypes.DEATH_PROTECTION)) {
                this.pruebaNo1$consumedTotem = stack.copy();
                break;
            }
        }
    }

    /**
     * Justo después de que el tótem salva al jugador:
     * 1. Ajustamos los corazones exactos de absorción para Isratotem (5 corazones) y Lontotem (8 corazones + doble vida).
     * 2. Mostramos en el chat para todos los jugadores quién consumió qué tótem.
     * 3. Reproducimos para todos los jugadores el sonido de explosión real con altos fuertes y bajos profundos.
     */
    @Inject(method = "tryUseDeathProtector", at = @At("RETURN"))
    private void onTotemConsumed(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || this.pruebaNo1$consumedTotem.isEmpty()) {
            return;
        }

        ItemStack totemStack = this.pruebaNo1$consumedTotem;
        this.pruebaNo1$consumedTotem = ItemStack.EMPTY;

        // Ajustar corazones exactos de absorción según el tótem personalizado
        if (totemStack.isOf(ModItem.ISRATOTEM)) {
            // 5 corazones de absorción exactos (cada corazón son 2.0F puntos -> 10.0F)
            this.setAbsorptionAmount(10.0F);
        } else if (totemStack.isOf(ModItem.LONTOTEM)) {
            // El doble de vida base que el tótem tradicional (2.0F) y 8 corazones de absorción (16.0F)
            this.setHealth(2.0F);
            this.setAbsorptionAmount(16.0F);
        }

        // Anuncio en el chat y sonido global de explosión en el servidor
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            MinecraftServer server = serverWorld.getServer();
            String playerName = this.getDisplayName().getString();
            String totemName = totemStack.getName().getString();

            String description;
            Formatting color;
            if (totemStack.isOf(ModItem.NAZITOTEM)) {
                description = "¡" + playerName + " ha consumido un " + totemName + " y desató una furia explosiva!";
                color = Formatting.RED;
            } else if (totemStack.isOf(ModItem.ISRATOTEM)) {
                description = "¡" + playerName + " ha consumido un " + totemName + " y comenzó a levitar con escudo divino!";
                color = Formatting.AQUA;
            } else if (totemStack.isOf(ModItem.LONTOTEM)) {
                description = "¡" + playerName + " ha consumido un " + totemName + " obteniendo el doble de poder inmortal!";
                color = Formatting.LIGHT_PURPLE;
            } else {
                description = "¡" + playerName + " ha consumido un " + totemName + " y engañó a la muerte!";
                color = Formatting.GOLD;
            }

            Text chatMessage = Text.literal("💥 [TÓTEM] ").formatted(Formatting.DARK_RED, Formatting.BOLD)
                    .append(Text.literal(description).formatted(color));

            // Enviar mensaje a todos los jugadores en el chat
            server.getPlayerManager().broadcast(chatMessage, false);

            // Reproducir sonido de impacto explosivo (altos fuertes y bajos al comenzar y al terminar) para TODOS los jugadores
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();

                // 1. Al comenzar: Impacto seco con agudos fuertes (chasquido explosivo) y golpe grave profundo
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, SoundCategory.PLAYERS, 1.5F, 0.85F);
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1.3F, 0.70F);
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2F, 0.75F);

                // 2. Cuerpo y final: Explosión real con retumbo de bajos profundos y agudos al terminar
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 1.4F, 0.60F);
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.4F, 0.65F);
                player.getWorld().playSound(null, px, py, pz, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 1.2F, 0.75F);
            }
        }
    }
}
