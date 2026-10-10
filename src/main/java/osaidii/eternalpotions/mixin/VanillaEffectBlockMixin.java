package osaidii.eternalpotions.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.effect.EternalEffects;

import java.util.Map;

/**
 * Refuses to apply a vanilla effect if the entity already has the
 * corresponding Eternal effect, and tells the player why.
 */
@Mixin(LivingEntity.class)
public class VanillaEffectBlockMixin {

    private static final Map<Holder<MobEffect>, Holder<MobEffect>> BLOCK_MAP = Map.of(
            MobEffects.SPEED, EternalEffects.ETERNAL_SPEED,
            MobEffects.REGENERATION, EternalEffects.ETERNAL_REGENERATION,
            MobEffects.RESISTANCE, EternalEffects.ETERNAL_RESISTANCE,
            MobEffects.STRENGTH, EternalEffects.ETERNAL_STRENGTH
    );

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z",
            at = @At("HEAD"), cancellable = true)
    private void eternalPotions$blockVanillaAdd(MobEffectInstance incoming,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (shouldBlock(incoming)) cir.setReturnValue(false);
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$blockVanillaForce(MobEffectInstance incoming,
                                                  Entity source,
                                                  CallbackInfo ci) {
        if (shouldBlock(incoming)) ci.cancel();
    }

    private boolean shouldBlock(MobEffectInstance incoming) {
        if (incoming == null) return false;

        Holder<MobEffect> eternal = BLOCK_MAP.get(incoming.getEffect());
        if (eternal == null) return false;

        LivingEntity self = (LivingEntity)(Object) this;
        if (self.getEffect(eternal) == null) return false;

        if (self instanceof ServerPlayer player) {
            player.sendSystemMessage(
                    Component.translatable(
                            "eternal-potions.effect.vanilla_blocked",
                            Component.translatable(incoming.getEffect().value().getDescriptionId()),
                            Component.translatable(eternal.value().getDescriptionId())
                    )
            );
        }
        return true;
    }
}