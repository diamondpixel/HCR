package takys;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import takys.Util.Inventories;
import takys.Util.Utilities;


public class Commands implements CommandExecutor {


      public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

            //Nothing Here
            if (args.length == 0) {
                  sender.sendMessage("Commands:");
                  sender.sendMessage("/hr token <player> <amount> | /hr cleardeaths <player>");
                  return true;
            }

            //Token Command - 100% Complete!

            if (args.length == 1 && args[0].equalsIgnoreCase("token")){
                  if (sender instanceof Player){
                        return this.addToken((Player)sender,1);
                  }else{sender.sendMessage("Hi, Console!"); return false;}
            }else if (args.length == 2 && args[0].equalsIgnoreCase("token")) {
                  if (Utilities.isNumeric(String.valueOf(args[1]))) {
                        if(sender instanceof Player) {
                              return this.addToken((Player) sender, Integer.valueOf(args[1]));
                        }else{sender.sendMessage("Hi, Console!"); return false;}
                  } else if (!Utilities.isNumeric(String.valueOf(args[1]))) {
                        if(Bukkit.getPlayer(args[1]) != null){
                              return this.addToken(Bukkit.getPlayer(args[1]), 1);
                        }else{sender.sendMessage("Player not found!"); return false;}
                  } else if (sender == sender){
                        return this.addToken((Player)sender,1);
                  }
            }else if (args.length == 3 && args[0].equalsIgnoreCase("token")){
                  if (!Utilities.isNumeric(args[1]) && Utilities.isNumeric(args[2]) && Bukkit.getPlayer(args[1]) != null){
                        return this.addToken(Bukkit.getPlayer(args[1]),Integer.valueOf(args[2]));
                  }else{sender.sendMessage("Incorrect argument input!"); return false;}
            }




           //ClearDeaths 100% Complete maybe add detail to messages
                  if (args.length <= 2 && args[0].equalsIgnoreCase("cleardeaths")) {
                      if (!sender.hasPermission("hr.deathToken")) {
                            return Utilities.noPermission(sender);
                      }

                      if (args.length == 2) {
                            Player p = Bukkit.getPlayer(args[1]);
                            if (p != null) {
                            	return this.resetDeaths(sender,Bukkit.getPlayer(args[1]));
                            }

                            sender.sendMessage("Player could not be found.");
                            return true;
                      }

                      if (sender instanceof Player) {
                    	  return this.resetDeaths(sender,(Player)sender);
                      }
                }

                  sender.sendMessage("That command is not recognized.");
                  return true;
            }


      
     
      private boolean resetDeaths(CommandSender sender,Player p) {
    	  p.setStatistic(Statistic.DEATHS, 0);
    	  sender.sendMessage("Player's death count is now 0. ");
    	  return true;
      }
      
      private boolean addToken(Player p, int amount) {
            if (amount == 1) {
                  Utilities.token.setAmount(1);
                  p.getInventory().addItem(new ItemStack[]{Utilities.token});
                  p.sendMessage("You have received a Revival Token!");
                  return true;
            }else{
                  Utilities.token.setAmount(amount);
                  p.getInventory().addItem(new ItemStack[]{Utilities.token});
                  p.sendMessage("You have received " + amount + " Revival Tokens!");
                  return true;
            }
      }

}
