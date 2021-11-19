package takys.Util;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import takys.FileHandler;
import takys.PlayerObj;
import takys.Setup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Utilities implements Listener {


    private final Setup setup = Setup.instance;

    public static List DeadPlayers = new ArrayList();
    public static List trimmedPlayers = new ArrayList();
    public static NamespacedKey key;
    public static ItemStack token;
    //public static List deadPlayers = new ArrayList();

    public static ItemStack getPlayer(PlayerObj pObj) {
            Player player = Bukkit.getPlayer(pObj.GetUUID());
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)item.getItemMeta();
            meta.setDisplayName(player.getName());
            meta.setOwningPlayer(player);
            ArrayList lore = new ArrayList();
            long secs = ((new Date()).getTime() - pObj.GetDate().getTime()) / 1000L;
            int days = (int)secs / 86400;
            secs -= (long)(days * 86400);
            int hours = (int)secs / 3600;
            secs -= (long)(hours * 3600);
            int mins = (int)secs / 60;
            lore.add("Died " + days + "d, " + hours + "h, " + mins + "m ago");
            lore.add("Cause of Death: " + pObj.DC);
            lore.add("Death Count: " + player.getStatistic(Statistic.DEATHS));
            lore.add("Coordinates of latest death:");
            lore.add(pObj.GetLoc());
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }

        public static boolean noPermission(CommandSender sender) {
            sender.sendMessage("Sorry, you don't have permission to do that.");
            return true;
        }

        public static boolean isNumeric(String strNum) {
            if (strNum == null) {
                return false;
            }
            try {
                double d = Double.parseDouble(strNum);
            } catch (NumberFormatException nfe) {
                return false;
            }
            return true;
        }

        public static void tokenRecipe() {
            token = new ItemStack(Material.END_CRYSTAL, 1);
            token.addUnsafeEnchantment(Enchantment.LOYALTY, 10);
            ItemMeta meta = token.getItemMeta();
            ArrayList lore = new ArrayList();
            lore.add("Choose a fallen ally to ressurect.");
            lore.add("They will appear at your location");
            meta.setLore(lore);
            meta.setDisplayName("Revival Token");
            token.setItemMeta(meta);
            key = new NamespacedKey(Setup.getPlugin(), "Token");
            ShapedRecipe tokenRecipe = new ShapedRecipe(key, token);
            tokenRecipe.shape(new String[]{"123", "456", "789"});
            tokenRecipe.setIngredient('1', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Top Left")));
            tokenRecipe.setIngredient('2', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Top Center")));
            tokenRecipe.setIngredient('3', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Top Right")));
            tokenRecipe.setIngredient('4', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Middle Left")));
            tokenRecipe.setIngredient('5', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Middle Center")));
            tokenRecipe.setIngredient('6', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Middle Right")));
            tokenRecipe.setIngredient('7', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Bottom Left")));
            tokenRecipe.setIngredient('8', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Bottom Center")));
            tokenRecipe.setIngredient('9', Material.getMaterial(Setup.config.getString("Crafting.Revival Token.Bottom Right")));

                Bukkit.removeRecipe(key);
                Bukkit.addRecipe(tokenRecipe);

            Bukkit.getScheduler().scheduleSyncDelayedTask(Setup.getPlugin(), new Runnable() {
                public void run() {
                    Iterator var = Setup.getPlugin().getServer().getOnlinePlayers().iterator();

                    while (var.hasNext()) {
                        Player p = (Player) var.next();
                        p.discoverRecipe(key);
                    }

                }
            }, 1L);
         }

        public ItemStack backArrow(){

            ItemStack BackArrow = SkullCreator.itemFromBase64(back_arrow_head);
            ItemMeta BackArrowMeta = BackArrow.getItemMeta();
            BackArrowMeta.setDisplayName("§3§lGo Back!");
            BackArrow.setItemMeta(BackArrowMeta);

            return BackArrow;
        }

        public ItemStack worldSpawnHead(UUID uuid){

            ItemStack WorldSpawnHead = SkullCreator.itemFromBase64(world_spawn_head);
            ItemMeta WorldSpawnHeadMeta = WorldSpawnHead.getItemMeta();
            WorldSpawnHeadMeta.setDisplayName("§3§lWorld Spawn");
            ArrayList WorldSpawnHeadLore = new ArrayList();
            WorldSpawnHeadLore.add("§3Click to respawn " + Bukkit.getPlayer(uuid).getName());
            WorldSpawnHeadLore.add("§3at world's spawnpoint!");
            WorldSpawnHeadMeta.setLore(WorldSpawnHeadLore);
            WorldSpawnHead.setItemMeta(WorldSpawnHeadMeta);

            return WorldSpawnHead;
        }

        public ItemStack eyeLocationHead(UUID uuid){

            ItemStack EyeLocationHead = SkullCreator.itemFromBase64(eye_location_head);
            ItemMeta EyeLocationHeadMeta = EyeLocationHead.getItemMeta();
            EyeLocationHeadMeta.setDisplayName("§3§lSimple Revive");
            ArrayList EyeLocationHeadLore = new ArrayList();
            EyeLocationHeadLore.add("§3Click to respawn " + Bukkit.getPlayer(uuid).getName());
            EyeLocationHeadLore.add("§3where you are looking!");
            EyeLocationHeadMeta.setLore(EyeLocationHeadLore);
            EyeLocationHead.setItemMeta(EyeLocationHeadMeta);

            return EyeLocationHead;
        }

        public ItemStack bed(UUID uuid) {
        ItemStack Bed;
            if (Bukkit.getPlayer(uuid).getBedSpawnLocation() != null) {
                Bed = new ItemStack(Material.GREEN_BED, 1);
                ItemMeta BedMeta = Bed.getItemMeta();
                BedMeta.setDisplayName("§3§lBed Spawn");
                ArrayList BedLore = new ArrayList();
                BedLore.add("§3Click to respawn " + Bukkit.getPlayer(uuid).getName());
                BedLore.add("§3at his bed!");
                BedMeta.setLore(BedLore);
                Bed.setItemMeta(BedMeta);
            } else {
                Bed = new ItemStack(Material.RED_BED, 1);
                ItemMeta BedMeta = Bed.getItemMeta();
                BedMeta.setDisplayName("§3§lBed Spawn");
                ArrayList BedLore = new ArrayList();
                BedLore.add("§3" + Bukkit.getPlayer(uuid).getName());
                BedLore.add("§3has no bed!");
                BedMeta.setLore(BedLore);
                Bed.setItemMeta(BedMeta);

            }
            return Bed;
        }

    public static String getString(String s, File f) {
        Properties pr = new Properties();
        try {
            FileInputStream in = new FileInputStream(f);
            pr.load(in);
            String string = pr.getProperty(s);
            return string;
        } catch (IOException e) {

        }
        return "";
    }
    public void revivePlayer(Player p, Location loc, World world) {
            world.strikeLightningEffect(loc);
            p.teleport(loc);
            p.setGameMode(GameMode.SURVIVAL);
            p.addPotionEffect(PotionEffectType.DAMAGE_RESISTANCE.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.ABSORPTION.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.REGENERATION.createEffect(1200, 4));
            p.addPotionEffect(PotionEffectType.UNLUCK.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.GLOWING.createEffect(60, 0));
            FileHandler.SaveFallen();
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
        }
    private final static String back_arrow_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=";
    private final static String world_spawn_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4OWQ1YjE3ODYyNmVhMjNkMGIwYzNkMmRmNWMwODVlODM3NTA1NmJmNjg1YjVlZDViYjQ3N2ZlODQ3MmQ5NCJ9fX0=";
    private final static String eye_location_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQyY2Y4Y2U0ODdiNzhmYTIwM2Q1NmNmMDE0OTE0MzRiNGMzM2U1ZDIzNjgwMmM2ZDY5MTQ2YTUxNDM1YjAzZCJ9fX0=";
    }
