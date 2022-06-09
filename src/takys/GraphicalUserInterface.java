package takys;

import com.samjakob.spigui.SGMenu;
import com.samjakob.spigui.buttons.SGButton;
import com.samjakob.spigui.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.util.Vector;
import takys.Objects.PlayerObj;
import takys.Objects.Serializers;

import java.io.File;
import java.util.Objects;

public class GraphicalUserInterface {


    public static SGMenu DeadPlayersGui() {
        SGMenu gui = Setup.spiGUI.create("&6Dead players", 5);
        for (PlayerObj pObj : Setup.DeadPlayers) {
          if (pObj.GetPlayer().isOnline()) {
            SGButton head = new SGButton(Utilities.GetPlayerSkull(pObj)).withListener((InventoryClickEvent event) -> {
              Setup.DeadPlayers.forEach((Player) ->{
               String str = event.getCurrentItem().getItemMeta().getDisplayName();
                 if (Objects.equals(Player.GetUUID(),Bukkit.getPlayer(str).getUniqueId())){
                     event.getWhoClicked().openInventory(SpecificDeadPlayersGui((Player) event.getWhoClicked(),Player).getInventory());
                 }
               });
             });
            gui.addButton(head);
          }
        }
        return gui;
    }

    public static SGMenu SpecificDeadPlayersGui(Player Viewer, PlayerObj pObj) {
        Player player = Bukkit.getPlayer(pObj.GetUUID());
        SGMenu gui = Setup.spiGUI.create("&c" + player.getName(), 1);

        SGButton BackArrow = new SGButton(Utilities.BackArrowItem()).withListener((InventoryClickEvent event) -> {
            Viewer.openInventory(DeadPlayersGui().getInventory());
        });

        SGButton Bed = new SGButton(Utilities.BedItem(pObj.GetUUID())).withListener((InventoryClickEvent event) -> {
           if (player.getBedSpawnLocation() != null) Utilities.RevivePlayer(Viewer,pObj,player.getBedSpawnLocation());
        });

        SGButton WorldSpawn = new SGButton(Utilities.WorldSpawnItem(pObj.GetUUID())).withListener((InventoryClickEvent event) -> {
           File file = new File("server.properties"); Utilities.RevivePlayer(Viewer,pObj,Bukkit.getWorld(Utilities.GetString("level-name",file)).getSpawnLocation());
        });

        SGButton LookingLocation = new SGButton(Utilities.EyeLocationItem(pObj.GetUUID())).withListener((InventoryClickEvent event) -> {
            Location Loc = event.getWhoClicked().getLocation().add(event.getWhoClicked().getLocation().getDirection().multiply(2));
            Loc.setY(Loc.getY() + 1.0D);
            Vector dir = Loc.clone().subtract(event.getWhoClicked().getEyeLocation()).toVector();
            Loc.setDirection(dir);
            Utilities.RevivePlayer(Viewer,pObj,Loc);
        });

        SGButton DeathLocation = new SGButton(Utilities.DeathLocationItem(pObj)).withListener((InventoryClickEvent event) -> {
            Utilities.RevivePlayer(Viewer,pObj, Serializers.GetDeserializedLocation(pObj.GetLoc()));
        });

        for (int i = 0; i < 9; i++){
            gui.setButton(i,new SGButton(new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).build()));
        }

        gui.setButton(0,BackArrow);
        gui.setButton(3,Bed);
        gui.setButton(4,WorldSpawn);
        gui.setButton(5,LookingLocation);
        gui.setButton(6,DeathLocation);
        gui.setAutomaticPaginationEnabled(false);

        return gui;
    }
}