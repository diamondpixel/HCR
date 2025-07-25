package takys.GUIs;

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
import takys.Setup;
import takys.Utilities.SafeTeleportManager;
import takys.Utilities.Utilities;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeadPlayersGUI {
    private static final int SLOTS_PER_PAGE = 45;
    private static final int GUI_ROWS = 5;
    private static final int SPECIFIC_GUI_ROWS = 1;
    private static final long REFRESH_INTERVAL = 20L; // 1 second

    // Static constants for button positions
    private static final int BACK_BUTTON_SLOT = 0;
    private static final int BED_BUTTON_SLOT = 3;
    private static final int WORLD_SPAWN_SLOT = 4;
    private static final int LOOKING_LOCATION_SLOT = 5;
    private static final int DEATH_LOCATION_SLOT = 6;

    // Use more efficient data structures
    private final Map<String, CustomSGButton> activeButtons = new ConcurrentHashMap<>();
    private final Set<UUID> trackedViewers = ConcurrentHashMap.newKeySet();
    private BukkitTask centralRefreshTask;

    public SGMenu deadPlayersGui(Player player) {
        SGMenu gui = Setup.spiGUI.create("&6Dead players", GUI_ROWS);
        gui.setAutomaticPaginationEnabled(true);

        // Clear any existing buttons for this player
        clearActiveButtons(player);

        // Track viewer and ensure central refresh task is running
        trackedViewers.add(player.getUniqueId());
        if (centralRefreshTask == null || centralRefreshTask.isCancelled()) {
            startCentralRefreshTask();
        }

        int page = 0;
        int slot = 0;

        for (PlayerObj playerObj : Setup.DeadPlayers) {
            if (playerObj == null || playerObj.getPlayer() == null) {
                continue; // Skip null entries but continue processing
            }

            CustomSGButton button = new CustomSGButton(playerObj, player, gui, page, slot);
            gui.setButton(page, slot, button);

            // Track active buttons
            String buttonKey = player.getUniqueId() + ":" + page + ":" + slot;
            activeButtons.put(buttonKey, button);

            if (slot + 1 >= SLOTS_PER_PAGE) {
                slot = 0;
                page++;
            } else {
                slot++;
            }
        }
        return gui;
    }

    public SGMenu specificDeadPlayersGui(Player viewer, PlayerObj playerObj) {
        Player targetPlayer = Bukkit.getPlayer(playerObj.getUUID());
        if (targetPlayer == null) {
            return null;
        }

        SGMenu gui = Setup.spiGUI.create("&c" + targetPlayer.getName(), SPECIFIC_GUI_ROWS);

        // Fill with glass panes first
        fillWithGlassPanes(gui);

        // Then set functional buttons (this will overwrite the glass panes in those slots)
        setupSpecificPlayerButtons(gui, viewer, playerObj, targetPlayer);

        gui.setAutomaticPaginationEnabled(false);
        return gui;
    }



    private void setupSpecificPlayerButtons(SGMenu gui, Player viewer, PlayerObj playerObj, Player targetPlayer) {
        gui.setButton(BACK_BUTTON_SLOT, createBackButton(viewer));
        gui.setButton(BED_BUTTON_SLOT, createBedButton(viewer, playerObj, targetPlayer));
        gui.setButton(WORLD_SPAWN_SLOT, createWorldSpawnButton(viewer, playerObj));
        gui.setButton(LOOKING_LOCATION_SLOT, createLookingLocationButton(viewer, playerObj));
        gui.setButton(DEATH_LOCATION_SLOT, createDeathLocationButton(viewer, playerObj));
    }

    private SGButton createBackButton(Player viewer) {
        return new SGButton(Utilities.backArrowItem())
                .withListener(event -> viewer.openInventory(deadPlayersGui(viewer).getInventory()));
    }

    private SGButton createBedButton(Player viewer, PlayerObj playerObj, Player targetPlayer) {
        return new SGButton(Utilities.bedItem(playerObj.getUUID()))
                .withListener(event -> {
                    Location bedLocation = targetPlayer.getBedSpawnLocation();
                    if (bedLocation != null) {
                        Utilities.revivePlayer(viewer, playerObj, bedLocation);
                    }
                });
    }

    private SGButton createWorldSpawnButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.worldSpawnItem(playerObj.getUUID()))
                .withListener(event -> {
                    Location spawnLocation = getWorldSpawnLocation();
                    if (spawnLocation != null) {
                        Utilities.revivePlayer(viewer, playerObj, spawnLocation);
                    }
                });
    }

    private SGButton createLookingLocationButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.eyeLocationItem(playerObj.getUUID()))
                .withListener(event -> {
                    Location targetLocation = calculateLookingLocation(event.getWhoClicked().getLocation());
                    Utilities.revivePlayer(viewer, playerObj, targetLocation);
                });
    }

    private SGButton createDeathLocationButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.deathLocationItem(playerObj))
                .withListener(event -> {
                    Location safeLocation = SafeTeleportManager.findSafeTeleportLocation(playerObj);
                    Utilities.revivePlayer(viewer, playerObj, safeLocation);
                });
    }

    private void fillWithGlassPanes(SGMenu gui) {
        SGButton glassPane = new SGButton(
                new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                        .name("\u200C")
                        .build()
        );

        for (int i = 0; i < 9; i++) {
            gui.setButton(i, glassPane);
        }
    }

    // Helper methods for better code organization

    private void clearActiveButtons(Player player) {
        String playerPrefix = player.getUniqueId() + ":";
        activeButtons.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(playerPrefix)) {
                entry.getValue().cleanup();
                return true;
            }
            return false;
        });
    }

    private String createButtonKey(UUID playerId, int page, int slot) {
        return playerId + ":" + page + ":" + slot;
    }

    private Location getWorldSpawnLocation() {
        try {
            File serverProperties = new File("server.properties");
            String worldName = Utilities.getString("level-name", serverProperties);
            return Bukkit.getWorld(worldName).getSpawnLocation();
        } catch (Exception e) {
            // Fallback to default world spawn
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
    }

    private Location calculateLookingLocation(Location clickerLocation) {
        Location targetLocation = clickerLocation.add(clickerLocation.getDirection().multiply(2));
        targetLocation.setY(targetLocation.getY() + 1.0D);

        Vector direction = targetLocation.clone()
                .subtract(clickerLocation.add(0, 1.62, 0)) // Eye height offset
                .toVector();

        targetLocation.setDirection(direction);
        return targetLocation;
    }

    private void startCentralRefreshTask() {
        centralRefreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeButtons.isEmpty()) {
                    cancel();
                    return;
                }
                activeButtons.values().forEach(CustomSGButton::updateIcon);
            }
        }.runTaskTimerAsynchronously(Setup.instance, 0L, REFRESH_INTERVAL);
    }

    public void cleanup() {
        activeButtons.values().forEach(CustomSGButton::cleanup);
        activeButtons.clear();
        trackedViewers.clear();

        if (centralRefreshTask != null && !centralRefreshTask.isCancelled()) {
            centralRefreshTask.cancel();
        }
    }

    // Inner class for custom button behavior
    class CustomSGButton extends SGButton implements Listener {
        private final PlayerObj playerObj;
        private final HumanEntity viewer;
        private final SGMenu gui;
        private final int page;
        private final int slot;
        private volatile boolean isActive = true;

        public CustomSGButton(PlayerObj playerObj, HumanEntity viewer, SGMenu gui, int page, int slot) {
            super(Utilities.getPlayerSkull(playerObj));
            this.playerObj = playerObj;
            this.viewer = viewer;
            this.gui = gui;
            this.page = page;
            this.slot = slot;

            Setup.instance.getServer().getPluginManager().registerEvents(this, Setup.instance);
            withListener(this::handleClick);
        }

        private void handleClick(InventoryClickEvent event) {
            if (!isValidClick(event)) {
                return;
            }

            String displayName = event.getCurrentItem().getItemMeta().getDisplayName();
            PlayerObj deadPlayer = findDeadPlayerByDisplayName(displayName);

            if (deadPlayer != null) {
                Player clicker = (Player) event.getWhoClicked();
                SGMenu specificGui = specificDeadPlayersGui(clicker, deadPlayer);
                if (specificGui != null) {
                    clicker.openInventory(specificGui.getInventory());
                }
            }
        }

        private boolean isValidClick(InventoryClickEvent event) {
            return event.getCurrentItem() != null &&
                    event.getCurrentItem().getItemMeta() != null;
        }

        private PlayerObj findDeadPlayerByDisplayName(String displayName) {
            return Setup.DeadPlayers.stream()
                    .filter(Objects::nonNull)
                    .filter(deadPlayer -> deadPlayer.getPlayer() != null)
                    .filter(deadPlayer -> deadPlayer.getPlayer().getName().equals(displayName))
                    .findFirst()
                    .orElse(null);
        }

        public void updateIcon() {
            if (!isActive) {
                return;
            }
            try {
                if (playerObj.getPlayer() != null) {
                    setIcon(Objects.requireNonNull(Utilities.getPlayerSkull(playerObj)));
                    gui.refreshInventory(viewer);
                } else {
                    cleanup();
                }
            } catch (Exception ignored) {
                cleanup();
            }
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (isActive && playerObj.getUUID().equals(event.getEntity().getUniqueId())) {
                gui.removeButton(page, slot);
                cleanup();
            }
        }

        public void cleanup() {
            if (isActive) {
                isActive = false;
                HandlerList.unregisterAll(this);
            }
        }
    }
}