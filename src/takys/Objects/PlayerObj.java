package takys.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a player object with death information.
 */
public class PlayerObj {

    final UUID uuid;
    final DamageCause damageCause;
    final LocalDateTime deathDate;
    final Location deathLocation;

    /**
     * Constructs a new PlayerObj instance.
     *
     * @param uuid        the player's UUID
     * @param deathDate   the date of the player's death
     * @param damageCause the cause of the player's death
     * @param loc         the location of the player's death
     */
    public PlayerObj(UUID uuid, LocalDateTime deathDate, DamageCause damageCause, Location loc) {
        this.uuid = uuid;
        this.deathDate = deathDate;
        this.damageCause = damageCause;
        this.deathLocation = loc;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player's UUID
     */
    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * Returns the date of the player's death.
     *
     * @return the date of the player's death
     */
    public LocalDateTime getDate() {
        return this.deathDate;
    }

    /**
     * Returns the location of the player's death.
     *
     * @return the location of the player's death
     */
    public Location getLoc() {
        return this.deathLocation;
    }

    /**
     * Returns the player object associated with the UUID.
     *
     * @return the player object
     */
    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(this.getUUID());
    }

    /**
     * Returns the cause of the player's death.
     *
     * @return the cause of the player's death
     */
    public DamageCause getDamageCause() {
        return this.damageCause;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PlayerObj playerObj = (PlayerObj) obj;
        return uuid.equals(playerObj.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}