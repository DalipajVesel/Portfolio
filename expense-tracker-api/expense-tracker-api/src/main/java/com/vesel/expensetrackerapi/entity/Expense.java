package com.vesel.expensetrackerapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    public String name;

    // money is saved in cents so the totals do not get rounding errors
    @Column(name = "amount_cents")
    public long amountCents;

    public long timestamp;

    public String street;

    // which user the expense belongs to, the server fills it from the token
    @Column(name = "user_id")
    public int userId;
}
