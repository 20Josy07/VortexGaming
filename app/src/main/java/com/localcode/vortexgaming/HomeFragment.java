package com.localcode.vortexgaming;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.localcode.vortexgaming.adapters.GameAdapter;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameResponse;
import com.localcode.vortexgaming.models.Genre;
import com.localcode.vortexgaming.models.GenreResponse;
import com.localcode.vortexgaming.models.NotificationItem;
import com.localcode.vortexgaming.utils.ContentFilter;
import com.localcode.vortexgaming.utils.NotificationHelper;
import com.localcode.vortexgaming.utils.NotificationsStore;
import com.localcode.vortexgaming.views.details.GameDetailActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvBadge;
    private NotificationsStore notifStore;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNewGames, rvTopRated, rvFastLaunch;
    private LinearLayout chipGroupGenres;

    private View cardFeatured;
    private ImageView ivFeaturedBg;
    private TextView tvFeaturedTitle, tvFeaturedRating, tvFeaturedMeta;
    private Game featuredGame;

    private String selectedGenre = null;
    private TextView activeChip = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        rvNewGames         = view.findViewById(R.id.rvNewGames);
        rvTopRated         = view.findViewById(R.id.rvTopRated);
        rvFastLaunch       = view.findViewById(R.id.rvFastLaunch);
        chipGroupGenres    = view.findViewById(R.id.chipGroupGenres);
        cardFeatured       = view.findViewById(R.id.cardFeatured);
        ivFeaturedBg       = view.findViewById(R.id.ivFeaturedBg);
        tvFeaturedTitle    = view.findViewById(R.id.tvFeaturedTitle);
        tvFeaturedRating   = view.findViewById(R.id.tvFeaturedRating);
        tvFeaturedMeta     = view.findViewById(R.id.tvFeaturedMeta);

        // Notification bell + badge
        notifStore = new NotificationsStore(requireContext());
        tvBadge = view.findViewById(R.id.tvBadge);
        updateBadge();

        view.findViewById(R.id.btnNotificationBell).setOnClickListener(v -> {
            NotificationsSheet sheet = new NotificationsSheet();
            sheet.setOnReadListener(this::updateBadge);
            sheet.show(getParentFragmentManager(), "notifs");
        });

        // Search bar → navigate to SearchFragment
        view.findViewById(R.id.searchCard).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_search));

        // Featured card → game detail
        cardFeatured.setOnClickListener(v -> {
            if (featuredGame == null) return;
            Intent intent = new Intent(requireContext(), GameDetailActivity.class);
            intent.putExtra("game_id", featuredGame.id);
            intent.putExtra("game_title", featuredGame.name);
            intent.putExtra("game_image", featuredGame.background_image);
            startActivity(intent);
        });

        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.purple_accent));
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);

        loadGenreChips();
        fetchData();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvBadge = null;
        swipeRefreshLayout = null;
        rvNewGames = null;
        rvTopRated = null;
        rvFastLaunch = null;
        chipGroupGenres = null;
        cardFeatured = null;
        ivFeaturedBg = null;
        tvFeaturedTitle = null;
        tvFeaturedRating = null;
        tvFeaturedMeta = null;
    }

    private boolean isViewReady() {
        return isAdded() && rvNewGames != null;
    }

    // ── GET /genres ──────────────────────────────────────────────────────────
    private void loadGenreChips() {
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        service.getGenres(getString(R.string.rawg_api_key)).enqueue(new Callback<GenreResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenreResponse> c, @NonNull Response<GenreResponse> r) {
                if (!isViewReady() || !r.isSuccessful() || r.body() == null) return;
                chipGroupGenres.removeAllViews();
                addChip("All", null, true);
                for (Genre genre : r.body().results) addChip(genre.name, String.valueOf(genre.id), false);
            }
            @Override public void onFailure(@NonNull Call<GenreResponse> c, @NonNull Throwable t) {}
        });
    }

    private void addChip(String label, String genreId, boolean isActive) {
        TextView chip = new TextView(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMarginEnd(8);
        chip.setLayoutParams(p);
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(14), dp(7), dp(14), dp(7));
        // Active chip: always white text on purple. Inactive: theme-aware text on surface
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                isActive ? R.color.chip_active_text : R.color.text_muted));
        chip.setBackground(ContextCompat.getDrawable(requireContext(),
                isActive ? R.drawable.bg_chip_active : R.drawable.bg_chip_default));
        if (isActive) activeChip = chip;

        chip.setOnClickListener(v -> {
            if (activeChip != null) {
                activeChip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_default));
                activeChip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
            }
            chip.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_active));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_active_text));
            activeChip = chip;
            selectedGenre = genreId;
            fetchData();
        });
        chipGroupGenres.addView(chip);
    }

    // ── GET /games — trending + top rated + upcoming ─────────────────────────
    private void fetchData() {
        if (!isAdded()) return;
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        String key = getString(R.string.rawg_api_key);

        service.getGames(key, "-metacritic", null, null, selectedGenre, null, 12)
                .enqueue(new Callback<GameResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (!isViewReady()) return;
                        if (r.isSuccessful() && r.body() != null && !r.body().results.isEmpty()) {
                            List<Game> list = ContentFilter.apply(requireContext(), r.body().results);
                            if (!list.isEmpty()) {
                                bindFeatured(list.get(0));
                                addGameNotification(list.get(0), "trending");
                            }
                            if (list.size() > 1) {
                                List<Game> rest = list.subList(1, list.size());
                                rvNewGames.setAdapter(new GameAdapter(rest));
                                if (!rest.isEmpty()) addGameNotification(rest.get(0), "release");
                            }
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                });

        service.getGames(key, "-rating", null, null, selectedGenre, null, 12)
                .enqueue(new Callback<GameResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                        if (!isViewReady()) return;
                        if (r.isSuccessful() && r.body() != null)
                            rvTopRated.setAdapter(new GameAdapter(ContentFilter.apply(requireContext(), r.body().results)));
                    }
                    @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {}
                });

        service.getGames(key, "-added", "2026-06-01,2026-12-31", null, selectedGenre, null, 10)
                .enqueue(new Callback<GameResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                        if (!isViewReady()) return;
                        if (r.isSuccessful() && r.body() != null)
                            rvFastLaunch.setAdapter(new GameAdapter(ContentFilter.apply(requireContext(), r.body().results)));
                    }
                    @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {}
                });
    }

    private void updateBadge() {
        if (tvBadge == null) return;
        int count = notifStore.getUnreadCount();
        if (count == 0) {
            tvBadge.setVisibility(View.GONE);
        } else {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    private void addGameNotification(Game game, String type) {
        if (game == null || game.name == null) return;
        String msg = type.equals("trending")
                ? "Está entre los más jugados ahora mismo"
                : "Nuevo lanzamiento disponible para explorar";
        notifStore.add(new NotificationItem("game_" + game.id, game.name, msg, type));
        updateBadge();
    }

    private void bindFeatured(Game game) {
        if (!isViewReady() || cardFeatured == null) return;
        featuredGame = game;
        cardFeatured.setVisibility(View.VISIBLE);
        tvFeaturedTitle.setText(game.name);
        tvFeaturedRating.setText(game.rating > 0 ? "★ " + String.format("%.1f", game.rating) : "");
        if (game.metacritic > 0) {
            tvFeaturedMeta.setVisibility(View.VISIBLE);
            tvFeaturedMeta.setText("MC " + game.metacritic);
        } else {
            tvFeaturedMeta.setVisibility(View.GONE);
        }
        Glide.with(this).load(game.background_image).into(ivFeaturedBg);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
