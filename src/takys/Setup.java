package takys;

import com.samjakob.spigui.SpiGUI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import takys.Files.DataManager;
import takys.Objects.PlayerObj;

import java.io.File;
import java.util.List;

public class Setup extends JavaPlugin {

    public static ShapedRecipe Recipe;
    public static ItemStack Item;
    public static List<PlayerObj> DeadPlayers;
    public static Setup instance;
    public static SpiGUI spiGUI;
    public static DataManager dataManager;
    public static GraphicalUserInterface gui;

    @SuppressWarnings("all")
    public void onEnable() {

        instance = this;
        dataManager = new DataManager();
        gui = new GraphicalUserInterface();

        if (!(new File(this.getDataFolder() + "/DeadPlayers/").exists()))
            new File(this.getDataFolder() + "/DeadPlayers/").mkdirs();
        DeadPlayers = dataManager.readJsonFiles(this.getDataFolder() + "/DeadPlayers/");

        spiGUI = new SpiGUI(this);
        Item = Utilities.ConstructRecipe().getValue();
        Recipe = Utilities.ConstructRecipe().getKey();

        this.saveDefaultConfig();

        Bukkit.addRecipe(Recipe);
        Bukkit.getPluginManager().registerEvents(new Listeners(),this);
        Bukkit.getOnlinePlayers().forEach((player -> { player.discoverRecipe(Recipe.getKey());}));
    }
    public void onDisable(){
        this.saveDefaultConfig();
    }
}