package takys.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

public class Serializers {

    public static String GetSerializedLocation(Location loc) {
        return loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getWorld().getUID();
    }

    public static Location GetDeserializedLocation(String s) {
        String[] parts = s.split(";");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        UUID u = UUID.fromString(parts[3]);
        World w = Bukkit.getServer().getWorld(u);
        return new Location(w, x, y, z);
    }

    public static String SerializedToFormattedString(String string) {
        String[] parts = string.split(";");
        String x = String.valueOf((int) Double.parseDouble(parts[0]));
        String y = String.valueOf((int) Double.parseDouble(parts[1]));
        String z = String.valueOf((int) Double.parseDouble(parts[2]));
        String w = Bukkit.getServer().getWorld(UUID.fromString(parts[3])).getName();
        String s = "%w , %x , %y , %z".replace("%w", w).replace("%x", x).replace("%y", y).replace("%z", z);
        return s;
    }
}