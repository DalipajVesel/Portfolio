package com.vesel.expensetracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements ExpenseAdapter.OnExpenseClickListener {

    private final SimpleDateFormat rangeFormat =
            new SimpleDateFormat("d MMM", Locale.getDefault());

    private ExpenseAdapter adapter;
    private TextView textTotal;
    private ChipGroup chipGroupFilters;
    private Chip chipCustom;

    private long rangeFrom;
    private long rangeTo;

    // the fixed ranges are counted from the current time every time the screen opens
    // the custom one keeps the dates the user picked
    private boolean customRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // without a token there is nothing to show, the login screen comes first
        if (TokenStore.get(this) == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        textTotal = findViewById(R.id.textTotal);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        chipCustom = findViewById(R.id.chipCustom);

        adapter = new ExpenseAdapter(this);
        RecyclerView recycler = findViewById(R.id.recyclerExpenses);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddEditExpenseActivity.class));
            }
        });

        // picking a chip changes the range and loads the list again
        chipGroupFilters.setOnCheckedStateChangeListener(
                new ChipGroup.OnCheckedStateChangeListener() {
                    @Override
                    public void onCheckedChanged(@NonNull ChipGroup group,
                                                 @NonNull List<Integer> checkedIds) {
                        if (checkedIds.isEmpty()) {
                            return;
                        }
                        int checkedId = checkedIds.get(0);
                        if (checkedId == R.id.chipCustom) {
                            showDateRangePicker();
                        } else {
                            customRange = false;
                            chipCustom.setText(R.string.filter_custom);
                            setRelativeRange(checkedId);
                            load();
                        }
                    }
                });

        setRelativeRange(R.id.chip24h);
    }

    // reloading here also covers coming back from add, edit and delete
    @Override
    protected void onResume() {
        super.onResume();

        if (TokenStore.get(this) == null) {
            return;
        }

        if (!customRange) {
            setRelativeRange(chipGroupFilters.getCheckedChipId());
        }
        load();
    }

    @Override
    public void onExpenseClick(Expense expense) {
        Intent intent = new Intent(this, AddEditExpenseActivity.class);
        intent.putExtra(AddEditExpenseActivity.EXTRA_EXPENSE_ID, expense.id);
        startActivity(intent);
    }

    // from now back one day, week, month or year, depending on the chip
    private void setRelativeRange(int checkedId) {
        Calendar calendar = Calendar.getInstance();
        rangeTo = calendar.getTimeInMillis();

        if (checkedId == R.id.chipWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, -7);
        } else if (checkedId == R.id.chipMonth) {
            calendar.add(Calendar.MONTH, -1);
        } else if (checkedId == R.id.chipYear) {
            calendar.add(Calendar.YEAR, -1);
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1);
        }
        rangeFrom = calendar.getTimeInMillis();
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText(R.string.date_range_title)
                        .build();

        picker.addOnPositiveButtonClickListener(
                new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
                    @Override
                    public void onPositiveButtonClick(Pair<Long, Long> selection) {
                        rangeFrom = startOfDayLocal(selection.first);
                        rangeTo = endOfDayLocal(selection.second);
                        customRange = true;
                        chipCustom.setText(rangeFormat.format(new Date(rangeFrom))
                                + " - " + rangeFormat.format(new Date(rangeTo)));
                        load();
                    }
                });

        // if the picker is closed without dates, go back to the 24h chip
        picker.addOnNegativeButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chipGroupFilters.check(R.id.chip24h);
            }
        });

        picker.addOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                chipGroupFilters.check(R.id.chip24h);
            }
        });

        picker.show(getSupportFragmentManager(), "date_range");
    }

    // the picker gives back midnight in UTC, so I take only the date and build it again in local time
    private long startOfDayLocal(long utcMidnight) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);

        Calendar local = Calendar.getInstance();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        local.set(Calendar.MILLISECOND, 0);
        return local.getTimeInMillis();
    }

    private long endOfDayLocal(long utcMidnight) {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(utcMidnight);

        Calendar local = Calendar.getInstance();
        local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        local.set(Calendar.MILLISECOND, 999);
        return local.getTimeInMillis();
    }

    // the list comes from the api, the total is added up here
    private void load() {
        ApiClient.getService().getExpenses(TokenStore.header(this), rangeFrom, rangeTo)
                .enqueue(new Callback<List<Expense>>() {
                    @Override
                    public void onResponse(Call<List<Expense>> call, Response<List<Expense>> response) {
                        // 401 or 403 means the token is not valid anymore, so log in again
                        if (response.code() == 401 || response.code() == 403) {
                            goToLogin();
                            return;
                        }

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(MainActivity.this, R.string.error_network,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<Expense> expenses = response.body();
                        adapter.setExpenses(expenses);

                        long totalCents = 0;
                        for (int i = 0; i < expenses.size(); i++) {
                            totalCents = totalCents + expenses.get(i).amountCents;
                        }

                        textTotal.setText(ExpenseAdapter.formatAmount(totalCents));
                    }

                    @Override
                    public void onFailure(Call<List<Expense>> call, Throwable t) {
                        Toast.makeText(MainActivity.this, R.string.error_network,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToLogin() {
        TokenStore.clear(this);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}