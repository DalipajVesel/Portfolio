package com.vesel.expensetracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault());

    private ExpenseDao dao;
    private FusedLocationProviderClient locationClient;
    private EditText editName;
    private EditText editAmount;
    private EditText editStreet;
    private Button buttonDateTime;
    private Button buttonSave;
    private Expense existing;
    private long timestamp;
    private String currentStreet;

    // asks for the location permission and gets the location if the user says yes
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean granted) {
                            if (granted) {
                                fetchLocation();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_expense);

        dao = AppDatabase.getInstance(this).expenseDao();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        TextView textTitle = findViewById(R.id.textTitle);
        TextView labelStreet = findViewById(R.id.labelStreet);
        TextView labelDateTime = findViewById(R.id.labelDateTime);
        editName = findViewById(R.id.editName);
        editAmount = findViewById(R.id.editAmount);
        editStreet = findViewById(R.id.editStreet);
        buttonDateTime = findViewById(R.id.buttonDateTime);
        buttonSave = findViewById(R.id.buttonSave);
        Button buttonCancel = findViewById(R.id.buttonCancel);
        Button buttonDelete = findViewById(R.id.buttonDelete);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDelete();
            }
        });

        buttonDateTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // no id means a new expense, an id means a row was tapped to edit it
        int expenseId = getIntent().getIntExtra(EXTRA_EXPENSE_ID, -1);

        if (expenseId == -1) {
            textTitle.setText(R.string.title_add_expense);
            // on a new expense the time and the street are taken automatically
            timestamp = System.currentTimeMillis();
            requestLocation();
        } else {
            textTitle.setText(R.string.title_edit_expense);
            labelStreet.setVisibility(View.VISIBLE);
            editStreet.setVisibility(View.VISIBLE);
            labelDateTime.setVisibility(View.VISIBLE);
            buttonDateTime.setVisibility(View.VISIBLE);
            buttonDelete.setVisibility(View.VISIBLE);
            buttonSave.setEnabled(false);
            loadExpense(expenseId);
        }
    }

    // reads the expense from the database and fills the fields with it
    private void loadExpense(int id) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Expense expense = dao.getById(id);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        existing = expense;
                        timestamp = expense.timestamp;
                        editName.setText(expense.name);
                        // put a dot in it so the amount can be read back when saving
                        editAmount.setText(String.format(Locale.US, "%.2f",
                                expense.amountCents / 100.0));
                        editStreet.setText(expense.street);
                        buttonDateTime.setText(dateFormat.format(new Date(timestamp)));
                        buttonSave.setEnabled(true);
                    }
                });
            }
        });
    }

    // first the date is picked and then the time
    private void showDatePicker() {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(timestamp);

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                showTimePicker(year, month, dayOfMonth);
            }
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH),
                current.get(Calendar.DAY_OF_MONTH)).show();
    }

    // builds the timestamp again from the date and the time that were picked
    private void showTimePicker(int year, int month, int dayOfMonth) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(timestamp);

        new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                picked.set(Calendar.MILLISECOND, 0);
                timestamp = picked.getTimeInMillis();
                buttonDateTime.setText(dateFormat.format(new Date(timestamp)));
            }
        }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show();
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fetchLocation();
    }

    // one location fix, it is only used to find the street
    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellation = new CancellationTokenSource();
        locationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.getToken())
                // tied to the activity so it does not run after the activity is gone
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            return;
                        }

                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    }
                });
    }

    private void reverseGeocode(double latitude, double longitude) {
        if (!Geocoder.isPresent()) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                String found = null;

                try {
                    Geocoder geocoder =
                            new Geocoder(AddEditExpenseActivity.this, Locale.getDefault());

                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                    if (addresses != null && !addresses.isEmpty()) {
                        found = addresses.get(0).getThoroughfare();
                    }
                } catch (IOException e) {
                    // if the geocoder fails just leave the street empty and let the save continue
                }

                String street = found;

                // back on the main thread so save() finds the value
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentStreet = street;
                    }
                });
            }
        });
    }

    private void save() {
        String name = editName.getText().toString().trim();

        if (name.isEmpty()) {
            editName.setError(getString(R.string.error_required));
            return;
        }

        // the amount is typed with a dot and saved as cents
        long cents;

        try {
            String raw = editAmount.getText().toString().trim();
            cents = Math.round(Double.parseDouble(raw) * 100);
        } catch (NumberFormatException e) {
            editAmount.setError(getString(R.string.error_invalid_amount));
            return;
        }

        if (cents <= 0) {
            editAmount.setError(getString(R.string.error_invalid_amount));
            return;
        }

        boolean isNew = existing == null;

        Expense expense;

        if (isNew) {
            expense = new Expense();
            expense.street = currentStreet;
        } else {
            expense = existing;

            String typed = editStreet.getText().toString().trim();

            if (typed.isEmpty()) {
                expense.street = null;
            } else {
                expense.street = typed;
            }
        }

        expense.name = name;
        expense.amountCents = cents;
        expense.timestamp = timestamp;

        // Room does not allow writing on the main thread, so the save runs on the executor
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (isNew) {
                    dao.insert(expense);
                } else {
                    dao.update(expense);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AddEditExpenseActivity.this,
                                R.string.expense_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }
        });
    }

    // asks before deleting, the delete button is only on the edit screen
    private void confirmDelete() {
        if (existing == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                dao.delete(existing);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                });
                            }
                        });
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}