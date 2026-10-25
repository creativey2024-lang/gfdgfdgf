package com.example.timerplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartCommand implements CommandExecutor {

    private final TimerPlugin plugin;

    public StartCommand(TimerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("אין לך הרשאה להשתמש בפקודה זו.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
            plugin.stopMatch();
            sender.sendMessage(Component.text("המשחק נעצר - הטיימר והאירועים בוטלו.", NamedTextColor.YELLOW));
            return true;
        }

        plugin.startMatch();
        sender.sendMessage(Component.text(
                "המשחק התחיל! טיימר לשעה + אירועים אקראיים כל 10 דקות הופעלו.",
                NamedTextColor.GREEN));
        return true;
    }
}

