package osaidii.eternalpotions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;

import java.util.ArrayList;
import java.util.List;

public record KingsDataPayload(List<KingEntry> kings) implements CustomPacketPayload {

    public static final Type<KingsDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "kings_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KingsDataPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.kings().size());
                        for (KingEntry entry : payload.kings()) {
                            buf.writeUtf(entry.effectId(), 256);
                            buf.writeUtf(entry.kingName(), 64);
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<KingEntry> list = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            String effectId = buf.readUtf(256);
                            String kingName = buf.readUtf(64);
                            list.add(new KingEntry(effectId, kingName));
                        }
                        return new KingsDataPayload(list);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record KingEntry(String effectId, String kingName) {}
}