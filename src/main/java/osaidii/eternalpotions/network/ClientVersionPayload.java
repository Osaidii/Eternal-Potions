package osaidii.eternalpotions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;

public record ClientVersionPayload(String clientVersion) implements CustomPacketPayload {

    public static final Type<ClientVersionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "client_version"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientVersionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.clientVersion(), 64),
                    buf -> new ClientVersionPayload(buf.readUtf(64))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}