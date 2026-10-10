package osaidii.eternalpotions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Registry of active Eternal brew waypoints.
 *
 * A transmitter is registered with the level's ServerWaypointManager when a brew
 * starts, and removed when the shard is consumed, the stand is destroyed, or the
 * server is stopped. No client-side code; the locator bar is pure vanilla UI.
 */
public final class BrewWaypoints {

    private static final Map<GlobalPos, BrewWaypointTransmitter> ACTIVE = new HashMap<>();

    private BrewWaypoints() {}

    public static void startBrew(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        if (ACTIVE.containsKey(key)) return;

        BrewWaypointTransmitter transmitter = new BrewWaypointTransmitter(level, pos);
        level.getWaypointManager().trackWaypoint(transmitter);
        ACTIVE.put(key, transmitter);
    }

    public static void stopBrew(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        BrewWaypointTransmitter transmitter = ACTIVE.remove(key);
        if (transmitter != null) {
            level.getWaypointManager().untrackWaypoint(transmitter);
        }
    }

    /** Called every server tick. Drops transmitters whose brew has ended. */
    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<GlobalPos, BrewWaypointTransmitter>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<GlobalPos, BrewWaypointTransmitter> entry = it.next();
            BrewWaypointTransmitter transmitter = entry.getValue();

            if (!transmitter.isTransmittingWaypoint()) {
                ServerLevel level = server.getLevel(entry.getKey().dimension());
                if (level != null) {
                    level.getWaypointManager().untrackWaypoint(transmitter);
                }
                it.remove();
            }
        }
    }

    /** Called on server start/shutdown. Untracks every transmitter. */
    public static void clear(MinecraftServer server) {
        for (Map.Entry<GlobalPos, BrewWaypointTransmitter> entry : ACTIVE.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level != null) {
                level.getWaypointManager().untrackWaypoint(entry.getValue());
            }
        }
        ACTIVE.clear();
    }
}