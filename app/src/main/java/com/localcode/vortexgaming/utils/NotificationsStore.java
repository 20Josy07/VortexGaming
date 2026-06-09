package com.localcode.vortexgaming.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.localcode.vortexgaming.models.NotificationItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NotificationsStore {

    private static final String PREFS = "vortex_notifications";
    private static final String KEY   = "notif_list";
    private static final int    MAX   = 50;
    private static final Gson   GSON  = new Gson();

    private final SharedPreferences prefs;

    public NotificationsStore(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /** Returns all stored notifications, newest-first. */
    public List<NotificationItem> getAll() {
        String json = prefs.getString(KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<NotificationItem>>() {}.getType();
        List<NotificationItem> list = GSON.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    /** Returns only unread notifications. */
    public List<NotificationItem> getUnread() {
        List<NotificationItem> out = new ArrayList<>();
        for (NotificationItem n : getAll()) if (!n.read) out.add(n);
        return out;
    }

    /** Returns notifications whose {@code type} matches the given value. */
    public List<NotificationItem> getByType(String type) {
        List<NotificationItem> out = new ArrayList<>();
        for (NotificationItem n : getAll()) if (type.equals(n.type)) out.add(n);
        return out;
    }

    public int getUnreadCount() {
        int count = 0;
        for (NotificationItem n : getAll()) if (!n.read) count++;
        return count;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * Adds a notification at the front of the list.
     * No-op if an item with the same {@code id} already exists.
     */
    public void add(NotificationItem item) {
        List<NotificationItem> list = getAll();
        for (NotificationItem n : list) {
            if (n.id != null && n.id.equals(item.id)) return;
        }
        list.add(0, item);
        if (list.size() > MAX) list = list.subList(0, MAX);
        save(list);
    }

    /** Marks a single notification as read by its ID. */
    public void markRead(String id) {
        List<NotificationItem> list = getAll();
        boolean changed = false;
        for (NotificationItem n : list) {
            if (id.equals(n.id) && !n.read) {
                n.read = true;
                changed = true;
                break;
            }
        }
        if (changed) save(list);
    }

    /** Marks every notification as read. */
    public void markAllRead() {
        List<NotificationItem> list = getAll();
        boolean changed = false;
        for (NotificationItem n : list) {
            if (!n.read) { n.read = true; changed = true; }
        }
        if (changed) save(list);
    }

    /** Removes the notification with the given ID. */
    public void delete(String id) {
        List<NotificationItem> list = getAll();
        if (list.removeIf(n -> id.equals(n.id))) save(list);
    }

    /** Removes all notifications. */
    public void deleteAll() {
        prefs.edit().remove(KEY).apply();
    }

    // ── Internal ──────────────────────────────────────────────────────────
    private void save(List<NotificationItem> list) {
        prefs.edit().putString(KEY, GSON.toJson(list)).apply();
    }
}
