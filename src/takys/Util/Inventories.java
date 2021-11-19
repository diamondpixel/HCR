package takys.Util;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.A;
import takys.PlayerObj;
import takys.Setup;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;


public class Inventories implements Listener {



    private final Setup setup = Setup.instance;
    private Utilities util = setup.getUtil();


    public void openDeadGUI(Player p) {
        Inventory deadGUI = Bukkit.createInventory((InventoryHolder) null, 54, "§e§lDEAD PEOPLE");
        util.trimmedPlayers.clear();
        util.DeadPlayers.forEach((player) -> {
            if (Bukkit.getPlayer(((PlayerObj) player).GetUUID()) != null && !util.trimmedPlayers.contains(player)) {
                Utilities.trimmedPlayers.add(player);
                deadGUI.addItem(new ItemStack[]{Utilities.getPlayer((PlayerObj) player)});
            }
        });

        p.openInventory(deadGUI);
    }

    @EventHandler
    public void onGUIclick_1(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if ((e.getClickedInventory() != null) && (e.getCurrentItem() != null)) {
            if (e.getView().getTitle().equals("§e§lDEAD PEOPLE")) {
                e.setCancelled(true);
                Utilities.trimmedPlayers.clear();
                Utilities.DeadPlayers.forEach((player) -> {
                if (Bukkit.getPlayer(((PlayerObj) player).GetUUID()) != null && !Utilities.trimmedPlayers.contains(player)) {
                        Utilities.trimmedPlayers.add(Bukkit.getPlayer(((PlayerObj) player).GetUUID()).getName());
                    }return;
                });
                if (Utilities.trimmedPlayers.contains(e.getCurrentItem().getItemMeta().getDisplayName())) {
                    String str = e.getCurrentItem().getItemMeta().getDisplayName();
                    Player var = Bukkit.getPlayer(str);
                    UUID uuid = var.getUniqueId();
                    openPlayerGUI(p, uuid);
                }
            }
        }
    }

    public void openPlayerGUI(Player ukn, UUID uuid) {
        Inventory playerGUI = Bukkit.createInventory((InventoryHolder) null, 9, "§e§l" + Bukkit.getPlayer(uuid).getName());

        playerGUI.setItem(0,new ItemStack(util.backArrow()));
        playerGUI.setItem(3,new ItemStack(util.worldSpawnHead(uuid)));
        playerGUI.setItem(4,new ItemStack(util.eyeLocationHead(uuid)));
        playerGUI.setItem(5,new ItemStack(util.bed(uuid)));

        ukn.openInventory(playerGUI);

    }


    @EventHandler
    public void onGUIclick_2(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        File f = new File("server.properties");
        if ((e.getClickedInventory() != null) && (e.getCurrentItem() != null)) {
            for (int i = 0; i < Utilities.DeadPlayers.size(); i++) {
                if (e.getView().getTitle().equals("§e§l" + Bukkit.getPlayer(((PlayerObj) Utilities.DeadPlayers.get(i)).GetUUID()).getName())) {
                    String str = Bukkit.getPlayer(((PlayerObj) Utilities.DeadPlayers.get(i)).GetUUID()).getName();
                    Player var = Bukkit.getPlayer(str);
                    UUID uuid = var.getUniqueId();
                    ItemStack token = p.getInventory().getItemInMainHand();
                    e.setCancelled(true);
                   if (e.getCurrentItem().getItemMeta().equals(util.backArrow().getItemMeta())){
                        openDeadGUI(p);
                  }else if (e.getCurrentItem().getItemMeta().equals(util.worldSpawnHead(uuid).getItemMeta())){
                       World world = Bukkit.getWorld(Utilities.getString("level-name", f));
                       Location defaultSpawn = world.getSpawnLocation();
                       util.revivePlayer(Bukkit.getPlayer(uuid),defaultSpawn, world);
                       token.setAmount(token.getAmount() - 1);
                       Utilities.DeadPlayers.remove(Utilities.DeadPlayers.get(i));
                       p.closeInventory();
                   }else if(e.getCurrentItem().getItemMeta().equals(util.eyeLocationHead(uuid).getItemMeta())){
                       Location spawnLoc = e.getWhoClicked().getLocation().add(e.getWhoClicked().getLocation().getDirection().multiply(2));
                       spawnLoc.setY(spawnLoc.getY() + 1.0D);
                       Vector dir = spawnLoc.clone().subtract(e.getWhoClicked().getEyeLocation()).toVector();
                       spawnLoc.setDirection(dir);
                       util.revivePlayer(Bukkit.getPlayer(uuid), spawnLoc, e.getWhoClicked().getWorld());
                       token.setAmount(token.getAmount() - 1);
                       Utilities.DeadPlayers.remove(Utilities.DeadPlayers.get(i));
                       p.closeInventory();
                   }else if(e.getCurrentItem().getItemMeta().equals(util.bed(uuid).getItemMeta())){
                         if(util.bed(uuid).getType().equals(Material.GREEN_BED)){
                            Location Loc = new Location(Bukkit.getPlayer(uuid).getBedSpawnLocation().getWorld(),
                                    Bukkit.getPlayer(uuid).getBedSpawnLocation().getX(),
                                    Bukkit.getPlayer(uuid).getBedSpawnLocation().getY() + 1,
                                    Bukkit.getPlayer(uuid).getBedSpawnLocation().getZ());
                            World world = Loc.getWorld();
                            token.setAmount(token.getAmount() - 1);
                             Utilities.DeadPlayers.remove(Utilities.DeadPlayers.get(i));
                            util.revivePlayer(var,Loc,world);
                         }else{
                            e.setCancelled(true);
                     }
                   }
                }
            }
        }
    }


}

