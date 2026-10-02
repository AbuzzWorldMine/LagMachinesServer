package ru.awtps.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import ru.awtps.AWtps;
import ru.awtps.model.ChunkKey;


public class RedstoneListener implements Listener {

    private final AWtps plugin;

    public RedstoneListener(AWtps plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        ChunkKey key = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);

        if (event.getOldCurrent() != event.getNewCurrent()) {
            plugin.getActivityTracker().recordRedstoneUpdate(key);
        }

        if (plugin.getThrottleManager().isRedstoneThrottled(key)) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        ChunkKey key = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
        if (plugin.getThrottleManager().isRedstoneThrottled(key)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block block = event.getBlock();
        ChunkKey key = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
        if (plugin.getThrottleManager().isRedstoneThrottled(key)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block block = event.getBlock();
        ChunkKey key = new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
        if (plugin.getThrottleManager().isRedstoneThrottled(key)) {
            event.setCancelled(true);
        }
    }
}
