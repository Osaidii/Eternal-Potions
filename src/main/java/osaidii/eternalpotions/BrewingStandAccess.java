package osaidii.eternalpotions;

/**
 * Duck interface implemented by the BrewingStandBlockEntity mixin.
 * Lets external code read brewTime without shadow access.
 */
public interface BrewingStandAccess {
    int eternalPotions$getBrewTime();
}