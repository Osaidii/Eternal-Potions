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
 * applies 30% per level (1.5x). Total reduction from this hook (vanilla + Eternal)
 * is capped at 80% so a stacked player cannot become invincible.
 */
@Mixin(LivingEntity.class)
public class EternalResistanceMixin {

    private static final float MAX_TOTAL_REDUCTION = 0.80F;

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void eternalPotions$boostResistance(DamageSource source, float damage,
                                                CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        MobEffectInstance effect = self.getEffect(EternalEffects.ETERNAL_RESISTANCE);
        if (effect == null) return;
        if (damage <= 0.0F) return;

        float vanillaResult = cir.getReturnValue();
        float vanillaReduction = 1.0F - (vanillaResult / damage);

        float eternalReduction = 0.30F * (effect.getAmplifier() + 1);
        float totalReduction = 1.0F - (1.0F - vanillaReduction) * (1.0F - eternalReduction);

        if (totalReduction > MAX_TOTAL_REDUCTION) {
            totalReduction = MAX_TOTAL_REDUCTION;
        }

        cir.setReturnValue(damage * (1.0F - totalReduction));
    }
}