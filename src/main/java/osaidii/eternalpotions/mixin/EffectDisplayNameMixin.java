package osaidii.eternalpotions.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.EternalPotions;

@Mixin(MobEffectInstance.class)
public class EffectDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$alwaysShowRomanNumeral(CallbackInfoReturnable<Component> cir) {
        MobEffectInstance self = (MobEffectInstance) (Object) this;
        Holder<MobEffect> effect = self.getEffect();

        String descriptionId = effect.value().getDescriptionId();
        if (!descriptionId.startsWith("effect." + EternalPotions.MOD_ID + ".")) {
            return;
        }

        int level = self.getAmplifier() + 1;
        MutableComponent result = Component.translatable(descriptionId)
                .append(Component.literal(" " + roman(level)));

        cir.setReturnValue(result);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(n);
        };
    }
}