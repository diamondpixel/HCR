package takys;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import takys.Files.DataManager;
import takys.Objects.Pair;
import takys.Objects.PlayerObj;
import takys.SkullCreator.SkullCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Utilities {

    public final static Setup setup = Setup.instance;
    public final static DataManager dataManager = Setup.dataManager;
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
            return pr.getProperty(s);
        } catch (IOException ignored) {}
        return "";
    }

    @SuppressWarnings("all")
    public static ItemStack GetPlayerSkull(PlayerObj pObj) {
        Player player = Bukkit.getPlayer(pObj.GetUUID());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(player.getName());
        meta.setOwningPlayer(player);
        ArrayList lore = new ArrayList();
        String serializedLocation = GetSerializedLocation(pObj.GetLoc());
        Duration duration = calculateDifference(pObj.GetDate(), LocalDateTime.now());
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long seconds = duration.getSeconds();
        lore.add("Died " + days + "d, " + hours + "h, " + minutes + "m, " + seconds + "s" + " ago");
        lore.add("Cause of Death: " + GetSerializedDamageCause(pObj.GetDamageCause()));
        lore.add("Death Count: " + player.getStatistic(Statistic.DEATHS));
        lore.add("Coordinates of latest death:");
        lore.add(SerializedToFormattedString(serializedLocation));
        new BukkitRunnable() {@Override public void run() { }}.runTaskAsynchronously(setup);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("all")
    public static Pair<ShapedRecipe, ItemStack> ConstructRecipe() {
        ItemStack Token = new ItemStack(Material.END_CRYSTAL, 1);
        Token.addUnsafeEnchantment(Enchantment.LOYALTY, 10);
        ItemMeta meta = Token.getItemMeta();
        ArrayList lore = new ArrayList();
        lore.add("Choose a dead player to revive.");
        meta.setLore(lore);
        meta.setDisplayName("Revival Token");
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        Token.setItemMeta(meta);
        NamespacedKey Key = new NamespacedKey(setup, "Token");
        ShapedRecipe TokenRecipe = new ShapedRecipe(Key, Token);
        List<String> Materials = Setup.instance.getConfig().getStringList("recipe");
        TokenRecipe.shape("123", "456", "789");
        TokenRecipe.setIngredient('1', Material.valueOf(Materials.get(0)));
        TokenRecipe.setIngredient('2', Material.valueOf(Materials.get(1)));
        TokenRecipe.setIngredient('3', Material.valueOf(Materials.get(2)));
        TokenRecipe.setIngredient('4', Material.valueOf(Materials.get(3)));
        TokenRecipe.setIngredient('5', Material.valueOf(Materials.get(4)));
        TokenRecipe.setIngredient('6', Material.valueOf(Materials.get(5)));
        TokenRecipe.setIngredient('7', Material.valueOf(Materials.get(6)));
        TokenRecipe.setIngredient('8', Material.valueOf(Materials.get(7)));
        TokenRecipe.setIngredient('9', Material.valueOf(Materials.get(8)));
        return new Pair<>(TokenRecipe, Token);
    }

    @SuppressWarnings("all")
    public static ItemStack BackArrowItem() {

        ItemStack BackArrow = SkullCreator.itemFromBase64(back_arrow_head);
        ItemMeta BackArrowMeta = BackArrow.getItemMeta();
        BackArrowMeta.setDisplayName("§3§lGo Back!");
        BackArrow.setItemMeta(BackArrowMeta);

        return BackArrow;
    }

    @SuppressWarnings("all")
    public static ItemStack DeathLocationItem(PlayerObj pObj) {

        ItemStack DeathLocation = isBelowAir(pObj) ? new ItemStack(Material.BARRIER) : SkullCreator.itemFromBase64(death_location_head);
        ItemMeta DeathLocationMeta = DeathLocation.getItemMeta();
        DeathLocationMeta.setDisplayName("§0§lDeath Location");
        ArrayList DeathLocationLore = new ArrayList();
        String serializedLocation = GetSerializedLocation(pObj.GetLoc());

        if (isBelowAir(pObj)) {
            DeathLocationLore.add("§3Cannot respawn at coords because floor is air. Probably void.");
            DeathLocationLore.add("§3Death coordinates (" + SerializedToFormattedString(serializedLocation) + ")");
        } else {
            DeathLocationLore.add("§3Click to respawn " + Bukkit.getPlayer(pObj.GetUUID()).getName());
            DeathLocationLore.add("§3where he died! (" + SerializedToFormattedString(serializedLocation) + ")");
        }

        DeathLocationMeta.setLore(DeathLocationLore);
        DeathLocation.setItemMeta(DeathLocationMeta);

        return DeathLocation;
    }

    @SuppressWarnings("all")
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

    @SuppressWarnings("all")
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

    @SuppressWarnings("all")
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

    @SuppressWarnings("all")
    public static void RevivePlayer(Player Viewer, PlayerObj pObj, Location loc) {
        Player p = Bukkit.getPlayer(pObj.GetUUID());
        if (p.isOnline()) {
            Viewer.closeInventory();
            RemoveFirstItem(Viewer, setup.Item);
            setup.DeadPlayers.remove(pObj);
            dataManager.deleteJsonFile(setup.getDataFolder() + "\\DeadPlayers\\" + pObj.GetUUID() + ".json");
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

    public static String GetSerializedLocation(Location loc) {
        return loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" + loc.getWorld().getUID();
    }

    public static Location GetDeserializedLocation(String s) {
        String[] parts = s.split(";");
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        UUID u = UUID.fromString(parts[3]);
        World w = Bukkit.getServer().getWorld(u);
        return new Location(w, x, y, z);
    }

    public static String SerializedToFormattedString(String string) {
        String[] parts = string.split(";");
        String x = String.valueOf((int) Double.parseDouble(parts[0]));
        String y = String.valueOf((int) Double.parseDouble(parts[1]));
        String z = String.valueOf((int) Double.parseDouble(parts[2]));
        String w = Bukkit.getServer().getWorld(UUID.fromString(parts[3])).getName();
        return "%w , %x , %y , %z".replace("%w", w).replace("%x", x).replace("%y", y).replace("%z", z);
    }

    public static String GetSerializedLocalDateTime(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy-HH.mm.ss");
        return localDateTime.format(formatter);
    }

    public static LocalDateTime GetDeserializedLocalDateTime(String localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy-HH.mm.ss");
        return LocalDateTime.parse(localDateTime, formatter);
    }

    public static String GetSerializedDamageCause(EntityDamageEvent.DamageCause damageCause) {
        return damageCause.toString();
    }

    public static EntityDamageEvent.DamageCause GetDeserializedDamageCause(String damageCause) {
        return EntityDamageEvent.DamageCause.valueOf(damageCause);
    }

    public static Duration calculateDifference(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return Duration.between(dateTime1, dateTime2);
    }

    public static void removeUUIDfromDeadPlayers(UUID uuid) {
        Setup.DeadPlayers.removeIf(playerObj -> Objects.equals(playerObj.GetUUID(), uuid));
    }

    public static boolean isBelowAir(PlayerObj playerObj) {
        World deathWorld = playerObj.GetLoc().getWorld();
        double blockYBelowPlayersDeath = playerObj.GetLoc().getBlockY() - 1;
        Location loc = new Location(deathWorld, playerObj.GetLoc().getX(), blockYBelowPlayersDeath, playerObj.GetLoc().getZ());
        return loc.getBlock().getType() == Material.AIR;
    }
}