package com.example.timerplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * מנהל את רשימת האירועים האקראיים (10 אפשרויות) - כל אירוע כולל
 * הודעת מסך (Title + צ'אט) ואפקט משחק אמיתי, אקסטרים ומסוכן.
 * שימו לב: האפקטים כאן דורשים נזק אמיתי, פיצוצים שמהרסים בלוקים, וברקים
 * שמכים בפועל. כווננו לפי הצורך של השרת שלכם אם זה חזק מדי.
 */
public class EventManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private BukkitTask task;

    private final Map<String, EventDefinition> events = new LinkedHashMap<>();

    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
        registerEvents();
    }

    private void registerEvents() {
        events.put("meteors", new EventDefinition("גשם מטאורים ענק נופל מהשמיים! ☄🔥", this::effectMeteors));
        events.put("earthquake", new EventDefinition("רעידת אדמה הרסנית מתחילה! 🌍💥", this::effectEarthquake));
        events.put("storm", new EventDefinition("סופת ברקים קטלנית מכה בשטח! ⚡⚡", this::effectStorm));
        events.put("zombies", new EventDefinition("פלישת זומבים המונית מתחילה! 🧟🧟🧟", this::effectZombies));
        events.put("blood_rain", new EventDefinition("גשם דם ארסי יורד מהשמיים! 🩸🩸", this::effectBloodRain));
        events.put("volcano", new EventDefinition("התפרצות געשית ענקית! 🌋🔥", this::effectVolcano));
        events.put("fog", new EventDefinition("ערפל סמיך ומטריף חושים יורד! 🌫️😵", this::effectFog));
        events.put("cold_wave", new EventDefinition("קפאון קיצוני תוקף את האזור! ❄🥶", this::effectColdWave));
        events.put("lights", new EventDefinition("כוח בלתי ידוע שואב אתכם למעלה! 👽🛸", this::effectLights));
        events.put("underground_noise", new EventDefinition("משהו... מתעורר מתחת לרגליים שלכם...", this::effectUndergroundNoise));
    }

    public java.util.Set<String> getEventKeys() {
        return events.keySet();
    }

    /** מתחיל את המחזור האוטומטי - אירוע אקראי כל 10 דקות */
    public void startScheduler() {
        long interval = 20L * 60L * 10L; // 10 דקות
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::triggerRandomEvent, interval, interval);
    }

    public void stopScheduler() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void triggerRandomEvent() {
        List<String> keys = List.copyOf(events.keySet());
        String key = keys.get(random.nextInt(keys.size()));
        triggerEvent(key);
    }

    /** מציג הודעה ומפעיל את האפקט האמיתי של האירוע. מחזיר false אם המפתח לא קיים */
    public boolean triggerEvent(String key) {
        EventDefinition definition = events.get(key);
        if (definition == null) {
            return false;
        }

        Component title = Component.text("⚠ אירוע מיוחד! ⚠", NamedTextColor.RED);
        Component subtitle = Component.text(definition.message, NamedTextColor.YELLOW);
        Title displayTitle = Title.title(
                title,
                subtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(displayTitle);
        }
        Bukkit.broadcast(Component.text(definition.message, NamedTextColor.GOLD));

        definition.effect.run();
        return true;
    }

    // ===================== האפקטים בפועל (אקסטרים) =====================

    private void effectMeteors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            Location base = player.getLocation();

            for (int i = 0; i < 10; i++) {
                double offsetX = random.nextDouble() * 24 - 12;
                double offsetZ = random.nextDouble() * 24 - 12;
                Location spawnLoc = base.clone().add(offsetX, 25, offsetZ);

                FallingBlock meteor = world.spawnFallingBlock(spawnLoc, Material.MAGMA_BLOCK.createBlockData());
                meteor.setDropItem(false);
                meteor.setHurtEntities(true);

                Location impactLoc = base.clone().add(offsetX, 0, offsetZ);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // פיצוץ גדול ואמיתי, כולל הרס בלוקים
                        world.createExplosion(impactLoc, 4.5f, true, true);
                    }
                }.runTaskLater(plugin, 30L);
            }
        }
    }

    private void effectEarthquake() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 140, 2));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3f, 0.3f);
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 3f, 0.3f);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // כ-5 שניות של הרס
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location loc = player.getLocation();
                    player.setVelocity(new Vector(
                            (random.nextDouble() - 0.5) * 0.3,
                            0.6,
                            (random.nextDouble() - 0.5) * 0.3
                    ));
                    player.getWorld().spawnParticle(Particle.BLOCK, loc, 60, 2.5, 0.3, 2.5,
                            Material.STONE.createBlockData());

                    // נזק תקופתי מקריסת מבנים
                    if (ticks % 30 == 0) {
                        player.damage(1.5);
                    }

                    // בלוקים "קורסים" ונופלים מסביב
                    if (random.nextInt(3) == 0) {
                        double ox = random.nextDouble() * 6 - 3;
                        double oz = random.nextDouble() * 6 - 3;
                        Location fallLoc = loc.clone().add(ox, 6, oz);
                        FallingBlock fb = player.getWorld().spawnFallingBlock(fallLoc, Material.GRAVEL.createBlockData());
                        fb.setDropItem(false);
                        fb.setHurtEntities(true);
                    }
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void effectStorm() {
        // פשוט: 3 ברקים אמיתיים שמכים ישירות על כל שחקן, בלי לגעת במזג האוויר בכלל
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 3; i++) {
                long delay = i * 15L; // ברק כל 0.75 שניות בערך
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            // מכה בדיוק במיקום הנוכחי של השחקן - נזק ואש אמיתיים
                            player.getWorld().strikeLightning(player.getLocation());
                        }
                    }
                }.runTaskLater(plugin, delay);
            }
        }
    }

    /** מנקה את מזג האוויר בכל העולמות - שקול ל-/weather clear */
    private void clearWeather() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(0);
                world.setThunderDuration(0);
            }
        }
    }

    private void effectZombies() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            Location loc = player.getLocation();

            for (int i = 0; i < 12; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 4 + random.nextDouble() * 6;
                Location spawnLoc = loc.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

                LivingEntity entity = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
                if (entity instanceof Zombie zombie) {
                    zombie.setTarget(player);
                    zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                    zombie.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1));
                }
            }
        }
    }

    private void effectBloodRain() {
        // מפעילים גשם אמיתי (בלי רעמים) כדי שבאמת ייראה כאילו יורד גשם,
        // ומעליו המון חלקיקים אדומים צפופים שנותנים לו את הצבע האדום.
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL) {
                world.setStorm(true);
                world.setWeatherDuration(300); // 15 שניות בדיוק
            }
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 300) { // 15 שניות בדיוק, ואז נעצר לגמרי
                    cancel();
                    clearWeather(); // בדיוק כמו /weather clear, שלא יישאר תקוע
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location loc = player.getLocation();
                    World world = player.getWorld();

                    // המון חלקיקים אדומים ממש סביב וקרוב לשחקן - עמוד "גשם" אדום נראה לעין
                    for (int i = 0; i < 30; i++) {
                        double x = loc.getX() + (random.nextDouble() * 8 - 4);
                        double z = loc.getZ() + (random.nextDouble() * 8 - 4);
                        double y = loc.getY() + 1 + random.nextDouble() * 6;
                        world.spawnParticle(Particle.DUST, new Location(world, x, y, z), 1,
                                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 2.5f));
                    }

                    if (ticks % 40 == 0) {
                        player.damage(1.0); // "ארסי" - נזק ישיר תקופתי
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));
                    }
                }
                ticks += 4;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void effectVolcano() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            World world = player.getWorld();
            Location loc = player.getLocation();

            for (int i = 0; i < 5; i++) {
                double offsetX = random.nextDouble() * 10 - 5;
                double offsetZ = random.nextDouble() * 10 - 5;
                Location explosionLoc = loc.clone().add(offsetX, 0, offsetZ);
                // פיצוץ ענק שמהרס בלוקים ומצית אש
                world.createExplosion(explosionLoc, 5.0f, true, true);
            }

            // מטר בלוקי מאגמה בוערים
            for (int i = 0; i < 6; i++) {
                double offsetX = random.nextDouble() * 14 - 7;
                double offsetZ = random.nextDouble() * 14 - 7;
                Location spawnLoc = loc.clone().add(offsetX, 15, offsetZ);
                FallingBlock magma = world.spawnFallingBlock(spawnLoc, Material.MAGMA_BLOCK.createBlockData());
                magma.setDropItem(false);
                magma.setHurtEntities(true);
            }

            player.setFireTicks(140);
        }
    }

    private void effectFog() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 160, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 160, 1));
        }
    }

    private void effectColdWave() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 260, 3));
            player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 260, 2));
            player.setFreezeTicks(280);
            player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 100, 4, 4, 4, 0.08);
        }

        // נזק תקופתי מהיפותרמיה
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 200) { // 10 שניות
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.damage(0.5);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 20, 1, 1, 1, 0.05);
                }
                ticks += 40;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    private void effectLights() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // שאיבה חזקה כלפי מעלה
            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 4));
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 3, 0), 80, 2, 4, 2, 0.08);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 3f, 0.5f);

            // אחרי השאיבה - נפילה איטית כדי שלא ימותו, אבל עדיין חוויה קיצונית
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0));
                }
            }.runTaskLater(plugin, 95L);
        }
    }

    private void effectUndergroundNoise() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 140, 0));
            player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 3f, 0.4f);
        }

        // דגלת מפחיד - התפרצות פתאומית אחרי כמה שניות, כשהשחקנים כבר לא ערוכים
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Location loc = player.getLocation();
                    World world = player.getWorld();

                    player.playSound(loc, Sound.ENTITY_WARDEN_ROAR, 4f, 1f);
                    player.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 4f, 1f);
                    world.spawnParticle(Particle.BLOCK, loc, 80, 2, 1, 2, Material.DEEPSLATE.createBlockData());

                    player.setVelocity(new Vector(0, 0.5, 0));
                    player.damage(2.0);

                    for (int i = 0; i < 5; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double dist = 2 + random.nextDouble() * 3;
                        Location spawnLoc = loc.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                        LivingEntity entity = (LivingEntity) world.spawnEntity(spawnLoc, EntityType.SILVERFISH);
                        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                    }
                }
            }
        }.runTaskLater(plugin, 70L); // כ-3.5 שניות אחרי - בדיוק כשחושבים שזה נגמר
    }

    /** מבנה עזר קטן שמחזיק הודעה ואת האפקט (Runnable) שירוץ בפועל */
    private static class EventDefinition {
        final String message;
        final Runnable effect;

        EventDefinition(String message, Runnable effect) {
            this.message = message;
            this.effect = effect;
        }
    }
}
