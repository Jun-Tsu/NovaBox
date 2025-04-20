package com.example.novaflix;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    public static final String PREF_NAME = "UserPrefs";
    private TextInputEditText emailEditText, passwordEditText;
    private CheckBox rememberMeCheckBox, agreeCheckBox;
    private Button loginButton;
    private TextView registerLink, forgotPasswordLink, resendEmailLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Initialize FirebaseAuth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        agreeCheckBox = view.findViewById(R.id.Agree);
        rememberMeCheckBox = view.findViewById(R.id.rememberMe);
        loginButton = view.findViewById(R.id.loginButton);
        registerLink = view.findViewById(R.id.loginLink);
        forgotPasswordLink = view.findViewById(R.id.forgotPasswordLink);
        resendEmailLink = view.findViewById(R.id.resendEmailLink);

        // Set click listener for login button
        loginButton.setOnClickListener(v -> loginUser());

        // Set click listener for register link to navigate to RegisterFragment
        registerLink.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Check if the user is already logged in and authenticated
        if (mAuth.getCurrentUser() != null && isUserRemembered()) {
            // Directly open MainActivity if user is authenticated and "Remember Me" is checked
            navigateToMainActivity();
        } else {
            // Restore the email and password if "Remember Me" was checked previously
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            emailEditText.setText(sharedPreferences.getString("email", ""));
            passwordEditText.setText(sharedPreferences.getString("password", ""));
            rememberMeCheckBox.setChecked(sharedPreferences.getBoolean("rememberMe", false));
        }

        forgotPasswordLink.setOnClickListener(v -> showForgotPasswordDialog());

        resendEmailLink.setOnClickListener(v -> resendVerificationEmail());

        return view;
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if agree checkbox is checked
        if (!agreeCheckBox.isChecked()) {
            Toast.makeText(getContext(), "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        if (user.isEmailVerified()) {
                            retrieveUserPlan(email);
                        } else {
                            Toast.makeText(getContext(), "Please verify your email before logging in", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void retrieveUserPlan(String email) {
        firestore.collection("users").document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String plan = documentSnapshot.getString("plan");

                        saveToPreferences(email, plan, rememberMeCheckBox.isChecked());
                        Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(getContext(), "User not found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error retrieving plan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveToPreferences(String email, String plan, boolean rememberMe) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("email", email);
        editor.putString("plan", plan);
        editor.putBoolean("rememberMe", rememberMe);
        editor.putString("password", passwordEditText.getText().toString()); // Save password too
        editor.apply();
    }

    private boolean isUserRemembered() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean("rememberMe", false);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Reset Password");

        final EditText input = new EditText(getContext());
        input.setHint("Enter your email");
        builder.setView(input);

        builder.setPositiveButton("Send Reset Link", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            sendPasswordResetEmail(email);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Verification email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to send verification email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "No user found or email already verified", Toast.LENGTH_SHORT).show();
        }
    }
}
