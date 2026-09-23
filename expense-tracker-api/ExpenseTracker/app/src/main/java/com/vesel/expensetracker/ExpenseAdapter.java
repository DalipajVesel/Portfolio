package com.vesel.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses = new ArrayList<>();
    private final OnExpenseClickListener listener;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());

    public ExpenseAdapter(OnExpenseClickListener listener) {
        this.listener = listener;
    }

    // the cents become a decimal only here, when the amount is shown
    public static String formatAmount(long amountCents) {
        return String.format(Locale.getDefault(), "%.2f €", amountCents / 100.0);
    }

    public void setExpenses(List<Expense> newExpenses) {
        expenses.clear();
        expenses.addAll(newExpenses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        final Expense expense = expenses.get(position);

        holder.textName.setText(expense.name);
        holder.textAmount.setText(formatAmount(expense.amountCents));

        String when = dateFormat.format(new Date(expense.timestamp));
        if (expense.street == null || expense.street.isEmpty()) {
            holder.textDetails.setText(when);
        } else {
            holder.textDetails.setText(expense.street + " · " + when);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onExpenseClick(expense);
            }
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    // keeps the views of one row so findViewById runs only once for each row
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {

        final TextView textName;
        final TextView textDetails;
        final TextView textAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textName);
            textDetails = itemView.findViewById(R.id.textDetails);
            textAmount = itemView.findViewById(R.id.textAmount);
        }
    }
}