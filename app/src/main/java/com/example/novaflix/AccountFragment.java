package com.example.novaflix;

import static com.example.novaflix.LoginFragment.PREF_NAME;
import static com.google.firebase.appcheck.internal.util.Logger.TAG;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AccountFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserUid;
    private TextView usernameText, emailText;
    private Spinner subscriptionPlanSpinner;
    private EditText cardNumberEditText, expiryDateEditText, cvvEditText;
    private Button saveCardDetailsButton, logoutButton;

    public AccountFragment() {
        super(R.layout.fragment_account);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Get the current user ID
        currentUserUid = mAuth.getCurrentUser().getUid();

        usernameText = view.findViewById(R.id.username);
        emailText = view.findViewById(R.id.email);
        usernameText = view.findViewById(R.id.username);
        emailText = view.findViewById(R.id.email);

        if (usernameText == null) {
            Log.e(TAG, "usernameText is null. Check the layout file.");
        }
        if (emailText == null) {
            Log.e(TAG, "emailText is null. Check the layout file.");
        }
        subscriptionPlanSpinner = view.findViewById(R.id.spinner_subscription_plan);
        cardNumberEditText = view.findViewById(R.id.edit_card_number);
        expiryDateEditText = view.findViewById(R.id.edit_expiry_date);
        cvvEditText = view.findViewById(R.id.edit_cvv);
        saveCardDetailsButton = view.findViewById(R.id.button_save_card_details);
        logoutButton = view.findViewById(R.id.button_logout);

        // Set up event listeners
        expiryDateEditText.setOnClickListener(v -> openExpiryDatePicker());
        saveCardDetailsButton.setOnClickListener(v -> saveCardDetails());
        logoutButton.setOnClickListener(v -> logoutUser());

        // Load user details
        loadUserDetails();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.plan_options,  // Array of subscription plans
                R.layout.spinner_item   // Custom layout for spinner items
        );
        adapter.setDropDownViewResource(R.layout.spinner_item); // Custom layout for dropdown view
        subscriptionPlanSpinner.setAdapter(adapter);



    }

    private void loadUserDetails() {
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            usernameText.setText(email);  // Set username to email
            emailText.setText(email);

            String plan = getCurrentUserSubscriptionType(); // Retrieve the user's subscription plan

            // Find the position of the subscription plan in the spinner

        }
    }


    private void openExpiryDatePicker() {
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Format month and year as MM/YY
                    String formattedExpiryDate = String.format("%02d/%02d", monthOfYear + 1, year % 100);
                    expiryDateEditText.setText(formattedExpiryDate);
                },
                currentYear,
                currentMonth,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveCardDetails() {
        String cardNumber = cardNumberEditText.getText().toString();
        String expiryDate = expiryDateEditText.getText().toString();
        String cvv = cvvEditText.getText().toString();


        // Validate card number (must be 16 digits)
        if (cardNumber.isEmpty() || cardNumber.length() != 16) {
            Toast.makeText(getContext(), "Card number must be 16 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate expiry date
        if (expiryDate.isEmpty() || !expiryDate.matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) {
            Toast.makeText(getContext(), "Invalid expiry date. Use MM/YY format.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate CVV (must be 3 digits)
        if (cvv.isEmpty() || cvv.length() != 3) {
            Toast.makeText(getContext(), "CVV must be 3 digits.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save card details to Firebase
        mDatabase.child("users").child(currentUserUid).child("card_details").setValue(new CardDetails(cardNumber, expiryDate, cvv))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Card details saved.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Error saving card details.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutUser() {
        mAuth.signOut();  // Log the user out

        // Restart the app and go back to login screen
        Intent intent = new Intent(getContext(), AuthActivity.class);  // AuthActivity should handle the login screen
        startActivity(intent);
        getActivity().finish();  // Close the current activity
    }

    public static class CardDetails {
        public String cardNumber;
        public String expiryDate;
        public String cvv;

        public CardDetails(String cardNumber, String expiryDate, String cvv) {
            this.cardNumber = cardNumber;
            this.expiryDate = expiryDate;
            this.cvv = cvv;
        }
    }




    private String getCurrentUserSubscriptionType() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String subscription = sharedPreferences.getString("plan", null);


        Log.d(TAG, "User subscription plan retrieved: " + subscription);


        return subscription;
    }
}
