package ru.awtps.integration;

import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public final class DiscordNotifier {

    private final AWtps plugin;

    public DiscordNotifier(AWtps plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public void sendIfEnabled(String message, boolean isCritical) {
        if (!cfg().discordEnabled) return;
        if (cfg().discordOnlyCritical && !isCritical) return;
        send(message);
    }

    private void send(String content) {
        String url = cfg().discordWebhookUrl;
        if (url == null || url.isBlank()) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String json = "{\"username\":\"" + escape(cfg().discordUsername)
                        + "\",\"content\":\"" + escape(content) + "\"}";

                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoOutput(true);

                try (OutputStream out = connection.getOutputStream()) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 300) {
                    plugin.getLogger().warning("Discord webhook вернул код ответа " + responseCode);
                }
                connection.disconnect();
            } catch (IOException ex) {
                plugin.getLogger().warning("Не удалось отправить сообщение в Discord: " + ex.getMessage());
            }
        });
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
