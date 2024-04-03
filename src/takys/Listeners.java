package takys;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import takys.Files.DataManager;
import takys.Objects.PlayerObj;

import java.time.LocalDateTime;
import java.util.Objects;

public class Listeners implements Listener {

    public final static Setup setup = Setup.instance;
    public final static DataManager dataManager = Setup.dataManager;
    public final static GraphicalUserInterface gui = Setup.gui;
    @EventHandler
    @SuppressWarnings("all")
    public void OnDeath(PlayerDeathEvent event) {


        Player player = event.getEntity();
        Location loc = player.getLocation();
        LocalDateTime localDateTime = LocalDateTime.now();
        EntityDamageEvent.DamageCause dc = player.getLastDamageCause().getCause();
        PlayerObj pObj = new PlayerObj(player.getUniqueId(), localDateTime, dc, loc);

        dataManager.createJsonFile(player.getUniqueId(), loc, localDateTime, dc);
        setup.DeadPlayers.add(pObj);

    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(Setup.Recipe.getKey());
    }

    @EventHandler
    public void OnPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null)
            return;

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
            return;

        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if (event.getItem().getType() != Material.END_CRYSTAL)
            return;

        if (!Objects.equals(event.getItem().getItemMeta(), Setup.Item.getItemMeta()))
            return;

        event.setCancelled(true);
        event.getPlayer().openInventory(gui.deadPlayersGui(event.getPlayer()).getInventory());
    }
}