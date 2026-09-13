package ru.awtps.notify;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;


public class NotificationService {

    private final AWtps plugin;

    public NotificationService(AWtps plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public void broadcastToStaff(String rawMessage) {
        String message = ChatColor.translateAlternateColorCodes('&', cfg().prefix + rawMessage);
        String permission = cfg().notifyPermission;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(message);
            }
        }
        if (cfg().notifyAlsoConsole) {
            CommandSender console = Bukkit.getConsoleSender();
            console.sendMessage(message);
        }
    }

    public String format(String template, String... placeholdersAndValues) {
        String result = template;
        for (int i = 0; i + 1 < placeholdersAndValues.length; i += 2) {
            result = result.replace(placeholdersAndValues[i], placeholdersAndValues[i + 1]);
        }
        return result;
    }
}
