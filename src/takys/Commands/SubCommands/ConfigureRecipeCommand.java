package takys.Commands.SubCommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import takys.GUIs.RecipeConfiguratorGUI;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigureRecipeCommand {

    // Constants for better maintainability
    private static final String PERMISSION_NODE = "hcr.configurerecipe";
    private static final String NO_PERMISSION_MSG = "§cYou don't have permission to use this command.";
    private static final String PLAYER_ONLY_MSG = "§cThis command can only be used by players.";
    private static final String SUCCESS_MSG = "§aOpened recipe configuration GUI.";
    private static final String ERROR_MSG = "§cError opening recipe configuration GUI: ";

    private static final Logger LOGGER = Logger.getLogger(ConfigureRecipeCommand.class.getName());

    private final RecipeConfiguratorGUI recipeConfiguratorGUI;

    public ConfigureRecipeCommand() {
        this.recipeConfiguratorGUI = new RecipeConfiguratorGUI();
    }

    // Alternative constructor for dependency injection (better testability)
    public ConfigureRecipeCommand(RecipeConfiguratorGUI recipeConfiguratorGUI) {
        this.recipeConfiguratorGUI = recipeConfiguratorGUI;
    }

    public boolean execute(CommandSender sender, String[] args) {
        // Early return pattern for cleaner flow
        if (!sender.hasPermission(PERMISSION_NODE)) {
            sender.sendMessage(NO_PERMISSION_MSG);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(PLAYER_ONLY_MSG);
            return true;
        }

        Player player = (Player) sender;
        return openRecipeGUI(player);
    }

    /**
     * Opens the recipe configuration GUI for the specified player.
     * Extracted to separate method for better readability and testability.
     *
     * @param player The player to open the GUI for
     * @return true if command execution completed (always true for this command)
     */
    private boolean openRecipeGUI(Player player) {
        try {
            // More concise GUI opening - assuming createRecipeGUI returns an Inventory or InventoryHolder
            player.openInventory(recipeConfiguratorGUI.createRecipeGUI(player).getInventory());
            player.sendMessage(SUCCESS_MSG);
        } catch (Exception e) {
            // Better error handling with proper logging
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
            player.sendMessage(ERROR_MSG + errorMessage);

            // Use proper logging instead of printStackTrace for production code
            LOGGER.log(Level.SEVERE, "Failed to open recipe configuration GUI for player: " + player.getName(), e);
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}