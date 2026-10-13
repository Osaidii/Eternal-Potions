package osaidii.eternalpotions.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import osaidii.eternalpotions.client.BrewTimerClientCache;

@Mixin(LocatorBar.class)
public class LocatorBarTimerMixin {

    private static final int TIMER_COLOR = 0xFFFFFFFF;
    private static final float TEXT_SCALE = 0.6f;
    private static final int SPRITE_SIZE = 9;

    @Inject(method = "lambda$extractRenderState$1", at = @At("RETURN"))
    private void eternalPotions$drawBrewTimer(Entity entity,
                                              Level level,
                                              PartialTickSupplier partialTick,
                                              GuiGraphicsExtractor graphics,
                                              int barTop,
                                              TrackedWaypoint waypoint,
                                              CallbackInfo ci) {
        Integer ticks = BrewTimerClientCache.get(waypoint);
        if (ticks == null) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.mainCamera();

        double yaw = waypoint.yawAngleToCamera(level, camera, partialTick);
        int centerX = Mth.ceil((graphics.guiWidth() - SPRITE_SIZE) / 2.0f);
        int offset = Mth.floor(yaw * 173.0 / 120.0);
        int spriteX = centerX + offset;
        int spriteY = barTop - 2;

        int barLeft = (graphics.guiWidth() - ContextualBar.WIDTH) / 2;
        int barRight = barLeft + ContextualBar.WIDTH;
        if (spriteX < barLeft || spriteX + SPRITE_SIZE > barRight) return;

        String text = formatTicks(ticks);
        int textWidth = mc.font.width(text);

        float invScale = 1.0f / TEXT_SCALE;
        float drawX = (spriteX + 7 - textWidth / 2.0f) * invScale;
        float drawY = (spriteY - 7) * invScale;

        graphics.pose().pushMatrix();
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        graphics.text(mc.font, text, (int) drawX, (int) drawY, TIMER_COLOR, true);
        graphics.pose().popMatrix();
    }

    private static String formatTicks(int ticks) {
        int totalSec = Math.max(0, ticks / 20);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }
}