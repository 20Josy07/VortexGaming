package com.localcode.vortexgaming.api;

import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RawgApiService {
    @GET("games")
    Call<GameResponse> getGames(
            @Query("key") String apiKey,
            @Query("ordering") String ordering,
            @Query("dates") String dates
    );
    @GET("games/{id}")
    Call<Game> getGameDetail(
            @retrofit2.http.Path("id") int gameId,
            @Query("key") String apiKey
    );
}
