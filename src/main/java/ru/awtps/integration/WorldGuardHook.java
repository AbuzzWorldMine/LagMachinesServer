package ru.awtps.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;

public final class WorldGuardHook {

    private static Boolean pluginPresent;
    private static boolean reflectionReady;
    private static boolean reflectionFailed;

    private static Method miGetInstance;
    private static Method miGetPlatform;
    private static Method miGetRegionContainer;
    private static Method miCreateQuery;
    private static Method miGetApplicableRegions;
    private static Method miAdapt;
    private static Method miGetId;

    private WorldGuardHook() {
    }

    public static boolean isAvailable() {
        if (pluginPresent == null) {
            Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
            pluginPresent = worldGuard != null && worldGuard.isEnabled();
        }
        return pluginPresent;
    }

    public static boolean isWhitelisted(Location location, Set<String> whitelistedRegionIds) {
        if (!isAvailable() || location == null || location.getWorld() == null || whitelistedRegionIds.isEmpty()) {
            return false;
        }
        if (!ensureReflection()) {
            return false;
        }
        try {
            Object worldGuard = miGetInstance.invoke(null);
            Object platform = miGetPlatform.invoke(worldGuard);
            Object regionContainer = miGetRegionContainer.invoke(platform);
            Object query = miCreateQuery.invoke(regionContainer);
            Object weLocation = miAdapt.invoke(null, location);
            Object applicableRegions = miGetApplicableRegions.invoke(query, weLocation);

            for (Object region : (Iterable<?>) applicableRegions) {
                String id = (String) miGetId.invoke(region);
                if (id != null && whitelistedRegionIds.contains(id.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean ensureReflection() {
        if (reflectionReady) return true;
        if (reflectionFailed) return false;
        try {
            Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> weLocationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");

            miGetInstance = worldGuardClass.getMethod("getInstance");
            Object worldGuard = miGetInstance.invoke(null);

            miGetPlatform = worldGuardClass.getMethod("getPlatform");
            Object platform = miGetPlatform.invoke(worldGuard);

            miGetRegionContainer = platform.getClass().getMethod("getRegionContainer");
            Object regionContainer = miGetRegionContainer.invoke(platform);

            miCreateQuery = regionContainer.getClass().getMethod("createQuery");
            Object query = miCreateQuery.invoke(regionContainer);

            miGetApplicableRegions = query.getClass().getMethod("getApplicableRegions", weLocationClass);
            miAdapt = bukkitAdapterClass.getMethod("adapt", Location.class);
            miGetId = protectedRegionClass.getMethod("getId");

            reflectionReady = true;
            return true;
        } catch (Throwable t) {
            reflectionFailed = true;
            Bukkit.getLogger().warning("[AWtps] WorldGuard установлен, но не удалось подключиться к его API (whitelist-zones не будут работать): " + t);
            return false;
        }
    }
}
