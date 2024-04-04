package takys.Files;

import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import takys.Objects.PlayerObj;
import takys.Setup;
import takys.Utilities;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final static Path path = new File(Setup.instance.getDataFolder() + "/DeadPlayers/").toPath();

    @SuppressWarnings("all")
    public static void createJsonFile(UUID uuid, Location location, LocalDateTime deathTime, DamageCause damageCause) {


        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deathLocation", Utilities.GetSerializedLocation(location));
        jsonObject.put("deathTime", Utilities.GetSerializedLocalDateTime(deathTime));
        jsonObject.put("damageCause", Utilities.GetSerializedDamageCause(damageCause));


        String fileName = uuid.toString() + ".json";
        Path _path = Path.of(String.valueOf(path), fileName);

        try (FileWriter fileWriter = new FileWriter(Path.of(String.valueOf(path), fileName).toString())) {
            fileWriter.write(jsonObject.toString());
        } catch (IOException e) {
        }
    }

    @SuppressWarnings("all")
    public static PlayerObj readJsonFile(String filePath) {
        try {
            String fileName = Path.of(filePath).getFileName().toString();
            String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
            UUID uuid = UUID.fromString(uuidString);
            FileReader fileReader = new FileReader(filePath);
            Object jsonContent = new JSONParser().parse(fileReader);
            fileReader.close();
            JSONObject jsonObject = (JSONObject) jsonContent;
            LocalDateTime deathDate = Utilities.GetDeserializedLocalDateTime((String) jsonObject.get("deathTime"));
            DamageCause damageCause = Utilities.GetDeserializedDamageCause((String) jsonObject.get("damageCause"));
            Location location = Utilities.GetDeserializedLocation((String) jsonObject.get("deathLocation"));

            return new PlayerObj(uuid, deathDate, damageCause, location);
        } catch (IOException | ParseException e) {
            return null;
        }
    }

    @SuppressWarnings("all")
    public static List<PlayerObj> readJsonFiles(String directoryPath) {
        List<PlayerObj> playerObjs = new ArrayList<>();
        try {
            try (var directoryStream = Files.newDirectoryStream(Path.of(directoryPath))) {
                for (Path file : directoryStream) {
                    PlayerObj playerObj = readJsonFile(file.toString());
                    if (playerObj != null) {
                        playerObjs.add(playerObj);
                    }
                }
            }
        } catch (IOException e) {
        }
        return playerObjs;
    }

    @SuppressWarnings("all")
    public static void deleteJsonFile(String filePath) {
        File file = new File(filePath);
        file.delete();
    }

    public static void deleteJsonFiles(List<String> filePaths) {
        for (String filePath : filePaths) {
            deleteJsonFile(filePath);
        }
    }

    @SuppressWarnings("all")
    public static void editJsonFile(String filePath, String key, String value) {
        try {
            Object jsonContent = new JSONParser().parse(new FileReader(filePath));
            JSONObject jsonObject = (JSONObject) jsonContent;
            jsonObject.put(key, value);
            Files.writeString(Path.of(filePath), jsonObject.toString());
        } catch (IOException | ParseException e) {
        }
    }
}