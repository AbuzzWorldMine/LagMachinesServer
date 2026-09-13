package ru.awtps.analysis;

import org.bukkit.Material;
import ru.awtps.config.ConfigManager;
import ru.awtps.model.ChunkKey;
import ru.awtps.model.LagCause;
import ru.awtps.model.PlacementInfo;
import ru.awtps.tracking.PlacementTracker;
import ru.awtps.tracking.EntityOwnerTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class AttributionManager {

    private final PlacementTracker placementTracker;
    private final ConfigManager cfg;
    private EntityOwnerTracker entityOwnerTracker;

    public AttributionManager(PlacementTracker placementTracker, ConfigManager cfg) {
        this.placementTracker = placementTracker;
        this.cfg = cfg;
    }


    public void setEntityOwnerTracker(EntityOwnerTracker tracker) {
        this.entityOwnerTracker = tracker;
    }
    
    public String findResponsible(ChunkKey chunk, LagCause cause) {
        Set<Material> relevant = switch (cause) {
            case MECHANISMS -> cfg.mechanismMaterials;
            case ITEM_TRANSPORT -> cfg.itemTransportMaterials;
            default -> null;
        };
        if (relevant == null || relevant.isEmpty()) {
            return null;
        }

        Collection<PlacementInfo> placements = placementTracker.placementsInChunk(chunk);
        if (placements.isEmpty()) {
            return null;
        }

        Map<String, Integer> countsByPlayer = new HashMap<>();
        for (PlacementInfo info : placements) {
            if (!relevant.contains(info.material())) continue;
            countsByPlayer.merge(info.playerName(), 1, Integer::sum);
        }

        String topPlayer = null;
        int topCount = 0;
        for (Map.Entry<String, Integer> entry : countsByPlayer.entrySet()) {
            if (entry.getValue() > topCount) {
                topCount = entry.getValue();
                topPlayer = entry.getKey();
            }
        }
        return topPlayer;
    }
    
    public String findResponsibleAny(ChunkKey chunk) {
        
        Map<String, Integer> scores = new HashMap<>();
        collectPlacementScores(chunk, scores);

        
        
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkKey near = new ChunkKey(chunk.world(), chunk.x() + dx, chunk.z() + dz);
                collectPlacementScores(near, scores, 0.35);
            }
        }

        
        
        if (scores.isEmpty() && entityOwnerTracker != null) {
            World world = Bukkit.getWorld(chunk.world());
            if (world != null && world.isChunkLoaded(chunk.x(), chunk.z())) {
                Chunk loaded = world.getChunkAt(chunk.x(), chunk.z());
                for (Entity entity : loaded.getEntities()) {
                    String owner = entityOwnerTracker.owner(entity);
                    if (owner != null) scores.merge(owner, 4, Integer::sum);
                }
            }
        }

        String top = null;
        double best = 0;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
            }
        }
        return top;
    }

    private void collectPlacementScores(ChunkKey chunk, Map<String, Integer> scores) {
        collectPlacementScores(chunk, scores, 1.0);
    }

    private void collectPlacementScores(ChunkKey chunk, Map<String, Integer> scores, double multiplier) {
        for (PlacementInfo info : placementTracker.placementsInChunk(chunk)) {
            int weight = 1;
            if (cfg.mechanismMaterials.contains(info.material())) weight = 3;
            else if (cfg.itemTransportMaterials.contains(info.material())) weight = 2;
            int finalWeight = Math.max(1, (int) Math.round(weight * multiplier));
            scores.merge(info.playerName(), finalWeight, Integer::sum);
        }
    }

}
