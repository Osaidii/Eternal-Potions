package osaidii.eternalpotions.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.network.KingsDataPayload;

import java.util.List;

public class KingsScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 130;
    private static final int ROW_HEIGHT = 22;
    private static final int SLOT_SIZE = 18;

    private static final int GUI_BG = 0xFFC6C6C6;
    private static final int GUI_HIGHLIGHT = 0xFFFFFFFF;
    private static final int GUI_SHADOW = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int TITLE_TEXT = 0xFF404040;
    private static final int BODY_TEXT = 0xFF202020;
    private static final int KING_TEXT = 0xFF804000;

    public KingsScreen() {
        super(Component.translatable("eternal-potions.kings.title"));
        EternalPotions.LOGGER.info("[EternalPotion] KingsScreen constructor");
    }

    @Override
    protected void init() {
        super.init();
        EternalPotions.LOGGER.info("[EternalPotion] KingsScreen init: width={}, height={}",
                this.width, this.height);
    }

    @Override
    public void removed() {
        EternalPotions.LOGGER.info("[EternalPotion] KingsScreen removed");
        super.removed();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        EternalPotions.LOGGER.info("[EternalPotion] KingsScreen extractRenderState: width={}, height={}",
                this.width, this.height);
        try {
            if (this.width <= 0 || this.height <= 0) {
                EternalPotions.LOGGER.warn("[EternalPotion] invalid screen size, skipping draw");
                return;
            }

            graphics.fill(0, 0, this.width, this.height, 0xC0000000);

            int panelX = (this.width - PANEL_WIDTH) / 2;
            int panelY = (this.height - PANEL_HEIGHT) / 2;

            drawVanillaPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

            int titleY = panelY + 6;
            graphics.text(this.font, "\u265B", panelX + 8, titleY, KING_TEXT);
            graphics.text(this.font,
                    Component.translatable("eternal-potions.kings.title"),
                    panelX + 22, titleY, TITLE_TEXT);

            graphics.fill(panelX + 4, panelY + 20, panelX + PANEL_WIDTH - 4, panelY + 21, GUI_SHADOW);
            graphics.fill(panelX + 4, panelY + 21, panelX + PANEL_WIDTH - 4, panelY + 22, GUI_HIGHLIGHT);

            List<KingsDataPayload.KingEntry> kings = EternalPotionsClient.getLastKings();
            int rowY = panelY + 30;

            for (int i = 0; i < 4; i++) {
                int slotX = panelX + 8;
                drawSlot(graphics, slotX, rowY);

                String effectText;
                String kingText;
                int kingColor;

                if (i < kings.size()) {
                    KingsDataPayload.KingEntry entry = kings.get(i);
                    effectText = Component.translatable(entry.effectId()).getString();
                    if (entry.kingName() == null || entry.kingName().isEmpty()) {
                        kingText = Component.translatable("eternal-potions.kings.empty").getString();
                        kingColor = 0xFF707070;
                    } else {
                        kingText = entry.kingName();
                        kingColor = KING_TEXT;
                    }
                } else {
                    effectText = "?";
                    kingText = Component.translatable("eternal-potions.kings.empty").getString();
                    kingColor = 0xFF707070;
                }

                int textY = rowY + 5;
                graphics.text(this.font, effectText, slotX + SLOT_SIZE + 6, textY, BODY_TEXT);
                graphics.text(this.font, kingText, panelX + 130, textY, kingColor);

                rowY += ROW_HEIGHT;
            }
        } catch (Throwable t) {
            EternalPotions.LOGGER.error("[EternalPotion] KingsScreen render crashed", t);
        }
    }

    private void drawVanillaPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, GUI_BG);
        g.fill(x, y, x + w - 1, y + 1, GUI_HIGHLIGHT);
        g.fill(x, y, x + 1, y + h - 1, GUI_HIGHLIGHT);
        g.fill(x + w - 1, y, x + w, y + h, GUI_SHADOW);
        g.fill(x, y + h - 1, x + w, y + h, GUI_SHADOW);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG);
        g.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT_SIZE, SLOT_DARK);
        g.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, GUI_HIGHLIGHT);
        g.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, GUI_HIGHLIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}