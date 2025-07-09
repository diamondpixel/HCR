package takys;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import takys.Files.DataManager;
import takys.Objects.Pair;
import takys.Objects.PlayerObj;
import takys.SkullCreator.SkullCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Improved Utilities class with performance optimizations and better SkullCreator integration
 */
public class Utilities {

    public final static Setup setup = Setup.instance;
    public final static DataManager dataManager = Setup.dataManager;

    // Pre-encoded base64 skull textures - using SkullCreator for better performance
    public final static String DEATH_LOCATION_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjllMmYzM2ViMTgwZjA0MzQ5MTZkYzVkMmJiMzI2YTZlYTIyZmM5YmJmOTg4YmMzMWEyNDFmZDQyNzgwMjMifX19";
    public final static String BACK_ARROW_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=";
    public final static String WORLD_SPAWN_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4OWQ1YjE3ODYyNmVhMjNkMGIwYzNkMmRmNWMwODVlODM3NTA1NmJmNjg1YjVlZDViYjQ3N2ZlODQ3MmQ5NCJ9fX0=";
    public final static String EYE_LOCATION_HEAD = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQyY2Y4Y2U0ODdiNzhmYTIwM2Q1NmNmMDE0OTE0MzRiNGMzM2U1ZDIzNjgwMmM2ZDY5MTQ2YTUxNDM1YjAzZCJ9fX0=";

    // Performance optimizations: pre-create reusable objects
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy-HH.mm.ss");
    private static final Properties PROPERTIES_CACHE = new Properties();
    private static final Map<String, String> FILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack> PLAYER_SKULL_CACHE = new ConcurrentHashMap<>();

    // Pre-created barrier item for void death locations
    private static final ItemStack BARRIER_ITEM = new ItemStack(Material.BARRIER);

    // ThreadLocal StringBuilder for string operations
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = ThreadLocal.withInitial(() -> new StringBuilder(128));

    /**
     * Optimized item removal with early returns and single pass
     */
    public static void removeFirstItem(Player player, ItemStack targetStack) {
        if (player == null || targetStack == null || !targetStack.hasItemMeta()) return;

        ItemMeta targetMeta = targetStack.getItemMeta();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.hasItemMeta() && Objects.equals(item.getItemMeta(), targetMeta)) {
                item.setAmount(item.getAmount() - 1);
                return; // Early return after first match
            }
        }
    }

    /**
     * Optimized file reading with caching
     */
    public static String getString(String key, File file) {
        String cacheKey = file.getAbsolutePath() + ":" + key;

        // Check cache first
        if (FILE_CACHE.containsKey(cacheKey)) {
            return FILE_CACHE.get(cacheKey);
        }

        try (FileInputStream in = new FileInputStream(file)) {
            PROPERTIES_CACHE.clear();
            PROPERTIES_CACHE.load(in);
            String value = PROPERTIES_CACHE.getProperty(key, "");
            FILE_CACHE.put(cacheKey, value);
            return value;
        } catch (IOException e) {
            FILE_CACHE.put(cacheKey, "");
            return "";
        }
    }

    /**
     * Optimized player skull creation with time-sensitive caching
     * This method creates fresh skulls each time to ensure dynamic content (time elapsed) is updated
     */
    public static ItemStack getPlayerSkull(PlayerObj pObj) {
        UUID uuid = pObj.getUUID();
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return null;

        // Use SkullCreator for optimized skull creation (it has its own caching for textures)
        ItemStack skull = SkullCreator.itemFromUuid(uuid);
        ItemMeta meta = skull.getItemMeta();

        meta.setDisplayName(player.getName());

        // Calculate current time difference (this needs to be fresh each time)
        Duration duration = Duration.between(pObj.getDate(), LocalDateTime.now());
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        long seconds = duration.minusDays(days).minusHours(hours).minusMinutes(minutes).getSeconds();

        // Use ArrayList with initial capacity for better performance
        List<String> lore = new ArrayList<>(5);
        lore.add(String.format("Died %dd, %dh, %dm, %ds ago", days, hours, minutes, seconds));
        lore.add("Cause of Death: " + getSerializedDamageCause(pObj.getDamageCause()));
        lore.add("Death Count: " + player.getStatistic(Statistic.DEATHS));
        lore.add("Coordinates of latest death:");
        lore.add(serializedToFormattedString(getSerializedLocation(pObj.getLoc())));

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return skull;
    }

    /**
     * Creates a cached base player skull (without dynamic content) for static use cases
     * This is useful for skulls that don't need to update frequently
     */
    public static ItemStack getCachedPlayerSkull(UUID uuid) {
        // Check cache first for static skulls
        if (PLAYER_SKULL_CACHE.containsKey(uuid)) {
            return PLAYER_SKULL_CACHE.get(uuid).clone();
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return null;

        // Create basic skull without dynamic content
        ItemStack skull = SkullCreator.itemFromUuid(uuid);
        ItemMeta meta = skull.getItemMeta();
        meta.setDisplayName(player.getName());
        skull.setItemMeta(meta);

        // Cache the basic skull
        PLAYER_SKULL_CACHE.put(uuid, skull.clone());

        return skull;
    }

    /**
     * Optimized recipe construction with reusable components
     * Recipe safe - defaults to a fallback recipe if config is invalid
     */
    public static Pair<ShapedRecipe, ItemStack> constructRecipe() {
        ItemStack token = new ItemStack(Material.END_CRYSTAL, 1);
        token.addUnsafeEnchantment(Enchantment.LOYALTY, 10);

        ItemMeta meta = token.getItemMeta();
        meta.setDisplayName("Revival Token");
        meta.setLore(Collections.singletonList("Choose a dead player to revive."));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        token.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(setup, "Token");
        ShapedRecipe recipe = new ShapedRecipe(key, token);

        List<String> materials = getValidRecipeFromConfig();
        recipe.shape("123", "456", "789");

        // Optimized ingredient setting
        char[] chars = {'1', '2', '3', '4', '5', '6', '7', '8', '9'};
        for (int i = 0; i < Math.min(chars.length, materials.size()); i++) {
            String materialName = materials.get(i);
            if (materialName != null && !materialName.isEmpty() && !materialName.equals("AIR")) {
                try {
                    recipe.setIngredient(chars[i], Material.valueOf(materialName));
                } catch (IllegalArgumentException e) {
                    debug("Invalid material in recipe position %d: %s, skipping", i, materialName);
                    // Skip invalid materials - they won't be required in the recipe
                }
            }
            // Skip AIR or empty slots - they remain as empty spaces in the recipe
        }

        return new Pair<>(recipe, token);
    }

    /**
     * Gets a valid recipe from config, falling back to memory or default
     * Automatically saves default recipe to config when invalid values are detected
     */
    private static List<String> getValidRecipeFromConfig() {
        List<String> configRecipe = setup.getConfig().getStringList("recipe");

        // Default recipe - a simple cross pattern with common materials
        List<String> defaultRecipe = Arrays.asList(
                "AIR", "DIAMOND", "AIR",
                "DIAMOND", "NETHER_STAR", "DIAMOND",
                "AIR", "DIAMOND", "AIR"
        );

        debug("Retrieved recipe from config with %d items", configRecipe.size());

        // Check if config recipe exists and has correct size
        if (configRecipe.size() != 9) {
            debug("Invalid recipe size in config: %d", configRecipe.size());
            return handleInvalidRecipe(defaultRecipe, "Invalid recipe size");
        }

        // Validate each material in the recipe
        List<String> validatedRecipe = new ArrayList<>();
        boolean hasInvalidMaterials = false;
        int nonAirCount = 0; // Track non-AIR materials

        for (int i = 0; i < 9; i++) {
            String materialName = configRecipe.get(i);

            if (materialName == null) {
                debug("Null material at position %d", i);
                hasInvalidMaterials = true;
                break;
            } else if (materialName.isEmpty()) {
                debug("Empty material at position %d, treating as AIR", i);
                validatedRecipe.add("AIR");
            } else if (materialName.equals("AIR")) {
                validatedRecipe.add("AIR");
                debug("Valid material at position %d: AIR (Explicit)", i);
            } else {
                try {
                    Material.valueOf(materialName.toUpperCase());
                    validatedRecipe.add(materialName.toUpperCase());
                    nonAirCount++; // Count non-AIR materials
                    debug("Valid material at position %d: %s", i, materialName);
                } catch (IllegalArgumentException e) {
                    debug("Invalid material at position %d: %s", i, materialName);
                    hasInvalidMaterials = true;
                    break;
                }
            }
        }

        if (hasInvalidMaterials) {
            return handleInvalidRecipe(defaultRecipe, "Invalid materials found");
        }

        // Check for all AIR slots edge case
        if (nonAirCount == 0) {
            debug("Recipe contains only AIR slots, this would create an invalid recipe");
            return handleInvalidRecipe(defaultRecipe, "Recipe contains only AIR slots");
        }

        return validatedRecipe;
    }

    /**
     * Handles invalid recipe cases by writing default values to config
     */
    private static List<String> handleInvalidRecipe(List<String> defaultRecipe, String reason) {
        debug("Recipe validation failed: %s", reason);
        debug("Writing default recipe to config");

        // Write default recipe to config
        setup.getConfig().set("recipe", defaultRecipe);
        setup.saveConfig();

        return defaultRecipe;
    }

    /**
     * Handles the recipe reload process including server registry and player discovery
     */
    public static void reloadRecipe(Collection<? extends Player> players) {
        unloadRecipe(players);
        loadRecipe(players);
    }

    /**
     * Helper method to load recipe
     */
    public static void loadRecipe(Collection<? extends Player> players) {
        // Construct new recipe
        int playerCount = players.size();
        Pair<ShapedRecipe, ItemStack> recipeData = Utilities.constructRecipe();
        Setup.Item = recipeData.getValue();
        Setup.Recipe = recipeData.getKey();

        NamespacedKey newRecipeKey = Setup.Recipe.getKey();
        debug("New recipe constructed with key: %s", newRecipeKey);

        // Add new recipe to server
        debug("Adding new recipe to server");
        Bukkit.addRecipe(Setup.Recipe);

        // Discover new recipe for all players
        debug("Discovering new recipe for %d players", playerCount);
        players.forEach(player -> player.discoverRecipe(newRecipeKey));

        debug("Recipe reload completed successfully");
    }

    /**
     * Helper method to unload recipe
     */
    public static void unloadRecipe(Collection<? extends Player> players) {
        NamespacedKey oldRecipeKey = Setup.Recipe != null ? Setup.Recipe.getKey() : null;
        int playerCount = players.size();

        // Remove old recipe from server and undiscover from players
        if (oldRecipeKey != null) {
            debug("Removing old recipe from server: %s", oldRecipeKey);
            Bukkit.removeRecipe(oldRecipeKey);

            debug("Undiscovering old recipe for %d players", playerCount);
            players.forEach(player -> player.undiscoverRecipe(oldRecipeKey));
        }
    }


    /**
     * Optimized back arrow item creation using SkullCreator
     */
    public static ItemStack backArrowItem() {
        ItemStack backArrow = SkullCreator.itemFromBase64(BACK_ARROW_HEAD);
        ItemMeta meta = backArrow.getItemMeta();
        meta.setDisplayName("§3§lGo Back!");
        backArrow.setItemMeta(meta);
        return backArrow;
    }

    /**
     * Optimized death location item with better performance
     */
    public static ItemStack deathLocationItem(PlayerObj pObj) {
        boolean isVoid = isBelowAir(pObj);
        ItemStack item = isVoid ? BARRIER_ITEM.clone() : SkullCreator.itemFromBase64(DEATH_LOCATION_HEAD);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§0§lDeath Location");

        String playerName = Bukkit.getPlayer(pObj.getUUID()).getName();
        String coordinates = serializedToFormattedString(getSerializedLocation(pObj.getLoc()));

        List<String> lore = new ArrayList<>(2);
        if (isVoid) {
            lore.add("§3Cannot respawn at coords because floor is air. Probably void.");
            lore.add("§3Death coordinates (" + coordinates + ")");
        } else {
            lore.add("§3Click to respawn " + playerName);
            lore.add("§3where he died! (" + coordinates + ")");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Optimized world spawn item creation
     */
    public static ItemStack worldSpawnItem(UUID uuid) {
        ItemStack item = SkullCreator.itemFromBase64(WORLD_SPAWN_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§3§lWorld Spawn");

        String playerName = Bukkit.getPlayer(uuid).getName();
        List<String> lore = Arrays.asList(
                "§3Click to respawn " + playerName,
                "§3at world's spawnpoint!"
        );

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Optimized eye location item creation
     */
    public static ItemStack eyeLocationItem(UUID uuid) {
        ItemStack item = SkullCreator.itemFromBase64(EYE_LOCATION_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§3§lSimple Revive");

        String playerName = Bukkit.getPlayer(uuid).getName();
        List<String> lore = Arrays.asList(
                "§3Click to respawn " + playerName,
                "§3where you are looking!"
        );

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Optimized bed item creation with better logic
     */
    public static ItemStack bedItem(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return new ItemStack(Material.RED_BED);

        Location bedLocation = player.getBedSpawnLocation();
        String playerName = player.getName();

        if (bedLocation == null) {
            ItemStack bed = new ItemStack(Material.RED_BED);
            ItemMeta meta = bed.getItemMeta();
            meta.setDisplayName("§4§lBed");
            meta.setLore(Collections.singletonList("§3" + playerName + " doesn't have a bed :("));
            bed.setItemMeta(meta);
            return bed;
        }

        boolean isNether = bedLocation.getWorld().getEnvironment() == World.Environment.NETHER;
        ItemStack bed = new ItemStack(isNether ? Material.RESPAWN_ANCHOR : Material.GREEN_BED);
        ItemMeta meta = bed.getItemMeta();

        if (isNether) {
            meta.setDisplayName("§5§lRespawn Anchor");
            meta.setLore(Arrays.asList(
                    "§3Click to respawn " + playerName,
                    "§3at his respawn anchor!"
            ));
        } else {
            meta.setDisplayName("§a§lBed");
            meta.setLore(Arrays.asList(
                    "§3Click to respawn " + playerName,
                    "§3at his bed!"
            ));
        }

        bed.setItemMeta(meta);
        return bed;
    }

    /**
     * Optimized player revival with better effect management
     */
    public static void revivePlayer(Player viewer, PlayerObj pObj, Location loc) {
        Player player = Bukkit.getPlayer(pObj.getUUID());
        if (player == null || !player.isOnline()) return;

        // Perform all operations in sequence for better performance
        viewer.closeInventory();
        removeFirstItem(viewer, setup.Item);
        setup.DeadPlayers.remove(pObj);
        dataManager.deleteJsonFile(setup.getDataFolder() + File.separator + "DeadPlayers" + File.separator + pObj.getUUID() + ".json");

        // Teleport and set game mode
        player.teleport(loc);
        player.setGameMode(GameMode.SURVIVAL);

        // Visual and audio effects
        loc.getWorld().strikeLightningEffect(loc);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);

        // Apply effects efficiently
        int duration = 1200; // 60 seconds
        player.addPotionEffect(PotionEffectType.RESISTANCE.createEffect(duration, 0));
        player.addPotionEffect(PotionEffectType.ABSORPTION.createEffect(duration, 0));
        player.addPotionEffect(PotionEffectType.REGENERATION.createEffect(duration, 4));
        player.addPotionEffect(PotionEffectType.LUCK.createEffect(duration, 0));
        player.addPotionEffect(PotionEffectType.GLOWING.createEffect(60, 0));
    }

    /**
     * Optimized location serialization using StringBuilder
     */
    public static String getSerializedLocation(Location loc) {
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0);
        return sb.append(loc.getX()).append(";")
                .append(loc.getY()).append(";")
                .append(loc.getZ()).append(";")
                .append(loc.getWorld().getUID()).toString();
    }

    /**
     * Optimized location deserialization with better error handling
     */
    public static Location getDeserializedLocation(String serialized) {
        try {
            String[] parts = serialized.split(";");
            if (parts.length != 4) throw new IllegalArgumentException("Invalid location format");

            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            UUID worldUUID = UUID.fromString(parts[3]);
            World world = Bukkit.getServer().getWorld(worldUUID);

            if (world == null) throw new IllegalArgumentException("World not found: " + worldUUID);

            return new Location(world, x, y, z);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to deserialize location: " + serialized, e);
        }
    }

    /**
     * Optimized coordinate formatting using StringBuilder
     */
    public static String serializedToFormattedString(String serialized) {
        String[] parts = serialized.split(";");
        if (parts.length != 4) return "Invalid coordinates";

        int x = (int) Double.parseDouble(parts[0]);
        int y = (int) Double.parseDouble(parts[1]);
        int z = (int) Double.parseDouble(parts[2]);
        String worldName = Bukkit.getServer().getWorld(UUID.fromString(parts[3])).getName();

        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0);
        return sb.append(worldName).append(" , ")
                .append(x).append(" , ")
                .append(y).append(" , ")
                .append(z).toString();
    }

    /**
     * Optimized date serialization using pre-created formatter
     */
    public static String getSerializedLocalDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Optimized date deserialization using pre-created formatter
     */
    public static LocalDateTime getDeserializedLocalDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }

    /**
     * Simple damage cause serialization
     */
    public static String getSerializedDamageCause(EntityDamageEvent.DamageCause cause) {
        return cause.name();
    }

    /**
     * Simple damage cause deserialization with error handling
     */
    public static EntityDamageEvent.DamageCause getDeserializedDamageCause(String cause) {
        try {
            return EntityDamageEvent.DamageCause.valueOf(cause);
        } catch (IllegalArgumentException e) {
            return EntityDamageEvent.DamageCause.CUSTOM; // Default fallback
        }
    }

    /**
     * Direct duration calculation
     */
    public static Duration calculateDifference(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }

    /**
     * Optimized UUID removal with removeIf
     */
    public static void removeUUIDFromDeadPlayers(UUID uuid) {
        Setup.DeadPlayers.removeIf(playerObj -> uuid.equals(playerObj.getUUID()));
    }

    /**
     * Optimized air check with direct block access
     */
    public static boolean isBelowAir(PlayerObj playerObj) {
        Location deathLoc = playerObj.getLoc();
        World world = deathLoc.getWorld();

        // Check block directly below death location
        int blockY = deathLoc.getBlockY() - 1;
        return world.getBlockAt(deathLoc.getBlockX(), blockY, deathLoc.getBlockZ()).getType() == Material.AIR;
    }

    /**
     * Clear all caches for memory management
     */
    public static void clearCaches() {
        FILE_CACHE.clear();
        PLAYER_SKULL_CACHE.clear();
        SkullCreator.clearCache();
    }

    /**
     * Clear only the player skull cache (useful when you need fresh skulls but want to keep other caches)
     */
    public static void clearPlayerSkullCache() {
        PLAYER_SKULL_CACHE.clear();
    }

    /**
     * Remove a specific player from the skull cache
     */
    public static void removePlayerFromCache(UUID uuid) {
        PLAYER_SKULL_CACHE.remove(uuid);
    }

    /**
     * Get cache statistics for monitoring
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("fileCache", FILE_CACHE.size());
        stats.put("playerSkullCache", PLAYER_SKULL_CACHE.size());
        stats.put("skullCreatorCache", SkullCreator.getCacheSize());
        return stats;
    }

    public static void debug(String message) {
        if (Setup.instance.getConfig().getBoolean("debug", true)) {
            Setup.instance.getLogger().info("[DEBUG] " + message);
        }
    }

    public static void debug(String format, Object... args) {
        if (Setup.instance.getConfig().getBoolean("debug", true)) {
            Setup.instance.getLogger().info("[DEBUG] " + String.format(format, args));
        }
    }

    public static void debug(String message, java.util.logging.Level level) {
        if (Setup.instance.getConfig().getBoolean("debug", true)) {
            Setup.instance.getLogger().log(level, "[DEBUG] " + message);
        }
    }
}