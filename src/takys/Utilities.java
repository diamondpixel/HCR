package takys;

import dev.dbassett.skullcreator.SkullCreator;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import takys.Files.DataManager;
import takys.Objects.Pair;
import takys.Objects.PlayerObj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static takys.Objects.Serializers.SerializedToFormattedString;

public class Utilities {

    public final static String death_location_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjllMmYzM2ViMTgwZjA0MzQ5MTZkYzVkMmJiMzI2YTZlYTIyZmM5YmJmOTg4YmMzMWEyNDFmZDQyNzgwMjMifX19";
    public final static String back_arrow_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=";
    public final static String world_spawn_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4OWQ1YjE3ODYyNmVhMjNkMGIwYzNkMmRmNWMwODVlODM3NTA1NmJmNjg1YjVlZDViYjQ3N2ZlODQ3MmQ5NCJ9fX0=";
    public final static String eye_location_head = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQyY2Y4Y2U0ODdiNzhmYTIwM2Q1NmNmMDE0OTE0MzRiNGMzM2U1ZDIzNjgwMmM2ZDY5MTQ2YTUxNDM1YjAzZCJ9fX0=";

    public static void RemoveFirstItem(Player player, ItemStack stack) {
        for (ItemStack item : player.getInventory()) {
            if (item == null)
                continue;
            if (!item.hasItemMeta())
                continue;
            if (Objects.equals(item.getItemMeta(), stack.getItemMeta()))
                item.setAmount(item.getAmount() - 1);
        }
    }

    public static String GetString(String s, File f) {
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

    public static ItemStack GetPlayerSkull(PlayerObj pObj) {
        Player player = Bukkit.getPlayer(pObj.GetUUID());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(player.getName());
        meta.setOwningPlayer(player);
        ArrayList lore = new ArrayList();
        long secs = ((new Date()).getTime() - pObj.GetDate().getTime()) / 1000L;
        int days = (int) secs / 86400;
        secs -= days * 86400;
        int hours = (int) secs / 3600;
        secs -= hours * 3600;
        int mins = (int) secs / 60;
        lore.add("Died " + days + "d, " + hours + "h, " + mins + "m ago");
        lore.add("Cause of Death: " + pObj.DC);
        lore.add("Death Count: " + player.getStatistic(Statistic.DEATHS));
        lore.add("Coordinates of latest death:");
        lore.add((SerializedToFormattedString(pObj.GetLoc())));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static Pair<ShapedRecipe, ItemStack> ConstructRecipe() {
        ItemStack Token = new ItemStack(Material.END_CRYSTAL, 1);
        Token.addUnsafeEnchantment(Enchantment.LOYALTY, 10);
        ItemMeta meta = Token.getItemMeta();
        ArrayList lore = new ArrayList();
        lore.add("Choose a fallen ally to ressurect.");
        lore.add("They will appear at your location");
        meta.setLore(lore);
        meta.setDisplayName("Revival Token");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        Token.setItemMeta(meta);
        NamespacedKey Key = new NamespacedKey(Setup.instance, "Token");
        ShapedRecipe TokenRecipe = new ShapedRecipe(Key, Token);
        TokenRecipe.shape("123", "456", "789");
        TokenRecipe.setIngredient('1', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('2', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('3', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('4', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('5', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('6', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('7', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('8', Material.END_CRYSTAL);
        TokenRecipe.setIngredient('9', Material.END_CRYSTAL);
        return new Pair<>(TokenRecipe, Token);
    }

    public static ItemStack BackArrowItem() {

        ItemStack BackArrow = SkullCreator.itemFromBase64(back_arrow_head);
        ItemMeta BackArrowMeta = BackArrow.getItemMeta();
        BackArrowMeta.setDisplayName("§3§lGo Back!");
        BackArrow.setItemMeta(BackArrowMeta);

        return BackArrow;
    }

    public static ItemStack DeathLocationItem(PlayerObj pObj) {

        ItemStack DeathLocation = SkullCreator.itemFromBase64(death_location_head);
        ItemMeta DeathLocationMeta = DeathLocation.getItemMeta();
        DeathLocationMeta.setDisplayName("§0§lDeath Location");
        ArrayList DeathLocationLore = new ArrayList();
        DeathLocationLore.add("§3Click to respawn " + Bukkit.getPlayer(pObj.GetUUID()).getName());
        DeathLocationLore.add("§3where he died! (" + (SerializedToFormattedString(pObj.GetLoc())) + ")");
        DeathLocationMeta.setLore(DeathLocationLore);
        DeathLocation.setItemMeta(DeathLocationMeta);

        return DeathLocation;
    }

    public static ItemStack WorldSpawnItem(UUID uuid) {

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

    public static ItemStack EyeLocationItem(UUID uuid) {

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

    public static ItemStack BedItem(UUID uuid) {

        Player player = Bukkit.getPlayer(uuid);
        ItemStack Bed = new ItemStack(Material.RED_BED);
        ItemMeta BedMeta = Bed.getItemMeta();
        ArrayList BedLore = new ArrayList();

        if (player.getBedSpawnLocation() == null) {
            BedLore.add("§3" + Bukkit.getPlayer(uuid).getName() + " doesn't have a bed :(");
            BedMeta.setLore(BedLore);
            BedMeta.setDisplayName("§4§lBed");
            Bed.setItemMeta(BedMeta);
            return Bed;
        }

        if (player.getBedSpawnLocation().getWorld().getEnvironment() != World.Environment.NETHER) {
            Bed = new ItemStack(Material.GREEN_BED);
            BedLore.add("§3Click to respawn " + Bukkit.getPlayer(uuid).getName());
            BedLore.add("§3at his bed!");
            BedMeta.setLore(BedLore);
            BedMeta.setDisplayName("§a§lBed");
        } else {
            Bed = new ItemStack(Material.RESPAWN_ANCHOR);
            BedLore.add("§3Click to respawn " + Bukkit.getPlayer(uuid).getName());
            BedLore.add("§3at his respawn anchor!");
            BedMeta.setLore(BedLore);
            BedMeta.setDisplayName("§5§lRespawn Anchor");
        }
        Bed.setItemMeta(BedMeta);
        return Bed;
    }

    public static void RevivePlayer(Player Viewer, PlayerObj pObj, Location loc) {
        Player p = Bukkit.getPlayer(pObj.GetUUID());
        if (p.isOnline()) {
            Viewer.closeInventory();
            RemoveFirstItem(Viewer, Setup.Item);
            Setup.DeadPlayers.remove(pObj);
            DataManager.EraseFileContents();
            Setup.DeadPlayers.forEach((player) -> {
                DataManager.WriteObjectToFile(player);
            });
            p.teleport(loc);
            p.setGameMode(GameMode.SURVIVAL);
            loc.getWorld().strikeLightningEffect(loc);
            p.addPotionEffect(PotionEffectType.DAMAGE_RESISTANCE.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.ABSORPTION.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.REGENERATION.createEffect(1200, 4));
            p.addPotionEffect(PotionEffectType.LUCK.createEffect(1200, 0));
            p.addPotionEffect(PotionEffectType.GLOWING.createEffect(60, 0));
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0F, 1.0F);
            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 1.0F);
        }
    }
}