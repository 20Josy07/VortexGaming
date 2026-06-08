package com.localcode.vortexgaming.api;

import com.localcode.vortexgaming.models.GameRequestBody;
import com.localcode.vortexgaming.models.GameRequestResponse;
import com.localcode.vortexgaming.models.LoginRequest;
import com.localcode.vortexgaming.models.RegisterRequest;
import com.localcode.vortexgaming.models.VortexAuthResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface VortexApiService {
    @POST("api/auth/register")
    Call<VortexAuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/login")
    Call<VortexAuthResponse> login(@Body LoginRequest request);

    @POST("api/requests")
    Call<GameRequestResponse> saveRequest(
            @Header("Authorization") String bearerToken,
            @Body GameRequestBody request
    );

    @GET("api/requests/mine")
    Call<GameRequestResponse> getMyRequests(
            @Header("Authorization") String bearerToken
    );
}
