package osaidii.eternalpotions.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class EternalNetworking {

    private EternalNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(
                KingsDataPayload.TYPE,
                KingsDataPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ModPresencePayload.TYPE,
                ModPresencePayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ModPresencePayload.TYPE,
                (payload, context) -> {
                    // No-op. The existence of this receiver is what tells
                    // clients (via ClientPlayNetworking.canSend) that this
                    // server is running Eternal Potions.
                }
        );
    }
}