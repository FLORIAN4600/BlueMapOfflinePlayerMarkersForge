package com.technicjelle.bluemapofflineplayermarkers.struct;

import java.util.List;
import java.util.UUID;

public class OfflinePlayer {

    public UUID uuid;
    public String name;
    public long lastTimeOnline;
    public List<Double> position;
    public String dimension;
    public int gameMode;


    public OfflinePlayer(UUID uuid, String name, long lastTimeOnline, List<Double> position, String dimension, int gameMode) {
        this.uuid = uuid;
        this.name = name;
        this.lastTimeOnline = lastTimeOnline;
        this.position = position;
        this.dimension = dimension;
        this.gameMode = gameMode;
    }

}
