package osaidii.eternalpotions.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import osaidii.eternalpotions.EternalPotions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BrewTimerPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final Type<BrewTimerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EternalPotions.MOD_ID, "brew_timers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewTimerPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.entries().size());
                        for (Entry e : payload.entries()) {
                            buf.writeUUID(e.id());
                            buf.writeVarInt(e.ticksLeft());
                        }
                    },
                    buf -> {
                        int n = buf.readVarInt();
                        List<Entry> list = new ArrayList<>(n);
                        for (int i = 0; i < n; i++) {
                            UUID id = buf.readUUID();
                            int t = buf.readVarInt();
                            list.add(new Entry(id, t));
                        }
                        return new BrewTimerPayload(list);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(UUID id, int ticksLeft) {}
}