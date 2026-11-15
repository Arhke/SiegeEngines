package com.github.arhke.siegeengines.util;

import com.github.arhke.siegeengines.SiegeEngine;
import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.SiegeEnginesLogger;
import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.listeners.PlayerHandler;
import io.papermc.paper.datacomponent.DataComponentTypes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.EulerAngle;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SiegeEnginesUtil {

    public static final ItemStack DEFAULT_ROCKET = new ItemStack(Material.FIREWORK_ROCKET);

    /**
     * Stops a siege engine item from being worn, carved pumpkins can be worn as helmets otherwise.
     * Armor stands still show it on their head.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack makeUnwearable(ItemStack item) {
        item.unsetData(DataComponentTypes.EQUIPPABLE);
        return item;
    }

    /**
     * @return true if the item is a siege engine, including one picked up in the middle of firing
     */
    public static boolean isSiegeEngineItem(ItemStack item) {
        if (item == null || item.getType() != Material.CARVED_PUMPKIN || !item.hasItemMeta()
            || !item.getItemMeta().hasCustomModelData()) {
            return false;
        }
        final int model = item.getItemMeta().getCustomModelData();
        for (SiegeEngine siegeEngine : SiegeEngines.definedSiegeEngines.values()) {
            if (siegeEngine.getReadyModelNumber() == model || siegeEngine.getFiringModelNumbers().contains(model)) {
                return true;
            }
        }
        return false;
    }

    public static void UpdateEntityIdModel(Entity ent, int modelNumber, String WorldName) {
        if (ent instanceof LivingEntity liv) {
            final EntityEquipment equipment = liv.getEquipment();
            if (equipment == null)
                return;

            final ItemStack helmet = equipment.getHelmet();
            if (helmet != null) {
                ItemMeta meta = helmet.getItemMeta();
                meta.setCustomModelData(modelNumber);
                helmet.setItemMeta(meta);
                equipment.setHelmet(helmet);
            }
        }
    }

    public static FixedMetadataValue addMetaDataValue(Object value) {
        return new FixedMetadataValue(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("SiegeEngines")), value);
    }

    public static String convertTime(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        time -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        time -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        time -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);
        return String.format("§e" + minutes + " Minutes " + seconds + " Seconds§f");
    }

    public static SiegeEngine createCloneFromCustomModelData(Integer ModelId) {
        for (SiegeEngine siegeEngine : SiegeEngines.definedSiegeEngines.values()) {
            // Also match siege engines left mid-animation or resting on their loaded model
            if (siegeEngine.getReadyModelNumber() == ModelId || siegeEngine.getLoadedModelNumber() == ModelId
                || siegeEngine.getFiringModelNumbers().contains(ModelId)) {
                try {
                    return siegeEngine.clone();
                } catch (CloneNotSupportedException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isSiegeEngine(Entity entity, boolean add) {
        LivingEntity living = (LivingEntity) entity;

        final EntityEquipment equipment = living.getEquipment();
        if (equipment == null)
            return false;

        if (equipment.getHelmet() != null && equipment.getHelmet().getType() == Material.CARVED_PUMPKIN) {
            if (equipment.getHelmet() == null || equipment.getHelmet().getItemMeta() == null) {
                return false;
            }

            ArmorStand stand = (ArmorStand) entity;
            SiegeEngine siegeEngines;
            stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
            stand.setBasePlate(true);

            SiegeEnginesLogger.debug("ACTIVE ENGINES : " + SiegeEngines.activeSiegeEngines);
            if (SiegeEngines.activeSiegeEngines.containsKey(entity.getUniqueId())) {
                siegeEngines = SiegeEngines.activeSiegeEngines.get(entity.getUniqueId());
                if (siegeEngines == null || !siegeEngines.getEnabled()) {
                    return false;
                }
            } else {
                siegeEngines = SiegeEnginesUtil.createCloneFromCustomModelData(
                    living.getEquipment().getHelmet().getItemMeta().getCustomModelData());
                if (siegeEngines == null || !siegeEngines.getEnabled()) {
                    return false;
                }
                siegeEngines.setEntity(entity);
                siegeEngines.setEntityId(entity.getUniqueId());
            }
            stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
            stand.setBasePlate(true);
            // player.sendMessage("§eNow controlling the equipment.");
            if (add) SiegeEngines.activeSiegeEngines.put(entity.getUniqueId(), siegeEngines);
            return true;
        }
        return false;
    }

    public static void takeControl(Entity player, Entity entity) {
        LivingEntity living = (LivingEntity) entity;
        if (SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(player.getUniqueId())) {
            List<Entity> entities = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
            if (entities.contains(entity)) {
                if (player instanceof Player) {
                    player.sendMessage("§eYou are already commanding this Siege Engine!");
                }
                return;
            }
        }
        // Can only have one pilot
        final HashSet<UUID> keys = new HashSet<>(SiegeEngines.siegeEngineEntitiesPerPlayer.keySet());
        int numPilots = 0;
        for (UUID uuid : keys) {
            if (uuid.equals(player.getUniqueId())) {
                continue;
            }
            if (SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(uuid)) {
                if (SiegeEngines.siegeEngineEntitiesPerPlayer.get(uuid).contains(entity)) {
                    numPilots++;
                }
            }
        }
        if (numPilots > 1) {
            if (player instanceof Player)
                player.sendMessage("§eOnly one Player may command this Siege Engine!");
            return;
        }

        if (SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(player.getUniqueId())) {
            SiegeEnginesLogger.debug("CURRENT ENGINES : " + SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId()));
            if (SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId())
                .size() > Config.maxSiegeEnginesControlled) {
                if (player instanceof Player) {
                    player.sendMessage("§eYou are commanding too many Siege Engines!");
                    PlayerHandler.releasePlayerSiegeEngine(((Player) player), entity);
                }
                return;
            }
        }
        final EntityEquipment equipment = living.getEquipment();
        if (equipment == null)
            return;

        if (equipment.getHelmet() != null && equipment.getHelmet().getType() == Material.CARVED_PUMPKIN) {
            if (equipment.getHelmet() == null || equipment.getHelmet().getItemMeta() == null) {
                return;
            }

            ArmorStand stand = (ArmorStand) entity;
            SiegeEngine equip;
            stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
            stand.setBasePlate(true);

            if (SiegeEngines.activeSiegeEngines.containsKey(entity.getUniqueId())) {
                equip = SiegeEngines.activeSiegeEngines.get(entity.getUniqueId());
                if (equip == null || !equip.getEnabled()) {
                    return;
                }
            } else {
                equip = SiegeEnginesUtil.createCloneFromCustomModelData(
                    living.getEquipment().getHelmet().getItemMeta().getCustomModelData());
                if (equip == null || !equip.getEnabled()) {
                    return;
                }
                equip.setEntity(entity);
                equip.setEntityId(entity.getUniqueId());
            }
            stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
            stand.setBasePlate(true);
            if (SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(player.getUniqueId())) {
                List<Entity> entities = SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId());
                entities.add(entity);
                SiegeEngines.siegeEngineEntitiesPerPlayer.remove(player.getUniqueId());
                SiegeEngines.siegeEngineEntitiesPerPlayer.put(player.getUniqueId(), entities);
            } else {
                List<Entity> newList = new ArrayList<>();
                newList.add(entity);
                SiegeEngines.siegeEngineEntitiesPerPlayer.remove(player.getUniqueId());
                SiegeEngines.siegeEngineEntitiesPerPlayer.put(player.getUniqueId(), newList);
            }
            SiegeEnginesLogger.debug("NEW ENGINES : " + SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId()));
            SiegeEngines.activeSiegeEngines.put(entity.getUniqueId(), equip);
            // player.sendMessage("§eNow controlling the equipment.");
            if (player instanceof Player) {
                player.sendMessage("§eYou are now commanding a total of "
                    + SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId()).size()
                    + " Siege Engines!");
            }
        }
    }

    public static void aimUp(Entity player, float amount) {

        for (Entity ent : SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId())) {
            if (ent.isDead()) {
                continue;
            }
            doAimUp(ent, amount, player);
        }
    }

    public static void aimDown(Entity player, float amount) {

        for (Entity ent : SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId())) {
            if (ent.isDead()) {
                continue;
            }
            doAimDown(ent, amount, player);
        }
    }

    public static void doAimUp(Entity ent, float amount, Entity player) {
        Location loc = ent.getLocation();
        ArmorStand stand = (ArmorStand) ent;
        // player.sendMessage(String.format("" + loc.getPitch()));
        if (loc.getPitch() == -85 || loc.getPitch() - amount < -85) {
            return;
        }
        loc.setPitch(loc.getPitch() - amount);
        SiegeEngine equipment = SiegeEngines.activeSiegeEngines.get(ent.getUniqueId());
        if (equipment != null) {
            if (player instanceof Player)
                equipment.ShowFireLocation(player);
            if (equipment.isRotateStandHead()) {
                stand.setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()), 0, 0));
            }
            ent.teleport(loc);
        }
    }

    public static void doAimDown(Entity ent, float amount, Entity player) {
        Location loc = ent.getLocation();
        ArmorStand stand = (ArmorStand) ent;
        // player.sendMessage(String.format("" + loc.getPitch()));
        if (loc.getPitch() == 85 || loc.getPitch() - amount < 65) {
            return;
        }
        loc.setPitch(loc.getPitch() - amount);
        SiegeEngine equipment = SiegeEngines.activeSiegeEngines.get(ent.getUniqueId());
        if (equipment != null) {
            if (player instanceof Player)
                equipment.ShowFireLocation(player);
            if (equipment.isRotateStandHead()) {
                stand.setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()), 0, 0));
            }
            ent.teleport(loc);
        }
    }

    public static void firstShotDoAimDown(Entity ent, float amount, Entity player) {
        Location loc = ent.getLocation();
        ArmorStand stand = (ArmorStand) ent;
        // player.sendMessage(String.format("" + loc.getPitch()));
        if (loc.getPitch() == 85 || loc.getPitch() + amount > 85) {
            return;
        }
        SiegeEngine equipment = SiegeEngines.activeSiegeEngines.get(ent.getUniqueId());
        if (equipment != null) {
            if (player instanceof Player) {
                equipment.ShowFireLocation(player);
            }
            if (equipment.isRotateStandHead()) {
                stand.setHeadPose(new EulerAngle(Math.toRadians(loc.getPitch()), 0, 0));

            }
        }
        loc.setPitch(loc.getPitch() + amount);

        ent.teleport(loc);
    }

    public static void saveSiegeEngine(Player player, Block block) {
        List<String> Ids = new ArrayList<>();
        if (!SiegeEngines.siegeEngineEntitiesPerPlayer.containsKey(player.getUniqueId())) {
            return;
        }
        for (Entity ent : SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId())) {
            if (ent.isDead()) {
                continue;
            }
            Ids.add(ent.getUniqueId().toString());
        }
        for (Entity ent : SiegeEngines.siegeEngineEntitiesPerPlayer.get(player.getUniqueId())) {
            if (ent.isDead()) {
                continue;
            }
            SiegeEnginesUtil.doAimUp(ent, 0, player);
        }
    }
}
