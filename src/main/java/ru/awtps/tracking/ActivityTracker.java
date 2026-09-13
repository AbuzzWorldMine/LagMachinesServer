package ru.awtps.tracking;

import ru.awtps.model.ChunkKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class ActivityTracker {

    private final Map<ChunkKey, AtomicInteger> redstoneUpdates = new ConcurrentHashMap<>();
    private final Map<ChunkKey, AtomicInteger> hopperTransfers = new ConcurrentHashMap<>();

    public void recordRedstoneUpdate(ChunkKey chunk) {
        redstoneUpdates.computeIfAbsent(chunk, k -> new AtomicInteger()).incrementAndGet();
    }

    public void recordHopperTransfer(ChunkKey chunk) {
        hopperTransfers.computeIfAbsent(chunk, k -> new AtomicInteger()).incrementAndGet();
    }

    public int redstoneUpdates(ChunkKey chunk) {
        AtomicInteger v = redstoneUpdates.get(chunk);
        return v == null ? 0 : v.get();
    }

    public int hopperTransfers(ChunkKey chunk) {
        AtomicInteger v = hopperTransfers.get(chunk);
        return v == null ? 0 : v.get();
    }

    
    public void resetWindow() {
        redstoneUpdates.clear();
        hopperTransfers.clear();
    }
}
