package ru.awtps.listeners;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import ru.awtps.AWtps;
import ru.awtps.model.ChunkKey;


public class HopperListener implements Listener {

    private final AWtps plugin;

    public HopperListener(AWtps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMoveItem(InventoryMoveItemEvent event) {
        Location loc = event.getSource().getLocation();
        if (loc == null || loc.getWorld() == null) return;

        ChunkKey key = new ChunkKey(loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        plugin.getActivityTracker().recordHopperTransfer(key);

        if (plugin.getThrottleManager().isHopperThrottled(key)) {
            event.setCancelled(true);
        }
    }
}
