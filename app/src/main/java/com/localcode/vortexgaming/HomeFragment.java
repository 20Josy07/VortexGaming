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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);
        fetchData();
        
        return view;
    }

    private void fetchData() {
        swipeRefreshLayout.setRefreshing(true);
        RawgApiService service = RetrofitClient.getClient().create(RawgApiService.class);
        Call<GameResponse> call = service.getGames("TU_API_KEY", "-metacritic", null);
        
        call.enqueue(new Callback<GameResponse>() {
            @Override
            public void onResponse(Call<GameResponse> call, Response<GameResponse> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    RecyclerView rvTrending = getView().findViewById(R.id.rv_trending);
                    RecyclerView rvTopRated = getView().findViewById(R.id.rv_top_rated);
                    RecyclerView rvUpcoming = getView().findViewById(R.id.rv_upcoming);
                    
                    rvTrending.setAdapter(new GameAdapter(response.body().results));
                    rvTopRated.setAdapter(new GameAdapter(response.body().results));
                    rvUpcoming.setAdapter(new GameAdapter(response.body().results));
                }
            }

            @Override
            public void onFailure(Call<GameResponse> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }
}
