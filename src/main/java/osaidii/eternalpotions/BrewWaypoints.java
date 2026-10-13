package osaidii.eternalpotions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BrewWaypoints {

    public static final class Brew {
        public final BlockPos pos;
        public int ticksLeft;

        Brew(BlockPos pos, int ticksLeft) {
            this.pos = pos;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final Map<GlobalPos, Brew> ACTIVE = new HashMap<>();
    private static final Map<GlobalPos, BrewWaypointTransmitter> TRANSMITTERS = new HashMap<>();

    private BrewWaypoints() {}

    public static void startBrew(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        if (TRANSMITTERS.containsKey(key)) return;

        BrewWaypointTransmitter transmitter = new BrewWaypointTransmitter(level, pos);
        try {
            level.getWaypointManager().trackWaypoint(transmitter);
            EternalPotions.LOGGER.info("[BrewWaypoint] trackWaypoint OK");
        } catch (Throwable t) {
            EternalPotions.LOGGER.error("[BrewWaypoint] trackWaypoint FAILED", t);
            return;
        }
        TRANSMITTERS.put(key, transmitter);
    }

    public static void touch(ServerLevel level, BlockPos pos, int ticksLeft) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        Brew existing = ACTIVE.get(key);
        if (existing == null) {
            ACTIVE.put(key, new Brew(pos.immutable(), ticksLeft));
        } else {
            existing.ticksLeft = ticksLeft;
        }
    }

    public static void stopBrew(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        ACTIVE.remove(key);
        BrewWaypointTransmitter transmitter = TRANSMITTERS.remove(key);
        if (transmitter != null) {
            try {
                level.getWaypointManager().untrackWaypoint(transmitter);
            } catch (Throwable t) {
                EternalPotions.LOGGER.error("[BrewWaypoint] untrackWaypoint FAILED", t);
            }
        }
    }

    public static void tick(ServerLevel level) {
        List<GlobalPos> toRemove = new ArrayList<>();
        for (Map.Entry<GlobalPos, BrewWaypointTransmitter> entry : TRANSMITTERS.entrySet()) {
            if (!entry.getKey().dimension().equals(level.dimension())) continue;
            if (!entry.getValue().isTransmittingWaypoint()) {
                level.getWaypointManager().untrackWaypoint(entry.getValue());
                toRemove.add(entry.getKey());
            }
        }
        for (GlobalPos key : toRemove) {
            TRANSMITTERS.remove(key);
        }
    }

    public static void clear(ServerLevel level) {
        for (Map.Entry<GlobalPos, BrewWaypointTransmitter> entry : TRANSMITTERS.entrySet()) {
            if (entry.getKey().dimension().equals(level.dimension())) {
                level.getWaypointManager().untrackWaypoint(entry.getValue());
            }
        }
        TRANSMITTERS.entrySet().removeIf(e -> e.getKey().dimension().equals(level.dimension()));
        ACTIVE.entrySet().removeIf(e -> e.getKey().dimension().equals(level.dimension()));
    }

    public static Collection<Brew> getAll() {
        return ACTIVE.values();
    }

    public static Collection<BrewWaypointTransmitter> getAllTransmitters() {
        return TRANSMITTERS.values();
    }

    public static Brew getBrew(BlockPos pos) {
        for (Brew b : ACTIVE.values()) {
            if (b.pos.equals(pos)) return b;
        }
        return null;
    }
}