package com.localcode.vortexgaming.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.models.NotificationItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(NotificationItem item, int position);
    }

    private List<NotificationItem> items;
    private final OnItemClickListener listener;
    private int lastAnimatedPosition = -1;

    public NotificationsAdapter(List<NotificationItem> items, OnItemClickListener listener) {
        this.items    = new ArrayList<>(items);
        this.listener = listener;
    }

    // ── Data mutations (used by sheet for filter / swipe-undo) ────────────

    public void updateItems(List<NotificationItem> newItems) {
        this.items = new ArrayList<>(newItems);
        lastAnimatedPosition = -1;
        notifyDataSetChanged();
    }

    /** Removes item at position and returns it (for undo snackbar). */
    public NotificationItem removeAt(int position) {
        NotificationItem removed = items.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    /** Re-inserts a previously removed item (undo). */
    public void restoreAt(int position, NotificationItem item) {
        items.add(position, item);
        notifyItemInserted(position);
    }

    /** Marks item at position as read in the local list and updates the dot. */
    public void markReadAt(int position) {
        if (position < 0 || position >= items.size()) return;
        NotificationItem item = items.get(position);
        if (!item.read) {
            item.read = true;
            notifyItemChanged(position, "read"); // partial update — avoids re-animation
        }
    }

    public NotificationItem getItem(int position) {
        return items.get(position);
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NotificationItem n = items.get(position);
        h.title.setText(n.title);
        h.message.setText(n.message);
        h.time.setText(relativeTime(n.timestamp));
        h.icon.setText(iconFor(n.type));
        h.iconBg.setBackgroundTintList(ColorStateList.valueOf(colorFor(n.type)));
        h.unreadDot.setVisibility(n.read ? View.GONE : View.VISIBLE);
        h.accentBar.setVisibility(n.read ? View.GONE : View.VISIBLE);

        // Unread row highlight
        h.itemView.setBackgroundColor(n.read
                ? Color.TRANSPARENT
                : Color.parseColor("#0D7B61FB")); // ~5% purple tint

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(n, h.getAdapterPosition());
        });

        // Stagger slide-in (only for items not yet animated)
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            h.itemView.setAlpha(0f);
            h.itemView.setTranslationX(30f);
            h.itemView.animate()
                    .alpha(1f).translationX(0f)
                    .setDuration(260)
                    .setStartDelay(position * 40L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        // Partial update when only the read-state changed
        if (!payloads.isEmpty() && "read".equals(payloads.get(0))) {
            NotificationItem n = items.get(position);
            h.unreadDot.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(200)
                    .withEndAction(() -> h.unreadDot.setVisibility(View.GONE)).start();
            h.accentBar.animate().alpha(0f).setDuration(300)
                    .withEndAction(() -> h.accentBar.setVisibility(View.GONE)).start();
            h.itemView.animate().setDuration(400)
                    .withEndAction(() -> h.itemView.setBackgroundColor(Color.TRANSPARENT)).start();
        } else {
            super.onBindViewHolder(h, position, payloads);
        }
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String iconFor(String type) {
        if (type == null) return "🔔";
        switch (type) {
            case "release":  return "🚀";
            case "request":  return "✅";
            case "trending": return "🔥";
            case "promo":    return "⭐";
            default:         return "🔔";
        }
    }

    /**
     * Returns a circle background color per notification type.
     * These are ARGB hex values applied as a tint over the existing circular drawable.
     */
    private int colorFor(String type) {
        if (type == null) return Color.parseColor("#FF7B61FB"); // purple
        switch (type) {
            case "release":  return Color.parseColor("#FFE8651A"); // orange
            case "request":  return Color.parseColor("#FF22C55E"); // green
            case "trending": return Color.parseColor("#FFEF4444"); // red
            case "promo":    return Color.parseColor("#FF3B82F6"); // blue
            default:         return Color.parseColor("#FF7B61FB"); // purple
        }
    }

    private String relativeTime(long ts) {
        long diff = System.currentTimeMillis() - ts;
        if (diff < TimeUnit.MINUTES.toMillis(1))  return "Ahora mismo";
        if (diff < TimeUnit.HOURS.toMillis(1))
            return "Hace " + TimeUnit.MILLISECONDS.toMinutes(diff) + " min";
        if (diff < TimeUnit.DAYS.toMillis(1))
            return "Hace " + TimeUnit.MILLISECONDS.toHours(diff) + " h";
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        return days == 1 ? "Ayer" : "Hace " + days + " días";
    }

    // ── ViewHolder ────────────────────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {
        TextView title, message, time, icon;
        FrameLayout iconBg;
        View unreadDot, accentBar;

        VH(@NonNull View v) {
            super(v);
            title     = v.findViewById(R.id.notifTitle);
            message   = v.findViewById(R.id.notifMessage);
            time      = v.findViewById(R.id.notifTime);
            icon      = v.findViewById(R.id.notifIcon);
            iconBg    = v.findViewById(R.id.notifIconBg);
            unreadDot = v.findViewById(R.id.notifUnreadDot);
            accentBar = v.findViewById(R.id.notifAccentBar);
        }
    }
}
