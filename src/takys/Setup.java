package takys;

import com.samjakob.spigui.SpiGUI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import takys.Files.DataManager;
import takys.Objects.PlayerObj;

import java.util.ArrayList;

public class Setup extends JavaPlugin {

    public static ShapedRecipe Recipe;
    public static ItemStack Item;
    public static ArrayList<PlayerObj> DeadPlayers;
    public static Setup instance;
    public static SpiGUI spiGUI;

    public void onEnable() {
        instance = this;
        spiGUI = new SpiGUI(this);
        Item = Utilities.ConstructRecipe().getValue();
        Recipe = Utilities.ConstructRecipe().getKey();
        DeadPlayers = DataManager.LoadObjectsFromFile();
        Bukkit.addRecipe(Recipe);
        Bukkit.getPluginManager().registerEvents(new Listeners(),this);
        Bukkit.getOnlinePlayers().forEach((player -> { player.discoverRecipe(Recipe.getKey());}));
    }
}