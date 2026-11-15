package com.github.arhke.siegeengines.listeners;

import com.github.arhke.siegeengines.SiegeEngine;
import com.github.arhke.siegeengines.SiegeEngines;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SiegeEngineFireListener implements Listener {

    // Player UUID, server tick they last fired on
    private final HashMap<UUID, Integer> lastFireTick = new HashMap<>();

    @EventHandler
    public void onSiegeEngineFire(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        fire(event.getPlayer());
    }

    // Left clicking any entity fires too, and a siege engine you command is not damaged by it
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSiegeEngineAttack(PrePlayerAttackEntityEvent event) {
        final Player player = event.getPlayer();
        List<Entity> siegeEngineEntities = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
        if (siegeEngineEntities == null || siegeEngineEntities.isEmpty()) {
            return;
        }
        if (siegeEngineEntities.contains(event.getAttacked())) {
            event.setCancelled(true);
        }
        fire(player);
    }

    /**
     * Fires all siege engines commanded by the player.
     */
    private void fire(Player player) {
        List<Entity> siegeEngineEntities = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
        if (siegeEngineEntities == null) {
            return;
        }
        // One click can raise both an attack and an interact event, only fire once for it
        final int tick = Bukkit.getCurrentTick();
        final Integer lastTick = lastFireTick.put(player.getUniqueId(), tick);
        if (lastTick != null && lastTick == tick) {
            return;
        }
        for (Entity siegeEngineEntity : siegeEngineEntities) {
            if (SiegeEngines.activeSiegeEngines.containsKey(siegeEngineEntity.getUniqueId())) {
                SiegeEngine siegeEngine = SiegeEngines.activeSiegeEngines.get(siegeEngineEntity.getUniqueId());
                if (siegeEngine != null && siegeEngine.getEnabled() && !(siegeEngineEntity.isDead())) {
                    siegeEngine.Fire(player, 10f);
                }
            }
        }
    }

}
