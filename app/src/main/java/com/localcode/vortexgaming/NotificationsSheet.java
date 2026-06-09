package com.localcode.vortexgaming;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.localcode.vortexgaming.adapters.NotificationsAdapter;
import com.localcode.vortexgaming.models.NotificationItem;
import com.localcode.vortexgaming.utils.NotificationsStore;

import java.util.ArrayList;
import java.util.List;

public class NotificationsSheet extends BottomSheetDialogFragment {

    // Callback fired when the unread count may have changed
    public interface OnBadgeChangedListener { void onBadgeChanged(); }
    private OnBadgeChangedListener badgeListener;
    public void setOnBadgeChangedListener(OnBadgeChangedListener l) { this.badgeListener = l; }

    // ── State ─────────────────────────────────────────────────────────────
    private NotificationsStore store;
    private List<NotificationItem> allItems    = new ArrayList<>();
    private List<NotificationItem> shownItems  = new ArrayList<>(); // what the adapter shows
    private NotificationsAdapter adapter;
    private String currentFilter = "all"; // "all" | "unread" | "release" | "request"

    // ── Views ─────────────────────────────────────────────────────────────
    private RecyclerView rv;
    private View emptyState;
    private TextView tvEmptyMessage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        store    = new NotificationsStore(requireContext());
        allItems = store.getAll();

        rv           = root.findViewById(R.id.rvNotifications);
        emptyState   = root.findViewById(R.id.notifEmpty);
        tvEmptyMessage = root.findViewById(R.id.tvEmptyMessage);

        // Adapter
        applyFilter(currentFilter);
        adapter = new NotificationsAdapter(shownItems, this::onItemClicked);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(adapter);

        // Filter chips
        ChipGroup chipGroup = root.findViewById(R.id.chipGroupFilter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipAll)      applyFilterAndRefresh("all");
            else if (id == R.id.chipUnread)   applyFilterAndRefresh("unread");
            else if (id == R.id.chipRelease)  applyFilterAndRefresh("release");
            else if (id == R.id.chipRequest)  applyFilterAndRefresh("request");
        });
        // Ensure "Todas" starts checked
        ((Chip) root.findViewById(R.id.chipAll)).setChecked(true);

        // "Marcar leídas"
        root.findViewById(R.id.tvMarkAllRead).setOnClickListener(v -> {
            store.markAllRead();
            for (NotificationItem n : allItems) n.read = true;
            applyFilterAndRefresh(currentFilter);
            notifyBadgeChanged();
        });

        // "Limpiar todo"
        root.findViewById(R.id.tvClearAll).setOnClickListener(v -> {
            allItems.clear();
            store.deleteAll();
            shownItems.clear();
            adapter.updateItems(shownItems);
            showEmptyState(true);
            notifyBadgeChanged();
        });

        // Swipe-to-delete
        attachSwipeToDelete(root);

        updateEmptyState();
    }

    // ── Filtering ─────────────────────────────────────────────────────────

    private void applyFilter(String filter) {
        currentFilter = filter;
        shownItems = new ArrayList<>();
        for (NotificationItem n : allItems) {
            switch (filter) {
                case "unread":  if (!n.read) shownItems.add(n);  break;
                case "release": if ("release".equals(n.type))  shownItems.add(n); break;
                case "request": if ("request".equals(n.type))  shownItems.add(n); break;
                default:        shownItems.add(n);
            }
        }
    }

    private void applyFilterAndRefresh(String filter) {
        applyFilter(filter);
        if (adapter != null) adapter.updateItems(shownItems);
        updateEmptyState();
    }

    // ── Item click → mark read ────────────────────────────────────────────

    private void onItemClicked(NotificationItem item, int position) {
        if (!item.read) {
            store.markRead(item.id);
            // Update in allItems list too
            for (NotificationItem n : allItems) {
                if (item.id.equals(n.id)) { n.read = true; break; }
            }
            adapter.markReadAt(position);

            // If filter is "unread", item should disappear after a brief pause
            if ("unread".equals(currentFilter)) {
                rv.postDelayed(() -> {
                    if (!isAdded()) return;
                    int idx = shownItems.indexOf(item);
                    if (idx >= 0) {
                        shownItems.remove(idx);
                        adapter.removeAt(idx);
                        updateEmptyState();
                    }
                }, 500);
            }

            notifyBadgeChanged();
        }
    }

    // ── Swipe-to-delete with undo ─────────────────────────────────────────

    private void attachSwipeToDelete(View root) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            private final ColorDrawable bg = new ColorDrawable(Color.parseColor("#FFEF4444"));

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder v,
                                  @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos < 0 || pos >= shownItems.size()) return;

                NotificationItem removed = adapter.removeAt(pos);
                shownItems.remove(removed);

                // Persist the delete
                allItems.remove(removed);
                store.delete(removed.id);
                updateEmptyState();
                notifyBadgeChanged();

                // Undo snackbar
                Snackbar.make(root, "Notificación eliminada", Snackbar.LENGTH_LONG)
                        .setAction("Deshacer", v -> {
                            // Re-add at front of allItems
                            allItems.add(0, removed);
                            store.add(removed);
                            // Re-add at pos in shownItems if still passes filter
                            boolean passes = passesFilter(removed, currentFilter);
                            if (passes) {
                                int restorePos = Math.min(pos, shownItems.size());
                                shownItems.add(restorePos, removed);
                                adapter.restoreAt(restorePos, removed);
                                updateEmptyState();
                                notifyBadgeChanged();
                            }
                        })
                        .setActionTextColor(Color.parseColor("#FF7B61FB"))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv,
                                    @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View item = vh.itemView;
                bg.setBounds(item.getRight() + (int) dX, item.getTop(),
                        item.getRight(), item.getBottom());
                bg.draw(c);
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }

    private boolean passesFilter(NotificationItem n, String filter) {
        switch (filter) {
            case "unread":  return !n.read;
            case "release": return "release".equals(n.type);
            case "request": return "request".equals(n.type);
            default:        return true;
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────

    private void updateEmptyState() {
        showEmptyState(shownItems.isEmpty());
    }

    private void showEmptyState(boolean empty) {
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        // Contextual empty message per filter
        if (empty && tvEmptyMessage != null) {
            switch (currentFilter) {
                case "unread":  tvEmptyMessage.setText("No tienes notificaciones sin leer"); break;
                case "release": tvEmptyMessage.setText("Sin alertas de lanzamientos"); break;
                case "request": tvEmptyMessage.setText("Sin actualizaciones de solicitudes"); break;
                default:        tvEmptyMessage.setText("Sin notificaciones por ahora"); break;
            }
        }
    }

    // ── Badge callback ────────────────────────────────────────────────────

    private void notifyBadgeChanged() {
        if (badgeListener != null) badgeListener.onBadgeChanged();
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        // Mark all currently visible items as read when user closes the sheet
        store.markAllRead();
        notifyBadgeChanged();
        super.onDismiss(dialog);
    }
}
