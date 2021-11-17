package takys;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import takys.Util.Inventories;
import takys.Util.Utilities;


public class Events implements Listener {
      String filePath;


      public Events() {
            this.filePath = Setup.dataFolder + "/Dead Players.data";
      }

		@EventHandler
		public void onPlayerJoin(PlayerJoinEvent e) {
			e.getPlayer().discoverRecipe(Utilities.key);
		}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		  ItemMeta im = e.getPlayer().getInventory().getItemInMainHand().getItemMeta();
		  ItemMeta tm = Utilities.token.getItemMeta();
		if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
				&& e.hasItem() && im.equals(tm)){

			e.setCancelled(true);
			  Inventories.openDeadGUI(e.getPlayer());
		   }
	  }
	  @EventHandler
      public void onDamage(EntityDamageEvent e) {
		  if (e.getEntity() instanceof Player) {
			  Player p = (Player) e.getEntity();
			  if (p.getHealth() - e.getDamage() <= 0) {
				  Location spawn = p.getBedSpawnLocation();
				  if (spawn == null) {
					  spawn = p.getWorld().getSpawnLocation();
				  }


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
				  Utilities.DeadPlayers.forEach((player) -> {
					  if (((PlayerObj) player).GetUUID().equals(p.getUniqueId())) {
						  duplicates.add(player);
					  }

				  });
				  Utilities.DeadPlayers.removeAll(duplicates);
				  Utilities.DeadPlayers.add(new PlayerObj(p.getUniqueId(), new Date(), dc, loc));
				  FileHandler.SaveFallen();
			  }
		  }
		     }

		  }