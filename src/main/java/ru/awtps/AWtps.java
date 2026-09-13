package ru.awtps;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.awtps.analysis.AttributionManager;
import ru.awtps.defense.LagDefenseEngine;
import ru.awtps.analysis.LagAnalyzer;
import ru.awtps.commands.AdminCommand;
import ru.awtps.commands.CommandManager;
import ru.awtps.commands.TpsCommand;
import ru.awtps.commands.TpsPriorityListener;
import ru.awtps.config.ConfigManager;
import ru.awtps.listeners.BlockListener;
import ru.awtps.logging.LagMachineLogger;
import ru.awtps.tracking.EntityOwnerTracker;
import ru.awtps.listeners.OwnershipListener;
import ru.awtps.mitigation.Mitigator;
import ru.awtps.notify.NotificationService;
import ru.awtps.tracking.ActivityTracker;
import ru.awtps.tracking.PlacementTracker;
import ru.awtps.tracking.ThrottleManager;
import ru.awtps.tracking.TpsMonitor;

public final class AWtps extends JavaPlugin {

    private ConfigManager configManager;
    private ActivityTracker activityTracker;
    private PlacementTracker placementTracker;
    private ThrottleManager throttleManager;
    private NotificationService notificationService;
    private TpsMonitor tpsMonitor;
    private LagAnalyzer lagAnalyzer;
    private BukkitTask protectionTask;
    private LagMachineLogger lagMachineLogger;

    private boolean protectionEnabled = true;

    private LagDefenseEngine lagDefenseEngine;
    private AttributionManager attributionManager;
    private EntityOwnerTracker entityOwnerTracker;

    private void printBanner() {
        String pink = "\u001B[95m";
        String reset = "\u001B[0m";

        String banner = String.join(System.lineSeparator(),
                "█████████████████████████████████████████████████████████████████",
                "██▀▄─██▄─▄─▀█▄─██─▄█░▄▄░▄█░▄▄░▄█▄─█▀▀▀█─▄█─▄▄─█▄─▄▄▀█▄─▄███▄─▄▄▀█",
                "██─▀─███─▄─▀██─██─███▀▄█▀██▀▄█▀██─█─█─█─██─██─██─▄─▄██─██▀██─██─█",
                "▀▄▄▀▄▄▀▄▄▄▄▀▀▀▄▄▄▄▀▀▄▄▄▄▄▀▄▄▄▄▄▀▀▄▄▄▀▄▄▄▀▀▄▄▄▄▀▄▄▀▄▄▀▄▄▄▄▄▀▄▄▄▄▀▀");

        System.out.println(
                pink
                        + banner
                        + System.lineSeparator()
                        + "version 1.0.0 | author AbuzzWorld"
                        + reset
        );
    }

    @Override
    public void onEnable() {
        printBanner();

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.load();

        protectionEnabled = configManager.mitigationEnabled;

        activityTracker = new ActivityTracker();

        placementTracker = new PlacementTracker();
        placementTracker.startPersistence(this);

        throttleManager = new ThrottleManager();

        notificationService = new NotificationService(this);

        attributionManager = new AttributionManager(
                placementTracker,
                configManager
        );

        entityOwnerTracker = new EntityOwnerTracker(this);
        attributionManager.setEntityOwnerTracker(entityOwnerTracker);

        Mitigator mitigator = new Mitigator(
                configManager,
                throttleManager
        );

        lagAnalyzer = new LagAnalyzer(
                this,
                attributionManager,
                mitigator
        );

        lagMachineLogger = new LagMachineLogger(this);
        lagMachineLogger.start();

        tpsMonitor = new TpsMonitor(this);
        tpsMonitor.start();

        lagDefenseEngine = new LagDefenseEngine(this);

        getServer().getPluginManager().registerEvents(
                lagDefenseEngine,
                this
        );

        lagDefenseEngine.start();

        new BukkitRunnable() {
            @Override
            public void run() {
                activityTracker.resetWindow();
            }
        }.runTaskTimer(
                this,
                configManager.activityWindowSeconds * 20L,
                configManager.activityWindowSeconds * 20L
        );

        getServer().getPluginManager().registerEvents(
                new BlockListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new OwnershipListener(this),
                this
        );

        TpsCommand tps = new TpsCommand(this);

        getServer().getPluginManager().registerEvents(
                new TpsPriorityListener(this, tps),
                this
        );

        CommandManager.registerPriorityCommands(
                this,
                tps,
                new AdminCommand(this, tps)
        );

        restartProtectionTask();

        getLogger().info(
                "AWtps включён. TPS-команда имеет приоритет. Защита лаг-машин: "
                        + (protectionEnabled ? "ВКЛ" : "ВЫКЛ")
        );
    }

    public void restartProtectionTask() {
        if (lagDefenseEngine != null) {
            lagDefenseEngine.start();
        }
    }

    public void runProtectionScan(boolean forced) {
        if (!protectionEnabled) {
            return;
        }

        double tps = tpsMonitor.current1mTps();

        if (!forced && tps >= configManager.protectionStartTps) {
            return;
        }

        if (lagDefenseEngine != null) {
            lagDefenseEngine.forceScan();
        }
    }

    public void setProtectionEnabled(boolean enabled) {
        protectionEnabled = enabled;

        if (enabled) {
            restartProtectionTask();
        } else {
            if (protectionTask != null) {
                protectionTask.cancel();
            }

            protectionTask = null;

            if (throttleManager != null) {
                throttleManager.clearAll();
            }

            if (lagDefenseEngine != null) {
                lagDefenseEngine.clear();
            }
        }
    }

    public boolean isProtectionEnabled() {
        return protectionEnabled;
    }

    @Override
    public void onDisable() {
        if (protectionTask != null) {
            protectionTask.cancel();
        }

        if (tpsMonitor != null) {
            tpsMonitor.stop();
        }

        if (placementTracker != null) {
            placementTracker.stopPersistence();
        }

        if (lagDefenseEngine != null) {
            lagDefenseEngine.stop();
        }

        if (throttleManager != null) {
            throttleManager.clearAll();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ActivityTracker getActivityTracker() {
        return activityTracker;
    }

    public PlacementTracker getPlacementTracker() {
        return placementTracker;
    }

    public ThrottleManager getThrottleManager() {
        return throttleManager;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public TpsMonitor getTpsMonitor() {
        return tpsMonitor;
    }

    public LagAnalyzer getLagAnalyzer() {
        return lagAnalyzer;
    }

    public LagDefenseEngine getLagDefenseEngine() {
        return lagDefenseEngine;
    }

    public AttributionManager getAttributionManager() {
        return attributionManager;
    }

    public EntityOwnerTracker getEntityOwnerTracker() {
        return entityOwnerTracker;
    }

    public LagMachineLogger getLagMachineLogger() {
        return lagMachineLogger;
    }
}