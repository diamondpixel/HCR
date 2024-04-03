package takys;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import takys.Objects.PlayerObj;

import java.io.File;

public class GraphicalUserInterface {


    private final SGButton head = new SGButton(new ItemStack(Material.AIR))
            .withListener((InventoryClickEvent event) -> {
                if (event.getCurrentItem() == null) {
                    return;
                }
                String playerName = event.getCurrentItem().getItemMeta().getDisplayName();
                for (PlayerObj deadPlayer : Setup.DeadPlayers) {
                    if (deadPlayer.GetPlayer().getName().equals(playerName)) {
                        event.getWhoClicked().openInventory(specificDeadPlayersGui((Player) event.getWhoClicked(), deadPlayer).getInventory());
                        return;
                    }
                }
            });

    @SuppressWarnings("all")
    public SGMenu deadPlayersGui(Player player) {
        SGMenu gui = Setup.spiGUI.create("&6Dead players", 5);
        gui.setAutomaticPaginationEnabled(true);

        // Create and start a BukkitRunnable to update the buttons every 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < Setup.DeadPlayers.size(); i++) {
                    PlayerObj playerObj = Setup.DeadPlayers.get(i);
                    if (playerObj.GetPlayer().isOnline()) {
                        SGButton button = head;
                        button.setIcon(Utilities.GetPlayerSkull(playerObj));
                        gui.setButton(i / 45, i % 45, button);
                    }
                }
                gui.refreshInventory(player);
            }
        }.runTaskTimerAsynchronously(Setup.instance, 0L, 20L); // Run every 2 seconds

        return gui;
    }

    @SuppressWarnings("all")
    public SGMenu specificDeadPlayersGui(Player viewer, PlayerObj playerObj) {
        Player player = Bukkit.getPlayer(playerObj.GetUUID());
        SGMenu gui = Setup.spiGUI.create("&c" + player.getName(), 1);

        SGButton backArrow = new SGButton(Utilities.BackArrowItem()).withListener((InventoryClickEvent event) -> {
            viewer.openInventory(deadPlayersGui(viewer).getInventory());
        });

        SGButton bed = new SGButton(Utilities.BedItem(playerObj.GetUUID())).withListener((InventoryClickEvent event) -> {
            if (player.getBedSpawnLocation() != null)
                Utilities.RevivePlayer(viewer, playerObj, player.getBedSpawnLocation());
        });

        SGButton worldSpawn = new SGButton(Utilities.WorldSpawnItem(playerObj.GetUUID())).withListener((InventoryClickEvent event) -> {
            File file = new File("server.properties");
            Utilities.RevivePlayer(viewer, playerObj, Bukkit.getWorld(Utilities.GetString("level-name", file)).getSpawnLocation());
        });

        SGButton lookingLocation = new SGButton(Utilities.EyeLocationItem(playerObj.GetUUID())).withListener((InventoryClickEvent event) -> {
            Location loc = event.getWhoClicked().getLocation().add(event.getWhoClicked().getLocation().getDirection().multiply(2));
            loc.setY(loc.getY() + 1.0D);
            Vector dir = loc.clone().subtract(event.getWhoClicked().getEyeLocation()).toVector();
            loc.setDirection(dir);
            Utilities.RevivePlayer(viewer, playerObj, loc);
        });

        SGButton deathLocation = new SGButton(Utilities.DeathLocationItem(playerObj)).withListener((InventoryClickEvent event) -> {
            Utilities.RevivePlayer(viewer, playerObj, playerObj.GetLoc());
        });

        for (int i = 0; i < 9; i++) {
            gui.setButton(i, new SGButton(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).build()));
        }

        gui.setButton(0, backArrow);
        gui.setButton(3, bed);
        gui.setButton(4, worldSpawn);
        gui.setButton(5, lookingLocation);
        gui.setButton(6, deathLocation);
        gui.setAutomaticPaginationEnabled(false);

        return gui;
    }

    static class HeadsInformation {

        private final int page;
        private final int slot;
        private final PlayerObj pObj;

        public HeadsInformation(int page, int slot, PlayerObj pObj) {
            this.page = page;
            this.slot = slot;
            this.pObj = pObj;
        }

        protected int getPage() {
            return this.page;
        }

        protected int getSlot() {
            return this.slot;
        }

        protected PlayerObj getPlayerObj() {
            return this.pObj;
        }
    }
}
