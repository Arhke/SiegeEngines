package com.github.arhke.siegeengines.reinforcement;

import com.github.arhke.siegeengines.SiegeEngines;
import com.github.arhke.siegeengines.config.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shows how many breaks a block has left: a small copy of the block pops out of its closest visible face with the
 * number next to it, colored from green to red, gives a thump whenever the block is hit, and slides back in when hidden.
 */
public class ReinforcementHologram {

    private static final BlockFace[] FACES = {BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN, BlockFace.SOUTH, BlockFace.NORTH};
    // Blocks next to a face that still let it be seen
    private static final Set<Material> SEE_THROUGH = EnumSet.of(Material.AIR, Material.CAVE_AIR, Material.WATER, Material.LAVA, Material.FIRE);
    private static final long HIDE_DELAY_TICKS = 40;

    // Size of the small block, and how far its middle sits out from the middle of the real block
    private static final float MINI_SCALE = 0.4f;
    private static final double MINI_OFFSET = 0.52;
    // How far the number sits out past the small block, and above it (below it on the bottom face)
    private static final double TEXT_OFFSET = 0.1;
    private static final double TEXT_RISE = 0.3;
    private static final double TEXT_DROP = 0.42;
    // How long sliding out and in takes, which the client animates
    private static final int SLIDE_OUT_TICKS = 6;
    private static final int SLIDE_IN_TICKS = 4;
    // The pop when it first comes out: starts this small and turned this far, overshoots, then settles
    private static final float POP_START_SCALE = 0.1f;
    private static final float POP_START_TURN = (float) Math.toRadians(-120);
    private static final float POP_SCALE = 1.25f;
    private static final int POP_SETTLE_TICKS = 4;
    // The thump: grow and tilt over a couple of ticks, flashing the number white, then settle back. Bigger hits thump
    // harder, up to the max at a hit of THUMP_FULL_HIT of the block's breaks
    private static final float THUMP_MIN_SCALE = 1.35f;
    private static final float THUMP_MAX_SCALE = 1.7f;
    private static final float THUMP_MIN_TILT = (float) Math.toRadians(12);
    private static final float THUMP_MAX_TILT = (float) Math.toRadians(30);
    private static final float THUMP_FULL_HIT = 0.25f;
    private static final int THUMP_GROW_TICKS = 2;
    private static final int THUMP_SETTLE_TICKS = 6;
    // Dark, mostly see-through backing behind the number
    private static final Color BACKGROUND = Color.fromARGB(150, 12, 12, 12);
    private static final TextColor FULL = TextColor.color(85, 255, 85);
    private static final TextColor HALF = TextColor.color(255, 230, 70);
    private static final TextColor EMPTY = TextColor.color(255, 60, 50);

    private BlockDisplay miniBlock;
    private TextDisplay text;
    private BlockFace face;
    // The middle of the real block, which the hologram slides out of and back into
    private Location origin;
    private boolean hiding;
    private BukkitTask hideTask;
    private BukkitTask slideTask;
    private BukkitTask thumpTask;
    private BukkitTask popTask;
    private float breaksLeft;
    private boolean flashing;

    /**
     * Shows the hologram on the face of the block closest to the viewer, until it is hidden.
     */
    public void show(Block block, Location viewer, float breaksLeft) {
        cancelHide();
        final BlockFace closest = getClosestVisibleFace(block, viewer);
        if (miniBlock == null || !miniBlock.isValid() || text == null || !text.isValid()) {
            removeNow();
            spawn(block, closest);
        } else if (hiding || closest != face) {
            // Slide back out, or over to the face that is now closest
            slideOut(block, closest);
        }
        hiding = false;
        this.breaksLeft = breaksLeft;
        updateText();
    }

    /**
     * Shows the hologram with a thump, and hides it again after a couple of seconds.
     *
     * @param hitShare how much of the block's breaks the hit took, which decides how hard it thumps
     */
    public void flash(Block block, Location viewer, float breaksLeft, float hitShare) {
        show(block, viewer, breaksLeft);
        thump(hitShare);
        hideLater();
    }

    // Rounded up, so a block only shows 0 once it breaks
    private void updateText() {
        if (text != null) {
            text.text(Component.text((int) Math.ceil(breaksLeft),
                flashing ? NamedTextColor.WHITE : greenToRed(breaksLeft, Config.reinforcementDefaultBreaks)));
        }
    }

    public void hideLater() {
        cancelHide();
        hideTask = Bukkit.getScheduler().runTaskLater(SiegeEngines.getInstance(), this::hide, HIDE_DELAY_TICKS);
    }

    /**
     * Slides the hologram back into the block, then removes it.
     */
    public void hide() {
        cancelHide();
        if (miniBlock == null || hiding) {
            return;
        }
        if (!miniBlock.isValid() || !text.isValid()) {
            removeNow();
            return;
        }
        hiding = true;
        cancelSlide();
        miniBlock.setTeleportDuration(SLIDE_IN_TICKS);
        text.setTeleportDuration(SLIDE_IN_TICKS);
        miniBlock.teleport(origin);
        text.teleport(origin);
        // Shrinks back into the block as it slides in
        if (thumpTask != null) {
            thumpTask.cancel();
            thumpTask = null;
        }
        if (popTask != null) {
            popTask.cancel();
            popTask = null;
        }
        flashing = false;
        setTransformation(POP_START_SCALE, new Quaternionf(), SLIDE_IN_TICKS);
        hideTask = Bukkit.getScheduler().runTaskLater(SiegeEngines.getInstance(), this::removeNow, SLIDE_IN_TICKS + 1);
    }

    /**
     * Removes the hologram straight away, e.g. when the block breaks.
     */
    public void removeNow() {
        cancelHide();
        cancelSlide();
        if (thumpTask != null) {
            thumpTask.cancel();
            thumpTask = null;
        }
        if (popTask != null) {
            popTask.cancel();
            popTask = null;
        }
        flashing = false;
        if (miniBlock != null) {
            miniBlock.remove();
            miniBlock = null;
        }
        if (text != null) {
            text.remove();
            text = null;
        }
        face = null;
        origin = null;
        hiding = false;
    }

    /**
     * @return whether the hologram is showing and not on its way back in
     */
    public boolean isShown() {
        return miniBlock != null && !hiding;
    }

    // Spawns both displays inside the block, and slides them out the next tick, once the client has them
    private void spawn(Block block, BlockFace closest) {
        final Location center = block.getLocation().add(0.5, 0.5, 0.5);
        final Block outside = block.getRelative(closest);
        // Lit like the air in front of the face, not the inside of the block they start in
        final Display.Brightness brightness = new Display.Brightness(outside.getLightFromBlocks(), outside.getLightFromSky());
        miniBlock = block.getWorld().spawn(center, BlockDisplay.class, display -> {
            display.setPersistent(false);
            display.setBlock(block.getBlockData());
            display.setBrightness(brightness);
            display.setTransformation(getMiniTransformation(POP_START_SCALE, new Quaternionf().rotateY(POP_START_TURN)));
        });
        text = block.getWorld().spawn(center, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(brightness);
            display.setShadowed(true);
            display.setBackgroundColor(BACKGROUND);
            display.setTransformation(getTextTransformation(POP_START_SCALE));
        });
        face = closest;
        origin = center;
        slideTask = Bukkit.getScheduler().runTaskLater(SiegeEngines.getInstance(), () -> {
            slideTask = null;
            if (miniBlock != null && !hiding) {
                slideOut(block, face);
                pop();
            }
        }, 1);
    }

    // Grows and spins out past full size while sliding out, then settles back
    private void pop() {
        setTransformation(POP_SCALE, new Quaternionf(), SLIDE_OUT_TICKS);
        popTask = Bukkit.getScheduler().runTaskLater(SiegeEngines.getInstance(), () -> {
            popTask = null;
            setTransformation(1, new Quaternionf(), POP_SETTLE_TICKS);
        }, SLIDE_OUT_TICKS);
    }

    private void slideOut(Block block, BlockFace closest) {
        cancelSlide();
        face = closest;
        final Location center = block.getLocation().add(0.5, 0.5, 0.5);
        final Vector direction = closest.getDirection();
        final Location miniLocation = center.clone().add(direction.clone().multiply(MINI_OFFSET));
        final Location textLocation = miniLocation.clone().add(direction.clone().multiply(TEXT_OFFSET));
        // Above the small block, or below it on the bottom face so it isn't inside the block
        textLocation.add(0, closest == BlockFace.DOWN ? -TEXT_DROP : TEXT_RISE, 0);
        miniBlock.setTeleportDuration(SLIDE_OUT_TICKS);
        text.setTeleportDuration(SLIDE_OUT_TICKS);
        miniBlock.teleport(miniLocation);
        text.teleport(textLocation);
    }

    // Grows, tilts and flashes white, then settles back, so it is clear which blocks a shot hit and how hard
    private void thump(float hitShare) {
        // Still popping out, which already draws the eye
        if (popTask != null || slideTask != null) {
            return;
        }
        if (thumpTask != null) {
            thumpTask.cancel();
        }
        final float strength = Math.max(0, Math.min(1, hitShare / THUMP_FULL_HIT));
        final float scale = THUMP_MIN_SCALE + (THUMP_MAX_SCALE - THUMP_MIN_SCALE) * strength;
        final float tilt = THUMP_MIN_TILT + (THUMP_MAX_TILT - THUMP_MIN_TILT) * strength;
        // Knocked over around a random flat axis
        final double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        final Quaternionf rotation = new Quaternionf().rotateAxis(tilt, (float) Math.cos(angle), 0, (float) Math.sin(angle));
        setTransformation(scale, rotation, THUMP_GROW_TICKS);
        flashing = true;
        updateText();
        thumpTask = Bukkit.getScheduler().runTaskLater(SiegeEngines.getInstance(), () -> {
            thumpTask = null;
            flashing = false;
            updateText();
            setTransformation(1, new Quaternionf(), THUMP_SETTLE_TICKS);
        }, THUMP_GROW_TICKS);
    }

    private void setTransformation(float scale, Quaternionf rotation, int ticks) {
        if (miniBlock == null || text == null) {
            return;
        }
        miniBlock.setInterpolationDelay(0);
        miniBlock.setInterpolationDuration(ticks);
        miniBlock.setTransformation(getMiniTransformation(scale, rotation));
        text.setInterpolationDelay(0);
        text.setInterpolationDuration(ticks);
        text.setTransformation(getTextTransformation(scale));
    }

    // Block displays are drawn from their corner, so move the rotated corner back to center it on its location
    private static Transformation getMiniTransformation(float scale, Quaternionf rotation) {
        final float size = MINI_SCALE * scale;
        final Vector3f offset = rotation.transform(new Vector3f(-size / 2));
        return new Transformation(offset, rotation, new Vector3f(size), new Quaternionf());
    }

    private static Transformation getTextTransformation(float scale) {
        return new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale), new Quaternionf());
    }

    private void cancelHide() {
        if (hideTask != null) {
            hideTask.cancel();
            hideTask = null;
        }
    }

    private void cancelSlide() {
        if (slideTask != null) {
            slideTask.cancel();
            slideTask = null;
        }
    }

    private static BlockFace getClosestVisibleFace(Block block, Location viewer) {
        final Location center = block.getLocation().add(0.5, 0.5, 0.5);
        BlockFace closest = BlockFace.UP;
        double closestDistance = Double.MAX_VALUE;
        for (BlockFace face : FACES) {
            if (!SEE_THROUGH.contains(block.getRelative(face).getType())) {
                continue;
            }
            final double distance = center.clone().add(face.getDirection().multiply(0.6)).distanceSquared(viewer);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = face;
            }
        }
        return closest;
    }

    // Blends from green when full, through yellow at half, to red when nearly broken
    private static TextColor greenToRed(float value, float max) {
        final float health = Math.max(0, Math.min(1, value / max));
        return health >= 0.5f ? lerp(HALF, FULL, (health - 0.5f) * 2) : lerp(EMPTY, HALF, health * 2);
    }

    private static TextColor lerp(TextColor from, TextColor to, float t) {
        return TextColor.color(
            Math.round(from.red() + (to.red() - from.red()) * t),
            Math.round(from.green() + (to.green() - from.green()) * t),
            Math.round(from.blue() + (to.blue() - from.blue()) * t));
    }
}
