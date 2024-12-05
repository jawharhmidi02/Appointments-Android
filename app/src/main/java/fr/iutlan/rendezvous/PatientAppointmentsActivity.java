package fr.iutlan.rendezvous;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatientAppointmentsActivity extends AppCompatActivity {
    private ListView patientAppointmentListView;
    private List<String> patientAppointmentList;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;
    private TextView noAppointmentsText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_appointments);

        patientAppointmentListView = findViewById(R.id.patient_appointment_list_view);
        patientAppointmentList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, patientAppointmentList);
        patientAppointmentListView.setAdapter(adapter);
        Button backButton = findViewById(R.id.back_button);
        noAppointmentsText = findViewById(R.id.no_appointments_text);

        backButton.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadAppointments();

        patientAppointmentListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void loadAppointments() {
        String userId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("Appointment")
                .whereEqualTo("doctorID", userId) // Replace with actual doctor ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        patientAppointmentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String patientID = document.getString("patientID");
                            String availabilityID = document.getString("availabilityID");

                            // Fetch patient details
                            fetchPatientDetails(patientID, availabilityID, document.getId());
                        }
                        if (task.getResult().isEmpty()) {
                            noAppointmentsText.setVisibility(View.VISIBLE);
                            patientAppointmentListView.setVisibility(View.GONE);
                        } else {
                            noAppointmentsText.setVisibility(View.GONE);
                            patientAppointmentListView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(this, "Error loading appointments.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchPatientDetails(String patientID, String availabilityID, String appointmentID) {
        db.collection("User")
                .document(patientID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");
                        java.lang.Long phone = (java.lang.Long) documentSnapshot.get("phone");

                        String appointmentDetails = "Name: " + name + "\nEmail: " + email + "\nPhone: " + phone.toString();
                        patientAppointmentList.add(appointmentDetails);
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showDeleteConfirmation(int position) {
//        new AlertDialog.Builder(this)
//                .setTitle("Delete Appointment")
//                .setMessage("Are you sure you want to delete this appointment?")
//                .setPositiveButton("Yes", (dialog, which) -> {
//                    deleteAppointment(position);
//                })
//                .setNegativeButton("No", null)
//                .show();
    }

    private void deleteAppointment(int position) {
        String selectedAppointment = patientAppointmentList.get(position);
        db.collection("Appointment")
                .document("appointmentID") // Retrieve the correct appointment ID
                .delete()
                .addOnSuccessListener(aVoid -> {
                    patientAppointmentList.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Appointment deleted.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete appointment.", Toast.LENGTH_SHORT).show());
    }
}
