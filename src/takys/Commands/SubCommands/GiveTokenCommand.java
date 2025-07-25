package takys.Commands.SubCommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import takys.Setup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GiveTokenCommand {

    private static final String NO_PERMISSION_MSG = "§cYou don't have permission to use this command.";
    private static final String PLAYER_NOT_FOUND_MSG = "§cPlayer '%s' not found or not online.";
    private static final String CONSOLE_USAGE_MSG = "§cYou must specify a player when using this command from console.\n§cUsage: /hcr giveToken <player> [amount]";
    private static final String INVALID_AMOUNT_MSG = "§cAmount must be a positive number.";
    private static final String INVALID_NUMBER_MSG = "§cInvalid amount: %s";
    private static final String TOKEN_NOT_FOUND_MSG = "§cError: Revival token item not found.";
    private static final String INVENTORY_FULL_MSG = "§eYour inventory is full! Revival token(s) dropped on the ground.";
    private static final String RECEIVED_TOKEN_MSG = "§aYou received %d revival token(s)!";
    private static final String GAVE_TOKEN_MSG = "§aGave %d revival token(s) to %s";

    private static final List<String> AMOUNT_SUGGESTIONS = Arrays.asList("1", "5", "10", "16", "32", "64");

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hcr.givetoken")) {
            sender.sendMessage(NO_PERMISSION_MSG);
            return true;
        }

        Player targetPlayer = getTargetPlayer(sender, args);
        if (targetPlayer == null) {
            return true; // Error messages already sent in getTargetPlayer
        }

        int amount = getAmount(sender, args);
        if (amount == -1) {
            return true; // Error messages already sent in getAmount
        }

        return giveTokensToPlayer(sender, targetPlayer, amount);
    }

    private Player getTargetPlayer(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            Player targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage(String.format(PLAYER_NOT_FOUND_MSG, args[1]));
            }
            return targetPlayer;
        }

        if (sender instanceof Player) {
            return (Player) sender;
        }

        sender.sendMessage(CONSOLE_USAGE_MSG);
        return null;
    }

    private int getAmount(CommandSender sender, String[] args) {
        if (args.length < 3) {
            return 1; // Default amount
        }

        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0) {
                sender.sendMessage(INVALID_AMOUNT_MSG);
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(String.format(INVALID_NUMBER_MSG, args[2]));
            return -1;
        }
    }

    private boolean giveTokensToPlayer(CommandSender sender, Player targetPlayer, int amount) {
        ItemStack revivalToken = Setup.Item;
        if (revivalToken == null) {
            sender.sendMessage(TOKEN_NOT_FOUND_MSG);
            return true;
        }

        // Clone the item to avoid modifying the original
        ItemStack tokenToGive = revivalToken.clone();
        tokenToGive.setAmount(amount);

        // Give tokens to player
        if (targetPlayer.getInventory().firstEmpty() == -1) {
            targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), tokenToGive);
            targetPlayer.sendMessage(INVENTORY_FULL_MSG);
        } else {
            targetPlayer.getInventory().addItem(tokenToGive);
        }

        // Send success messages
        targetPlayer.sendMessage(String.format(RECEIVED_TOKEN_MSG, amount));
        if (!sender.equals(targetPlayer)) {
            sender.sendMessage(String.format(GAVE_TOKEN_MSG, amount, targetPlayer.getName()));
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 2:
                // Player name completion
                String playerPrefix = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(playerPrefix))
                        .collect(Collectors.toList());

            case 3:
                // Amount completion
                String amountPrefix = args[2];
                return AMOUNT_SUGGESTIONS.stream()
                        .filter(amount -> amount.startsWith(amountPrefix))
                        .collect(Collectors.toList());

            default:
                return Collections.emptyList();
        }
    }
}