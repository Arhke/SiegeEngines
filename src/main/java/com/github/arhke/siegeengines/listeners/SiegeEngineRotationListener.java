package com.github.arhke.siegeengines.listeners;

import com.github.arhke.siegeengines.SiegeEngine;
import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SiegeEngineRotationListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        NamespacedKey key = new NamespacedKey(SiegeEngines.getInstance(), "siege_engines");
        if (SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(player.getUniqueId())) {
            final List<Entity> list = new ArrayList<>(SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId()));
            for (Entity ent : list) {
                if (ent != null) {
                    SiegeEngine equipment = SiegeEngines.activeSiegeEngines.get(ent.getUniqueId());
                    if (equipment == null) continue;
                    final LivingEntity living = (LivingEntity) ent;
                    if (Config.disabledWorlds.contains(ent.getWorld())) {
                        living.setHealth(0.0d);
                        PlayerHandler.siegeEngineEntityDied(ent);
                        continue;
                    }
                    if (!(ent.getLocation().getWorld().equals(player.getLocation().getWorld()))) {
                        PlayerHandler.releasePlayerSiegeEngine(player, ent);
                        continue;
                    }
                    if (ent.isDead()) {
                        PlayerHandler.releasePlayerSiegeEngine(player, ent);
                        continue;
                    }
                    if (!ent.isValid()) {
                        PlayerHandler.releasePlayerSiegeEngine(player, ent);
                        continue;
                    }
                    final double distance = player.getLocation().distance(ent.getLocation());
                    if (distance >= Config.controlDistance) {
                        PlayerHandler.releasePlayerSiegeEngine(player, ent);
                        continue;
                    }
                    if (distance <= Config.rotateDistance) {
                        Location loc = ent.getLocation();

                        if (equipment.isRotateSideways()) {
                            Location direction = player.getLocation().add(player.getLocation().getDirection().multiply(50));

                            Vector dirBetweenLocations = direction.toVector().subtract(loc.toVector());
                            if (ent.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                                Entity base = Bukkit.getEntity(UUID.fromString(Objects.requireNonNull(ent.getPersistentDataContainer().get(key, PersistentDataType.STRING))));
                                if (base == null)
                                    continue;

                                Location baseloc = base.getLocation();
                                baseloc.setDirection(dirBetweenLocations);
                                base.teleport(baseloc);
                            }
                            loc.setDirection(dirBetweenLocations);
                        }


                        ArmorStand stand = (ArmorStand) living;

                        if (equipment.isRotateUpDown()) {
                            loc.setPitch(equipment.clampPitch(player.getLocation().getPitch()));
                            if (equipment.isRotateStandHead()) {
                                stand.setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()), 0, 0));
                            }
                        }


                        living.teleport(loc);
                        equipment.ShowFireLocation(player);
                    }
                }
            }
        }
    }
}
