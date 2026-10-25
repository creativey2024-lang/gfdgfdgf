package com.example.timerplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * מנהל את מערכת הלוט דרופ: חביות אוצר שנופלות ליד שחקנים אקראיים,
 * מלאות בפריטים שעוזרים לשחקנים (XP, ספרי קסם, אוכל, חומרים).
 * מופעל דרך /lootdrop start - שתי חביות נופלות במהלך חצי שעה.
 */
public class LootDropManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private boolean running = false;

    public LootDropManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return running;
    }

    /** מתחיל מחזור של 2 חביות אוצר שנופלות במהלך חצי שעה (אחת בכל חצי מהזמן) */
    public void startLootDrops() {
        if (running) {
            return;
        }
        running = true;

        long halfWindow = 20L * 60L * 15L; // 15 דקות = חצי מחצי השעה

        long firstDelay = (long) (random.nextDouble() * halfWindow);
        long secondDelay = halfWindow + (long) (random.nextDouble() * halfWindow);

        Bukkit.getScheduler().runTaskLater(plugin, this::dropLootBarrel, firstDelay);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            dropLootBarrel();
            running = false;
        }, secondDelay);
    }

    public void stopLootDrops() {
        running = false;
    }

    private void dropLootBarrel() {
        List<Player> players = List.copyOf(Bukkit.getOnlinePlayers());
        if (players.isEmpty()) {
            running = false;
            return;
        }

        Player target = players.get(random.nextInt(players.size()));
        dropLootBarrel(target);
    }

    /** מוריד חבית אוצר מיד ליד שחקן ספציפי - משמש לבדיקה (/lootdrop test) ולמחזור הרגיל */
    public void dropLootBarrel(Player target) {
        World world = target.getWorld();
        Location base = target.getLocation();

        double offsetX = random.nextDouble() * 10 - 5;
        double offsetZ = random.nextDouble() * 10 - 5;
        Location dropLoc = base.clone().add(offsetX, 0, offsetZ);
        dropLoc.setY(world.getHighestBlockYAt(dropLoc) + 1);

        Block block = dropLoc.getBlock();
        block.setType(Material.BARREL);

        if (block.getState() instanceof Barrel barrel) {
            fillWithLoot(barrel.getInventory());
            barrel.update();
        }

        Component title = Component.text("📦 חבית אוצר נחתה!", NamedTextColor.GOLD);
        Component subtitle = Component.text(
                "ליד " + target.getName() + " במיקום " + dropLoc.getBlockX() + ", " + dropLoc.getBlockY() + ", " + dropLoc.getBlockZ(),
                NamedTextColor.YELLOW
        );
        Title displayTitle = Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(displayTitle);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2f, 1f);
        }
        Bukkit.broadcast(Component.text(
                "📦 חבית אוצר נחתה ליד " + target.getName() + "! מיקום: " +
                        dropLoc.getBlockX() + ", " + dropLoc.getBlockY() + ", " + dropLoc.getBlockZ(),
                NamedTextColor.GOLD));
    }

    private void fillWithLoot(Inventory inventory) {
        inventory.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 12 + random.nextInt(8)));
        inventory.addItem(new ItemStack(Material.GOLDEN_APPLE, 1 + random.nextInt(2)));

        if (random.nextInt(4) == 0) {
            inventory.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        }

        inventory.addItem(randomEnchantedBook());
        inventory.addItem(randomEnchantedBook());
        inventory.addItem(new ItemStack(Material.DIAMOND, 2 + random.nextInt(4)));
        inventory.addItem(new ItemStack(Material.IRON_INGOT, 4 + random.nextInt(4)));
        inventory.addItem(new ItemStack(Material.COOKED_BEEF, 6 + random.nextInt(6)));
    }

    private ItemStack randomEnchantedBook() {
        Enchantment[] pool = {
                Enchantment.SHARPNESS,
                Enchantment.PROTECTION,
                Enchantment.EFFICIENCY,
                Enchantment.UNBREAKING,
                Enchantment.FIRE_ASPECT,
                Enchantment.POWER,
                Enchantment.FEATHER_FALLING
        };
        Enchantment chosen = pool[random.nextInt(pool.length)];
        int level = 1 + random.nextInt(Math.max(1, chosen.getMaxLevel()));

        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        if (book.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.addStoredEnchant(chosen, level, true);
            book.setItemMeta(storageMeta);
        }
        return book;
    }
}
