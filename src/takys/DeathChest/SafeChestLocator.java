package takys.DeathChest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import takys.DeathChest.ChestPlacement;
import takys.Setup;
import takys.Utilities.Utilities;
import takys.Utilities.LiquidPlatformFinder;
import takys.Common.SearchUtils;
import takys.Common.BlockSafety;

/**
 * Optimized utility for finding safe double-chest placement locations.
 * Uses parallel search with early termination for improved performance.
 */
public final class SafeChestLocator {

    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(2);
    private static final int MAX_RADIUS = 20;
    private static final int SEARCH_TIMEOUT_SECONDS = 10;
    private static final double VERTICAL_WEIGHT = 0.5;

    // Precomputed orientation offsets for chest placement
    private static final int[][] CHEST_ORIENTATIONS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    private SafeChestLocator() {}

    /**
     * Finds a safe chest placement near the specified location within the given radius.
     *
     * @param baseLoc The base location to search around
     * @param radius The search radius (capped at 20)
     * @return ChestPlacement if found, null otherwise
     */
    public static ChestPlacement find(Location baseLoc, int radius) {
        radius = Math.min(radius, MAX_RADIUS);

        final boolean debug = Setup.instance.getConfig().getBoolean("debug", false);
        final long startTime = debug ? System.currentTimeMillis() : 0;
        final AtomicInteger checkedCount = debug ? new AtomicInteger() : null;

        // Primary search with parallel threads
        ChestPlacement result = searchParallel(baseLoc, radius, checkedCount, debug);

        // Fallback to liquid platform search if primary search fails
        if (result == null) {
            result = LiquidPlatformFinder.findForChest(baseLoc, radius);
        }

        if (debug) {
            logSearchResults(baseLoc, radius, checkedCount, startTime, result);
        }

        return result;
    }

    /**
     * Performs parallel search using two threads for upward and downward directions.
     */
    private static ChestPlacement searchParallel(Location baseLoc, int radius,
                                                 AtomicInteger checkedCount, boolean debug) {
        final World world = baseLoc.getWorld();
        if (world == null) return null;

        final int baseX = baseLoc.getBlockX();
        final int baseY = baseLoc.getBlockY();
        final int baseZ = baseLoc.getBlockZ();

        final ClosestTracker tracker = new ClosestTracker(baseLoc);
        final CountDownLatch latch = new CountDownLatch(2);

        // Submit upward search task
        THREAD_POOL.submit(() -> {
            try {
                searchVertical(world, baseX, baseY, baseZ, radius, true, checkedCount, tracker, debug);
            } finally {
                latch.countDown();
            }
        });

        // Submit downward search task
        THREAD_POOL.submit(() -> {
            try {
                searchVertical(world, baseX, baseY - 1, baseZ, radius, false, checkedCount, tracker, debug);
            } finally {
                latch.countDown();
            }
        });

        // Wait for completion with timeout
        try {
            latch.await(SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return tracker.getBest();
    }

    /**
     * Searches vertically in the specified direction with spiral rings at each Y level.
     */
    private static void searchVertical(World world, int centerX, int startY, int centerZ,
                                       int radius, boolean searchUp, AtomicInteger checkedCount,
                                       ClosestTracker tracker, boolean debug) {

        final int maxY = Math.min(world.getMaxHeight() - 2, startY + radius * 2);
        final int minY = Math.max(world.getMinHeight() + 1, startY - radius * 2);
        final int step = searchUp ? 1 : -1;

        int currentY = startY;
        while (currentY >= minY && currentY <= maxY && !tracker.isPerfectFound()) {

            // Dynamic radius optimization based on current best distance
            final double verticalComponent = Math.abs(currentY - tracker.getDeathY()) * VERTICAL_WEIGHT;
            final double allowedRadius = Math.max(0, tracker.getBestDistance() - verticalComponent);
            final int effectiveRadius = (int) Math.min(radius, allowedRadius);

            // Search in expanding rings at current Y level
            for (int ringRadius = 0; ringRadius <= effectiveRadius && !tracker.isPerfectFound(); ringRadius++) {
                final int finalY = currentY;
                SearchUtils.forEachRingCoord(centerX, centerZ, ringRadius, (x, z) ->
                        processLocation(world, x, finalY, z, checkedCount, tracker, debug)
                );
            }

            currentY += step;
        }
    }

    /**
     * Processes a single location for chest placement viability.
     */
    private static void processLocation(World world, int x, int y, int z,
                                        AtomicInteger checkedCount, ClosestTracker tracker, boolean debug) {

        // Early distance check to avoid unnecessary processing
        final double horizontalDistance = Math.abs(x - tracker.getDeathX()) + Math.abs(z - tracker.getDeathZ());
        if (horizontalDistance > tracker.getBestDistance()) {
            return;
        }

        final Block primaryBlock = world.getBlockAt(x, y, z);

        // Quick safety check for primary block
        if (!isLocationSafe(primaryBlock)) {
            if (debug) checkedCount.incrementAndGet();
            return;
        }

        if (debug) checkedCount.incrementAndGet();

        // Test all possible chest orientations
        for (int[] offset : CHEST_ORIENTATIONS) {
            final Block secondaryBlock = world.getBlockAt(x + offset[0], y, z + offset[1]);

            if (isLocationSafe(secondaryBlock)) {
                tracker.updateIfCloser(new ChestPlacement(primaryBlock, secondaryBlock));

                if (tracker.isPerfectFound()) {
                    return; // Perfect placement found, stop searching
                }

                // Found valid orientation for this location, no need to test others
                break;
            }
        }
    }

    /**
     * Comprehensive safety check for chest placement location.
     */
    private static boolean isLocationSafe(Block chestBlock) {
        // Must be replaceable
        if (!BlockSafety.isReplaceable(chestBlock)) {
            return false;
        }

        // Check ground safety
        final Block groundBlock = chestBlock.getRelative(0, -1, 0);
        if (!groundBlock.getType().isSolid() || BlockSafety.isDangerous(groundBlock.getType())) {
            return false;
        }

        // Check air space for chest and above
        final Block aboveBlock = chestBlock.getRelative(0, 1, 0);
        if (!chestBlock.getType().isAir() || !aboveBlock.getType().isAir()) {
            return false;
        }

        // Ensure no liquid interference
        return !chestBlock.isLiquid() && !aboveBlock.isLiquid();
    }

    /**
     * Thread-safe tracker for the closest valid chest placement.
     */
    private static final class ClosestTracker {
        private final Location deathLocation;
        private final int deathX;
        private final int deathZ;
        private final int deathY;

        private volatile ChestPlacement bestPlacement = null;
        private volatile double bestDistance = Double.MAX_VALUE;

        ClosestTracker(Location deathLocation) {
            this.deathLocation = deathLocation;
            this.deathX = deathLocation.getBlockX();
            this.deathZ = deathLocation.getBlockZ();
            this.deathY = deathLocation.getBlockY();
        }

        synchronized void updateIfCloser(ChestPlacement placement) {
            final double distance = SearchUtils.weightedDistance(placement.primary.getLocation(), deathLocation);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlacement = placement;
            }
        }

        ChestPlacement getBest() { return bestPlacement; }
        double getBestDistance() { return bestDistance; }
        boolean isPerfectFound() { return bestDistance == 0.0; }
        int getDeathX() { return deathX; }
        int getDeathZ() { return deathZ; }
        int getDeathY() { return deathY; }
    }

    /**
     * Logs comprehensive search results for debugging purposes.
     */
    private static void logSearchResults(Location baseLoc, int radius, AtomicInteger checkedCount,
                                         long startTime, ChestPlacement result) {
        final StringBuilder log = new StringBuilder("\n [SafeChestLocator] Search Summary:\n")
                .append("  Location: (").append(baseLoc.getBlockX()).append(", ")
                .append(baseLoc.getBlockY()).append(", ").append(baseLoc.getBlockZ()).append(")\n")
                .append("  Radius: ").append(radius).append("\n")
                .append("  Locations checked: ").append(checkedCount != null ? checkedCount.get() : "N/A").append("\n")
                .append("  Search time: ").append(System.currentTimeMillis() - startTime).append("ms\n")
                .append("  Result: ");

        if (result == null) {
            log.append("No safe placement found");
        } else {
            log.append("Primary: ").append(formatLocation(result.primary))
                    .append(", Secondary: ").append(formatLocation(result.secondary));
        }

        Utilities.debug(log.toString(), Level.INFO);
    }

    private static String formatLocation(Block block) {
        return String.format("(%d, %d, %d)", block.getX(), block.getY(), block.getZ());
    }
}