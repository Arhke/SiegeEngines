package com.github.arhke.siegeengines.listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Takes siege engines off players' heads. New siege engine items can't be worn, this catches ones made before that.
 */
public class SiegeEngineWearListener implements Listener {

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.HEAD || !SiegeEnginesUtil.isSiegeEngineItem(event.getNewItem())) {
            return;
        }
        final Player player = event.getPlayer();
        // The event is called while the helmet is being changed, so take it off afterwards
        Bukkit.getScheduler().runTask(SiegeEngines.getInstance(), () -> {
            final PlayerInventory inventory = player.getInventory();
            final ItemStack helmet = inventory.getHelmet();
            if (!SiegeEnginesUtil.isSiegeEngineItem(helmet)) {
                return;
            }
            inventory.setHelmet(null);
            for (ItemStack leftover : inventory.addItem(SiegeEnginesUtil.makeUnwearable(helmet)).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        });
    }
}
