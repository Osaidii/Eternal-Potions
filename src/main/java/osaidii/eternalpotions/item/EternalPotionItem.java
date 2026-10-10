package osaidii.eternalpotions.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.EternalState;
import osaidii.eternalpotions.effect.EternalEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EternalPotionItem extends Item {

    private static final List<Holder<MobEffect>> BIG_FOUR = List.of(
            EternalEffects.ETERNAL_SPEED,
            EternalEffects.ETERNAL_REGENERATION,
            EternalEffects.ETERNAL_RESISTANCE,
            EternalEffects.ETERNAL_STRENGTH
    );

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

            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            EternalState state = EternalPotions.getState(serverLevel.getServer());

            List<Holder<MobEffect>> eligible = new ArrayList<>();

            for (Holder<MobEffect> effect : BIG_FOUR) {
                MobEffectInstance existing = serverPlayer.getEffect(effect);
                String effectId = effect.value().getDescriptionId();

                if (existing == null) {
                    eligible.add(effect);
                } else if (existing.getAmplifier() == 0) {
                    eligible.add(effect);
                } else if (existing.getAmplifier() == 1) {
                    if (state.isSlotEmpty(effectId)) {
                        eligible.add(effect);
                    }
                }
            }

            if (eligible.isEmpty()) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("eternal-potions.drink.no_effects_left")
                                .withStyle(ChatFormatting.RED)
                );
                return stack;
            }

            Holder<MobEffect> chosen = eligible.get(level.getRandom().nextInt(eligible.size()));

            MobEffectInstance existingChosen = serverPlayer.getEffect(chosen);
            int newAmplifier = existingChosen == null ? 0 : existingChosen.getAmplifier() + 1;

            serverPlayer.addEffect(EternalPotions.eternalInstance(chosen, newAmplifier));

            String levelText = newAmplifier == 0 ? "I" : (newAmplifier == 1 ? "II" : "III");
            Component prettyName = EternalPotions.effectName(chosen);

            if (newAmplifier == 2) {
                String effectId = chosen.value().getDescriptionId();
                state.crownKing(effectId, serverPlayer.getUUID(), serverPlayer.getName().getString());

                serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable("eternal-potions.king.crowned",
                                        serverPlayer.getName(), prettyName)
                                .withStyle(ChatFormatting.GOLD),
                        false
                );

                EternalPotions.checkThrones(serverLevel.getServer(), state);
            }

            serverPlayer.sendSystemMessage(
                    Component.translatable("eternal-potions.drink.you_got", prettyName, levelText)
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
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> textConsumer, TooltipFlag flag) {
        textConsumer.accept(Component.translatable("eternal-potions.tooltip.effect_unknown")
                .withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("eternal-potions.tooltip.reveal_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}