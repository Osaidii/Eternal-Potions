package osaidii.eternalpotions.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import osaidii.eternalpotions.EternalPotions;
import osaidii.eternalpotions.SingleplayerDetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(RecipeManager.class)
public class RecipeHideMixin {

    @Inject(method = "getLearnableRecipes", at = @At("RETURN"), cancellable = true)
    private void eternalPotions$hideLearnableRecipes(CallbackInfoReturnable<Collection<RecipeHolder<?>>> cir) {
        if (EternalPotions.EVERYTHING_EVERYWHERE) return;
        if (!SingleplayerDetection.isSingleplayer()) return;

        Collection<RecipeHolder<?>> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<RecipeHolder<?>> filtered = new ArrayList<>(original.size());
        int removed = 0;

        for (RecipeHolder<?> holder : original) {
            Identifier id = holder.id().identifier();
            if (id.getNamespace().equals(EternalPotions.MOD_ID)) {
                removed++;
                continue;
            }
            filtered.add(holder);
        }

        if (removed > 0) {
            EternalPotions.LOGGER.info("[EternalPotion] hid {} eternal recipes from recipe book", removed);
            cir.setReturnValue(filtered);
        }
    }
}