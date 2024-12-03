package fr.iutlan.rendezvous;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

public class PatientActivity extends AppCompatActivity {
    private ListView appointmentListView;
    private List<String> appointmentList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        Button searchDoctorButton = findViewById(R.id.search_doctor_button);
        Button logoutButton = findViewById(R.id.logout_button);

        // Navigate to Search Doctor Screen
        searchDoctorButton.setOnClickListener(v -> {
            Intent intent = new Intent(PatientActivity.this, SearchDoctorActivity.class);
            startActivity(intent);
        });

        // Logout and return to the authentication screen
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(PatientActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the activity stack
            startActivity(intent);
        });

        // Initialize views
        appointmentListView = findViewById(R.id.appointment_list_view);

        // Initialize example appointments
        appointmentList = new ArrayList<>();
        appointmentList.add("Dr. Smith (Cardiologist) - Date: 2024-12-01, Time: 10:00-11:00 AM");
        appointmentList.add("Dr. Johnson (Dentist) - Date: 2024-12-02, Time: 2:00-3:00 PM");
        appointmentList.add("Dr. Brown (Dermatologist) - Date: 2024-12-03, Time: 4:00-5:00 PM");

        // Set up adapter for ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appointmentList);
        appointmentListView.setAdapter(adapter);

        // Set up item click listener for deletion
        appointmentListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void showDeleteConfirmation(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove item from list
                    appointmentList.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Appointment deleted.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
