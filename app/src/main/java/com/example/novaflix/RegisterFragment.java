package com.example.novaflix;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final String PREF_NAME = "UserPrefs";
    private TextInputEditText emailEditText, passwordEditText, repeatPasswordEditText, yobEditText, referralCodeEditText;
    private Spinner genderSpinner;
    private CheckBox termsCheckbox;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        emailEditText = view.findViewById(R.id.email);
        passwordEditText = view.findViewById(R.id.password);
        repeatPasswordEditText = view.findViewById(R.id.repeat_password);
        yobEditText = view.findViewById(R.id.yob);
        referralCodeEditText = view.findViewById(R.id.referral_code);
        genderSpinner = view.findViewById(R.id.gender_spinner);
        termsCheckbox = view.findViewById(R.id.terms_checkbox);
        registerButton = view.findViewById(R.id.register_button);
        loginTextView = view.findViewById(R.id.loginLink);

        // Register button click listener
        registerButton.setOnClickListener(v -> registerUser());

        // Login text click listener to navigate to login fragment
        loginTextView.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment()) // Corrected to LoginFragment
                    .addToBackStack(null)
                    .commit();
        });

        // Set up the spinner adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        return view;
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String repeatPassword = repeatPasswordEditText.getText().toString().trim();
        String yob = yobEditText.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String referralCode = referralCodeEditText.getText().toString().trim();

        // Basic validations
        if (!validateInput(email, password, repeatPassword, yob)) return;

        // Validate referral code if provided
        if (!TextUtils.isEmpty(referralCode)) {
            validateReferralCode(referralCode, isValid -> {
                if (isValid) {
                    proceedWithRegistration(email, password, gender, yob, referralCode);
                } else {
                    referralCodeEditText.setError("Invalid referral code.");
                    showToast("Please enter a valid referral code.");
                }
            });
        } else {
            proceedWithRegistration(email, password, gender, yob, null);
        }
    }

    private void validateReferralCode(String referralCode, OnReferralCodeValidationListener listener) {
        firestore.collection("rewards")
                .whereEqualTo("referralCode", referralCode.toLowerCase())
                .get()
                .addOnSuccessListener(querySnapshot -> listener.onValidationComplete(!querySnapshot.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e("ReferralCodeError", "Error validating referral code: " + e.getMessage());
                    listener.onValidationComplete(false); // Treat as invalid if there's an error
                });
    }

    interface OnReferralCodeValidationListener {
        void onValidationComplete(boolean isValid);
    }
    private void proceedWithRegistration(String email, String password, String gender, String yob, String referralCode) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            showToast("Registration successful. Please verify your email.");
                                            String newReferralCode = generateReferralCode();

                                            if (referralCode != null) {
                                                handleReferralPoints(referralCode, email);
                                            }

                                            saveUserDataAndRewards(email, gender, yob, newReferralCode);
                                            navigateToLoginFragment();
                                        } else {
                                            showToast("Failed to send verification email.");
                                        }
                                    });
                        }
                    } else {
                        showToast("Registration failed: " + task.getException().getMessage());
                    }
                });
    }

    private boolean validateInput(String email, String password, String repeatPassword, String yob) {
        if (!isValidEmail(email)) {
            emailEditText.setError("Enter a valid email address.");
            return false;
        }
        if (!isValidPassword(password)) {
            passwordEditText.setError("Password must be at least 6 characters.");
            return false;
        }
        if (!password.equals(repeatPassword)) {
            repeatPasswordEditText.setError("Passwords do not match.");
            return false;
        }
        if (!isValidYearOfBirth(yob)) {
            yobEditText.setError("Enter a valid year of birth.");
            return false;
        }
        if (!termsCheckbox.isChecked()) {
            showToast("Please agree to the terms and conditions.");
            return false;
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6;
    }

    private boolean isValidYearOfBirth(String yob) {
        try {
            int year = Integer.parseInt(yob);
            return year >= 1900 && year <= 2023;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }



    private void saveUserDataAndRewards(String email, String gender, String yob, String referralCode) {
        if (isInvalidInput(email, gender, yob, referralCode)) {
            showToast("Invalid input data!");
            return;
        }

        WriteBatch batch = firestore.batch();

        // Prepare user data
        DocumentReference userRef = firestore.collection("users").document(email);
        batch.set(userRef, createUserDataMap(email, gender, yob, referralCode), SetOptions.merge());

        // Prepare rewards data
        DocumentReference rewardsRef = firestore.collection("rewards").document(email);
        batch.set(rewardsRef, createRewardsDataMap(referralCode));

        // Commit changes
        batch.commit()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User and rewards initialized successfully."))
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to save data: " + e.getMessage());
                    showToast("Error saving data!");
                });
    }


    // Helper method to validate input data
    private boolean isInvalidInput(String email, String gender, String yob, String referralCode) {
        return email == null || email.isEmpty() ||
                gender == null || gender.isEmpty() ||
                yob == null || yob.isEmpty() ||
                referralCode == null || referralCode.isEmpty();
    }

    // Helper method to create the user data map
    private Map<String, Object> createUserDataMap(String email, String gender, String yob, String referralCode) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("gender", gender);
        user.put("yearOfBirth", yob);
        user.put("referralCode", referralCode);
        user.put("plan", "Basic"); // Default plan
        return user;
    }

    // Helper method to create the rewards data map
    private Map<String, Object> createRewardsDataMap(String referralCode) {
        Map<String, Object> rewardsData = new HashMap<>();
        rewardsData.put("adRewardsEarned", 0);
        rewardsData.put("adWatchCount", 0);
        rewardsData.put("balance", 0);
        rewardsData.put("referralCodeUsed", 0);
        rewardsData.put("dailyBonusClaimed", 0);
        rewardsData.put("dailyCheckInDate", null);
        rewardsData.put("gains", "0");
        rewardsData.put("lastDailyCheckInDate", null);
        rewardsData.put("lifetimePoints", 0);
        rewardsData.put("rank", "");
        rewardsData.put("referralBonus", 0);
        rewardsData.put("referralCode", referralCode);
        rewardsData.put("referrals", 0);
        rewardsData.put("totalReferrals", 0);
        return rewardsData;
    }
    private String generateReferralCode() {
        return "REF" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    private void handleReferralPoints(String referralCode, String newUserEmail) {
        firestore.collection("rewards")
                .whereEqualTo("referralCode", referralCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String referrerDocId = querySnapshot.getDocuments().get(0).getId();

                        // Update the referrer's data
                        firestore.collection("rewards").document(referrerDocId)
                                .update(
                                        "balance", FieldValue.increment(5),
                                        "referralBonus", FieldValue.increment(5),
                                        "totalReferrals", FieldValue.increment(1),
                                        "lifetimePoints", FieldValue.increment(5)
                                )
                                .addOnSuccessListener(aVoid -> updateRank(referrerDocId))
                                .addOnFailureListener(e -> Log.e("ReferralUpdateError", "Error updating referrer: " + e.getMessage()));

                        // Initialize new user's data
                        initializeNewUserData(newUserEmail, referralCode, 15); // 15 points for using referral
                    } else {
                        showToast("Invalid referral code!");
                    }
                })
                .addOnFailureListener(e -> Log.e("ReferralFetchError", "Error fetching referral code: " + e.getMessage()));
    }

    private void initializeNewUserData(String email, String referralCode, int initialBalance) {
        Map<String, Object> newUserData = new HashMap<>();
        newUserData.put("adRewardsEarned", 0);
        newUserData.put("adWatchCount", 0);
        newUserData.put("balance", initialBalance);
        newUserData.put("referralCodeUsed", referralCode);
        newUserData.put("dailyBonusClaimed", false);
        newUserData.put("dailyCheckInDate", null);
        newUserData.put("gains", "0");
        newUserData.put("lastDailyCheckInDate", null);
        newUserData.put("lifetimePoints", initialBalance);
        newUserData.put("rank", getRank(initialBalance));
        newUserData.put("referralBonus", 0);
        newUserData.put("referralCode", generateReferralCode());
        newUserData.put("referrals", 0);
        newUserData.put("totalReferrals", 0);

        firestore.collection("rewards").document(email)
                .set(newUserData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> showToast("New user data initialized!"))
                .addOnFailureListener(e -> Log.e("NewUserError", "Error initializing user data: " + e.getMessage()));
    }

    private void updateRank(String referrerDocId) {
        firestore.collection("rewards").document(referrerDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int lifetimePoints = documentSnapshot.getLong("lifetimePoints").intValue();
                        String newRank = getRank(lifetimePoints);

                        firestore.collection("rewards").document(referrerDocId)
                                .update("rank", newRank)
                                .addOnSuccessListener(aVoid -> Log.d("RankUpdate", "Rank updated to: " + newRank))
                                .addOnFailureListener(e -> Log.e("RankUpdateError", "Error updating rank: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e("RankFetchError", "Error fetching referrer data: " + e.getMessage()));
    }

    // Helper method to determine the rank based on lifetime points
    private String getRank(int lifetimePoints) {
        Map<Integer, String> rankThresholds = new LinkedHashMap<>();
        rankThresholds.put(50, "Gold");
        rankThresholds.put(20, "Silver");
        rankThresholds.put(0, "Bronze");

        for (Map.Entry<Integer, String> entry : rankThresholds.entrySet()) {
            if (lifetimePoints >= entry.getKey()) {
                return entry.getValue();
            }
        }
        return "Bronze";
    }


    private void navigateToLoginFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .addToBackStack(null)
                .commit();
    }
}
