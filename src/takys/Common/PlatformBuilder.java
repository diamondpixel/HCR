package takys.Common;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import takys.Setup;
import takys.Utilities.SafeTeleportManager;
import takys.Utilities.Utilities;

import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized helper for building and (optionally) deleting temporary platforms used by both
 * SafeTeleportManager and SafeChest systems.
 */
public final class PlatformBuilder {

    private static final Setup setup = Setup.instance;

    private PlatformBuilder() {}

    // Constants
    private static final double PLATFORM_LEAVE_DISTANCE = 6.0;
    private static final double PLATFORM_LEAVE_DISTANCE_SQUARED = PLATFORM_LEAVE_DISTANCE * PLATFORM_LEAVE_DISTANCE;
    private static final int PLATFORM_SIZE = 4;
    private static final int GLASS_PAD_SIZE = 3;
    private static final int ENCLOSED_HEIGHT = 3;
    private static final int CEILING_OFFSET = 4;
    private static final long TIMER_INTERVAL_TICKS = 40L;
    private static final int MIN_ACCELERATED_TICKS = 40;
    private static final int ACCELERATION_DIVISOR = 3;

    // Protected materials that should not be overwritten
    private static final EnumSet<Material> PROTECTED_MATERIALS = EnumSet.of(
            Material.BEDROCK,
            Material.CHEST,
            Material.OBSIDIAN,
            Material.BARRIER
    );

    // Thread-safe map for timer management
    private static final ConcurrentHashMap<Player, BukkitTask> actionBarTimers = new ConcurrentHashMap<>();

    /**
     * Builds a 4×4 obsidian platform at the specified location with 2-block clearance above.
     * Optionally schedules automatic deletion based on configuration.
     *
     * @param world The world to build in
     * @param centerX Center X coordinate
     * @param y Y coordinate for the platform
     * @param centerZ Center Z coordinate
     * @param skipDeletion Whether to skip automatic deletion
     * @param player The player associated with this platform (for deletion tracking)
     */
    public static void createObsidianPlatform(World world, int centerX, int y, int centerZ,
                                              boolean skipDeletion, Player player) {
        buildPlatformFloor(world, centerX, y, centerZ, Material.OBSIDIAN);
        clearPlatformHeadroom(world, centerX, y, centerZ, 2);

        if (!skipDeletion) {
            schedulePlatformDeletion(world, centerX, y, centerZ, false, player);
        }
    }

    /**
     * Builds a 4×3×4 enclosed glass box with obsidian floor and ceiling for liquid protection.
     * Optionally schedules automatic deletion based on configuration.
     *
     * @param world The world to build in
     * @param centerX Center X coordinate
     * @param y Y coordinate for the platform floor
     * @param centerZ Center Z coordinate
     * @param skipDeletion Whether to skip automatic deletion
     * @param player The player associated with this platform (for deletion tracking)
     */
    public static void createEnclosedGlassPlatform(World world, int centerX, int y, int centerZ,
                                                   boolean skipDeletion, Player player) {
        // Build floor and ceiling
        buildPlatformFloor(world, centerX, y, centerZ, Material.OBSIDIAN);
        buildPlatformFloor(world, centerX, y + CEILING_OFFSET, centerZ, Material.OBSIDIAN);

        // Clear interior space
        clearEnclosedInterior(world, centerX, y, centerZ);

        // Build glass walls
        buildGlassWalls(world, centerX, y, centerZ);

        if (!skipDeletion) {
            schedulePlatformDeletion(world, centerX, y, centerZ, true, player);
        }
    }

    /**
     * Creates a simple 3×3 glass pad beneath a chest. Does not schedule deletion.
     * Only replaces liquid blocks and air, preserving existing solid blocks.
     *
     * @param world The world to build in
     * @param centerX Center X coordinate
     * @param y Y coordinate for the pad
     * @param centerZ Center Z coordinate
     */
    public static void createGlassPad3x3(World world, int centerX, int y, int centerZ) {
        final int halfSize = GLASS_PAD_SIZE / 2; // 1 for 3x3

        for (int dx = -halfSize; dx <= halfSize; dx++) {
            for (int dz = -halfSize; dz <= halfSize; dz++) {
                Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                Material blockType = block.getType();

                // Only place glass where there's air or liquid, skip protected materials
                if (!PROTECTED_MATERIALS.contains(blockType) &&
                        (block.isLiquid() || blockType == Material.AIR)) {
                    block.setType(Material.GLASS);
                }
            }
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Builds a 4×4 platform floor with the specified material.
     */
    private static void buildPlatformFloor(World world, int centerX, int y, int centerZ, Material material) {
        final int halfSize = (PLATFORM_SIZE / 2) - 1; // -1 to 2 for 4x4

        for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
            for (int dz = -halfSize; dz <= halfSize + 1; dz++) {
                Block block = world.getBlockAt(centerX + dx, y, centerZ + dz);
                if (!PROTECTED_MATERIALS.contains(block.getType())) {
                    block.setType(material);
                }
            }
        }
    }

    /**
     * Clears headroom above the platform.
     */
    private static void clearPlatformHeadroom(World world, int centerX, int y, int centerZ, int height) {
        final int halfSize = (PLATFORM_SIZE / 2) - 1;

        for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
            for (int dz = -halfSize; dz <= halfSize + 1; dz++) {
                for (int dy = 1; dy <= height; dy++) {
                    Block block = world.getBlockAt(centerX + dx, y + dy, centerZ + dz);
                    if (!PROTECTED_MATERIALS.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Clears the interior of an enclosed platform.
     */
    private static void clearEnclosedInterior(World world, int centerX, int y, int centerZ) {
        final int halfSize = (PLATFORM_SIZE / 2) - 1;

        for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
            for (int dz = -halfSize; dz <= halfSize + 1; dz++) {
                for (int dy = 1; dy <= ENCLOSED_HEIGHT; dy++) {
                    Block block = world.getBlockAt(centerX + dx, y + dy, centerZ + dz);
                    if (!PROTECTED_MATERIALS.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Builds glass walls for the enclosed platform.
     */
    private static void buildGlassWalls(World world, int centerX, int y, int centerZ) {
        final int halfSize = (PLATFORM_SIZE / 2) - 1;

        // North and South walls (along X axis)
        for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
            for (int dy = 1; dy <= ENCLOSED_HEIGHT; dy++) {
                // North wall (negative Z)
                Block northWall = world.getBlockAt(centerX + dx, y + dy, centerZ - halfSize);
                if (northWall.getType() == Material.AIR) {
                    northWall.setType(Material.GLASS);
                }

                // South wall (positive Z)
                Block southWall = world.getBlockAt(centerX + dx, y + dy, centerZ + halfSize + 1);
                if (southWall.getType() == Material.AIR) {
                    southWall.setType(Material.GLASS);
                }
            }
        }

        // East and West walls (along Z axis, excluding corners already covered)
        for (int dz = 0; dz <= 1; dz++) {
            for (int dy = 1; dy <= ENCLOSED_HEIGHT; dy++) {
                // West wall (negative X)
                Block westWall = world.getBlockAt(centerX - halfSize, y + dy, centerZ + dz);
                if (westWall.getType() == Material.AIR) {
                    westWall.setType(Material.GLASS);
                }

                // East wall (positive X)
                Block eastWall = world.getBlockAt(centerX + halfSize + 1, y + dy, centerZ + dz);
                if (eastWall.getType() == Material.AIR) {
                    eastWall.setType(Material.GLASS);
                }
            }
        }
    }

    /**
     * Schedules platform deletion based on configuration and player proximity.
     */
    private static void schedulePlatformDeletion(World world, int centerX, int y, int centerZ,
                                                 boolean isEnclosed, Player player) {
        if (!setup.getConfig().getBoolean("delete_platform", false)) {
            return;
        }

        final int effectTicks = setup.getConfig().getInt("effect_duration", 2400);

        // If no player is associated, simply schedule deletion after the configured duration
        if (player == null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    deletePlatform(world, centerX, y, centerZ, isEnclosed);
                }
            }.runTaskLater(setup, effectTicks);
            return;
        }

        // Apply effects immediately while the player is on the platform
        Utilities.applyRevivalPotionEffects(player);
        stopTimer(player); // ensure no countdown until player leaves

        final Location platformCenter = new Location(world, centerX + 0.5, y + 1, centerZ + 0.5);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Cancel task if player went offline
                if (!player.isOnline()) {
                    deletePlatform(world, centerX, y, centerZ, isEnclosed);
                    stopTimer(player);
                    cancel();
                    return;
                }

                // Player still near platform – keep applying effects and wait
                if (isPlayerNearPlatform(player, world, platformCenter)) {
                    Utilities.applyRevivalPotionEffects(player);
                    return;
                }

                // Player has left the platform – start countdown and schedule deletion
                final int acceleratedTicks = Math.max(MIN_ACCELERATED_TICKS, effectTicks / ACCELERATION_DIVISOR);

                stopTimer(player);
                startPlatformTimer(player, acceleratedTicks);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        deletePlatform(world, centerX, y, centerZ, isEnclosed);
                        stopTimer(player);
                    }
                }.runTaskLater(setup, acceleratedTicks);

                cancel();
            }
        }.runTaskTimer(setup, TIMER_INTERVAL_TICKS, TIMER_INTERVAL_TICKS);
    }

    /**
     * Checks if a player is near the platform.
     */
    private static boolean isPlayerNearPlatform(Player player, World world, Location platformCenter) {
        return player.getWorld().equals(world) &&
                player.getLocation().distanceSquared(platformCenter) <= PLATFORM_LEAVE_DISTANCE_SQUARED;
    }

    /**
     * Removes the specified platform from the world.
     */
    private static void deletePlatform(World world, int centerX, int y, int centerZ, boolean isEnclosed) {
        final int halfSize = (PLATFORM_SIZE / 2) - 1;

        if (isEnclosed) {
            // Remove enclosed platform (floor, ceiling, and walls)
            for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
                for (int dz = -halfSize; dz <= halfSize + 1; dz++) {
                    // Remove floor
                    removeBlockIfType(world.getBlockAt(centerX + dx, y, centerZ + dz), Material.OBSIDIAN);

                    // Remove ceiling
                    removeBlockIfType(world.getBlockAt(centerX + dx, y + CEILING_OFFSET, centerZ + dz), Material.OBSIDIAN);

                    // Remove glass walls
                    for (int dy = 1; dy <= ENCLOSED_HEIGHT; dy++) {
                        removeBlockIfType(world.getBlockAt(centerX + dx, y + dy, centerZ + dz), Material.GLASS);
                    }
                }
            }
        } else {
            // Remove simple obsidian platform
            for (int dx = -halfSize; dx <= halfSize + 1; dx++) {
                for (int dz = -halfSize; dz <= halfSize + 1; dz++) {
                    removeBlockIfType(world.getBlockAt(centerX + dx, y, centerZ + dz), Material.OBSIDIAN);
                }
            }
        }
    }

    /**
     * Helper method to remove a block only if it matches the expected type.
     */
    private static void removeBlockIfType(Block block, Material expectedType) {
        if (block.getType() == expectedType) {
            block.setType(Material.AIR);
        }
    }

    // ===== TIMER MANAGEMENT =====

    /**
     * Starts a platform countdown timer for the player.
     * Implementation depends on Utilities ActionBar helper availability.
     */
    private static void startPlatformTimer(Player player, int ticks) {
        stopTimer(player);

        final int[] remaining = {ticks};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                int secondsLeft = Math.max(0, remaining[0] / 20);

                String msg = SafeTeleportManager.createActionBarMessage("Platform", secondsLeft, (double) remaining[0] / ticks);
                try {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(msg));
                } catch (NoSuchMethodError ignored) {
                    player.sendMessage(msg);
                }

                // Play click sound every second
                if (remaining[0] % 20 == 0) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.4F, 1.8F);
                }

                remaining[0] -= 20;
                if (remaining[0] < 0) {
                    cancel();
                }
            }
        }.runTaskTimer(setup, 0L, 20L);

        actionBarTimers.put(player, task);
    }

    /**
     * Stops and removes any active timer for the specified player.
     */
    private static void stopTimer(Player player) {
        BukkitTask task = actionBarTimers.remove(player);
        if (task != null) task.cancel();
    }
}