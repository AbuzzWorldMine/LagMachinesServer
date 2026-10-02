package ru.awtps.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import ru.awtps.AWtps;


public final class OwnershipListener implements Listener {
    private final AWtps plugin;

    public OwnershipListener(AWtps plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!plugin.getConfigManager().ownerTrackingEnabled) return;
        if (event.getBreeder() instanceof Player player) {
            plugin.getEntityOwnerTracker().recordBreed(event.getEntity(), player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (var block : event.blockList()) plugin.getPlacementTracker().clear(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (var block : event.blockList()) plugin.getPlacementTracker().clear(block);
    }
}
