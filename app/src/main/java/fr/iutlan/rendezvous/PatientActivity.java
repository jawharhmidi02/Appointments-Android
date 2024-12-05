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

public class PatientActivity extends AppCompatActivity {
    private ListView appointmentListView;
    private List<String> appointmentList;
    private ArrayAdapter<String> adapter;
    private TextView noAppointmentsText;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);
        createNotificationChannel();

        // Initialize Firebase Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Button searchDoctorButton = findViewById(R.id.search_doctor_button);
        Button logoutButton = findViewById(R.id.logout_button);
        noAppointmentsText = findViewById(R.id.no_appointments_text);

        // Navigate to Search Doctor Screen
        searchDoctorButton.setOnClickListener(v -> {
            startActivity(new Intent(PatientActivity.this, SearchDoctorActivity.class));
        });

        // Logout
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(PatientActivity.this, AuthActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });

        // Initialize ListView and data
        appointmentListView = findViewById(R.id.appointment_list_view);
        appointmentList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, appointmentList);
        appointmentListView.setAdapter(adapter);

        // Load appointments from Firebase
        loadAppointments();

        // Handle appointment deletion
        appointmentListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void loadAppointments() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Fetch appointments where patientID matches the logged-in user
        db.collection("Appointment")
                .whereEqualTo("patientID", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No appointments found
                        appointmentListView.setVisibility(View.GONE);
                        noAppointmentsText.setVisibility(View.VISIBLE);
                    } else {
                        // Populate appointments
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            String doctorID = doc.getString("doctorID");
                            String availabilityID = doc.getString("availabilityID");

                            // Fetch doctor and availability details
                            fetchDoctorAndAvailabilityDetails(doctorID, availabilityID);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load appointments.", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmation(int position) {
//        String appointmentDetails = appointmentList.get(position);
//        String[] parts = appointmentDetails.split("\n");
//        String availabilityID = parts[parts.length - 1].split(": ")[1]; // Extract Availability ID
//
//        new AlertDialog.Builder(this)
//                .setTitle("Delete Appointment")
//                .setMessage("Are you sure you want to delete this appointment?")
//                .setPositiveButton("Yes", (dialog, which) -> {
//                    // Delete the appointment
//                    db.collection("Appointment")
//                            .whereEqualTo("availabilityID", availabilityID)
//                            .get()
//                            .addOnSuccessListener(querySnapshot -> {
//                                if (!querySnapshot.isEmpty()) {
//                                    String appointmentID = querySnapshot.getDocuments().get(0).getId();
//
//                                    db.collection("Appointment").document(appointmentID).delete()
//                                            .addOnSuccessListener(aVoid -> {
//                                                // Update availability status
//                                                db.collection("Availability").document(availabilityID)
//                                                        .update("status", "available")
//                                                        .addOnSuccessListener(aVoid1 -> {
//                                                            appointmentList.remove(position);
//                                                            adapter.notifyDataSetChanged();
//                                                            Toast.makeText(this, "Appointment deleted.", Toast.LENGTH_SHORT).show();
//                                                        });
//                                            })
//                                            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete appointment.", Toast.LENGTH_SHORT).show());
//                                }
//                            })
//                            .addOnFailureListener(e -> Toast.makeText(this, "Error finding appointment.", Toast.LENGTH_SHORT).show());
//                })
//                .setNegativeButton("No", null)
//                .show();
    }

    private void fetchDoctorAndAvailabilityDetails(String doctorID, String availabilityID) {
        // Fetch doctor details
        db.collection("Doctor")
                .document(doctorID)
                .get()
                .addOnSuccessListener(doctorDoc -> {
                    String userID = doctorDoc.getString("userID");
                    db.collection("User")
                            .document(userID)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String doctorName = userDoc.getString("name");
                                String profession = doctorDoc.getString("profession");

                                // Fetch availability details
                                db.collection("Availability")
                                        .document(availabilityID)
                                        .get()
                                        .addOnSuccessListener(availabilityDoc -> {
                                            String date = availabilityDoc.getString("date");
                                            String startHour = availabilityDoc.getString("startHour");
                                            String endHour = availabilityDoc.getString("endHour");

                                            // Add appointment to list
                                            String appointment = String.format("%s (%s) - Date: %s, Time: %s-%s",
                                                    doctorName, profession, date, startHour, endHour);
                                            appointmentList.add(appointment);
                                            adapter.notifyDataSetChanged();

                                            appointmentListView.setVisibility(View.VISIBLE);
                                            noAppointmentsText.setVisibility(View.GONE);
                                        });
                            });

                });
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
