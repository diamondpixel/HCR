package takys.Commands.SubCommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import takys.GUIs.RecipeConfiguratorGUI;

import java.util.ArrayList;
import java.util.List;

public class ConfigureRecipeCommand {

    private final RecipeConfiguratorGUI recipeConfiguratorGUI;

    public ConfigureRecipeCommand() {
        this.recipeConfiguratorGUI = new RecipeConfiguratorGUI();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hcr.configurerecipe")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        try {
            // Open the recipe configuration GUI
            player.openInventory(recipeConfiguratorGUI.createRecipeGUI(player).getInventory());
            player.sendMessage("§aOpened recipe configuration GUI.");
        } catch (Exception e) {
            player.sendMessage("§cError opening recipe configuration GUI: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // No additional arguments needed for this command
        return new ArrayList<>();
    }
}