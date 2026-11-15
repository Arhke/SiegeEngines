package com.github.arhke.siegeengines.util;

import com.github.arhke.siegeengines.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Creates explosions for siege engines, which use the block resistances from the config instead of the vanilla ones.
 */
public class SiegeExplosionUtil {

    private static final ExplosionDamageCalculator CALCULATOR = new ExplosionDamageCalculator() {
        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, FluidState fluid) {
            final Optional<Float> vanilla = super.getBlockExplosionResistance(explosion, level, pos, state, fluid);
            if (vanilla.isEmpty()) {
                return vanilla;
            }
            // Water, lava and bubble columns are passed through like air
            if (Config.siegeExplosionsIgnoreFluids && (state.getBlock() instanceof LiquidBlock || state.getBlock() == Blocks.BUBBLE_COLUMN)) {
                return Optional.empty();
            }
            // Unbreakable blocks keep their resistance
            if (state.getDestroySpeed(level, pos) < 0) {
                return vanilla;
            }
            // Waterlogged blocks only count the block itself
            final float resistance = Config.siegeExplosionsIgnoreFluids ? state.getBlock().getExplosionResistance() : vanilla.get();
            final float maxResistance = state.getBlock() == Blocks.REINFORCED_DEEPSLATE
                ? Config.siegeExplosionsReinforcedDeepslateBlastResistance
                : Config.siegeExplosionsMaxBlastResistance;
            return Optional.of(Math.min(resistance, maxResistance));
        }
    };

    /**
     * Creates a siege engine explosion that breaks blocks like TNT.
     *
     * @param source  the entity that exploded, which the explosion event is called for
     * @param shooter the entity that gets credit for kills, or null for nobody
     */
    public static void explode(Entity source, @Nullable Entity shooter, Location location, float power) {
        final ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        final net.minecraft.world.entity.Entity nmsSource = ((CraftEntity) source).getHandle();
        final net.minecraft.world.entity.Entity nmsShooter = shooter == null ? null : ((CraftEntity) shooter).getHandle();
        final DamageSource damageSource = level.damageSources().explosion(nmsSource, nmsShooter);
        level.explode(nmsSource, damageSource, CALCULATOR, location.getX(), location.getY(), location.getZ(),
            power, false, Level.ExplosionInteraction.TNT);
    }
}
