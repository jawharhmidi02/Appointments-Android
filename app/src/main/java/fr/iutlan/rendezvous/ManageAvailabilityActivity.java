package fr.iutlan.rendezvous;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManageAvailabilityActivity extends AppCompatActivity {

    private EditText dateInput, timeInput;
    private List<String> availabilityList;
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_availability);

        // Initialize views
        dateInput = findViewById(R.id.date_input);
        timeInput = findViewById(R.id.time_input);
        Button addAvailabilityButton = findViewById(R.id.add_availability_button);
        ListView availabilityListView = findViewById(R.id.availability_list_view);
        Button backButton = findViewById(R.id.back_button);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize availability list and adapter
        availabilityList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, availabilityList);
        availabilityListView.setAdapter(adapter);

        // Load availability data
        loadAvailability();

        // Add availability button click listener
        addAvailabilityButton.setOnClickListener(v -> addAvailability());

        // Back button click listener
        backButton.setOnClickListener(v -> finish());

        // Set item click listener for deletion
        availabilityListView.setOnItemClickListener((parent, view, position, id) -> showDeleteConfirmation(position));
    }

    private void addAvailability() {
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Date and time cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] times = time.split("-");
        if (times.length != 2) {
            Toast.makeText(this, "Invalid time format. Use hh:mm-hh:mm.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        db.collection("Doctor").whereEqualTo("userID", userID).get().addOnSuccessListener(usersDoc -> {
            for(QueryDocumentSnapshot userDoc : usersDoc){
                String doctorID = userDoc.getId();
                String availabilityID = db.collection("Availability").document().getId(); // Generate unique ID
                Availability availability = new Availability(availabilityID, date, times[0].trim(), times[1].trim(), doctorID, "available");

                // Save to Firestore
                db.collection("Availability").document(availabilityID)
                        .set(availability)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Availability added.", Toast.LENGTH_SHORT).show();
                            dateInput.setText("");
                            timeInput.setText("");
                            loadAvailability();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to add availability.", Toast.LENGTH_SHORT).show());
                break;
            }
        });


    }

    private void loadAvailability() {
        String userID = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        db.collection("Doctor").whereEqualTo("userID", userID).get().addOnSuccessListener(docUsers -> {
            for(QueryDocumentSnapshot docUser : docUsers){
                String doctorID = docUser.getId();

                db.collection("Availability")
                        .whereEqualTo("doctorID", doctorID)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                availabilityList.clear();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String date = document.getString("date");
                                    String startHour = document.getString("startHour");
                                    String endHour = document.getString("endHour");
                                    String status = document.getString("status");

                                    if (date != null && startHour != null && endHour != null) {
                                        availabilityList.add("Date: " + date + ", Time: " + startHour + "-" + endHour + ", Status: " + status);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(this, "Error loading availability.", Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            }
        });
    }

    private void showDeleteConfirmation(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Availability")
                .setMessage("Are you sure you want to delete this availability?")
                .setPositiveButton("Yes", (dialog, which) -> deleteAvailability(position))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAvailability(int position) {
        String selectedItem = availabilityList.get(position);
        String[] parts = selectedItem.split(", Time:");
        String datePart = parts[0].split(": ")[1];

        db.collection("Availability")
                .whereEqualTo("date", datePart)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String status = document.getString("status");
                            if(status.equals("taken")){
                                db.collection("Appointment").whereEqualTo("availabilityID", document.getId()).get().addOnCompleteListener(tasks -> {
                                   for(QueryDocumentSnapshot task2 : tasks.getResult()){
                                       task2.getReference().delete().addOnSuccessListener(aVoid -> {
                                           Toast.makeText(this, "Appointment deleted.", Toast.LENGTH_SHORT).show();
                                       }).addOnFailureListener(e -> Toast.makeText(this, "Error deleting Appointment.", Toast.LENGTH_SHORT).show());

                                   }
                                });
                            }
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        availabilityList.remove(position);
                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(this, "Availability deleted.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting availability.", Toast.LENGTH_SHORT).show());
                        }
                    }
                });
    }

    public static class Availability {
        private String id;
        private String date;
        private String startHour;
        private String endHour;
        private String doctorID;
        private String status;

        public Availability() {
            // Default constructor
        }

        public Availability(String id, String date, String startHour, String endHour, String doctorID, String status) {
            this.id = id;
            this.date = date;
            this.startHour = startHour;
            this.endHour = endHour;
            this.doctorID = doctorID;
            this.status = status;
        }

        // Getters
        public String getId() { return id; }
        public String getDate() { return date; }
        public String getStartHour() { return startHour; }
        public String getEndHour() { return endHour; }
        public String getDoctorID() { return doctorID; }
        public String getStatus() { return status; }

        // Setters
        public void setId(String id) { this.id = id; }
        public void setDate(String date) { this.date = date; }
        public void setStartHour(String startHour) { this.startHour = startHour; }
        public void setEndHour(String endHour) { this.endHour = endHour; }
        public void setDoctorID(String doctorID) { this.doctorID = doctorID; }
        public void setStatus(String status) { this.status = status; }
    }

}
