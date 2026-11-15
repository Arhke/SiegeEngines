package com.github.arhke.siegeengines.listeners;

import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.SiegeEnginesDamageSource;
import com.github.arhke.siegeengines.SiegeEnginesLogger;
import com.github.arhke.siegeengines.config.Config;
import com.github.arhke.siegeengines.data.SiegeEnginesData;
import com.github.arhke.siegeengines.projectile.ExplosiveProjectile;
import com.github.arhke.siegeengines.util.GeneralUtil;
import com.github.arhke.siegeengines.util.ImpactAngleUtil;
import com.github.arhke.siegeengines.util.SiegeEnginesUtil;
import com.github.arhke.siegeengines.util.SiegeExplosionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;

public class SiegeEngineDamagedListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onSiegeEngineHitByProjectile(ProjectileHitEvent event) {
        for (Entity entity : event.getEntity().getNearbyEntities(2, 2, 2)) {
            if (entity instanceof ArmorStand stand) {
                if (event.isCancelled()) return;
                if (SiegeEnginesUtil.isSiegeEngine(stand, false)) {
                    event.setCancelled(true);
                    SiegeEnginesLogger.debug("ARROW DAMAGE CANCELLED? : " + Config.arrowDamageToggle);
                    if (Config.arrowDamageToggle) {
                        continue;
                    }
                    SiegeEnginesLogger.debug("HEALTH BEOFRE SHOT : " + stand.getHealth());
                    if (stand.getHealth() - 2 > 0) {
                        stand.setHealth(stand.getHealth() - 2);
                    } else {
                        EntityDeathEvent death = new EntityDeathEvent(stand, new SiegeEnginesDamageSource(stand, event.getHitEntity(), stand), SiegeEnginesData.items);
                        Bukkit.getServer().getPluginManager().callEvent(death);
                        if (!death.isCancelled()) stand.setHealth(0.0f);
                    }
                    SiegeEnginesLogger.debug("HEALTH AFTER SHOT : " + stand.getHealth());
                    if (stand.isDead()) {
                        PlayerHandler.siegeEngineEntityDied(stand);
                    }
                }
            }
        }
        if (SiegeEnginesData.projectiles.containsKey(event.getEntity().getUniqueId())) {
            ExplosiveProjectile proj = SiegeEnginesData.projectiles.get(event.getEntity().getUniqueId());
            Projectile snowball = event.getEntity();
            Entity player = (Entity) snowball.getShooter();
            if (player instanceof Player) {
                player.sendMessage("§eDistance to impact: "
                        + String.format("%.2f", player.getLocation().distance(snowball.getLocation())));
            }
            Location loc = snowball.getLocation();

            SiegeEnginesData.projectiles.remove(event.getEntity().getUniqueId());

            if (proj.placeBlocks) {
                TNTPrimed tntEnt = (TNTPrimed) event.getEntity().getWorld().spawnEntity(loc, EntityType.TNT);
                tntEnt.setYield(0);
                tntEnt.setFuseTicks(0);
                if (event.getHitBlock() != null) {
                    List<Block> Blocks = GeneralUtil.getSphere(event.getHitBlock().getLocation(), (int) proj.explodePower);
                    for (int i = 0; i < proj.blocksToPlaceAmount; i++) {
                        Block replace = (Block) GeneralUtil.getRandomElement(Blocks);
                        replace.setType(proj.blockToPlace);
                    }
                }
            } else {
                // Vary each shot's power by up to ExplodePowerVariation either way
                float explodePower = proj.explodePower
                    * (1 + (SiegeEngines.random.nextFloat() * 2 - 1) * proj.explodePowerVariation);
                // Direct hits on entities explode with half power
                if (event.getHitEntity() != null) {
                    explodePower *= 0.5f;
                }
                snowball.getPersistentDataContainer().set(SiegeEnginesData.explosionStrengthKey, PersistentDataType.FLOAT, proj.strength);
                // Glancing hits on blocks do less reinforcement damage than head-on hits, and the surface decides which
                // blocks are thin: those not backed up by a full solid block the explosion didn't hit, going into the surface
                final Vector velocity = snowball.getVelocity();
                if (event.getHitBlock() != null && velocity.lengthSquared() > 0) {
                    final Vector normal = ImpactAngleUtil.getSurfaceNormal(loc, velocity);
                    snowball.getPersistentDataContainer().set(SiegeEnginesData.explosionImpactFactorKey, PersistentDataType.FLOAT,
                        ImpactAngleUtil.getImpactFactor(normal, velocity));
                    if (normal != null) {
                        final Vector into = ImpactAngleUtil.getIntoSurface(normal, velocity);
                        snowball.getPersistentDataContainer().set(SiegeEnginesData.explosionDirectionKey, PersistentDataType.LIST.doubles(),
                            List.of(into.getX(), into.getY(), into.getZ()));
                    }
                }
                SiegeExplosionUtil.explode(snowball, player, loc, explodePower);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSiegeEngineDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            if (SiegeEnginesUtil.isSiegeEngine(stand, false)) return;
            if (event.isCancelled()) return;
            if (event.getDamager() instanceof Player) {
                PlayerHandler.releasePlayerSiegeEngine((Player) (event.getDamager()), event.getEntity());
            }
            SiegeEnginesLogger.debug("HEALTH BEOFRE HIT : " + stand.getHealth());
            if (stand.getHealth() - 2 > 0) {
                stand.setHealth(stand.getHealth() - 2);
            } else {
                EntityDeathEvent death = new EntityDeathEvent(stand, event.getDamageSource(), SiegeEnginesData.items);
                Bukkit.getServer().getPluginManager().callEvent(death);
                if (death.isCancelled()) return;
                stand.setHealth(0.0f);
                PlayerHandler.siegeEngineEntityDied(event.getEntity());
            }
            SiegeEnginesLogger.debug("HEALTH AFTER HIT : " + stand.getHealth());
        }
    }
}

