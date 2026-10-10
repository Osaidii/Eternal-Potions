package osaidii.eternalpotions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointTransmitter;
import osaidii.eternalpotions.item.ModItems;

import java.util.Optional;

/**
 * Waypoint transmitter for an active Eternal brew, rendered on the vanilla
 * locator bar. Created when a brew starts and removed when it ends.
 *
 * Server-side only — no client code, no packets, no external mods.
 */
public class BrewWaypointTransmitter implements WaypointTransmitter {

    private final ServerLevel level;
    private final BlockPos pos;

    public BrewWaypointTransmitter(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    public BlockPos getPos() {
        return pos;
    }

    /** True only while the stand still contains an Eternal Shard in slot 3. */
    @Override
    public boolean isTransmittingWaypoint() {
        if (!level.isLoaded(pos)) return false;
        if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) return false;
        ItemStack reagent = stand.getItem(3);
        return !reagent.isEmpty() && reagent.is(ModItems.ETERNAL_SHARD);
    }

    @Override
    public Optional<Connection> makeWaypointConnectionWith(ServerPlayer player) {
        if (!isTransmittingWaypoint()) return Optional.empty();
        if (!player.level().dimension().equals(level.dimension())) return Optional.empty();

        return Optional.of(new BlockConnection() {
            @Override
            public int distanceManhattan() {
                ChunkPos brew = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
                ChunkPos p = new ChunkPos(player.blockPosition().getX() >> 4,
                        player.blockPosition().getZ() >> 4);
                return Math.abs(brew.x() - p.x()) + Math.abs(brew.z() - p.z());
            }

            @Override
            public void connect() {
                // no-op
            }

            @Override
            public void disconnect() {
                // no-op
            }

            @Override
            public void update() {
                // no-op
            }

            @Override
            public boolean isBroken() {
                return !isTransmittingWaypoint();
            }
        });
    }

    @Override
    public Waypoint.Icon waypointIcon() {
        return new Waypoint.Icon();
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