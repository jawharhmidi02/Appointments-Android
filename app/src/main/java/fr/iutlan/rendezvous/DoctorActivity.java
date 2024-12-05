package fr.iutlan.rendezvous;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DoctorActivity extends AppCompatActivity {
    private ListView appointmentListView;
    private TextView noAppointmentsText;
    private List<String> appointmentList;
    private ArrayAdapter<String> adapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        // Firebase initialization
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Buttons and ListView
        Button searchDoctorButton = findViewById(R.id.search_doctor_button);
        Button logoutButton = findViewById(R.id.logout_button);
        Button manageAvailabilityButton = findViewById(R.id.manage_availability_button);
        Button viewPatientAppointmentsButton = findViewById(R.id.view_patient_appointments_button);
        appointmentListView = findViewById(R.id.appointment_list_view);
        noAppointmentsText = findViewById(R.id.no_appointments_text);

        // Initialize appointment list and adapter
        appointmentList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appointmentList);
        appointmentListView.setAdapter(adapter);

        // Set up button listeners
        manageAvailabilityButton.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorActivity.this, ManageAvailabilityActivity.class);
            startActivity(intent);
        });

        searchDoctorButton.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorActivity.this, SearchDoctorActivity.class);
            startActivity(intent);
        });

        viewPatientAppointmentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(DoctorActivity.this, PatientAppointmentsActivity.class);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(DoctorActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear activity stack
            startActivity(intent);
        });

        // Load doctor's own appointments
        loadDoctorAppointments();

        createNotificationChannel();

        // Handle appointment deletion
        appointmentListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void loadDoctorAppointments() {
        String currentDoctorID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Query appointments where the current doctor is the patient
        db.collection("Appointment")
                .whereEqualTo("patientID", currentDoctorID)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    appointmentList.clear();

                    if (querySnapshot.isEmpty()) {
                        noAppointmentsText.setVisibility(View.VISIBLE);
                        appointmentListView.setVisibility(View.GONE);
                    } else {
                        noAppointmentsText.setVisibility(View.GONE);
                        appointmentListView.setVisibility(View.VISIBLE);

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            String doctorID = doc.getString("doctorID");
                            String availabilityID = doc.getString("availabilityID");

                            // Fetch availability details
                            assert availabilityID != null;
                            db.collection("Availability").document(availabilityID)
                                    .get()
                                    .addOnSuccessListener(availabilityDoc -> {
                                        if (availabilityDoc.exists()) {
                                            String date = availabilityDoc.getString("date");
                                            String startHour = availabilityDoc.getString("startHour");
                                            String endHour = availabilityDoc.getString("endHour");
                                            db.collection("User").document(availabilityID)
                                                    .get()
                                                    .addOnSuccessListener(userDoc -> {
                                                        if(userDoc.exists()) {
                                                            String doctorName = userDoc.getString("name");
                                                            String appointmentDetails = String.format(
                                                                    "Appointment with Dr. %s\nDate: %s, Time: %s-%s\nAvailabilityID: %s",
                                                                    doctorName, date, startHour, endHour, availabilityID
                                                            );
                                                            appointmentList.add(appointmentDetails);
                                                            adapter.notifyDataSetChanged();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading appointments.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmation(int position) {
        String appointmentDetails = appointmentList.get(position);
        String[] parts = appointmentDetails.split("\n");
        String availabilityID = parts[parts.length - 1].split(": ")[1]; // Extract Availability ID

        new AlertDialog.Builder(this)
                .setTitle("Delete Appointment")
                .setMessage("Are you sure you want to delete this appointment?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete the appointment
                    db.collection("Appointment")
                            .whereEqualTo("availabilityID", availabilityID)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String appointmentID = querySnapshot.getDocuments().get(0).getId();

                                    db.collection("Appointment").document(appointmentID).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                // Update availability status
                                                db.collection("Availability").document(availabilityID)
                                                        .update("status", "available")
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            appointmentList.remove(position);
                                                            adapter.notifyDataSetChanged();
                                                            Toast.makeText(this, "Appointment deleted.", Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete appointment.", Toast.LENGTH_SHORT).show());
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Error finding appointment.", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }


    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "appointment_channel";
            String channelName = "Appointment Reminders";
            String channelDescription = "Notifies users about their appointments";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

}
