package com.github.arhke.siegeengines.reinforcement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Players in view mode see how many breaks the damaged blocks around them have left.
 */
public class ReinforcementView {

    private static final int RADIUS = 5;

    private static final Set<UUID> viewers = new HashSet<>();
    private static Set<ReinforcementData.BlockDamage> shown = new HashSet<>();

    /**
     * @return true if view mode is now on for the player
     */
    public static boolean toggle(Player player) {
        if (viewers.remove(player.getUniqueId())) {
            return false;
        }
        viewers.add(player.getUniqueId());
        return true;
    }

    public static void remove(Player player) {
        viewers.remove(player.getUniqueId());
    }

    /**
     * Shows the holograms near players in view mode, and hides the ones no longer near any of them.
     */
    public static void update() {
        final Set<ReinforcementData.BlockDamage> nowShown = new HashSet<>();
        for (UUID uuid : viewers) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            for (ReinforcementData.BlockDamage damage : ReinforcementData.getNear(player.getLocation(), RADIUS)) {
                if (nowShown.add(damage)) {
                    damage.getHologram().show(damage.getLocation().getBlock(), player.getEyeLocation(), damage.getBreaksLeft());
                }
            }
        }
        for (ReinforcementData.BlockDamage damage : shown) {
            if (!nowShown.contains(damage) && damage.getHologram().isShown()) {
                damage.getHologram().hideLater();
            }
        }
        shown = nowShown;
    }

    public static void clear() {
        viewers.clear();
        shown.clear();
    }
}
