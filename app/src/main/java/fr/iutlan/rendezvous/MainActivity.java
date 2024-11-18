package fr.iutlan.rendezvous;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore database;
    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        database = FirebaseFirestore.getInstance();

        // Check if the user is signed in
        FirebaseAuth.getInstance().signOut();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Not signed in, start FirebaseUI sign-in flow
            Log.v("a","SignUp");
            startSignInFlow();
        } else {
//            startSignInFlow();
            Log.v("a","SignIn");
            // User is already signed in, proceed with the app
            startMainApp();
        }
    }

    // FirebaseUI sign-in flow
    private void startSignInFlow() {
        // Create a list of authentication providers (email/password in this case)
        List<AuthUI.IdpConfig> providers = Collections.singletonList(
                new AuthUI.IdpConfig.EmailBuilder().build()
        );

        // Launch the FirebaseUI sign-in flow
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false, false)  // Disable Smart Lock
                        .setTheme(R.style.FirebaseUITheme)    // Optional: Apply custom theme
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // User successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null && Objects.requireNonNull(user.getMetadata()).getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp()) {
                    // If user just signed up, log them out and redirect to sign-in page
                    FirebaseAuth.getInstance().signOut();
                    startSignInFlow(); // Redirect to sign-in page again after sign-up
                } else {
                    // User signed in successfully, proceed to the main app
                    startMainApp();
                }
            } else {
                // Sign-in failed, handle the error
                Log.e("AuthError", "Sign-in failed");

            }
        }
    }

    // Save user information to Firestore
    private void saveUserToFirestore(FirebaseUser user) {
        // Create a map for user data
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "Anonymous");
        userData.put("email", user.getEmail());
        userData.put("role", "patient"); // Default role is "patient"

        // Add the user data to the "User" collection
        database.collection("User").document(user.getUid()).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirestoreAdd", "User data added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreAddError", "Error adding user data", e);
                });
    }

    private void startMainApp() {
        // Redirect to the main part of your app (or load another activity)
    }
}
