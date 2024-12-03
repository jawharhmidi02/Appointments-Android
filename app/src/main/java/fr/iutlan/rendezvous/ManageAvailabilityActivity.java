package fr.iutlan.rendezvous;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageAvailabilityActivity extends AppCompatActivity {

    private EditText dateInput, timeInput;
    private Button addAvailabilityButton, backButton;
    private ListView availabilityListView;
    private List<String> availabilityList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_availability);

        // Initialize views
        dateInput = findViewById(R.id.date_input);
        timeInput = findViewById(R.id.time_input);
        addAvailabilityButton = findViewById(R.id.add_availability_button);
        availabilityListView = findViewById(R.id.availability_list_view);
        backButton = findViewById(R.id.back_button);
        // Initialize availability list
        availabilityList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, availabilityList);
        availabilityListView.setAdapter(adapter);

        // Add availability button click listener
        addAvailabilityButton.setOnClickListener(v -> addAvailability());

        backButton.setOnClickListener(v -> finish());
        // Set item click listener for deletion
        availabilityListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void addAvailability() {
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();

        // Validate inputs
        if (!isValidDate(date)) {
            Toast.makeText(this, "Invalid date format. Use yyyy-mm-dd.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidTimeRange(time)) {
            Toast.makeText(this, "Invalid time format. Use hh:mm-hh:mm.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to list
        String availability = "Date: " + date + ", Time: " + time;
        availabilityList.add(availability);
        adapter.notifyDataSetChanged();

        // Clear inputs
        dateInput.setText("");
        timeInput.setText("");

        Toast.makeText(this, "Availability added.", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmation(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Availability")
                .setMessage("Are you sure you want to delete this availability?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove item from list
                    availabilityList.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Availability deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private boolean isValidDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setLenient(false);
        try {
            Date parsedDate = dateFormat.parse(date);
            return parsedDate != null && !parsedDate.before(new Date()); // Ensure date is not in the past
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidTimeRange(String timeRange) {
        String[] times = timeRange.split("-");
        if (times.length != 2) return false;

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeFormat.setLenient(false);
        try {
            Date startTime = timeFormat.parse(times[0].trim());
            Date endTime = timeFormat.parse(times[1].trim());
            return startTime != null && endTime != null && startTime.before(endTime);
        } catch (ParseException e) {
            return false;
        }
    }
}
