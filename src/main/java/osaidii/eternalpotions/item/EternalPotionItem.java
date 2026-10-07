package osaidii.eternalpotions.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EternalPotionItem extends Item {

    private static final List<Holder<MobEffect>> BIG_FOUR = List.of(
            MobEffects.SPEED,
            MobEffects.REGENERATION,
            MobEffects.RESISTANCE,
            MobEffects.STRENGTH
    );

    // Tweak these to change the feel of the randomness
    private static final int WEIGHT_NEW = 3;      // effect you don't have yet -> level I
    private static final int WEIGHT_UPGRADE = 1;  // level I -> level II
    private static final int WEIGHT_MAXED = 1;    // already II -> wasted roll (set to 0 to disable)

    public EternalPotionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer serverPlayer) {

            // Build a weighted pool
            List<Holder<MobEffect>> pool = new ArrayList<>();

            for (Holder<MobEffect> effect : BIG_FOUR) {
                MobEffectInstance existing = serverPlayer.getEffect(effect);

                int weight;
                if (existing == null) {
                    weight = WEIGHT_NEW;
                } else if (existing.getAmplifier() == 0) {
                    weight = WEIGHT_UPGRADE;
                } else {
                    weight = WEIGHT_MAXED;
                }

                for (int i = 0; i < weight; i++) {
                    pool.add(effect);
                }
            }

            if (pool.isEmpty()) {
                serverPlayer.sendSystemMessage(
                        Component.literal("You already have every Eternal effect at max level.")
                                .withStyle(ChatFormatting.RED)
                );
                return stack;
            }

            Holder<MobEffect> chosen = pool.get(serverPlayer.getRandom().nextInt(pool.size()));
            MobEffectInstance existingChosen = serverPlayer.getEffect(chosen);

            // Rolled an effect that's already maxed -> wasted potion
            if (existingChosen != null && existingChosen.getAmplifier() >= 1) {
                serverPlayer.sendSystemMessage(
                        Component.literal("The potion fizzled: ")
                                .append(Component.translatable(chosen.value().getDescriptionId()))
                                .append(Component.literal(" is already maxed."))
                                .withStyle(ChatFormatting.GRAY)
                );
                if (!serverPlayer.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                return stack;
            }

            int newAmplifier = existingChosen == null ? 0 : 1;

            serverPlayer.addEffect(new MobEffectInstance(chosen, MobEffectInstance.INFINITE_DURATION, newAmplifier));

            String levelText = newAmplifier == 0 ? "I" : "II";

            serverPlayer.sendSystemMessage(
                    Component.literal("You got: ")
                            .append(Component.translatable(chosen.value().getDescriptionId()))
                            .append(Component.literal(" " + levelText))
                            .withStyle(ChatFormatting.GOLD)
            );

            if (!serverPlayer.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }

        return stack;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> textConsumer, TooltipFlag flag) {
        textConsumer.accept(Component.literal("Effect: ???").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.literal("Right-click to reveal").withStyle(ChatFormatting.DARK_GRAY));
    }
}