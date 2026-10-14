package osaidii.eternalpotions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;

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