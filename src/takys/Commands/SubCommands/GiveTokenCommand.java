package takys.Commands.SubCommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import takys.Setup;
import takys.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveTokenCommand {

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("hcr.givetoken")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        Player targetPlayer = null;
        int amount = 1;

        if (args.length >= 2) {
            // Get target player
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer '" + args[1] + "' not found or not online.");
                return true;
            }
        } else if (sender instanceof Player) {
            // If no target specified and sender is a player, give to self
            targetPlayer = (Player) sender;
        } else {
            sender.sendMessage("§cYou must specify a player when using this command from console.");
            sender.sendMessage("§cUsage: /hcr giveToken <player> [amount]");
            return true;
        }

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cAmount must be a positive number.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount: " + args[2]);
                return true;
            }
        }

        // Get the revival token item
        ItemStack revivalToken = Setup.Item;
        if (revivalToken == null) {
            sender.sendMessage("§cError: Revival token item not found.");
            return true;
        }

        // Set the amount
        revivalToken.setAmount(amount);

        // Give the token to the player
        if (targetPlayer.getInventory().firstEmpty() == -1) {
            // Inventory is full, drop the item
            targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), revivalToken);
            targetPlayer.sendMessage("§eYour inventory is full! Revival token(s) dropped on the ground.");
        } else {
            targetPlayer.getInventory().addItem(revivalToken);
        }

        // Send success messages
        targetPlayer.sendMessage("§aYou received " + amount + " revival token(s)!");
        if (!sender.equals(targetPlayer)) {
            sender.sendMessage("§aGave " + amount + " revival token(s) to " + targetPlayer.getName());
        }

        return true;
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Second argument - player names
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // Third argument - amount
            List<String> amounts = Arrays.asList("1", "5", "10", "16", "32", "64");
            return amounts.stream()
                    .filter(amount -> amount.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}