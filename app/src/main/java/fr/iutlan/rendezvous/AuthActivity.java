package fr.iutlan.rendezvous;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, nameInput, phoneInput;
    private Button authButton;
    private TextView authSwitch;
    private FirebaseFirestore database;
    private boolean isSignInMode = true;

    private ProgressBar loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        createNotificationChannel();
        // Initialize Firebase Firestore
        database = FirebaseFirestore.getInstance();

        // Initialize Views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        authButton = findViewById(R.id.auth_button);
        authSwitch = findViewById(R.id.auth_switch);
        loadingSpinner = findViewById(R.id.loading_spinner);

        authButton.setOnClickListener(v -> {
            if (isSignInMode) {
                signIn();
            } else {
                signUp();
            }
        });

        authSwitch.setOnClickListener(v -> toggleAuthMode());

        // Show loading spinner while checking login status
        loadingSpinner.setVisibility(View.VISIBLE);
        new Handler().postDelayed(this::checkLoginState, 2000); // Simulate loading time
    }

    private void checkLoginState() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loadingSpinner.setVisibility(View.GONE); // Hide spinner after check
        if (currentUser != null) {
            fetchUserRole(currentUser.getUid());
            registerFcmToken(currentUser.getUid());
        } else {
            // No user is logged in; show the auth form
            findViewById(R.id.auth_form).setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SetTextI18n")
    private void toggleAuthMode() {
        emailInput.setText("");
        passwordInput.setText("");
        nameInput.setText("");
        phoneInput.setText("");
        if (isSignInMode) {
            authButton.setText("Register");
            nameInput.setVisibility(View.VISIBLE);
            phoneInput.setVisibility(View.VISIBLE);
            authSwitch.setText("Already have an account? Sign In");
            isSignInMode = false;
        } else {
            authButton.setText("Sign In");
            nameInput.setVisibility(View.GONE);
            phoneInput.setVisibility(View.GONE);
            authSwitch.setText("Don't have an account? Sign Up");
            isSignInMode = true;
        }
    }

    private void signIn() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password must not be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        assert user != null;
                        fetchUserRole(user.getUid());
                        registerFcmToken(user.getUid());
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUp() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        String name = nameInput.getText().toString();
        String phone = phoneInput.getText().toString();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        assert user != null;
                        saveUserToDatabase(user.getUid(), name, phone, email);
                        isSignInMode = false;
                        toggleAuthMode();
                    } else {
                        Toast.makeText(AuthActivity.this, "Registration Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDatabase(String userId, String name, String phone, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("phone", phone);
        userData.put("email", email);
        userData.put("role", "patient"); // Default role

        database.collection("User").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AuthActivity.this, "User added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding user", e));
    }

    private void fetchUserRole(String userId) {
        database.collection("User").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            startMainApp(role);
                        } else {
                            Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching role", e);
                    Toast.makeText(this, "Error Database!", Toast.LENGTH_SHORT).show();
                });
    }

    private void startMainApp(String role) {
        String welcomeMessage = "Welcome " + (role.equals("doctor") ? "Doctor" : "Patient") + "!";
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();

        Intent intent;
        if (role.equals("doctor")) {
            intent = new Intent(AuthActivity.this, DoctorActivity.class);
        } else {
            intent = new Intent(AuthActivity.this, PatientActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void registerFcmToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        // Update the token in Firestore
                        database.collection("User")
                                .document(userId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Token registered successfully"))
                                .addOnFailureListener(e -> Log.e("FCM", "Error registering token", e));
                    }
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
