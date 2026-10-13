package osaidii.eternalpotions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class SingleplayerDetection {

    private static volatile boolean singleplayer = false;

    private SingleplayerDetection() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            singleplayer = !server.isDedicatedServer();
            EternalPotions.LOGGER.info("[EternalPotion] singleplayer detected: {}", singleplayer);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            singleplayer = false;
        });
    }

    public static boolean isSingleplayer() {
        return singleplayer;
    }
}