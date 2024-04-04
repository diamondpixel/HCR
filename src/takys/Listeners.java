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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import takys.Files.DataManager;
import takys.Objects.PlayerObj;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Listeners implements Listener {

    public final static Setup setup = Setup.instance;
    public final static DataManager dataManager = Setup.dataManager;
    public final static GraphicalUserInterface gui = Setup.gui;

    @EventHandler
    @SuppressWarnings("all")
    public void OnDeath(PlayerDeathEvent event) {

        boolean found = false;
        Player player = event.getEntity();
        Location loc = player.getLocation();
        LocalDateTime localDateTime = LocalDateTime.now();
        EntityDamageEvent.DamageCause dc = player.getLastDamageCause().getCause();
        PlayerObj pObj = new PlayerObj(player.getUniqueId(), localDateTime, dc, loc);

        for (PlayerObj playerObj : Setup.DeadPlayers) {
            if (playerObj == null)
                break;
            if (Objects.equals(playerObj.GetUUID(), pObj.GetUUID()))
                found = true;
                break;
        }

        if (found)
            Utilities.removeUUIDfromDeadPlayers(pObj.GetUUID());
            dataManager.deleteJsonFile(setup.getDataFolder() + "\\DeadPlayers\\" + player.getUniqueId().toString() + ".json");

        Setup.DeadPlayers.add(pObj);
        dataManager.createJsonFile(player.getUniqueId(), loc, localDateTime, dc);
    }

    @EventHandler
    @SuppressWarnings("all")
    public void OnPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(Setup.Recipe.getKey());
        UUID uuid = event.getPlayer().getUniqueId();

        PlayerObj playerObj = dataManager.readJsonFile(setup.getDataFolder() + "\\DeadPlayers\\" + uuid.toString() + ".json");
        if (!Setup.DeadPlayers.contains(playerObj)) {
            Setup.DeadPlayers.add(playerObj);
        }
    }


    @EventHandler
    @SuppressWarnings("all")
    public void OnPlayerLeaver(PlayerQuitEvent event) {
        try {
            UUID uuid = event.getPlayer().getUniqueId();
            Utilities.removeUUIDfromDeadPlayers(uuid);
        } catch (Exception e) {}
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