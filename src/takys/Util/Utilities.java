package takys.Util;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import takys.FileHandler;
import takys.PlayerObj;
import takys.Setup;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Utilities implements Listener {


    public static List DeadPlayers = new ArrayList();
    public static List trimmedPlayers = new ArrayList();
    public static NamespacedKey key;
    public static ItemStack token;

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
        public ItemStack createItem(Material material, String name) {
            ItemStack item = new ItemStack(material, 1);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(name);
            item.setItemMeta(itemMeta);
            return item;
        }

        public ItemStack createItemWithData(Material material, String name, int data) {
            ItemStack item = new ItemStack(material, 1, (short) data);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(name);
            item.setItemMeta(itemMeta);
            return item;
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


        private void revivePlayer(Player p, Location loc, World world) {
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
    }
