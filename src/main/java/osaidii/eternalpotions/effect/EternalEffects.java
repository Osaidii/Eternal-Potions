package osaidii.eternalpotions.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import osaidii.eternalpotions.EternalPotions;

/**
 * Custom Eternal effects \u2014 1.5x the strength of their vanilla counterparts.
 *
 *   Vanilla Speed      = +20% per level   ->  Eternal Speed      = +30% per level
 *   Vanilla Strength   = +3 dmg per level ->  Eternal Strength   = +4.5 dmg per level
 *   Vanilla Regen      = 1 HP / 50t/lvl   ->  Eternal Regen      = 1 HP / 33t/lvl
 *   Vanilla Resistance = -20% per level   ->  Eternal Resistance = -30% per level
 *     (Resistance is applied via EternalResistanceMixin, not an attribute.)
 */
public final class EternalEffects {

    public static final Holder<MobEffect> ETERNAL_SPEED = register("eternal_speed", new EternalSpeedEffect());
    public static final Holder<MobEffect> ETERNAL_REGENERATION = register("eternal_regeneration", new EternalRegenerationEffect());
    public static final Holder<MobEffect> ETERNAL_RESISTANCE = register("eternal_resistance", new EternalResistanceEffect());
    public static final Holder<MobEffect> ETERNAL_STRENGTH = register("eternal_strength", new EternalStrengthEffect());

    private EternalEffects() {}

    public static void initialize() {
    }

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        Identifier id = Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, name);
        Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect);
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    // -----------------------------------------------------------------
    //  Speed: +30% per level (vanilla is +20%)
    // -----------------------------------------------------------------
    public static class EternalSpeedEffect extends MobEffect {
        private static final Identifier MODIFIER_ID =
                Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "eternal_speed");

        public EternalSpeedEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x7CAFC6);
        }

        @Override
        public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
            AttributeInstance instance = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (instance == null) return;

            AttributeModifier existing = instance.getModifier(MODIFIER_ID);
            if (existing != null) {
                instance.removeModifier(existing);
            }

            AttributeModifier modifier = new AttributeModifier(
                    MODIFIER_ID,
                    0.30D * (amplifier + 1),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            instance.addTransientModifier(modifier);
        }

        @Override
        public void removeAttributeModifiers(AttributeMap attributes) {
            AttributeInstance instance = attributes.getInstance(Attributes.MOVEMENT_SPEED);
            if (instance == null) return;
            AttributeModifier existing = instance.getModifier(MODIFIER_ID);
            if (existing != null) {
                instance.removeModifier(existing);
            }
        }
    }

    // -----------------------------------------------------------------
    //  Strength: +4.5 damage per level (vanilla is +3)
    // -----------------------------------------------------------------
    public static class EternalStrengthEffect extends MobEffect {
        private static final Identifier MODIFIER_ID =
                Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "eternal_strength");

        public EternalStrengthEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xFFC700);
        }

        @Override
        public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
            AttributeInstance instance = attributes.getInstance(Attributes.ATTACK_DAMAGE);
            if (instance == null) return;

            AttributeModifier existing = instance.getModifier(MODIFIER_ID);
            if (existing != null) {
                instance.removeModifier(existing);
            }

            AttributeModifier modifier = new AttributeModifier(
                    MODIFIER_ID,
                    4.5D * (amplifier + 1),
                    AttributeModifier.Operation.ADD_VALUE
            );
            instance.addTransientModifier(modifier);
        }

        @Override
        public void removeAttributeModifiers(AttributeMap attributes) {
            AttributeInstance instance = attributes.getInstance(Attributes.ATTACK_DAMAGE);
            if (instance == null) return;
            AttributeModifier existing = instance.getModifier(MODIFIER_ID);
            if (existing != null) {
                instance.removeModifier(existing);
            }
        }
    }

    // -----------------------------------------------------------------
    //  Regeneration: heals 1 HP every (33 / level) ticks (vanilla is 50 / level).
    // -----------------------------------------------------------------
    public static class EternalRegenerationEffect extends MobEffect {
        public EternalRegenerationEffect() {
            super(MobEffectCategory.BENEFICIAL, 0xCD5CAB);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            int interval = Math.max(1, (int) (33.0 / (amplifier + 1)));
            if (entity.tickCount % interval == 0 && entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
            return true;
        }
    }

    // -----------------------------------------------------------------
    //  Resistance: no attribute. Damage reduction applied by EternalResistanceMixin.
    // -----------------------------------------------------------------
    public static class EternalResistanceEffect extends MobEffect {
        public EternalResistanceEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9146D5);
        }
    }
}