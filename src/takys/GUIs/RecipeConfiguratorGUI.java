package takys.GUIs;

import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import com.samjakob.spigui.menu.SGMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import takys.Setup;

import java.util.*;

import static takys.Utilities.debug;
import static takys.Utilities.reloadRecipe;

public class RecipeConfiguratorGUI {
    private static final int GUI_ROWS = 3;
    private static final long DEFAULT_GLASS_SHUFFLE_INTERVAL = 10L; // Fallback value

    private final Random random = new Random();

    private AnimatedGlassPanel glassPanel; // Store reference to stop animation when needed

    public SGMenu createRecipeGUI(Player player) {
        debug("Creating recipe GUI for player: %s", player.getName());

        SGMenu gui = Setup.spiGUI.create("§6Recipe Configuration", GUI_ROWS);
        gui.setAutomaticPaginationEnabled(false);

        // Create interactive recipe slots first
        setupRecipeSlots(gui, player);

        // Create exit button (bottom left - slot 18)
        SGButton exitButton = new SGButton(
                new ItemBuilder(Material.BARRIER)
                        .name("§cExit")
                        .lore("§7Click to exit without saving")
                        .build()
        ).withListener(event -> {
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);
            debug("Exit button clicked by player: %s", p.getName());

            if (glassPanel != null) {
                glassPanel.removeViewer(p);
                glassPanel.stopAnimation();
            }
            Setup.instance.activeRecipeConfiguratorGUIs.remove(p.getUniqueId());
            p.closeInventory();
            player.sendMessage("§cRecipe configuration cancelled!");
        });

        // Create confirm button (bottom right - slot 26)
        SGButton confirmButton = new SGButton(
                new ItemBuilder(Material.EMERALD)
                        .name("§aConfirm Changes")
                        .lore("§7Click to save the recipe")
                        .build()
        ).withListener(event -> {
            Player p = (Player) event.getWhoClicked();
            event.setCancelled(true);
            debug("Confirm button clicked by player: %s", p.getName());

            try {
                saveRecipeToConfig(gui);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cError saving recipe: " + e.getMessage());
                return;
            }
            
            reloadRecipe(Bukkit.getOnlinePlayers());

            if (glassPanel != null) {
                glassPanel.removeViewer(p);
                glassPanel.stopAnimation();
            }
            Setup.instance.activeRecipeConfiguratorGUIs.remove(p.getUniqueId());
            p.closeInventory();
            player.sendMessage("§aRecipe configuration saved successfully!");
        });

        // Set buttons
        gui.setButton(18, exitButton); // Bottom left
        gui.setButton(26, confirmButton); // Bottom right

        // Load current recipe from config if it exists
        loadCurrentRecipe(gui);

        // Create animated glass panels AFTER setting up other elements
        glassPanel = new AnimatedGlassPanel(gui);
        debug("Created animated glass panel with identifier: %s", glassPanel.getIdentifier());

        //Add to globally tracked map
        Setup.instance.activeRecipeConfiguratorGUIs.put(player.getUniqueId(), glassPanel);

        player.setMetadata("recipeGUI_id", new FixedMetadataValue(Setup.instance, glassPanel.getIdentifier()));

        //Add the player as a viewer BEFORE starting animation
        glassPanel.addViewer(player);
        glassPanel.startAnimation();

        debug("Recipe GUI created successfully for player: %s", player.getName());
        return gui;
    }

    private void setupRecipeSlots(SGMenu gui, Player player) {
        debug("Setting up recipe slots for player: %s", player.getName());

        // Map recipe positions to GUI slots
        int[] craftingSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

        for (int slot : craftingSlots) {
            SGButton recipeSlot = createEmptyRecipeSlot(gui);
            gui.setButton(slot, recipeSlot);
        }

        debug("Recipe slots setup completed for player: %s", player.getName());
    }

    private SGButton createEmptyRecipeSlot(SGMenu gui) {
        return new SGButton(
                new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("§7Empty Recipe Slot")
                        .lore("§eLeft-click: §7Place item from cursor",
                                "§eRight-click: §7Clear this slot")
                        .build()
        ).withListener(event -> {
            event.setCancelled(true);
            handleRecipeSlotClick(event, gui);
        });
    }

    private SGButton createRecipeSlot(ItemStack item, SGMenu gui) {
        // Create a copy of the item to avoid modifying the original
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            // Add purple color to the item name
            String originalName = meta.hasDisplayName() ? meta.getDisplayName() :
                    formatItemName(item.getType().name());
            meta.setDisplayName("§5" + originalName);

            // Preserve original lore and add interaction instructions
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // Add separator and interaction instructions
            lore.add("");
            lore.add("§eLeft-click: §7Place item from cursor");
            lore.add("§eRight-click: §7Clear this slot");

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return new SGButton(displayItem).withListener(event -> {
            event.setCancelled(true);
            handleRecipeSlotClick(event, gui);
        });
    }

    private void handleRecipeSlotClick(InventoryClickEvent event, SGMenu gui) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        debug("Recipe slot click at slot %d by player %s with click type: %s", slot, player.getName(), event.getClick());

        if (event.getClick() == ClickType.LEFT) {
            // Place item from cursor
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                debug("Placing item from cursor: %s (amount: %d)", cursor.getType(), cursor.getAmount());

                ItemStack singleItem = cursor.clone();
                singleItem.setAmount(1);

                // Create recipe slot with proper lore
                SGButton newButton = createRecipeSlot(singleItem, gui);
                gui.setButton(slot, newButton);

                // Force refresh the visual
                refreshGUISlot(gui, slot, player);

                // Remove one item from cursor
                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                } else {
                    event.setCursor(null);
                }

                debug("Item placed successfully at slot %d", slot);
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            // Clear slot
            debug("Clearing recipe slot: %d", slot);
            clearRecipeSlot(gui, slot, player);
        }
    }

    private void clearRecipeSlot(SGMenu gui, int slot, Player player) {
        debug("Clearing recipe slot: %d for player: %s", slot, player.getName());

        SGButton emptySlot = createEmptyRecipeSlot(gui);
        gui.setButton(slot, emptySlot);

        // Force refresh the visual
        refreshGUISlot(gui, slot, player);

        debug("Recipe slot %d cleared successfully", slot);
    }

    private void refreshGUISlot(SGMenu gui, int slot, Player player) {
        // Use a small delay to ensure the GUI state is updated first
        Bukkit.getScheduler().runTaskLater(Setup.instance, () -> {
            try {
                SGButton button = gui.getButton(slot);
                if (button != null) {
                    Inventory openInv = player.getOpenInventory().getTopInventory();

                    // Verify this is our GUI
                    if (openInv != null && openInv.getSize() == 27) {
                        // Force update the visual slot
                        ItemStack newItem = button.getIcon();
                        openInv.setItem(slot, newItem);
                        debug("Force refreshed slot %d for player %s", slot, player.getName());
                    }
                }
            } catch (Exception e) {
                debug("Failed to refresh GUI slot %d for player %s: %s", slot, player.getName(), e.getMessage());
            }
        }, 1L);
    }

    private void loadCurrentRecipe(SGMenu gui) {
        debug("Loading current recipe from config");

        FileConfiguration config = Setup.instance.getConfig();
        List<String> recipe = config.getStringList("recipe");

        if (recipe.size() == 9) {
            debug("Found recipe with 9 items in config");

            // Map recipe positions to GUI slots
            int[] craftingSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

            for (int i = 0; i < 9; i++) {
                String itemName = recipe.get(i);
                if (itemName != null && !itemName.isEmpty() && !itemName.equals("AIR")) {
                    try {
                        ItemStack item = new ItemStack(Material.valueOf(itemName));
                        SGButton button = createRecipeSlot(item, gui);
                        gui.setButton(craftingSlots[i], button);
                        debug("Loaded item %s at position %d (slot %d)", itemName, i, craftingSlots[i]);
                    } catch (IllegalArgumentException e) {
                        debug("Invalid material name in config: %s", itemName);
                    }
                }
            }
        } else {
            debug("No valid recipe found in config (size: %d)", recipe.size());
        }
    }

    private void saveRecipeToConfig(SGMenu gui) throws IllegalArgumentException {
        debug("Saving recipe to config");

        List<String> recipe = new ArrayList<>();
        int nonAirItems = 0; // Track non-AIR items

        // Map GUI slots to recipe positions
        int[] craftingSlots = {3, 4, 5, 12, 13, 14, 21, 22, 23};

        for (int i = 0; i < 9; i++) {
            SGButton button = gui.getButton(craftingSlots[i]);
            if (button != null && button.getIcon() != null) {
                ItemStack item = button.getIcon();
                String itemName = getItemMaterialName(item);
                if (!itemName.equals("LIGHT_GRAY_STAINED_GLASS_PANE")) { // Don't save empty slots
                    recipe.add(itemName);
                    nonAirItems++; // Count valid items
                    debug("Saved item %s at position %d", itemName, i);
                } else {
                    recipe.add("AIR");
                    debug("Saved AIR at position %d", i);
                }
            } else {
                recipe.add("AIR");
                debug("Saved AIR at position %d (no button)", i);
            }
        }

        // Check if all slots are AIR - throw exception if so
        if (nonAirItems == 0) {
            debug("Recipe contains only AIR slots - throwing exception");
            throw new IllegalArgumentException("Recipe cannot contain only AIR slots. At least one ingredient must be specified.");
        }

        FileConfiguration config = Setup.instance.getConfig();
        config.set("recipe", recipe);
        Setup.instance.saveConfig();
        debug("Recipe saved to config successfully with %d non-AIR items", nonAirItems);
    }

    /**
     * Gets the material name for saving to config (without color codes)
     */
    private String getItemMaterialName(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "AIR";
        }
        return item.getType().name();
    }

    /**
     * Formats material name to CamelCase for display
     */
    private String formatItemName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return "";
        }

        String[] parts = materialName.split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase())
                        .append(part.substring(1).toLowerCase() + " ");
            }
        }

        return result.toString();
    }

    private long getGlassShuffleInterval() {
        FileConfiguration config = Setup.instance.getConfig();
        long interval = config.getLong("config_gui_shuffle_interval", DEFAULT_GLASS_SHUFFLE_INTERVAL);
        debug("Retrieved glass shuffle interval from config: %d", interval);
        return interval;
    }

    public class AnimatedGlassPanel {
        private final SGMenu gui;
        private BukkitTask animationTask;
        private volatile boolean isActive = true;
        private int iterationCount = 0;
        private final Set<UUID> trackedViewers = new HashSet<>();
        private final String guiIdentifier; // Unique identifier for this GUI instance

        // More distinct glass colors for better visibility
        private final Material[] distinctGlassPanes = {
                Material.RED_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.GREEN_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.PINK_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE
        };

        public AnimatedGlassPanel(SGMenu gui) {
            this.gui = gui;
            this.guiIdentifier = "RecipeGUI_" + System.currentTimeMillis() + "_" + gui.hashCode();
            debug("Created AnimatedGlassPanel with identifier: %s", guiIdentifier);
        }

        public String getIdentifier() {
            return this.guiIdentifier;
        }

        // Add method to register a viewer
        public void addViewer(Player player) {
            trackedViewers.add(player.getUniqueId());
            debug("Added viewer: %s (Total: %d)", player.getName(), trackedViewers.size());
        }

        // Add method to remove a viewer
        public void removeViewer(Player player) {
            trackedViewers.remove(player.getUniqueId());
            debug("Removed viewer: %s (Total: %d)", player.getName(), trackedViewers.size());

            // Stop animation if no viewers left
            if (trackedViewers.isEmpty()) {
                debug("No tracked viewers left - stopping animation");
                stopAnimation();
            }
        }

        public void startAnimation() {
            debug("Starting glass panel animation");

            // Initial glass pane setup
            setupInitialGlassPanes();

            // Get shuffle interval from config
            long shuffleInterval = getGlassShuffleInterval();
            debug("Using glass shuffle interval: %d ticks", shuffleInterval);

            // Start animation timer with non-blocking approach
            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isActive) {
                        debug("Animation task cancelled - isActive is false");
                        cancel();
                        return;
                    }

                    // Debug logging
                    iterationCount++;
                    if (iterationCount % 10 == 0) { // Log every 10th iteration
                        debug("Glass animation iteration: %d (Tracked: %d)", iterationCount, trackedViewers.size());
                    }

                    // Schedule the update on the main thread to avoid blocking
                    Bukkit.getScheduler().runTask(Setup.instance, () -> {
                        try {
                            updateGlassPanesSafely();
                        } catch (Exception e) {
                            debug("Glass panel update error: %s", e.getMessage());
                            isActive = false; // Signal to stop on next iteration
                        }
                    });
                }
            }.runTaskTimerAsynchronously(Setup.instance, 0L, shuffleInterval);

            debug("Glass panel animation started successfully");
        }

        private void setupInitialGlassPanes() {
            debug("Setting up initial glass panes");

            // First 3 columns (slots 0-2, 9-11, 18-20)
            int[] leftSlots = {0, 1, 2, 9, 10, 11, 18, 19, 20};
            // Last 3 columns (slots 6-8, 15-17, 24-26)
            int[] rightSlots = {6, 7, 8, 15, 16, 17, 24, 25, 26};

            // Setup left side glass panes
            for (int slot : leftSlots) {
                if (slot != 18) { // Don't override exit button
                    Material randomGlass = distinctGlassPanes[random.nextInt(distinctGlassPanes.length)];
                    SGButton glassButton = new SGButton(
                            new ItemBuilder(randomGlass)
                                    .name("§7")
                                    .build()
                    ).withListener(event -> event.setCancelled(true));
                    gui.setButton(slot, glassButton);
                }
            }

            // Setup right side glass panes
            for (int slot : rightSlots) {
                if (slot != 26) { // Don't override confirm button
                    Material randomGlass = distinctGlassPanes[random.nextInt(distinctGlassPanes.length)];
                    SGButton glassButton = new SGButton(
                            new ItemBuilder(randomGlass)
                                    .name("§7")
                                    .build()
                    ).withListener(event -> event.setCancelled(true));
                    gui.setButton(slot, glassButton);
                }
            }

            debug("Initial glass panes setup completed");
        }

        private void updateGlassPanesSafely() {
            // Check if we have any tracked viewers
            if (trackedViewers.isEmpty()) {
                debug("No tracked viewers - stopping animation");
                stopAnimation();
                return;
            }

            // Clean up tracked viewers and get valid ones
            List<Player> validViewers = new ArrayList<>();
            Iterator<UUID> iterator = trackedViewers.iterator();
            while (iterator.hasNext()) {
                UUID viewerId = iterator.next();
                Player player = Bukkit.getPlayer(viewerId);

                if (player == null || !player.isOnline()) {
                    debug("Removing offline player: %s", viewerId);
                    iterator.remove();
                    continue;
                }

                // Multi-layered inventory validation
                if (player.getOpenInventory() != null) {
                    Inventory topInv = player.getOpenInventory().getTopInventory();
                    String topTitle = player.getOpenInventory().getTitle();

                    debug("Player %s has inventory: %s", player.getName(), topTitle);

                    // Check 1: Title match
                    boolean titleMatch = topTitle != null && topTitle.equals("§6Recipe Configuration");

                    // Check 2: Inventory size match (27 slots for 3 rows)
                    boolean sizeMatch = topInv.getSize() == 27;

                    // Check 3: Check for our specific button items (exit and confirm buttons)
                    boolean hasOurButtons = false;
                    try {
                        ItemStack exitItem = topInv.getItem(18);
                        ItemStack confirmItem = topInv.getItem(26);

                        hasOurButtons = (exitItem != null && exitItem.getType() == Material.BARRIER) &&
                                (confirmItem != null && confirmItem.getType() == Material.EMERALD);
                    } catch (Exception e) {
                        // If we can't check buttons, skip this validation
                    }

                    // Check 4: Object reference equality as backup
                    boolean objectMatch = topInv.equals(gui.getInventory());

                    debug("Validation for %s - Title: %b, Size: %b, Buttons: %b, Object: %b",
                            player.getName(), titleMatch, sizeMatch, hasOurButtons, objectMatch);

                    // Accept if we have title + size + buttons, or if we have object match
                    if ((titleMatch && sizeMatch && hasOurButtons) || objectMatch) {
                        validViewers.add(player);
                        debug("Valid viewer found: %s", player.getName());
                    } else {
                        debug("Player %s validation failed", player.getName());
                        iterator.remove();
                    }
                } else {
                    debug("Player %s has no open inventory", player.getName());
                    iterator.remove();
                }
            }

            if (validViewers.isEmpty()) {
                debug("No valid viewers found - stopping animation");
                stopAnimation();
                return;
            }

            debug("Updating glass panes for %d valid viewers", validViewers.size());

            // Update glass panes
            int[] leftSlots = {0, 1, 2, 9, 10, 11, 18, 19, 20};
            int[] rightSlots = {6, 7, 8, 15, 16, 17, 24, 25, 26};

            updateGlassPaneBatch(leftSlots, validViewers);
            updateGlassPaneBatch(rightSlots, validViewers);
        }

        private void updateGlassPaneBatch(int[] slots, List<Player> viewers) {
            for (int slot : slots) {
                if ((slot == 18 || slot == 26)) continue; // Skip button slots

                try {
                    Material newGlass = distinctGlassPanes[random.nextInt(distinctGlassPanes.length)];

                    // Create new button
                    SGButton glassButton = new SGButton(
                            new ItemBuilder(newGlass)
                                    .name("§7")
                                    .build()
                    ).withListener(event -> event.setCancelled(true));

                    // Update in SpiGUI
                    gui.setButton(slot, glassButton);

                    // Direct inventory update for immediate visual feedback
                    ItemStack newItem = glassButton.getIcon();
                    for (Player viewer : viewers) {
                        try {
                            if (viewer.getOpenInventory() != null) {
                                Inventory topInv = viewer.getOpenInventory().getTopInventory();
                                String topTitle = viewer.getOpenInventory().getTitle();

                                // Use same validation as above
                                boolean titleMatch = topTitle != null && topTitle.equals("§6Recipe Configuration");
                                boolean sizeMatch = topInv.getSize() == 27;
                                boolean objectMatch = topInv.equals(gui.getInventory());

                                if ((titleMatch && sizeMatch) || objectMatch) {
                                    topInv.setItem(slot, newItem);
                                    debug("Updated slot %d for viewer %s", slot, viewer.getName());
                                }
                            }
                        } catch (Exception e) {
                            debug("Failed to update inventory for viewer %s: %s", viewer.getName(), e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    debug("Failed to update glass pane at slot %d: %s", slot, e.getMessage());
                }
            }
        }

        public void stopAnimation() {
            synchronized (this) {
                debug("Stopping glass panel animation");
                isActive = false;
                if (animationTask != null && !animationTask.isCancelled()) {
                    animationTask.cancel();
                    debug("Animation task cancelled");
                }
                trackedViewers.clear();
                debug("All tracked viewers cleared");
            }
        }
    }
}