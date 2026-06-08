package com.localcode.vortexgaming.api;

import com.localcode.vortexgaming.models.Game;
import com.localcode.vortexgaming.models.GameResponse;
import com.localcode.vortexgaming.models.GenreResponse;
import com.localcode.vortexgaming.models.MovieResponse;
import com.localcode.vortexgaming.models.ScreenshotResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RawgApiService {

    @GET("games")
    Call<GameResponse> getGames(
            @Query("key") String apiKey,
            @Query("ordering") String ordering,
            @Query("dates") String dates,
            @Query("search") String searchQuery,
            @Query("genres") String genres,
            @Query("search_precise") Boolean searchPrecise,
            @Query("page_size") Integer pageSize
    );

    @GET("games/{id}")
    Call<Game> getGameDetail(
            @Path("id") int gameId,
            @Query("key") String apiKey
    );

    @GET("games/{id}/screenshots")
    Call<ScreenshotResponse> getScreenshots(
            @Path("id") int gameId,
            @Query("key") String apiKey
    );

    @GET("games/{id}/movies")
    Call<MovieResponse> getMovies(
            @Path("id") int gameId,
            @Query("key") String apiKey
    );

    @GET("games/{id}/additions")
    Call<GameResponse> getAdditions(
            @Path("id") int gameId,
            @Query("key") String apiKey
    );

    @GET("genres")
    Call<GenreResponse> getGenres(
            @Query("key") String apiKey
    );
}
