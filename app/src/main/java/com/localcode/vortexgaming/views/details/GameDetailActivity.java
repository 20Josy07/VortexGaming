package com.localcode.vortexgaming.views.details;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.transition.platform.MaterialElevationScale;

import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.adapters.ScreenshotAdapter;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.Movie;
import com.localcode.vortexgaming.models.MovieResponse;
import com.localcode.vortexgaming.models.ScreenshotResponse;
import com.localcode.vortexgaming.utils.FavoritesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameDetailActivity extends AppCompatActivity {

    private int gameId;
    private RawgApiService service;
    private String apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Material elevation scale transition (enter = grow in, return = shrink out)
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setEnterTransition(new MaterialElevationScale(true));
        getWindow().setReturnTransition(new MaterialElevationScale(false));

        // Edge-to-edge: backdrop extends behind status bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_detail);

        gameId  = getIntent().getIntExtra("game_id", 0);
        String title    = getIntent().getStringExtra("game_title");
        String imageUrl = getIntent().getStringExtra("game_image");
        apiKey  = getString(R.string.rawg_api_key);
        service = RetrofitClient.getClient().create(RawgApiService.class);

        ImageView ivCover = findViewById(R.id.ivDetailBackdrop);
        TextView tvTitle  = findViewById(R.id.tvDetailGameTitle);

        tvTitle.setText(title);
        Glide.with(this).load(imageUrl).into(ivCover);

        setupInsets();
        setupFavorites();
        loadGameDetail();
        loadScreenshots();
        loadTrailer();
    }

    private void setupInsets() {
        // Push action buttons below status bar
        View actionBar = findViewById(R.id.actionBar);
        ViewCompat.setOnApplyWindowInsetsListener(actionBar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), top + dp(6), v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Push notify button above gesture nav bar
        View btnNotify = findViewById(R.id.btnNotifyAction);
        ViewCompat.setOnApplyWindowInsetsListener(btnNotify, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) v.getLayoutParams();
            params.bottomMargin = dp(16) + bottom;
            v.setLayoutParams(params);
            return insets;
        });
    }

    // ── Endpoint 2: GET /games/{id} ──────────────────────────────────────────
    private void loadGameDetail() {
        service.getGameDetail(gameId, apiKey).enqueue(new Callback<Game>() {
            @Override
            public void onResponse(@NonNull Call<Game> call, @NonNull Response<Game> response) {
                if (!isFinishing() && response.isSuccessful() && response.body() != null) {
                    Game g = response.body();
                    bindMeta(g);
                    bindDescription(g.description);
                }
            }
            @Override public void onFailure(@NonNull Call<Game> call, @NonNull Throwable t) {}
        });
    }

    private void bindMeta(Game g) {
        TextView tvRating     = findViewById(R.id.tvRating);
        TextView tvMetacritic = findViewById(R.id.tvMetacritic);
        TextView tvPlaytime   = findViewById(R.id.tvPlaytime);
        TextView tvReleased   = findViewById(R.id.tvReleased);

        tvRating.setText(g.rating > 0 ? String.format("★ %.1f", g.rating) : "★ —");
        tvMetacritic.setText(g.metacritic > 0 ? "MC " + g.metacritic : "MC —");
        tvPlaytime.setText(g.playtime > 0 ? "⏱ " + g.playtime + "h" : "⏱ —");
        tvReleased.setText(g.released != null ? "📅 " + g.released : "📅 —");
    }

    private void bindDescription(String html) {
        if (html == null || html.isEmpty()) return;
        TextView tvDesc = findViewById(R.id.tvGameDescription);
        tvDesc.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
    }

    // ── Endpoint 3: GET /games/{id}/screenshots ──────────────────────────────
    private void loadScreenshots() {
        service.getScreenshots(gameId, apiKey).enqueue(new Callback<ScreenshotResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScreenshotResponse> call, @NonNull Response<ScreenshotResponse> response) {
                if (isFinishing()) return;
                RecyclerView rv = findViewById(R.id.rvGameScreenshots);
                TextView tvNone = findViewById(R.id.tvNoScreenshots);
                boolean hasScreenshots = response.isSuccessful()
                        && response.body() != null
                        && response.body().results != null
                        && !response.body().results.isEmpty();
                if (hasScreenshots) {
                    rv.setVisibility(View.VISIBLE);
                    rv.setLayoutManager(new LinearLayoutManager(
                            GameDetailActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    rv.setAdapter(new ScreenshotAdapter(response.body().results));
                } else {
                    tvNone.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(@NonNull Call<ScreenshotResponse> call, @NonNull Throwable t) {
                if (!isFinishing()) {
                    TextView tvNone = findViewById(R.id.tvNoScreenshots);
                    tvNone.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // ── Endpoint 4: GET /games/{id}/movies ───────────────────────────────────
    private void loadTrailer() {
        service.getMovies(gameId, apiKey).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (isFinishing() || !response.isSuccessful()
                        || response.body() == null || response.body().results.isEmpty()) return;

                Movie movie = response.body().results.get(0);
                String videoUrl = movie.data != null ? movie.data.bestUrl() : null;
                if (videoUrl == null) return;

                View trailerContainer = findViewById(R.id.trailerContainer);
                ImageView ivThumb = findViewById(R.id.ivTrailerThumb);

                trailerContainer.setVisibility(View.VISIBLE);
                Glide.with(GameDetailActivity.this).load(movie.preview).into(ivThumb);

                String finalVideoUrl = videoUrl;
                trailerContainer.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalVideoUrl));
                    intent.setDataAndType(Uri.parse(finalVideoUrl), "video/mp4");
                    startActivity(Intent.createChooser(intent, "Play trailer"));
                });
            }
            @Override public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {}
        });
    }

    private void setupFavorites() {
        FavoritesManager favManager = new FavoritesManager(this);
        ImageButton fab = findViewById(R.id.btnFavorite);

        updateFavIcon(fab, favManager.isFavorite(String.valueOf(gameId)));

        fab.setOnClickListener(v -> {
            favManager.toggleFavorite(String.valueOf(gameId));
            updateFavIcon(fab, favManager.isFavorite(String.valueOf(gameId)));
        });

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void updateFavIcon(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
