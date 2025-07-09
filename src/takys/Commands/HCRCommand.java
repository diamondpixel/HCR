package takys.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import takys.Commands.SubCommands.GiveTokenCommand;
import takys.Commands.SubCommands.ConfigureRecipeCommand;
import takys.Commands.SubCommands.ReloadCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HCRCommand implements CommandExecutor, TabCompleter {

    private final GiveTokenCommand giveTokenCommand;
    private final ConfigureRecipeCommand configureRecipeCommand;
    private final ReloadCommand reloadCommand;

    public HCRCommand() {
        this.giveTokenCommand = new GiveTokenCommand();
        this.configureRecipeCommand = new ConfigureRecipeCommand();
        this.reloadCommand = new ReloadCommand();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("hcr")) {
            return false;
        }

        // Check if sender has permission
        if (!sender.hasPermission("hcr.use")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "givetoken":
                return giveTokenCommand.execute(sender, args);
            case "configurerecipe":
                return configureRecipeCommand.execute(sender, args);
            case "reload":
                return reloadCommand.execute(sender, args);
            default:
                sender.sendMessage("§cUnknown subcommand: " + args[0]);
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== HCR Commands ===");
        sender.sendMessage("§e/hcr giveToken [player] [amount] &7- Give revival tokens");
        sender.sendMessage("§e/hcr configureRecipe &7- Open recipe configuration GUI");
        sender.sendMessage("§e/hcr reload &7- Reload the plugin.");
        sender.sendMessage("§6==================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("hcr")) {
            return null;
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            // Filter based on permissions
            if (sender.hasPermission("hcr.givetoken")) {
                completions.add("giveToken");
            }
            if (sender.hasPermission("hcr.configurerecipe")) {
                completions.add("configureRecipe");
            }

            if (sender.hasPermission("hcr.reload")) {
                completions.add("reload");
            }

            // Filter based on what the player has typed
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length >= 2) {
            // Delegate to subcommand tab completion
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("givetoken") && sender.hasPermission("hcr.givetoken")) {
                return giveTokenCommand.onTabComplete(sender, args);
            } else if (subCommand.equals("configurerecipe") && sender.hasPermission("hcr.configurerecipe")) {
                return configureRecipeCommand.onTabComplete(sender, args);
            } else if (subCommand.equals("reload") && sender.hasPermission("hcr.reload")) {
                return reloadCommand.onTabComplete(sender, args);
            }

        }

        return new ArrayList<>();
    }
}