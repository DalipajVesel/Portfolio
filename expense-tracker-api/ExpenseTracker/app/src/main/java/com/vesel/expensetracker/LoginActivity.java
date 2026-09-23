package com.vesel.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// only a username is needed, there is no password in this project
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editUsername = findViewById(R.id.editUsername);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    private String readUsername() {
        String username = editUsername.getText().toString().trim();

        if (username.isEmpty()) {
            editUsername.setError(getString(R.string.error_required));
            return null;
        }

        return username;
    }

    private void login() {
        String username = readUsername();

        if (username == null) {
            return;
        }

        ApiClient.getService().login(new AuthRequest(username)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LoginActivity.this, R.string.error_login, Toast.LENGTH_SHORT).show();
                    return;
                }

                TokenStore.save(LoginActivity.this, response.body().token);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void register() {
        String username = readUsername();

        if (username == null) {
            return;
        }

        ApiClient.getService().register(new AuthRequest(username)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, R.string.registered, Toast.LENGTH_SHORT).show();
                    // after registering, log in right away with the same username
                    login();
                } else {
                    Toast.makeText(LoginActivity.this, R.string.error_username_taken, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(LoginActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }
}