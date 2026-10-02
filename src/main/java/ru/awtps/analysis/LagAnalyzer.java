package ru.awtps.analysis;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;
import ru.awtps.mitigation.Mitigator;
import ru.awtps.model.ChunkKey;
import ru.awtps.model.ChunkMetrics;
import ru.awtps.model.LagCause;
import ru.awtps.model.LagReportEntry;
import ru.awtps.notify.NotificationService;
import ru.awtps.tracking.TpsMonitor;

import java.util.ArrayList;
import java.util.List;


public class LagAnalyzer {

    private static final int TOP_CHUNKS_TO_REPORT = 5;
    private static final int MAX_CHUNKS_TO_MITIGATE = 10;

    private final AWtps plugin;
    private final AttributionManager attributionManager;
    private final Mitigator mitigator;

    private List<LagReportEntry> lastReport = new ArrayList<>();

    public LagAnalyzer(AWtps plugin, AttributionManager attributionManager, Mitigator mitigator) {
        this.plugin = plugin;
        this.attributionManager = attributionManager;
        this.mitigator = mitigator;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public List<LagReportEntry> lastReport() {
        return lastReport;
    }

    public void analyzeAndMitigate(double tpsAtTrigger) {
        
        
        
        List<ScoredChunk> scored = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                ChunkMetrics metrics = ChunkScanner.scan(chunk, plugin.getActivityTracker());
                double score = metrics.weightedScore(
                        cfg().weightItems, cfg().weightMobs, cfg().weightHopperTransfers,
                        cfg().weightRedstoneUpdates, cfg().weightTileEntities, cfg().weightBlockEntitiesTotal);
                if (score <= 0) continue;
                scored.add(new ScoredChunk(chunk, metrics, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<LagReportEntry> report = new ArrayList<>();
        NotificationService notify = plugin.getNotificationService();
        long now = System.currentTimeMillis();

        int limit = Math.min(MAX_CHUNKS_TO_MITIGATE, scored.size());
        for (int i = 0; i < limit; i++) {
            ScoredChunk sc = scored.get(i);
            LagCause cause = determineCause(sc.metrics);
            ChunkKey key = ChunkKey.of(sc.chunk);

            int mitigatedCount = mitigator.mitigate(sc.chunk, cause, sc.metrics);

            LagReportEntry entry = new LagReportEntry(key, cause, sc.score, mitigatedCount, now, sc.chunk.getX() * 16, sc.chunk.getWorld().getMinHeight(), sc.chunk.getZ() * 16);
            if (report.size() < TOP_CHUNKS_TO_REPORT) report.add(entry);

            if (cfg().notifyEnabled) {
                String causeDisplay = cfg().groupDisplayNames.getOrDefault(cause, cause.name());
                String message = notify.format(cfg().messageAction,
                        "%location%", key.toString(),
                        "%cause%", causeDisplay,
                        "%count%", String.valueOf(mitigatedCount));
                notify.broadcastToStaff(message);
            }
        }

        lastReport = report;

        plugin.getLogger().info(String.format(
                "Анализ лага запущен при TPS=%.2f. Найдено нагруженных чанков: %d, в отчёт вошло: %d.",
                tpsAtTrigger, scored.size(), report.size()));
    }

    private LagCause determineCause(ChunkMetrics m) {
        ConfigManager c = cfg();

        double mechanismScore = m.redstoneUpdatesInWindow() * c.weightRedstoneUpdates;
        double transportScore = m.hopperTransfersInWindow() * c.weightHopperTransfers;
        double mobScore = m.mobEntities() * c.weightMobs;
        double itemScore = m.itemEntities() * c.weightItems;

        
        
        
        boolean overMobs = m.mobEntities() > c.maxMobsPerChunk;
        boolean overItems = m.itemEntities() > c.maxItemsPerChunk;
        boolean overHoppers = m.hopperTransfersInWindow() > c.maxHopperTransfersPerWindow;
        boolean overRedstone = m.redstoneUpdatesInWindow() > c.maxRedstoneUpdatesPerWindow;

        if (overRedstone && mechanismScore >= transportScore) return LagCause.MECHANISMS;
        if (overHoppers) return LagCause.ITEM_TRANSPORT;
        if (overMobs) return LagCause.MOBS;
        if (overItems) return LagCause.ITEMS;

        double max = Math.max(Math.max(mechanismScore, transportScore), Math.max(mobScore, itemScore));
        if (max <= 0) return LagCause.UNKNOWN;
        if (max == mechanismScore) return LagCause.MECHANISMS;
        if (max == transportScore) return LagCause.ITEM_TRANSPORT;
        if (max == mobScore) return LagCause.MOBS;
        return LagCause.ITEMS;
    }

    private record ScoredChunk(Chunk chunk, ChunkMetrics metrics, double score) {
    }
}
