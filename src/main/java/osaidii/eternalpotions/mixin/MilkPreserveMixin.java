package osaidii.eternalpotions.mixin;

import net.minecraft.core.Holder;
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
 * Preserves Eternal effects when milk is drunk.
 *
 * Milk has no dedicated class in 26.3, so this hooks LivingEntity.removeAllEffects
 * and restores Eternal effects UNLESS the call comes from a server command
 * (i.e. /effect clear, which should free the throne as designed).
 */
@Mixin(LivingEntity.class)
public class MilkPreserveMixin {

    private static final List<Holder<MobEffect>> ETERNAL_EFFECTS = List.of(
            EternalEffects.ETERNAL_SPEED,
            EternalEffects.ETERNAL_REGENERATION,
            EternalEffects.ETERNAL_RESISTANCE,
            EternalEffects.ETERNAL_STRENGTH
    );

    @Inject(method = "removeAllEffects", at = @At("HEAD"))
    private void eternalPotions$preserveEternalEffects(CallbackInfoReturnable<Boolean> cir) {
        if (eternalPotions$isCommandContext()) return;

        LivingEntity self = (LivingEntity)(Object) this;
        if (self.level().isClientSide()) return;

        List<MobEffectInstance> saved = new ArrayList<>(4);
        for (Holder<MobEffect> effect : ETERNAL_EFFECTS) {
            MobEffectInstance inst = self.getEffect(effect);
            if (inst != null) {
                saved.add(new MobEffectInstance(inst));
            }
        }
        if (saved.isEmpty()) return;

        // Re-apply on the next server tick so we overwrite the removal.
        var server = self.level().getServer();
        if (server == null) return;

        server.execute(() -> {
            for (MobEffectInstance inst : saved) {
                self.addEffect(inst);
            }
        });
    }

    @Unique
    private static boolean eternalPotions$isCommandContext() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String cn = e.getClassName();
            if (cn.startsWith("net.minecraft.commands.")) return true;
        }
        return false;
    }
}