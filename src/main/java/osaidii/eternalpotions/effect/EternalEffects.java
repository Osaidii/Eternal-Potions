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

    public static class EternalSpeedEffect extends MobEffect {
        private static final Identifier MODIFIER_ID =
                Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "eternal_speed");

        private static final double[] SPEED_BY_LEVEL = { 0.15D, 0.25D, 0.40D };

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

            int index = Math.min(amplifier, SPEED_BY_LEVEL.length - 1);

            AttributeModifier modifier = new AttributeModifier(
                    MODIFIER_ID,
                    SPEED_BY_LEVEL[index],
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

    public static class EternalResistanceEffect extends MobEffect {
        public EternalResistanceEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x9146D5);
        }
    }
}