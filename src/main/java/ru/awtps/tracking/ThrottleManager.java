package ru.awtps.tracking;

import ru.awtps.model.ChunkKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ThrottleManager {

    private final Map<ChunkKey, Long> redstoneThrottledUntil = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Long> hopperThrottledUntil = new ConcurrentHashMap<>();

    public void throttleRedstone(ChunkKey chunk, long cooldownMillis) {
        redstoneThrottledUntil.put(chunk, System.currentTimeMillis() + cooldownMillis);
    }

    public void throttleHoppers(ChunkKey chunk, long cooldownMillis) {
        hopperThrottledUntil.put(chunk, System.currentTimeMillis() + cooldownMillis);
    }

    public boolean isRedstoneThrottled(ChunkKey chunk) {
        return isStillActive(redstoneThrottledUntil, chunk);
    }

    public boolean isHopperThrottled(ChunkKey chunk) {
        return isStillActive(hopperThrottledUntil, chunk);
    }

    public void clearRedstoneThrottle(ChunkKey chunk) {
        redstoneThrottledUntil.remove(chunk);
    }

    public void clearHopperThrottle(ChunkKey chunk) {
        hopperThrottledUntil.remove(chunk);
    }

    public int size() {
        cleanup(redstoneThrottledUntil);
        cleanup(hopperThrottledUntil);
        return redstoneThrottledUntil.size() + hopperThrottledUntil.size();
    }

    public void clearAll() {
        redstoneThrottledUntil.clear();
        hopperThrottledUntil.clear();
    }

    private void cleanup(Map<ChunkKey, Long> map) {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(e -> e.getValue() <= now);
    }

    private boolean isStillActive(Map<ChunkKey, Long> map, ChunkKey chunk) {
        Long until = map.get(chunk);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            map.remove(chunk);
            return false;
        }
        return true;
    }
}
