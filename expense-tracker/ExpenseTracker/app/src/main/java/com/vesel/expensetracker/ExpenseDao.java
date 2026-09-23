package com.vesel.expensetracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    // BETWEEN takes both ends, so the same query works for all the filters
    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    List<Expense> getInRange(long from, long to);

    // SUM gives back null when nothing matches, so it has to be Long and not long
    @Query("SELECT SUM(amount_cents) FROM expenses WHERE timestamp BETWEEN :from AND :to")
    Long getTotalInRange(long from, long to);

    @Query("SELECT * FROM expenses WHERE id = :id")
    Expense getById(int id);
}