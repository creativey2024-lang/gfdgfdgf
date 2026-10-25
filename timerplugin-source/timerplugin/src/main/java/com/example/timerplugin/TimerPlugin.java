package com.example.timerplugin;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerPlugin extends JavaPlugin implements CommandExecutor {

    // תבנית שתופסת מספר ואחריו d/h/m/s, לדוגמה: 1d, 2h, 30m, 45s (אפשר לשלב: 1d2h3m4s)
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);

    private BossBar activeBossBar;
    private BukkitTask activeTask;
    private long totalSeconds;
    private long remainingSeconds;

    private EventManager eventManager;
    private MaceRecipeManager maceRecipeManager;
    private LootDropManager lootDropManager;

    @Override
    public void onEnable() {
        getCommand("timer").setExecutor(this);

        // מערכת האירועים האקראיים כל 10 דקות + פקודת /event run
        // שימו לב: לא מתחיל אוטומטית יותר - רק דרך /start (או /event run ידנית)
        eventManager = new EventManager(this);
        EventCommand eventCommand = new EventCommand(eventManager);
        getCommand("event").setExecutor(eventCommand);
        getCommand("event").setTabCompleter(eventCommand);

        // מערכת לוט דרופ - חביות אוצר, מופעלת דרך /lootdrop start
        lootDropManager = new LootDropManager(this);
        getCommand("lootdrop").setExecutor(new LootDropCommand(lootDropManager));

        // פקודת /start - מפעילה טיימר לשעה + מחזור אירועים יחד
        getCommand("start").setExecutor(new StartCommand(this));

        // מתכון מותאם אישית למייס (Heavy Core + Breeze Rod + נוצ' אפל)
        maceRecipeManager = new MaceRecipeManager(this);
        getServer().getPluginManager().registerEvents(maceRecipeManager, this);

        getLogger().info("TimerPlugin הופעל בהצלחה!");
    }

    @Override
    public void onDisable() {
        stopTimer(false);
        if (eventManager != null) {
            eventManager.stopScheduler();
        }
        if (lootDropManager != null) {
            lootDropManager.stopLootDrops();
        }
    }

    /** מפעיל את "המשחק" כולו: טיימר לשעה + מחזור אירועים כל 10 דקות. נקרא מ-/start */
    public void startMatch() {
        startTimer(3600); // שעה שלמה
        eventManager.startScheduler();
    }

    /** עוצר את "המשחק" - מבטל את הטיימר ואת מחזור האירועים. נקרא מ-/start stop */
    public void stopMatch() {
        stopTimer(true);
        eventManager.stopScheduler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // רק OP יכול להשתמש בפקודת הטיימר
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("אין לך הרשאה להשתמש בפקודה זו.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("שימוש: /timer <זמן, לדוגמה 1d1h1m1s> | /timer cancel", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel") || args[0].equalsIgnoreCase("cancal") || args[0].equalsIgnoreCase("stop")) {
            if (activeTask == null) {
                sender.sendMessage(Component.text("אין טיימר פעיל כרגע.", NamedTextColor.YELLOW));
            } else {
                stopTimer(true);
                sender.sendMessage(Component.text("הטיימר בוטל.", NamedTextColor.YELLOW));
            }
            return true;
        }

        long seconds = parseDuration(args[0]);
        if (seconds <= 0) {
            sender.sendMessage(Component.text(
                    "פורמט זמן לא תקין. השתמשו למשל: 1d, 2h, 30m, 10s או שילוב כמו 1d2h3m",
                    NamedTextColor.RED));
            return true;
        }

        startTimer(seconds);
        sender.sendMessage(Component.text("הטיימר התחיל למשך " + formatDuration(seconds) + ".", NamedTextColor.GREEN));
        return true;
    }

    /**
     * מפרק מחרוזת כמו "1d1h1m1s" למספר שניות כולל.
     * תומך בכל שילוב וסדר של d (ימים), h (שעות), m (דקות), s (שניות).
     */
    private long parseDuration(String input) {
        Matcher matcher = DURATION_PATTERN.matcher(input);
        long totalSeconds = 0;
        boolean foundAny = false;

        while (matcher.find()) {
            foundAny = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            switch (unit) {
                case "d" -> totalSeconds += value * 86400L;
                case "h" -> totalSeconds += value * 3600L;
                case "m" -> totalSeconds += value * 60L;
                case "s" -> totalSeconds += value;
            }
        }

        return foundAny ? totalSeconds : -1;
    }

    private void startTimer(long seconds) {
        // אם יש טיימר פעיל, נעצור אותו קודם
        stopTimer(false);

        this.totalSeconds = seconds;
        this.remainingSeconds = seconds;

        activeBossBar = BossBar.bossBar(
                Component.text("הפיויפי יפעל בעוד: " + formatDuration(remainingSeconds)),
                1.0f,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS
        );

        // הצגת ה-bossbar לכל השחקנים המחוברים כרגע
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(activeBossBar);
        }

        activeTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            remainingSeconds--;

            if (remainingSeconds <= 0) {
                finishTimer();
                return;
            }

            float progress = (float) remainingSeconds / (float) totalSeconds;
            activeBossBar.progress(Math.max(0f, Math.min(1f, progress)));
            activeBossBar.name(Component.text("הפיויפי יפעל בעוד: " + formatDuration(remainingSeconds)));

            // הוספת שחקנים שהתחברו במהלך הטיימר
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showBossBar(activeBossBar);
            }
        }, 20L, 20L); // רץ כל שנייה (20 טיקים)
    }

    private void finishTimer() {
        Component title = Component.text("⚔ הפיויפי מופעל! ⚔", NamedTextColor.RED);
        Component subtitle = Component.text("תתכוננו לקרב!", NamedTextColor.YELLOW);
        Title displayTitle = Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(displayTitle);
            player.sendMessage(Component.text("⏰ הטיימר הסתיים - הפיויפי מופעל!", NamedTextColor.GOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
        }
        stopTimer(false);

        // הסיבוב נגמר - עוצרים גם את מחזור האירועים האקראיים
        if (eventManager != null) {
            eventManager.stopScheduler();
        }
    }

    private void stopTimer(boolean silent) {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
        if (activeBossBar != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.hideBossBar(activeBossBar);
            }
            activeBossBar = null;
        }
    }

    /**
     * הופך שניות לפורמט קריא כמו "1d 2h 3m 4s"
     */
    private String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
