package com.localcode.vortexgaming;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.localcode.vortexgaming.models.NotificationItem;
import com.localcode.vortexgaming.utils.NotificationHelper;
import com.localcode.vortexgaming.utils.NotificationsStore;

/**
 * BroadcastReceiver that fires system + in-app notifications.
 *
 * Use {@link #schedule} to post a notification after a delay — it uses AlarmManager so the
 * notification fires even if the app process has been killed.
 */
public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_TITLE    = "n_title";
    public static final String EXTRA_BODY     = "n_body";
    public static final String EXTRA_CHANNEL  = "n_channel";
    public static final String EXTRA_NOTIF_ID = "n_id";
    public static final String EXTRA_STORE_ID = "n_store_id";
    public static final String EXTRA_TYPE     = "n_type";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String title   = intent.getStringExtra(EXTRA_TITLE);
        String body    = intent.getStringExtra(EXTRA_BODY);
        String channel = intent.getStringExtra(EXTRA_CHANNEL);
        int    notifId = intent.getIntExtra(EXTRA_NOTIF_ID, NotificationHelper.ID_LAUNCH);
        String storeId = intent.getStringExtra(EXTRA_STORE_ID);
        String type    = intent.getStringExtra(EXTRA_TYPE);

        if (title == null || body == null) return;
        if (channel == null) channel = NotificationHelper.CH_REQUESTS;

        // 1. Fire the system notification
        NotificationHelper.show(ctx, channel, notifId, title, body);

        // 2. Persist to in-app store so the bell badge and sheet show it
        if (storeId != null) {
            new NotificationsStore(ctx).add(
                    new NotificationItem(storeId, title, body,
                            type != null ? type : "request"));
        }
    }

    // ── Static scheduling helper ──────────────────────────────────────────

    /**
     * Schedules a notification to fire after {@code delayMs} milliseconds.
     * Safe to call from any context — uses AlarmManager, survives app death.
     *
     * @param ctx       application or activity context
     * @param delayMs   delay in milliseconds (e.g. 60_000L for 1 minute)
     * @param title     notification title shown in the tray
     * @param body      notification body text (also saved to in-app store)
     * @param channelId one of {@link NotificationHelper#CH_RELEASES} etc.
     * @param notifId   stable integer ID for this notification slot
     * @param storeId   unique string ID saved to {@link NotificationsStore};
     *                  pass {@code null} to skip in-app persistence
     * @param type      one of "release", "request", "trending", "promo"
     */
    public static void schedule(Context ctx, long delayMs,
                                 String title, String body,
                                 String channelId, int notifId,
                                 String storeId, String type) {
        Intent intent = new Intent(ctx, NotificationReceiver.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_CHANNEL, channelId);
        intent.putExtra(EXTRA_NOTIF_ID, notifId);
        intent.putExtra(EXTRA_STORE_ID, storeId);
        intent.putExtra(EXTRA_TYPE, type);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                   ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, notifId, intent, piFlags);

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        long triggerAt = System.currentTimeMillis() + delayMs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // setExactAndAllowWhileIdle works even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // Exact alarms not permitted → fallback to inexact (still fires, just imprecise)
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /**
     * Cancels a previously scheduled notification with the given {@code notifId}.
     */
    public static void cancel(Context ctx, int notifId) {
        Intent intent = new Intent(ctx, NotificationReceiver.class);
        int piFlags = PendingIntent.FLAG_NO_CREATE
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                   ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, notifId, intent, piFlags);
        if (pi != null) {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            am.cancel(pi);
            pi.cancel();
        }
    }
}
