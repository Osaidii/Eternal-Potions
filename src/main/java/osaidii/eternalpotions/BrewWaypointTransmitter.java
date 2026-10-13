package osaidii.eternalpotions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class BrewWaypointTransmitter implements WaypointTransmitter {

    private final ServerLevel level;
    private final BlockPos pos;
    private final UUID id;

    public BrewWaypointTransmitter(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
        String key = level.dimension().toString() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        this.id = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public BlockPos getPos() { return pos; }
    public ServerLevel getLevel() { return level; }
    public UUID getId() { return id; }

    @Override
    public boolean isTransmittingWaypoint() {
        for (BrewWaypoints.Brew b : BrewWaypoints.getAll()) {
            if (b.pos.equals(pos)) return true;
        }
        return false;
    }

    @Override
    public Optional<Connection> makeWaypointConnectionWith(ServerPlayer player) {
        EternalPotions.LOGGER.info("[BrewWaypoint] makeWaypointConnectionWith called by {}", player.getName().getString());

        if (!isTransmittingWaypoint()) return Optional.empty();
        if (!player.level().dimension().equals(level.dimension())) return Optional.empty();

        final Waypoint.Icon icon = waypointIcon();
        final BlockPos targetPos = pos;
        final UUID targetId = id;
        final ServerPlayer receiver = player;

        return Optional.of(new BlockConnection() {
            @Override
            public int distanceManhattan() {
                ChunkPos brew = new ChunkPos(targetPos.getX() >> 4, targetPos.getZ() >> 4);
                ChunkPos p = new ChunkPos(receiver.blockPosition().getX() >> 4,
                        receiver.blockPosition().getZ() >> 4);
                return Math.abs(brew.x() - p.x()) + Math.abs(brew.z() - p.z());
            }

            @Override
            public void connect() {
                EternalPotions.LOGGER.info("[BrewWaypoint] connect() sending addWaypointPosition id={}", targetId);
                receiver.connection.send(ClientboundTrackedWaypointPacket.addWaypointPosition(
                        targetId, icon, targetPos));
            }

            @Override
            public void disconnect() {
                EternalPotions.LOGGER.info("[BrewWaypoint] disconnect() sending removeWaypoint id={}", targetId);
                receiver.connection.send(ClientboundTrackedWaypointPacket.removeWaypoint(targetId));
            }

            @Override
            public void update() {
                // Position never changes for a brewing stand. Nothing to send.
            }

            @Override
            public boolean isBroken() {
                return !isTransmittingWaypoint();
            }
        });
    }

    @Override
    public Waypoint.Icon waypointIcon() {
        Waypoint.Icon icon = new Waypoint.Icon();
        icon.style = WaypointStyleAssets.DEFAULT;
        return icon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrewWaypointTransmitter other)) return false;
        return level.dimension().equals(other.level.dimension()) && pos.equals(other.pos);
    }

    @Override
    public int hashCode() {
        return level.dimension().hashCode() * 31 + pos.hashCode();
    }
}