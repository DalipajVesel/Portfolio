package com.vesel.expensetracker;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 10.0.2.2 is how the emulator sees the computer that runs the api
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private static ApiService service;

    public static synchronized ApiService getService() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(ApiService.class);
        }

        return service;
    }
}