package com.example.shipperincome.deliverynotes;

public final class LocationNote {
    public final long id;
    public final String name;
    public final String note;
    public final double latitude;
    public final double longitude;
    public final int radiusMeters;
    public final long createdAt;
    public final long lastNotifiedAt;

    public LocationNote(long id, String name, String note, double latitude, double longitude,
                         int radiusMeters, long createdAt, long lastNotifiedAt) {
        this.id = id;
        this.name = name;
        this.note = note;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeters = radiusMeters;
        this.createdAt = createdAt;
        this.lastNotifiedAt = lastNotifiedAt;
    }
}
