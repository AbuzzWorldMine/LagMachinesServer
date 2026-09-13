package ru.awtps.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;
import ru.awtps.model.LagReportEntry;
import ru.awtps.tracking.TpsMonitor;

import java.util.ArrayList;
import java.util.List;

public final class TpsCommand implements TabExecutor {
    private final AWtps plugin;

    public TpsCommand(AWtps plugin) { this.plugin = plugin; }

    private ConfigManager cfg() { return plugin.getConfigManager(); }

    private void send(CommandSender sender, String raw) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', cfg().prefix + raw));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("awtps.tps")) {
            send(sender, cfg().noPermission);
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "report" -> { sendReport(sender); return true; }
                case "help" -> {
                    send(sender, "&b/tps &7— собственный TPS AWtps");
                    send(sender, "&b/tps report &7— последний отчёт защиты");
                    return true;
                }
                default -> { send(sender, "&cНеизвестная подкоманда. &7/tps help"); return true; }
            }
        }

        TpsMonitor monitor = plugin.getTpsMonitor();
        double[] tps = monitor.allTps();
        send(sender, cfg().tpsHeader);
        String[] ranges = {"1m", "5m", "15m"};
        for (int i = 0; i < Math.min(tps.length, ranges.length); i++) {
            send(sender, cfg().tpsLine
                    .replace("%range%", ranges[i])
                    .replace("%tps%", TpsMonitor.formatTps(Math.min(20.0, tps[i]))));
        }

        double mspt = monitor.averageTickTimeMillis();
        if (mspt >= 0) {
            send(sender, cfg().msptLine.replace("%mspt%", String.format("%.2f", mspt)));
        }

        send(sender, "&7Защита: " + (plugin.isProtectionEnabled() ? "&aВКЛ" : "&cВЫКЛ")
                + " &8| &7Карантинов: &f" + plugin.getLagDefenseEngine().protectedChunks()
                + " &8| &7Ограничений: &f" + plugin.getLagDefenseEngine().activeRestrictions());
        send(sender, "&7Отслеживается чанков: &f" + plugin.getLagDefenseEngine().trackedChunks());
        return true;
    }

    private void sendReport(CommandSender sender) {
        send(sender, cfg().reportHeader);
        List<LagReportEntry> report = plugin.getLagAnalyzer().lastReport();
        if (report.isEmpty()) {
            send(sender, cfg().reportEmpty);
            return;
        }
        for (LagReportEntry entry : report) {
            String causeDisplay = cfg().groupDisplayNames.getOrDefault(entry.cause(), entry.cause().name());
            send(sender, cfg().reportLine
                    .replace("%location%", entry.chunk().world() + "/" + entry.x() + "," + entry.y() + "," + entry.z())
                    .replace("%world%", entry.chunk().world())
                    .replace("%cause%", causeDisplay)
                    );
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            for (String s : List.of("report", "help")) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }
        return List.of();
    }
}
