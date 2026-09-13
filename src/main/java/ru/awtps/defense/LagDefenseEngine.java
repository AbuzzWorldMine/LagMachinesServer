package ru.awtps.defense;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.ThreadLocalRandom;
import ru.awtps.AWtps;
import ru.awtps.config.ConfigManager;
import ru.awtps.model.ChunkKey;
import ru.awtps.logging.LagMachineLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LagDefenseEngine implements Listener {
    private final AWtps plugin;
    private final Map<ChunkKey, ChunkThreat> threats = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Long> lastAction = new ConcurrentHashMap<>();
    private BukkitTask task;
    private long lastGlobalAction;
    private long particleTick;
    private final Map<ChunkKey, Long> lastStructureScan = new ConcurrentHashMap<>();

    public LagDefenseEngine(AWtps plugin) {
        this.plugin = plugin;
    }

    private ConfigManager c() { return plugin.getConfigManager(); }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override public void run() {
                tick();
                particleTick++;
                if (c().particlesEnabled && particleTick % c().particleIntervalTicks == 0) spawnThreatParticles();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        threats.clear();
        lastAction.clear();
        lastStructureScan.clear();
    }

    private ChunkThreat threat(Block b) {
        ChunkThreat t=threats.computeIfAbsent(new ChunkKey(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4), ChunkThreat::new);
        t.lastX=b.getX(); t.lastY=b.getY(); t.lastZ=b.getZ();
        return t;
    }
    private ChunkThreat threat(Location l) {
        return l == null || l.getWorld() == null ? null :
                threats.computeIfAbsent(new ChunkKey(l.getWorld().getName(), l.getBlockX() >> 4, l.getBlockZ() >> 4), ChunkThreat::new);
    }

    private boolean enabled() { return plugin.isProtectionEnabled(); }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void redstone(BlockRedstoneEvent e) {
        Block block = e.getBlock();
        if (!c().mechanismMaterials.contains(block.getType()) && !plugin.getPlacementTracker().hasPlacements(new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4))) return;
        ChunkThreat t=threat(block); t.redstone++; t.lastEvent=System.currentTimeMillis();
        if (enabled() && t.quarantined() && t.active.contains(ProtectionType.REDSTONE)) e.setNewCurrent(e.getOldCurrent());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void pistonExtend(BlockPistonExtendEvent e) { piston(e.getBlock(), e); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void pistonRetract(BlockPistonRetractEvent e) { piston(e.getBlock(), e); }

    private void piston(Block b, Cancellable e) {
        ChunkThreat t=threat(b); t.piston++; t.lastEvent=System.currentTimeMillis();
        if (enabled() && t.quarantined() && t.active.contains(ProtectionType.PISTON)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void dispense(BlockDispenseEvent e) {
        ChunkThreat t=threat(e.getBlock()); t.dispenser++; t.lastEvent=System.currentTimeMillis();
        if (enabled() && t.quarantined() && t.active.contains(ProtectionType.DISPENSER)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void hopper(InventoryMoveItemEvent e) {
        InventoryHolder source = e.getSource().getHolder();
        InventoryHolder destination = e.getDestination().getHolder();
        if (!(source instanceof org.bukkit.block.Hopper) && !(destination instanceof org.bukkit.block.Hopper)
                && !(source instanceof org.bukkit.entity.minecart.HopperMinecart) && !(destination instanceof org.bukkit.entity.minecart.HopperMinecart)) return;
        Location l=e.getSource().getLocation();
        if (l == null || l.getWorld() == null) return;
        ChunkThreat t=threat(l);
        if(t==null) return;
        t.hopper++; t.lastEvent=System.currentTimeMillis();
        if(enabled() && t.quarantined() && t.active.contains(ProtectionType.HOPPER)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void interact(PlayerInteractEvent e) {
        if(e.getClickedBlock()==null) return;
        Material m=e.getClickedBlock().getType();
        if(m==Material.LEVER || m==Material.STONE_BUTTON || m==Material.POLISHED_BLACKSTONE_BUTTON
                || m==Material.OAK_BUTTON || m==Material.SPRUCE_BUTTON || m==Material.BIRCH_BUTTON
                || m==Material.JUNGLE_BUTTON || m==Material.ACACIA_BUTTON || m==Material.DARK_OAK_BUTTON
                || m==Material.MANGROVE_BUTTON || m==Material.CHERRY_BUTTON || m==Material.BAMBOO_BUTTON
                || m==Material.CRIMSON_BUTTON || m==Material.WARPED_BUTTON) {
            ChunkThreat t=threat(e.getClickedBlock()); t.lever++; t.lastEvent=System.currentTimeMillis();
            if(enabled() && t.quarantined() && t.active.contains(ProtectionType.LEVER)) e.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void sculk(BlockReceiveGameEvent e) {
        ChunkThreat t=threat(e.getBlock());
        t.redstone += 2;
        t.lastEvent=System.currentTimeMillis();
        if(enabled() && t.quarantined() && t.active.contains(ProtectionType.REDSTONE)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void spawn(EntitySpawnEvent e) {
        Entity x=e.getEntity();
        if (!(x instanceof Item) && !(x instanceof Mob) && !(x instanceof Minecart) && !(x instanceof Boat)) return;
        ChunkThreat t=threat(x.getLocation()); if(t==null) return;
        t.lastEvent=System.currentTimeMillis();
        if(x instanceof Item) t.itemSpawn++;
        if(x instanceof Minecart) t.minecart++;
        if(x instanceof Boat) t.boat++;
        if(x instanceof Mob) t.mobSpawn++;

        if(!enabled() || !t.quarantined()) return;
        if(x instanceof Minecart && t.active.contains(ProtectionType.MINECART)) e.setCancelled(true);
        else if(x instanceof Boat && t.active.contains(ProtectionType.BOAT)) e.setCancelled(true);
        else if(x instanceof Mob && t.active.contains(ProtectionType.MOBS)) e.setCancelled(true);
        else if(x instanceof Item && t.active.contains(ProtectionType.ITEMS)) e.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void entityPlace(EntityPlaceEvent e) {
        Entity x=e.getEntity();
        ChunkThreat t=threat(x.getLocation()); if(t==null) return;
        if(x instanceof Minecart) t.minecart++;
        if(x instanceof Boat) t.boat++;
        if(enabled() && t.quarantined()) {
            if(x instanceof Minecart && t.active.contains(ProtectionType.MINECART)) e.setCancelled(true);
            if(x instanceof Boat && t.active.contains(ProtectionType.BOAT)) e.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void vehicleCreate(VehicleCreateEvent e) {
        Entity x=e.getVehicle();
        ChunkThreat t=threat(x.getLocation()); if(t==null) return;
        if(x instanceof Minecart) t.minecart++;
        if(x instanceof Boat) t.boat++;
        if(enabled() && t.quarantined()) {
            if(x instanceof Minecart && t.active.contains(ProtectionType.MINECART)) e.setCancelled(true);
            if(x instanceof Boat && t.active.contains(ProtectionType.BOAT)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void unload(ChunkUnloadEvent e) {
        threats.remove(new ChunkKey(e.getWorld().getName(), e.getChunk().getX(), e.getChunk().getZ()));
    }

    private void tick() {
        long now=System.currentTimeMillis();
        double tps=plugin.getTpsMonitor().current1mTps();
        for(ChunkThreat t: threats.values()) {
            t.decay();
            if(t.lastEvent + 30000 < now && !t.quarantined()) {
                threats.remove(t.key);
                continue;
            }
            evaluate(t, tps, now);
        }
        if(tps < c().emergencyTps && now-lastGlobalAction > 5000) emergency(tps, now);
    }

    private void spawnThreatParticles() {
        for (ChunkThreat t : threats.values()) {
            if (!t.quarantined()) continue;
            World world = Bukkit.getWorld(t.key.world());
            if (world == null || !world.isChunkLoaded(t.key.x(), t.key.z())) continue;

            
            Location center = new Location(world, t.lastX + 0.5, Math.max(world.getMinHeight() + 1, Math.min(world.getMaxHeight() - 1, t.lastY + 1.0)), t.lastZ + 0.5);
            boolean nearby = false;
            double maxDistanceSquared = 32.0 * 32.0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getWorld().equals(world)) continue;
                if (player.getLocation().distanceSquared(center) <= maxDistanceSquared) { nearby = true; break; }
            }
            if (!nearby) continue;
            double cx = center.getX();
            double cy = center.getY();
            double cz = center.getZ();
            int points = c().particlePoints;
            double radius = c().particleRadius;
            double height = c().particleHeight;

            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0 * i) / points;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + (i % 3) * (height / 3.0);
                world.spawnParticle(c().particleType, x, y, z, c().particleCount,
                        0.0, 0.05, 0.0, c().particleSpeed);
            }
        }
    }

    private void evaluate(ChunkThreat t, double tps, long now) {
        
        boolean machine =
                t.piston >= c().machinePistonEvents ||
                t.hopper >= c().machineHopperTransfers ||
                t.redstone >= c().machineRedstoneUpdates ||
                t.dispenser >= c().machineDispenses ||
                t.minecart >= c().machineMinecarts ||
                t.boat >= c().machineBoats ||
                t.lever >= c().machineLeverClicks;

        
        
        int structure = 0;
        if (machine || t.activityScore() >= c().machineScore) {
            long lastScan = lastStructureScan.getOrDefault(t.key, 0L);
            if (now - lastScan >= 5000L) {
                structure = structureScore(t);
                lastStructureScan.put(t.key, now);
            }
        }
        if(!machine && t.activityScore() < c().machineScore && structure < c().structureScore) return;
        if(structure >= c().structureScore) machine = true;

        boolean hard = tps < c().criticalThreshold || t.activityScore() >= c().hardMachineScore;
        long duration=(hard ? c().hardQuarantineSeconds : c().quarantineSeconds)*1000L;
        t.quarantineUntil=Math.max(t.quarantineUntil, now+duration);
        if(hard) t.hardUntil=Math.max(t.hardUntil, now+duration);

        t.active.clear();
        if(t.redstone >= c().machineRedstoneUpdates || t.piston >= c().machinePistonEvents) {
            t.active.add(ProtectionType.REDSTONE); t.active.add(ProtectionType.PISTON);
        }
        if(t.hopper >= c().machineHopperTransfers) t.active.add(ProtectionType.HOPPER);
        if(t.dispenser >= c().machineDispenses) t.active.add(ProtectionType.DISPENSER);
        if(t.minecart >= c().machineMinecarts) t.active.add(ProtectionType.MINECART);
        if(t.boat >= c().machineBoats) t.active.add(ProtectionType.BOAT);
        if(t.lever >= c().machineLeverClicks) t.active.add(ProtectionType.LEVER);

        if(hard || t.mobSpawn >= c().mobSpawnBurst) t.active.add(ProtectionType.MOBS);
        if(hard || t.itemSpawn >= c().itemSpawnBurst) t.active.add(ProtectionType.ITEMS);

        long previous=lastAction.getOrDefault(t.key,0L);
        if(now-previous < c().actionCooldownSeconds*1000L) return;
        lastAction.put(t.key,now);

        int removedEntities=cleanup(t, hard);
        String types=t.active.stream().map(Enum::name).reduce((a,b)->a+", "+b).orElse("UNKNOWN");
        int mitigated = removedEntities + t.active.size();
        
        
        double mspt = plugin.getTpsMonitor().averageTickTimeMillis();
        plugin.getLagMachineLogger().log(t, tps, mspt, types, mitigated, removedEntities, hard, structure);

        plugin.getLogger().warning(String.format(Locale.US,
                "Lag machine protected: %s/%d,%d,%d | TPS %.2f | MSPT %.2f | score %d | %s | hard %s | mitigated %d | removedEntities %d",
                t.key.world(), t.lastX, t.lastY, t.lastZ, tps, mspt, t.activityScore(), types, hard, mitigated, removedEntities));
        if(c().notifyEnabled) {
            plugin.getNotificationService().broadcastToStaff(
                    c().messageAction
                    .replace("%location%", t.key.world() + "/" + t.lastX + "," + t.lastY + "," + t.lastZ)
                    .replace("%world%", t.key.world())
                    .replace("%cause%", types)
                    .replace("%count%", String.valueOf(mitigated)));
        }
    }

    private int structureScore(ChunkThreat t) {
        World w=Bukkit.getWorld(t.key.world());
        if(w==null || !w.isChunkLoaded(t.key.x(),t.key.z())) return 0;
        int score=0;
        int minY=Math.max(w.getMinHeight(), t.lastY-4);
        int maxY=Math.min(w.getMaxHeight()-1, t.lastY+4);
        
        for(int x=t.lastX-4;x<=t.lastX+4;x++) for(int z=t.lastZ-4;z<=t.lastZ+4;z++) for(int y=minY;y<=maxY;y++) {
            Material m=w.getBlockAt(x,y,z).getType();
            if(m==Material.REDSTONE_WIRE){score++;}
            else if(m==Material.PISTON || m==Material.STICKY_PISTON){score+=4;}
            else if(m==Material.HOPPER){score+=3;}
            else if(m==Material.OBSERVER){score+=3;}
            else if(m==Material.REPEATER){score+=2;}
            else if(m==Material.COMPARATOR){score+=2;}
            else if(m==Material.REDSTONE_BLOCK || m==Material.DISPENSER || m==Material.DROPPER){score+=2;}
            if(score>=c().structureScore) return score;
        }
        return score;
    }

    private int cleanup(ChunkThreat t, boolean hard) {
        World w=Bukkit.getWorld(t.key.world()); if(w==null || !w.isChunkLoaded(t.key.x(),t.key.z())) return 0;
        Chunk ch=w.getChunkAt(t.key.x(),t.key.z());
        List<Entity> mobs=new ArrayList<>(), items=new ArrayList<>(), carts=new ArrayList<>(), boats=new ArrayList<>();
        for(Entity e:ch.getEntities()) {
            if(e instanceof Player) continue;
            if(e instanceof Item) items.add(e);
            else if(e instanceof Minecart) carts.add(e);
            else if(e instanceof Boat) boats.add(e);
            else if(e instanceof Mob) mobs.add(e);
        }
        int removed=0;
        int maxItems=hard?c().hardItemsPerChunk:c().safeItemsPerChunk;
        int maxMobs=hard?c().hardMobsPerChunk:c().safeMobsPerChunk;
        int maxCarts=hard?c().hardMinecartsPerChunk:c().safeMinecartsPerChunk;
        int maxBoats=hard?c().hardBoatsPerChunk:c().safeBoatsPerChunk;

        removed += removeExcess(items,maxItems,false);
        removed += removeExcess(carts,maxCarts,true);
        removed += removeExcess(boats,maxBoats,true);
        removed += removeExcess(mobs,maxMobs,true);
        return removed;
    }

    private int removeExcess(List<Entity> list,int limit,boolean preserveNamed) {
        if(list.size()<=limit) return 0;
        list.sort(Comparator.comparingInt(Entity::getTicksLived).thenComparing(e->e.getUniqueId().toString()));
        int remove=list.size()-limit, n=0;
        for(Entity e:list) {
            if(n>=remove) break;
            if(e instanceof LivingEntity l) {
                if(preserveNamed && l.getCustomName()!=null) continue;
                if(preserveNamed && l instanceof Tameable t && t.isTamed()) continue;
            }
            e.remove(); n++;
        }
        return n;
    }

    private void emergency(double tps,long now) {
        lastGlobalAction=now;
        int removed=0;
        for(ChunkThreat t: threats.values()) {
            if(!t.quarantined()) continue;
            removed += cleanup(t,true);
        }
        double mspt = plugin.getTpsMonitor().averageTickTimeMillis();
        plugin.getLagMachineLogger().logEmergency(tps, mspt, removed);
        plugin.getLogger().warning(String.format(Locale.US,
                "EMERGENCY lag protection active: TPS %.2f, MSPT %.2f, cleaned %d entities from protected chunks.",
                tps, mspt, removed));
    }

    public int protectedChunks() {
        int n=0; for(ChunkThreat t:threats.values()) if(t.quarantined()) n++; return n;
    }
    public int trackedChunks(){ return threats.size(); }
    public int activeRestrictions(){
        int n=0; for(ChunkThreat t:threats.values()) if(t.quarantined()) n+=t.active.size(); return n;
    }
    public void clear() { threats.clear(); lastAction.clear(); lastStructureScan.clear(); }
    public void forceScan() { for(ChunkThreat t:threats.values()) evaluate(t, plugin.getTpsMonitor().current1mTps(), System.currentTimeMillis()); }
}