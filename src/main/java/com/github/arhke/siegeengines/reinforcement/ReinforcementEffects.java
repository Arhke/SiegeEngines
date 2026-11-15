package com.github.arhke.siegeengines.reinforcement;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Makes damaged blocks look damaged to everyone, without needing view mode: cracks on the block, debris when it is
 * hit, and dust trickling out of it when it is close to breaking.
 */
public class ReinforcementEffects {

    // How far away players see the cracks
    private static final int CRACK_RANGE = 64;
    // Clients forget cracks after 20 seconds without an update, so they are sent again before then
    private static final int CRACK_REFRESH_TICKS = 200;
    // Blocks with less than this much of their breaks left trickle dust
    private static final float CRUMBLING = 0.3f;
    // Chance each run that a crumbling block drops dust, at its most damaged
    private static final double DUST_CHANCE = 0.35;
    private static final BlockFace[] SIDES = {BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH};

    // How often tick runs
    public static final int TICK_INTERVAL = 5;

    private static int ticks;

    /**
     * Runs every {@link #TICK_INTERVAL} ticks: drops dust from crumbling blocks, and sends the cracks again.
     */
    public static void tick() {
        ticks += TICK_INTERVAL;
        final boolean refreshCracks = ticks >= CRACK_REFRESH_TICKS;
        if (refreshCracks) {
            ticks = 0;
        }
        for (ReinforcementData.BlockDamage damage : ReinforcementData.getAll()) {
            if (refreshCracks) {
                sendCracks(damage);
            }
            crumble(damage);
        }
    }

    /**
     * Debris flies off the block, more for bigger hits.
     *
     * @param breaks how many breaks the hit did
     */
    public static void hit(Block block, ReinforcementData.BlockDamage damage, float breaks) {
        final BlockData data = block.getBlockData();
        final Location center = block.getLocation().add(0.5, 0.5, 0.5);
        final float hitShare = Math.min(1, breaks / damage.getMaxBreaks());
        final int count = 6 + Math.round(30 * Math.min(1, hitShare * 4));
        block.getWorld().spawnParticle(Particle.BLOCK, center, count, 0.35, 0.35, 0.35, 0.15, data);
        sendCracks(damage);
    }

    /**
     * Shows the block's cracks to the players near it, in proportion to the breaks it has taken.
     */
    public static void sendCracks(ReinforcementData.BlockDamage damage) {
        // Nothing taken would clear the cracks, so the smallest bit of damage shows the first crack
        final float progress = Math.max(0.001f, Math.min(1, 1 - damage.getBreaksLeft() / damage.getMaxBreaks()));
        sendCracks(damage, progress);
    }

    /**
     * Clears the block's cracks for the players near it.
     */
    public static void clearCracks(ReinforcementData.BlockDamage damage) {
        sendCracks(damage, 0);
    }

    private static void sendCracks(ReinforcementData.BlockDamage damage, float progress) {
        final Location location = damage.getLocation();
        final World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= CRACK_RANGE * CRACK_RANGE) {
                player.sendBlockDamage(location, progress, damage.getCrackId());
            }
        }
    }

    // Dust falls out of the bottom of a crumbling block, or down its sides when something is under it
    private static void crumble(ReinforcementData.BlockDamage damage) {
        final float health = damage.getBreaksLeft() / damage.getMaxBreaks();
        if (health >= CRUMBLING) {
            return;
        }
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        // More dust the closer it is to breaking
        final double severity = 1 - health / CRUMBLING;
        if (random.nextDouble() >= DUST_CHANCE * (0.3 + 0.7 * severity)) {
            return;
        }
        final Block block = damage.getLocation().getBlock();
        final Location spawn = getDustLocation(block, random);
        if (spawn == null) {
            return;
        }
        block.getWorld().spawnParticle(Particle.FALLING_DUST, spawn, 1 + random.nextInt(2), 0.1, 0, 0.1, 0, block.getBlockData());
    }

    // Checked without loading the chunk next door, which would be slow to do every few ticks
    private static boolean isOpen(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4) && block.isPassable();
    }

    private static Location getDustLocation(Block block, ThreadLocalRandom random) {
        final Location corner = block.getLocation();
        if (isOpen(block.getRelative(BlockFace.DOWN))) {
            return corner.add(0.15 + random.nextDouble() * 0.7, -0.05, 0.15 + random.nextDouble() * 0.7);
        }
        final List<BlockFace> open = new ArrayList<>(4);
        for (BlockFace side : SIDES) {
            if (isOpen(block.getRelative(side))) {
                open.add(side);
            }
        }
        if (open.isEmpty()) {
            return null;
        }
        final BlockFace side = open.get(random.nextInt(open.size()));
        // Just outside the face, low down, somewhere along it
        final double along = 0.15 + random.nextDouble() * 0.7;
        final double x = side.getModX() == 0 ? along : side.getModX() > 0 ? 1.05 : -0.05;
        final double z = side.getModZ() == 0 ? along : side.getModZ() > 0 ? 1.05 : -0.05;
        return corner.add(x, 0.1 + random.nextDouble() * 0.3, z);
    }
}
