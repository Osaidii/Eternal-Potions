package osaidii.eternalpotions.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.effect.EternalEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Preserves Eternal effects across a Totem of Undying save.
 *
 * Vanilla's totem calls removeAllEffects() before applying its own buffs,
 * which strips our effects and (for Kings) triggers the ghost-king cleanup
 * on the next tick, freeing the throne. This mixin snapshots our effects
 * just before the totem logic runs and re-applies them if the totem fired.
 */
@Mixin(LivingEntity.class)
public class TotemSaveMixin {

    private static final List<Holder<MobEffect>> ETERNAL_EFFECTS = List.of(
            EternalEffects.ETERNAL_SPEED,
            EternalEffects.ETERNAL_REGENERATION,
            EternalEffects.ETERNAL_RESISTANCE,
            EternalEffects.ETERNAL_STRENGTH
    );

    @Unique
    private List<MobEffectInstance> eternalPotions$savedEternalEffects = null;

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"))
    private void eternalPotions$snapshotEternalEffects(DamageSource source,
                                                       CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object) this;
        List<MobEffectInstance> saved = new ArrayList<>(4);

        for (Holder<MobEffect> effect : ETERNAL_EFFECTS) {
            MobEffectInstance inst = self.getEffect(effect);
            if (inst != null) {
                saved.add(new MobEffectInstance(inst));
            }
        }

        this.eternalPotions$savedEternalEffects = saved;
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void eternalPotions$restoreEternalEffects(DamageSource source,
                                                      CallbackInfoReturnable<Boolean> cir) {
        List<MobEffectInstance> saved = this.eternalPotions$savedEternalEffects;
        this.eternalPotions$savedEternalEffects = null;

        if (saved == null || saved.isEmpty()) return;
        if (!cir.getReturnValue()) return; // no totem consumed, nothing to restore

        LivingEntity self = (LivingEntity)(Object) this;
        for (MobEffectInstance inst : saved) {
            self.addEffect(inst);
        }
    }
}