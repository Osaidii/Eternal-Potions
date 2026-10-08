package osaidii.eternalpotions.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import osaidii.eternalpotions.item.ModItems;

public class EternalPotionTooltip implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.getItem() == ModItems.ETERNAL_POTION) {
                // Replace the first line (the item name) with a rainbow version
                if (!lines.isEmpty()) {
                    MutableComponent rainbow = rainbowText("The Eternal Potion");
                    lines.set(0, rainbow);
                }
            }
        });
    }

    private static MutableComponent rainbowText(String text) {
        MutableComponent result = Component.empty();
        int len = text.length();

        for (int i = 0; i < len; i++) {
            // Hue cycles across the string
            float hue = (float) i / len;
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);

            MutableComponent c = Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)));
            result.append(c);
        }

        return result;
    }
}