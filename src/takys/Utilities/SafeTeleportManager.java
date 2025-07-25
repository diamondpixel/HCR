package takys.Utilities;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import takys.Objects.PlayerObj;
import takys.Setup;
import takys.Utilities.Utilities;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import takys.Common.SearchUtils;
import takys.Common.BlockSafety;
import takys.Common.PlatformBuilder;

/**
 * SafeTeleportManager handles finding safe teleportation locations near death points.
 * Uses advanced distance-based searching with multi-threaded optimization.
 * Includes integrated ActionBar timer for platform deletion countdown.
 */
public class SafeTeleportManager {

    private static final Setup setup = Setup.instance;

    // Search constants
    private static final int DEFAULT_SEARCH_RADIUS = 20;
    private static final int SEARCH_TIMEOUT_SECONDS = 10;
    private static final double VERY_CLOSE_DISTANCE = 2.0;
    private static final double VERTICAL_WEIGHT = 0.5;

    // ActionBar Timer constants
    private static final ConcurrentHashMap<Player, BukkitTask> activeTimers = new ConcurrentHashMap<>();
    private static final int BAR_LENGTH = 10;
    private static final String BAR_CHAR = "█";

    // Reusable thread pool for better performance
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "SafeTeleport-Search");
        t.setDaemon(true);
        return t;
    });

    /**
     * Thread-safe container for tracking the closest safe location found.
     * Uses volatile fields for better performance than synchronized blocks on every read.
     */
    private static class ClosestLocationTracker {
        private volatile Location closestLocation = null;
        private volatile double closestDistance = Double.MAX_VALUE;

        /**
         * Updates the closest location if the new location is closer.
         * Returns true if this location was set as the new closest.
         */
        public boolean updateIfCloser(Location newLocation, Location deathLocation) {
            if (newLocation == null) return false;

            double distance = SearchUtils.weightedDistance(newLocation, deathLocation);

            // Quick check without synchronization
            if (distance >= closestDistance) return false;

            // Only synchronize when we need to update
            synchronized (this) {
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestLocation = newLocation.clone();
                    return true;
                }
            }
            return false;
        }

        public Location getClosestLocation() {
            Location loc = closestLocation;
            return loc != null ? loc.clone() : null;
        }

        public double getClosestDistance() {
            return closestDistance;
        }
    }

    /**
     * Attempts to find a safe teleport location near the provided position.
     * Uses a distance-based search with two async threads to find the absolute closest safe spot.
     *
     * @param baseLoc The death/base location to search around
     * @param searchRadius The radius to search within (use -1 for config default)
     * @param player Optional player to show timer for platform deletion (can be null)
     * @return A safe location to teleport to
     */
    public static Location findSafeTeleportLocation(Location baseLoc, int searchRadius, Player player) {
        World world = baseLoc.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Base location world cannot be null");
        }

        // Pre-calculate coordinates once
        final int bx = baseLoc.getBlockX();
        final int by = baseLoc.getBlockY();
        final int bz = baseLoc.getBlockZ();

        // Initialize debug tracking
        final boolean debug = Setup.instance.getConfig().getBoolean("debug", false);
        final long startTime = debug ? System.currentTimeMillis() : 0;
        final AtomicInteger checked = debug ? new AtomicInteger() : null;

        // Track the closest safe location found
        ClosestLocationTracker tracker = new ClosestLocationTracker();
        CountDownLatch searchComplete = new CountDownLatch(2);

        try {
            // Submit both search tasks
            CompletableFuture<Void> upwardSearch = CompletableFuture.runAsync(() -> {
                try {
                    searchVerticalDirection(world, bx, by, bz, searchRadius, baseLoc, tracker, true, checked);
                } finally {
                    searchComplete.countDown();
                }
            }, SEARCH_EXECUTOR);

            CompletableFuture<Void> downwardSearch = CompletableFuture.runAsync(() -> {
                try {
                    searchVerticalDirection(world, bx, by, bz, searchRadius, baseLoc, tracker, false, checked);
                } finally {
                    searchComplete.countDown();
                }
            }, SEARCH_EXECUTOR);

            // Wait for both searches with timeout
            boolean completed = searchComplete.await(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Cancel any still-running tasks
            if (!completed) {
                upwardSearch.cancel(true);
                downwardSearch.cancel(true);
            }

            Location closestLocation = tracker.getClosestLocation();
            if (closestLocation != null) {
                logDebugResult(debug, "search", bx, by, bz, world, searchRadius, checked,
                        startTime, closestLocation, tracker.getClosestDistance(), completed);
                return closestLocation;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Fallback 1: Try liquid platform finder
        Location crestLoc = takys.Utilities.LiquidPlatformFinder.findForTeleport(baseLoc, searchRadius);
        if (crestLoc != null) {
            PlatformBuilder.createObsidianPlatform(world, crestLoc.getBlockX(), crestLoc.getBlockY() - 1, crestLoc.getBlockZ(), false, player);

            if (debug) {
                logLiquidFallbackDebug(bx, by, bz, crestLoc, searchRadius, checked, startTime);
            }
            return crestLoc;
        }

        // Fallback 2: Generate appropriate platform
        return createFallbackPlatform(world, bx, by, bz, baseLoc, player, debug, startTime, searchRadius, checked);
    }

    /**
     * Overloaded method using default search radius from config.
     */
    public static Location findSafeTeleportLocation(PlayerObj pObj) {
        int configRadius = setup.getConfig().getInt("safe_teleport_search_radius", DEFAULT_SEARCH_RADIUS);
        int searchRadius = Math.max(configRadius, DEFAULT_SEARCH_RADIUS); // Ensure minimum radius

        if (configRadius < DEFAULT_SEARCH_RADIUS) {
            setup.getConfig().set("safe_teleport_search_radius", DEFAULT_SEARCH_RADIUS);
            setup.saveConfig();
        }

        return findSafeTeleportLocation(pObj.getLoc(), searchRadius, pObj.getPlayer());
    }

    /**
     * Searches in a vertical direction (up or down) from the base location.
     * Optimized with early termination and distance checking.
     */
    private static void searchVerticalDirection(World world, int centerX, int centerY, int centerZ,
                                                int radius, Location baseLoc, ClosestLocationTracker tracker,
                                                boolean searchUpward, AtomicInteger checked) {

        final int startY = searchUpward ? centerY : centerY - 1;
        final int endY = searchUpward ? Math.min(world.getMaxHeight() - 2, centerY + radius * 2)
                : Math.max(world.getMinHeight() + 1, centerY - radius * 2);
        final int step = searchUpward ? 1 : -1;

        // Search each Y level
        for (int y = startY; searchUpward ? (y <= endY) : (y >= endY); y += step) {
            // Early termination if we've found something very close
            if (tracker.getClosestDistance() < VERY_CLOSE_DISTANCE) {
                break;
            }

            // Check for thread interruption
            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            // Search in expanding spirals at this Y level
            searchAtYLevel(world, centerX, centerZ, y, radius, baseLoc, tracker, checked);
        }
    }

    /**
     * Searches at a specific Y level using expanding spirals.
     * Separated for better code organization and potential future optimizations.
     */
    private static void searchAtYLevel(World world, int centerX, int centerZ, int y, int radius,
                                       Location baseLoc, ClosestLocationTracker tracker, AtomicInteger checked) {

        for (int spiralRadius = 0; spiralRadius <= radius; spiralRadius++) {
            // Distance-based early termination
            double minPossibleDistance = spiralRadius + Math.abs(y - baseLoc.getBlockY()) * VERTICAL_WEIGHT;
            if (tracker.getClosestDistance() <= minPossibleDistance) {
                break;
            }

            SearchUtils.forEachRingCoord(centerX, centerZ, spiralRadius, (x, z) -> {
                if (checked != null) checked.incrementAndGet();

                Location safeLoc = checkSafeLocation(world, x, y, z, baseLoc);
                if (safeLoc != null) {
                    tracker.updateIfCloser(safeLoc, baseLoc);
                }
            });
        }
    }

    /**
     * Optimized safe location check with early returns.
     */
    private static Location checkSafeLocation(World world, int x, int y, int z, Location baseLoc) {
        // Bounds check first (cheapest operation)
        if (y < world.getMinHeight() + 1 || y > world.getMaxHeight() - 2) {
            return null;
        }

        try {
            // Get blocks once and reuse
            Block belowBlock = world.getBlockAt(x, y - 1, z);
            Block feetBlock = world.getBlockAt(x, y, z);
            Block headBlock = world.getBlockAt(x, y + 1, z);

            // Fast liquid checks
            if (belowBlock.isLiquid() || feetBlock.isLiquid() || headBlock.isLiquid()) {
                return null;
            }

            // Solid ground check
            if (!belowBlock.getType().isSolid()) {
                return null;
            }

            // Air space check
            if (!BlockSafety.isReplaceable(feetBlock) || !BlockSafety.isReplaceable(headBlock)) {
                return null;
            }

            // Danger check (most expensive, do last)
            if (BlockSafety.isDangerous(belowBlock.getType())) {
                return null;
            }

            return new Location(world, x + 0.5, y, z + 0.5, baseLoc.getYaw(), baseLoc.getPitch());

        } catch (Exception e) {
            // Handle world loading issues gracefully
            return null;
        }
    }

    /**
     * Creates fallback platform with optimized liquid detection.
     */
    private static Location createFallbackPlatform(World world, int bx, int by, int bz, Location baseLoc,
                                                   Player player, boolean debug, long startTime,
                                                   int searchRadius, AtomicInteger checked) {

        int platformY = Math.max(world.getMinHeight() + 5, by);
        PlatformBuilder.createObsidianPlatform(world, bx, platformY - 1, bz, false, player);
        if (debug) {
            logFallbackDebug(bx, by, bz, world, searchRadius, checked, startTime, platformY);
        }

        return new Location(world, bx + 0.5, platformY, bz + 0.5, baseLoc.getYaw(), baseLoc.getPitch());
    }

    /**
     * Optimized liquid detection with early termination.
     */
    private static boolean hasLiquidsInArea(World world, int centerX, int y, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    try {
                        if (world.getBlockAt(centerX + dx, y + dy, centerZ + dz).isLiquid()) {
                            return true; // Early termination
                        }
                    } catch (Exception ignored) {
                        // Handle chunk loading issues
                    }
                }
            }
        }
        return false;
    }

    // ==================== TIMER METHODS ====================

    /**
     * Starts a countdown timer for a player with action bar display.
     */
    public static void startTimer(Player player, int totalTicks, String timerName) {
        stopTimer(player); // Cancel any existing timer

        BukkitTask task = new BukkitRunnable() {
            private int remainingTicks = totalTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeTimers.remove(player);
                    return;
                }

                int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
                double progress = (double) remainingTicks / totalTicks;

                String actionBarMessage = createActionBarMessage(timerName, remainingSeconds, progress);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));

                if (--remainingTicks < 0) {
                    cancel();
                    activeTimers.remove(player);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ChatColor.RED + "⚠ " + timerName + " Complete! ⚠"));
                }
            }
        }.runTaskTimer(setup, 0L, 1L);

        activeTimers.put(player, task);
    }

    /**
     * Stops the timer for a specific player.
     */
    public static void stopTimer(Player player) {
        BukkitTask existingTask = activeTimers.remove(player);
        if (existingTask != null) {
            existingTask.cancel();
        }
    }

    /**
     * Stops all active timers.
     */
    public static void stopAllTimers() {
        activeTimers.values().forEach(BukkitTask::cancel);
        activeTimers.clear();
    }

    /**
     * Checks if a player has an active timer.
     */
    public static boolean hasActiveTimer(Player player) {
        return activeTimers.containsKey(player);
    }

    /**
     * Creates the formatted action bar message with progress bar and countdown.
     */
    public static String createActionBarMessage(String timerName, int remainingSeconds, double progress) {
        // Calculate colors
        ChatColor fillColor = progress > 0.66 ? ChatColor.GREEN :
                progress > 0.33 ? ChatColor.YELLOW : ChatColor.RED;

        // Build progress bar
        int filledSegments = (int) Math.round(BAR_LENGTH * progress);
        StringBuilder progressBar = new StringBuilder()
                .append(ChatColor.DARK_GRAY).append("[")
                .append(fillColor).append(BAR_CHAR.repeat(Math.max(0, filledSegments)))
                .append(ChatColor.GRAY).append(BAR_CHAR.repeat(Math.max(0, BAR_LENGTH - filledSegments)))
                .append(ChatColor.DARK_GRAY).append("]");

        // Format time
        String timeDisplay = remainingSeconds >= 60 ?
                String.format("%dm %ds", remainingSeconds / 60, remainingSeconds % 60) :
                remainingSeconds + "s";

        return ChatColor.GRAY + "⏱ " + progressBar + ChatColor.RESET + " " + fillColor + timeDisplay;
    }

    // ==================== DEBUG METHODS ====================

    /**
     * Consolidated debug logging for main search result.
     */
    private static void logDebugResult(boolean debug, String phase, int bx, int by, int bz, World world,
                                       int searchRadius, AtomicInteger checked, long startTime,
                                       Location result, double distance, boolean completed) {
        if (!debug) return;

        StringBuilder dbg = new StringBuilder("\n [SafeTeleportManager] [").append(phase).append("]\n")
                .append(" • baseLoc : (").append(bx).append(',').append(by).append(',').append(bz)
                .append(") world=").append(world.getName()).append('\n')
                .append(" • radius  : ").append(searchRadius).append('\n')
                .append(" • worldY  : ").append(world.getMinHeight()).append("..").append(world.getMaxHeight()).append('\n')
                .append(" • checked : ").append(checked != null ? checked.get() : -1).append('\n')
                .append(" • time    : ").append(System.currentTimeMillis() - startTime).append("ms\n")
                .append(" • threads : 2 async completed=").append(completed).append('\n');

        if (result == null) {
            dbg.append(" • result  : NONE");
        } else {
            dbg.append(" • result  : (").append(result.getBlockX()).append(',')
                    .append(result.getBlockY()).append(',').append(result.getBlockZ())
                    .append(") dist=").append(String.format("%.2f", distance));
        }

        Utilities.debug(dbg.toString(), Level.INFO);
    }

    /**
     * Debug logging for liquid fallback.
     */
    private static void logLiquidFallbackDebug(int bx, int by, int bz, Location crestLoc,
                                               int searchRadius, AtomicInteger checked, long startTime) {
        StringBuilder dbg = new StringBuilder("\n [SafeTeleportManager] [liquid_fallback]\n")
                .append(" • baseLoc : (").append(bx).append(',').append(by).append(',').append(bz).append(")\n")
                .append(" • crest   : (").append(crestLoc.getBlockX()).append(',')
                .append(crestLoc.getBlockY()).append(',').append(crestLoc.getBlockZ())
                .append(") diffY=").append(crestLoc.getBlockY() - by).append('\n')
                .append(" • radius  : ").append(searchRadius).append('\n')
                .append(" • checked : ").append(checked != null ? checked.get() : -1).append('\n')
                .append(" • platform: obsidian_pad4x4\n")
                .append(" • time    : ").append(System.currentTimeMillis() - startTime).append("ms");

        Utilities.debug(dbg.toString(), Level.INFO);
    }

    /**
     * Debug logging for generic fallback.
     */
    private static void logFallbackDebug(int bx, int by, int bz, World world, int searchRadius,
                                         AtomicInteger checked, long startTime, int platformY) {
        StringBuilder dbg = new StringBuilder("\n [SafeTeleportManager] [fallback]\n")
                .append(" • baseLoc : (").append(bx).append(',').append(by).append(',').append(bz)
                .append(") world=").append(world.getName()).append('\n')
                .append(" • radius  : ").append(searchRadius).append('\n')
                .append(" • worldY  : ").append(world.getMinHeight()).append("..").append(world.getMaxHeight()).append('\n')
                .append(" • checked : ").append(checked != null ? checked.get() : -1).append('\n')
                .append(" • platformType : obsidian\n")
                .append(" • platformY    : ").append(platformY).append('\n')
                .append(" • time    : ").append(System.currentTimeMillis() - startTime).append("ms\n")
                .append(" • result  : teleported to platform centre");

        Utilities.debug(dbg.toString(), Level.INFO);
    }
}