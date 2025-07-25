package takys.Commands.SubCommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import takys.Setup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static takys.Utilities.Utilities.debug;
import static takys.Utilities.Utilities.reloadRecipe;

public class ReloadCommand {

    private static final String PERMISSION = "hcr.reload";
    private static final String CONFIG_FILE_NAME = "config.yml";

    // Message constants for better maintainability
    private static final String NO_PERMISSION_MSG = "§cYou don't have permission to use this command.";
    private static final String USAGE_MSG = "§cUsage: /hcr reload";
    private static final String RELOADING_MSG = "§eReloading plugin configuration...";
    private static final String CONFIG_NOT_FOUND_MSG = "§eConfig file not found, regenerating...";
    private static final String CONFIG_REGENERATED_MSG = "§aDefault configuration file has been regenerated!";
    private static final String SUCCESS_MSG = "§aPlugin configuration reloaded successfully!";
    private static final String ERROR_MSG = "§cAn error occurred while reloading the plugin:";

    public boolean execute(CommandSender sender, String[] args) {
        // Early permission check
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(NO_PERMISSION_MSG);
            return true;
        }

        // Validate arguments
        if (args.length > 1) {
            sender.sendMessage(USAGE_MSG);
            return true;
        }

        return performReload(sender);
    }

    /**
     * Performs the actual reload operation
     * @param sender The command sender
     * @return true if command was handled successfully
     */
    private boolean performReload(CommandSender sender) {
        try {
            sender.sendMessage(RELOADING_MSG);

            final Setup plugin = Setup.instance;
            if (plugin == null) {
                sender.sendMessage("§cPlugin instance not available!");
                return true;
            }

            // Handle config file regeneration if needed
            handleConfigFile(sender, plugin);

            // Reload configuration
            plugin.reloadConfig();

            // Get online players and reload recipes
            final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            reloadRecipe(onlinePlayers);

            sender.sendMessage(SUCCESS_MSG);

        } catch (Exception e) {
            handleReloadError(sender, e);
        }

        return true;
    }

    /**
     * Handles config file existence and regeneration
     * @param sender The command sender
     * @param plugin The plugin instance
     */
    private void handleConfigFile(CommandSender sender, Setup plugin) {
        final File configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            sender.sendMessage(CONFIG_NOT_FOUND_MSG);
            debug("Config file missing, regenerating default configuration");

            plugin.saveDefaultConfig();
            sender.sendMessage(CONFIG_REGENERATED_MSG);
        }
    }

    /**
     * Handles reload errors with proper logging
     * @param sender The command sender
     * @param e The exception that occurred
     */
    private void handleReloadError(CommandSender sender, Exception e) {
        sender.sendMessage(ERROR_MSG);
        sender.sendMessage("§c" + e.getMessage());

        // Log the full stack trace for debugging
        debug("Reload command error: %s", e.getMessage());
        e.printStackTrace();
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}