package com.github.alathra.siegeengines.api;

import com.github.alathra.siegeengines.SiegeEngine;
import com.github.alathra.siegeengines.SiegeEngines;
import com.github.alathra.siegeengines.config.Config;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SiegeEnginesAPI {

    public static SiegeEngine getSiegeEngineFromEntity(Entity entity) {
        return SiegeEngines.activeSiegeEngines.getOrDefault(entity.getUniqueId(), null);
    }

    public static ItemStack getTrebuchetItem() {
        ItemStack trebuchetItem = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = trebuchetItem.getItemMeta();
        meta.setCustomModelData(122);
        meta.displayName(Config.trebuchetItemName);
        meta.lore(Config.trebuchetItemLore);
        trebuchetItem.setItemMeta(meta);
        return trebuchetItem;
    }

    public static ItemStack getBallistaItem() {
        ItemStack ballistaItem = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = ballistaItem.getItemMeta();
        meta.setCustomModelData(145);
        meta.displayName(Config.ballistaItemName);
        meta.lore(Config.ballistaItemLore);
        ballistaItem.setItemMeta(meta);
        return ballistaItem;
    }

    public static ItemStack getSwivelCannonItem() {
        ItemStack swivelCannonItem = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = swivelCannonItem.getItemMeta();
        meta.setCustomModelData(141);
        meta.displayName(Config.swivelCannonItemName);
        meta.lore(Config.swivelCannonItemLore);
        swivelCannonItem.setItemMeta(meta);
        return swivelCannonItem;
    }

    public static ItemStack getBreachCannonItem() {
        ItemStack breachCannonItem = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = breachCannonItem.getItemMeta();
        meta.setCustomModelData(142);
        meta.displayName(Config.breachCannonItemName);
        meta.lore(Config.breachCannonItemLore);
        breachCannonItem.setItemMeta(meta);
        return breachCannonItem;
    }
}