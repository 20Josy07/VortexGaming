package com.localcode.vortexgaming.models;

public class NotificationItem {
    public String id;
    public String title;
    public String message;
    public String type;      // "release", "request", "promo"
    public long   timestamp;
    public boolean read;

    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String type) {
        this.id        = id;
        this.title     = title;
        this.message   = message;
        this.type      = type;
        this.timestamp = System.currentTimeMillis();
        this.read      = false;
    }
}
