package fr.iutlan.rendezvous;

import static java.lang.Integer.parseInt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput, nameInput, phoneInput;
    private Button authButton;
    private TextView authSwitch;
    private FirebaseFirestore database;
    private boolean isSignInMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize Firebase Firestore
        database = FirebaseFirestore.getInstance();

        // Initialize Views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        nameInput = findViewById(R.id.name_input);
        phoneInput = findViewById(R.id.phone_input);
        authButton = findViewById(R.id.auth_button);
        authSwitch = findViewById(R.id.auth_switch);

        authButton.setOnClickListener(v -> {
            if (isSignInMode) {
                signIn();
            } else {
                signUp();
            }
        });

        authSwitch.setOnClickListener(v -> toggleAuthMode());
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

    @SuppressLint("RestrictedApi")
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
                    } else {
                        Toast.makeText(AuthActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("RestrictedApi")
    private void signUp() {
        String email = emailInput.getText().toString();
        String password = passwordInput.getText().toString();
        String name = nameInput.getText().toString();
        int phone = parseInt(phoneInput.getText().toString());

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthUI.getInstance().getAuth()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign-up success, save user data
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        assert user != null;
                        saveUserToDatabase(user.getUid(), name, phone, email);
                        isSignInMode = false;

                        toggleAuthMode();
                    } else {
                        // If sign-up fails, display a message to the user.
                        Toast.makeText(AuthActivity.this, "Registration Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void saveUserToDatabase(String userId, String name, int phone, String email) {
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

    private void saveUserToDatabase(String userId, String name, int phone, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("phone", phone);
        userData.put("email", email);
        userData.put("role", role); // Default role

        database.collection("User").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AuthActivity.this, "User added successfully", Toast.LENGTH_SHORT).show();
                    startMainApp(role);
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding user", e));
    }


    private void fetchUserRole(String userId) {
        startMainApp("Test");
//        database.collection("User").document(userId).get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        String role = documentSnapshot.getString("role");
//                        if (role != null) {
//                            startMainApp(role); // Pass the role to the main app
//                        } else {
//                            Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("Firestore", "Error fetching role", e);
//                    Toast.makeText(this, "Error Database!", Toast.LENGTH_SHORT).show();
//                });
    }


    private void startMainApp(String role) {
        String welcomeMessage = "Welcome " + (role.equals("doctor") ? "Doctor" : "Patient") + "!";
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_LONG).show();

        Intent intent;
        if (role.equals("doctor")) {
            intent = new Intent(AuthActivity.this, DoctorActivity.class);
        } else {
            intent = new Intent(AuthActivity.this, DoctorActivity.class);

//            intent = new Intent(AuthActivity.this, PatientActivity.class);
        }
        startActivity(intent);
        finish();
    }

}
