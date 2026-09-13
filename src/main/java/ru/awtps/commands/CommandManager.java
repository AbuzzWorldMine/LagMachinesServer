package ru.awtps.commands;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandManager {
    private CommandManager() {}

    public static void registerPriorityCommands(JavaPlugin plugin, TpsCommand tps, AdminCommand admin) {
        PluginCommand adminCommand = plugin.getCommand("awtps");
        if (adminCommand != null) {
            adminCommand.setExecutor(admin);
            adminCommand.setTabCompleter(admin);
        }
        plugin.getLogger().info("AWtps: /tps перехватывается с HIGHEST priority; /minecraft:tps остаётся системным.");
    }
}