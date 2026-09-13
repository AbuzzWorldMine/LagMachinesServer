package ru.awtps.tracking;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.awtps.model.ChunkKey;
import ru.awtps.model.PlacementInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class PlacementTracker {
    private final Map<ChunkKey, Map<Long, PlacementInfo>> byChunk = new ConcurrentHashMap<>();
    private Path file;
    private BukkitTask saveTask;
    private volatile boolean dirty;
    private volatile long revision;

    public void startPersistence(JavaPlugin plugin) {
        file = plugin.getDataFolder().toPath().resolve("data").resolve("placements.tsv");
        load();
        saveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::saveSnapshot, 20L * 30L, 20L * 30L);
    }

    public void stopPersistence() {
        if (saveTask != null) saveTask.cancel();
        saveTask = null;
        saveSnapshot();
    }

    private long blockKey(Block block) {
        long x = block.getX() & 0xFFFFFL;
        long y = block.getY() & 0xFFFL;
        long z = block.getZ() & 0xFFFFFL;
        return (x << 32) ^ (z << 12) ^ y;
    }

    private ChunkKey chunkKey(Block block) {
        return new ChunkKey(block.getWorld().getName(), block.getX() >> 4, block.getZ() >> 4);
    }

    public void recordPlacement(Block block, UUID playerId, String playerName) {
        ChunkKey chunk = chunkKey(block);
        byChunk.computeIfAbsent(chunk, k -> new ConcurrentHashMap<>())
                .put(blockKey(block), new PlacementInfo(playerId, playerName, block.getType(), System.currentTimeMillis()));
        dirty = true;
        revision++;
    }

    public PlacementInfo clear(Block block) {
        Map<Long, PlacementInfo> inChunk = byChunk.get(chunkKey(block));
        if (inChunk == null) return null;
        PlacementInfo removed = inChunk.remove(blockKey(block));
        if (inChunk.isEmpty()) byChunk.remove(chunkKey(block), inChunk);
        if (removed != null) { dirty = true; revision++; }
        return removed;
    }

    public PlacementInfo get(Block block) {
        Map<Long, PlacementInfo> inChunk = byChunk.get(chunkKey(block));
        return inChunk == null ? null : inChunk.get(blockKey(block));
    }

    public PlacementInfo get(Location location) {
        return get(location.getBlock());
    }

    public Collection<PlacementInfo> placementsInChunk(ChunkKey chunk) {
        Map<Long, PlacementInfo> inChunk = byChunk.get(chunk);
        return inChunk == null ? Collections.emptyList() : inChunk.values();
    }

    public boolean hasPlacements(ChunkKey chunk) {
        Map<Long, PlacementInfo> inChunk = byChunk.get(chunk);
        return inChunk != null && !inChunk.isEmpty();
    }

    public int size() {
        int total = 0;
        for (Map<Long, PlacementInfo> m : byChunk.values()) total += m.size();
        return total;
    }

    private void load() {
        if (file == null || Files.notExists(file)) return;
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank() || line.charAt(0) == '#') continue;
                String[] p = line.split("\\t", -1);
                if (p.length != 9 || !"P".equals(p[0])) continue;
                try {
                    String world = p[1];
                    int x = Integer.parseInt(p[2]);
                    int y = Integer.parseInt(p[3]);
                    int z = Integer.parseInt(p[4]);
                    UUID uuid = UUID.fromString(p[5]);
                    String name = p[6].replace((char) 1, '\t');
                    org.bukkit.Material material = org.bukkit.Material.matchMaterial(p[7]);
                    long placed = Long.parseLong(p[8]);
                    if (material == null) continue;
                    ChunkKey key = new ChunkKey(world, x >> 4, z >> 4);
                    BlockRecordKey br = new BlockRecordKey(x, y, z);
                    byChunk.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                            .put(br.key(), new PlacementInfo(uuid, name, material, placed));
                } catch (RuntimeException ignored) { }
            }
        } catch (IOException ignored) { }
        dirty = false;
    }

    private synchronized void saveSnapshot() {
        if (!dirty || file == null) return;
        long savedRevision = revision;
        Map<ChunkKey, Map<Long, PlacementInfo>> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<ChunkKey, Map<Long, PlacementInfo>> e : byChunk.entrySet()) {
            snapshot.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        try {
            Files.createDirectories(file.getParent());
            saveDirect(snapshot);
            if (revision == savedRevision) dirty = false;
        } catch (IOException ignored) { }
    }

    private void saveDirect(Map<ChunkKey, Map<Long, PlacementInfo>> snapshot) throws IOException {
        
        Path tmp = file.resolveSibling("placements.tsv.tmp");
        try (BufferedWriter out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write("# type\\tworld\\tx\\ty\\tz\\tuuid\\tname\\tmaterial\\tplacedAtMillis");
            out.newLine();
            for (Map.Entry<ChunkKey, Map<Long, PlacementInfo>> e : snapshot.entrySet()) {
                for (Map.Entry<Long, PlacementInfo> r : e.getValue().entrySet()) {
                    long key = r.getKey();
                    int x = (int)((key >>> 32) & 0xFFFFFL);
                    int z = (int)((key >>> 12) & 0xFFFFFL);
                    int y = (int)(key & 0xFFFL);
                    
                    if ((x & 0x80000) != 0) x |= ~0xFFFFF;
                    if ((z & 0x80000) != 0) z |= ~0xFFFFF;
                    if ((y & 0x800) != 0) y |= ~0xFFF;
                    PlacementInfo info = r.getValue();
                    String name = info.playerName().replace('\t', ' ');
                    out.write("P\t" + e.getKey().world() + "\t" + x + "\t" + y + "\t" + z + "\t"
                            + info.playerId() + "\t" + name + "\t" + info.material().name() + "\t" + info.placedAtMillis());
                    out.newLine();
                }
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private record BlockRecordKey(int x, int y, int z) {
        long key() {
            long xx = x & 0xFFFFFL;
            long yy = y & 0xFFFL;
            long zz = z & 0xFFFFFL;
            return (xx << 32) ^ (zz << 12) ^ yy;
        }
    }
}
