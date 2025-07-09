package takys.Files;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import takys.Objects.PlayerObj;
import takys.Setup;
import takys.Utilities;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataManager {

    private static final Logger LOGGER = Logger.getLogger(DataManager.class.getName());
    private static final Path DEAD_PLAYERS_PATH = Setup.instance.getDataFolder().toPath().resolve("DeadPlayers");
    private static final JSONParser JSON_PARSER = new JSONParser();

    // Static initialization to ensure directory exists
    static {
        try {
            Files.createDirectories(DEAD_PLAYERS_PATH);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create DeadPlayers directory", e);
        }
    }

    /**
     * Creates a JSON file containing player death information
     * @param uuid Player's UUID
     * @param location Death location
     * @param deathTime Time of death
     * @param damageCause Cause of death
     * @return true if file was created successfully, false otherwise
     */
    public static boolean createJsonFile(UUID uuid, Location location, LocalDateTime deathTime, DamageCause damageCause) {
        if (uuid == null || location == null || deathTime == null || damageCause == null) {
            LOGGER.warning("Cannot create JSON file with null parameters");
            return false;
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deathLocation", Utilities.getSerializedLocation(location));
        jsonObject.put("deathTime", Utilities.getSerializedLocalDateTime(deathTime));
        jsonObject.put("damageCause", Utilities.getSerializedDamageCause(damageCause));

        Path filePath = DEAD_PLAYERS_PATH.resolve(uuid.toString() + ".json");

        try {
            Files.writeString(filePath, jsonObject.toJSONString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create JSON file for UUID: " + uuid, e);
            return false;
        }
    }

    /**
     * Reads a JSON file and returns a PlayerObj
     * @param filePath Path to the JSON file
     * @return PlayerObj if successful, null otherwise
     */
    public static PlayerObj readJsonFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LOGGER.warning("Cannot read JSON file with null or empty path");
            return null;
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            LOGGER.warning("File does not exist or is not readable: " + filePath);
            return null;
        }

        try {
            UUID uuid = extractUuidFromFileName(path.getFileName().toString());
            if (uuid == null) {
                return null;
            }

            String content = Files.readString(path);
            JSONObject jsonObject = (JSONObject) JSON_PARSER.parse(content);

            return parsePlayerObjFromJson(uuid, jsonObject);
        } catch (IOException | ParseException e) {
            LOGGER.log(Level.WARNING, "Failed to read JSON file: " + filePath, e);
            return null;
        }
    }

    /**
     * Reads all JSON files in the specified directory
     * @param directoryPath Path to the directory containing JSON files
     * @return List of PlayerObj instances
     */
    public static List<PlayerObj> readJsonFiles(String directoryPath) {
        List<PlayerObj> playerObjs = new ArrayList<>();

        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            LOGGER.warning("Cannot read JSON files from null or empty directory path");
            return playerObjs;
        }

        Path dirPath = Path.of(directoryPath);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            LOGGER.warning("Directory does not exist or is not a directory: " + directoryPath);
            return playerObjs;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dirPath, "*.json")) {
            for (Path file : directoryStream) {
                PlayerObj playerObj = readJsonFile(file.toString());
                if (playerObj != null) {
                    playerObjs.add(playerObj);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read JSON files from directory: " + directoryPath, e);
        }

        return playerObjs;
    }

    /**
     * Deletes a JSON file
     * @param filePath Path to the file to delete
     * @return true if deletion was successful, false otherwise
     */
    public static boolean deleteJsonFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            LOGGER.warning("Cannot delete file with null or empty path");
            return false;
        }

        try {
            return Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete JSON file: " + filePath, e);
            return false;
        }
    }

    // Helper methods

    /**
     * Extracts UUID from filename
     * @param fileName Name of the file
     * @return UUID if valid, null otherwise
     */
    private static UUID extractUuidFromFileName(String fileName) {
        try {
            String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            LOGGER.warning("Invalid UUID format in filename: " + fileName);
            return null;
        }
    }

    /**
     * Parses PlayerObj from JSON object
     * @param uuid Player UUID
     * @param jsonObject JSON object containing player data
     * @return PlayerObj if successful, null otherwise
     */
    private static PlayerObj parsePlayerObjFromJson(UUID uuid, JSONObject jsonObject) {
        try {
            String deathTimeStr = (String) jsonObject.get("deathTime");
            String damageCauseStr = (String) jsonObject.get("damageCause");
            String locationStr = (String) jsonObject.get("deathLocation");

            if (deathTimeStr == null || damageCauseStr == null || locationStr == null) {
                LOGGER.warning("Missing required fields in JSON for UUID: " + uuid);
                return null;
            }

            LocalDateTime deathDate = Utilities.getDeserializedLocalDateTime(deathTimeStr);
            DamageCause damageCause = Utilities.getDeserializedDamageCause(damageCauseStr);
            Location location = Utilities.getDeserializedLocation(locationStr);

            if (deathDate == null || damageCause == null || location == null) {
                LOGGER.warning("Failed to deserialize data for UUID: " + uuid);
                return null;
            }

            return new PlayerObj(uuid, deathDate, damageCause, location);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse PlayerObj from JSON for UUID: " + uuid, e);
            return null;
        }
    }
}