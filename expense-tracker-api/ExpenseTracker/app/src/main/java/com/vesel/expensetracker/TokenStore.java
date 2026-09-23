package com.vesel.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;

// the token is kept on the phone so the user does not log in every time
public class TokenStore {

    private static final String FILE = "login";
    private static final String KEY_TOKEN = "token";

    public static void save(Context context, String token) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    public static String get(Context context) {
        return prefs(context).getString(KEY_TOKEN, null);
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_TOKEN).apply();
    }

    public static String header(Context context) {
        return "Bearer " + get(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}