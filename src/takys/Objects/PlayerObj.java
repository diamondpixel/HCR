package takys.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import java.time.LocalDateTime;
import java.util.UUID;


public class PlayerObj{ ;

    final DamageCause damageCause;
    final UUID uuid;
    final LocalDateTime deathDate;
    final Location deathLocation;

    public PlayerObj(UUID uuid, LocalDateTime deathDate, DamageCause damageCause, Location loc) {
        this.uuid = uuid;
        this.deathDate = deathDate;
        this.damageCause = damageCause;
        this.deathLocation = loc;
    }

    public UUID GetUUID() {
        return this.uuid;
    }

    public LocalDateTime GetDate() {
        return this.deathDate;
    }

    public Location GetLoc() {
        return this.deathLocation;
    }

    public Player GetPlayer() {
        return Bukkit.getServer().getPlayer(this.GetUUID());
    }

    public DamageCause GetDamageCause() {
        return this.damageCause;
    }

}