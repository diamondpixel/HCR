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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static takys.Utilities.Utilities.debug;
import static takys.Utilities.Utilities.reloadRecipe;

public class RecipeConfiguratorGUI {
    // Constants
    private static final int GUI_ROWS = 3;
    private static final int GUI_SIZE = GUI_ROWS * 9;
    private static final long DEFAULT_GLASS_SHUFFLE_INTERVAL = 10L;
    private static final String GUI_TITLE = "§6Recipe Configuration";
    private static final int EXIT_SLOT = 18;
    private static final int CONFIRM_SLOT = 26;
    private static final int[] CRAFTING_SLOTS = {3, 4, 5, 12, 13, 14, 21, 22, 23};
    private static final int[] LEFT_GLASS_SLOTS = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int[] RIGHT_GLASS_SLOTS = {6, 7, 8, 15, 16, 17, 24, 25, 26};

    // Reusable objects to reduce GC pressure
    private static final List<String> EMPTY_SLOT_LORE = Arrays.asList(
            "§eLeft-click: §7Place item from cursor",
            "§eRight-click: §7Clear this slot"
    );

    // Glass materials array - static final for better performance
    private static final Material[] GLASS_MATERIALS = {
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

    private final Random random = new Random();
    private AnimatedGlassPanel glassPanel;

    public SGMenu createRecipeGUI(Player player) {
        debug("Creating recipe GUI for player: %s", player.getName());

        SGMenu gui = Setup.spiGUI.create(GUI_TITLE, GUI_ROWS);
        gui.setAutomaticPaginationEnabled(false);

        setupRecipeSlots(gui);
        setupControlButtons(gui, player);
        loadCurrentRecipe(gui);

        // Initialize animated glass panel
        glassPanel = new AnimatedGlassPanel(gui);
        Setup.instance.activeRecipeConfiguratorGUIs.put(player.getUniqueId(), glassPanel);

        player.setMetadata("recipeGUI_id", new FixedMetadataValue(Setup.instance, glassPanel.getIdentifier()));
        glassPanel.addViewer(player);
        glassPanel.startAnimation();

        debug("Recipe GUI created successfully for player: %s", player.getName());
        return gui;
    }

    private void setupRecipeSlots(SGMenu gui) {
        debug("Setting up recipe slots");
        Arrays.stream(CRAFTING_SLOTS).forEach(slot ->
                gui.setButton(slot, createEmptyRecipeSlot(gui))
        );
    }

    private void setupControlButtons(SGMenu gui, Player player) {
        // Exit button
        gui.setButton(EXIT_SLOT, new SGButton(
                new ItemBuilder(Material.BARRIER)
                        .name("§cExit")
                        .lore("§7Click to exit without saving")
                        .build()
        ).withListener(event -> handleExitClick(event, player)));

        // Confirm button
        gui.setButton(CONFIRM_SLOT, new SGButton(
                new ItemBuilder(Material.EMERALD)
                        .name("§aConfirm Changes")
                        .lore("§7Click to save the recipe")
                        .build()
        ).withListener(event -> handleConfirmClick(event, gui, player)));
    }

    private void handleExitClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        debug("Exit button clicked by player: %s", player.getName());

        cleanupAndClose(player, "§cRecipe configuration cancelled!");
    }

    private void handleConfirmClick(InventoryClickEvent event, SGMenu gui, Player player) {
        event.setCancelled(true);
        debug("Confirm button clicked by player: %s", player.getName());

        try {
            saveRecipeToConfig(gui);
            reloadRecipe(Bukkit.getOnlinePlayers());
            cleanupAndClose(player, "§aRecipe configuration saved successfully!");
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cError saving recipe: " + e.getMessage());
        }
    }

    private void cleanupAndClose(Player player, String message) {
        if (glassPanel != null) {
            glassPanel.removeViewer(player);
            glassPanel.stopAnimation();
        }
        Setup.instance.activeRecipeConfiguratorGUIs.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage(message);
    }

    private SGButton createEmptyRecipeSlot(SGMenu gui) {
        return new SGButton(
                new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                        .name("§7Empty Recipe Slot")
                        .lore(EMPTY_SLOT_LORE)
                        .build()
        ).withListener(event -> {
            event.setCancelled(true);
            handleRecipeSlotClick(event, gui);
        });
    }

    private SGButton createRecipeSlot(ItemStack item, SGMenu gui) {
        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            String originalName = meta.hasDisplayName() ? meta.getDisplayName() :
                    formatItemName(item.getType().name());
            meta.setDisplayName("§5" + originalName);

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.addAll(EMPTY_SLOT_LORE);
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return new SGButton(displayItem).withListener(event -> {
            event.setCancelled(true);
            handleRecipeSlotClick(event, gui);
        });
    }

    private void handleRecipeSlotClick(InventoryClickEvent event, SGMenu gui) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        debug("Recipe slot click at slot %d by player %s with click type: %s",
                slot, player.getName(), event.getClick());

        if (event.getClick() == ClickType.LEFT) {
            handleLeftClick(event, gui, slot, player);
        } else if (event.getClick() == ClickType.RIGHT) {
            clearRecipeSlot(gui, slot, player);
        }
    }

    private void handleLeftClick(InventoryClickEvent event, SGMenu gui, int slot, Player player) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) return;

        debug("Placing item from cursor: %s (amount: %d)", cursor.getType(), cursor.getAmount());

        ItemStack singleItem = cursor.clone();
        singleItem.setAmount(1);

        gui.setButton(slot, createRecipeSlot(singleItem, gui));
        refreshGUISlot(gui, slot, player);

        // Update cursor
        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
        } else {
            event.setCursor(null);
        }

        debug("Item placed successfully at slot %d", slot);
    }

    private void clearRecipeSlot(SGMenu gui, int slot, Player player) {
        debug("Clearing recipe slot: %d for player: %s", slot, player.getName());
        gui.setButton(slot, createEmptyRecipeSlot(gui));
        refreshGUISlot(gui, slot, player);
    }

    private void refreshGUISlot(SGMenu gui, int slot, Player player) {
        Bukkit.getScheduler().runTaskLater(Setup.instance, () -> {
            try {
                SGButton button = gui.getButton(slot);
                if (button != null) {
                    Inventory openInv = player.getOpenInventory().getTopInventory();
                    if (openInv != null && openInv.getSize() == GUI_SIZE) {
                        openInv.setItem(slot, button.getIcon());
                        debug("Refreshed slot %d for player %s", slot, player.getName());
                    }
                }
            } catch (Exception e) {
                debug("Failed to refresh GUI slot %d: %s", slot, e.getMessage());
            }
        }, 1L);
    }

    private void loadCurrentRecipe(SGMenu gui) {
        debug("Loading current recipe from config");

        List<String> recipe = Setup.instance.getConfig().getStringList("recipe");
        if (recipe.size() != 9) {
            debug("No valid recipe found in config (size: %d)", recipe.size());
            return;
        }

        for (int i = 0; i < 9; i++) {
            String itemName = recipe.get(i);
            if (itemName != null && !itemName.isEmpty() && !"AIR".equals(itemName)) {
                try {
                    ItemStack item = new ItemStack(Material.valueOf(itemName));
                    gui.setButton(CRAFTING_SLOTS[i], createRecipeSlot(item, gui));
                    debug("Loaded item %s at position %d", itemName, i);
                } catch (IllegalArgumentException e) {
                    debug("Invalid material name in config: %s", itemName);
                }
            }
        }
    }

    private void saveRecipeToConfig(SGMenu gui) throws IllegalArgumentException {
        debug("Saving recipe to config");

        List<String> recipe = new ArrayList<>(9);
        int nonAirItems = 0;

        for (int slot : CRAFTING_SLOTS) {
            SGButton button = gui.getButton(slot);
            String itemName = "AIR";

            if (button != null && button.getIcon() != null) {
                String materialName = getItemMaterialName(button.getIcon());
                if (!"LIGHT_GRAY_STAINED_GLASS_PANE".equals(materialName)) {
                    itemName = materialName;
                    nonAirItems++;
                }
            }

            recipe.add(itemName);
            debug("Saved item %s at slot %d", itemName, slot);
        }

        if (nonAirItems == 0) {
            throw new IllegalArgumentException("Recipe cannot contain only AIR slots. At least one ingredient must be specified.");
        }

        FileConfiguration config = Setup.instance.getConfig();
        config.set("recipe", recipe);
        Setup.instance.saveConfig();
        debug("Recipe saved to config successfully with %d non-AIR items", nonAirItems);
    }

    private String getItemMaterialName(ItemStack item) {
        return (item == null || item.getType() == Material.AIR) ? "AIR" : item.getType().name();
    }

    private String formatItemName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return "";

        return Arrays.stream(materialName.split("_"))
                .filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private long getGlassShuffleInterval() {
        long interval = Setup.instance.getConfig().getLong("config_gui_shuffle_interval", DEFAULT_GLASS_SHUFFLE_INTERVAL);
        debug("Retrieved glass shuffle interval: %d", interval);
        return interval;
    }

    public class AnimatedGlassPanel {
        private final SGMenu gui;
        private final String guiIdentifier;
        private final Set<UUID> trackedViewers = ConcurrentHashMap.newKeySet();

        private volatile BukkitTask animationTask;
        private volatile boolean isActive = true;
        private int iterationCount = 0;

        public AnimatedGlassPanel(SGMenu gui) {
            this.gui = gui;
            this.guiIdentifier = "RecipeGUI_" + System.currentTimeMillis() + "_" + gui.hashCode();
            debug("Created AnimatedGlassPanel with identifier: %s", guiIdentifier);
        }

        public String getIdentifier() {
            return guiIdentifier;
        }

        public void addViewer(Player player) {
            trackedViewers.add(player.getUniqueId());
            debug("Added viewer: %s (Total: %d)", player.getName(), trackedViewers.size());
        }

        public void removeViewer(Player player) {
            trackedViewers.remove(player.getUniqueId());
            debug("Removed viewer: %s (Total: %d)", player.getName(), trackedViewers.size());

            if (trackedViewers.isEmpty()) {
                debug("No tracked viewers left - stopping animation");
                stopAnimation();
            }
        }

        public void startAnimation() {
            debug("Starting glass panel animation");
            setupInitialGlassPanes();

            long shuffleInterval = getGlassShuffleInterval();
            animationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!isActive) {
                        cancel();
                        return;
                    }

                    iterationCount++;
                    if (iterationCount % 20 == 0) { // Reduced logging frequency
                        debug("Glass animation iteration: %d (Viewers: %d)", iterationCount, trackedViewers.size());
                    }

                    Bukkit.getScheduler().runTask(Setup.instance, () -> {
                        try {
                            updateGlassPanes();
                        } catch (Exception e) {
                            debug("Glass panel update error: %s", e.getMessage());
                            isActive = false;
                        }
                    });
                }
            }.runTaskTimerAsynchronously(Setup.instance, 0L, shuffleInterval);
        }

        private void setupInitialGlassPanes() {
            debug("Setting up initial glass panes");
            updateGlassSlots(LEFT_GLASS_SLOTS, EXIT_SLOT);
            updateGlassSlots(RIGHT_GLASS_SLOTS, CONFIRM_SLOT);
        }

        private void updateGlassSlots(int[] slots, int skipSlot) {
            for (int slot : slots) {
                if (slot == skipSlot) continue;

                Material randomGlass = GLASS_MATERIALS[random.nextInt(GLASS_MATERIALS.length)];
                SGButton glassButton = new SGButton(
                        new ItemBuilder(randomGlass).name("§7").build()
                ).withListener(event -> event.setCancelled(true));

                gui.setButton(slot, glassButton);
            }
        }

        private void updateGlassPanes() {
            if (trackedViewers.isEmpty()) {
                stopAnimation();
                return;
            }

            List<Player> validViewers = getValidViewers();
            if (validViewers.isEmpty()) {
                stopAnimation();
                return;
            }

            debug("Updating glass panes for %d valid viewers", validViewers.size());
            updateGlassSlots(LEFT_GLASS_SLOTS, EXIT_SLOT);
            updateGlassSlots(RIGHT_GLASS_SLOTS, CONFIRM_SLOT);

            // Update visual for all valid viewers
            updateViewerInventories(validViewers);
        }

        private List<Player> getValidViewers() {
            return trackedViewers.stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .filter(Player::isOnline)
                    .filter(this::hasValidInventory)
                    .collect(Collectors.toList());
        }

        private boolean hasValidInventory(Player player) {
            if (player.getOpenInventory() == null) return false;

            Inventory topInv = player.getOpenInventory().getTopInventory();
            String title = player.getOpenInventory().getTitle();

            boolean titleMatch = GUI_TITLE.equals(title);
            boolean sizeMatch = topInv.getSize() == GUI_SIZE;
            boolean hasButtons = hasControlButtons(topInv);

            return titleMatch && sizeMatch && hasButtons;
        }

        private boolean hasControlButtons(Inventory inv) {
            try {
                ItemStack exitItem = inv.getItem(EXIT_SLOT);
                ItemStack confirmItem = inv.getItem(CONFIRM_SLOT);
                return (exitItem != null && exitItem.getType() == Material.BARRIER) &&
                        (confirmItem != null && confirmItem.getType() == Material.EMERALD);
            } catch (Exception e) {
                return false;
            }
        }

        private void updateViewerInventories(List<Player> viewers) {
            int[] allGlassSlots = Arrays.stream(new int[][]{LEFT_GLASS_SLOTS, RIGHT_GLASS_SLOTS})
                    .flatMapToInt(Arrays::stream)
                    .filter(slot -> slot != EXIT_SLOT && slot != CONFIRM_SLOT)
                    .toArray();

            for (int slot : allGlassSlots) {
                SGButton button = gui.getButton(slot);
                if (button == null) continue;

                ItemStack newItem = button.getIcon();
                for (Player viewer : viewers) {
                    try {
                        viewer.getOpenInventory().getTopInventory().setItem(slot, newItem);
                    } catch (Exception e) {
                        debug("Failed to update slot %d for viewer %s: %s", slot, viewer.getName(), e.getMessage());
                    }
                }
            }
        }

        public synchronized void stopAnimation() {
            debug("Stopping glass panel animation");
            isActive = false;

            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
                animationTask = null;
            }

            trackedViewers.clear();
            debug("Animation stopped and viewers cleared");
        }
    }
}