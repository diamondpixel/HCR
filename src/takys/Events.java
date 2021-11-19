package takys;

import java.util.*;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import takys.Util.Inventories;
import takys.Util.Utilities;


public class Events implements Listener {

	String filePath;
	private final Setup setup = Setup.instance;
	private Utilities util = setup.getUtil();
	private Inventories inv = setup.getInventories();

      public Events() {
            this.filePath = Setup.dataFolder + "/Dead Players.data";
      }

		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			e.getPlayer().discoverRecipe(Utilities.key);
		}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
    if(e.hasItem() && e.getItem().hasItemMeta()){
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))){
				if(e.getPlayer().getInventory().getItemInMainHand().getItemMeta().equals(Utilities.token.getItemMeta())){
				e.setCancelled(true);
				inv.openDeadGUI(e.getPlayer());
			  }return;
		     }
		   }
	      }


	@EventHandler
      public void onDeath(PlayerDeathEvent e) {
		  Player p = e.getPlayer();
				  int x = (int) p.getLocation().getX();
				  int y = (int) p.getLocation().getY();
				  int z = (int) p.getLocation().getZ();

				  String world = p.getWorld().getName();

				  String loc = (world.substring(0, 1).toUpperCase() + world.substring(1))
						  + " : "
						  + String.valueOf(x)
						  + " : "
						  + String.valueOf(y)
						  + " : "
						  + String.valueOf(z);


				  EntityDamageEvent ede = e.getEntity().getLastDamageCause();
				  DamageCause dc = ede.getCause();
				  List duplicates = new ArrayList();
				  util.DeadPlayers.forEach((player) -> {
					  if (((PlayerObj) player).GetUUID().equals(p.getUniqueId())) {
						  duplicates.add(player);
					  }

				  });
					  util.DeadPlayers.removeAll(duplicates);
					  util.DeadPlayers.add(new PlayerObj(p.getUniqueId(), new Date(), dc, loc));
					  FileHandler.SaveFallen();

			  }
		  }


