package takys.Common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.bukkit.Location;

/**
 * General search/geometry helpers shared by SafeTeleportManager, SafeChestLocator, etc.
 * Provides efficient spiral search patterns and distance calculations for Minecraft world operations.
 */
public final class SearchUtils {

    private SearchUtils() {}

    /**
     * Iterates the perimeter of a square spiral ring at the given radius around (cx,cz).
     * The order matches vanilla Minecraft chunk-spiral: start East, go South, West, then North.
     * Each coordinate pair is supplied to the provided consumer.
     *
     * @param cx center X coordinate
     * @param cz center Z coordinate
     * @param radius ring radius (0 for center point only)
     * @param consumer function to process each coordinate pair
     */
    public static void forEachRingCoord(int cx, int cz, int radius, BiConsumer<Integer, Integer> consumer) {
        if (radius == 0) {
            consumer.accept(cx, cz);
            return;
        }

        // Pre-calculate bounds to avoid repeated arithmetic
        final int minOffset = -radius;
        final int maxOffset = radius;
        final int eastX = cx + radius;
        final int westX = cx - radius;
        final int southZ = cz + radius;
        final int northZ = cz - radius;

        // East edge (including south-east corner)
        for (int dz = minOffset + 1; dz <= maxOffset; dz++) {
            consumer.accept(eastX, cz + dz);
        }

        // South edge (excluding south-east corner)
        for (int dx = maxOffset - 1; dx >= minOffset; dx--) {
            consumer.accept(cx + dx, southZ);
        }

        // West edge (excluding south-west corner)
        for (int dz = maxOffset - 1; dz >= minOffset; dz--) {
            consumer.accept(westX, cz + dz);
        }

        // North edge (excluding both corners)
        for (int dx = minOffset + 1; dx < maxOffset; dx++) {
            consumer.accept(cx + dx, northZ);
        }
    }

    /**
     * Returns a list of coordinate pairs for the ring at the specified radius.
     * Pre-allocates the list with exact capacity for better memory efficiency.
     *
     * @param cx center X coordinate
     * @param cz center Z coordinate
     * @param radius ring radius
     * @return list of [x,z] coordinate pairs
     */
    public static List<int[]> ringCoords(int cx, int cz, int radius) {
        if (radius == 0) {
            List<int[]> singlePoint = new ArrayList<>(1);
            singlePoint.add(new int[]{cx, cz});
            return singlePoint;
        }

        // Calculate exact capacity: perimeter = 8 * radius for a square ring
        final int capacity = 8 * radius;
        List<int[]> coords = new ArrayList<>(capacity);

        forEachRingCoord(cx, cz, radius, (x, z) -> coords.add(new int[]{x, z}));
        return coords;
    }

    /**
     * Calculates weighted distance used by teleport/chest logic.
     * Formula: horizontal_distance + vertical_distance * 0.5
     *
     * This weighting makes horizontal movement preferred over vertical movement,
     * which is typically more expensive in Minecraft due to fall damage and climbing.
     *
     * @param loc1 first location
     * @param loc2 second location
     * @return weighted distance between the locations
     */
    public static double weightedDistance(Location loc1, Location loc2) {
        final double dx = loc1.getX() - loc2.getX();
        final double dy = loc1.getY() - loc2.getY();
        final double dz = loc1.getZ() - loc2.getZ();

        // Use direct calculation instead of Math.sqrt for better performance
        // when only relative distances matter
        final double horizontalSq = dx * dx + dz * dz;
        final double horizontal = Math.sqrt(horizontalSq);

        return horizontal + Math.abs(dy) * 0.5;
    }

    /**
     * Calculates squared weighted distance for cases where relative comparison is sufficient.
     * Avoids the expensive square root operation for better performance.
     *
     * @param loc1 first location
     * @param loc2 second location
     * @return squared weighted distance (for comparison purposes only)
     */
    public static double weightedDistanceSquared(Location loc1, Location loc2) {
        final double dx = loc1.getX() - loc2.getX();
        final double dy = loc1.getY() - loc2.getY();
        final double dz = loc1.getZ() - loc2.getZ();

        final double horizontalSq = dx * dx + dz * dz;
        final double verticalWeighted = Math.abs(dy) * 0.5;

        // Return approximation: horizontal² + (vertical_weighted)²
        // Note: This isn't mathematically equivalent to (horizontal + vertical_weighted)²
        // but maintains relative ordering for comparison purposes
        return horizontalSq + verticalWeighted * verticalWeighted;
    }
}