package takys.DeathChest;

import org.bukkit.block.Block;

/**
 * Simple value object representing the two blocks that make up a double chest.
 * Both blocks are expected to be on the same Y level and adjacent.
 */
public final class ChestPlacement {

    public final Block primary;
    public final Block secondary;

    public ChestPlacement(Block primary, Block secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }
}
