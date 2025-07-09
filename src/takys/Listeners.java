package takys;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import takys.Files.DataManager;
import takys.GUIs.DeadPlayersGUI;
import takys.GUIs.RecipeConfiguratorGUI;
import takys.Objects.PlayerObj;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Listeners implements Listener {

    private static final Logger LOGGER = Logger.getLogger(Listeners.class.getName());
    private static final Setup setup = Setup.instance;
    private static final DataManager dataManager = Setup.dataManager;
    private static final DeadPlayersGUI gui = Setup.gui;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (player == null) {
            LOGGER.warning("PlayerDeathEvent received with null player");
            return;
        }

        UUID playerUUID = player.getUniqueId();
        Location location = player.getLocation();
        LocalDateTime deathTime = LocalDateTime.now();

        EntityDamageEvent.DamageCause damageCause =
                player.getLastDamageCause() != null ? player.getLastDamageCause().getCause() : EntityDamageEvent.DamageCause.CUSTOM;

        PlayerObj newPlayerObj = new PlayerObj(playerUUID, deathTime, damageCause, location);

        Setup.DeadPlayers.removeIf(playerObj ->
                playerObj != null && Objects.equals(playerObj.getUUID(), playerUUID));

        Setup.DeadPlayers.add(newPlayerObj);

        CompletableFuture.runAsync(() -> {
            try {
                String filePath = getPlayerDataPath(playerUUID);
                dataManager.deleteJsonFile(filePath);
                dataManager.createJsonFile(playerUUID, location, deathTime, damageCause);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling player death data for UUID: " + playerUUID, e);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        try {
            if (Setup.Recipe != null) {
                player.discoverRecipe(Setup.Recipe.getKey());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to discover recipe for player: " + player.getName(), e);
        }

        UUID playerUUID = player.getUniqueId();

        CompletableFuture.runAsync(() -> {
            try {
                String filePath = getPlayerDataPath(playerUUID);
                File dataFile = new File(filePath);

                if (!dataFile.exists()) {
                    return;
                }

                PlayerObj playerObj = dataManager.readJsonFile(filePath);
                if (playerObj != null) {
                    if (!Setup.DeadPlayers.contains(playerObj)) {
                        // Sync back to main thread for collection modification
                        setup.getServer().getScheduler().runTask(setup, () -> {
                            Setup.DeadPlayers.add(playerObj);
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading player data for UUID: " + playerUUID, e);
            }
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        UUID playerUUID = player.getUniqueId();

        try {
            Utilities.removeUUIDFromDeadPlayers(playerUUID);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing player from dead players list: " + playerUUID, e);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Early returns for efficiency
        if (event.getItem() == null) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) return;
        if (event.getItem().getType() != Material.END_CRYSTAL) return;

        // Safe item meta comparison
        if (!isTargetItem(event.getItem())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        try {
            if (gui != null) {
                player.openInventory(gui.deadPlayersGui(player).getInventory());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening GUI for player: " + player.getName(), e);
            player.sendMessage("§cError opening interface. Please try again.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();

        if (player.hasMetadata("recipeGUI_id")) {
            String guiId = player.getMetadata("recipeGUI_id").get(0).asString();
            RecipeConfiguratorGUI.AnimatedGlassPanel glassPanel =
                    Setup.instance.activeRecipeConfiguratorGUIs.get(guiId);
            if (glassPanel != null) {
                glassPanel.removeViewer(player);
                Setup.instance.activeRecipeConfiguratorGUIs.remove(guiId);
                player.removeMetadata("recipeGUI_id", Setup.instance);
            }
        }
    }

    private boolean isTargetItem(org.bukkit.inventory.ItemStack item) {
        if (Setup.Item == null || Setup.Item.getItemMeta() == null) {
            return false;
        }

        return Objects.equals(item.getItemMeta(), Setup.Item.getItemMeta());
    }

    private String getPlayerDataPath(UUID playerUUID) {
        return setup.getDataFolder() + File.separator + "DeadPlayers" + File.separator + playerUUID.toString() + ".json";
    }
}