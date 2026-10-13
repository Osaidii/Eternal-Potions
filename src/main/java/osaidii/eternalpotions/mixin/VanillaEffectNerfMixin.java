package osaidii.eternalpotions.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.EternalPotions;

@Mixin(MobEffect.class)
public class VanillaEffectNerfMixin {

    private static final Identifier NERFED_SPEED_ID =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "nerfed_vanilla_speed");
    private static final Identifier NERFED_STRENGTH_ID =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "nerfed_vanilla_strength");

    @Inject(method = "addAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$nerfVanillaAdd(AttributeMap attributes, int amplifier, CallbackInfo ci) {
        MobEffect self = (MobEffect)(Object) this;

        if (self == MobEffects.SPEED.value()) {
            AttributeInstance inst = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (inst != null) {
                AttributeModifier existing = inst.getModifier(NERFED_SPEED_ID);
                if (existing != null) inst.removeModifier(existing);
                inst.addTransientModifier(new AttributeModifier(
                        NERFED_SPEED_ID,
                        0.10D * (amplifier + 1),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            ci.cancel();
        } else if (self == MobEffects.STRENGTH.value()) {
            AttributeInstance inst = attributes.getInstance(Attributes.ATTACK_DAMAGE);
            if (inst != null) {
                AttributeModifier existing = inst.getModifier(NERFED_STRENGTH_ID);
                if (existing != null) inst.removeModifier(existing);
                inst.addTransientModifier(new AttributeModifier(
                        NERFED_STRENGTH_ID,
                        1.5D * (amplifier + 1),
                        AttributeModifier.Operation.ADD_VALUE));
            }
            ci.cancel();
        }
    }

    @Inject(method = "removeAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$nerfVanillaRemove(AttributeMap attributes, CallbackInfo ci) {
        MobEffect self = (MobEffect)(Object) this;

        if (self == MobEffects.SPEED.value()) {
            AttributeInstance inst = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (inst != null) {
                AttributeModifier existing = inst.getModifier(NERFED_SPEED_ID);
                if (existing != null) inst.removeModifier(existing);
            }
            ci.cancel();
        } else if (self == MobEffects.STRENGTH.value()) {
            AttributeInstance inst = attributes.getInstance(Attributes.ATTACK_DAMAGE);
            if (inst != null) {
                AttributeModifier existing = inst.getModifier(NERFED_STRENGTH_ID);
                if (existing != null) inst.removeModifier(existing);
            }
            ci.cancel();
        }
    }

    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$nerfVanillaRegen(ServerLevel level, LivingEntity entity, int amplifier,
                                                 CallbackInfoReturnable<Boolean> cir) {
        MobEffect self = (MobEffect)(Object) this;
        if (self != MobEffects.REGENERATION.value()) return;

        int interval = 100 >> amplifier;
        if (interval > 0 && entity.tickCount % interval == 0 && entity.getHealth() < entity.getMaxHealth()) {
            entity.heal(1.0F);
        }
        cir.setReturnValue(true);
    }
}