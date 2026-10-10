package osaidii.eternalpotions.client;

public final class ServerModCheck {

    /**
     * DEBUG TOGGLE.
     * Set to true to force-enable every Eternal Potions feature
     * (Kings menu, rainbow tooltip, etc.) regardless of whether the
     * connected server actually has the mod.
     *
     * Use for singleplayer testing or on servers without the mod.
     * Set to false before shipping.
     */
    public static final boolean FORCE_ENABLE = true;

    private static volatile boolean serverHasMod = false;

    private ServerModCheck() {}

    public static boolean serverHasMod() {
        if (FORCE_ENABLE) return true;
        return serverHasMod;
    }

    public static void set(boolean value) {
        serverHasMod = value;
    }
}