package ru.awtps.model;


public record ChunkMetrics(
        int itemEntities,
        int mobEntities,
        int tileEntities,
        int totalEntities,
        int hopperTransfersInWindow,
        int redstoneUpdatesInWindow
) {

    public double weightedScore(double wItems, double wMobs, double wHopper, double wRedstone,
                                 double wTileEntities, double wBlockEntitiesTotal) {
        return itemEntities * wItems
                + mobEntities * wMobs
                + hopperTransfersInWindow * wHopper
                + redstoneUpdatesInWindow * wRedstone
                + tileEntities * wTileEntities
                + totalEntities * wBlockEntitiesTotal;
    }
}
