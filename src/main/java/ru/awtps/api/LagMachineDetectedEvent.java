package ru.awtps.api;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.awtps.defense.ProtectionType;

import java.util.Collections;
import java.util.Set;

public final class LagMachineDetectedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location location;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final Set<ProtectionType> protections;
    private final String protectionsDisplay;
    private final int mitigatedActions;
    private final int removedEntities;
    private final boolean hardMode;
    private final double tps;
    private final double mspt;

    public LagMachineDetectedEvent(Location location, String worldName, int x, int y, int z,
                                    Set<ProtectionType> protections, String protectionsDisplay,
                                    int mitigatedActions, int removedEntities,
                                    boolean hardMode, double tps, double mspt) {
        this.location = location;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.protections = Collections.unmodifiableSet(protections);
        this.protectionsDisplay = protectionsDisplay;
        this.mitigatedActions = mitigatedActions;
        this.removedEntities = removedEntities;
        this.hardMode = hardMode;
        this.tps = tps;
        this.mspt = mspt;
    }

    public Location getLocation() {
        return location;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Set<ProtectionType> getProtections() {
        return protections;
    }

    public String getProtectionsDisplay() {
        return protectionsDisplay;
    }

    public int getMitigatedActions() {
        return mitigatedActions;
    }

    public int getRemovedEntities() {
        return removedEntities;
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public double getTps() {
        return tps;
    }

    public double getMspt() {
        return mspt;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
