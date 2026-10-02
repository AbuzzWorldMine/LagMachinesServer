package ru.awtps.commands;

import ru.awtps.AWtps;
import ru.awtps.tracking.TpsMonitor;

public final class TpsCommandUtil {
    private TpsCommandUtil() {}
    public static String tps(AWtps plugin) {
        return TpsMonitor.formatTps(plugin.getTpsMonitor().current1mTps());
    }
}
