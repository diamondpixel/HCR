package takys;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import takys.Files.DataManager;
import takys.Objects.PlayerObj;
import takys.Objects.Serializers;

import java.util.Date;
import java.util.Objects;

public class Listeners implements Listener {

    @EventHandler
    public void OnDeath(PlayerDeathEvent event) {

        Date date = new Date();
        Player player = event.getPlayer();
        String loc = Serializers.GetSerializedLocation(player.getLocation());
        EntityDamageEvent.DamageCause dc = player.getLastDamageCause().getCause();
        PlayerObj pObj = new PlayerObj(player.getUniqueId(), date, dc, loc);

        for (PlayerObj obj : Setup.DeadPlayers) {
            if (Objects.equals(obj.GetUUID(), pObj.GetUUID()))
                return;
        }
        DataManager.WriteObjectToFile(pObj);
        Setup.DeadPlayers.add(pObj);
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipe(Setup.Recipe.getKey());
    }

    @EventHandler
    public void OnPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null)
            return;

        if (event.getAction().isLeftClick())
            return;

        if (event.getHand() == EquipmentSlot.OFF_HAND)
            return;

        if (event.getItem().getType() != Material.END_CRYSTAL)
            return;

        if (!Objects.equals(event.getItem().getItemMeta(), Setup.Item.getItemMeta()))
            return;

        event.setCancelled(true);
        event.getPlayer().openInventory(GraphicalUserInterface.DeadPlayersGui().getInventory());
    }
}