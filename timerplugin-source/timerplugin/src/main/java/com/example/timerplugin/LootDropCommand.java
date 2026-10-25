package com.example.timerplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LootDropCommand implements CommandExecutor {

    private final LootDropManager lootDropManager;

    public LootDropCommand(LootDropManager lootDropManager) {
        this.lootDropManager = lootDropManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("אין לך הרשאה להשתמש בפקודה זו.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("שימוש: /lootdrop start | /lootdrop test", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("הפקודה הזו חייבת להיות מופעלת על ידי שחקן.", NamedTextColor.RED));
                return true;
            }
            lootDropManager.dropLootBarrel(player);
            sender.sendMessage(Component.text("חבית אוצר לבדיקה נשלחה לידך.", NamedTextColor.GREEN));
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (lootDropManager.isRunning()) {
                sender.sendMessage(Component.text("מחזור לוט דרופ כבר פעיל.", NamedTextColor.YELLOW));
                return true;
            }
            lootDropManager.startLootDrops();
            sender.sendMessage(Component.text(
                    "מחזור לוט דרופ התחיל! 2 חביות אוצר ייפלו במהלך חצי השעה הקרובה.",
                    NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("שימוש: /lootdrop start | /lootdrop test", NamedTextColor.RED));
        return true;
    }
}

