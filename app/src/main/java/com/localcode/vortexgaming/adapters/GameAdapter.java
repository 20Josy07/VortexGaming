package com.localcode.vortexgaming.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.views.details.GameDetailActivity;
import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private final List<Game> gameList;
    // Track the last animated position so we don't re-animate on scroll-back
    private int lastAnimatedPosition = -1;

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

        if (game.rating > 0) {
            holder.rating.setVisibility(View.VISIBLE);
            holder.rating.setText(String.format("★ %.1f", game.rating));
        } else {
            holder.rating.setVisibility(View.GONE);
        }

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

        // Staggered fade + slide-in (only for new items scrolling into view)
        if (position > lastAnimatedPosition) {
            lastAnimatedPosition = position;
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationX(40f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(350)
                    .setStartDelay(Math.min(position, 6) * 50L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Press scale → launch detail
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120)
                                .setInterpolator(new DecelerateInterpolator())
                                .withEndAction(() -> {
                                    Context ctx = v.getContext();
                                    Intent intent = new Intent(ctx, GameDetailActivity.class);
                                    intent.putExtra("game_id", game.id);
                                    intent.putExtra("game_title", game.name);
                                    intent.putExtra("game_image", game.background_image);
                                    if (ctx instanceof Activity) {
                                        ActivityOptionsCompat opts = ActivityOptionsCompat
                                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
                                        ctx.startActivity(intent, opts.toBundle());
                                    } else {
                                        ctx.startActivity(intent);
                                    }
                                }).start();
                    }).start();
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
            cover      = itemView.findViewById(R.id.img_game_cover);
            title      = itemView.findViewById(R.id.tv_game_title);
            rating     = itemView.findViewById(R.id.tv_rating);
            metacritic = itemView.findViewById(R.id.tv_metacritic);
        }
    }
}
