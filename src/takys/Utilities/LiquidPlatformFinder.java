package takys.Utilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import takys.DeathChest.ChestPlacement;

/**
 * Utility that, as a last-resort, identifies the crest of a vertical liquid column (water or lava)
 * that is exposed to air and provides enough head-room to either teleport a player or place a
 * double chest. No blocks are placed here – callers are responsible for replacing the crest liquid
 * with the desired platform block once a suitable crest location is returned.
 */
public final class LiquidPlatformFinder {

    // Constants for better readability and maintainability
    private static final int TELEPORT_HEADROOM = 3; // crest + 2 air blocks
    private static final int CHEST_HEADROOM = 2;    // crest + 1 air block
    private static final int MAX_STEP_HEIGHT = 3;   // maximum step up between adjacent blocks
    private static final double LOCATION_CENTER_OFFSET = 0.5;

    // Pre-computed direction arrays to avoid repeated array creation
    private static final int[][] CARDINAL_DIRECTIONS = {{1,0}, {-1,0}, {0,1}, {0,-1}};
    private static final int[][] ALL_DIRECTIONS = {
            {1,0}, {-1,0}, {0,1}, {0,-1},
            {1,1}, {1,-1}, {-1,1}, {-1,-1}
    };

    private LiquidPlatformFinder() {}

    /**
     * Finds a suitable teleport location above a liquid crest.
     * @param baseLoc the base location to search from
     * @param radius the search radius
     * @return Location positioned for player teleport, or null if none found
     */
    public static Location findForTeleport(Location baseLoc, int radius) {
        World world = baseLoc.getWorld();
        if (world == null) return null;

        Crest crest = findLiquidCrest(baseLoc, radius, TELEPORT_HEADROOM);
        if (crest == null) return null;

        return new Location(world,
                crest.x + LOCATION_CENTER_OFFSET,
                crest.y + 1,
                crest.z + LOCATION_CENTER_OFFSET,
                baseLoc.getYaw(),
                baseLoc.getPitch()
        );
    }

    /**
     * Finds a suitable chest placement location, placing glass support block.
     * @param baseLoc the base location to search from
     * @param radius the search radius
     * @return ChestPlacement with primary and secondary blocks, or null if none found
     */
    public static ChestPlacement findForChest(Location baseLoc, int radius) {
        World world = baseLoc.getWorld();
        if (world == null) return null;

        Crest crest = findLiquidCrest(baseLoc, radius, CHEST_HEADROOM);
        if (crest == null) return null;

        // Replace crest liquid with glass for support
        world.getBlockAt(crest.x, crest.y, crest.z).setType(Material.GLASS);

        Block primaryBlock = world.getBlockAt(crest.x, crest.y + 1, crest.z);

        // Find adjacent air block for secondary chest placement
        for (int[] direction : CARDINAL_DIRECTIONS) {
            Block secondaryBlock = world.getBlockAt(
                    crest.x + direction[0],
                    crest.y + 1,
                    crest.z + direction[1]
            );
            if (secondaryBlock.getType() == Material.AIR) {
                return new ChestPlacement(primaryBlock, secondaryBlock);
            }
        }

        return null; // no adjacent air space found
    }

    /**
     * Searches for liquid crests in a spiral pattern outward from the base location.
     */
    private static Crest findLiquidCrest(Location baseLoc, int radius, int headRoomRequired) {
        World world = baseLoc.getWorld();
        int baseX = baseLoc.getBlockX();
        int baseY = baseLoc.getBlockY();
        int baseZ = baseLoc.getBlockZ();

        // Check center first (radius 0)
        Crest centerCrest = checkCrestWithNeighbours(world, baseX, baseY, baseZ, headRoomRequired);
        if (centerCrest != null) return centerCrest;

        // Spiral search outward
        for (int r = 1; r <= radius; r++) {
            Crest crest = searchRing(world, baseX, baseY, baseZ, r, headRoomRequired);
            if (crest != null) return crest;
        }

        return null;
    }

    /**
     * Searches a ring at the specified radius using an optimized spiral pattern.
     */
    private static Crest searchRing(World world, int baseX, int baseY, int baseZ, int radius, int headRoom) {
        // Start from east and move counter-clockwise
        int x = baseX + radius;
        int z = baseZ;

        // East edge (moving south)
        for (int i = 0; i < radius; i++) {
            Crest crest = checkCrestWithNeighbours(world, x, baseY, z + i, headRoom);
            if (crest != null) return crest;
        }

        // South edge (moving west)
        z = baseZ + radius;
        for (int i = 0; i < radius; i++) {
            Crest crest = checkCrestWithNeighbours(world, x - i, baseY, z, headRoom);
            if (crest != null) return crest;
        }

        // West edge (moving north)
        x = baseX - radius;
        for (int i = 0; i < radius; i++) {
            Crest crest = checkCrestWithNeighbours(world, x, baseY, z - i, headRoom);
            if (crest != null) return crest;
        }

        // North edge (moving east)
        z = baseZ - radius;
        for (int i = 0; i < radius; i++) {
            Crest crest = checkCrestWithNeighbours(world, x + i, baseY, z, headRoom);
            if (crest != null) return crest;
        }

        return null;
    }

    /**
     * Checks if a position has a suitable liquid crest, considering neighboring positions
     * for the best (highest) viable option within step height limits.
     */
    private static Crest checkCrestWithNeighbours(World world, int x, int startY, int z, int headRoom) {
        int baseY = findCrestY(world, x, startY, z, headRoom);
        if (baseY == -1) return null;

        int bestY = baseY;
        int bestX = x;
        int bestZ = z;

        // Check all 8 neighboring positions for a better (higher) crest
        for (int[] direction : ALL_DIRECTIONS) {
            int neighborY = findCrestY(world, x + direction[0], startY, z + direction[1], headRoom);
            if (neighborY > bestY && neighborY - baseY <= MAX_STEP_HEIGHT) {
                bestY = neighborY;
                bestX = x + direction[0];
                bestZ = z + direction[1];
            }
        }

        return new Crest(bestX, bestY, bestZ);
    }

    /**
     * Finds the Y coordinate of a liquid crest at the given x,z position.
     * @return crest Y coordinate or -1 if no suitable crest found
     */
    private static int findCrestY(World world, int x, int startY, int z, int headRoomRequired) {
        int minY = world.getMinHeight();

        // Search downward from startY to find liquid
        for (int y = startY; y >= minY; y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();

            if (isLiquid(blockType)) {
                // Find the top of this liquid column
                int crestY = findLiquidTop(world, x, y, z, blockType);

                // Verify we have enough headroom above the crest
                if (hasRequiredHeadroom(world, x, crestY, z, headRoomRequired)) {
                    return crestY;
                }
            }
        }

        return -1; // no suitable liquid crest found
    }

    /**
     * Finds the topmost block of a liquid column of the same type.
     */
    private static int findLiquidTop(World world, int x, int startY, int z, Material liquidType) {
        int maxY = world.getMaxHeight() - 1;
        int topY = startY;

        // Move up while we're still in the same liquid type
        while (topY < maxY && world.getBlockAt(x, topY + 1, z).getType() == liquidType) {
            topY++;
        }

        return topY;
    }

    /**
     * Verifies that there are enough air blocks above the specified position.
     */
    private static boolean hasRequiredHeadroom(World world, int x, int crestY, int z, int headRoomRequired) {
        int maxY = world.getMaxHeight();

        // Check if we have room for the required headroom
        if (crestY + headRoomRequired >= maxY) {
            return false;
        }

        // Verify all required blocks above crest are air
        for (int dy = 1; dy <= headRoomRequired; dy++) {
            if (world.getBlockAt(x, crestY + dy, z).getType() != Material.AIR) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a material is a liquid (water or lava).
     */
    private static boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    /**
         * Represents the coordinates of a liquid crest.
         */
        private record Crest(int x, int y, int z) {

        @Override
            public String toString() {
                return String.format("Crest{x=%d, y=%d, z=%d}", x, y, z);
            }
        }
}