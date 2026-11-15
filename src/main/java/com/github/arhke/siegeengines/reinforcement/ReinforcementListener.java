package com.github.arhke.siegeengines.reinforcement;

import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.data.SiegeEnginesData;
import com.github.arhke.siegeengines.util.ThinBlockUtil;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every reinforceable block takes {@link Config#reinforcementDefaultBreaks} breaks from explosions before it breaks
 * (see {@link ReinforcementData}). The damage is split between the x, y and z directions the explosion pushes the block
 * in, and the share going in each direction that is thin takes {@link Config#reinforcementThinBlockDamageMultiplier}
 * times the damage (see {@link ThinBlockUtil}). Mining by hand is not affected.
 */
public class ReinforcementListener implements Listener {

    // Runs after protection plugins have removed the blocks they protect
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!breaksBlocks(event.getExplosionResult())) {
            return;
        }
        explode(event.getBlock().getLocation().add(0.5, 0.5, 0.5), event.blockList(), false, null, 1f, null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!breaksBlocks(event.getExplosionResult())) {
            return;
        }
        final PersistentDataContainer data = event.getEntity().getPersistentDataContainer();
        explode(event.getLocation(), event.blockList(), true,
            data.get(SiegeEnginesData.explosionStrengthKey, PersistentDataType.FLOAT),
            data.getOrDefault(SiegeEnginesData.explosionImpactFactorKey, PersistentDataType.FLOAT, 1f),
            getDirection(data.get(SiegeEnginesData.explosionDirectionKey, PersistentDataType.LIST.doubles())));
    }

    /**
     * Wind charges list the blocks they reach too, but only trigger them (doors, buttons...), so they must not damage them.
     */
    private static boolean breaksBlocks(ExplosionResult result) {
        return result == ExplosionResult.DESTROY || result == ExplosionResult.DESTROY_WITH_DECAY;
    }

    private static @Nullable Vector getDirection(@Nullable List<Double> components) {
        if (components == null || components.size() != 3) {
            return null;
        }
        return new Vector(components.get(0), components.get(1), components.get(2));
    }

    /**
     * @param strength     the siege engine explosion's strength, or null for any other explosion
     * @param impactFactor how squarely the projectile hit, 1 for head-on, which the damage is scaled by
     * @param direction    the direction into the surface a siege engine projectile hit, or null to push each block
     *                     directly away from the explosion
     */
    private static void explode(Location center, List<Block> affectedBlocks, boolean entityExplosion, @Nullable Float strength,
                                float impactFactor, @Nullable Vector direction) {
        if (!Config.reinforcementEnabled) {
            return;
        }
        final float maxDamage = Math.max(0, strength == null ? Config.reinforcementExplosionStrength : strength);
        // Worked out before anything breaks or leaves the list, so breaking one block doesn't make another thin
        final Set<Block> affected = new HashSet<>(affectedBlocks);
        final Map<Block, Float> thinFactors = new HashMap<>();
        for (Block block : affectedBlocks) {
            if (isReinforced(block)) {
                thinFactors.put(block, ThinBlockUtil.getThinFactor(block,
                    direction == null ? ThinBlockUtil.getDirection(center, block) : direction, affected,
                    Config.reinforcementThinBlockDamageMultiplier));
            }
        }
        final Iterator<Block> affectedBlockIterator = affectedBlocks.iterator();
        while (affectedBlockIterator.hasNext()) {
            final Block block = affectedBlockIterator.next();
            if (!isReinforced(block)) {
                continue;
            }
            affectedBlockIterator.remove();

            // Full strength at the explosion, fading to nothing at the falloff radius
            float breaks = maxDamage * impactFactor;
            if (Config.reinforcementDamageFalloffRadius > 0) {
                final double distance = block.getLocation().add(0.5, 0.5, 0.5).distance(center);
                breaks *= (float) Math.max(0, 1 - distance / Config.reinforcementDamageFalloffRadius);
            }
            breaks *= thinFactors.get(block);
            // Out of reach of the falloff: the block is protected, but isn't tracked as damaged
            if (breaks <= 0) {
                continue;
            }
            final ReinforcementData.BlockDamage damage = ReinforcementData.getOrCreate(block);
            damage.damage(breaks);
            if (damage.getBreaksLeft() > 0) {
                ReinforcementEffects.hit(block, damage, breaks);
                damage.getHologram().flash(block, center, damage.getBreaksLeft(), breaks / damage.getMaxBreaks());
                continue;
            }

            ReinforcementData.remove(block);
            block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 0.5f);
            if (entityExplosion) {
                block.breakNaturally();
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), 1));
                block.setType(Material.AIR);
            }
        }
    }

    private static boolean isReinforced(Block block) {
        return !block.getType().isAir() && Config.isReinforceable(block.getType());
    }

    // Damage belongs to the block that took it, not to whatever is put there next
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ReinforcementData.remove(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ReinforcementData.remove(event.getBlock());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        ReinforcementData.removeChunk(event.getChunk());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ReinforcementView.remove(event.getPlayer());
    }
}
