package com.vesel.expensetracker;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    // money is saved in cents so the totals do not get rounding errors
    @ColumnInfo(name = "amount_cents")
    public long amountCents;

    public long timestamp;

    @Nullable
    public String street;
}