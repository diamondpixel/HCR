package takys.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


public class PlayerObj implements Serializable {

    private static final long serialVersionUID = 1L;

    public String DC;
    private String _uuid;
    private long _date;
    private String loc;

    public PlayerObj(UUID uuid, Date date, DamageCause Dc, String loc) {
        this._uuid = uuid.toString();
        this._date = date.getTime();
        this.DC = Dc.toString();
        this.loc = loc;
    }

    public UUID GetUUID() {
        return UUID.fromString(this._uuid);
    }

    public Date GetDate() {
        Date tempDate = new Date(this._date);
        return tempDate;
    }

    public String GetLoc() {
        return loc;
    }

    public Player GetPlayer() {
        return Bukkit.getServer().getPlayer(this.GetUUID());
    }

}