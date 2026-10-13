package osaidii.eternalpotions.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.waypoints.ServerWaypointManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWaypointManager.class)
public class LocatorBarGateMixin {

    @Inject(method = "isLocatorBarEnabledFor", at = @At("HEAD"), cancellable = true)
    private static void eternalPotions$alwaysEnable(ServerPlayer player,
                                                    CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}