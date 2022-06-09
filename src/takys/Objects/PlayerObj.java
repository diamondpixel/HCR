package takys.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
import org.bukkit.inventory.Inventory;
import java.io.IOException;
 */

public class PlayerObj implements Serializable {

    private static final long serialVersionUID = 1L;

    public String DC;
    private String _uuid;
    private long _date;
    private String loc;
    /**private String inv;*/

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
  /**
    public PlayerObj(UUID uuid, Date date, DamageCause Dc, String loc, Inventory inventory) {
        this.inv = Serializers.toBase64(inventory);
        this._uuid = uuid.toString();
        this._date = date.getTime();
        this.DC = Dc.toString();
        this.loc = loc;

    }     //FUTURE USE!!!

    public Inventory GetInventory() throws IOException {
        return Serializers.fromBase64(this.inv);
    }*/
}