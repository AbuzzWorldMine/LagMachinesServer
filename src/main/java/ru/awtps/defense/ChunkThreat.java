package ru.awtps.defense;

import ru.awtps.model.ChunkKey;
import java.util.EnumSet;
import java.util.Set;

public final class ChunkThreat {
    public final ChunkKey key;
    public long redstone, piston, hopper, dispenser, minecart, boat, mobSpawn, itemSpawn, lever;
    public long lastEvent;
    public int lastX, lastY, lastZ;
    public long quarantineUntil;
    public long hardUntil;
    public final Set<ProtectionType> active = EnumSet.noneOf(ProtectionType.class);

    public ChunkThreat(ChunkKey key) {
        this.key = key;
        this.lastEvent = System.currentTimeMillis();
    }

    public int activityScore() {
        return (int)Math.min(Integer.MAX_VALUE,
                redstone / 8 + piston * 6 + hopper / 3 + dispenser * 3
                        + minecart * 12 + boat * 8 + mobSpawn * 2 + itemSpawn + lever * 2);
    }

    public boolean quarantined() {
        return quarantineUntil > System.currentTimeMillis();
    }

    public boolean hard() {
        return hardUntil > System.currentTimeMillis();
    }

    public void decay() {
        redstone = redstone * 55 / 100;
        piston = piston * 60 / 100;
        hopper = hopper * 60 / 100;
        dispenser = dispenser * 60 / 100;
        minecart = minecart * 60 / 100;
        boat = boat * 60 / 100;
        mobSpawn = mobSpawn * 60 / 100;
        itemSpawn = itemSpawn * 60 / 100;
        lever = lever * 55 / 100;
        if (quarantineUntil <= System.currentTimeMillis()) active.clear();
    }
}