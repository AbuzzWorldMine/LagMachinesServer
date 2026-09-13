package ru.awtps.model;


public final class LagReportEntry {

    private final ChunkKey chunk;
    private final LagCause cause;
    private final double score;
    private final int x, y, z;
    private final int mitigatedCount;
    private final long timestampMillis;

    public LagReportEntry(ChunkKey chunk, LagCause cause, double score,
                           int mitigatedCount, long timestampMillis, int x, int y, int z) {
        this.chunk = chunk;
        this.cause = cause;
        this.score = score;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mitigatedCount = mitigatedCount;
        this.timestampMillis = timestampMillis;
    }

    public ChunkKey chunk() {
        return chunk;
    }

    public LagCause cause() {
        return cause;
    }

    public double score() {
        return score;
    }


    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public int mitigatedCount() {
        return mitigatedCount;
    }

    public long timestampMillis() {
        return timestampMillis;
    }
}
