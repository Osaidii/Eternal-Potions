package osaidii.eternalpotions.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.effect.EternalEffects;

/**
 * Applies Eternal Resistance's damage reduction.
 *
 * Vanilla Resistance reduces incoming damage by 20% per level. Eternal Resistance
 * applies 30% per level (1.5x), applied on top of whatever protection the player
 * already had.
 */
@Mixin(LivingEntity.class)
public class EternalResistanceMixin {

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void eternalPotions$boostResistance(DamageSource source, float damage,
                                                CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance effect = self.getEffect(EternalEffects.ETERNAL_RESISTANCE);
        if (effect == null) return;

        float reduction = Math.min(0.80F, 0.30F * (effect.getAmplifier() + 1));
        cir.setReturnValue(cir.getReturnValue() * (1.0F - reduction));
    }
}