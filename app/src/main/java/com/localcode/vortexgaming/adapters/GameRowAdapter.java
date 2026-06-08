package com.localcode.vortexgaming.adapters;

import android.content.Intent;
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
import com.localcode.vortexgaming.views.details.GameDetailActivity;
import java.util.List;

public class GameRowAdapter extends RecyclerView.Adapter<GameRowAdapter.VH> {

    private List<Game> items;

    public GameRowAdapter(List<Game> items) { this.items = items; }

    public void updateItems(List<Game> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Game g = items.get(position);
        h.title.setText(g.name);

        if (g.rating > 0) {
            h.rating.setVisibility(View.VISIBLE);
            h.rating.setText(String.format("★ %.1f", g.rating));
        } else {
            h.rating.setVisibility(View.GONE);
        }

        if (g.metacritic > 0) {
            h.metacritic.setVisibility(View.VISIBLE);
            h.metacritic.setText("MC " + g.metacritic);
        } else {
            h.metacritic.setVisibility(View.GONE);
        }

        if (g.released != null && !g.released.isEmpty()) {
            h.released.setVisibility(View.VISIBLE);
            h.released.setText(g.released);
        } else {
            h.released.setVisibility(View.GONE);
        }

        Glide.with(h.cover.getContext())
                .load(g.background_image)
                .placeholder(R.color.bg_surface)
                .into(h.cover);

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), GameDetailActivity.class);
            intent.putExtra("game_id", g.id);
            intent.putExtra("game_title", g.name);
            intent.putExtra("game_image", g.background_image);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, rating, metacritic, released;
        VH(View v) {
            super(v);
            cover       = v.findViewById(R.id.img_row_cover);
            title       = v.findViewById(R.id.tv_row_title);
            rating      = v.findViewById(R.id.tv_row_rating);
            metacritic  = v.findViewById(R.id.tv_row_metacritic);
            released    = v.findViewById(R.id.tv_row_released);
        }
    }
}
