package com.github.arhke.siegeengines.util;

import com.github.arhke.siegeengines.config.Config;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Set;

/**
 * Works out how much of an explosion's push on a block goes in thin directions. The push (into the surface a siege
 * engine projectile hit, or directly away from any other explosion) is split into its x, y and z parts, in proportion
 * to how much it goes each way. Each part is checked on its own, going block by block in that direction through blocks
 * the explosion also hit: it is thin if it comes out into anything that isn't a full solid block, and not thin if it
 * comes out into a full solid block the explosion didn't hit (which includes blocks that protection plugins kept out
 * of the explosion) or one that isn't reinforceable (glass, sand...), even if the explosion hit it.
 */
public class ThinBlockUtil {

    /**
     * @return the direction from the explosion to the middle of the block
     */
    public static Vector getDirection(Location center, Block block) {
        return block.getLocation().add(0.5, 0.5, 0.5).toVector().subtract(center.toVector());
    }

    /**
     * Splits the damage between the x, y and z parts of the push, and multiplies the parts going in thin directions.
     *
     * @param direction      the direction the explosion pushes the block, which doesn't need to be normalized
     * @param affectedBlocks every block the explosion hit, before any were removed from its list
     * @param thinMultiplier what damage going in a thin direction is multiplied by
     * @return what to multiply the block's damage by: 1 if no part is thin, up to the multiplier if every part is
     */
    public static float getThinFactor(Block block, Vector direction, Set<Block> affectedBlocks, float thinMultiplier) {
        final double x = Math.abs(direction.getX()), y = Math.abs(direction.getY()), z = Math.abs(direction.getZ());
        final double total = x + y + z;
        if (total == 0) {
            return thinMultiplier;
        }
        double thinShare = 0;
        if (x > 0 && isThin(block, direction.getX() > 0 ? BlockFace.EAST : BlockFace.WEST, affectedBlocks)) {
            thinShare += x;
        }
        if (y > 0 && isThin(block, direction.getY() > 0 ? BlockFace.UP : BlockFace.DOWN, affectedBlocks)) {
            thinShare += y;
        }
        if (z > 0 && isThin(block, direction.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH, affectedBlocks)) {
            thinShare += z;
        }
        thinShare /= total;
        return (float) (1 - thinShare + thinShare * thinMultiplier);
    }

    /**
     * @param affectedBlocks every block the explosion hit, before any were removed from its list
     * @return whether no full block backs the block up in the direction, counting only reinforceable ones the explosion
     * didn't hit and non-reinforceable ones whether it hit them or not
     */
    public static boolean isThin(Block block, BlockFace direction, Set<Block> affectedBlocks) {
        Block current = block;
        // The explosion only hit so many blocks, so this ends once it is past them
        while (true) {
            current = current.getRelative(direction);
            if (!isFullCube(current)) {
                return true;
            }
            if (!affectedBlocks.contains(current) || !Config.isReinforceable(current.getType())) {
                return false;
            }
        }
    }

    /**
     * @return whether the block's collision is a whole block, so chests, slabs, stairs, fences, panes, doors... don't count
     */
    public static boolean isFullCube(Block block) {
        if (block.isPassable()) {
            return false;
        }
        final Collection<BoundingBox> boxes = block.getCollisionShape().getBoundingBoxes();
        if (boxes.size() != 1) {
            return false;
        }
        final BoundingBox box = boxes.iterator().next();
        return box.getMinX() <= 0 && box.getMinY() <= 0 && box.getMinZ() <= 0
            && box.getMaxX() >= 1 && box.getMaxY() >= 1 && box.getMaxZ() >= 1;
    }
}
