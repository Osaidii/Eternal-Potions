package osaidii.eternalpotions.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
    }

    public static void registerClientConfigReceiver() {
        ClientConfigurationNetworking.registerGlobalReceiver(
                ConfigPresencePayload.TYPE,
                (payload, context) -> {
                    // No-op.
                }
        );
    }

    public record ConfigPresencePayload() implements CustomPacketPayload {

        public static final ConfigPresencePayload INSTANCE = new ConfigPresencePayload();

        public static final Type<ConfigPresencePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "config_presence"));

        public static final StreamCodec<FriendlyByteBuf, ConfigPresencePayload> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}