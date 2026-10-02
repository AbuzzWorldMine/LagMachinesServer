package ru.awtps.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import ru.awtps.AWtps;

import java.util.Locale;

public final class AwtpsPlaceholders extends PlaceholderExpansion {

    private final AWtps plugin;

    public AwtpsPlaceholders(AWtps plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "awtps";
    }

    @Override
    public String getAuthor() {
        return "AbuzzWorld";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        double[] tps = plugin.getTpsMonitor().allTps();

        return switch (key) {
            case "tps", "tps_1m" -> format(tps[0]);
            case "tps_5m" -> format(tps[1]);
            case "tps_15m" -> format(tps[2]);
            case "mspt" -> String.format(Locale.US, "%.2f", plugin.getTpsMonitor().averageTickTimeMillis());
            case "protection" -> plugin.isProtectionEnabled() ? "on" : "off";
            case "protected_chunks" -> String.valueOf(plugin.getLagDefenseEngine().protectedChunks());
            case "tracked_chunks" -> String.valueOf(plugin.getLagDefenseEngine().trackedChunks());
            case "active_restrictions" -> String.valueOf(plugin.getLagDefenseEngine().activeRestrictions());
            default -> null;
        };
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.2f", Math.max(0.0, Math.min(20.0, value)));
    }
}
