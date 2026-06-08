package com.localcode.vortexgaming;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestFragment extends Fragment {

    private RawgApiService service;
    private String apiKey;

    private RecyclerView rvGameResults;
    private GameCompactAdapter gameAdapter;
    private View cardAdditions;
    private View cardNoDlc;
    private ImageView ivSelectedGameCover;
    private TextView tvSelectedGame;
    private Spinner spinnerExpansions;
    private Button btnRequest;

    private List<Game> additionsList = new ArrayList<>();
    private Game currentGame;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        service = RetrofitClient.getClient().create(RawgApiService.class);
        apiKey  = getString(R.string.rawg_api_key);

        rvGameResults     = view.findViewById(R.id.rvGameResults);
        cardAdditions     = view.findViewById(R.id.cardAdditions);
        cardNoDlc         = view.findViewById(R.id.cardNoDlc);
        ivSelectedGameCover = view.findViewById(R.id.ivSelectedGameCover);
        tvSelectedGame    = view.findViewById(R.id.tvSelectedGame);
        spinnerExpansions = view.findViewById(R.id.spinner_expansions);
        btnRequest        = view.findViewById(R.id.btn_request);

        gameAdapter = new GameCompactAdapter(new ArrayList<>(), this::onGameSelected);
        rvGameResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGameResults.setAdapter(gameAdapter);

        // Search
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchViewRequest);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { searchGames(q); return true; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() >= 3) searchGames(newText);
                else if (newText.isEmpty()) clearResults();
                return true;
            }
        });

        // Clear/change selection
        view.findViewById(R.id.btnClearSelection).setOnClickListener(v -> clearSelection(searchView));

        // Request button — saves to API + schedules notification after 1 minute
        btnRequest.setOnClickListener(v -> {
            if (cardAdditions.getVisibility() != View.VISIBLE) {
                Toast.makeText(getContext(), "Primero selecciona un juego", Toast.LENGTH_SHORT).show();
                return;
            }
            if (spinnerExpansions.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Selecciona una expansión válida", Toast.LENGTH_SHORT).show();
                return;
            }
            Game selected = additionsList.get(spinnerExpansions.getSelectedItemPosition() - 1);
            saveRequestToApi(currentGame, selected);
        });

        return view;
    }

    private void saveRequestToApi(Game game, Game expansion) {
        SessionManager session = new SessionManager(requireContext());
        String token = session.getToken();

        // Token vacío → sesión antigua sin JWT, notificar y lanzar igual
        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(),
                    "Cierra sesión y vuelve a iniciarla para guardar solicitudes.",
                    Toast.LENGTH_LONG).show();
            scheduleNotification(expansion.name);
            return;
        }

        VortexApiService api = RetrofitClient.getVortexClient().create(VortexApiService.class);
        GameRequestBody body = new GameRequestBody(game.name, game.id, expansion.name, expansion.id);

        btnRequest.setEnabled(false);
        api.saveRequest("Bearer " + token, body).enqueue(new Callback<GameRequestResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameRequestResponse> call, @NonNull Response<GameRequestResponse> response) {
                if (!isAdded()) return;
                btnRequest.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(),
                            "¡Solicitud enviada! Notificación en 1 minuto.", Toast.LENGTH_LONG).show();
                    scheduleNotification(expansion.name);
                } else if (response.code() == 401) {
                    // Token expirado o inválido
                    Toast.makeText(getContext(),
                            "Sesión expirada. Cierra sesión e inicia de nuevo.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(),
                            "No se pudo guardar la solicitud (código " + response.code() + ").",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<GameRequestResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                btnRequest.setEnabled(true);
                // API no disponible: notificamos igual
                Toast.makeText(getContext(),
                        "API sin conexión. Notificación en 1 minuto.", Toast.LENGTH_LONG).show();
                scheduleNotification(expansion.name);
            }
        });
    }

    private void scheduleNotification(String expansionName) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getContext() != null) {
                NotificationHelper.show(requireContext(),
                        NotificationHelper.ID_LAUNCH,
                        "¡Solicitud aprobada!",
                        expansionName + " ya está disponible para jugar.");
            }
        }, 60_000L);
    }

    private void clearResults() {
        gameAdapter.updateItems(new ArrayList<>());
        rvGameResults.setVisibility(View.GONE);
    }

    private void clearSelection(androidx.appcompat.widget.SearchView searchView) {
        cardAdditions.setVisibility(View.GONE);
        cardNoDlc.setVisibility(View.GONE);
        currentGame = null;
        searchView.setQuery("", false);
        searchView.requestFocus();
    }

    // ── Endpoint 1: GET /games (búsqueda de juego base) ─────────────────────
    private void searchGames(String query) {
        service.getGames(apiKey, null, null, query, null, true, 8)
                .enqueue(new Callback<GameResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GameResponse> call, @NonNull Response<GameResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            List<Game> results = ContentFilter.apply(requireContext(), response.body().results);
                            gameAdapter.updateItems(results);
                            rvGameResults.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }
                    @Override public void onFailure(@NonNull Call<GameResponse> call, @NonNull Throwable t) {}
                });
    }

    // ── Endpoint 6: GET /games/{id}/additions ────────────────────────────────
    private void onGameSelected(Game game) {
        currentGame = game;
        rvGameResults.setVisibility(View.GONE);
        cardAdditions.setVisibility(View.GONE);
        cardNoDlc.setVisibility(View.GONE);

        tvSelectedGame.setText(game.name);
        Glide.with(this)
                .load(game.background_image)
                .placeholder(R.color.bg_main)
                .into(ivSelectedGameCover);

        service.getAdditions(game.id, apiKey).enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(@NonNull Call<GameResponse> call, @NonNull Response<GameResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().results.isEmpty()) {
                    additionsList = response.body().results;

                    List<String> names = new ArrayList<>();
                    names.add("Selecciona una expansión…");
                    for (Game addition : additionsList) names.add(addition.name);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(), android.R.layout.simple_spinner_dropdown_item, names);
                    spinnerExpansions.setAdapter(adapter);
                    cardAdditions.setVisibility(View.VISIBLE);
                } else {
                    cardNoDlc.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(@NonNull Call<GameResponse> call, @NonNull Throwable t) {
                if (isAdded()) cardNoDlc.setVisibility(View.VISIBLE);
            }
        });
    }
}
