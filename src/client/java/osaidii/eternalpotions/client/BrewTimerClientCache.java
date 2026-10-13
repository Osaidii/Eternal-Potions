package osaidii.eternalpotions.client;

import net.minecraft.world.waypoints.TrackedWaypoint;
import osaidii.eternalpotions.network.BrewTimerPayload;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BrewTimerClientCache {

    private static final Map<UUID, Integer> TIMERS = new HashMap<>();

    private BrewTimerClientCache() {}

    public static void update(List<BrewTimerPayload.Entry> entries) {
        TIMERS.clear();
        for (BrewTimerPayload.Entry e : entries) {
            TIMERS.put(e.id(), e.ticksLeft());
        }
    }

    public static Integer get(TrackedWaypoint waypoint) {
        UUID id = waypoint.id().left().orElse(null);
        if (id == null) return null;
        return TIMERS.get(id);
    }

    public static void clear() {
        TIMERS.clear();
    }
}