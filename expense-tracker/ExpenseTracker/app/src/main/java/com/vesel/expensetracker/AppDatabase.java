package com.vesel.expensetracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Expense.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    // one database for the whole app, made the first time it is asked for
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "expenses.db")
                    // on a new version the table is made again from the start
                    .fallbackToDestructiveMigration(true)
                    .build();
        }
        return instance;
    }

    public abstract ExpenseDao expenseDao();
}