package com.github.arhke.siegeengines.data;

import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.projectile.ExplosiveProjectile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;

public class SiegeEnginesData {
    public static HashMap<UUID, ExplosiveProjectile> projectiles;
    public static EnumSet<Material> fluidMaterials;
    public static NamespacedKey key;
    // How many reinforcement breaks an explosion from a siege engine does, read by Reinforcement
    public static NamespacedKey explosionStrengthKey;
    // How squarely the projectile hit, which scales its reinforcement damage
    public static NamespacedKey explosionImpactFactorKey;
    // The direction into the surface the projectile hit, which decides which blocks are thin
    public static NamespacedKey explosionDirectionKey;
    public static ArrayList<ItemStack> items;

    public static void init() {
        projectiles = new HashMap<>();
        fluidMaterials = EnumSet.of(Material.WATER, Material.LAVA, Material.BUBBLE_COLUMN,
            Material.SEAGRASS, Material.TALL_SEAGRASS, Material.KELP, Material.KELP_PLANT, Material.SEA_PICKLE);
        key = new NamespacedKey(SiegeEngines.getInstance(), "siege_engines");
        explosionStrengthKey = new NamespacedKey(SiegeEngines.getInstance(), "explosion_strength");
        explosionImpactFactorKey = new NamespacedKey(SiegeEngines.getInstance(), "explosion_impact_factor");
        explosionDirectionKey = new NamespacedKey(SiegeEngines.getInstance(), "explosion_direction");
        items = new ArrayList<>();
    }
}
