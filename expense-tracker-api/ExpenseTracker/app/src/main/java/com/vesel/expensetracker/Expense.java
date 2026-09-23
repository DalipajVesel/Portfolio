package com.vesel.expensetracker;

public class Expense {

    public int id;

    public String name;

    // money is saved in cents so the totals do not get rounding errors
    public long amountCents;

    public long timestamp;

    public String street;

    public int userId;
}