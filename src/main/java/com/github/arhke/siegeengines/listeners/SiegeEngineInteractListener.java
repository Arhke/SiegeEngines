package com.github.arhke.siegeengines.listeners;

import com.github.arhke.siegeengines.SiegeEngine;
import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.data.SiegeEnginesData;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SiegeEngineInteractListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onSiegeEngineClick(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.ARMOR_STAND) {
            // Only handle the main hand, otherwise control would toggle twice per click
            if (event.getHand() != EquipmentSlot.HAND) {
                return;
            }
            if (!SiegeEnginesUtil.isSiegeEngine(entity, true)) {
                return;
            }
            SiegeEngine siegeEngine = SiegeEngines.activeSiegeEngines.get(entity.getUniqueId());
            if (siegeEngine == null) {
                return;
            }
            event.setCancelled(true);

            // Right clicking toggles control of the siege engine
            List<Entity> controlled = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
            if (controlled != null && controlled.contains(entity)) {
                PlayerHandler.releasePlayerSiegeEngine(player, entity);
                return;
            }
            SiegeEnginesUtil.takeControl(player, entity);
            controlled = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
            if (controlled != null && controlled.contains(entity)) {
                player.sendMessage("§eLeft click to fire, right click the Siege Engine again to release it.");
            }
            return;
        }
        if (entity.getType() == EntityType.RAVAGER || entity.getType() == EntityType.HORSE
            || entity.getType() == EntityType.DONKEY) {
            if (player.getInventory().getItemInMainHand().getType() == Material.CARVED_PUMPKIN) {
                final ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()) {
                    int customModel = item.getItemMeta().getCustomModelData();
                    SiegeEngine siegeEngine = null;
                    // Search for match in custom model data value in defined siege engines
                    for (SiegeEngine entry : SiegeEngines.definedSiegeEngines.values()) {
                        if (entry.getCustomModelID() == customModel) {
                            try {
                                siegeEngine = entry.clone();
                            } catch (CloneNotSupportedException e) {
                            }
                            break;
                        } else {
                            // if siege engine was broken during one of its firing stages
                            if (entry.getFiringModelNumbers().contains(customModel)) {
                                try {
                                    siegeEngine = entry.clone();
                                } catch (CloneNotSupportedException e) {
                                    break;
                                }
                            }
                        }
                    }
                    // If SiegeEngine found, place it
                    if (siegeEngine != null) {
                        if (!siegeEngine.isMountable()) {
                            player.sendMessage("§eThis type of Siege Engine cannot be mounted to mobs.");
                            event.setCancelled(true);
                            return;
                        }
                        if (Config.disabledWorlds.contains(entity.getWorld())) {
                            player.sendMessage("§eSiege Engines cannot be placed in this World.");
                            event.setCancelled(true);
                        }
                        if (SiegeEnginesData.fluidMaterials.contains(entity.getLocation().getBlock().getType())) {
                            player.sendMessage("§eSiege Engines cannot be placed in Fluid Blocks.");
                            event.setCancelled(true);
                        }
                        if (event.isCancelled()) {
                            return;
                        }
                        if (siegeEngine.place(player, entity.getLocation(), entity)) {
                            // If player is in creative mode, don't remove the item from their inventory
                            if (player.getGameMode() != GameMode.CREATIVE) {
                                item.setAmount(item.getAmount() - 1);
                            }
                            player.getInventory().setItemInMainHand(item);
                            player.sendMessage(
                                "§eSiege Engine mounted to the " + entity.getType().toString().toLowerCase() + "!");
                        } else {
                            player.sendMessage("§eSiege Engine cannot be placed within a " + Config.placementDensity
                                + " Block-Radius of other Siege Engines.");
                        }
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
