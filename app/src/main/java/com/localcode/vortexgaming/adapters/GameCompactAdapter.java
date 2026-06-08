package com.localcode.vortexgaming.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.models.Game;
import java.util.List;

public class GameCompactAdapter extends RecyclerView.Adapter<GameCompactAdapter.VH> {

    public interface OnGameClickListener {
        void onGameClick(Game game);
    }

    private List<Game> items;
    private final OnGameClickListener listener;

    public GameCompactAdapter(List<Game> items, OnGameClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<Game> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_compact, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Game game = items.get(position);
        holder.title.setText(game.name);
        Glide.with(holder.image.getContext())
                .load(game.background_image)
                .into(holder.image);
        holder.itemView.setOnClickListener(v -> listener.onGameClick(game));
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        VH(View v) {
            super(v);
            image = v.findViewById(R.id.img_game_thumb);
            title = v.findViewById(R.id.tv_game_name);
        }
    }
}
