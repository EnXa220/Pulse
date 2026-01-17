package com.azk.pulse.core;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordWebhook {
    private DiscordWebhook() {
    }

    public static void send(JavaPlugin plugin, String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isBlank() || content == null || content.isBlank()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                String payload = "{\"content\":\"" + escape(content) + "\"}";
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);

                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(data);
                }

                connection.getInputStream().close();
            } catch (Exception ignored) {
            }
        });
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
