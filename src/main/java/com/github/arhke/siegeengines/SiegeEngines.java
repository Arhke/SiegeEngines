package com.github.arhke.siegeengines;

import com.github.arhke.siegeengines.command.CommandHandler;
import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.data.SiegeEnginesData;
import com.github.arhke.siegeengines.listeners.*;
import com.github.arhke.siegeengines.reinforcement.ReinforcementData;
import com.github.arhke.siegeengines.reinforcement.ReinforcementEffects;
import com.github.arhke.siegeengines.reinforcement.ReinforcementListener;
import com.github.arhke.siegeengines.reinforcement.ReinforcementView;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValueAdapter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class SiegeEngines extends JavaPlugin {

    private CommandHandler commandHandler;

    public static SiegeEngines instance;
    public static MetadataValueAdapter metadata;
    public static final Random random = new Random();

    // model id, defined siege engine types
    public static final HashMap<Integer, SiegeEngine> definedSiegeEngines = new HashMap<>();
    // SiegeEngine entity, SiegeEngine object
    public static final HashMap<UUID, SiegeEngine> activeSiegeEngines = new HashMap<>();
    // Player UUID, SiegeEngine entity
    public static final HashMap<UUID, List<Entity>> siegeEngineEntitiesPerPlayer = new HashMap<>();

    public static SiegeEngines getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        commandHandler = new CommandHandler(this);
        commandHandler.onLoad();
        instance = this;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Config.reload();
        activeSiegeEngines.clear();
        siegeEngineEntitiesPerPlayer.clear();
        definedSiegeEngines.clear();
        getServer().getPluginManager().registerEvents(new PlayerHandler(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineDamagedListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineDeathListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineFireListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineInteractListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEnginePlaceListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineRotationListener(), this);
        getServer().getPluginManager().registerEvents(new SiegeEngineWearListener(), this);
        getServer().getPluginManager().registerEvents(new ReinforcementListener(), this);
        // Forget old damage every minute, and update holograms for players in view mode twice a second
        getServer().getScheduler().runTaskTimer(this, ReinforcementData::removeStale, 1200, 1200);
        getServer().getScheduler().runTaskTimer(this, ReinforcementView::update, 10, 10);
        getServer().getScheduler().runTaskTimer(this, ReinforcementEffects::tick,
            ReinforcementEffects.TICK_INTERVAL, ReinforcementEffects.TICK_INTERVAL);
        SiegeEnginesData.items.clear();
        SiegeEnginesData.items.add(new ItemStack(Material.ARMOR_STAND));
        addDefaults();
        if (Config.doDebug) {
            for (SiegeEngine i : definedSiegeEngines.values()) {
                SiegeEnginesLogger.info("Enabled Weapon : " + i.getEngineName());
                for (ItemStack proj : i.getProjectiles().keySet()) {
                    SiegeEnginesLogger.info("Weapon Projectile ItemStacks : " + proj);
                }
            }
        }
        commandHandler.onEnable();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDisable() {
        commandHandler.onDisable();
        ReinforcementView.clear();
        ReinforcementData.clear();
        for (SiegeEngine siegeEngine : activeSiegeEngines.values()) {
            if (siegeEngine.getEntity() != null) {
                ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
                ItemMeta meta = item.getItemMeta();
                meta.setCustomModelData(siegeEngine.getReadyModelNumber());
                switch (siegeEngine.getType()) {
                    case TREBUCHET:
                        meta.displayName(Config.trebuchetItemName);
                        meta.lore(Config.trebuchetItemLore);
                        break;
                    case BALLISTA:
                        meta.displayName(Config.ballistaItemName);
                        meta.lore(Config.ballistaItemLore);
                        break;
                    case SWIVEL_CANNON:
                        meta.displayName(Config.swivelCannonItemName);
                        meta.lore(Config.swivelCannonItemLore);
                        break;
                    case BREACH_CANNON:
                        meta.displayName(Config.breachCannonItemName);
                        meta.lore(Config.breachCannonItemLore);
                        break;
                }
                item.setItemMeta(meta);

                final EntityEquipment entityEquipment = ((LivingEntity) siegeEngine.getEntity()).getEquipment();
                if (entityEquipment == null)
                    continue;
                entityEquipment.setHelmet(SiegeEnginesUtil.makeUnwearable(item));
            }
        }
    }

    public static void addDefaults() {

        // Trebuchet
        SiegeEngine trebuchet = new SiegeEngine("Trebuchet", Config.trebuchetProjectiles, 122);
        // config options
        trebuchet.setType(SiegeEngineType.TREBUCHET);
        trebuchet.setItemName(Config.trebuchetItemName);
        trebuchet.setItemLore(Config.trebuchetItemLore);
        trebuchet.setShotAmount(Config.trebuchetShotAmount);
        trebuchet.setVelocity(Config.trebuchetVelocity);
        trebuchet.setCooldownTicks(Config.trebuchetCooldownTicks);
        trebuchet.setProjectiles(Config.trebuchetProjectiles);
        trebuchet.setDefaultProjectile(Config.trebuchetDefaultProjectile);
        trebuchet.setHealth(Config.trebuchetHealth);
        trebuchet.setMinAimAngle(Config.trebuchetMinAimAngle);
        trebuchet.setMaxAimAngle(Config.trebuchetMaxAimAngle);
        trebuchet.setXOffset(3);
        trebuchet.setYOffset(3);
        trebuchet.setPlacementOffsetY(0.0);
        trebuchet.setRotateStandHead(false);
        trebuchet.setRotateSideways(true);
        trebuchet.setReadyModelNumber(122);
        trebuchet.setModelNumberToFireAt(135);
        trebuchet.setMillisecondsBetweenFiringStages(2);
        trebuchet.setFiringModelNumbers(new ArrayList<>(
            Arrays.asList(123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139)));
        trebuchet.setCycleThroughModelsWhileFiring(true);
        definedSiegeEngines.put(trebuchet.getReadyModelNumber(), trebuchet);

        // Ballista
        SiegeEngine ballista = new SiegeEngine("Ballista", Config.ballistaProjectiles, 146);
        // config options
        ballista.setType(SiegeEngineType.BALLISTA);
        ballista.setItemName(Config.ballistaItemName);
        ballista.setItemLore(Config.ballistaItemLore);
        ballista.setShotAmount(Config.ballistaShotAmount);
        ballista.setVelocity(Config.ballistaVelocity);
        ballista.setCooldownTicks(Config.ballistaCooldownTicks);
        ballista.setProjectiles(Config.ballistaProjectiles);
        ballista.setDefaultProjectile(Config.ballistaDefaultProjectile);
        ballista.setHealth(Config.ballistaHealth);
        ballista.setMinAimAngle(Config.ballistaMinAimAngle);
        ballista.setMaxAimAngle(Config.ballistaMaxAimAngle);
        ballista.setXOffset(2);
        ballista.setYOffset(1);
        ballista.setPlacementOffsetY(-1.25);
        ballista.setRotateStandHead(true);
        ballista.setRotateSideways(true);
        ballista.setReadyModelNumber(146);
        ballista.setModelNumberToFireAt(146);
        // Rests drawn with a bolt, the firing models release it and reloading draws it back
        ballista.setLoadedModelNumber(143);
        ballista.setFiringModelNumbers(new ArrayList<>(Arrays.asList(143, 144, 145, 146)));
        ballista.setCycleThroughModelsWhileFiring(true);
        definedSiegeEngines.put(ballista.getReadyModelNumber(), ballista);

        // Siege Cannon
        SiegeEngine swivelCannon = new SiegeEngine("Swivel Cannon", Config.swivelCannonProjectiles, 141);
        // config options
        swivelCannon.setType(SiegeEngineType.SWIVEL_CANNON);
        swivelCannon.setItemName(Config.swivelCannonItemName);
        swivelCannon.setItemLore(Config.swivelCannonItemLore);
        swivelCannon.setShotAmount(Config.swivelCannonShotAmount);
        swivelCannon.setVelocity(Config.swivelCannonVelocity);
        swivelCannon.setCooldownTicks(Config.swivelCannonCooldownTicks);
        swivelCannon.setProjectiles(Config.swivelCannonProjectiles);
        swivelCannon.setDefaultProjectile(Config.swivelCannonDefaultProjectile);
        swivelCannon.setHealth(Config.swivelCannonHealth);
        swivelCannon.setMinAimAngle(Config.swivelCannonMinAimAngle);
        swivelCannon.setMaxAimAngle(Config.swivelCannonMaxAimAngle);
        swivelCannon.setXOffset(3);
        swivelCannon.setPlacementOffsetY(-1);
        swivelCannon.setReadyModelNumber(141);
        swivelCannon.setModelNumberToFireAt(141);
        swivelCannon.setFiringModelNumbers(new ArrayList<>());
        swivelCannon.setRotateStandHead(true);
        swivelCannon.setRotateSideways(true);
        definedSiegeEngines.put(swivelCannon.getReadyModelNumber(), swivelCannon);

        // Naval Cannon
        SiegeEngine breachCannon = new SiegeEngine("Breach Cannon", Config.breachCannonProjectiles, 142);
        // config options
        breachCannon.setType(SiegeEngineType.BREACH_CANNON);
        breachCannon.setItemName(Config.breachCannonItemName);
        breachCannon.setItemLore(Config.breachCannonItemLore);
        breachCannon.setShotAmount(Config.breachCannonShotAmount);
        breachCannon.setVelocity(Config.breachCannonVelocity);
        breachCannon.setCooldownTicks(Config.breachCannonCooldownTicks);
        breachCannon.setProjectiles(Config.breachCannonProjectiles);
        breachCannon.setDefaultProjectile(Config.breachCannonDefaultProjectile);
        breachCannon.setHealth(Config.breachCannonHealth);
        breachCannon.setMinAimAngle(Config.breachCannonMinAimAngle);
        breachCannon.setMaxAimAngle(Config.breachCannonMaxAimAngle);
        breachCannon.setXOffset(3);
        breachCannon.setPlacementOffsetY(-1);
        breachCannon.setReadyModelNumber(142);
        breachCannon.setModelNumberToFireAt(142);
        breachCannon.setFiringModelNumbers(new ArrayList<>());
        breachCannon.setRotateStandHead(true);
        breachCannon.setRotateSideways(false);
        definedSiegeEngines.put(breachCannon.getReadyModelNumber(), breachCannon);

    }

}
