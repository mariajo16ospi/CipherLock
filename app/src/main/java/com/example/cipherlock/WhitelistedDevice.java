package com.example.cipherlock;

/**
 * Clase que representa un dispositivo USB en la lista blanca
 */
public class WhitelistedDevice {
    private String id;
    private String name;
    private long addedTimestamp;

    public WhitelistedDevice(String id, String name, long addedTimestamp) {
        this.id = id;
        this.name = name;
        this.addedTimestamp = addedTimestamp;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }
}