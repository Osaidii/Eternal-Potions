package osaidii.eternalpotions.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.BrewWaypoints;
import osaidii.eternalpotions.BrewingStandAccess;
import osaidii.eternalpotions.item.ModItems;

@Mixin(BrewingStandBlockEntity.class)
public class BrewingStandMixin implements BrewingStandAccess {

    @Shadow private int brewTime;
    @Shadow private int totalBrewTime;
    @Shadow private NonNullList<ItemStack> items;

    @Override
    public int eternalPotions$getBrewTime() {
        return this.brewTime;
    }

    /** True only while the Eternal Shard is actively being consumed. */
    private boolean eternalPotions$isEternalBrew() {
        ItemStack ingredient = this.items.get(3);
        return ingredient != null && !ingredient.isEmpty()
                && ingredient.is(ModItems.ETERNAL_SHARD)
                && this.brewTime > 0;
    }

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void eternalPotions$serverTick(
            ServerLevel level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity entity, CallbackInfo ci) {
        BrewingStandMixin self = (BrewingStandMixin)(Object) entity;

        if (!self.eternalPotions$isEternalBrew()) return;

        boolean firstEternalTick = self.totalBrewTime != 12000;
        if (firstEternalTick) {
            int elapsed = self.totalBrewTime - self.brewTime;
            self.totalBrewTime = 12000;
            self.brewTime = 12000 - Math.max(0, elapsed);

            BrewWaypoints.startBrew(level, pos);

            Component msg = Component.translatable("eternal-potions.brew.started",
                            pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
            level.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }

        ItemStack slot0 = self.items.get(0);
        ItemStack slot1 = self.items.get(1);
        ItemStack slot2 = self.items.get(2);

        int occupied = 0;
        if (!slot0.isEmpty()) occupied++;
        if (!slot1.isEmpty()) occupied++;
        if (!slot2.isEmpty()) occupied++;

        if (occupied == 1 && !slot1.isEmpty() && slot0.isEmpty() && slot2.isEmpty()) return;

        ItemStack keep = ItemStack.EMPTY;
        if (!slot1.isEmpty()) keep = slot1.copy();
        else if (!slot0.isEmpty()) keep = slot0.copy();
        else if (!slot2.isEmpty()) keep = slot2.copy();

        if (keep.isEmpty()) return;

        self.items.set(0, ItemStack.EMPTY);
        self.items.set(1, keep);
        self.items.set(2, ItemStack.EMPTY);
    }

    @Inject(method = "canTakeItemThroughFace", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$lockExtraction(int slot, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.eternalPotions$isEternalBrew()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$lockPlacement(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.eternalPotions$isEternalBrew()) {
            cir.setReturnValue(false);
        }
    }
}

@Mixin(AbstractContainerMenu.class)
class BrewingStandMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void eternalPotions$lockGui(int slotId, int button, ContainerInput input,
                                        Player player, CallbackInfo ci) {
        if (slotId < 0) return;

        AbstractContainerMenu menu = (AbstractContainerMenu)(Object) this;
        if (slotId >= menu.slots.size()) return;

        Slot slot = menu.slots.get(slotId);
        if (!(slot.container instanceof BrewingStandBlockEntity stand)) return;

        // Same rule as eternalPotions$isEternalBrew(): shard present AND brewTime > 0.
        // When brewing ends, brewTime is 0 and the lock releases even if leftover
        // shards remain in slot 3.
        if (!(stand instanceof BrewingStandAccess access)) return;
        if (access.eternalPotions$getBrewTime() <= 0) return;

        ItemStack reagent = stand.getItem(3);
        if (reagent.isEmpty() || !reagent.is(ModItems.ETERNAL_SHARD)) return;

        ci.cancel();
    }
}