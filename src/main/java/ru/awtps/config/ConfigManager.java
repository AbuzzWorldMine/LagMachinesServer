package ru.awtps.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import ru.awtps.AWtps;
import ru.awtps.model.LagCause;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


public class ConfigManager {

    private final AWtps plugin;

    
    public int sampleIntervalTicks;
    public int historySize;
    public int protectionScanIntervalSeconds;
    public double protectionStartTps;

    
    public double warnThreshold;
    public double criticalThreshold;
    public double minThreshold;
    public int consecutiveChecksBeforeAction;
    public long reanalyzeCooldownMillis;

    
    public double weightItems;
    public double weightMobs;
    public double weightHopperTransfers;
    public double weightRedstoneUpdates;
    public double weightTileEntities;
    public double weightBlockEntitiesTotal;

    
    public int maxItemsPerChunk;
    public int maxMobsPerChunk;
    public int maxHopperTransfersPerWindow;
    public int maxRedstoneUpdatesPerWindow;
    public int activityWindowSeconds;

    
    public boolean mitigationEnabled;
    public boolean mitigateItems;
    public boolean mitigateMobs;
    public boolean mobsIgnoreNamed;
    public boolean mobsIgnoreTamed;
    public boolean mitigateRedstone;
    public long redstoneThrottleCooldownMillis;
    public boolean mitigateHoppers;
    public long hopperThrottleCooldownMillis;

    
    public final Map<LagCause, String> groupDisplayNames = new EnumMap<>(LagCause.class);
    public final Set<Material> mechanismMaterials = EnumSet.noneOf(Material.class);
    public final Set<Material> itemTransportMaterials = EnumSet.noneOf(Material.class);
    public final Set<Material> ownerTrackedMaterials = EnumSet.noneOf(Material.class);


    
    public double emergencyTps;
    public int machineRedstoneUpdates, machinePistonEvents, machineHopperTransfers, machineDispenses, structureScore;
    public int machineMinecarts, machineBoats, machineLeverClicks, machineScore, hardMachineScore;
    public int mobSpawnBurst, itemSpawnBurst;
    public int quarantineSeconds, hardQuarantineSeconds, actionCooldownSeconds;
    public int safeItemsPerChunk, safeMobsPerChunk, safeMinecartsPerChunk, safeBoatsPerChunk;
    public int hardItemsPerChunk, hardMobsPerChunk, hardMinecartsPerChunk, hardBoatsPerChunk;

    
    public boolean particlesEnabled;
    public Particle particleType;
    public int particleIntervalTicks;
    public double particleRadius;
    public double particleHeight;
    public int particlePoints;
    public double particleSpeed;
    public int particleCount;

    
    public boolean ownerTrackingEnabled;
    public boolean ownerTrackingMechanisms;
    public String ownerTrackingUnknownName;

    
    public boolean notifyEnabled;
    public String notifyPermission;
    public boolean notifyAlsoConsole;
    public String messageWarn;
    public String messageCritical;
    public String messageAction;

    
    public String prefix;
    public String tpsHeader;
    public String tpsLine;
    public String msptLine;
    public String reportHeader;
    public String reportEmpty;
    public String reportLine;
    public String reloadSuccess;
    public String noPermission;

    public ConfigManager(AWtps plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        sampleIntervalTicks = c.getInt("monitor.sample-interval-ticks", 20);
        historySize = c.getInt("monitor.history-size", 60);
        protectionScanIntervalSeconds = Math.max(1, c.getInt("monitor.protection-scan-interval-seconds", 5));
        protectionStartTps = c.getDouble("monitor.protection-start-tps", 18.0);

        warnThreshold = c.getDouble("tps.warn-threshold", 18.0);
        criticalThreshold = c.getDouble("tps.critical-threshold", 15.0);
        minThreshold = c.getDouble("tps.min-threshold", 10.0);
        consecutiveChecksBeforeAction = c.getInt("tps.consecutive-checks-before-action", 3);
        reanalyzeCooldownMillis = c.getLong("tps.reanalyze-cooldown-seconds", 30) * 1000L;

        weightItems = c.getDouble("weights.items-on-ground", 1.0);
        weightMobs = c.getDouble("weights.mobs", 2.5);
        weightHopperTransfers = c.getDouble("weights.hopper-transfers", 1.5);
        weightRedstoneUpdates = c.getDouble("weights.redstone-updates", 0.4);
        weightTileEntities = c.getDouble("weights.tile-entities", 1.0);
        weightBlockEntitiesTotal = c.getDouble("weights.block-entities-total", 0.2);

        maxItemsPerChunk = c.getInt("limits.max-items-per-chunk", 40);
        maxMobsPerChunk = c.getInt("limits.max-mobs-per-chunk", 30);
        maxHopperTransfersPerWindow = c.getInt("limits.max-hopper-transfers-per-window", 400);
        maxRedstoneUpdatesPerWindow = c.getInt("limits.max-redstone-updates-per-window", 600);
        activityWindowSeconds = c.getInt("limits.activity-window-seconds", 10);

        emergencyTps = c.getDouble("defense.emergency-tps", 12.0);
        machineRedstoneUpdates = c.getInt("defense.thresholds.redstone-updates-per-window", 160);
        machinePistonEvents = c.getInt("defense.thresholds.piston-events-per-window", 28);
        machineHopperTransfers = c.getInt("defense.thresholds.hopper-transfers-per-window", 180);
        machineDispenses = c.getInt("defense.thresholds.dispenses-per-window", 80);
        structureScore = c.getInt("defense.thresholds.structure-score", 240);
        machineMinecarts = c.getInt("defense.thresholds.minecarts-per-window", 12);
        machineBoats = c.getInt("defense.thresholds.boats-per-window", 12);
        machineLeverClicks = c.getInt("defense.thresholds.lever-clicks-per-window", 45);
        machineScore = c.getInt("defense.thresholds.machine-score", 160);
        hardMachineScore = c.getInt("defense.thresholds.hard-machine-score", 420);
        mobSpawnBurst = c.getInt("defense.thresholds.mob-spawn-burst", 18);
        itemSpawnBurst = c.getInt("defense.thresholds.item-spawn-burst", 45);
        quarantineSeconds = c.getInt("defense.quarantine-seconds", 10);
        hardQuarantineSeconds = c.getInt("defense.hard-quarantine-seconds", 20);
        actionCooldownSeconds = c.getInt("defense.action-cooldown-seconds", 4);
        safeItemsPerChunk = c.getInt("defense.cleanup.safe-items-per-chunk", 48);
        safeMobsPerChunk = c.getInt("defense.cleanup.safe-mobs-per-chunk", 35);
        safeMinecartsPerChunk = c.getInt("defense.cleanup.safe-minecarts-per-chunk", 8);
        safeBoatsPerChunk = c.getInt("defense.cleanup.safe-boats-per-chunk", 8);
        hardItemsPerChunk = c.getInt("defense.cleanup.hard-items-per-chunk", 18);
        hardMobsPerChunk = c.getInt("defense.cleanup.hard-mobs-per-chunk", 20);
        hardMinecartsPerChunk = c.getInt("defense.cleanup.hard-minecarts-per-chunk", 3);
        hardBoatsPerChunk = c.getInt("defense.cleanup.hard-boats-per-chunk", 3);

        particlesEnabled = c.getBoolean("particles.enabled", true);
        particleIntervalTicks = Math.max(1, c.getInt("particles.interval-ticks", 5));
        particleRadius = Math.max(0.5, c.getDouble("particles.radius", 3.0));
        particleHeight = Math.max(0.5, c.getDouble("particles.height", 2.5));
        particlePoints = Math.max(4, c.getInt("particles.points", 24));
        particleSpeed = Math.max(0.0, c.getDouble("particles.speed", 0.01));
        particleCount = Math.max(1, c.getInt("particles.count", 1));
        String particleName = c.getString("particles.type", "FLAME");
        try {
            particleType = Particle.valueOf(particleName.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            particleType = Particle.FLAME;
            plugin.getLogger().warning("Неизвестная частица particles.type: " + particleName + ". Используется FLAME.");
        }

        mitigationEnabled = c.getBoolean("mitigation.enabled", true);
        mitigateItems = c.getBoolean("mitigation.actions.items.enabled", true);
        mitigateMobs = c.getBoolean("mitigation.actions.mobs.enabled", true);
        mobsIgnoreNamed = c.getBoolean("mitigation.actions.mobs.ignore-named", true);
        mobsIgnoreTamed = c.getBoolean("mitigation.actions.mobs.ignore-tamed", true);
        mitigateRedstone = c.getBoolean("mitigation.actions.redstone-throttle.enabled", true);
        redstoneThrottleCooldownMillis = c.getLong("mitigation.actions.redstone-throttle.cooldown-seconds", 60) * 1000L;
        mitigateHoppers = c.getBoolean("mitigation.actions.hopper-throttle.enabled", true);
        hopperThrottleCooldownMillis = c.getLong("mitigation.actions.hopper-throttle.cooldown-seconds", 60) * 1000L;

        groupDisplayNames.clear();
        for (LagCause cause : LagCause.values()) {
            String display = c.getString("groups." + cause.name(), cause.name());
            groupDisplayNames.put(cause, display);
        }

        mechanismMaterials.clear();
        for (String name : c.getStringList("group-materials.MECHANISMS")) {
            Material m = Material.matchMaterial(name);
            if (m != null) mechanismMaterials.add(m);
        }
        itemTransportMaterials.clear();
        for (String name : c.getStringList("group-materials.ITEM_TRANSPORT")) {
            Material m = Material.matchMaterial(name);
            if (m != null) itemTransportMaterials.add(m);
        }
        ownerTrackedMaterials.clear();
        ownerTrackedMaterials.addAll(mechanismMaterials);
        ownerTrackedMaterials.addAll(itemTransportMaterials);
        for (String name : c.getStringList("owner-tracking.extra-materials")) {
            Material m = Material.matchMaterial(name);
            if (m != null) ownerTrackedMaterials.add(m);
        }

        notifyEnabled = c.getBoolean("notify.enabled", true);
        notifyPermission = c.getString("notify.permission", "awtps.notify");
        notifyAlsoConsole = c.getBoolean("notify.also-log-to-console", true);
        messageWarn = c.getString("notify.message-warn", "&e⚠ TPS warning: %tps%");
        messageCritical = c.getString("notify.message-critical", "&c⚠ TPS critical: %tps%");
        messageAction = c.getString("notify.message-action", "&cИсточник лага: &f%location% &8| &fПричина: &6%cause% &8| &fУстранено: &a%count%");
        if (messageAction.contains("%player%") || messageAction.toLowerCase(java.util.Locale.ROOT).contains("ответственн")) {
            messageAction = "&cИсточник лага: &f%location% &8| &fПричина: &6%cause% &8| &fУстранено: &a%count%";
        }

        prefix = c.getString("messages.prefix", "&8[&6LagMachinesServer&8] &r");
        tpsHeader = c.getString("messages.tps-header", "&aServer TPS:");
        tpsLine = c.getString("messages.tps-line", "&7%range%: &f%tps%");
        msptLine = c.getString("messages.mspt-line", "&7MSPT: &f%mspt% ms");
        reportHeader = c.getString("messages.report-header", "&eLast lag report:");
        reportEmpty = c.getString("messages.report-empty", "&7No issues detected.");
        reportLine = c.getString("messages.report-line", "&7- &f%location% &7— &6%cause%");
        if (reportLine.contains("%player%") || reportLine.toLowerCase(java.util.Locale.ROOT).contains("ответственн")) {
            reportLine = "&7- &f%location% &7— &6%cause%";
        }
        reloadSuccess = c.getString("messages.reload-success", "&aReloaded.");
        noPermission = c.getString("messages.no-permission", "&cNo permission.");
    }
}
