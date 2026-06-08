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

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final List<Game> gameList;

    public GameAdapter(List<Game> gameList) {
        this.gameList = gameList;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = gameList.get(position);

        holder.title.setText(game.name);

        // Rating badge
        if (game.rating > 0) {
            holder.rating.setVisibility(View.VISIBLE);
            holder.rating.setText(String.format("★ %.1f", game.rating));
        } else {
            holder.rating.setVisibility(View.GONE);
        }

        // Metacritic badge
        if (game.metacritic > 0) {
            holder.metacritic.setVisibility(View.VISIBLE);
            holder.metacritic.setText(String.valueOf(game.metacritic));
        } else {
            holder.metacritic.setVisibility(View.GONE);
        }

        Glide.with(holder.itemView.getContext())
                .load(game.background_image)
                .placeholder(R.color.bg_surface)
                .into(holder.cover);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), GameDetailActivity.class);
            intent.putExtra("game_id", game.id);
            intent.putExtra("game_title", game.name);
            intent.putExtra("game_image", game.background_image);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return gameList != null ? gameList.size() : 0;
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, rating, metacritic;

        GameViewHolder(View itemView) {
            super(itemView);
            cover       = itemView.findViewById(R.id.img_game_cover);
            title       = itemView.findViewById(R.id.tv_game_title);
            rating      = itemView.findViewById(R.id.tv_rating);
            metacritic  = itemView.findViewById(R.id.tv_metacritic);
        }
    }
}
