package com.localcode.vortexgaming.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.localcode.vortexgaming.MainActivity;
import com.localcode.vortexgaming.R;

public class NotificationHelper {

    // ── Channels ──────────────────────────────────────────────────────────
    /** High-importance channel for DLC / game release alerts */
    public static final String CH_RELEASES = "ch_releases";
    /** High-importance channel for user-request status updates */
    public static final String CH_REQUESTS = "ch_requests";
    /** Default-importance channel for trending / promo news */
    public static final String CH_PROMOS   = "ch_promos";

    // ── Notification IDs ──────────────────────────────────────────────────
    public static final int ID_LAUNCH        = 1002;   // kept for callers that don't specify
    public static final int ID_GROUP_SUMMARY = 9999;

    // ── Android notification group key ────────────────────────────────────
    public static final String GROUP_KEY = "com.localcode.vortexgaming.NOTIF_GROUP";

    // ── Channel creation (call once from Application / Activity) ─────────
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        NotificationChannel releases = new NotificationChannel(
                CH_RELEASES, "Lanzamientos", NotificationManager.IMPORTANCE_HIGH);
        releases.setDescription("Lanzamientos de juegos y DLCs");
        releases.setShowBadge(true);

        NotificationChannel requests = new NotificationChannel(
                CH_REQUESTS, "Mis solicitudes", NotificationManager.IMPORTANCE_HIGH);
        requests.setDescription("Estado de tus solicitudes de expansiones");
        requests.setShowBadge(true);

        NotificationChannel promos = new NotificationChannel(
                CH_PROMOS, "Novedades", NotificationManager.IMPORTANCE_DEFAULT);
        promos.setDescription("Tendencias y noticias de VortexGaming");
        promos.setShowBadge(false);

        nm.createNotificationChannel(releases);
        nm.createNotificationChannel(requests);
        nm.createNotificationChannel(promos);
    }

    /** @deprecated kept for backward-compat callers; routes to CH_REQUESTS */
    public static void createChannel(Context ctx) {
        createChannels(ctx);
    }

    // ── Show ──────────────────────────────────────────────────────────────

    /**
     * Post a system notification.
     *
     * @param channelId one of {@link #CH_RELEASES}, {@link #CH_REQUESTS}, {@link #CH_PROMOS}
     * @param notifId   stable integer ID — same ID overwrites a previous notification of the
     *                  same logical event; use a fresh ID (e.g. based on a hash) for distinct ones
     */
    public static void show(Context ctx, String channelId, int notifId, String title, String body) {
        if (!hasPermission(ctx)) return;

        int priority = CH_PROMOS.equals(channelId)
                ? NotificationCompat.PRIORITY_DEFAULT
                : NotificationCompat.PRIORITY_HIGH;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setContentIntent(buildOpenIntent(ctx, notifId))
                .setAutoCancel(true)
                .setGroup(GROUP_KEY);

        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        nm.notify(notifId, builder.build());
        postGroupSummary(ctx, nm);
    }

    /**
     * Backward-compatible overload — routes to {@link #CH_REQUESTS}.
     */
    public static void show(Context ctx, int notifId, String title, String body) {
        show(ctx, CH_REQUESTS, notifId, title, body);
    }

    // ── Group summary (keeps notifications collapsed in the shade) ────────
    private static void postGroupSummary(Context ctx, NotificationManagerCompat nm) {
        if (!hasPermission(ctx)) return;
        NotificationCompat.Builder summary = new NotificationCompat.Builder(ctx, CH_RELEASES)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(buildOpenIntent(ctx, ID_GROUP_SUMMARY));
        nm.notify(ID_GROUP_SUMMARY, summary.build());
    }

    // ── Deep-link PendingIntent ───────────────────────────────────────────
    /**
     * Returns a PendingIntent that opens {@link MainActivity} with
     * {@code open_notifications=true} so the app auto-shows the notifications sheet.
     */
    private static PendingIntent buildOpenIntent(Context ctx, int requestCode) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_notifications", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                   ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getActivity(ctx, requestCode, intent, flags);
    }

    // ── Permission ────────────────────────────────────────────────────────
    public static boolean hasPermission(Context ctx) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }
}
