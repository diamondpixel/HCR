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
import takys.Utilities;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class DeadPlayersGUI {
    private static final int SLOTS_PER_PAGE = 45;
    private static final int GUI_ROWS = 5;
    private static final int SPECIFIC_GUI_ROWS = 1;
    private static final long REFRESH_INTERVAL = 20L; // 1 second

    // Cache for active buttons to prevent memory leaks
    private final Map<String, CustomSGButton> activeButtons = new ConcurrentHashMap<>();

    public SGMenu deadPlayersGui(Player player) {
        SGMenu gui = Setup.spiGUI.create("&6Dead players", GUI_ROWS);
        gui.setAutomaticPaginationEnabled(true);

        // Clear any existing buttons for this player
        clearActiveButtons(player);

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
        Player player = Bukkit.getPlayer(playerObj.getUUID());
        if (player == null) {
            return null; // Handle case where player is no longer online
        }

        SGMenu gui = Setup.spiGUI.create("&c" + player.getName(), SPECIFIC_GUI_ROWS);

        // Create buttons with optimized event handling
        SGButton backArrow = createBackButton(viewer);
        SGButton bed = createBedButton(viewer, playerObj, player);
        SGButton worldSpawn = createWorldSpawnButton(viewer, playerObj);
        SGButton lookingLocation = createLookingLocationButton(viewer, playerObj);
        SGButton deathLocation = createDeathLocationButton(viewer, playerObj);

        // Fill with glass panes
        fillWithGlassPanes(gui);

        // Set functional buttons
        gui.setButton(0, backArrow);
        gui.setButton(3, bed);
        gui.setButton(4, worldSpawn);
        gui.setButton(5, lookingLocation);
        gui.setButton(6, deathLocation);
        gui.setAutomaticPaginationEnabled(false);

        return gui;
    }

    private SGButton createBackButton(Player viewer) {
        return new SGButton(Utilities.backArrowItem())
                .withListener(event -> viewer.openInventory(deadPlayersGui(viewer).getInventory()));
    }

    private SGButton createBedButton(Player viewer, PlayerObj playerObj, Player player) {
        return new SGButton(Utilities.bedItem(playerObj.getUUID()))
                .withListener(event -> {
                    Location bedLocation = player.getBedSpawnLocation();
                    if (bedLocation != null) {
                        Utilities.revivePlayer(viewer, playerObj, bedLocation);
                    }
                });
    }

    private SGButton createWorldSpawnButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.worldSpawnItem(playerObj.getUUID()))
                .withListener(event -> {
                    File file = new File("server.properties");
                    String worldName = Utilities.getString("level-name", file);
                    Location spawnLocation = Bukkit.getWorld(worldName).getSpawnLocation();
                    Utilities.revivePlayer(viewer, playerObj, spawnLocation);
                });
    }

    private SGButton createLookingLocationButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.eyeLocationItem(playerObj.getUUID()))
                .withListener(event -> {
                    Location clickerLoc = event.getWhoClicked().getLocation();
                    Location targetLoc = clickerLoc.add(clickerLoc.getDirection().multiply(2));
                    targetLoc.setY(targetLoc.getY() + 1.0D);
                    Vector direction = targetLoc.clone().subtract(event.getWhoClicked().getEyeLocation()).toVector();
                    targetLoc.setDirection(direction);
                    Utilities.revivePlayer(viewer, playerObj, targetLoc);
                });
    }

    private SGButton createDeathLocationButton(Player viewer, PlayerObj playerObj) {
        return new SGButton(Utilities.deathLocationItem(playerObj))
                .withListener(event -> {
                    if (!Utilities.isBelowAir(playerObj)) {
                        Utilities.revivePlayer(viewer, playerObj, playerObj.getLoc());
                    }
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

    /**
     * Perhaps future use
     * */
    public void cleanup() {
        // Clean up all active buttons when plugin is disabled
        activeButtons.values().forEach(CustomSGButton::cleanup);
        activeButtons.clear();
    }

    class CustomSGButton extends SGButton implements Listener {
        private final PlayerObj playerObj;
        private final HumanEntity viewer;
        private final SGMenu gui;
        private final int page;
        private final int slot;
        private BukkitTask refreshTask;
        private boolean isActive = true;

        public CustomSGButton(PlayerObj playerObj, HumanEntity viewer, SGMenu gui, int page, int slot) {
            super(Utilities.getPlayerSkull(playerObj));
            this.playerObj = playerObj;
            this.viewer = viewer;
            this.gui = gui;
            this.page = page;
            this.slot = slot;

            startRefreshTask();
            Setup.instance.getServer().getPluginManager().registerEvents(this, Setup.instance);

            // Add click listener
            withListener(this::handleClick);
        }

        private void handleClick(InventoryClickEvent event) {
            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
                return;
            }

            String playerName = event.getCurrentItem().getItemMeta().getDisplayName();
            PlayerObj deadPlayer = findDeadPlayerByName(playerName);

            if (deadPlayer != null) {
                Player clicker = (Player) event.getWhoClicked();
                SGMenu specificGui = specificDeadPlayersGui(clicker, deadPlayer);
                if (specificGui != null) {
                    clicker.openInventory(specificGui.getInventory());
                }
            }
        }

        private PlayerObj findDeadPlayerByName(String displayName) {
            return Setup.DeadPlayers.stream()
                    .filter(Objects::nonNull)
                    .filter(deadPlayer -> deadPlayer.getPlayer() != null)
                    .filter(deadPlayer -> deadPlayer.getPlayer().getName().equals(displayName))
                    .findFirst()
                    .orElse(null);
        }

        private void startRefreshTask() {
            refreshTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isActive) {
                        cancel();
                        return;
                    }

                    try {
                        if (playerObj.getPlayer() != null) {
                            setIcon(Objects.requireNonNull(Utilities.getPlayerSkull(playerObj)));
                            gui.refreshInventory(viewer);
                        } else {
                            cleanup();
                        }
                    } catch (Exception e) {
                        cleanup();
                    }
                }
            }.runTaskTimerAsynchronously(Setup.instance, 0L, REFRESH_INTERVAL);
        }

        @EventHandler
        public void onPlayerDeath(PlayerDeathEvent event) {
            if (playerObj.getUUID().equals(event.getEntity().getUniqueId())) {
                gui.removeButton(this.page, this.slot);
                cleanup();
            }
        }

        public void cleanup() {
            isActive = false;
            if (refreshTask != null && !refreshTask.isCancelled()) {
                refreshTask.cancel();
            }
            HandlerList.unregisterAll(this);
        }
    }
}