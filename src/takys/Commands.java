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

import java.util.Locale;


public class Commands implements CommandExecutor {


      private final Setup setup = Setup.instance;
      private Utilities util = setup.getUtil();


      @Override
      public boolean onCommand(final CommandSender cs, final Command cmd, final String lb, final String[] args) {

            if (args.length == 0) {
                  cs.sendMessage("Commands:");
                  cs.sendMessage("/hr token <player> <amount> | /hr cleardeaths <player> | /hr cleardatabase");
                  return true;
            }

            switch (args[0].toLowerCase()){
                  case "token":
                        if (cs.hasPermission("hr.token")){
                              if (args.length == 1 && args[0].equalsIgnoreCase("token")) {
                                    if (cs instanceof Player) {
                                          return this.addToken((Player) cs, 1);
                                    } else {
                                          cs.sendMessage("Hi, Console!");
                                          return false;
                                    }
                              } else if (args.length == 2 && args[0].equalsIgnoreCase("token")) {
                                    if (util.isNumeric(String.valueOf(args[1]))) {
                                          if (cs instanceof Player) {
                                                return this.addToken((Player) cs, Integer.valueOf(args[1]));
                                          } else {
                                                cs.sendMessage("Hi, Console!");
                                                return false;
                                          }
                                    } else if (!util.isNumeric(String.valueOf(args[1]))) {
                                          if (Bukkit.getPlayer(args[1]) != null) {
                                                return this.addToken(Bukkit.getPlayer(args[1]), 1);
                                          } else {
                                                cs.sendMessage("Player not found!");
                                                return false;
                                          }
                                    } else if (cs == cs) {
                                          return this.addToken((Player) cs, 1);
                                    }
                              } else if (args.length == 3 && args[0].equalsIgnoreCase("token")) {
                                    if (!util.isNumeric(args[1]) && util.isNumeric(args[2]) && Bukkit.getPlayer(args[1]) != null) {
                                          return this.addToken(Bukkit.getPlayer(args[1]), Integer.valueOf(args[2]));
                                    } else {
                                          cs.sendMessage("Incorrect argument input!");
                                          return false;
                                    }
                              }
                        }return Utilities.noPermission(cs);
                  case "cleardeaths":
                        if (cs.hasPermission("hr.cd")){
                              if (args.length == 2) {
                                    Player p = Bukkit.getPlayer(args[1]);
                                    if (p != null) {return this.resetDeaths(cs, Bukkit.getPlayer(args[1]));}
                                    cs.sendMessage("Player could not be found.");return true;}
                              if (cs instanceof Player) {return this.resetDeaths(cs, (Player) cs);}
                        }return Utilities.noPermission(cs);
                  case "cleardatabase":
                        if (cs.hasPermission("hr.cdb")){
                              cs.sendMessage("Successfully deleted database!");
                              FileHandler.deleteFallen();
                              return true;
                        }return Utilities.noPermission(cs);
                  default:
                        cs.sendMessage("Invalid command!");

            }
            return true;
      }
      private boolean resetDeaths(CommandSender sender,Player p) {
    	  p.setStatistic(Statistic.DEATHS, 0);
    	  sender.sendMessage("Player's death count is now 0. ");
    	  return true;
      }
      
      private boolean addToken(Player p, int amount) {
            if (amount == 1) {
                  util.token.setAmount(1);
                  p.getInventory().addItem(new ItemStack[]{Utilities.token});
                  p.sendMessage("You have received a Revival Token!");
                  return true;
            }else{
                  util.token.setAmount(amount);
                  p.getInventory().addItem(new ItemStack[]{Utilities.token});
                  p.sendMessage("You have received " + amount + " Revival Tokens!");
                  return true;
            }
      }

}
