package osaidii.eternalpotions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;

public record ModPresencePayload() implements CustomPacketPayload {

    public static final ModPresencePayload INSTANCE = new ModPresencePayload();

    public static final Type<ModPresencePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "mod_presence"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModPresencePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}