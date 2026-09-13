package ru.awtps.model;

import org.bukkit.Material;

import java.util.UUID;


public final class PlacementInfo {

    private final UUID playerId;
    private final String playerName;
    private final Material material;
    private final long placedAtMillis;

    public PlacementInfo(UUID playerId, String playerName, Material material, long placedAtMillis) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.material = material;
        this.placedAtMillis = placedAtMillis;
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public Material material() {
        return material;
    }

    public long placedAtMillis() {
        return placedAtMillis;
    }
}
