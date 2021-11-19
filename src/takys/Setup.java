package takys;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import takys.Util.Inventories;
import takys.Util.Utilities;

public class Setup extends JavaPlugin {

      public static File dataFolder;

      public static FileConfiguration config;

      public static Setup instance;

      private Inventories inventories;

      public Inventories getInventories()
      {
            return inventories;
      }

      private Utilities util;

      public Utilities getUtil()
      {
            return util;
      }

      private Events events;

      public Events getEvents()
      {
            return events;
      }
      public void onEnable() {


            instance = this;
            inventories = new Inventories();
            util = new Utilities();

            this.getServer().getPluginManager().registerEvents(new Events(), this);
            this.getServer().getPluginManager().registerEvents(new Inventories(), this);

            dataFolder = this.getDataFolder();
            config = this.getConfig();
            File configFile = new File(this.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                  this.ConfigCreate();
            }

            this.saveConfig();
            this.getCommand("hr").setExecutor(new Commands());
            FileHandler.LoadFallen();
            Utilities.tokenRecipe();
      }
      public void onDisable(){

             instance = null;
             this.saveConfig();
             FileHandler.SaveFallen();

      }


      public static Plugin getPlugin() {
            return instance;
      }
      
      private void ConfigCreate() {
            config.set("Crafting.Revival Token.Top Left", "NETHERITE_BLOCK");
            config.set("Crafting.Revival Token.Top Center", "TOTEM_OF_UNDYING");
            config.set("Crafting.Revival Token.Top Right", "NETHERITE_BLOCK");
            config.set("Crafting.Revival Token.Middle Left", "HEART_OF_THE_SEA");
            config.set("Crafting.Revival Token.Middle Center", "NETHER_STAR");
            config.set("Crafting.Revival Token.Middle Right", "SHULKER_BOX");
            config.set("Crafting.Revival Token.Bottom Left", "NETHERITE_BLOCK");
            config.set("Crafting.Revival Token.Bottom Center", "END_CRYSTAL");
            config.set("Crafting.Revival Token.Bottom Right", "NETHERITE_BLOCK");
      }
}
