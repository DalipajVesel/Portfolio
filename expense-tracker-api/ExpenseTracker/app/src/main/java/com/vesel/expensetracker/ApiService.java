package com.vesel.expensetracker;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/auth/register")
    Call<Void> register(@Body AuthRequest request);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body AuthRequest request);

    @GET("api/expenses")
    Call<List<Expense>> getExpenses(@Header("Authorization") String token,
                                    @Query("from") long from,
                                    @Query("to") long to);

    @GET("api/expenses/{id}")
    Call<Expense> getExpense(@Header("Authorization") String token, @Path("id") int id);

    @POST("api/expenses")
    Call<Expense> addExpense(@Header("Authorization") String token, @Body Expense expense);

    @PUT("api/expenses/{id}")
    Call<Expense> updateExpense(@Header("Authorization") String token,
                                @Path("id") int id,
                                @Body Expense expense);

    @DELETE("api/expenses/{id}")
    Call<Void> deleteExpense(@Header("Authorization") String token, @Path("id") int id);
}