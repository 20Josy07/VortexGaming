package com.localcode.vortexgaming;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.transition.MaterialFadeThrough;
import com.localcode.vortexgaming.adapters.GameCompactAdapter;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.api.VortexApiService;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameRequestBody;
import com.localcode.vortexgaming.models.GameRequestResponse;
import com.localcode.vortexgaming.models.GameResponse;
import com.localcode.vortexgaming.utils.ContentFilter;
import com.localcode.vortexgaming.utils.NotificationHelper;
import com.localcode.vortexgaming.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestFragment extends Fragment {

    private RawgApiService service;
    private String apiKey;

    // Views
    private RecyclerView rvGameResults;
    private View cardResults, cardSelectedGame, cardDlcSection, cardNoDlc, viewEmptyState;
    private ImageView ivSelectedGameBackdrop;
    private TextView tvSelectedGameTitle, tvSelectedGameRating;
    private MaterialAutoCompleteTextView actvExpansion;
    private ProgressBar progressDlc;
    private Button btnRequest;

    // Step indicator views
    private FrameLayout stepCircle1, stepCircle2, stepCircle3;
    private TextView stepNum1, stepNum2, stepNum3;
    private TextView stepLabel1, stepLabel2, stepLabel3;
    private View stepLine1, stepLine2;

    // State
    private GameCompactAdapter gameAdapter;
    private List<Game> additionsList = new ArrayList<>();
    private Game currentGame;
    private int selectedDlcIndex = -1; // -1 = nothing selected

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
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        service = RetrofitClient.getClient().create(RawgApiService.class);
        apiKey  = getString(R.string.rawg_api_key);

        // Bind step indicator
        stepCircle1 = view.findViewById(R.id.stepCircle1);
        stepCircle2 = view.findViewById(R.id.stepCircle2);
        stepCircle3 = view.findViewById(R.id.stepCircle3);
        stepNum1    = view.findViewById(R.id.stepNum1);
        stepNum2    = view.findViewById(R.id.stepNum2);
        stepNum3    = view.findViewById(R.id.stepNum3);
        stepLabel1  = view.findViewById(R.id.stepLabel1);
        stepLabel2  = view.findViewById(R.id.stepLabel2);
        stepLabel3  = view.findViewById(R.id.stepLabel3);
        stepLine1   = view.findViewById(R.id.stepLine1);
        stepLine2   = view.findViewById(R.id.stepLine2);

        // Bind content views
        rvGameResults        = view.findViewById(R.id.rvGameResults);
        cardResults          = view.findViewById(R.id.cardResults);
        cardSelectedGame     = view.findViewById(R.id.cardSelectedGame);
        cardDlcSection       = view.findViewById(R.id.cardDlcSection);
        cardNoDlc            = view.findViewById(R.id.cardNoDlc);
        viewEmptyState       = view.findViewById(R.id.viewEmptyState);
        ivSelectedGameBackdrop = view.findViewById(R.id.ivSelectedGameBackdrop);
        tvSelectedGameTitle  = view.findViewById(R.id.tvSelectedGameTitle);
        tvSelectedGameRating = view.findViewById(R.id.tvSelectedGameRating);
        actvExpansion        = view.findViewById(R.id.actvExpansion);
        progressDlc          = view.findViewById(R.id.progressDlc);
        btnRequest           = view.findViewById(R.id.btn_request);

        // Search results recycler
        gameAdapter = new GameCompactAdapter(new ArrayList<>(), this::onGameSelected);
        rvGameResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGameResults.setAdapter(gameAdapter);

        // Search bar
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchViewRequest);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) { searchGames(q); return true; }
            @Override
            public boolean onQueryTextChange(String text) {
                if (text.length() >= 2) searchGames(text);
                else if (text.isEmpty()) hideResults();
                return true;
            }
        });

        // Clear selection button (✕)
        view.findViewById(R.id.btnClearSelection).setOnClickListener(v -> {
            v.animate().rotation(90f).setDuration(180).withEndAction(() -> {
                v.setRotation(0f);
                clearSelection(searchView);
            }).start();
        });

        // DLC dropdown selection
        actvExpansion.setOnItemClickListener((parent, v, position, id) -> {
            selectedDlcIndex = position;
            setStep(3);
            updateButtonState();
        });

        // Submit button
        btnRequest.setOnClickListener(v -> {
            if (currentGame == null) return;
            if (selectedDlcIndex < 0 || selectedDlcIndex >= additionsList.size()) {
                Toast.makeText(getContext(), "Selecciona una expansión válida", Toast.LENGTH_SHORT).show();
                return;
            }
            Game dlc = additionsList.get(selectedDlcIndex);
            saveRequestToApi(currentGame, dlc);
        });

        setStep(1);
        return view;
    }

    // ── Step indicator ────────────────────────────────────────────────────
    /**
     * step=1 → searching (only step1 active)
     * step=2 → game picked, picking DLC (step1 done, step2 active)
     * step=3 → DLC selected, ready to submit (step1&2 done, step3 active)
     */
    private void setStep(int step) {
        // Step 1
        boolean s1Done = step > 1;
        setStepCircle(stepCircle1, stepNum1, s1Done ? "✓" : "1",
                s1Done || step == 1, "#FFFFFF");
        stepLabel1.setTextColor(getResColor(step == 1 ? R.color.purple_accent : R.color.text_muted));
        stepLine1.setBackgroundColor(getResColor(s1Done ? R.color.purple_accent : R.color.stroke_subtle));

        // Step 2
        boolean s2Done = step > 2;
        setStepCircle(stepCircle2, stepNum2, s2Done ? "✓" : "2",
                s2Done || step == 2, s2Done || step == 2 ? "#FFFFFF" : null);
        stepLabel2.setTextColor(getResColor(step == 2 ? R.color.purple_accent : R.color.text_muted));
        stepLabel2.setTypeface(null, step == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        stepLine2.setBackgroundColor(getResColor(s2Done ? R.color.purple_accent : R.color.stroke_subtle));

        // Step 3
        setStepCircle(stepCircle3, stepNum3, "3",
                step == 3, step == 3 ? "#FFFFFF" : null);
        stepLabel3.setTextColor(getResColor(step == 3 ? R.color.purple_accent : R.color.text_muted));
        stepLabel3.setTypeface(null, step == 3 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void setStepCircle(FrameLayout circle, TextView num, String text,
                                boolean active, @Nullable String textColor) {
        circle.setBackground(androidx.core.content.ContextCompat.getDrawable(requireContext(),
                active ? R.drawable.bg_step_active : R.drawable.bg_step_inactive));
        num.setText(text);
        num.setTextColor(textColor != null
                ? android.graphics.Color.parseColor(textColor)
                : getResColor(R.color.text_muted));

        // Animate circle scale on activation
        circle.animate().scaleX(active ? 1.1f : 1f).scaleY(active ? 1.1f : 1f)
                .setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
    }

    // ── Button state ──────────────────────────────────────────────────────
    private void updateButtonState() {
        boolean ready = currentGame != null && selectedDlcIndex >= 0;
        btnRequest.setEnabled(ready);
        btnRequest.animate().alpha(ready ? 1f : 0.45f).setDuration(200).start();
        if (currentGame == null) {
            btnRequest.setText("Selecciona un juego primero");
        } else if (selectedDlcIndex < 0) {
            btnRequest.setText("Selecciona una expansión");
        } else {
            btnRequest.setText("Notificarme al lanzar  🔔");
        }
    }

    // ── Search ────────────────────────────────────────────────────────────
    private void searchGames(String query) {
        service.getGames(apiKey, null, null, query, null, true, 8)
                .enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                if (!isAdded()) return;
                if (r.isSuccessful() && r.body() != null) {
                    List<Game> results = ContentFilter.apply(requireContext(), r.body().results);
                    gameAdapter.updateItems(results);
                    showResults(!results.isEmpty());
                }
            }
            @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {}
        });
    }

    private void showResults(boolean show) {
        if (show) {
            cardResults.setVisibility(View.VISIBLE);
            cardResults.setAlpha(0f);
            cardResults.setTranslationY(-8f);
            cardResults.animate().alpha(1f).translationY(0f).setDuration(220)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            cardResults.setVisibility(View.GONE);
        }
    }

    private void hideResults() {
        cardResults.setVisibility(View.GONE);
        gameAdapter.updateItems(new ArrayList<>());
    }

    // ── Game selected ─────────────────────────────────────────────────────
    private void onGameSelected(Game game) {
        currentGame = game;
        selectedDlcIndex = -1;
        hideResults();

        // Show game backdrop card
        tvSelectedGameTitle.setText(game.name);
        tvSelectedGameRating.setText(game.rating > 0 ? String.format("★ %.1f", game.rating) : "");
        Glide.with(this).load(game.background_image)
                .placeholder(R.color.bg_surface)
                .into(ivSelectedGameBackdrop);

        revealCard(cardSelectedGame);

        // Hide DLC panels while fetching
        cardDlcSection.setVisibility(View.GONE);
        cardNoDlc.setVisibility(View.GONE);
        viewEmptyState.setVisibility(View.GONE);

        setStep(2);
        updateButtonState();

        // Fetch DLCs
        progressDlc.setVisibility(View.VISIBLE);
        cardDlcSection.setVisibility(View.VISIBLE);
        cardDlcSection.setAlpha(0.4f);

        service.getAdditions(game.id, apiKey).enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> c, @NonNull Response<GameResponse> r) {
                if (!isAdded()) return;
                progressDlc.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null && !r.body().results.isEmpty()) {
                    additionsList = r.body().results;
                    List<String> names = new ArrayList<>();
                    for (Game dlc : additionsList) names.add(dlc.name);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            names);
                    actvExpansion.setAdapter(adapter);
                    actvExpansion.setText("", false); // clear previous selection
                    cardDlcSection.animate().alpha(1f).setDuration(250).start();
                } else {
                    cardDlcSection.setVisibility(View.GONE);
                    revealCard(cardNoDlc);
                }
            }
            @Override
            public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressDlc.setVisibility(View.GONE);
                cardDlcSection.setVisibility(View.GONE);
                revealCard(cardNoDlc);
            }
        });
    }

    // ── Clear selection ───────────────────────────────────────────────────
    private void clearSelection(androidx.appcompat.widget.SearchView searchView) {
        currentGame = null;
        selectedDlcIndex = -1;
        additionsList.clear();

        // Animate out
        dismissCard(cardSelectedGame);
        dismissCard(cardDlcSection);
        dismissCard(cardNoDlc);

        // Show empty state again
        viewEmptyState.setAlpha(0f);
        viewEmptyState.setVisibility(View.VISIBLE);
        viewEmptyState.animate().alpha(1f).setDuration(300).start();

        searchView.setQuery("", false);
        searchView.requestFocus();

        setStep(1);
        updateButtonState();
    }

    // ── API submit ────────────────────────────────────────────────────────
    private void saveRequestToApi(Game game, Game expansion) {
        SessionManager session = new SessionManager(requireContext());
        String token = session.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(),
                    "Cierra sesión y vuelve a iniciarla.", Toast.LENGTH_LONG).show();
            scheduleNotification(expansion.name);
            return;
        }

        VortexApiService api = RetrofitClient.getVortexClient().create(VortexApiService.class);
        GameRequestBody body = new GameRequestBody(game.name, game.id, expansion.name, expansion.id);

        setButtonLoading(true);
        api.saveRequest("Bearer " + token, body).enqueue(new Callback<GameRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameRequestResponse> call,
                                   @NonNull Response<GameRequestResponse> response) {
                if (!isAdded()) return;
                setButtonLoading(false);
                if (response.isSuccessful()) {
                    showSuccess(expansion.name);
                } else if (response.code() == 401) {
                    Toast.makeText(getContext(),
                            "Sesión expirada. Cierra sesión e inicia de nuevo.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(),
                            "Error al guardar (código " + response.code() + ").",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<GameRequestResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                setButtonLoading(false);
                Toast.makeText(getContext(),
                        "API sin conexión. Notificación en 1 min.", Toast.LENGTH_LONG).show();
                scheduleNotification(expansion.name);
            }
        });
    }

    private void showSuccess(String dlcName) {
        Toast.makeText(getContext(), "¡Solicitud enviada! 🎉", Toast.LENGTH_SHORT).show();
        scheduleNotification(dlcName);
        // Briefly flash the button green-ish, then reset
        btnRequest.setText("¡Solicitud enviada! ✓");
        btnRequest.setEnabled(false);
        btnRequest.animate().alpha(1f).setDuration(100).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            // Reset to fresh state
            btnRequest.setText("Selecciona un juego primero");
            selectedDlcIndex = -1;
            updateButtonState();
        }, 2500);
    }

    private void setButtonLoading(boolean loading) {
        btnRequest.setEnabled(!loading);
        btnRequest.setText(loading ? "Enviando…" : "Notificarme al lanzar  🔔");
        btnRequest.animate().alpha(loading ? 0.7f : 1f).setDuration(150).start();
    }

    private void scheduleNotification(String expansionName) {
        if (getContext() == null) return;

        // Use AlarmManager so the notification fires even if the app is killed
        String storeId = "req_" + System.currentTimeMillis();
        NotificationReceiver.schedule(
                requireContext().getApplicationContext(),
                60_000L,                                     // delay: 1 minute
                "¡Solicitud aprobada!",
                expansionName + " ya está disponible para jugar.",
                NotificationHelper.CH_REQUESTS,
                NotificationHelper.ID_LAUNCH,
                storeId,
                "request"
        );
    }

    // ── Animation helpers ─────────────────────────────────────────────────
    private void revealCard(View card) {
        card.setVisibility(View.VISIBLE);
        card.setAlpha(0f);
        card.setTranslationY(20f);
        card.animate().alpha(1f).translationY(0f)
                .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void dismissCard(View card) {
        if (card.getVisibility() != View.VISIBLE) return;
        card.animate().alpha(0f).translationY(-10f).setDuration(180)
                .withEndAction(() -> {
                    card.setVisibility(View.GONE);
                    card.setTranslationY(0f);
                }).start();
    }

    private int getResColor(int colorRes) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorRes);
    }
}
