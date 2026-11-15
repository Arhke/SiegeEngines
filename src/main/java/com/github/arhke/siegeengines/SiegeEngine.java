package com.github.arhke.siegeengines;

import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.projectile.EntityProjectile;
import com.github.arhke.siegeengines.projectile.FireworkProjectile;
import com.github.arhke.siegeengines.projectile.PotionProjectile;
import com.github.arhke.siegeengines.projectile.SiegeEngineProjectile;
import com.github.arhke.siegeengines.util.MessageUtil;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.ArmorStand.LockType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class SiegeEngine implements Cloneable {

    // Core variables
    private SiegeEngineType type;
    private Boolean enabled;
    private String id;
    private Component itemName;
    private List<Component> itemLore;
    private String worldName;
    private UUID entityId;
    private Entity entity;
    private int yOffset;
    private int xOffset;
    // private int pitchOffset; // Internal - Never Initialized and Defaulted to 0
    private double placementOffsetY;
    private float velocity;
    private double baseHealth = 5.0d;
    // Firing variables
    private int shotAmount;
    private int millisecondsBetweenFiringStages;
    private int cooldownTicks;
    // private int millisecondsToLoad; // Internal - Never Used
    private int modelNumberToFireAt;
    private boolean cycleThroughModelsWhileFiring;
    private boolean rotateSideways;
    private boolean rotateUpDown;
    private boolean rotateStandHead;
    // Degrees above the horizon the siege engine can aim between, negative is below
    private float minAimAngle;
    private float maxAimAngle;
    private boolean reloading; // Internal
    private int taskNumber;
    private List<Integer> firingModelNumbers;
    private int readyModelNumber;
    private int nextModelNumber; // Internal
    private int loadedModelNumber;
    private SiegeEngineProjectile defaultProjectile;

    // Optional variables
    private boolean allowInvisibleStand; // Internal
    private final boolean hasBaseStand; // Internal
    private final double baseStandOffset; // Internal
    private final int baseStandModelNumber; // Internal
    private boolean mountable = false;

    // Passed parameters
    private String engineName = "Unnammed SiegeEngine";
    private HashMap<ItemStack, SiegeEngineProjectile> projectiles = new HashMap<>();

    private int customModelID = 150;

    @Override
    public SiegeEngine clone() throws CloneNotSupportedException {
        return (SiegeEngine) super.clone();
    }

    public SiegeEngine(@NotNull String name, @NotNull HashMap<ItemStack, SiegeEngineProjectile> projectiles, int customModelID) {

        // Default custom model data id if it is passes as null
        if (customModelID == 0) {
            customModelID = 150;
        }

        // Set Default values
        setType(SiegeEngineType.UNKNOWN);
        setItemName(MessageUtil.legacy("&eUnknown Siege Engine"));
        setItemLore(new ArrayList<>());
        setXOffset(0);
        setYOffset(0);
        setVelocity(5.0625f);
        setCooldownTicks(40);
        id = name.replaceAll(" ", "_").toLowerCase();
        setRotateStandHead(true);
        setRotateSideways(true);
        setPlacementOffsetY(-1.125f);
        setReadyModelNumber(customModelID);
        setModelNumberToFireAt(customModelID);
        setLoadedModelNumber(customModelID);
        setFiringModelNumbers(new ArrayList<>());
        setCycleThroughModelsWhileFiring(false);
        allowInvisibleStand = false;
        setShotAmount(1);
        setEnabled(true);
        setRotateSideways(false);
        setRotateUpDown(true);
        setRotateStandHead(true);
        setMinAimAngle(-85);
        setMaxAimAngle(85);
        reloading = false;
        allowInvisibleStand = false;
        hasBaseStand = false;
        baseStandOffset = 0;
        baseStandModelNumber = 147;
        // PLACEHOLDER. SHOULD BE WE-WRITTEN
        EntityProjectile defaultProj = new EntityProjectile(new ItemStack(Material.GRASS_BLOCK));
        defaultProj.projectileCount = 2;
        defaultProj.entityType = EntityType.SNOWBALL;
        defaultProj.particleType = Particle.WHITE_ASH;
        defaultProj.soundType = Sound.ENTITY_BLAZE_SHOOT;

        // set passed parameters as object variables
        this.setEngineName(name);
        this.setCustomModelID(customModelID);

        // set projectiles
        if (this.getProjectiles() == null || this.getProjectiles().isEmpty()) {
            this.getProjectiles().put(new ItemStack(Material.GUNPOWDER), defaultProj);
        } else {
            this.setProjectiles(projectiles);
        }
    }

    public boolean equals(String EquipmentId) {
        return id.equals(EquipmentId);
    }

    public boolean isReloading() {
        return reloading;
    }

    /**
     * Resolves the projectile this siege engine fires for the given item.
     *
     * @return null if the item is not one of this siege engine's projectiles
     */
    public SiegeEngineProjectile getProjectileFor(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        SiegeEngineProjectile bestMatch = null;
        int bestChecks = -1;
        for (Map.Entry<ItemStack, SiegeEngineProjectile> entry : getProjectiles().entrySet()) {
            final Material type = entry.getKey().getType();
            // Fireworks and potions match on type, the held item's meta decides the effect
            if (type == Material.FIREWORK_ROCKET && item.getType() == type) {
                return FireworkProjectile.getDefaultRocketShot(item.asOne());
            }
            if (type == Material.SPLASH_POTION && item.getType() == type) {
                return new PotionProjectile(item.asOne());
            }
            // When several projectiles match, the one that checks the most (name and lore) wins
            final int checks = matchAmmo(entry.getKey(), item);
            if (checks > bestChecks) {
                bestMatch = entry.getValue();
                bestChecks = checks;
            }
        }
        return bestMatch;
    }

    /**
     * Checks a held item against a projectile's ammo item: the type always, the name and lore only if the ammo item has them.
     *
     * @return how many of the name and lore were checked, or -1 if the item doesn't match
     */
    private static int matchAmmo(ItemStack ammo, ItemStack item) {
        if (ammo.getType() != item.getType()) {
            return -1;
        }
        final ItemMeta ammoMeta = ammo.getItemMeta();
        final ItemMeta itemMeta = item.getItemMeta();
        int checks = 0;
        if (ammoMeta.hasDisplayName()) {
            final Component name = itemMeta.hasDisplayName() ? itemMeta.displayName()
                : itemMeta.hasItemName() ? itemMeta.itemName() : null;
            if (name == null || !MessageUtil.toLegacy(name).equals(MessageUtil.toLegacy(ammoMeta.displayName()))) {
                return -1;
            }
            checks++;
        }
        if (ammoMeta.hasLore()) {
            final List<Component> lore = itemMeta.hasLore() ? itemMeta.lore() : List.of();
            if (!lore.stream().map(MessageUtil::toLegacy).toList()
                .equals(ammoMeta.lore().stream().map(MessageUtil::toLegacy).toList())) {
                return -1;
            }
            checks++;
        }
        return checks;
    }

    public Location GetFireLocation(LivingEntity living) {
        if (living == null)
            return null;
        Location loc = living.getEyeLocation();
        Vector direction = living.getLocation().getDirection().multiply(getXOffset());
        loc.add(direction);
        if (getYOffset() > 0) {
            loc.setY(loc.getY() + getYOffset());
        } else {
            loc.setY(loc.getY() - getYOffset());
        }
        loc.setPitch(clampPitch(loc.getPitch()));
        return loc;
    }

    public void ShowFireLocation(Entity player) {
        LivingEntity living = (LivingEntity) getEntity();
        if (player instanceof Player) {
            if (living != null) {
                Location origin = this.GetFireLocation(living);
                Vector direction = origin.getDirection();
                direction.multiply(5 /* the range */);
                direction.normalize();
                for (int i = 0; i < 3 /* range */; i += 1) {
                    Location spawn = origin.add(direction);
                    ((Player) player).spawnParticle(Particle.CLOUD, spawn, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    public void Fire(Player player, float delay) {
        if (reloading) {
            player.sendMessage("§eThis Siege Engine is still reloading!");
            return;
        }
        if (!(getEntity() instanceof LivingEntity living) || living.isDead()) {
            return;
        }
        reloading = true;
        setWorldName(getEntity().getWorld().getName());

        if (isCycleThroughModelsWhileFiring()) {
            nextModelNumber = 0;
            taskNumber = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(SiegeEngines.getInstance(), () -> {
                if (!(getEntity() instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
                    Bukkit.getServer().getScheduler().cancelTask(taskNumber);
                    reloading = false;
                    return;
                }
                //firing stages
                if (nextModelNumber < getFiringModelNumbers().size()) {
                    int modelData = getFiringModelNumbers().get(nextModelNumber);
                    SiegeEnginesUtil.UpdateEntityIdModel(getEntity(), modelData, getWorldName());
                    if (modelData == getModelNumberToFireAt()) {
                        shoot(player, livingEntity);
                    }
                    nextModelNumber += 1;
                } else {
                    Bukkit.getServer().getScheduler().cancelTask(taskNumber);
                    reload();
                }
            }, 0, getMillisecondsBetweenFiringStages());
        } else {
            taskNumber = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(SiegeEngines.getInstance(), () -> {
                if (!(getEntity() instanceof LivingEntity livingEntity) || livingEntity.isDead()) {
                    reloading = false;
                    return;
                }
                shoot(player, livingEntity);
                reload();
            }, (long) delay);
        }
    }

    /**
     * Fires the projectile for the item in the player's main hand, consuming one of it.
     * Falls back to the default projectile if the held item is not a projectile of this siege engine.
     */
    private void shoot(Player player, LivingEntity living) {
        SiegeEngineProjectile projectile = null;
        if (player.isOnline()) {
            final ItemStack itemInHand = player.getInventory().getItemInMainHand();
            projectile = getProjectileFor(itemInHand);
            if (projectile != null && player.getGameMode() != GameMode.CREATIVE) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            }
        }
        if (projectile == null) {
            projectile = getDefaultProjectile();
        }
        if (projectile == null) {
            return;
        }
        projectile.Shoot(player, living, this.GetFireLocation(living), getVelocity());
    }

    /**
     * Plays the firing models in reverse over the cooldown, then rests on the loaded model.
     */
    private void reload() {
        final List<Integer> stages = getFiringModelNumbers();
        if (stages.isEmpty()) {
            taskNumber = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(SiegeEngines.getInstance(),
                () -> reloading = false, getCooldownTicks());
            return;
        }
        // The last firing model is already showing, so step back from the one before it
        nextModelNumber = stages.size() - 1;
        final long period = Math.max(1, getCooldownTicks() / stages.size());
        taskNumber = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(SiegeEngines.getInstance(), () -> {
            if (getEntity() == null || getEntity().isDead()) {
                Bukkit.getServer().getScheduler().cancelTask(taskNumber);
                reloading = false;
                return;
            }
            nextModelNumber -= 1;
            if (nextModelNumber >= 0) {
                SiegeEnginesUtil.UpdateEntityIdModel(getEntity(), stages.get(nextModelNumber), getWorldName());
            } else {
                Bukkit.getServer().getScheduler().cancelTask(taskNumber);
                SiegeEnginesUtil.UpdateEntityIdModel(getEntity(), getLoadedModelNumber(), getWorldName());
                reloading = false;
            }
        }, period, period);
    }

    public boolean place(Entity player, Location l) {
        return place(player, l, null);
    }

    @SuppressWarnings("deprecation")
    public boolean place(Entity player, Location l, Entity mount) {
        l.add(0.5, 0, 0.5);
        NamespacedKey key = new NamespacedKey(SiegeEngines.getInstance(), "siege_engines");
        if (!this.getEnabled()) {
            return false;
        }
        l.setY(l.getY() + 1);
        l.setDirection(player.getFacing().getDirection());
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();
        String id = "";
        Entity entity3;
        for (Entity enti : l.getWorld().getNearbyEntities(l, Config.placementDensity, Config.placementDensity, Config.placementDensity)) {
            if (SiegeEngines.activeSiegeEngines.containsKey(enti.getUniqueId())) {
                return false;
            }
        }
        if (this.hasBaseStand) {

            l.setY(l.getY() + this.baseStandOffset);
            entity3 = player.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);

            LivingEntity ent = (LivingEntity) entity3;
            ArmorStand stand = (ArmorStand) ent;
            id = entity3.getUniqueId().toString();
            meta.setCustomModelData(this.baseStandModelNumber);
            stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.HAND, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.OFF_HAND, LockType.ADDING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.HAND, LockType.REMOVING_OR_CHANGING);
            stand.addEquipmentLock(EquipmentSlot.OFF_HAND, LockType.REMOVING_OR_CHANGING);
            stand.setBasePlate(false);
            item.setItemMeta(meta);
            stand.setInvisible(this.allowInvisibleStand);
            ent.getEquipment().setHelmet(SiegeEnginesUtil.makeUnwearable(item));
            stand.setGravity(false);
            stand.setMarker(true);
        }
        l.setY(l.getY() + this.getPlacementOffsetY());
        Entity entity2 = player.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        if (!id.isEmpty()) {
            entity2.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        }
        meta.setCustomModelData(this.getLoadedModelNumber());
        meta.displayName(this.getItemName());
        meta.lore(this.getItemLore());
        item.setItemMeta(meta);


        LivingEntity ent = (LivingEntity) entity2;
        ArmorStand stand = (ArmorStand) ent;
        this.setEntity(entity2);

        this.setEntityId(entity2.getUniqueId());
        stand.addEquipmentLock(EquipmentSlot.HEAD, LockType.REMOVING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.LEGS, LockType.ADDING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.CHEST, LockType.ADDING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.FEET, LockType.ADDING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.HAND, LockType.REMOVING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.OFF_HAND, LockType.ADDING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.HAND, LockType.REMOVING_OR_CHANGING);
        stand.addEquipmentLock(EquipmentSlot.OFF_HAND, LockType.REMOVING_OR_CHANGING);
        stand.setInvisible(this.allowInvisibleStand);
        stand.setBasePlate(true);

        stand.setGravity(false);
        ent.getEquipment().setHelmet(SiegeEnginesUtil.makeUnwearable(item));
        stand.setMaxHealth(baseHealth);
        stand.setHealth(baseHealth);
        SiegeEngines.activeSiegeEngines.put(stand.getUniqueId(), this);
        if (mount != null && !(mount.isDead())) {
            entity2.setGravity(true);
            mount.addPassenger(entity2);
        }
        return true;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public SiegeEngineType getType() {
        return type;
    }

    public void setType(SiegeEngineType type) {
        this.type = type;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Component getItemName() {
        return itemName;
    }

    public void setItemName(Component itemName) {
        this.itemName = itemName;
    }

    public List<Component> getItemLore() {
        return itemLore;
    }

    public void setItemLore(List<Component> itemLore) {
        this.itemLore = itemLore;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getHealth() {
        return baseHealth;
    }

    public void setHealth(double health) {
        this.baseHealth = health;
    }

    /**
     * The siege engine's armor stand. When its chunk unloads and loads again, Bukkit creates a new entity object
     * with the same UUID and the old one stays dead, so look the current one up by UUID.
     */
    public Entity getEntity() {
        if ((entity == null || !entity.isValid()) && entityId != null) {
            final Entity current = Bukkit.getEntity(entityId);
            if (current != null) {
                entity = current;
            }
        }
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public int getYOffset() {
        return yOffset;
    }

    public void setYOffset(int yOffset) {
        this.yOffset = yOffset;
    }

    public int getXOffset() {
        return xOffset;
    }

    public void setXOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public double getPlacementOffsetY() {
        return placementOffsetY;
    }

    public void setPlacementOffsetY(double placementOffsetY) {
        this.placementOffsetY = placementOffsetY;
    }

    public float getVelocity() {
        return velocity;
    }

    public void setVelocity(float velocity) {
        this.velocity = velocity;
    }

    public int getShotAmount() {
        return shotAmount;
    }

    public void setShotAmount(int shotAmount) {
        this.shotAmount = shotAmount;
    }

    public int getMillisecondsBetweenFiringStages() {
        return millisecondsBetweenFiringStages;
    }

    public void setMillisecondsBetweenFiringStages(int millisecondsBetweenFiringStages) {
        this.millisecondsBetweenFiringStages = millisecondsBetweenFiringStages;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public int getModelNumberToFireAt() {
        return modelNumberToFireAt;
    }

    public void setModelNumberToFireAt(int modelNumberToFireAt) {
        this.modelNumberToFireAt = modelNumberToFireAt;
    }

    public boolean isCycleThroughModelsWhileFiring() {
        return cycleThroughModelsWhileFiring;
    }

    public void setCycleThroughModelsWhileFiring(boolean cycleThroughModelsWhileFiring) {
        this.cycleThroughModelsWhileFiring = cycleThroughModelsWhileFiring;
    }

    public boolean isRotateSideways() {
        return rotateSideways;
    }

    public void setRotateSideways(boolean rotateSideways) {
        this.rotateSideways = rotateSideways;
    }

    public boolean isRotateUpDown() {
        return rotateUpDown;
    }

    public void setRotateUpDown(boolean rotateUpDown) {
        this.rotateUpDown = rotateUpDown;
    }

    public boolean isRotateStandHead() {
        return rotateStandHead;
    }

    public void setRotateStandHead(boolean rotateStandHead) {
        this.rotateStandHead = rotateStandHead;
    }

    /**
     * Clamps a pitch to the angles this siege engine can aim between.
     * Pitch is negative upwards, so the max aim angle is the min pitch.
     */
    public float clampPitch(float pitch) {
        return Math.max(-getMaxAimAngle(), Math.min(-getMinAimAngle(), pitch));
    }

    public float getMinAimAngle() {
        return minAimAngle;
    }

    public void setMinAimAngle(float minAimAngle) {
        this.minAimAngle = minAimAngle;
    }

    public float getMaxAimAngle() {
        return maxAimAngle;
    }

    public void setMaxAimAngle(float maxAimAngle) {
        this.maxAimAngle = maxAimAngle;
    }

    public List<Integer> getFiringModelNumbers() {
        return firingModelNumbers;
    }

    public void setFiringModelNumbers(List<Integer> firingModelNumbers) {
        this.firingModelNumbers = firingModelNumbers;
    }

    public int getReadyModelNumber() {
        return readyModelNumber;
    }

    public void setReadyModelNumber(int readyModelNumber) {
        this.readyModelNumber = readyModelNumber;
    }

    public int getLoadedModelNumber() {
        return loadedModelNumber;
    }

    public void setLoadedModelNumber(int loadedModelNumber) {
        this.loadedModelNumber = loadedModelNumber;
    }

    public boolean isMountable() {
        return mountable;
    }

    public void setMountable(boolean canMount) {
        this.mountable = canMount;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public SiegeEngineProjectile getDefaultProjectile() {
        return defaultProjectile;
    }

    public void setDefaultProjectile(SiegeEngineProjectile defaultProjectile) {
        this.defaultProjectile = defaultProjectile;
    }

    public HashMap<ItemStack, SiegeEngineProjectile> getProjectiles() {
        return projectiles;
    }

    public void setProjectiles(HashMap<ItemStack, SiegeEngineProjectile> projectiles) {
        this.projectiles = projectiles;
    }

    public int getCustomModelID() {
        return customModelID;
    }

    public void setCustomModelID(int customModelID) {
        this.customModelID = customModelID;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
