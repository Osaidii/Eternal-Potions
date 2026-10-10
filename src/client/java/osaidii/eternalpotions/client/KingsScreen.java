package osaidii.eternalpotions.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import osaidii.eternalpotions.network.KingsDataPayload;

import java.util.List;

public class KingsScreen extends Screen {

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 160;
    private static final int ROW_HEIGHT = 24;

    public KingsScreen() {
        super(Component.translatable("eternal-potions.kings.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Dim the world behind
        graphics.fill(0, 0, this.width, this.height, 0xC0000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Panel background
        graphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, 0xFF000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF1F1F1F);

        // Title: crown glyph + "Kings"
        int titleY = panelY + 12;
        graphics.text(this.font, "\u265B", panelX + 16, titleY, 0xFFFFD700);
        graphics.text(this.font,
                Component.translatable("eternal-potions.kings.title"),
                panelX + 36, titleY, 0xFFFFD700);

        // Divider
        graphics.fill(panelX + 10, panelY + 38, panelX + PANEL_WIDTH - 10, panelY + 39, 0xFF555555);

        // Four rows: effect name on the left, King name on the right
        List<KingsDataPayload.KingEntry> kings = EternalPotionsClient.getLastKings();
        int rowY = panelY + 50;

        for (int i = 0; i < 4; i++) {
            String effectText;
            String kingText;
            int kingColor;

            if (i < kings.size()) {
                KingsDataPayload.KingEntry entry = kings.get(i);
                effectText = Component.translatable(entry.effectId()).getString();
                if (entry.kingName() == null || entry.kingName().isEmpty()) {
                    kingText = Component.translatable("eternal-potions.kings.empty").getString();
                    kingColor = 0xFF888888;
                } else {
                    kingText = entry.kingName();
                    kingColor = 0xFFFFAA00;
                }
            } else {
                effectText = "?";
                kingText = Component.translatable("eternal-potions.kings.empty").getString();
                kingColor = 0xFF888888;
            }

            graphics.text(this.font, effectText, panelX + 16, rowY, 0xFFFFFFFF);
            graphics.text(this.font, kingText, panelX + 150, rowY, kingColor);

            rowY += ROW_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}