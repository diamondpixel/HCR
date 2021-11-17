package takys.Util;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import takys.PlayerObj;



public class Inventories implements Listener {


        public static void openDeadGUI(Player p){
            Inventory deadGUI = Bukkit.createInventory((InventoryHolder)null,54,"§e§lDEAD PEOPLE");
                Utilities.trimmedPlayers.clear();
                Utilities.DeadPlayers.forEach((player) -> {
                    if (Bukkit.getPlayer(((PlayerObj) player).GetUUID()) != null && !Utilities.trimmedPlayers.contains(player)) {
                        Utilities.trimmedPlayers.add(player);
                        deadGUI.addItem(new ItemStack[]{Utilities.getPlayer((PlayerObj)player)});
                    }

                });
            p.openInventory(deadGUI);
        }

        public static void openPlayerGUI(Player p ){
            //Tomorrow
        }

}