package takys.Files;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import takys.Objects.PlayerObj;
import takys.Setup;
import takys.Utilities.Utilities;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Manages JSON file operations for dead player data storage and retrieval.
 * Thread-safe implementation with caching for improved performance.
 */
public class DataManager {

    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    private static final Path DEAD_PLAYERS_PATH = Setup.instance.getDataFolder().toPath().resolve("DeadPlayers");
    private static final JSONParser JSON_PARSER = new JSONParser();

    // Performance optimizations
    private static final Map<UUID, PlayerObj> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> VALIDATED_PATHS = ConcurrentHashMap.newKeySet();

    // JSON field constants for better maintainability
    private static final String DEATH_LOCATION_FIELD = "deathLocation";
    private static final String DEATH_TIME_FIELD = "deathTime";
    private static final String DAMAGE_CAUSE_FIELD = "damageCause";
    private static final String JSON_EXTENSION = ".json";

    static {
        initializeDirectory();
    }

    private static void initializeDirectory() {
        try {
            Files.createDirectories(DEAD_PLAYERS_PATH);
            LOGGER.info("DeadPlayers directory initialized at: " + DEAD_PLAYERS_PATH);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create DeadPlayers directory", e);
            throw new RuntimeException("Cannot initialize DataManager", e);
        }
    }

    /**
     * Creates a JSON file containing player death information.
     * Includes caching for improved read performance.
     *
     * @param uuid Player's UUID
     * @param location Death location
     * @param deathTime Time of death
     * @param damageCause Cause of death
     * @return CompletableFuture<Boolean> for async operation
     */
    public static CompletableFuture<Boolean> createJsonFileAsync(UUID uuid, Location location,
                                                                 LocalDateTime deathTime, DamageCause damageCause) {
        return CompletableFuture.supplyAsync(() -> createJsonFileSync(uuid, location, deathTime, damageCause));
    }

    /**
     * Synchronous version of createJsonFile for backward compatibility.
     */
    public static boolean createJsonFile(UUID uuid, Location location, LocalDateTime deathTime, DamageCause damageCause) {
        return createJsonFileSync(uuid, location, deathTime, damageCause);
    }

    private static boolean createJsonFileSync(UUID uuid, Location location, LocalDateTime deathTime, DamageCause damageCause) {
        if (!validateCreateParameters(uuid, location, deathTime, damageCause)) {
            return false;
        }

        try {
            JSONObject jsonObject = buildJsonObject(location, deathTime, damageCause);
            Path filePath = getPlayerFilePath(uuid);

            Files.writeString(filePath, jsonObject.toJSONString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // Update cache
            PlayerObj playerObj = new PlayerObj(uuid, deathTime, damageCause, location);
            CACHE.put(uuid, playerObj);

            LOGGER.fine("Successfully created JSON file for UUID: " + uuid);
            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create JSON file for UUID: " + uuid, e);
            return false;
        }
    }

    /**
     * Reads a JSON file and returns a PlayerObj with caching support.
     *
     * @param filePath Path to the JSON file
     * @return PlayerObj if successful, null otherwise
     */
    public static PlayerObj readJsonFile(String filePath) {
        if (!isValidFilePath(filePath)) {
            return null;
        }

        Path path = Paths.get(filePath);
        UUID uuid = extractUuidFromFileName(path.getFileName().toString());

        if (uuid == null) {
            return null;
        }

        // Check cache first
        PlayerObj cached = CACHE.get(uuid);
        if (cached != null) {
            return cached;
        }

        return readAndCachePlayerObj(path, uuid);
    }

    /**
     * Reads all JSON files in the DeadPlayers directory with improved performance.
     * Uses parallel processing for large datasets.
     *
     * @return List of PlayerObj instances
     */
    public static List<PlayerObj> readAllJsonFiles() {
        return readJsonFiles(DEAD_PLAYERS_PATH.toString());
    }

    /**
     * Reads all JSON files in the specified directory with parallel processing.
     *
     * @param directoryPath Path to the directory containing JSON files
     * @return List of PlayerObj instances
     */
    public static List<PlayerObj> readJsonFiles(String directoryPath) {
        if (!isValidDirectoryPath(directoryPath)) {
            return new ArrayList<>();
        }

        Path dirPath = Paths.get(directoryPath);

        try (Stream<Path> pathStream = Files.list(dirPath)) {
            return pathStream
                    .filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .parallel() // Use parallel processing for better performance
                    .map(path -> readJsonFile(path.toString()))
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read JSON files from directory: " + directoryPath, e);
            return new ArrayList<>();
        }
    }

    /**
     * Deletes a JSON file and removes from cache.
     *
     * @param uuid Player UUID
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteJsonFile(UUID uuid) {
        if (uuid == null) {
            LOGGER.warning("Cannot delete file with null UUID");
            return false;
        }

        Path filePath = getPlayerFilePath(uuid);
        return deleteJsonFile(filePath.toString());
    }

    /**
     * Deletes a JSON file by file path and removes from cache.
     *
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteJsonFile(String filePath) {
        if (!isValidFilePath(filePath)) {
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            UUID uuid = extractUuidFromFileName(path.getFileName().toString());

            boolean deleted = Files.deleteIfExists(path);

            if (deleted && uuid != null) {
                CACHE.remove(uuid); // Remove from cache
                LOGGER.fine("Successfully deleted JSON file for UUID: " + uuid);
            }

            return deleted;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete JSON file: " + filePath, e);
            return false;
        }
    }

    /**
     * Clears the internal cache. Useful for memory management or testing.
     */
    public static void clearCache() {
        CACHE.clear();
        VALIDATED_PATHS.clear();
        LOGGER.fine("Cache cleared");
    }

    /**
     * Gets cache statistics for monitoring purposes.
     *
     * @return Map containing cache statistics
     */
    public static Map<String, Integer> getCacheStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("cachedPlayers", CACHE.size());
        stats.put("validatedPaths", VALIDATED_PATHS.size());
        return stats;
    }

    // Private helper methods

    private static boolean validateCreateParameters(UUID uuid, Location location,
                                                    LocalDateTime deathTime, DamageCause damageCause) {
        if (uuid == null || location == null || deathTime == null || damageCause == null) {
            LOGGER.warning("Cannot create JSON file with null parameters");
            return false;
        }
        return true;
    }

    private static JSONObject buildJsonObject(Location location, LocalDateTime deathTime, DamageCause damageCause) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(DEATH_LOCATION_FIELD, Utilities.getSerializedLocation(location));
        jsonObject.put(DEATH_TIME_FIELD, Utilities.getSerializedLocalDateTime(deathTime));
        jsonObject.put(DAMAGE_CAUSE_FIELD, Utilities.getSerializedDamageCause(damageCause));
        return jsonObject;
    }

    private static Path getPlayerFilePath(UUID uuid) {
        return DEAD_PLAYERS_PATH.resolve(uuid.toString() + JSON_EXTENSION);
    }

    private static boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LOGGER.warning("Cannot process null or empty file path");
            return false;
        }

        // Use cache for path validation
        if (VALIDATED_PATHS.contains(filePath)) {
            return true;
        }

        Path path = Paths.get(filePath);
        boolean isValid = Files.exists(path) && Files.isReadable(path) && Files.isRegularFile(path);

        if (isValid) {
            VALIDATED_PATHS.add(filePath);
        }

        return isValid;
    }

    private static boolean isValidDirectoryPath(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            LOGGER.warning("Cannot read JSON files from null or empty directory path");
            return false;
        }

        Path dirPath = Paths.get(directoryPath);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            LOGGER.warning("Directory does not exist or is not a directory: " + directoryPath);
            return false;
        }

        return true;
    }

    private static PlayerObj readAndCachePlayerObj(Path path, UUID uuid) {
        try {
            String content = Files.readString(path);
            JSONObject jsonObject = (JSONObject) JSON_PARSER.parse(content);

            PlayerObj playerObj = parsePlayerObjFromJson(uuid, jsonObject);

            if (playerObj != null) {
                CACHE.put(uuid, playerObj); // Cache the result
            }

            return playerObj;

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to read JSON file: " + path, e);
            return null;
        }
    }

    private static UUID extractUuidFromFileName(String fileName) {
        if (fileName == null || !fileName.endsWith(JSON_EXTENSION)) {
            LOGGER.warning("Invalid filename format: " + fileName);
            return null;
        }

        try {
            String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            LOGGER.warning("Invalid UUID format in filename: " + fileName);
            return null;
        }
    }

    private static PlayerObj parsePlayerObjFromJson(UUID uuid, JSONObject jsonObject) {
        if (jsonObject == null) {
            LOGGER.warning("JSON object is null for UUID: " + uuid);
            return null;
        }

        try {
            String deathTimeStr = (String) jsonObject.get(DEATH_TIME_FIELD);
            String damageCauseStr = (String) jsonObject.get(DAMAGE_CAUSE_FIELD);
            String locationStr = (String) jsonObject.get(DEATH_LOCATION_FIELD);

            if (!areRequiredFieldsPresent(deathTimeStr, damageCauseStr, locationStr, uuid)) {
                return null;
            }

            LocalDateTime deathDate = Utilities.getDeserializedLocalDateTime(deathTimeStr);
            DamageCause damageCause = Utilities.getDeserializedDamageCause(damageCauseStr);
            Location location = Utilities.getDeserializedLocation(locationStr);

            if (!areDeserializedValuesValid(deathDate, damageCause, location, uuid)) {
                return null;
            }

            return new PlayerObj(uuid, deathDate, damageCause, location);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse PlayerObj from JSON for UUID: " + uuid, e);
            return null;
        }
    }

    private static boolean areRequiredFieldsPresent(String deathTimeStr, String damageCauseStr,
                                                    String locationStr, UUID uuid) {
        if (deathTimeStr == null || damageCauseStr == null || locationStr == null) {
            LOGGER.warning("Missing required fields in JSON for UUID: " + uuid);
            return false;
        }
        return true;
    }

    private static boolean areDeserializedValuesValid(LocalDateTime deathDate, DamageCause damageCause,
                                                      Location location, UUID uuid) {
        if (deathDate == null || damageCause == null || location == null) {
            LOGGER.warning("Failed to deserialize data for UUID: " + uuid);
            return false;
        }
        return true;
    }
}