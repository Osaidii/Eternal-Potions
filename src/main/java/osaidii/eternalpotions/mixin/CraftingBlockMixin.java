package osaidii.eternalpotions.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.SingleplayerDetection;
import osaidii.eternalpotions.item.ModItems;

@Mixin(AbstractContainerMenu.class)
public class CraftingBlockMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$blockEternalCraft(int slotId, int button, ContainerInput input,
                                                  Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu)(Object) this;

        if (!(menu instanceof CraftingMenu)) return;
        if (slotId != 0) return;
        if (EternalPotions.EVERYTHING_EVERYWHERE) return;
        if (!SingleplayerDetection.isSingleplayer()) return;
        if (player.level().isClientSide()) return;

        ItemStack result = menu.getSlot(0).getItem();
        if (result.isEmpty()) return;

        if (result.is(ModItems.ETERNAL_SHARD) || result.is(ModItems.ETERNAL_POTION)) {
            ci.cancel();
        }
    }
}