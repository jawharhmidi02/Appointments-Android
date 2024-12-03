package fr.iutlan.rendezvous;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchDoctorActivity extends AppCompatActivity {

    private EditText searchSpecialtyInput;
    private Button searchButton, backButton;
    private ListView doctorListView;

    private List<String> appointments; // To store selected appointments

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_doctor);

        // Initialize views
        searchSpecialtyInput = findViewById(R.id.search_specialty_input);
        searchButton = findViewById(R.id.search_button);
        backButton = findViewById(R.id.back_button);
        doctorListView = findViewById(R.id.doctor_list_view);

        // Initialize appointments list
        appointments = new ArrayList<>();

        // Set up listeners
        searchButton.setOnClickListener(v -> performSearch());
        backButton.setOnClickListener(v -> finish()); // Navigate back to the previous screen

        doctorListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDoctor = (String) parent.getItemAtPosition(position);
            addAppointment(selectedDoctor);
        });
    }

    private void performSearch() {
        String specialty = searchSpecialtyInput.getText().toString().trim();

        if (specialty.isEmpty()) {
            Toast.makeText(this, "Please enter a specialty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Example data - replace this with Firestore query later
        List<String> exampleDoctors = new ArrayList<>();
        exampleDoctors.add("Dr. Smith (Cardiologist) - Available: 10:00-11:00 AM");
        exampleDoctors.add("Dr. Johnson (Cardiologist) - Available: 2:00-3:00 PM");
        exampleDoctors.add("Dr. Brown (Cardiologist) - Available: 4:00-5:00 PM");

        // Filter example data
        List<String> filteredDoctors = new ArrayList<>();
        for (String doctor : exampleDoctors) {
            if (doctor.toLowerCase().contains(specialty.toLowerCase())) {
                filteredDoctors.add(doctor);
            }
        }

        if (filteredDoctors.isEmpty()) {
            Toast.makeText(this, "No doctors found for the specialty: " + specialty, Toast.LENGTH_SHORT).show();
        } else {
            // Populate ListView with results
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredDoctors);
            doctorListView.setAdapter(adapter);
        }
    }

    private void addAppointment(String doctor) {
        new AlertDialog.Builder(this)
                .setTitle("Add Appointment")
                .setMessage("Do you want to book an appointment with:\n" + doctor + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    appointments.add(doctor);
                    Toast.makeText(this, "Appointment added successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
