package takys.Utilities;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import takys.Files.DataManager;
import takys.Objects.Pair;
import takys.Objects.PlayerObj;
import com.skullcreator.SkullCreator;
import takys.Setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Optimized Utilities class with improved performance, clarity, and maintainability
 */
public class Utilities {

    // Constants - moved to top for better visibility
    private static final Setup SETUP = Setup.instance;
    private static final DataManager DATA_MANAGER = Setup.dataManager;

    // Pre-encoded skull textures
    private static final String BACK_ARROW_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=";
    private static final String WORLD_SPAWN_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4OWQ1YjE3ODYyNmVhMjNkMGIwYzNkMmRmNWMwODVlODM3NTA1NmJmNjg1YjVlZDViYjQ3N2ZlODQ3MmQ5NCJ9fX0=";
    private static final String EYE_LOCATION_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQyY2Y4Y2U0ODdiNzhmYTIwM2Q1NmNmMDE0OTE0MzRiNGMzM2U1ZDIzNjgwMmM2ZDY5MTQ2YTUxNDM1YjAzZCJ9fX0=";

    // Default recipe pattern - cleaner definition
    private static final List<String> DEFAULT_RECIPE = List.of(
            "AIR", "DIAMOND", "AIR",
            "DIAMOND", "NETHER_STAR", "DIAMOND",
            "AIR", "DIAMOND", "AIR"
    );

    // Formatters and caches
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy-HH.mm.ss");
    private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = ThreadLocal.withInitial(() -> new StringBuilder(128));

    // ==================== ITEM MANAGEMENT ====================

    /**
     * Removes the first matching item from player inventory
     * Optimized with early return and single pass
     */
    public static void removeFirstItem(Player player, ItemStack targetStack) {
        if (player == null || targetStack == null || !targetStack.hasItemMeta()) {
            return;
        }

        final ItemMeta targetMeta = targetStack.getItemMeta();
        final ItemStack[] contents = player.getInventory().getContents();

        for (ItemStack item : contents) {
            if (item != null && item.hasItemMeta() && Objects.equals(item.getItemMeta(), targetMeta)) {
                item.setAmount(item.getAmount() - 1);
                return;
            }
        }
    }

    // ==================== FILE OPERATIONS ====================

    /**
     * Retrieves string from properties file with caching
     * Uses try-with-resources for proper resource management
     */
    public static String getString(String key, File file) {
        final String cacheKey = file.getAbsolutePath() + ":" + key;

        return FILE_CACHE.computeIfAbsent(cacheKey, k -> {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty(key, "");
            } catch (IOException e) {
                debug("Failed to read file %s: %s", file.getAbsolutePath(), e.getMessage());
                return "";
            }
        });
    }

    // ==================== SKULL CREATION ====================

    /**
     * Creates player skull with dynamic time information
     * Fresh creation ensures accurate time display
     */
    public static ItemStack getPlayerSkull(PlayerObj pObj) {
        final UUID uuid = pObj.getUUID();
        final Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return null;
        }

        final ItemStack skull = SkullCreator.itemFromUuid(uuid);
        final ItemMeta meta = skull.getItemMeta();

        meta.setDisplayName(player.getName());
        meta.setLore(createPlayerSkullLore(pObj, player));
        skull.setItemMeta(meta);

        return skull;
    }

    /**
     * Creates lore for player skull with time information
     */
    private static List<String> createPlayerSkullLore(PlayerObj pObj, Player player) {
        final Duration duration = Duration.between(pObj.getDate(), LocalDateTime.now());
        final TimeComponents time = new TimeComponents(duration);

        return List.of(
                String.format("Died %dd, %dh, %dm, %ds ago", time.days, time.hours, time.minutes, time.seconds),
                "Cause of Death: " + getSerializedDamageCause(pObj.getDamageCause()),
                "Death Count: " + player.getStatistic(Statistic.DEATHS),
                "Coordinates of latest death:",
                serializedToFormattedString(getSerializedLocation(pObj.getLoc()))
        );
    }

    /**
     * Time components helper class for better readability
     */
    private static class TimeComponents {
        final long days, hours, minutes, seconds;

        TimeComponents(Duration duration) {
            this.days = duration.toDays();
            this.hours = duration.minusDays(days).toHours();
            this.minutes = duration.minusDays(days).minusHours(hours).toMinutes();
            this.seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();
        }
    }

    // ==================== RECIPE MANAGEMENT ====================

    /**
     * Constructs revival token recipe with validation
     * Returns pair of recipe and token item
     */
    public static Pair<ShapedRecipe, ItemStack> constructRecipe() {
        final ItemStack token = createRevivalToken();
        final NamespacedKey key = new NamespacedKey(SETUP, "Token");
        final ShapedRecipe recipe = new ShapedRecipe(key, token);

        configureRecipeShape(recipe);

        return new Pair<>(recipe, token);
    }

    /**
     * Creates the revival token item
     */
    private static ItemStack createRevivalToken() {
        final ItemStack token = new ItemStack(Material.END_CRYSTAL, 1);
        token.addUnsafeEnchantment(Enchantment.LOYALTY, 10);

        final ItemMeta meta = token.getItemMeta();
        meta.setDisplayName("Revival Token");
        meta.setLore(List.of("Choose a dead player to revive."));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        token.setItemMeta(meta);

        return token;
    }

    /**
     * Configures recipe shape and ingredients
     */
    private static void configureRecipeShape(ShapedRecipe recipe) {
        recipe.shape("123", "456", "789");

        final List<String> materials = getValidRecipeFromConfig();
        final char[] positions = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};

        for (int i = 0; i < Math.min(positions.length, materials.size()); i++) {
            final String materialName = materials.get(i);
            if (isValidMaterial(materialName)) {
                try {
                    recipe.setIngredient(positions[i], Material.valueOf(materialName));
                } catch (IllegalArgumentException e) {
                    debug("Invalid material in recipe position %d: %s", i, materialName);
                }
            }
        }
    }

    /**
     * Checks if material name is valid for recipe
     */
    private static boolean isValidMaterial(String materialName) {
        return materialName != null && !materialName.isEmpty() && !"AIR".equals(materialName);
    }

    /**
     * Gets validated recipe from config with fallback to default
     */
    private static List<String> getValidRecipeFromConfig() {
        final List<String> configRecipe = SETUP.getConfig().getStringList("recipe");

        if (configRecipe.size() != 9) {
            debug("Invalid recipe size in config: %d, using default", configRecipe.size());
            return handleInvalidRecipe("Invalid recipe size");
        }

        final RecipeValidator validator = new RecipeValidator(configRecipe);
        return validator.isValid() ? validator.getValidatedRecipe() : handleInvalidRecipe("Invalid materials found");
    }

    /**
     * Recipe validator helper class
     */
    private static class RecipeValidator {
        private final List<String> configRecipe;
        private final List<String> validatedRecipe = new ArrayList<>();
        private boolean valid = true;
        private int nonAirCount = 0;

        RecipeValidator(List<String> configRecipe) {
            this.configRecipe = configRecipe;
            validate();
        }

        private void validate() {
            for (int i = 0; i < 9 && valid; i++) {
                final String materialName = configRecipe.get(i);

                if (materialName == null) {
                    debug("Null material at position %d", i);
                    valid = false;
                } else if (materialName.isEmpty() || "AIR".equals(materialName)) {
                    validatedRecipe.add("AIR");
                } else {
                    if (isValidMaterialEnum(materialName)) {
                        validatedRecipe.add(materialName.toUpperCase());
                        nonAirCount++;
                    } else {
                        debug("Invalid material at position %d: %s", i, materialName);
                        valid = false;
                    }
                }
            }

            if (valid && nonAirCount == 0) {
                debug("Recipe contains only AIR slots");
                valid = false;
            }
        }

        private boolean isValidMaterialEnum(String materialName) {
            try {
                Material.valueOf(materialName.toUpperCase());
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        boolean isValid() { return valid; }
        List<String> getValidatedRecipe() { return validatedRecipe; }
    }

    /**
     * Handles invalid recipe by saving default to config
     */
    private static List<String> handleInvalidRecipe(String reason) {
        debug("Recipe validation failed: %s, writing default recipe to config", reason);

        SETUP.getConfig().set("recipe", DEFAULT_RECIPE);
        SETUP.saveConfig();

        return new ArrayList<>(DEFAULT_RECIPE);
    }

    /**
     * Reloads recipe for all players
     */
    public static void reloadRecipe(Collection<? extends Player> players) {
        unloadRecipe(players);
        loadRecipe(players);
    }

    /**
     * Loads new recipe and discovers it for players
     */
    public static void loadRecipe(Collection<? extends Player> players) {
        final Pair<ShapedRecipe, ItemStack> recipeData = constructRecipe();
        Setup.Item = recipeData.getValue();
        Setup.Recipe = recipeData.getKey();

        final NamespacedKey recipeKey = Setup.Recipe.getKey();
        debug("Loading recipe with key: %s for %d players", recipeKey, players.size());

        Bukkit.addRecipe(Setup.Recipe);
        players.forEach(player -> player.discoverRecipe(recipeKey));
    }

    /**
     * Unloads old recipe and undiscovers it for players
     */
    public static void unloadRecipe(Collection<? extends Player> players) {
        if (Setup.Recipe == null) return;

        final NamespacedKey oldRecipeKey = Setup.Recipe.getKey();
        debug("Unloading recipe with key: %s for %d players", oldRecipeKey, players.size());

        Bukkit.removeRecipe(oldRecipeKey);
        players.forEach(player -> player.undiscoverRecipe(oldRecipeKey));
    }

    // ==================== GUI ITEMS ====================

    public static ItemStack backArrowItem() {
        return createSkullItem(BACK_ARROW_HEAD, "§3§lGo Back!");
    }

    public static ItemStack worldSpawnItem(UUID uuid) {
        final String playerName = getPlayerName(uuid);
        return createSkullItem(WORLD_SPAWN_HEAD, "§3§lWorld Spawn",
                "§3Click to respawn " + playerName,
                "§3at world's spawnpoint!");
    }

    public static ItemStack eyeLocationItem(UUID uuid) {
        final String playerName = getPlayerName(uuid);
        return createSkullItem(EYE_LOCATION_HEAD, "§3§lSimple Revive",
                "§3Click to respawn " + playerName,
                "§3where you are looking!");
    }

    /**
     * Helper method to create skull items with lore
     */
    private static ItemStack createSkullItem(String base64, String displayName, String... lore) {
        final ItemStack skull = SkullCreator.itemFromBase64(base64);
        final ItemMeta meta = skull.getItemMeta();

        meta.setDisplayName(displayName);
        if (lore.length > 0) {
            meta.setLore(List.of(lore));
        }

        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * Creates death location item based on world environment
     */
    public static ItemStack deathLocationItem(PlayerObj pObj) {
        final World.Environment env = pObj.getLoc().getWorld().getEnvironment();
        final Material material = getEnvironmentMaterial(env);
        final String playerName = getPlayerName(pObj.getUUID());
        final String coordinates = serializedToFormattedString(getSerializedLocation(pObj.getLoc()));

        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§0§lDeath Location");
        meta.setLore(List.of(
                "§3Click to respawn " + playerName,
                "§3where they died! (" + coordinates + ")"
        ));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets appropriate material based on world environment
     */
    private static Material getEnvironmentMaterial(World.Environment env) {
        return switch (env) {
            case NETHER -> Material.WARPED_NYLIUM;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    /**
     * Creates bed/respawn anchor item based on player's spawn location
     */
    public static ItemStack bedItem(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return new ItemStack(Material.RED_BED);
        }

        final Location bedLocation = player.getBedSpawnLocation();
        final String playerName = player.getName();

        if (bedLocation == null) {
            return createNoBedItem(playerName);
        }

        final boolean isNether = bedLocation.getWorld().getEnvironment() == World.Environment.NETHER;
        return isNether ? createRespawnAnchorItem(playerName) : createBedItem(playerName);
    }

    private static ItemStack createNoBedItem(String playerName) {
        final ItemStack bed = new ItemStack(Material.RED_BED);
        final ItemMeta meta = bed.getItemMeta();
        meta.setDisplayName("§4§lBed");
        meta.setLore(List.of("§3" + playerName + " doesn't have a bed :("));
        bed.setItemMeta(meta);
        return bed;
    }

    private static ItemStack createRespawnAnchorItem(String playerName) {
        final ItemStack anchor = new ItemStack(Material.RESPAWN_ANCHOR);
        final ItemMeta meta = anchor.getItemMeta();
        meta.setDisplayName("§5§lRespawn Anchor");
        meta.setLore(List.of(
                "§3Click to respawn " + playerName,
                "§3at his respawn anchor!"
        ));
        anchor.setItemMeta(meta);
        return anchor;
    }

    private static ItemStack createBedItem(String playerName) {
        final ItemStack bed = new ItemStack(Material.GREEN_BED);
        final ItemMeta meta = bed.getItemMeta();
        meta.setDisplayName("§a§lBed");
        meta.setLore(List.of(
                "§3Click to respawn " + playerName,
                "§3at his bed!"
        ));
        bed.setItemMeta(meta);
        return bed;
    }

    // ==================== PLAYER REVIVAL ====================

    /**
     * Revives a player with effects and cleanup
     */
    public static void revivePlayer(Player viewer, PlayerObj pObj, Location loc) {
        final Player player = Bukkit.getPlayer(pObj.getUUID());
        if (player == null || !player.isOnline()) {
            return;
        }

        // Cleanup and preparation
        performRevivalCleanup(viewer, pObj);

        // Teleport and set game mode
        player.teleport(loc);
        player.setGameMode(GameMode.SURVIVAL);

        // Effects and sounds
        applyRevivalEffects(player, loc);
        applyRevivalPotionEffects(player);
    }

    /**
     * Performs cleanup operations for revival
     */
    private static void performRevivalCleanup(Player viewer, PlayerObj pObj) {
        viewer.closeInventory();
        removeFirstItem(viewer, SETUP.Item);
        SETUP.DeadPlayers.remove(pObj);

        final String filePath = SETUP.getDataFolder() + File.separator + "DeadPlayers" + File.separator + pObj.getUUID() + ".json";
        DATA_MANAGER.deleteJsonFile(filePath);
    }

    /**
     * Applies visual and audio effects for revival
     */
    private static void applyRevivalEffects(Player player, Location loc) {
        loc.getWorld().strikeLightningEffect(loc);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
    }

    /**
     * Applies potion effects to revived player
     */
    public static void applyRevivalPotionEffects(Player player) {
        final int duration = SETUP.getConfig().getInt("effect_duration", 2400);

        final List<PotionEffect> effects = List.of(
                new PotionEffect(PotionEffectType.RESISTANCE, duration, 0),
                new PotionEffect(PotionEffectType.ABSORPTION, duration, 0),
                new PotionEffect(PotionEffectType.REGENERATION, duration, 4),
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, 1),
                new PotionEffect(PotionEffectType.WATER_BREATHING, duration, 1),
                new PotionEffect(PotionEffectType.LUCK, duration, 0),
                new PotionEffect(PotionEffectType.GLOWING, 60, 0)
        );

        effects.forEach(player::addPotionEffect);
    }

    // ==================== SERIALIZATION ====================

    /**
     * Serializes location to string format
     */
    public static String getSerializedLocation(Location loc) {
        final StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0);
        return sb.append(loc.getX()).append(";")
                .append(loc.getY()).append(";")
                .append(loc.getZ()).append(";")
                .append(loc.getWorld().getUID()).toString();
    }

    /**
     * Deserializes location from string format
     */
    public static Location getDeserializedLocation(String serialized) {
        try {
            final String[] parts = serialized.split(";");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid location format: expected 4 parts, got " + parts.length);
            }

            final double x = Double.parseDouble(parts[0]);
            final double y = Double.parseDouble(parts[1]);
            final double z = Double.parseDouble(parts[2]);
            final UUID worldUUID = UUID.fromString(parts[3]);
            final World world = Bukkit.getServer().getWorld(worldUUID);

            if (world == null) {
                throw new IllegalArgumentException("World not found: " + worldUUID);
            }

            return new Location(world, x, y, z);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize location: " + serialized, e);
        }
    }

    /**
     * Formats serialized coordinates for display
     */
    public static String serializedToFormattedString(String serialized) {
        try {
            final String[] parts = serialized.split(";");
            if (parts.length != 4) return "Invalid coordinates";

            final int x = (int) Double.parseDouble(parts[0]);
            final int y = (int) Double.parseDouble(parts[1]);
            final int z = (int) Double.parseDouble(parts[2]);
            final World world = Bukkit.getServer().getWorld(UUID.fromString(parts[3]));
            final String worldName = world != null ? world.getName() : "Unknown";

            final StringBuilder sb = STRING_BUILDER.get();
            sb.setLength(0);
            return sb.append(worldName).append(" , ")
                    .append(x).append(" , ")
                    .append(y).append(" , ")
                    .append(z).toString();
        } catch (Exception e) {
            return "Invalid coordinates";
        }
    }

    // ==================== DATE/TIME OPERATIONS ====================

    public static String getSerializedLocalDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    public static LocalDateTime getDeserializedLocalDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }

    public static Duration calculateDifference(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }

    // ==================== DAMAGE CAUSE OPERATIONS ====================

    public static String getSerializedDamageCause(EntityDamageEvent.DamageCause cause) {
        return cause.name();
    }

    public static EntityDamageEvent.DamageCause getDeserializedDamageCause(String cause) {
        try {
            return EntityDamageEvent.DamageCause.valueOf(cause);
        } catch (IllegalArgumentException e) {
            debug("Invalid damage cause: %s, using CUSTOM", cause);
            return EntityDamageEvent.DamageCause.CUSTOM;
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Removes player from dead players list by UUID
     */
    public static void removeUUIDFromDeadPlayers(UUID uuid) {
        SETUP.DeadPlayers.removeIf(playerObj -> uuid.equals(playerObj.getUUID()));
    }

    /**
     * Gets player name safely
     */
    private static String getPlayerName(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : "Unknown Player";
    }

    /**
     * Clears all caches for memory management
     */
    public static void clearCaches() {
        FILE_CACHE.clear();
        SkullCreator.clearCache();
        debug("Caches cleared");
    }

    // ==================== DEBUG METHODS ====================

    public static void debug(String message) {
        if (SETUP.getConfig().getBoolean("debug", true)) {
            SETUP.getLogger().info("[DEBUG] " + message);
        }
    }

    public static void debug(String format, Object... args) {
        if (SETUP.getConfig().getBoolean("debug", true)) {
            SETUP.getLogger().info("[DEBUG] " + String.format(format, args));
        }
    }

    public static void debug(String message, java.util.logging.Level level) {
        if (SETUP.getConfig().getBoolean("debug", true)) {
            SETUP.getLogger().log(level, "[DEBUG] " + message);
        }
    }
}