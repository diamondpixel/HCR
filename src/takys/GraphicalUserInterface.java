package takys;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import takys.Objects.PlayerObj;

import java.io.File;
import java.util.Objects;

public class GraphicalUserInterface {

    @SuppressWarnings("all")
    public SGMenu deadPlayersGui(Player player) {
        SGMenu gui = Setup.spiGUI.create("&6Dead players", 5);
        gui.setAutomaticPaginationEnabled(true);

        int page = 0;
        int slot = 0;
        for (PlayerObj playerObj : Setup.DeadPlayers) {
            if (playerObj == null || Setup.DeadPlayers.isEmpty() || playerObj.GetPlayer() == null) {
                break;
            }
            gui.setButton(page, slot, new CustomSGButton(playerObj, player, gui, page, slot));

            if (slot + 1 > 44) {
                slot = 0;
                page++;
            } else {
                slot++;
            }
        }
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
            if (!Utilities.isBelowAir(playerObj)) {
                Utilities.RevivePlayer(viewer, playerObj, playerObj.GetLoc());
            }
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

    class CustomSGButton extends SGButton implements Listener {

        private PlayerObj playerObj;
        private HumanEntity viewer;
        private SGMenu gui;
        private BukkitTask borderRunnable;
        private int page;
        private int slot;

        public CustomSGButton(PlayerObj playerObj, HumanEntity viewer, SGMenu gui, int page, int slot) {
            super(Utilities.GetPlayerSkull(playerObj));
            this.playerObj = playerObj;
            this.viewer = viewer;
            this.gui = gui;
            this.page = page;
            this.slot = slot;
            startBorderRunnable();
            Setup.instance.getServer().getPluginManager().registerEvents(this, Setup.instance);

            // Adding listener
            withListener((InventoryClickEvent event) -> {
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
        }

        private void startBorderRunnable() {
            borderRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        setIcon(Objects.requireNonNull(Utilities.GetPlayerSkull(playerObj)));
                        gui.refreshInventory(viewer);
                    } catch (NullPointerException e) {
                        borderRunnable.cancel();
                    }
                }
            }.runTaskTimerAsynchronously(Setup.instance, 0L, 20L); // Run every 2 seconds
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (playerObj.GetUUID().equals(event.getEntity().getUniqueId())) {
                // Remove the button and cancel the borderRunnable
                gui.removeButton(this.page, this.slot);
                borderRunnable.cancel();
                HandlerList.unregisterAll(this); // Unregister this listener
            }
        }
    }
}
