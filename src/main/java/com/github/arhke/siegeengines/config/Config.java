package com.github.arhke.siegeengines.config;

import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.SiegeEnginesLogger;
import com.github.arhke.siegeengines.crafting.CraftingRecipes;
import com.github.arhke.siegeengines.data.SiegeEnginesData;
import com.github.arhke.siegeengines.projectile.*;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import com.github.arhke.siegeengines.util.MessageUtil;
import net.kyori.adventure.text.Component;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class Config {

    private static FileConfiguration config;

    // Universal Options - Defaults
    public static int configVersion = 1;

    public static double placementDensity = 2.5d;
    public static int controlDistance = 10;
    public static int rotateDistance = 10;
    public static int maxSiegeEnginesControlled = 5;
    public static boolean arrowDamageToggle = false;
    public static boolean craftingRecipes = true;
    public static boolean doDebug = false;

    // Siege Explosions - Defaults
    public static boolean siegeExplosionsIgnoreFluids = true;
    public static float siegeExplosionsMaxBlastResistance = 12f;
    public static float siegeExplosionsReinforcedDeepslateBlastResistance = 12f;

    // Reinforcement - Defaults
    public static boolean reinforcementEnabled = true;
    public static float reinforcementDefaultBreaks = 500f;
    public static float reinforcementExplosionStrength = 1f;
    public static long reinforcementForgetMillis = 60 * 60 * 1000;
    public static float reinforcementThinBlockDamageMultiplier = 5f;
    public static double reinforcementDamageFalloffRadius = 3;
    public static EnumSet<Material> reinforcementNotReinforceable = EnumSet.noneOf(Material.class);


    public static HashSet<World> disabledWorlds = new HashSet<>();

    // Siege Engine Options - Defaults
    public static int trebuchetShotAmount = 1;
    public static float trebuchetVelocity = 2.25f;
    public static int trebuchetCooldownTicks = 100;
    public static float trebuchetMinAimAngle = 30f;
    public static float trebuchetMaxAimAngle = 70f;
    public static double trebuchetHealth = 8d;
    public static HashMap<ItemStack, SiegeEngineProjectile> trebuchetProjectiles = new HashMap<>();
    public static SiegeEngineProjectile trebuchetDefaultProjectile;
    public static Component trebuchetItemName = MessageUtil.legacy("&e&oTrebuchet");
    public static List<Component> trebuchetItemLore;

    public static int ballistaShotAmount = 1;
    public static float ballistaVelocity = 4.0f;
    public static int ballistaCooldownTicks = 40;
    public static float ballistaMinAimAngle = -15f;
    public static float ballistaMaxAimAngle = 45f;
    public static double ballistaHealth = 5d;
    public static HashMap<ItemStack, SiegeEngineProjectile> ballistaProjectiles = new HashMap<>();
    public static SiegeEngineProjectile ballistaDefaultProjectile;
    public static Component ballistaItemName = MessageUtil.legacy("&e&oBallista");
    public static List<Component> ballistaItemLore;

    public static int swivelCannonShotAmount = 1;
    public static float swivelCannonVelocity = 4.875f;
    public static int swivelCannonCooldownTicks = 40;
    public static float swivelCannonMinAimAngle = -30f;
    public static float swivelCannonMaxAimAngle = 40f;
    public static double swivelCannonHealth = 15d;
    public static HashMap<ItemStack, SiegeEngineProjectile> swivelCannonProjectiles = new HashMap<>();
    public static SiegeEngineProjectile swivelCannonDefaultProjectile;
    public static Component swivelCannonItemName = MessageUtil.legacy("&e&oSwivel Cannon");
    public static List<Component> swivelCannonItemLore;

    public static int breachCannonShotAmount = 1;
    public static float breachCannonVelocity = 4.25f;
    public static int breachCannonCooldownTicks = 80;
    public static float breachCannonMinAimAngle = -5f;
    public static float breachCannonMaxAimAngle = 15f;
    public static double breachCannonHealth = 25d;
    public static HashMap<ItemStack, SiegeEngineProjectile> breachCannonProjectiles = new HashMap<>();
    public static SiegeEngineProjectile breachCannonDefaultProjectile;
    public static Component breachCannonItemName = MessageUtil.legacy("&e&oBreach Cannon");
    public static List<Component> breachCannonItemLore;

    // Projectiles

    public static LinkedHashMap<String, SiegeEngineProjectile> projectileMap = new LinkedHashMap<>();

    public static void initConfigVals() {
        // init config
        config = SiegeEngines.getInstance().getConfig();

        // GENERAL

        controlDistance = config.getInt("ControlDistance");
        rotateDistance = config.getInt("RotateDistance");
        placementDensity = config.getDouble("PlacementDensity");
        arrowDamageToggle = config.getBoolean("DisableArrowDamage");
        maxSiegeEnginesControlled = config.getInt("MaxSiegeEnginesControlled");
        doDebug = config.getBoolean("Debug");
        craftingRecipes = config.getBoolean("CraftingRecipes");

        // SIEGE EXPLOSIONS

        siegeExplosionsIgnoreFluids = config.getBoolean("SiegeExplosions.IgnoreFluids", true);
        siegeExplosionsMaxBlastResistance = (float) config.getDouble("SiegeExplosions.MaxBlastResistance", 12);
        siegeExplosionsReinforcedDeepslateBlastResistance = (float) config
            .getDouble("SiegeExplosions.ReinforcedDeepslateBlastResistance", 12);

        loadReinforcementConfig();

        loadProjectilesConfig();
        loadSiegeEngineConfig();

    }

    private static void loadReinforcementConfig() {
        reinforcementEnabled = config.getBoolean("Reinforcement.Enabled", true);
        reinforcementDefaultBreaks = (float) config.getDouble("Reinforcement.DefaultBreaks", 500);
        reinforcementThinBlockDamageMultiplier = (float) config.getDouble("Reinforcement.ThinBlockDamageMultiplier", 5);
        reinforcementDamageFalloffRadius = config.getDouble("Reinforcement.DamageFalloffRadius", 3);
        reinforcementExplosionStrength = (float) config.getDouble("Reinforcement.ExplosionStrength", 1);
        reinforcementForgetMillis = Math.round(config.getDouble("Reinforcement.ForgetDamageAfter", 60) * 60 * 1000);
        final boolean reinforceFallingBlocks = config.getBoolean("Reinforcement.ReinforceFallingBlocks", false);

        // A block can't be reinforced if its name contains any of the entries, unless its name contains BLOCK
        final List<String> entries = config.getStringList("Reinforcement.NotReinforceable");
        reinforcementNotReinforceable = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isBlock()) {
                continue;
            }
            // Sand, gravel, concrete powder, anvils, ... break the vanilla way
            if (!reinforceFallingBlocks && material.hasGravity()) {
                reinforcementNotReinforceable.add(material);
                continue;
            }
            if (material.name().contains("BLOCK")) {
                continue;
            }
            for (String entry : entries) {
                if (material.name().contains(entry.toUpperCase(Locale.ROOT))) {
                    reinforcementNotReinforceable.add(material);
                    break;
                }
            }
        }
    }

    public static boolean isReinforceable(Material material) {
        return !reinforcementNotReinforceable.contains(material);
    }

    private static void loadTrebuchetValues() {
        trebuchetHealth = config.getDouble("SiegeEngines.Trebuchet.Health");
        trebuchetMinAimAngle = (float) config.getDouble("SiegeEngines.Trebuchet.MinAimAngle", 30);
        trebuchetMaxAimAngle = (float) config.getDouble("SiegeEngines.Trebuchet.MaxAimAngle", 70);
        trebuchetShotAmount = config.getInt("SiegeEngines.Trebuchet.ShotAmount");
        trebuchetVelocity = (float) config.getDouble("SiegeEngines.Trebuchet.Velocity");
        trebuchetCooldownTicks = (int) Math.round(config.getDouble("SiegeEngines.Trebuchet.Cooldown") * 20);
        trebuchetDefaultProjectile = loadDefaultProjectile("Trebuchet");
        for (String projectileName : config.getStringList("SiegeEngines.Trebuchet.Projectiles")) {
            if (projectileMap.containsKey(projectileName)) {
                trebuchetProjectiles.put(projectileMap.get(projectileName).getAmmuinitionItem(),
                    projectileMap.get(projectileName));
            }
        }
        trebuchetItemName = MessageUtil.legacy(config.getString("SiegeEngines.Trebuchet.ItemName"));
        trebuchetItemLore = MessageUtil.legacy(config.getStringList("SiegeEngines.Trebuchet.Lore"));
    }

    private static void loadBallistaValues() {
        ballistaHealth = config.getDouble("SiegeEngines.Ballista.Health");
        ballistaMinAimAngle = (float) config.getDouble("SiegeEngines.Ballista.MinAimAngle", -15);
        ballistaMaxAimAngle = (float) config.getDouble("SiegeEngines.Ballista.MaxAimAngle", 45);
        ballistaShotAmount = config.getInt("SiegeEngines.Ballista.ShotAmount");
        ballistaVelocity = (float) config.getDouble("SiegeEngines.Ballista.Velocity");
        ballistaCooldownTicks = (int) Math.round(config.getDouble("SiegeEngines.Ballista.Cooldown") * 20);
        ballistaDefaultProjectile = loadDefaultProjectile("Ballista");
        for (String projectileName : config.getStringList("SiegeEngines.Ballista.Projectiles")) {
            if (projectileMap.containsKey(projectileName)) {
                ballistaProjectiles.put(projectileMap.get(projectileName).getAmmuinitionItem(),
                    projectileMap.get(projectileName));
            }
        }
        ballistaItemName = MessageUtil.legacy(config.getString("SiegeEngines.Ballista.ItemName"));
        ballistaItemLore = MessageUtil.legacy(config.getStringList("SiegeEngines.Ballista.Lore"));
    }

    private static void loadSwivelCannonValues() {
        swivelCannonHealth = config.getDouble("SiegeEngines.SwivelCannon.Health");
        swivelCannonMinAimAngle = (float) config.getDouble("SiegeEngines.SwivelCannon.MinAimAngle", -30);
        swivelCannonMaxAimAngle = (float) config.getDouble("SiegeEngines.SwivelCannon.MaxAimAngle", 40);
        swivelCannonShotAmount = config.getInt("SiegeEngines.SwivelCannon.ShotAmount");
        swivelCannonVelocity = (float) config.getDouble("SiegeEngines.SwivelCannon.Velocity");
        swivelCannonCooldownTicks = (int) Math.round(config.getDouble("SiegeEngines.SwivelCannon.Cooldown") * 20);
        swivelCannonDefaultProjectile = loadDefaultProjectile("SwivelCannon");
        for (String projectileName : config.getStringList("SiegeEngines.SwivelCannon.Projectiles")) {
            if (projectileMap.containsKey(projectileName)) {
                swivelCannonProjectiles.put(projectileMap.get(projectileName).getAmmuinitionItem(),
                    projectileMap.get(projectileName));
            }
        }
        swivelCannonItemName = MessageUtil.legacy(config.getString("SiegeEngines.SwivelCannon.ItemName"));
        swivelCannonItemLore = MessageUtil.legacy(config.getStringList("SiegeEngines.SwivelCannon.Lore"));
    }

    private static void loadBreachCannonValues() {
        breachCannonHealth = config.getDouble("SiegeEngines.BreachCannon.Health");
        breachCannonMinAimAngle = (float) config.getDouble("SiegeEngines.BreachCannon.MinAimAngle", -5);
        breachCannonMaxAimAngle = (float) config.getDouble("SiegeEngines.BreachCannon.MaxAimAngle", 15);
        breachCannonShotAmount = config.getInt("SiegeEngines.BreachCannon.ShotAmount");
        breachCannonVelocity = (float) config.getDouble("SiegeEngines.BreachCannon.Velocity");
        breachCannonCooldownTicks = (int) Math.round(config.getDouble("SiegeEngines.BreachCannon.Cooldown") * 20);
        breachCannonDefaultProjectile = loadDefaultProjectile("BreachCannon");
        for (String projectileName : config.getStringList("SiegeEngines.BreachCannon.Projectiles")) {
            if (projectileMap.containsKey(projectileName)) {
                breachCannonProjectiles.put(projectileMap.get(projectileName).getAmmuinitionItem(),
                    projectileMap.get(projectileName));
            }
        }
        breachCannonItemName = MessageUtil.legacy(config.getString("SiegeEngines.BreachCannon.ItemName"));
        breachCannonItemLore = MessageUtil.legacy(config.getStringList("SiegeEngines.BreachCannon.Lore"));
    }

    private static SiegeEngineProjectile loadDefaultProjectile(String siegeEngine) {
        final String projectileName = config.getString("SiegeEngines." + siegeEngine + ".DefaultProjectile");
        final SiegeEngineProjectile projectile = projectileMap.get(projectileName);
        if (projectile == null) {
            SiegeEnginesLogger.warn("Default projectile " + projectileName + " for " + siegeEngine
                + " could not be found, it will only fire held projectiles!");
        }
        return projectile;
    }

    /**
     * Resolves a sound from either a namespaced key (e.g. {@code entity.generic.explode}) or a legacy
     * enum-style name (e.g. {@code ENTITY_GENERIC_EXPLODE}).
     */
    private static Sound parseSound(String name) {
        final Registry<Sound> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);
        final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        final Sound sound = key != null ? registry.get(key) : null;
        if (sound != null)
            return sound;

        return registry.stream()
            .filter(s -> registry.getKeyOrThrow(s).getKey().replace('.', '_').equalsIgnoreCase(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown sound: " + name));
    }

    private static void loadSiegeEngineConfig() {

        loadTrebuchetValues();
        loadBallistaValues();
        loadSwivelCannonValues();
        loadBreachCannonValues();

        try {
            for (String worldName : config.getStringList("DisabledWorlds")) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    disabledWorlds.add(world);
                }
            }
            SiegeEnginesLogger.info("Disabled in worlds: " + config.getStringList("DisabledWorlds"));
        } catch (Exception e) {
            SiegeEnginesLogger.warn("Missing Disabled Worlds-List");
        }
    }

    private static void loadProjectilesConfig() {
        projectileMap.clear();

        for (String projectileName : config.getConfigurationSection("Projectiles").getKeys(false)) {

            ProjectileType projectileType;
            try {
                projectileType = ProjectileType
                    .valueOf(config.getString("Projectiles." + projectileName + ".ProjectileType"));
            } catch (Exception e) {
                SiegeEnginesLogger
                    .warn("Could not load projectile - " + projectileName + " likely due to a config error!");
                continue;
            }

            try {
                switch (projectileType) {
                    case EXPLOSIVE:
                        ExplosiveProjectile explosiveProjectile = new ExplosiveProjectile(loadAmmoItem(projectileName));
                        explosiveProjectile.explodePower = (float) config
                            .getDouble("Projectiles." + projectileName + ".ExplodePower");
                        explosiveProjectile.explodePowerVariation = (float) config
                            .getDouble("Projectiles." + projectileName + ".ExplodePowerVariation", 0.15);
                        explosiveProjectile.strength = (float) config
                            .getDouble("Projectiles." + projectileName + ".Strength", 2);
                        explosiveProjectile.inaccuracy = (float) config
                            .getDouble("Projectiles." + projectileName + ".Inaccuracy");
                        explosiveProjectile.projectilesCount = config
                            .getInt("Projectiles." + projectileName + ".ProjectileCount");
                        explosiveProjectile.soundType = parseSound(config.getString("Projectiles." + projectileName + ".FireSound"));
                        explosiveProjectile.velocityFactor = (float) config
                            .getDouble("Projectiles." + projectileName + ".VelocityFactor");
                        projectileMap.put(projectileName, explosiveProjectile);
                        break;
                    case ENTITY:
                        EntityProjectile entityProjectile = new EntityProjectile(loadAmmoItem(projectileName));
                        entityProjectile.inaccuracy = (float) config
                            .getDouble("Projectiles." + projectileName + ".Inaccuracy");
                        entityProjectile.projectileCount = config.getInt("Projectiles." + projectileName + ".ProjectileCount");
                        entityProjectile.entityType = EntityType
                            .valueOf(config.getString("Projectiles." + projectileName + ".EntityType"));
                        entityProjectile.soundType = parseSound(config.getString("Projectiles." + projectileName + ".FireSound"));
                        entityProjectile.velocityFactor = (float) config
                            .getDouble("Projectiles." + projectileName + ".VelocityFactor");
                        entityProjectile.strength = (float) config.getDouble("Projectiles." + projectileName + ".Strength", 1);
                        if (entityProjectile.entityType.equals(EntityType.ARROW)
                            || entityProjectile.entityType.equals(EntityType.SPECTRAL_ARROW)) {
                            entityProjectile.arrowDamageFactor = (float) config
                                .getDouble("Projectiles." + projectileName + ".ArrowDamageFactor");
                        }
                        projectileMap.put(projectileName, entityProjectile);
                        break;
                    case FIREWORK:
                        FireworkProjectile fireworkProjectile = new FireworkProjectile(SiegeEnginesUtil.DEFAULT_ROCKET);
                        fireworkProjectile.inaccuracy = (float) config
                            .getDouble("Projectiles." + projectileName + ".Inaccuracy");
                        fireworkProjectile.projectileCount = config.getInt("Projectiles." + projectileName + ".ProjectileCount");
                        fireworkProjectile.delayTime = config.getInt("Projectiles." + projectileName + ".ProjectileCount");
                        fireworkProjectile.delayedFire = fireworkProjectile.delayTime > 0;
                        fireworkProjectile.velocityFactor = (float) config
                            .getDouble("Projectiles." + projectileName + ".VelocityFactor");
                        projectileMap.put(projectileName, fireworkProjectile);
                        break;
                    case POTION:
                        PotionProjectile potionProjectile = new PotionProjectile(loadAmmoItem(projectileName));
                        potionProjectile.inaccuracy = (float) config
                            .getDouble("Projectiles." + projectileName + ".Inaccuracy");
                        potionProjectile.projectileCount = config.getInt("Projectiles." + projectileName + ".ProjectileCount");
                        potionProjectile.delayTime = config.getInt("Projectiles." + projectileName + ".ProjectileCount");
                        potionProjectile.delayedFire = potionProjectile.delayTime > 0;
                        potionProjectile.velocityFactor = (float) config
                            .getDouble("Projectiles." + projectileName + ".VelocityFactor");
                        projectileMap.put(projectileName, potionProjectile);
                        break;
                    default:
                        continue;
                }
            } catch (Exception e) {
                SiegeEnginesLogger.severe(e.toString());
            }
        }
    }

    /**
     * Creates the item a projectile is fired with. AmmoName and AmmoLore are optional, the held item only has to
     * match the ones that are set.
     */
    private static ItemStack loadAmmoItem(String projectileName) {
        final String path = "Projectiles." + projectileName + ".";
        final ItemStack ammoItem = new ItemStack(Material.getMaterial(config.getString(path + "AmmoItem")));
        final ItemMeta meta = ammoItem.getItemMeta();
        if (config.isString(path + "AmmoName")) {
            meta.displayName(MessageUtil.legacy(config.getString(path + "AmmoName")));
        }
        if (config.isList(path + "AmmoLore")) {
            meta.lore(MessageUtil.legacy(config.getStringList(path + "AmmoLore")));
        }
        ammoItem.setItemMeta(meta);
        return ammoItem;
    }

    public static void reload() {
        // put whatever you want here to run config-wise when plugin reloads
        SiegeEngines.getInstance().reloadConfig();
        SiegeEngines.getInstance().saveDefaultConfig();
        initConfigVals();

        // reinstantiate data class
        SiegeEnginesData.init();

        // load crafting recipes if enabled in config
        if (Config.craftingRecipes) {
            if (!CraftingRecipes.areLoaded()) {
                CraftingRecipes.loadCraftingRecipes();
            }
        } else {
            if (CraftingRecipes.areLoaded()) {
                CraftingRecipes.unloadCraftingRecipes();
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

}
