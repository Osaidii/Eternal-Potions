package osaidii.eternalpotions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EternalState extends SavedData {

    public static final String DATA_NAME = "eternal-potions-kings";

    public final Map<String, UUID> kings;
    public final Map<String, String> kingNames;

    public EternalState() {
        this.kings = new HashMap<>();
        this.kingNames = new HashMap<>();
    }

    public EternalState(Map<String, UUID> kings, Map<String, String> kingNames) {
        this.kings = new HashMap<>(kings);
        this.kingNames = new HashMap<>(kingNames);
    }

    public static final Codec<EternalState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC)
                            .fieldOf("kings")
                            .forGetter(state -> state.kings),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .fieldOf("kingNames")
                            .forGetter(state -> state.kingNames)
            ).apply(instance, EternalState::new)
    );

    public boolean isSlotEmpty(String effectId) {
        return !kings.containsKey(effectId);
    }

    public UUID getKing(String effectId) {
        return kings.get(effectId);
    }

    public String getKingName(String effectId) {
        return kingNames.get(effectId);
    }

    public void crownKing(String effectId, UUID playerUuid, String playerName) {
        kings.put(effectId, playerUuid);
        kingNames.put(effectId, playerName);
        setDirty();
    }

    public void removeKing(String effectId) {
        kings.remove(effectId);
        kingNames.remove(effectId);
        setDirty();
    }
}