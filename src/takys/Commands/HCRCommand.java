package takys.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import takys.Commands.SubCommands.GiveTokenCommand;
import takys.Commands.SubCommands.ConfigureRecipeCommand;
import takys.Commands.SubCommands.ReloadCommand;

import java.util.*;
import java.util.stream.Collectors;

public class HCRCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "hcr";
    private static final String NO_PERMISSION_MSG = "§cYou don't have permission to use this command.";
    private static final String UNKNOWN_SUBCOMMAND_MSG = "§cUnknown subcommand: ";

    // Cache subcommands for better performance
    private final Map<String, SubCommandHandler> subCommands;
    private final List<String> availableSubCommands;

    public HCRCommand() {
        // Initialize subcommands once
        this.subCommands = createSubCommandMap();
        this.availableSubCommands = List.of("giveToken", "configureRecipe", "reload");
    }

    private Map<String, SubCommandHandler> createSubCommandMap() {
        Map<String, SubCommandHandler> commands = new HashMap<>();

        GiveTokenCommand giveTokenCmd = new GiveTokenCommand();
        ConfigureRecipeCommand configureRecipeCmd = new ConfigureRecipeCommand();
        ReloadCommand reloadCmd = new ReloadCommand();

        commands.put("givetoken", new SubCommandHandler(giveTokenCmd::execute, giveTokenCmd::onTabComplete, "hcr.givetoken"));
        commands.put("configurerecipe", new SubCommandHandler(configureRecipeCmd::execute, configureRecipeCmd::onTabComplete, "hcr.configurerecipe"));
        commands.put("reload", new SubCommandHandler(reloadCmd::execute, reloadCmd::onTabComplete, "hcr.reload"));

        return commands;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!COMMAND_NAME.equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!sender.hasPermission("hcr.use")) {
            sender.sendMessage(NO_PERMISSION_MSG);
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommandHandler handler = subCommands.get(subCommandName);

        if (handler == null) {
            sender.sendMessage(UNKNOWN_SUBCOMMAND_MSG + args[0]);
            sendHelpMessage(sender);
            return true;
        }

        if (!sender.hasPermission(handler.permission)) {
            sender.sendMessage(NO_PERMISSION_MSG);
            return true;
        }

        return handler.executor.execute(sender, args);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6=== HCR Commands ===");

        // Only show commands the sender has permission for
        if (sender.hasPermission("hcr.givetoken")) {
            sender.sendMessage("§e/hcr giveToken [player] [amount] §7- Give revival tokens");
        }
        if (sender.hasPermission("hcr.configurerecipe")) {
            sender.sendMessage("§e/hcr configureRecipe §7- Open recipe configuration GUI");
        }
        if (sender.hasPermission("hcr.reload")) {
            sender.sendMessage("§e/hcr reload §7- Reload the plugin");
        }

        sender.sendMessage("§6==================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!COMMAND_NAME.equalsIgnoreCase(command.getName())) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return getFilteredSubCommands(sender, args[0]);
        }

        if (args.length >= 2) {
            return delegateTabCompletion(sender, args);
        }

        return Collections.emptyList();
    }

    private List<String> getFilteredSubCommands(CommandSender sender, String partialInput) {
        String lowerInput = partialInput.toLowerCase();

        return availableSubCommands.stream()
                .filter(subCmd -> {
                    String permission = subCommands.get(subCmd.toLowerCase()).permission;
                    return sender.hasPermission(permission) &&
                            subCmd.toLowerCase().startsWith(lowerInput);
                })
                .collect(Collectors.toList());
    }

    private List<String> delegateTabCompletion(CommandSender sender, String[] args) {
        String subCommandName = args[0].toLowerCase();
        SubCommandHandler handler = subCommands.get(subCommandName);

        if (handler != null && sender.hasPermission(handler.permission)) {
            return handler.tabCompleter.complete(sender, args);
        }

        return Collections.emptyList();
    }

    // Helper class to encapsulate subcommand data
    private static class SubCommandHandler {
        final CommandExecutorFunction executor;
        final TabCompleterFunction tabCompleter;
        final String permission;

        SubCommandHandler(CommandExecutorFunction executor, TabCompleterFunction tabCompleter, String permission) {
            this.executor = executor;
            this.tabCompleter = tabCompleter;
            this.permission = permission;
        }
    }

    // Functional interfaces for cleaner code
    @FunctionalInterface
    private interface CommandExecutorFunction {
        boolean execute(CommandSender sender, String[] args);
    }

    @FunctionalInterface
    private interface TabCompleterFunction {
        List<String> complete(CommandSender sender, String[] args);
    }
}