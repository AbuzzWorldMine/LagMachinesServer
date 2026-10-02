package ru.awtps.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.awtps.AWtps;
import ru.awtps.model.PlacementInfo;


public class BlockListener implements Listener {
    private final AWtps plugin;

    public BlockListener(AWtps plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfigManager().ownerTrackingEnabled || !plugin.getConfigManager().ownerTrackingMechanisms) return;
        var block = event.getBlockPlaced();
        var cfg = plugin.getConfigManager();
        if (cfg.ownerTrackedMaterials.contains(block.getType())) {
            plugin.getPlacementTracker().recordPlacement(block, event.getPlayer().getUniqueId(), event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().ownerTrackingEnabled || !plugin.getConfigManager().ownerTrackingMechanisms) return;
        PlacementInfo info = plugin.getPlacementTracker().clear(event.getBlock());
        if (info != null) {
        }
    }
}
