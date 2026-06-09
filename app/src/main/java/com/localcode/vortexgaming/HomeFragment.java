package com.localcode.vortexgaming;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
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
import com.google.android.material.transition.MaterialFadeThrough;
import com.localcode.vortexgaming.adapters.GameAdapter;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameResponse;
import com.localcode.vortexgaming.models.Genre;
import com.localcode.vortexgaming.models.GenreResponse;
import com.localcode.vortexgaming.models.NotificationItem;
import com.localcode.vortexgaming.utils.ContentFilter;
import com.localcode.vortexgaming.utils.NotificationsStore;
import com.localcode.vortexgaming.views.details.GameDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    // ── Badge ─────────────────────────────────────────────────────────────
    private TextView tvBadge;
    private NotificationsStore notifStore;

    // ── Layout refs ───────────────────────────────────────────────────────
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvNewGames, rvTopRated, rvFastLaunch;
    private LinearLayout chipGroupGenres;
    private View cardFeatured;

    // Hero card inner views
    private ImageView ivFeaturedBg, ivFeaturedBgOverlay;
    private TextView tvFeaturedTitle, tvFeaturedRating, tvFeaturedMeta;
    private LinearLayout dotsContainer;
    private View featuredProgressBar;

    // Section containers (for staggered entry animation)
    private View sectionTrending, sectionTopRated, sectionComingSoon;

    // ── State ─────────────────────────────────────────────────────────────
    private String selectedGenre = null;
    private TextView activeChip  = null;

    // ── Carousel ──────────────────────────────────────────────────────────
    private final List<Game> featuredGames        = new ArrayList<>();
    private int               featuredIndex        = 0;
    private final Handler     carouselHandler      = new Handler(Looper.getMainLooper());
    private Runnable          carouselRunnable;
    private static final long INTERVAL_MS          = 5000L;

    // Animations running on the hero card
    private ObjectAnimator kenBurnsAnim;
    private ValueAnimator  progressAnim;

    // Swipe gesture
    private GestureDetector gestureDetector;

    // ── Transition ────────────────────────────────────────────────────────
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Bind views
        swipeRefreshLayout    = view.findViewById(R.id.swipe_refresh);
        rvNewGames            = view.findViewById(R.id.rvNewGames);
        rvTopRated            = view.findViewById(R.id.rvTopRated);
        rvFastLaunch          = view.findViewById(R.id.rvFastLaunch);
        chipGroupGenres       = view.findViewById(R.id.chipGroupGenres);
        cardFeatured          = view.findViewById(R.id.cardFeatured);
        ivFeaturedBg          = view.findViewById(R.id.ivFeaturedBg);
        ivFeaturedBgOverlay   = view.findViewById(R.id.ivFeaturedBgOverlay);
        tvFeaturedTitle       = view.findViewById(R.id.tvFeaturedTitle);
        tvFeaturedRating      = view.findViewById(R.id.tvFeaturedRating);
        tvFeaturedMeta        = view.findViewById(R.id.tvFeaturedMeta);
        dotsContainer         = view.findViewById(R.id.dotsContainer);
        featuredProgressBar   = view.findViewById(R.id.featuredProgressBar);
        sectionTrending       = view.findViewById(R.id.sectionTrending);
        sectionTopRated       = view.findViewById(R.id.sectionTopRated);
        sectionComingSoon     = view.findViewById(R.id.sectionComingSoon);

        // Sections start invisible for staggered entry
        prepareForEntry(sectionTrending);
        prepareForEntry(sectionTopRated);
        prepareForEntry(sectionComingSoon);
        prepareForEntry(cardFeatured);

        // Notification bell
        notifStore = new NotificationsStore(requireContext());
        tvBadge    = view.findViewById(R.id.tvBadge);
        updateBadge();
        view.findViewById(R.id.btnNotificationBell).setOnClickListener(v -> openNotificationsSheet(v));

        // Search bar
        view.findViewById(R.id.searchCard).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_search));

        // Hero card: tap → open game detail
        cardFeatured.setOnClickListener(v -> {
            if (featuredGames.isEmpty()) return;
            Game game = featuredGames.get(featuredIndex);
            Intent intent = new Intent(requireContext(), GameDetailActivity.class);
            intent.putExtra("game_id",    game.id);
            intent.putExtra("game_title", game.name);
            intent.putExtra("game_image", game.background_image);
            cardFeatured.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90)
                    .withEndAction(() -> cardFeatured.animate()
                            .scaleX(1f).scaleY(1f).setDuration(110)
                            .withEndAction(() -> startActivity(intent)).start())
                    .start();
        });

        // Swipe left/right to manually change slide
        gestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    private static final float SWIPE_THRESHOLD    = 60f;
                    private static final float SWIPE_VEL_THRESHOLD = 80f;
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float vX, float vY) {
                        if (e1 == null || e2 == null) return false;
                        float dx = e2.getX() - e1.getX();
                        if (Math.abs(dx) > SWIPE_THRESHOLD && Math.abs(vX) > SWIPE_VEL_THRESHOLD) {
                            jumpBy(dx < 0 ? 1 : -1);
                            return true;
                        }
                        return false;
                    }
                });
        cardFeatured.setOnTouchListener((v, ev) -> {
            gestureDetector.onTouchEvent(ev);
            return false; // let click listener through
        });

        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.purple_accent));
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);

        loadGenreChips();
        fetchData();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!featuredGames.isEmpty()) startCarousel();

        // Refresh badge whenever the user returns to this fragment
        if (notifStore != null) updateBadge();

        // Auto-open sheet when app was launched via a notification tap
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).consumeOpenNotifications()) {
                openNotificationsSheet(null);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopEverything();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopEverything();
        tvBadge = null; swipeRefreshLayout = null;
        rvNewGames = null; rvTopRated = null; rvFastLaunch = null;
        chipGroupGenres = null;
        cardFeatured = null; ivFeaturedBg = null; ivFeaturedBgOverlay = null;
        tvFeaturedTitle = null; tvFeaturedRating = null; tvFeaturedMeta = null;
        dotsContainer = null; featuredProgressBar = null;
        sectionTrending = null; sectionTopRated = null; sectionComingSoon = null;
    }

    private boolean isReady() { return isAdded() && ivFeaturedBg != null; }

    // ── Genre chips ───────────────────────────────────────────────────────
    private void loadGenreChips() {
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        service.getGenres(getString(R.string.rawg_api_key))
                .enqueue(new Callback<GenreResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenreResponse> c,
                                   @NonNull Response<GenreResponse> r) {
                if (!isReady() || !r.isSuccessful() || r.body() == null) return;
                chipGroupGenres.removeAllViews();
                addChip("All", null, true);
                long delay = 30;
                for (Genre g : r.body().results) {
                    TextView chip = buildChip(g.name, String.valueOf(g.id), false);
                    chip.setAlpha(0f);
                    chipGroupGenres.addView(chip);
                    chip.animate().alpha(1f).setDuration(280).setStartDelay(delay).start();
                    delay += 35;
                }
            }
            @Override public void onFailure(@NonNull Call<GenreResponse> c, @NonNull Throwable t) {}
        });
    }

    private void addChip(String label, String genreId, boolean active) {
        chipGroupGenres.addView(buildChip(label, genreId, active));
    }

    private TextView buildChip(String label, String genreId, boolean active) {
        TextView chip = new TextView(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMarginEnd(8);
        chip.setLayoutParams(p);
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setPadding(dp(14), dp(7), dp(14), dp(7));
        applyChipState(chip, active);
        if (active) activeChip = chip;

        chip.setOnClickListener(v -> {
            if (activeChip != null) applyChipState(activeChip, false);
            applyChipState(chip, true);
            chip.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70)
                    .withEndAction(() -> chip.animate().scaleX(1f).scaleY(1f)
                            .setDuration(130).setInterpolator(new DecelerateInterpolator()).start())
                    .start();
            activeChip   = chip;
            selectedGenre = genreId;
            fetchData();
        });
        return chip;
    }

    private void applyChipState(TextView chip, boolean active) {
        chip.setBackground(ContextCompat.getDrawable(requireContext(),
                active ? R.drawable.bg_chip_active : R.drawable.bg_chip_default));
        chip.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.chip_active_text : R.color.text_muted));
    }

    // ── Data fetch ────────────────────────────────────────────────────────
    private void fetchData() {
        if (!isAdded()) return;
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        RawgApiService svc = RetrofitClient.getClient().create(RawgApiService.class);
        String key = getString(R.string.rawg_api_key);

        svc.getGames(key, "-metacritic", null, null, selectedGenre, null, 12)
                .enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (!isReady()) return;
                if (!r.isSuccessful() || r.body() == null || r.body().results.isEmpty()) return;

                List<Game> all = ContentFilter.apply(requireContext(), r.body().results);
                if (all.isEmpty()) return;

                // Top-5 → carousel
                stopEverything();
                featuredGames.clear();
                featuredGames.addAll(all.subList(0, Math.min(5, all.size())));
                featuredIndex = 0;

                // Show card immediately, bind first game
                cardFeatured.setVisibility(View.VISIBLE);
                bindImmediate(featuredGames.get(0));
                buildDots();
                animateEntry(cardFeatured, 0);
                startKenBurns();
                startProgressBar();
                startCarousel();

                // Remainder → Trending list
                List<Game> rest = all.size() > 5 ? all.subList(5, all.size()) : all;
                rvNewGames.setAdapter(new GameAdapter(rest));
                animateEntry(sectionTrending, 110);

                addGameNotification(all.get(0), "trending");
            }
            @Override
            public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });

        svc.getGames(key, "-rating", null, null, selectedGenre, null, 12)
                .enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                if (!isReady()) return;
                if (r.isSuccessful() && r.body() != null) {
                    rvTopRated.setAdapter(new GameAdapter(
                            ContentFilter.apply(requireContext(), r.body().results)));
                    animateEntry(sectionTopRated, 190);
                }
            }
            @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {}
        });

        svc.getGames(key, "-added", "2026-06-01,2026-12-31", null, selectedGenre, null, 10)
                .enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                if (!isReady()) return;
                if (r.isSuccessful() && r.body() != null) {
                    rvFastLaunch.setAdapter(new GameAdapter(
                            ContentFilter.apply(requireContext(), r.body().results)));
                    animateEntry(sectionComingSoon, 270);
                }
            }
            @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {}
        });
    }

    // ── Carousel timer ────────────────────────────────────────────────────
    private void startCarousel() {
        if (featuredGames.size() <= 1) return;
        carouselRunnable = () -> {
            if (!isReady() || featuredGames.isEmpty()) return;
            featuredIndex = (featuredIndex + 1) % featuredGames.size();
            crossfadeTo(featuredGames.get(featuredIndex));
            carouselHandler.postDelayed(carouselRunnable, INTERVAL_MS);
        };
        carouselHandler.postDelayed(carouselRunnable, INTERVAL_MS);
    }

    private void stopCarousel() {
        carouselHandler.removeCallbacksAndMessages(null);
        carouselRunnable = null;
    }

    /** Jump ±1 with user interaction: restarts the 5s timer */
    private void jumpBy(int direction) {
        if (featuredGames.isEmpty()) return;
        stopEverything();
        featuredIndex = (featuredIndex + direction + featuredGames.size()) % featuredGames.size();
        crossfadeTo(featuredGames.get(featuredIndex));
        startCarousel();
    }

    /** Jump directly to a specific dot index */
    private void jumpTo(int index) {
        if (index == featuredIndex || featuredGames.isEmpty()) return;
        stopEverything();
        featuredIndex = index;
        crossfadeTo(featuredGames.get(featuredIndex));
        startCarousel();
    }

    // ── Double-buffer crossfade ───────────────────────────────────────────
    /**
     * Crossfade strategy (no blank frame):
     *  1. Load next image into ivFeaturedBgOverlay (invisible, on top of current)
     *  2. Fade ivFeaturedBgOverlay → 1 (current image is still visible beneath)
     *  3. On complete: copy drawable to ivFeaturedBg, hide overlay instantly
     *  4. Start Ken Burns on ivFeaturedBg, start progress bar
     */
    private void crossfadeTo(Game game) {
        if (!isReady()) return;

        cancelAnimators();
        resetProgressBar();

        // Prep overlay
        ivFeaturedBgOverlay.setAlpha(0f);
        ivFeaturedBgOverlay.setScaleX(1f);
        ivFeaturedBgOverlay.setScaleY(1f);
        Glide.with(this).load(game.background_image).into(ivFeaturedBgOverlay);

        // Short delay so Glide can populate from cache, then fade in
        carouselHandler.postDelayed(() -> {
            if (!isReady()) return;
            ivFeaturedBgOverlay.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        if (!isReady()) return;
                        // Swap layers: background takes the image, overlay hides
                        ivFeaturedBg.setImageDrawable(ivFeaturedBgOverlay.getDrawable());
                        ivFeaturedBg.setScaleX(1f);
                        ivFeaturedBg.setScaleY(1f);
                        ivFeaturedBgOverlay.setAlpha(0f);
                        startKenBurns();
                        startProgressBar();
                    }).start();
        }, 60L);

        // Text: slide-up + fade
        tvFeaturedTitle.animate()
                .alpha(0f).translationY(-8f).setDuration(180)
                .withEndAction(() -> {
                    if (!isReady()) return;
                    tvFeaturedTitle.setText(game.name);
                    tvFeaturedTitle.setTranslationY(10f);
                    tvFeaturedTitle.animate()
                            .alpha(1f).translationY(0f).setDuration(260)
                            .setInterpolator(new DecelerateInterpolator()).start();
                }).start();

        // Rating: simple fade
        tvFeaturedRating.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> {
                    if (!isReady()) return;
                    tvFeaturedRating.setText(
                            game.rating > 0 ? String.format("★ %.1f", game.rating) : "");
                    tvFeaturedRating.animate().alpha(1f).setDuration(200).start();
                }).start();

        // Metacritic badge
        if (tvFeaturedMeta != null) {
            if (game.metacritic > 0) {
                tvFeaturedMeta.setVisibility(View.VISIBLE);
                tvFeaturedMeta.setText("MC " + game.metacritic);
            } else {
                tvFeaturedMeta.setVisibility(View.GONE);
            }
        }

        updateDots();
    }

    // ── Ken Burns effect ──────────────────────────────────────────────────
    /** Subtle slow zoom on the background image over the full interval */
    private void startKenBurns() {
        if (ivFeaturedBg == null) return;
        ivFeaturedBg.setScaleX(1f);
        ivFeaturedBg.setScaleY(1f);
        kenBurnsAnim = ObjectAnimator.ofPropertyValuesHolder(ivFeaturedBg,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.07f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.07f));
        kenBurnsAnim.setDuration(INTERVAL_MS + 600);
        kenBurnsAnim.setInterpolator(new LinearInterpolator());
        kenBurnsAnim.start();
    }

    // ── Progress bar ──────────────────────────────────────────────────────
    private void startProgressBar() {
        if (featuredProgressBar == null) return;
        featuredProgressBar.post(() -> {
            if (featuredProgressBar == null) return;
            View parent = (View) featuredProgressBar.getParent();
            int maxW = parent.getWidth();
            progressAnim = ValueAnimator.ofInt(0, maxW);
            progressAnim.setDuration(INTERVAL_MS);
            progressAnim.setInterpolator(new LinearInterpolator());
            progressAnim.addUpdateListener(a -> {
                if (featuredProgressBar == null) return;
                ViewGroup.LayoutParams lp = featuredProgressBar.getLayoutParams();
                lp.width = (int) a.getAnimatedValue();
                featuredProgressBar.setLayoutParams(lp);
            });
            progressAnim.start();
        });
    }

    private void resetProgressBar() {
        if (featuredProgressBar == null) return;
        ViewGroup.LayoutParams lp = featuredProgressBar.getLayoutParams();
        lp.width = 0;
        featuredProgressBar.setLayoutParams(lp);
    }

    // ── Dot indicators ────────────────────────────────────────────────────
    private void buildDots() {
        if (dotsContainer == null) return;
        dotsContainer.removeAllViews();
        int dotH = dp(6);
        for (int i = 0; i < featuredGames.size(); i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(6), dotH);
            lp.setMarginStart(dp(5));
            dot.setLayoutParams(lp);
            dot.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot_inactive));
            dotsContainer.addView(dot);

            final int idx = i;
            dot.setOnClickListener(v -> jumpTo(idx));
        }
        // Animate first dot to pill
        if (dotsContainer.getChildCount() > 0)
            animateDotToPill(dotsContainer.getChildAt(0), true);
    }

    private void updateDots() {
        if (dotsContainer == null) return;
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            animateDotToPill(dotsContainer.getChildAt(i), i == featuredIndex);
        }
    }

    /** Animates a dot's width between pill (20dp) and circle (6dp) */
    private void animateDotToPill(View dot, boolean toActive) {
        if (dot == null) return;
        dot.setBackground(ContextCompat.getDrawable(requireContext(),
                toActive ? R.drawable.bg_dot_active : R.drawable.bg_dot_inactive));
        int fromW = dot.getLayoutParams().width;
        int toW   = dp(toActive ? 20 : 6);
        if (fromW == toW) return;

        ValueAnimator anim = ValueAnimator.ofInt(fromW, toW);
        anim.setDuration(220);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = (int) a.getAnimatedValue();
            dot.setLayoutParams(lp);
        });
        anim.start();
    }

    // ── Bind (first load, no crossfade needed) ────────────────────────────
    private void bindImmediate(Game game) {
        if (!isReady()) return;
        tvFeaturedTitle.setText(game.name);
        tvFeaturedRating.setText(game.rating > 0 ? String.format("★ %.1f", game.rating) : "");
        if (game.metacritic > 0) {
            tvFeaturedMeta.setVisibility(View.VISIBLE);
            tvFeaturedMeta.setText("MC " + game.metacritic);
        } else {
            tvFeaturedMeta.setVisibility(View.GONE);
        }
        Glide.with(this).load(game.background_image).into(ivFeaturedBg);
    }

    // ── Cancel helpers ────────────────────────────────────────────────────
    private void cancelAnimators() {
        if (kenBurnsAnim != null) { kenBurnsAnim.cancel(); kenBurnsAnim = null; }
        if (progressAnim != null) { progressAnim.cancel(); progressAnim = null; }
    }

    private void stopEverything() {
        stopCarousel();
        cancelAnimators();
    }

    // ── Notifications sheet ───────────────────────────────────────────────
    private void openNotificationsSheet(@androidx.annotation.Nullable View bellView) {
        // Prevent double-open if sheet is already showing
        if (getParentFragmentManager().findFragmentByTag("notifs") != null) return;

        if (bellView != null) {
            bellView.animate().scaleX(1.28f).scaleY(1.28f).setDuration(120)
                    .withEndAction(() -> bellView.animate()
                            .scaleX(1f).scaleY(1f).setDuration(110).start())
                    .start();
        }
        NotificationsSheet sheet = new NotificationsSheet();
        sheet.setOnBadgeChangedListener(this::updateBadge);
        sheet.show(getParentFragmentManager(), "notifs");
    }

    // ── Badge ─────────────────────────────────────────────────────────────
    private void updateBadge() {
        if (tvBadge == null) return;
        int count = notifStore.getUnreadCount();
        tvBadge.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        if (count > 0) tvBadge.setText(count > 9 ? "9+" : String.valueOf(count));
    }

    private void addGameNotification(Game game, String type) {
        if (game == null || game.name == null) return;
        notifStore.add(new NotificationItem("game_" + game.id, game.name,
                "Está entre los más jugados ahora mismo", type));
        updateBadge();
    }

    // ── Animation helpers ─────────────────────────────────────────────────
    private void prepareForEntry(View v) {
        if (v == null) return;
        v.setAlpha(0f);
        v.setTranslationY(44f);
    }

    private void animateEntry(View v, long delayMs) {
        if (v == null) return;
        v.animate().cancel();
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).translationY(0f)
                .setDuration(420).setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(1.8f))
                .start();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
