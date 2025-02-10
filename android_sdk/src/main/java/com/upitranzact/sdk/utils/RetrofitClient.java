package com.upitranzact.sdk.utils;

import com.upitranzact.sdk.network.ApiService;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.Base64;

public class RetrofitClient {

    private static final String BASE_URL = "https://api.upitranzact.com/";

    private static Retrofit retrofit = null;

    public static ApiService getApiService(String publicKey, String secretKey) {
        String authHeader;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            authHeader = "Basic " + Base64.getEncoder().encodeToString((publicKey + ":" + secretKey).getBytes());
        } else {
            authHeader = null;
        }

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            assert authHeader != null;
            Request request = chain.request().newBuilder()
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                    .build();
            return chain.proceed(request);
        }).build();

        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit.create(ApiService.class);
    }
}

