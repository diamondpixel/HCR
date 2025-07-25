package takys.Common;

import org.bukkit.Material;
import org.bukkit.block.Block;
import java.util.EnumSet;
import java.util.Set;

/**
 * Centralized helper for block safety checks shared by teleport and chest placement.
 * Optimized for performance with pre-computed sets and reduced method calls.
 */
public final class BlockSafety {
    private BlockSafety() {}

    /** Pre-computed set of dangerous materials for O(1) lookup performance. */
    private static final Set<Material> DANGEROUS_MATERIALS = EnumSet.of(
            Material.MAGMA_BLOCK,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.CACTUS,
            Material.SWEET_BERRY_BUSH,
            Material.WITHER_ROSE,
            Material.FIRE,
            Material.SOUL_FIRE
    );

    /**
     * Blocks that harm players or are otherwise unsafe as ground.
     * Uses O(1) set lookup instead of multiple equality checks.
     */
    public static boolean isDangerous(Material material) {
        return DANGEROUS_MATERIALS.contains(material);
    }

    /**
     * Returns true if the block can be safely replaced when spawning a chest/platform.
     * Optimized to check AIR first (most common case) before more expensive calls.
     */
    public static boolean isReplaceable(Block block) {
        Material type = block.getType();
        return type == Material.AIR || (block.isPassable() && !block.isLiquid());
    }

    /**
     * Ground must be solid and non-dangerous.
     * Single method call to get type, reducing overhead.
     */
    public static boolean isSolidSafeGround(Block below) {
        Material type = below.getType();
        return type.isSolid() && !DANGEROUS_MATERIALS.contains(type);
    }

    /**
     * Feet and head blocks must be clear of collision and liquids.
     * Optimized to fail fast on most common failure case (non-air blocks).
     */
    public static boolean hasTwoBlockHeadroom(Block feet) {
        // Fail fast if feet block is not air (most common failure)
        if (!feet.getType().isAir()) {
            return false;
        }

        Block head = feet.getRelative(0, 1, 0);
        return head.getType().isAir() && !feet.isLiquid() && !head.isLiquid();
    }

    /**
     * Complete standing-spot validation (below/feet/head).
     * Optimized order: check air blocks first (fastest), then liquids, then ground safety.
     */
    public static boolean isSafeStandingSpot(Block below, Block feet, Block head) {
        // Fast checks first - air blocks
        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return false;
        }

        // Medium cost checks - liquids
        if (feet.isLiquid() || head.isLiquid()) {
            return false;
        }

        // Most expensive check last - ground validation
        return isSolidSafeGround(below);
    }

    /**
     * Utility overload using just the feet block.
     * Cached relative block calls to avoid redundant calculations.
     */
    public static boolean isSafeStandingSpot(Block feet) {
        Block below = feet.getRelative(0, -1, 0);
        Block head = feet.getRelative(0, 1, 0);
        return isSafeStandingSpot(below, feet, head);
    }
}