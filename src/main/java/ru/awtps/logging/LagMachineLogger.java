package ru.awtps.logging;

import org.bukkit.plugin.java.JavaPlugin;
import ru.awtps.defense.ChunkThreat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public final class LagMachineLogger {
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US)
                    .withZone(ZoneId.systemDefault());

    private final JavaPlugin plugin;
    private final Path file;

    public LagMachineLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = plugin.getDataFolder().toPath().resolve("logs").resolve("lag-machines.log");
    }

    
    public void start() {
        try {
            Files.createDirectories(file.getParent());
            if (Files.notExists(file)) Files.createFile(file);
        } catch (IOException ignored) { }
    }

    
    public synchronized void log(ChunkThreat t, double tpsBeforeCleanup,
                                  double msptBeforeCleanup, String protectionTypes,
                                  int mitigated, int removedEntities, boolean hard, int structureScore) {
        String location = t.key.world() + "/" + t.lastX + "," + t.lastY + "," + t.lastZ;
        String line = String.format(Locale.US,
                "[%s] LAG_MACHINE | machine=%s | location=%s | tps=%.2f | mspt=%.2f | " +
                "score=%d | structureScore=%d | redstone=%d | piston=%d | hopper=%d | dispenser=%d | " +
                "minecart=%d | boat=%d | mobs=%d | items=%d | lever=%d | protections=%s | hard=%s | mitigated=%d | removedEntities=%d",
                TIME.format(Instant.now()), safe(protectionTypes), safe(location),
                tpsBeforeCleanup, msptBeforeCleanup, t.activityScore(), structureScore,
                t.redstone, t.piston, t.hopper, t.dispenser, t.minecart, t.boat,
                t.mobSpawn, t.itemSpawn, t.lever, safe(protectionTypes), hard, mitigated, removedEntities);
        append(line);
    }

    
    public synchronized void logTps(String event, double tps, double mspt) {
        String line = String.format(Locale.US,
                "[%s] %s | tps=%.2f | mspt=%.2f",
                TIME.format(Instant.now()), safe(event), tps, mspt);
        append(line);
    }

    
    public synchronized void logEmergency(double tps, double mspt, int removed) {
        String line = String.format(Locale.US,
                "[%s] EMERGENCY | tps=%.2f | mspt=%.2f | removed=%d",
                TIME.format(Instant.now()), tps, mspt, removed);
        append(line);
    }

    private void append(String line) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                out.write(line);
                out.newLine();
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Не удалось записать lag-machines.log: " + ex.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "UNKNOWN" : value.replace('|', '/');
    }

    public Path file() {
        return file;
    }
}
