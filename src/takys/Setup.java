package takys;

import com.samjakob.spigui.SpiGUI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import takys.Commands.HCRCommand;
import takys.Files.DataManager;
import takys.GUIs.DeadPlayersGUI;
import takys.GUIs.RecipeConfiguratorGUI;
import takys.Objects.PlayerObj;
import com.skullcreator.SkullCreator;
import takys.Utilities.SafeTeleportManager;
import takys.Utilities.Utilities;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Setup extends JavaPlugin {

    public static ShapedRecipe Recipe;
    public static ItemStack Item;
    public static List<PlayerObj> DeadPlayers;
    public static Setup instance;
    public static SpiGUI spiGUI;
    public static DataManager dataManager;

    public static DeadPlayersGUI gui;
    public final Map<UUID, RecipeConfiguratorGUI.AnimatedGlassPanel> activeRecipeConfiguratorGUIs = new HashMap<>();

    @SuppressWarnings("all")
    public void onEnable() {

        instance = this;
        dataManager = new DataManager();
        gui = new DeadPlayersGUI();

        if (!(new File(this.getDataFolder() + "/DeadPlayers/").exists()))
            new File(this.getDataFolder() + "/DeadPlayers/").mkdirs();
        DeadPlayers = new CopyOnWriteArrayList<>(dataManager.readJsonFiles(this.getDataFolder() + "/DeadPlayers/"));

        spiGUI = new SpiGUI(this);

        this.saveDefaultConfig();
        Utilities.loadRecipe(Bukkit.getOnlinePlayers());

        Bukkit.getPluginManager().registerEvents(new Listeners(), this);

        HCRCommand hcrCommand = new HCRCommand();
        this.getCommand("hcr").setExecutor(hcrCommand);
        this.getCommand("hcr").setTabCompleter(hcrCommand);
    }

    public void onDisable() {

        activeRecipeConfiguratorGUIs
                .forEach((key, value)
                        -> value.stopAnimation());
        Utilities.clearCaches();
        Utilities.unloadRecipe(Bukkit.getOnlinePlayers());
        SafeTeleportManager.stopAllTimers();
        SkullCreator.clearCache();
        this.saveDefaultConfig();
    }
}