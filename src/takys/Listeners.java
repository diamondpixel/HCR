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
import org.bukkit.inventory.ItemStack;
import takys.Files.DataManager;
import takys.GUIs.DeadPlayersGUI;
import takys.GUIs.RecipeConfiguratorGUI;
import takys.Objects.PlayerObj;
import takys.Utilities.Utilities;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main event listener class handling player death, join/quit events, and interactions.
 * Manages dead player data persistence and GUI interactions.
 */
public class Listeners implements Listener {

    private static final Logger LOGGER = Logger.getLogger(Listeners.class.getName());

    // Cache frequently accessed objects
    private final Setup setup;
    private final DataManager dataManager;
    private final DeadPlayersGUI gui;

    public Listeners() {
        this.setup = Setup.instance;
        this.dataManager = Setup.dataManager;
        this.gui = Setup.gui;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();

        if (player == null) {
            LOGGER.warning("PlayerDeathEvent received with null player");
            return;
        }

        final UUID playerUUID = player.getUniqueId();
        final Location deathLocation = clampLocationToWorldBounds(player.getLocation());
        final LocalDateTime deathTime = LocalDateTime.now();
        final EntityDamageEvent.DamageCause damageCause = getDamageCause(player);

        final PlayerObj newPlayerObj = new PlayerObj(playerUUID, deathTime, damageCause, deathLocation);

        // Remove existing entry for this player and add new one
        updateDeadPlayersList(playerUUID, newPlayerObj);

        // Handle death chest creation if enabled
        handleDeathChest(newPlayerObj, event);

        // Asynchronously handle file operations
        handlePlayerDataAsync(playerUUID, deathLocation, deathTime, damageCause);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;

        // Handle recipe discovery
        discoverRecipeForPlayer(player);

        // Load player death data asynchronously
        final UUID playerUUID = player.getUniqueId();
        loadPlayerDataAsync(playerUUID);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (player == null) return;

        final UUID playerUUID = player.getUniqueId();

        try {
            Utilities.removeUUIDFromDeadPlayers(playerUUID);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error removing player from dead players list: " + playerUUID, e);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Fast path: check conditions that are most likely to fail first
        if (!isValidInteraction(event)) {
            return;
        }

        if (!isTargetItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        openDeadPlayersGUI(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();
        handleRecipeGUICleanup(player);
    }

    // ========== Private Helper Methods ==========

    /**
     * Clamps location to world bounds to prevent invalid coordinates
     */
    private Location clampLocationToWorldBounds(Location location) {
        if (location.getBlockY() >= location.getWorld().getMinHeight()) {
            return location;
        }

        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getWorld().getMinHeight() - 1,
                location.getBlockZ()
        );
    }

    /**
     * Gets the damage cause from the player's last damage event
     */
    private EntityDamageEvent.DamageCause getDamageCause(Player player) {
        return player.getLastDamageCause() != null
                ? player.getLastDamageCause().getCause()
                : EntityDamageEvent.DamageCause.CUSTOM;
    }

    /**
     * Thread-safe update of the dead players list
     */
    private void updateDeadPlayersList(UUID playerUUID, PlayerObj newPlayerObj) {
        // Remove existing entry for this player (thread-safe)
        Setup.DeadPlayers.removeIf(playerObj ->
                playerObj != null && Objects.equals(playerObj.getUUID(), playerUUID));

        Setup.DeadPlayers.add(newPlayerObj);
    }

    /**
     * Handles death chest creation if enabled in config
     */
    private void handleDeathChest(PlayerObj playerObj, PlayerDeathEvent event) {
        if (!setup.getConfig().getBoolean("death_chest", true)) {
            return;
        }

        try {
            takys.DeathChest.DeathChestManager.createDeathChest(
                    playerObj,
                    new ArrayList<>(event.getDrops())
            );
            event.getDrops().clear();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Failed to create death chest for player UUID: " + playerObj.getUUID(), ex);
        }
    }

    /**
     * Handles player data file operations asynchronously
     */
    private void handlePlayerDataAsync(UUID playerUUID, Location location,
                                       LocalDateTime deathTime, EntityDamageEvent.DamageCause damageCause) {
        CompletableFuture.runAsync(() -> {
            try {
                final String filePath = getPlayerDataPath(playerUUID);
                dataManager.deleteJsonFile(filePath);
                dataManager.createJsonFile(playerUUID, location, deathTime, damageCause);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling player death data for UUID: " + playerUUID, e);
            }
        });
    }

    /**
     * Discovers recipe for player with error handling
     */
    private void discoverRecipeForPlayer(Player player) {
        if (Setup.Recipe == null) {
            return;
        }

        try {
            player.discoverRecipe(Setup.Recipe.getKey());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to discover recipe for player: " + player.getName(), e);
        }
    }

    /**
     * Loads player death data asynchronously
     */
    private void loadPlayerDataAsync(UUID playerUUID) {
        CompletableFuture.runAsync(() -> {
            try {
                final String filePath = getPlayerDataPath(playerUUID);
                final File dataFile = new File(filePath);

                if (!dataFile.exists()) {
                    return;
                }

                final PlayerObj playerObj = dataManager.readJsonFile(filePath);
                if (playerObj != null && !Setup.DeadPlayers.contains(playerObj)) {
                    // Sync back to main thread for collection modification
                    setup.getServer().getScheduler().runTask(setup, () ->
                            Setup.DeadPlayers.add(playerObj));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading player data for UUID: " + playerUUID, e);
            }
        });
    }

    /**
     * Validates interaction event conditions for efficiency
     */
    private boolean isValidInteraction(PlayerInteractEvent event) {
        return event.getItem() != null
                && event.getHand() != EquipmentSlot.OFF_HAND
                && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && event.getItem().getType() == Material.END_CRYSTAL;
    }

    /**
     * Opens the dead players GUI with error handling
     */
    private void openDeadPlayersGUI(Player player) {
        try {
            if (gui != null) {
                player.openInventory(gui.deadPlayersGui(player).getInventory());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error opening GUI for player: " + player.getName(), e);
            player.sendMessage("§cError opening interface. Please try again.");
        }
    }

    /**
     * Handles cleanup for recipe GUI metadata
     */
    private void handleRecipeGUICleanup(Player player) {
        if (!player.hasMetadata("recipeGUI_id")) {
            return;
        }

        final String guiId = player.getMetadata("recipeGUI_id").get(0).asString();
        final RecipeConfiguratorGUI.AnimatedGlassPanel glassPanel =
                Setup.instance.activeRecipeConfiguratorGUIs.get(guiId);

        if (glassPanel != null) {
            glassPanel.removeViewer(player);
            Setup.instance.activeRecipeConfiguratorGUIs.remove(guiId);
            player.removeMetadata("recipeGUI_id", Setup.instance);
        }
    }

    /**
     * Checks if the item matches the target item for GUI opening
     */
    private boolean isTargetItem(ItemStack item) {
        return Setup.Item != null
                && Setup.Item.getItemMeta() != null
                && Objects.equals(item.getItemMeta(), Setup.Item.getItemMeta());
    }

    /**
     * Constructs the file path for player data storage
     */
    private String getPlayerDataPath(UUID playerUUID) {
        return setup.getDataFolder() + File.separator + "DeadPlayers" + File.separator + playerUUID + ".json";
    }
}