package takys.Commands.SubCommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import takys.Setup;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static takys.Utilities.debug;
import static takys.Utilities.reloadRecipe;

public class ReloadCommand {

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hcr.reload")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        // Check if there are extra arguments (reload command shouldn't have any)
        if (args.length > 1) {
            sender.sendMessage("§cUsage: /hcr reload");
            return true;
        }

        try {
            sender.sendMessage("§eReloading plugin configuration...");

            // Cache setup instance and online players
            Setup plugin = Setup.instance;
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();


            // Check if config file exists, if not regenerate it
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                sender.sendMessage("§eConfig file not found, regenerating...");
                debug("Config file missing, regenerating default configuration");

                // Save default config (this will create the file with default values)
                plugin.saveDefaultConfig();
                sender.sendMessage("§aDefault configuration file has been regenerated!");
            }

            // Reload configuration
            plugin.reloadConfig();

            // Handle recipe reload if needed
            reloadRecipe(onlinePlayers);

            sender.sendMessage("§aPlugin configuration reloaded successfully!");

        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while reloading the plugin:");
            sender.sendMessage("§c" + e.getMessage());

            // Log the full stack trace for debugging
            debug("Reload command error: %s", e.getMessage());
            e.printStackTrace();
        }

        return true;
    }



    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}