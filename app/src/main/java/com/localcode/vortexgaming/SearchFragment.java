package com.localcode.vortexgaming;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.localcode.vortexgaming.adapters.GameRowAdapter;
import com.localcode.vortexgaming.utils.ContentFilter;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private RecyclerView rvResults;
    private View layoutHint;
    private TextView tvHintMessage;
    private GameRowAdapter adapter;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        rvResults     = view.findViewById(R.id.rvSearchResults);
        layoutHint    = view.findViewById(R.id.layoutHint);
        tvHintMessage = view.findViewById(R.id.tvHintMessage);

        adapter = new GameRowAdapter(new ArrayList<>());
        rvResults.setAdapter(adapter);

        // Back button
        view.findViewById(R.id.btnSearchBack).setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        // SearchView — auto focus + keyboard
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchViewSearch);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { doSearch(q); return true; }
            @Override
            public boolean onQueryTextChange(String newText) {
                handler.removeCallbacks(searchRunnable);
                if (newText.trim().isEmpty()) {
                    showHint("Type to search games");
                    return true;
                }
                searchRunnable = () -> doSearch(newText.trim());
                handler.postDelayed(searchRunnable, 350);
                return true;
            }
        });

        // Open keyboard automatically
        searchView.post(() -> {
            searchView.requestFocus();
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(searchRunnable);
        // Hide keyboard on exit
        View focused = requireActivity().getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
        rvResults = null;
        layoutHint = null;
    }

    private void doSearch(String query) {
        if (!isAdded() || query.length() < 2) return;
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        service.getGames(getString(R.string.rawg_api_key), null, null, query, null, true, 25)
                .enqueue(new Callback<GameResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GameResponse> call, @NonNull Response<GameResponse> r) {
                        if (!isAdded() || rvResults == null) return;
                        if (r.isSuccessful() && r.body() != null) {
                            List<Game> results = ContentFilter.apply(requireContext(), r.body().results);
                            if (results.isEmpty()) {
                                showHint("No results for \"" + query + "\"");
                            } else {
                                showResults(results);
                            }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<GameResponse> c, @NonNull Throwable t) {
                        if (isAdded() && layoutHint != null) showHint("Connection error");
                    }
                });
    }

    private void showResults(List<Game> results) {
        if (layoutHint != null) layoutHint.setVisibility(View.GONE);
        if (rvResults != null) {
            rvResults.setVisibility(View.VISIBLE);
            adapter.updateItems(results);
        }
    }

    private void showHint(String message) {
        if (rvResults != null) rvResults.setVisibility(View.GONE);
        if (layoutHint != null) {
            layoutHint.setVisibility(View.VISIBLE);
            tvHintMessage.setText(message);
        }
    }
}
