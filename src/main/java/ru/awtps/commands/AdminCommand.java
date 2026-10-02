package ru.awtps.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import ru.awtps.AWtps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AdminCommand implements TabExecutor {
    private final AWtps plugin;
    private final TpsCommand tpsCommand;

    public AdminCommand(AWtps plugin, TpsCommand tpsCommand) {
        this.plugin = plugin;
        this.tpsCommand = tpsCommand;
    }

    private void send(CommandSender sender, String raw) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().prefix + raw));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("awtps.admin")) {
            send(sender, plugin.getConfigManager().noPermission);
            return true;
        }
        if (args.length == 0) {
            send(sender, "&7Использование: /awtps <reload|report|unthrottle|status|scan|protect|top>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().load();
                plugin.restartProtectionTask();
                send(sender, "&aКонфигурация AWtps перезагружена.");
            }
            case "report" -> tpsCommand.onCommand(sender, command, label, new String[]{"report"});
            case "unthrottle" -> {
                plugin.getThrottleManager().clearAll();
                plugin.getLagDefenseEngine().clear();
                send(sender, "&aВсе временные ограничения и карантины сняты.");
            }
            case "status" -> {
                send(sender, "&7TPS: &f" + TpsCommandUtil.tps(plugin));
                send(sender, "&7Защита: " + (plugin.isProtectionEnabled() ? "&aВКЛ" : "&cВЫКЛ"));
                send(sender, "&7Карантинов: &f" + plugin.getLagDefenseEngine().protectedChunks());
                send(sender, "&7Активных блокировок: &f" + plugin.getLagDefenseEngine().activeRestrictions());
                send(sender, "&7Отслеживается чанков: &f" + plugin.getLagDefenseEngine().trackedChunks());
            }
            case "scan" -> {
                plugin.runProtectionScan(true);
                send(sender, "&aПринудительное сканирование запущено.");
            }
            case "protect" -> {
                if (args.length < 2) {
                    send(sender, "&7Использование: /awtps protect <on|off>");
                    return true;
                }
                boolean enabled = args[1].equalsIgnoreCase("on");
                plugin.setProtectionEnabled(enabled);
                send(sender, enabled ? "&aЗащита включена." : "&cЗащита выключена.");
            }
            case "top" -> {
                int limit = 5;
                if (args.length >= 2) {
                    try {
                        limit = Math.max(1, Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {
                        
                    }
                }
                List<Map.Entry<String, Long>> top = plugin.getAttributionManager().topOffenders(limit);
                if (top.isEmpty()) {
                    send(sender, "&7Недостаточно данных для составления рейтинга.");
                } else {
                    send(sender, "&eТоп игроков по нагрузке на сервер:");
                    int place = 1;
                    for (Map.Entry<String, Long> entry : top) {
                        send(sender, "&7" + place + ". &f" + entry.getKey() + " &8— &6" + entry.getValue() + " &7очков нагрузки");
                        place++;
                    }
                }
            }
            case "help" -> {
                send(sender, "&b/awtps reload &7— перезагрузить конфиг");
                send(sender, "&b/awtps report &7— отчёт");
                send(sender, "&b/awtps unthrottle &7— снять ограничения");
                send(sender, "&b/awtps status &7— состояние защиты");
                send(sender, "&b/awtps scan &7— запустить проверку");
                send(sender, "&b/awtps protect <on|off> &7— защита");
                send(sender, "&b/awtps top [n] &7— топ игроков по нагрузке");
            }
            default -> send(sender, "&cНеизвестная команда. &7/awtps help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("awtps.admin")) return List.of();
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String s : List.of("reload","report","unthrottle","status","scan","protect","top","help")) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("protect")) {
            return List.of("on", "off").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
