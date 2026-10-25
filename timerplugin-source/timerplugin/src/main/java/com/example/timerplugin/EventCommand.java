package com.example.timerplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final EventManager eventManager;

    public EventCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // רק OP יכול להשתמש בפקודת האירועים
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("אין לך הרשאה להשתמש בפקודה זו.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("שימוש: /event run <שם אירוע> | /event list", NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder();
            for (String key : eventManager.getEventKeys()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(key);
            }
            sender.sendMessage(Component.text("אירועים זמינים: " + sb, NamedTextColor.AQUA));
            return true;
        }

        if (args[0].equalsIgnoreCase("run")) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("שימוש: /event run <שם אירוע>", NamedTextColor.RED));
                return true;
            }
            boolean success = eventManager.triggerEvent(args[1].toLowerCase());
            if (!success) {
                sender.sendMessage(Component.text("אירוע לא נמצא. השתמשו ב-/event list כדי לראות אירועים זמינים.", NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text("האירוע הופעל בהצלחה.", NamedTextColor.GREEN));
            }
            return true;
        }

        sender.sendMessage(Component.text("שימוש: /event run <שם אירוע> | /event list", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.isOp()) {
            return completions;
        }
        if (args.length == 1) {
            completions.add("run");
            completions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("run")) {
            completions.addAll(eventManager.getEventKeys());
        }
        return completions;
    }
}
