package takys;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class PlayerObj implements Serializable {
      private static final long serialVersionUID = -6607617821228194361L;
      public String DC;
      private String _uuid;
      private long _date;
      private String loc; 
      
      public PlayerObj(UUID uuid, Date date, DamageCause dc, String loc) {
            this._uuid = uuid.toString();
            this._date = date.getTime();
            this.DC = dc.toString();
            this.loc = loc.toString();
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
}
