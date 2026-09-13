package ru.awtps.commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import ru.awtps.AWtps;

public final class TpsPriorityListener implements Listener {
    private final TpsCommand tps;

    public TpsPriorityListener(AWtps plugin, TpsCommand tps) {
        this.tps = tps;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void player(PlayerCommandPreprocessEvent e) {
        String raw=e.getMessage();
        if(!isTps(raw)) return;
        String[] args=parse(raw);
        e.setCancelled(true);
        tps.onCommand(e.getPlayer(), null, "tps", args);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void console(ServerCommandEvent e) {
        String raw=e.getCommand();
        if(!isTps("/"+raw)) return;
        String[] args=parse("/"+raw);
        e.setCancelled(true);
        tps.onCommand(e.getSender(), null, "tps", args);
    }

    private boolean isTps(String raw) {
        String s=raw.trim();
        if(s.startsWith("/")) s=s.substring(1);
        String label=s.split("\\s+",2)[0];
        return label.equalsIgnoreCase("tps");
    }

    private String[] parse(String raw) {
        String s=raw.trim();
        if(s.startsWith("/")) s=s.substring(1);
        String[] p=s.split("\\s+");
        if(p.length<=1) return new String[0];
        String[] out=new String[p.length-1];
        System.arraycopy(p,1,out,0,out.length);
        return out;
    }
}