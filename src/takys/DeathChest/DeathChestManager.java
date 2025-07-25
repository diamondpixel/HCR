package takys.DeathChest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import takys.Objects.PlayerObj;
import takys.Setup;
import takys.DeathChest.SafeChestLocator;
import takys.DeathChest.ChestPlacement;
import takys.Utilities.Utilities;
import takys.Common.BlockSafety;
import takys.Common.PlatformBuilder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class that spawns a double chest at a player's death location and stores their items inside.
 * The chest placement follows specific rules based on death conditions and searches for valid placement locations.
 */
public final class DeathChestManager {

    // Constants for better maintainability
    private static final int DEFAULT_SEARCH_RADIUS = 20;
    private static final int NETHER_CEILING_MIN_Y = 124;
    private static final int NETHER_CEILING_MAX_Y = 127;
    private static final int NETHER_BEDROCK_SCAN_MIN = 100;
    private static final int BEDROCK_CLEARANCE = 5;
    private static final int MAX_LIQUID_SCAN_HEIGHT = 64;
    private static final int MAX_SEARCH_HEIGHT_OFFSET = 16;
    private static final int AIR_GAP_THRESHOLD = 16;
    private static final int VERTICAL_SEARCH_MULTIPLIER = 2;

    // Pre-computed chest orientation offsets
    private static final int[][] CHEST_OFFSETS = {{1,0}, {-1,0}, {0,1}, {0,-1}};

    // Cache for repeated config access
    private static Boolean debugEnabled = null;
    private static Integer searchRadius = null;

    private DeathChestManager() {}

    /**
     * Creates a double chest at the appropriate location based on death conditions and fills it with the supplied items.
     * Any items that cannot fit will be naturally dropped on the ground.
     */
    public static void createDeathChest(PlayerObj playerObj, List<ItemStack> items) {
        if (!isValidInput(playerObj, items)) return;

        Location deathLoc = playerObj.getLoc();
        World world = deathLoc.getWorld();
        if (world == null) return;

        boolean debug = isDebugEnabled();
        StringBuilder dbg = debug ? new StringBuilder("\n [DeathChestManager] [create]\n") : null;
        long startTime = debug ? System.currentTimeMillis() : 0;

        // Determine center block based on death conditions
        Location centerLoc = determineCenterLocation(deathLoc, world);

        if (debug) {
            dbg.append(" • center : (").append(centerLoc.getBlockX())
                    .append(',').append(centerLoc.getBlockY())
                    .append(',').append(centerLoc.getBlockZ()).append(")\n");
        }

        // Find valid placement for double chest
        ChestPlacement placement = findValidChestPlacement(centerLoc, world);
        if (placement == null) {
            debugLog(debug, dbg, " • result  : NONE\n");
            return;
        }

        if (debug) {
            dbg.append(" • result  : (").append(placement.primary.getX())
                    .append(',').append(placement.primary.getY())
                    .append(',').append(placement.primary.getZ())
                    .append(") & (").append(placement.secondary.getX())
                    .append(',').append(placement.secondary.getY())
                    .append(',').append(placement.secondary.getZ()).append(")\n");
        }

        // Setup chest environment
        setupChestEnvironment(placement, world, playerObj.getDamageCause(), debug, dbg);

        // Fill chest and handle overflow
        int stored = fillChestWithItems(placement, items);

        if (debug) {
            dbg.append(" • items   : ").append(stored).append(" stored\n")
                    .append(" • time    : ").append(System.currentTimeMillis() - startTime).append("ms\n");
            Utilities.debug(dbg.toString(), Level.INFO);
        }
    }

    private static boolean isValidInput(PlayerObj playerObj, List<ItemStack> items) {
        return playerObj != null && items != null && !items.isEmpty();
    }

    private static boolean isDebugEnabled() {
        if (debugEnabled == null) {
            debugEnabled = Setup.instance.getConfig().getBoolean("debug", false);
        }
        return debugEnabled;
    }

    private static int getSearchRadius()
    {
        if (searchRadius == null)
        {
            searchRadius = Math.max(1, Setup.instance.getConfig().getInt("death_chest_search_radius", DEFAULT_SEARCH_RADIUS));
        }

        if (searchRadius < 20)
        {
            searchRadius = DEFAULT_SEARCH_RADIUS;
            Setup.instance.getConfig().set("death_chest_search_radius", DEFAULT_SEARCH_RADIUS);
            Setup.instance.saveConfig();
        }
        return searchRadius;
    }

    private static void debugLog(boolean debug, StringBuilder dbg, String message) {
        if (debug && dbg != null) {
            dbg.append(message);
            Utilities.debug(dbg.toString(), Level.INFO);
        }
    }

    /**
     * Sets up the complete chest environment including placement, clearing, platform, and cross.
     */
    private static void setupChestEnvironment(ChestPlacement placement, World world, DamageCause damageCause, boolean debug, StringBuilder dbg) {
        // Clear area around chest
        clearAreaAroundChest(placement, world);

        // Place and configure the double chest
        if (!placeAndConfigureChest(placement)) {
            debugLog(debug, dbg, " • error   : failed to configure double chest blocks\n");
            return;
        }

        if (debug) dbg.append(" • chest   : placed\n");

        // Build platform if needed
        boolean voidDeath = damageCause == DamageCause.VOID;
        boolean unsupported = !hasSolidSupport(placement.primary, placement.secondary);
        boolean needsPlatform = voidDeath || unsupported;

        if (debug) {
            dbg.append(" • void    : ").append(voidDeath)
                    .append("  unsupported:").append(unsupported).append('\n');
        }

        buildGlassPlatformIfNeeded(placement, world, needsPlatform);

        if (debug) {
            dbg.append(" • platform: ").append(needsPlatform ? "built" : "not needed").append('\n');
        }

        // Build decorative cross
        buildStoneFenceCross(placement, world);
    }

    private static boolean placeAndConfigureChest(ChestPlacement placement) {
        // Set both blocks to chest without physics
        placement.primary.setType(Material.CHEST, false);
        placement.secondary.setType(Material.CHEST, false);

        return configureDoubleChest(placement);
    }

    private static int fillChestWithItems(ChestPlacement placement, List<ItemStack> items) {
        Chest chestState = (Chest) placement.primary.getState();
        Inventory inv = chestState.getInventory();
        World world = placement.primary.getWorld();
        Location dropLoc = placement.primary.getLocation().add(0.5, 1, 0.5);

        int stored = 0;
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) continue;

            Map<Integer, ItemStack> leftover = inv.addItem(stack);

            // Drop leftovers if chest is full
            for (ItemStack remaining : leftover.values()) {
                world.dropItemNaturally(dropLoc, remaining);
            }

            // Calculate stored amount efficiently
            int leftoverAmount = leftover.values().stream().mapToInt(ItemStack::getAmount).sum();
            stored += stack.getAmount() - leftoverAmount;
        }

        return stored;
    }

    /**
     * Determines the center location for chest placement based on death conditions.
     */
    private static Location determineCenterLocation(Location deathLoc, World world) {
        Location centerLoc = deathLoc.clone();

        // Handle void death
        if (deathLoc.getY() < world.getMinHeight()) {
            centerLoc.setY(world.getMinHeight() + BEDROCK_CLEARANCE);
        }

        // Handle Nether bedrock ceiling abuse prevention
        centerLoc = handleNetherCeiling(centerLoc, world);

        // Handle liquid death - find surface
        centerLoc = handleLiquidDeath(centerLoc, world);

        return centerLoc;
    }

    private static Location handleNetherCeiling(Location centerLoc, World world) {
        if (world.getEnvironment() != World.Environment.NETHER) return centerLoc;

        double y = centerLoc.getY();
        if (y < NETHER_CEILING_MIN_Y || y > NETHER_CEILING_MAX_Y) return centerLoc;

        int bedrockCeiling = findNetherBedrockCeiling(centerLoc, world);
        if (bedrockCeiling > 0) {
            centerLoc.setY(bedrockCeiling - BEDROCK_CLEARANCE);
        }

        return centerLoc;
    }

    private static Location handleLiquidDeath(Location centerLoc, World world) {
        Block deathBlock = world.getBlockAt(centerLoc);
        if (!isLiquidBlock(deathBlock)) return centerLoc;

        int surfaceY = findLiquidSurface(centerLoc, world);
        centerLoc.setY(surfaceY);
        return centerLoc;
    }

    private static boolean isLiquidBlock(Block block) {
        return block.isLiquid() || block.getType() == Material.LAVA;
    }

    private static int findLiquidSurface(Location loc, World world) {
        int startY = loc.getBlockY();
        int maxHeight = Math.min(world.getMaxHeight() - 1, startY + MAX_LIQUID_SCAN_HEIGHT);

        for (int y = startY; y <= maxHeight; y++) {
            Block checkBlock = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (!isLiquidBlock(checkBlock)) {
                return y;
            }
        }
        return startY; // Fallback if no surface found
    }

    /**
     * Finds the Y level of the Nether bedrock ceiling by scanning downward from world height.
     */
    private static int findNetherBedrockCeiling(Location loc, World world) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        for (int y = world.getMaxHeight() - 1; y >= NETHER_BEDROCK_SCAN_MIN; y--) {
            if (world.getBlockAt(x, y, z).getType() == Material.BEDROCK) {
                return findBedrockLayerBottom(world, x, z, y);
            }
        }
        return -1;
    }

    private static int findBedrockLayerBottom(World world, int x, int z, int startY) {
        for (int y = startY; y >= NETHER_BEDROCK_SCAN_MIN; y--) {
            if (world.getBlockAt(x, y, z).getType() != Material.BEDROCK) {
                return y + 1;
            }
        }
        return startY;
    }

    /**
     * Finds a valid placement for a double chest starting from the center location.
     */
    private static ChestPlacement findValidChestPlacement(Location centerLoc, World world) {
        // Try modern SafeChestLocator first
        ChestPlacement placement = SafeChestLocator.find(centerLoc, getSearchRadius());
        if (placement != null) return placement;

        // Fallback to legacy search - prioritize supported placements
        placement = searchForPlacement(centerLoc, world, true);
        if (placement != null) return placement;

        return searchForPlacement(centerLoc, world, false);
    }

    private static ChestPlacement searchForPlacement(Location centerLoc, World world, boolean requireSupport) {
        int startY = centerLoc.getBlockY();
        int maxY = Math.min(world.getMaxHeight() - 2, startY + MAX_SEARCH_HEIGHT_OFFSET);

        // 1. Try vertical column at center first
        ChestPlacement placement = searchVerticalColumn(world, centerLoc, startY, maxY, requireSupport);
        if (placement != null) return placement;

        // 2. Spiral search in XZ plane
        return searchSpiral(world, centerLoc, startY, maxY, requireSupport);
    }

    private static ChestPlacement searchVerticalColumn(World world, Location centerLoc, int startY, int maxY, boolean requireSupport) {
        int x = centerLoc.getBlockX();
        int z = centerLoc.getBlockZ();

        for (int y = startY; y <= maxY; y++) {
            Block centerBlock = world.getBlockAt(x, y, z);
            for (int[] offset : CHEST_OFFSETS) {
                Block primary = centerBlock;
                Block secondary = world.getBlockAt(x + offset[0], y, z + offset[1]);

                if (isValidChestPlacement(primary, secondary, requireSupport)) {
                    return new ChestPlacement(primary, secondary);
                }
            }
        }
        return null;
    }

    private static ChestPlacement searchSpiral(World world, Location centerLoc, int startY, int maxY, boolean requireSupport) {
        int radius = getSearchRadius();
        int centerX = centerLoc.getBlockX();
        int centerZ = centerLoc.getBlockZ();

        for (int r = 1; r <= radius; r++) {
            List<int[]> coordinates = generateSpiralCoordinatesAtRadius(centerX, centerZ, r);
            for (int[] coord : coordinates) {
                for (int[] offset : CHEST_OFFSETS) {
                    ChestPlacement placement = findVerticalPlacementAt(
                            world, coord[0], coord[1], offset, startY, maxY, requireSupport, r
                    );
                    if (placement != null) return placement;
                }
            }
        }
        return null;
    }

    private static boolean isValidChestPlacement(Block primary, Block secondary, boolean requireSupport) {
        return BlockSafety.isReplaceable(primary) &&
                BlockSafety.isReplaceable(secondary) &&
                (!requireSupport || hasSolidSupport(primary, secondary));
    }

    private static ChestPlacement findVerticalPlacementAt(World world, int baseX, int baseZ, int[] offset,
                                                          int startY, int maxY, boolean requireSupport, int radius) {
        int minY = Math.max(world.getMinHeight() + 1, startY - radius * VERTICAL_SEARCH_MULTIPLIER);

        for (int dy = 0; dy <= Math.max(maxY - startY, startY - minY); dy++) {
            int[] yCandidates = {startY - dy, startY + dy};
            for (int y : yCandidates) {
                if (y < minY || y > maxY) continue;

                Block primary = world.getBlockAt(baseX, y, baseZ);
                Block secondary = world.getBlockAt(baseX + offset[0], y, baseZ + offset[1]);

                if (isValidChestPlacement(primary, secondary, requireSupport)) {
                    return new ChestPlacement(primary, secondary);
                }
            }
        }
        return null;
    }

    /**
     * Generates spiral ring coordinates for chest placement search.
     */
    private static List<int[]> generateSpiralCoordinatesAtRadius(int centerX, int centerZ, int radius) {
        List<int[]> coords = new ArrayList<>();
        if (radius == 0) {
            coords.add(new int[]{centerX, centerZ});
            return coords;
        }

        // Generate spiral coordinates more efficiently
        addSpiralEdge(coords, centerX, centerZ, radius, 1, 0);   // Right edge (down)
        addSpiralEdge(coords, centerX, centerZ, radius, 0, -1);  // Bottom edge (left)
        addSpiralEdge(coords, centerX, centerZ, radius, -1, 0);  // Left edge (up)
        addSpiralEdge(coords, centerX, centerZ, radius, 0, 1);   // Top edge (right, incomplete)

        return coords;
    }

    private static void addSpiralEdge(List<int[]> coords, int centerX, int centerZ, int radius, int dx, int dz) {
        if (dx != 0) { // Vertical edge
            int x = centerX + (dx > 0 ? radius : -radius);
            int startZ = dx > 0 ? -radius + 1 : radius - 1;
            int endZ = dx > 0 ? radius : -radius;
            int step = dx > 0 ? 1 : -1;

            for (int z = startZ; (step > 0 ? z <= endZ : z >= endZ); z += step) {
                coords.add(new int[]{x, centerZ + z});
            }
        } else { // Horizontal edge
            int z = centerZ + (dz < 0 ? radius : -radius);
            int startX = dz < 0 ? radius - 1 : -radius + 1;
            int endX = dz < 0 ? -radius : radius - 1;
            int step = dz < 0 ? -1 : 1;

            for (int x = startX; (step > 0 ? x <= endX : x >= endX); x += step) {
                coords.add(new int[]{centerX + x, z});
            }
        }
    }

    private static boolean hasSolidSupport(Block primary, Block secondary) {
        return isSolid(primary.getRelative(0, -1, 0)) && isSolid(secondary.getRelative(0, -1, 0));
    }

    private static boolean isSolid(Block block) {
        Material type = block.getType();
        return type != Material.AIR && !block.isLiquid() && !block.isPassable();
    }

    /**
     * Builds a stone fence cross at the head of the chest placement.
     */
    private static void buildStoneFenceCross(ChestPlacement placement, World world) {
        Location headLoc = calculateHeadLocation(placement);
        boolean eastWestAxis = placement.secondary.getX() != placement.primary.getX();

        List<Block> crossBlocks = collectCrossBlocks(world, headLoc, eastWestAxis);

        // Only build if all positions are air
        if (crossBlocks.stream().allMatch(b -> b.getType() == Material.AIR)) {
            crossBlocks.forEach(b -> b.setType(Material.COBBLESTONE_WALL));
        }
    }

    private static Location calculateHeadLocation(ChestPlacement placement) {
        int dx = placement.secondary.getX() - placement.primary.getX();
        int dz = placement.secondary.getZ() - placement.primary.getZ();

        if (dx > 0) return placement.primary.getLocation().add(-1, 0, 0);
        if (dx < 0) return placement.primary.getLocation().add(1, 0, 0);
        if (dz > 0) return placement.primary.getLocation().add(0, 0, -1);
        return placement.primary.getLocation().add(0, 0, 1);
    }

    private static List<Block> collectCrossBlocks(World world, Location headLoc, boolean eastWestAxis) {
        List<Block> blocks = new ArrayList<>();

        // Vertical pole (3 blocks)
        for (int i = 0; i < 3; i++) {
            blocks.add(world.getBlockAt(headLoc.clone().add(0, i, 0)));
        }

        // Cross arms (2 blocks)
        Location armBase = headLoc.clone().add(0, 1, 0);
        if (eastWestAxis) {
            blocks.add(world.getBlockAt(armBase.clone().add(0, 0, 1)));
            blocks.add(world.getBlockAt(armBase.clone().add(0, 0, -1)));
        } else {
            blocks.add(world.getBlockAt(armBase.clone().add(1, 0, 0)));
            blocks.add(world.getBlockAt(armBase.clone().add(-1, 0, 0)));
        }

        return blocks;
    }

    /**
     * Configures the two chest blocks to form a proper double chest.
     */
    private static boolean configureDoubleChest(ChestPlacement placement) {
        BlockState primaryState = placement.primary.getState();
        BlockState secondaryState = placement.secondary.getState();

        if (!(primaryState instanceof Chest) || !(secondaryState instanceof Chest)) {
            return false;
        }

        return setChestOrientation(placement);
    }

    private static boolean setChestOrientation(ChestPlacement placement) {
        int dx = placement.secondary.getX() - placement.primary.getX();
        int dz = placement.secondary.getZ() - placement.primary.getZ();

        if (dx != 0) {
            return configureEastWestChest(placement, dx > 0);
        } else if (dz != 0) {
            return configureNorthSouthChest(placement, dz > 0);
        }

        return false;
    }

    private static boolean configureEastWestChest(ChestPlacement placement, boolean secondaryEast) {
        org.bukkit.block.data.type.Chest primaryData = (org.bukkit.block.data.type.Chest) placement.primary.getBlockData();
        org.bukkit.block.data.type.Chest secondaryData = (org.bukkit.block.data.type.Chest) placement.secondary.getBlockData();

        primaryData.setFacing(org.bukkit.block.BlockFace.NORTH);
        secondaryData.setFacing(org.bukkit.block.BlockFace.NORTH);

        if (secondaryEast) {
            primaryData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
            secondaryData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
        } else {
            primaryData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
            secondaryData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
        }

        // Apply the block data to the actual blocks
        placement.primary.setBlockData(primaryData, false);
        placement.secondary.setBlockData(secondaryData, false);
        return true;
    }

    private static boolean configureNorthSouthChest(ChestPlacement placement, boolean secondarySouth) {
        org.bukkit.block.data.type.Chest primaryData = (org.bukkit.block.data.type.Chest) placement.primary.getBlockData();
        org.bukkit.block.data.type.Chest secondaryData = (org.bukkit.block.data.type.Chest) placement.secondary.getBlockData();

        primaryData.setFacing(org.bukkit.block.BlockFace.EAST);
        secondaryData.setFacing(org.bukkit.block.BlockFace.EAST);

        if (secondarySouth) {
            primaryData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
            secondaryData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
        } else {
            primaryData.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
            secondaryData.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
        }

        // Apply the block data to the actual blocks
        placement.primary.setBlockData(primaryData, false);
        placement.secondary.setBlockData(secondaryData, false);
        return true;
    }

    /**
     * Clears a 3×3×3 cube around both chest blocks to give player space.
     */
    private static void clearAreaAroundChest(ChestPlacement placement, World world) {
        int minX = Math.min(placement.primary.getX(), placement.secondary.getX()) - 1;
        int maxX = Math.max(placement.primary.getX(), placement.secondary.getX()) + 1;
        int minZ = Math.min(placement.primary.getZ(), placement.secondary.getZ()) - 1;
        int maxZ = Math.max(placement.primary.getZ(), placement.secondary.getZ()) + 1;
        int y = placement.primary.getY();

        for (int x = minX; x <= maxX; x++) {
            for (int yOffset = 0; yOffset <= 2; yOffset++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y + yOffset, z);
                    clearBlockIfSafe(block, placement);
                }
            }
        }
    }

    private static void clearBlockIfSafe(Block block, ChestPlacement placement) {
        if (block.equals(placement.primary) || block.equals(placement.secondary)) return;

        Material type = block.getType();
        if (type != Material.BEDROCK && type != Material.OBSIDIAN) {
            block.setType(Material.AIR, false);
        }
    }

    /**
     * Creates a 3×3 glass platform beneath the chest when needed.
     */
    private static void buildGlassPlatformIfNeeded(ChestPlacement placement, World world, boolean forcePlatform) {
        if (forcePlatform || isPlatformNeeded(placement, world)) {
            int y = placement.primary.getY() - 1;
            PlatformBuilder.createGlassPad3x3(world, placement.primary.getX(), y, placement.primary.getZ());
        }
    }

    private static boolean isPlatformNeeded(ChestPlacement placement, World world) {
        int y = placement.primary.getY() - 1;
        int cx = placement.primary.getX();
        int cz = placement.primary.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(cx + dx, y, cz + dz);

                if (block.isLiquid()) return true;

                if (block.getType() == Material.AIR && hasLongAirGap(world, cx + dx, y, cz + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasLongAirGap(World world, int x, int y, int z) {
        int airCount = 0;
        int minY = world.getMinHeight();

        for (int checkY = y; checkY >= minY && airCount < AIR_GAP_THRESHOLD; checkY--) {
            if (world.getBlockAt(x, checkY, z).getType() != Material.AIR) {
                break;
            }
            airCount++;
        }

        return airCount >= AIR_GAP_THRESHOLD;
    }
}