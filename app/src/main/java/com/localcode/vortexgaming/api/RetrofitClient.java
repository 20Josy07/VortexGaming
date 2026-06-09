package com.localcode.vortexgaming.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL       = "https://api.rawg.io/api/";
    private static final String AUTH_BASE_URL  = "https://reqres.in/api/";
    public  static final String VORTEX_BASE_URL = "https://vortexgaming-api-production.up.railway.app/";

    private static Retrofit retrofit       = null;
    private static Retrofit authRetrofit   = null;
    private static Retrofit vortexRetrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getAuthClient() {
        if (authRetrofit == null) {
            authRetrofit = new Retrofit.Builder()
                    .baseUrl(AUTH_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return authRetrofit;
    }

    public static Retrofit getVortexClient() {
        if (vortexRetrofit == null) {
            vortexRetrofit = new Retrofit.Builder()
                    .baseUrl(VORTEX_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return vortexRetrofit;
    }

    /** Llama esto si cambias VORTEX_BASE_URL en tiempo de ejecución */
    public static void resetVortexClient() {
        vortexRetrofit = null;
    }
}
