package com.localcode.vortexgaming;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.api.RawgApiService;
import com.localcode.vortexgaming.models.GameResponse;
import com.localcode.vortexgaming.adapters.GameAdapter;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class HomeFragment extends Fragment {
    private SwipeRefreshLayout swipeRefreshLayout;
    private android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);
        fetchData();
        
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                searchHandler.removeCallbacks(searchRunnable);
                if (newText.length() >= 3) {
                    searchRunnable = () -> searchGames(newText);
                    searchHandler.postDelayed(searchRunnable, 500);
                }
                return true;
            }
        });
        
        return view;
    }

    private void searchGames(String query) {
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        service.getGames(getString(R.string.rawg_api_key), null, null).enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RecyclerView rv = getView().findViewById(R.id.rv_trending);
                    rv.setAdapter(new GameAdapter(response.body().results));
                }
            }
            @Override
            public void onFailure(Call<GameResponse> call, Throwable t) {}
        });
    }

    private void fetchData() {
        swipeRefreshLayout.setRefreshing(true);
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        String apiKey = getString(R.string.rawg_api_key);
        
        // Trending
        service.getGames(apiKey, "-metacritic", null).enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    RecyclerView rvTrending = getView().findViewById(R.id.rv_trending);
                    rvTrending.setAdapter(new GameAdapter(response.body().results));
                }
            }
            @Override
            public void onFailure(Call<GameResponse> call, Throwable t) { swipeRefreshLayout.setRefreshing(false); }
        });

        // Top Rated
        service.getGames(apiKey, "-rating", null).enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RecyclerView rvTopRated = getView().findViewById(R.id.rv_top_rated);
                    rvTopRated.setAdapter(new GameAdapter(response.body().results));
                }
            }
            @Override
            public void onFailure(Call<GameResponse> call, Throwable t) { }
        });

        // Upcoming
        service.getGames(apiKey, "-added", "2026-06-01,2026-12-31").enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RecyclerView rvUpcoming = getView().findViewById(R.id.rv_upcoming);
                    rvUpcoming.setAdapter(new GameAdapter(response.body().results));
                }
            }
            @Override
            public void onFailure(Call<GameResponse> call, Throwable t) { }
        });
    }
}
