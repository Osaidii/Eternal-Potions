package osaidii.eternalpotions.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.network.KingsDataPayload;

import java.util.List;

public class KingsScreen extends Screen {

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 158;
    private static final int ROW_HEIGHT = 22;
    private static final int SLOT_SIZE = 18;

    private static final int SLOT_X = 10;
    private static final int EFFECT_TEXT_X = 34;
    private static final int KING_TEXT_X = 172;
    private static final int KING_TEXT_RIGHT_PAD = 12;

    private static final int BG_FILL = 0xFFC6C6C6;
    private static final int BG_LIGHT = 0xFFFFFFFF;
    private static final int BG_DARK = 0xFF555555;
    private static final int BG_OUTLINE = 0xFF000000;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;

    private static final int TEXT_TITLE = 0xFF404040;
    private static final int TEXT_EMPTY = 0xFF7A7A7A;
    private static final int TEXT_KING = 0xFF202020;
    private static final int TEXT_FOOTER = 0xFF555555;
    private static final int TEXT_OUTLINE = 0xFF000000;

    private static final int COLOR_UNKNOWN = 0xFF808080;

    private static final int SCREEN_OVERLAY = 0xC0000000;

    private static final Identifier CROWN_TEXTURE =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "textures/particle/crown_particle.png");

    private static final Identifier EFFECT_SPEED_TEX =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "textures/mob_effect/eternal_speed.png");
    private static final Identifier EFFECT_REGEN_TEX =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "textures/mob_effect/eternal_regeneration.png");
    private static final Identifier EFFECT_RESIST_TEX =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "textures/mob_effect/eternal_resistance.png");
    private static final Identifier EFFECT_STRENGTH_TEX =
            Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "textures/mob_effect/eternal_strength.png");

    public KingsScreen() {
        super(Component.translatable("eternal-potions.kings.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try {
            graphics.fill(0, 0, this.width, this.height, SCREEN_OVERLAY);

            int panelX = (this.width - PANEL_WIDTH) / 2;
            int panelY = (this.height - PANEL_HEIGHT) / 2;

            drawVanillaPanel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

            // ---- Title bar ----
            drawTexture(graphics, CROWN_TEXTURE, panelX + 10, panelY + 9, 16, 16);

            Component title = Component.translatable("eternal-potions.kings.title");
            graphics.text(this.font, title, panelX + 32, panelY + 12, TEXT_TITLE, false);

            drawSeparator(graphics, panelX + 4, panelY + 28, PANEL_WIDTH - 8);

            // ---- Rows ----
            List<KingsDataPayload.KingEntry> kings = EternalPotionsClient.getLastKings();
            int rowY = panelY + 36;

            for (int i = 0; i < 4; i++) {
                int slotX = panelX + SLOT_X;
                drawSlot(graphics, slotX, rowY);

                String effectText;
                String kingText;
                int effectColor;
                int kingColor;
                Identifier effectIcon = null;

                if (i < kings.size()) {
                    KingsDataPayload.KingEntry entry = kings.get(i);
                    effectText = Component.translatable(entry.effectId()).getString();
                    effectColor = colorForEffect(entry.effectId());
                    effectIcon = textureForEffect(entry.effectId());

                    if (entry.kingName() == null || entry.kingName().isEmpty()) {
                        kingText = Component.translatable("eternal-potions.kings.empty").getString();
                        kingColor = TEXT_EMPTY;
                    } else {
                        kingText = entry.kingName();
                        kingColor = TEXT_KING;
                    }
                } else {
                    effectText = "?";
                    effectColor = COLOR_UNKNOWN;
                    kingText = Component.translatable("eternal-potions.kings.empty").getString();
                    kingColor = TEXT_EMPTY;
                }

                if (effectIcon != null) {
                    drawTexture(graphics, effectIcon, slotX + 1, rowY + 1, 16, 16);
                } else {
                    drawSlotIcon(graphics, slotX, rowY, effectColor);
                }

                int textY = rowY + 5;
                drawOutlinedText(graphics, effectText, panelX + EFFECT_TEXT_X, textY, effectColor);

                int kingWidth = this.font.width(kingText);
                int kingX = panelX + PANEL_WIDTH - KING_TEXT_RIGHT_PAD - kingWidth;
                if (kingX < panelX + KING_TEXT_X) {
                    kingX = panelX + KING_TEXT_X;
                }
                graphics.text(this.font, kingText, kingX, textY, kingColor, false);

                rowY += ROW_HEIGHT;
            }

            // ---- Footer ----
            int footerSepY = panelY + PANEL_HEIGHT - 20;
            drawSeparator(graphics, panelX + 4, footerSepY, PANEL_WIDTH - 8);

            int claimed = 0;
            for (KingsDataPayload.KingEntry e : kings) {
                if (e.kingName() != null && !e.kingName().isEmpty()) claimed++;
            }
            String footerText = "Thrones Claimed: " + claimed + "/4";
            int footerWidth = this.font.width(footerText);
            int footerX = panelX + (PANEL_WIDTH - footerWidth) / 2;
            graphics.text(this.font, footerText, footerX, footerSepY + 8, TEXT_FOOTER, false);

        } catch (Throwable t) {
            EternalPotions.LOGGER.error("[EternalPotion] KingsScreen render crashed", t);
        }
    }

    /**
     * Draws a texture at (x, y) with size (w, h).
     *
     * Correct call for GuiGraphicsExtractor.blit(Identifier, int, int, int, int, float, float, float, float):
     *   arguments are (x1, y1, x2, y2, u0, u1, v0, v1)
     * where x1/y1 is the top-left corner, x2/y2 is the bottom-right corner,
     * and UVs go from 0.0 to 1.0 for a full texture.
     */
    private void drawTexture(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h) {
        try {
            g.blit(texture, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
        } catch (Throwable t) {
            EternalPotions.LOGGER.warn("[EternalPotion] blit failed for {}", texture, t);
        }
    }

    private Identifier textureForEffect(String effectId) {
        if (effectId == null) return null;
        String id = effectId.toLowerCase();
        if (id.endsWith(".eternal_speed")) return EFFECT_SPEED_TEX;
        if (id.endsWith(".eternal_regeneration")) return EFFECT_REGEN_TEX;
        if (id.endsWith(".eternal_resistance")) return EFFECT_RESIST_TEX;
        if (id.endsWith(".eternal_strength")) return EFFECT_STRENGTH_TEX;
        return null;
    }

    private int colorForEffect(String effectId) {
        if (effectId == null) return COLOR_UNKNOWN;
        String id = effectId.toLowerCase();
        if (id.endsWith(".eternal_speed")) return 0xFF7CAFC6;
        if (id.endsWith(".eternal_regeneration")) return 0xFFCD5CAB;
        if (id.endsWith(".eternal_resistance")) return 0xFF9146D5;
        if (id.endsWith(".eternal_strength")) return 0xFFFFC700;
        return COLOR_UNKNOWN;
    }

    private void drawOutlinedText(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(this.font, text, x - 1, y, TEXT_OUTLINE, false);
        g.text(this.font, text, x + 1, y, TEXT_OUTLINE, false);
        g.text(this.font, text, x, y - 1, TEXT_OUTLINE, false);
        g.text(this.font, text, x, y + 1, TEXT_OUTLINE, false);
        g.text(this.font, text, x, y, color, false);
    }

    private void drawVanillaPanel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BG_OUTLINE);
        g.fill(x, y, x + w, y + h, BG_FILL);
        g.fill(x, y, x + w - 1, y + 1, BG_LIGHT);
        g.fill(x, y, x + 1, y + h - 1, BG_LIGHT);
        g.fill(x + w - 1, y, x + w, y + h, BG_DARK);
        g.fill(x, y + h - 1, x + w, y + h, BG_DARK);

        g.fill(x - 1, y - 1, x + 1, y, SCREEN_OVERLAY);
        g.fill(x - 1, y, x, y + 1, SCREEN_OVERLAY);
        g.fill(x + w - 1, y - 1, x + w + 1, y, SCREEN_OVERLAY);
        g.fill(x + w, y, x + w + 1, y + 1, SCREEN_OVERLAY);
        g.fill(x - 1, y + h, x + 1, y + h + 1, SCREEN_OVERLAY);
        g.fill(x - 1, y + h - 1, x, y + h, SCREEN_OVERLAY);
        g.fill(x + w - 1, y + h, x + w + 1, y + h + 1, SCREEN_OVERLAY);
        g.fill(x + w, y + h - 1, x + w + 1, y + h, SCREEN_OVERLAY);
    }

    private void drawSeparator(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x, y, x + w, y + 1, BG_DARK);
        g.fill(x, y + 1, x + w, y + 2, BG_LIGHT);
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, BG_OUTLINE);
        g.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_FILL);
        g.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_DARK);
        g.fill(x, y, x + 1, y + SLOT_SIZE, SLOT_DARK);
        g.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, BG_LIGHT);
        g.fill(x + 1, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, BG_LIGHT);
    }

    private void drawSlotIcon(GuiGraphicsExtractor g, int slotX, int slotY, int color) {
        int innerX = slotX + 1;
        int innerY = slotY + 1;
        int innerSize = SLOT_SIZE - 2;

        int hi = lighten(color, 0.25f);
        int sh = darken(color, 0.25f);

        g.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, color);
        g.fill(innerX, innerY, innerX + innerSize, innerY + 1, hi);
        g.fill(innerX, innerY, innerX + 1, innerY + innerSize, hi);
        g.fill(innerX + innerSize - 1, innerY + 1, innerX + innerSize, innerY + innerSize, sh);
        g.fill(innerX + 1, innerY + innerSize - 1, innerX + innerSize, innerY + innerSize, sh);
    }

    private static int lighten(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = Math.min(255, (int) (r + (255 - r) * amount));
        g = Math.min(255, (int) (g + (255 - g) * amount));
        b = Math.min(255, (int) (b + (255 - b) * amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int argb, float amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r * (1.0f - amount));
        g = (int) (g * (1.0f - amount));
        b = (int) (b * (1.0f - amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}