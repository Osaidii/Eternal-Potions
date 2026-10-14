package osaidii.eternalpotions.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import osaidii.eternalpotions.EternalPotions;

public final class EternalNetworking {

    private EternalNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundConfiguration().register(
                ConfigPresencePayload.TYPE,
                ConfigPresencePayload.STREAM_CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                KingsDataPayload.TYPE,
                KingsDataPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
                BrewTimerPayload.TYPE,
                BrewTimerPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                ClientVersionPayload.TYPE,
                ClientVersionPayload.STREAM_CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ClientVersionPayload.TYPE,
                (payload, context) -> {
                    String serverVersion = EternalPotions.getModVersion();
                    String clientVersion = payload.clientVersion();
                    if (!serverVersion.equals(clientVersion)) {
                        context.player().connection.disconnect(Component.literal(
                                "Eternal Potions: version mismatch.\n" +
                                        "Install version " + serverVersion + "."
                        ));
                    }
                }
        );
    }

    public static void registerClientConfigReceiver() {
        ClientConfigurationNetworking.registerGlobalReceiver(
                ConfigPresencePayload.TYPE,
                (payload, context) -> {
                    // No-op. Receiver existing is the signal.
                }
        );
    }
}