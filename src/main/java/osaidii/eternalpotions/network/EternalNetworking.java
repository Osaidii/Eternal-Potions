package osaidii.eternalpotions.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class EternalNetworking {

    private EternalNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
                KingsDataPayload.TYPE,
                KingsDataPayload.STREAM_CODEC
        );
    }
}