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
            MobEffects.STRENGTH,
            MobEffects.SPEED,
            MobEffects.RESISTANCE,
            MobEffects.REGENERATION
    );

    public EternalPotionItem(Properties properties) {
        super(properties);
    }

    private static boolean isKingSlotEmpty(Holder<MobEffect> effect) {
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer serverPlayer) {

            List<Holder<MobEffect>> eligible = new ArrayList<>();

            for (Holder<MobEffect> effect : BIG_FOUR) {
                MobEffectInstance existing = serverPlayer.getEffect(effect);

                if (existing == null) {
                    eligible.add(effect);
                } else if (existing.getAmplifier() == 0) {
                    eligible.add(effect);
                } else if (existing.getAmplifier() == 1) {
                    if (isKingSlotEmpty(effect)) {
                        eligible.add(effect);
                    }
                }
            }

            if (eligible.isEmpty()) {
                serverPlayer.sendSystemMessage(
                        Component.literal("No Eternal effects available to gain.")
                                .withStyle(ChatFormatting.RED)
                );
                return stack;
            }

            Holder<MobEffect> chosen = eligible.get(level.getRandom().nextInt(eligible.size()));

            MobEffectInstance existingChosen = serverPlayer.getEffect(chosen);
            int newAmplifier = existingChosen == null ? 0 : existingChosen.getAmplifier() + 1;

            serverPlayer.addEffect(new MobEffectInstance(chosen, MobEffectInstance.INFINITE_DURATION, newAmplifier));

            String levelText = newAmplifier == 0 ? "I" : (newAmplifier == 1 ? "II" : "III");

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