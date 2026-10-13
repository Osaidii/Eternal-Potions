package osaidii.eternalpotions.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.effect.EternalEffects;

@Mixin(LivingEntity.class)
public class EternalResistanceMixin {

    private static final float MAX_TOTAL_REDUCTION = 0.80F;

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void eternalPotions$handleResistance(DamageSource source, float damage,
                                                 CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity)(Object) this;
        if (damage <= 0.0F) return;

        MobEffectInstance vanilla = self.getEffect(MobEffects.RESISTANCE);
        MobEffectInstance eternal = self.getEffect(EternalEffects.ETERNAL_RESISTANCE);

        if (vanilla == null && eternal == null) return;

        float vanillaResult = cir.getReturnValue();
        float vanillaReduction = 1.0F - (vanillaResult / damage);

        float totalReduction;

        if (eternal != null) {
            // Eternal Resistance: 20% per level
            float eternalReduction = 0.20F * (eternal.getAmplifier() + 1);
            // Vanilla portion is halved (in case both are somehow present)
            totalReduction = 1.0F - (1.0F - vanillaReduction * 0.5F) * (1.0F - eternalReduction);
        } else {
            // Vanilla only — halve what vanilla already gave
            totalReduction = vanillaReduction * 0.5F;
        }

        if (totalReduction > MAX_TOTAL_REDUCTION) {
            totalReduction = MAX_TOTAL_REDUCTION;
        }

        cir.setReturnValue(damage * (1.0F - totalReduction));
    }
}