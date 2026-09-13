package ru.awtps.mitigation;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import ru.awtps.config.ConfigManager;
import ru.awtps.model.ChunkKey;
import ru.awtps.model.ChunkMetrics;
import ru.awtps.model.LagCause;
import ru.awtps.tracking.ThrottleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class Mitigator {

    private final ConfigManager cfg;
    private final ThrottleManager throttleManager;

    public Mitigator(ConfigManager cfg, ThrottleManager throttleManager) {
        this.cfg = cfg;
        this.throttleManager = throttleManager;
    }

    
    public int mitigate(Chunk chunk, LagCause cause, ChunkMetrics metrics) {
        if (!cfg.mitigationEnabled) {
            return 0;
        }
        ChunkKey key = ChunkKey.of(chunk);

        return switch (cause) {
            case MECHANISMS -> mitigateMechanisms(key, metrics);
            case ITEM_TRANSPORT -> mitigateItemTransport(key, metrics);
            case MOBS -> mitigateMobs(chunk);
            case ITEMS -> mitigateItems(chunk);
            case UNKNOWN -> 0;
        };
    }

    private int mitigateMechanisms(ChunkKey key, ChunkMetrics metrics) {
        if (!cfg.mitigateRedstone) return 0;
        throttleManager.throttleRedstone(key, cfg.redstoneThrottleCooldownMillis);
        return metrics.redstoneUpdatesInWindow();
    }

    private int mitigateItemTransport(ChunkKey key, ChunkMetrics metrics) {
        if (!cfg.mitigateHoppers) return 0;
        throttleManager.throttleHoppers(key, cfg.hopperThrottleCooldownMillis);
        return metrics.hopperTransfersInWindow();
    }

    private int mitigateMobs(Chunk chunk) {
        if (!cfg.mitigateMobs) return 0;

        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Player) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            if (cfg.mobsIgnoreNamed && living.getCustomName() != null) continue;
            if (cfg.mobsIgnoreTamed && living instanceof Tameable tameable && tameable.isTamed()) continue;
            mobs.add(living);
        }

        int excess = mobs.size() - cfg.maxMobsPerChunk;
        if (excess <= 0) return 0;

        
        
        mobs.sort(Comparator.comparingInt(Entity::getTicksLived));
        int removed = 0;
        for (int i = 0; i < excess && i < mobs.size(); i++) {
            mobs.get(i).remove();
            removed++;
        }
        return removed;
    }

    private int mitigateItems(Chunk chunk) {
        if (!cfg.mitigateItems) return 0;

        List<Item> items = new ArrayList<>();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item item) {
                items.add(item);
            }
        }

        int excess = items.size() - cfg.maxItemsPerChunk;
        if (excess <= 0) return 0;

        items.sort(Comparator.comparingInt(Entity::getTicksLived).reversed());
        int removed = 0;
        for (int i = 0; i < excess && i < items.size(); i++) {
            items.get(i).remove();
            removed++;
        }
        return removed;
    }
}
