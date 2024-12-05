package fr.iutlan.rendezvous;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchDoctorActivity extends AppCompatActivity {

    private EditText searchSpecialtyInput;
    private ListView doctorListView;
    private TextView noDoctorsText;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<String> doctorList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_doctor);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        searchSpecialtyInput = findViewById(R.id.search_specialty_input);
        Button searchButton = findViewById(R.id.search_button);
        Button backButton = findViewById(R.id.back_button);
        doctorListView = findViewById(R.id.doctor_list_view);
        noDoctorsText = findViewById(R.id.no_doctors_text);

        // Initialize doctor list and adapter
        doctorList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, doctorList);
        doctorListView.setAdapter(adapter);

        // Set up listeners
        searchButton.setOnClickListener(v -> performSearch());
        backButton.setOnClickListener(v -> finish()); // Navigate back to the previous screen

        // Handle doctor list item click
        doctorListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDoctorInfo = doctorList.get(position);
            bookAppointment(selectedDoctorInfo);
        });
    }

    @SuppressLint("SetTextI18n")
    private void performSearch() {
        String specialty = searchSpecialtyInput.getText().toString().trim();

        if (specialty.isEmpty()) {
            Toast.makeText(this, "Please enter a specialty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear previous results
        doctorList.clear();
        adapter.notifyDataSetChanged();
        noDoctorsText.setVisibility(View.GONE);
        doctorListView.setVisibility(View.GONE);

        // Query doctors by specialty
        db.collection("Doctor")
                .whereEqualTo("profession", specialty)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        noDoctorsText.setText("No doctors found for specialty: " + specialty);
                        noDoctorsText.setVisibility(View.VISIBLE);
                    } else {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doctorDoc : querySnapshot) {
                            String userID = doctorDoc.getString("userID");
                            Log.v("U", "userID from Doctor: " + userID);
                            String ID = doctorDoc.getId();
                            Log.v("U", "ID from Doctor: " + ID);
                            if (userID != null && !userID.isEmpty()) {
                                db.collection("User")  // Note: Changed to "Users" which is more common in Firebase
                                        .document(userID)
                                        .get()
                                        .addOnSuccessListener(userDoc -> {
                                            if (userDoc.exists()) {
                                                String doctorName = userDoc.getString("name");
                                                Log.v("U", "User name from Doctor: " + doctorName);
                                                if (doctorName != null && !doctorName.isEmpty()) {
                                                    fetchAvailableSlots(userID, doctorName);
                                                } else {
                                                    Log.v("U", "Doctor name is null or empty");
                                                }
                                            } else {
                                                Log.v("U", "No user document found for the given ID");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("U", "Error loading user details", e);
                                            Toast.makeText(this, "Error loading user details.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading doctors.", Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SetTextI18n")
    private void fetchAvailableSlots(String userID, String doctorName) {
        Log.v("U","User name from fetchAvailableSlots :"+doctorName);
        Log.v("U","User ID from fetchAvailableSlots :"+userID);

        db.collection("Availability")
                .whereEqualTo("doctorID", userID)
                .whereEqualTo("status", "available")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot availabilityDoc : querySnapshot) {
                            String date = availabilityDoc.getString("date");
                            String startHour = availabilityDoc.getString("startHour");
                            String endHour = availabilityDoc.getString("endHour");
                            String availabilityID = availabilityDoc.getId();

                            String info = String.format("%s: %s - %s to %s", doctorName, date, startHour, endHour);
                            doctorList.add(info + "|" + availabilityID + "|" + userID); // Append availability ID for booking
                            adapter.notifyDataSetChanged();
                            doctorListView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        noDoctorsText.setText("No available slots for " + doctorName);
                        noDoctorsText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading availability.", Toast.LENGTH_SHORT).show());
    }

    private void bookAppointment(String doctorInfo) {
        String[] details = doctorInfo.split("\\|");
        String[] doctorDetails = details[0].split(": ");
        Log.v("U", "Doctor Details0:"+ doctorDetails[0]);
        Log.v("U", "Doctor Details1:"+ doctorDetails[1]);
        String doctorName = doctorDetails[0];
        String[] dateDetails = doctorDetails[1].split(" - ");
        String date = dateDetails[0].trim();
        String Hour = dateDetails[1].substring(0, 5).trim();
        String availabilityID = details[1];
        String doctorID = details[2];
        String patientID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        new AlertDialog.Builder(this)
                .setTitle("Book Appointment")
                .setMessage("Do you want to book this slot?\n" + doctorName)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Add appointment to FireStore
                    db.collection("Appointment").add(new Appointment(patientID, availabilityID, doctorID))
                            .addOnSuccessListener(docRef -> {
                                // Update availability status
                                db.collection("Availability").document(availabilityID)
                                        .update("status", "taken")
                                        .addOnSuccessListener(aVoid -> {
                                            // Example call after saving an appointment
                                            // Format: YYYY-MM-DD
                                            // Format: HH:mm
                                            String title = "Appointment Reminder";
                                            String message = "You have an appointment scheduled tomorrow at " + Hour;

                                            NotificationScheduler.scheduleNotification(this, title, message, date, Hour);

                                            Toast.makeText(this, "Appointment booked successfully!", Toast.LENGTH_SHORT).show();
                                            doctorList.remove(doctorInfo);
                                            adapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update availability.", Toast.LENGTH_SHORT).show());
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to book appointment.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Appointment class to create Firestore document
    private static class Appointment {
        public String patientID;
        public String availabilityID;
        public String doctorID;

        public Appointment(String patientID, String availabilityID, String doctorID) {
            this.patientID = patientID;
            this.availabilityID = availabilityID;
            this.doctorID = doctorID;
        }
    }
}
