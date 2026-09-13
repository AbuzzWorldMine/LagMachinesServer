package ru.awtps.tracking;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;
import ru.awtps.notify.NotificationService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;


public class TpsMonitor {

    private final AWtps plugin;
    private final Deque<Double> tpsHistory = new ArrayDeque<>();
    private final Deque<TpsSample> samples = new ArrayDeque<>();
    private long lastSampleNanos = 0L;
    private double lastMspt = -1.0;

    private int consecutiveBelowMin = 0;
    private long lastAnalysisMillis = 0L;
    private long lastWarnNotifyMillis = 0L;
    private long lastCriticalNotifyMillis = 0L;
    private boolean previouslyBelowWarn = false;
    private boolean previouslyCritical = false;

    private BukkitTask task;

    public TpsMonitor(AWtps plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                sample();
            }
        }.runTaskTimer(plugin, cfg().sampleIntervalTicks, cfg().sampleIntervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sample() {
        long nowNanos = System.nanoTime();
        if (lastSampleNanos == 0L) {
            lastSampleNanos = nowNanos;
        } else {
            double elapsedSeconds = (nowNanos - lastSampleNanos) / 1_000_000_000.0;
            int ticks = Math.max(1, cfg().sampleIntervalTicks);
            double intervalTps = elapsedSeconds > 0.0 ? Math.min(20.0, ticks / elapsedSeconds) : 20.0;
            lastMspt = intervalTps > 0.0 ? 1000.0 / intervalTps : -1.0;
            samples.addLast(new TpsSample(nowNanos, intervalTps));
            while (!samples.isEmpty() && nowNanos - samples.peekFirst().timeNanos > 15L * 60L * 1_000_000_000L) {
                samples.removeFirst();
            }
            lastSampleNanos = nowNanos;
        }

        double currentTps = current1mTps();

        tpsHistory.addLast(currentTps);
        while (tpsHistory.size() > cfg().historySize) {
            tpsHistory.removeFirst();
        }

        NotificationService notify = plugin.getNotificationService();
        long now = System.currentTimeMillis();

        boolean criticalNow = currentTps < cfg().criticalThreshold;
        boolean warnNow = currentTps < cfg().warnThreshold;

        if (criticalNow && !previouslyCritical) {
            plugin.getLagMachineLogger().logTps("TPS_CRITICAL_START", currentTps, lastMspt);
        } else if (!criticalNow && previouslyCritical) {
            plugin.getLagMachineLogger().logTps("TPS_CRITICAL_RECOVERED", currentTps, lastMspt);
        } else if (warnNow && !previouslyBelowWarn) {
            plugin.getLagMachineLogger().logTps("TPS_WARNING_START", currentTps, lastMspt);
        } else if (!warnNow && previouslyBelowWarn) {
            plugin.getLagMachineLogger().logTps("TPS_WARNING_RECOVERED", currentTps, lastMspt);
        }
        previouslyCritical = criticalNow;
        previouslyBelowWarn = warnNow;

        if (currentTps < cfg().minThreshold) {
            consecutiveBelowMin++;
        } else {
            consecutiveBelowMin = 0;
        }

        if (currentTps < cfg().criticalThreshold && now - lastCriticalNotifyMillis > 10_000) {
            lastCriticalNotifyMillis = now;
            if (cfg().notifyEnabled) {
                notify.broadcastToStaff(notify.format(cfg().messageCritical, "%tps%", formatTps(currentTps)));
            }
        } else if (currentTps < cfg().warnThreshold && now - lastWarnNotifyMillis > 20_000) {
            lastWarnNotifyMillis = now;
            if (cfg().notifyEnabled) {
                notify.broadcastToStaff(notify.format(cfg().messageWarn, "%tps%", formatTps(currentTps)));
            }
        }

        
        
        if (consecutiveBelowMin >= cfg().consecutiveChecksBeforeAction) {
            consecutiveBelowMin = 0;
        }
    }

    
    public synchronized double current1mTps() {
        return windowTps(60L);
    }

    public synchronized double[] allTps() {
        return new double[]{windowTps(60L), windowTps(300L), windowTps(900L)};
    }

    public synchronized double averageTickTimeMillis() {
        return lastMspt;
    }

    private double windowTps(long seconds) {
        if (samples.isEmpty()) return 20.0;

        long now = System.nanoTime();
        long cutoff = now - seconds * 1_000_000_000L;
        double totalTps = 0.0;
        int count = 0;
        for (Iterator<TpsSample> it = samples.descendingIterator(); it.hasNext();) {
            TpsSample sample = it.next();
            if (sample.timeNanos < cutoff) break;
            totalTps += sample.tps;
            count++;
        }
        if (count == 0) return samples.peekLast().tps;
        return Math.max(0.0, Math.min(20.0, totalTps / count));
    }

    private record TpsSample(long timeNanos, double tps) {}

    public static String formatTps(double tps) {
        return String.format("%.2f", tps);
    }
}
