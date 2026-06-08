package com.localcode.vortexgaming.api;

import com.localcode.vortexgaming.models.AuthRequest;
import com.localcode.vortexgaming.models.AuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @POST("register")
    Call<AuthResponse> register(@Body AuthRequest request);
}
