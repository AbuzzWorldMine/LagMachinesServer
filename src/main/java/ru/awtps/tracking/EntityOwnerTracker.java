package ru.awtps.tracking;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;


public final class EntityOwnerTracker {
    private final NamespacedKey ownerIdKey;
    private final NamespacedKey ownerNameKey;

    public EntityOwnerTracker(JavaPlugin plugin) {
        ownerIdKey = new NamespacedKey(plugin, "breeder_uuid");
        ownerNameKey = new NamespacedKey(plugin, "breeder_name");
    }

    public void recordBreed(Entity child, Player breeder) {
        if (child == null || breeder == null) return;
        child.getPersistentDataContainer().set(ownerIdKey, PersistentDataType.STRING, breeder.getUniqueId().toString());
        child.getPersistentDataContainer().set(ownerNameKey, PersistentDataType.STRING, breeder.getName());
    }

    public String owner(Entity entity) {
        if (!(entity instanceof LivingEntity)) return null;
        return entity.getPersistentDataContainer().get(ownerNameKey, PersistentDataType.STRING);
    }

    public UUID ownerId(Entity entity) {
        String value = entity.getPersistentDataContainer().get(ownerIdKey, PersistentDataType.STRING);
        if (value == null) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }

    public boolean isTracked(Entity entity) {
        return owner(entity) != null;
    }
}
