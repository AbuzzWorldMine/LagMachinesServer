package ru.awtps.analysis;

import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.awtps.model.ChunkKey;
import ru.awtps.model.ChunkMetrics;
import ru.awtps.tracking.ActivityTracker;


public final class ChunkScanner {

    private ChunkScanner() {
    }

    public static ChunkMetrics scan(Chunk chunk, ActivityTracker activity) {
        int items = 0;
        int mobs = 0;
        int total = 0;

        for (Entity entity : chunk.getEntities()) {
            total++;
            if (entity instanceof Item) {
                items++;
            } else if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                mobs++;
            }
        }

        int tileEntities = 0;
        try {
            BlockState[] states = chunk.getTileEntities();
            tileEntities = states.length;
        } catch (Throwable ignored) {
            
        }

        ChunkKey key = ChunkKey.of(chunk);
        int hopperTransfers = activity.hopperTransfers(key);
        int redstoneUpdates = activity.redstoneUpdates(key);

        return new ChunkMetrics(items, mobs, tileEntities, total, hopperTransfers, redstoneUpdates);
    }
}
