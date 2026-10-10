package osaidii.eternalpotions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EternalState extends SavedData {

    public final Map<String, UUID> kings;
    public final Map<String, String> kingNames;
    public final Map<String, List<String>> pendingRespawns;

    public EternalState() {
        this.kings = new HashMap<>();
        this.kingNames = new HashMap<>();
        this.pendingRespawns = new HashMap<>();
    }

    public EternalState(Map<String, UUID> kings,
                        Map<String, String> kingNames,
                        Map<String, List<String>> pendingRespawns) {
        this.kings = new HashMap<>(kings);
        this.kingNames = new HashMap<>(kingNames);
        this.pendingRespawns = new HashMap<>();
        pendingRespawns.forEach((k, v) -> this.pendingRespawns.put(k, new ArrayList<>(v)));
    }

    public static final Codec<EternalState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC)
                            .fieldOf("kings")
                            .forGetter(state -> state.kings),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING)
                            .fieldOf("kingNames")
                            .forGetter(state -> state.kingNames),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                            .optionalFieldOf("pendingRespawns", Map.of())
                            .forGetter(state -> state.pendingRespawns)
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

    /** Refreshes the stored name if it changed. No-op if unchanged or slot empty. */
    public void updateKingName(String effectId, String newName) {
        if (!kingNames.containsKey(effectId)) return;
        if (newName.equals(kingNames.get(effectId))) return;
        kingNames.put(effectId, newName);
        setDirty();
    }

    public void addPendingRespawn(UUID uuid, String effectId) {
        pendingRespawns.computeIfAbsent(uuid.toString(), k -> new ArrayList<>()).add(effectId);
        setDirty();
    }

    public List<String> takePendingRespawns(UUID uuid) {
        List<String> result = pendingRespawns.remove(uuid.toString());
        if (result != null) setDirty();
        return result;
    }
}