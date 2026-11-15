package com.github.arhke.siegeengines.reinforcement;

import com.github.arhke.siegeengines.config.Config;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps track of the blocks damaged by explosions. Every reinforceable block starts with
 * {@link Config#reinforcementDefaultBreaks} breaks, so only blocks that have taken damage are tracked.
 * Nothing is saved: the damage is forgotten when its chunk unloads or after {@link Config#reinforcementForgetMillis}.
 */
public class ReinforcementData {

    private record ChunkKey(UUID world, int x, int z) {
        static ChunkKey of(Block block) {
            return new ChunkKey(block.getWorld().getUID(), block.getX() >> 4, block.getZ() >> 4);
        }

        static ChunkKey of(Chunk chunk) {
            return new ChunkKey(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        }
    }

    // Crack ids, which the client keeps cracks apart by, counting down so they don't clash with entity ids
    private static int nextCrackId = -1;

    public static class BlockDamage {
        private final Location location;
        private final int crackId = nextCrackId--;
        private final Material material;
        private final float maxBreaks;
        private float breaksTaken;
        private long lastDamaged;
        private final ReinforcementHologram hologram = new ReinforcementHologram();

        private BlockDamage(Block block) {
            location = block.getLocation();
            material = block.getType();
            maxBreaks = Config.reinforcementDefaultBreaks;
            lastDamaged = System.currentTimeMillis();
        }

        public Location getLocation() {
            return location;
        }

        public float getMaxBreaks() {
            return maxBreaks;
        }

        public int getCrackId() {
            return crackId;
        }

        public float getBreaksLeft() {
            return maxBreaks - breaksTaken;
        }

        public void damage(float breaks) {
            breaksTaken += breaks;
            lastDamaged = System.currentTimeMillis();
        }

        public ReinforcementHologram getHologram() {
            return hologram;
        }

        // Damage is forgotten after a while, or once the block has been replaced by a different one
        private boolean isStale(long now) {
            return now - lastDamaged > Config.reinforcementForgetMillis || location.getBlock().getType() != material;
        }
    }

    private static final Map<ChunkKey, Map<Location, BlockDamage>> damagedBlocks = new HashMap<>();

    /**
     * @return the block's damage, or null if it hasn't been damaged
     */
    public static @Nullable BlockDamage get(Block block) {
        final Map<Location, BlockDamage> chunk = damagedBlocks.get(ChunkKey.of(block));
        if (chunk == null) {
            return null;
        }
        final BlockDamage damage = chunk.get(block.getLocation());
        if (damage != null && damage.isStale(System.currentTimeMillis())) {
            remove(block);
            return null;
        }
        return damage;
    }

    public static BlockDamage getOrCreate(Block block) {
        final BlockDamage damage = get(block);
        if (damage != null) {
            return damage;
        }
        final BlockDamage created = new BlockDamage(block);
        damagedBlocks.computeIfAbsent(ChunkKey.of(block), key -> new HashMap<>()).put(created.location, created);
        return created;
    }

    public static void remove(Block block) {
        final ChunkKey key = ChunkKey.of(block);
        final Map<Location, BlockDamage> chunk = damagedBlocks.get(key);
        if (chunk == null) {
            return;
        }
        final BlockDamage damage = chunk.remove(block.getLocation());
        if (damage != null) {
            damage.hologram.removeNow();
            ReinforcementEffects.clearCracks(damage);
        }
        if (chunk.isEmpty()) {
            damagedBlocks.remove(key);
        }
    }

    public static void removeChunk(Chunk chunk) {
        final Map<Location, BlockDamage> removed = damagedBlocks.remove(ChunkKey.of(chunk));
        if (removed != null) {
            removed.values().forEach(damage -> {
                damage.hologram.removeNow();
                ReinforcementEffects.clearCracks(damage);
            });
        }
    }

    public static void removeStale() {
        final long now = System.currentTimeMillis();
        final Iterator<Map<Location, BlockDamage>> chunks = damagedBlocks.values().iterator();
        while (chunks.hasNext()) {
            final Map<Location, BlockDamage> chunk = chunks.next();
            chunk.values().removeIf(damage -> {
                if (!damage.isStale(now)) {
                    return false;
                }
                damage.hologram.hide();
                ReinforcementEffects.clearCracks(damage);
                return true;
            });
            if (chunk.isEmpty()) {
                chunks.remove();
            }
        }
    }

    public static void clear() {
        damagedBlocks.values().forEach(chunk -> chunk.values().forEach(damage -> {
            damage.hologram.removeNow();
            ReinforcementEffects.clearCracks(damage);
        }));
        damagedBlocks.clear();
    }

    /**
     * @return every damaged block that hasn't been forgotten yet
     */
    public static List<BlockDamage> getAll() {
        final List<BlockDamage> all = new ArrayList<>();
        final long now = System.currentTimeMillis();
        for (Map<Location, BlockDamage> chunk : damagedBlocks.values()) {
            for (BlockDamage damage : chunk.values()) {
                if (!damage.isStale(now)) {
                    all.add(damage);
                }
            }
        }
        return all;
    }

    /**
     * @return the damaged blocks within the radius of the location
     */
    public static List<BlockDamage> getNear(Location location, int radius) {
        final List<BlockDamage> near = new ArrayList<>();
        final UUID world = location.getWorld().getUID();
        final int minX = (location.getBlockX() - radius) >> 4, maxX = (location.getBlockX() + radius) >> 4;
        final int minZ = (location.getBlockZ() - radius) >> 4, maxZ = (location.getBlockZ() + radius) >> 4;
        final long now = System.currentTimeMillis();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final Map<Location, BlockDamage> chunk = damagedBlocks.get(new ChunkKey(world, x, z));
                if (chunk == null) {
                    continue;
                }
                for (BlockDamage damage : chunk.values()) {
                    if (!damage.isStale(now) && damage.location.clone().add(0.5, 0.5, 0.5).distanceSquared(location) <= radius * radius) {
                        near.add(damage);
                    }
                }
            }
        }
        return near;
    }
}
