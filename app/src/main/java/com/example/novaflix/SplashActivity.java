package com.example.novaflix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private LottieAnimationView lottieAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize the Lottie animation view
        lottieAnimationView = findViewById(R.id.lottieAnimationView);

        // Start the Lottie anim
        lottieAnimationView.playAnimation();
        // Directly check if the user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Show Lottie animation while checking Firestore data
            checkUserPlan();
        } else {
            // If user is not logged in, navigate to AuthActivity without delay
            navigateToAuthActivity();
        }
    }

    private void checkUserPlan() {
        // Get the logged-in user's email
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            Log.w(TAG, "No user logged in.");
            showToast("Please log in to proceed.");
            navigateToAuthActivity();
            return;
        }

        // Fetch user subscription plan from Firestore
        fetchUserPlanFromFirestore(userEmail);
    }

    private String getCurrentUserEmail() {
        // Retrieve current user email if available
        FirebaseUser currentUser = auth.getCurrentUser();
        return (currentUser != null) ? currentUser.getEmail() : null;
    }

    private void fetchUserPlanFromFirestore(String userEmail) {
        firestore.collection("users").document(userEmail).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleDocumentSnapshot(documentSnapshot);
                    } else {
                        handleMissingUserDocument();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching plan from Firestore");
                    showToast("Failed to fetch user plan.");
                    navigateToAuthActivity();
                });
    }

    private void handleDocumentSnapshot(DocumentSnapshot documentSnapshot) {
        // Extract the user's plan from the document snapshot
        String plan = documentSnapshot.getString("plan");
        if (plan != null) {
            Log.d(TAG, "User subscription plan retrieved: " + plan);
            savePlanToSharedPreferences(plan); // Save the plan locally (SharedPreferences)
            navigateToMainActivity(); // Proceed to main activity
        } else {
            Log.w(TAG, "No subscription plan found in Firestore document.");
            showToast("Your subscription plan is missing, please update it.");
            navigateToAuthActivity();
        }
    }

    private void handleMissingUserDocument() {
        // Handle case where the user document doesn't exist
        Log.w(TAG, "User document does not exist in Firestore.");
        showToast("User not found, please register.");
        navigateToAuthActivity();
    }

    private void showToast(String message) {
        // Show a toast message for user feedback
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void savePlanToSharedPreferences(String plan) {
        // Save the user's subscription plan to SharedPreferences (if needed)
        SharedPreferences preferences = getSharedPreferences("user_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_plan", plan);
        editor.apply();
    }



    // Method to navigate to MainActivity
    private void navigateToMainActivity() {
        // Add a small delay to ensure animation plays before transition
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1500); // Delay of 1.5 seconds for smooth transition
    }

    // Method to navigate to AuthActivity (Login/Registration)
    private void navigateToAuthActivity() {
        // Add a small delay to ensure animation plays before transition
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
        }, 1500); // Delay of 1.5 seconds for smooth transition
    }
}
