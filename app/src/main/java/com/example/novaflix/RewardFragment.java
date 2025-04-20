package com.example.novaflix;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RewardFragment extends Fragment {

    private TextView balanceValue;
    private ImageButton  followXButton, followInstagramButton, followFacebookButton;
    private ImageButton copyReferralButton, feedbackSubmitButton, watchAd1Button, watchAd2Button;
    private EditText referralCode;
    private Button dailyCheckInButton;
    private RatingBar feedbackRating;

    private int userBalance = 0;  // This can be fetched from Firestore

    // Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public RewardFragment() {
        super(R.layout.fragment_reward);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        balanceValue = view.findViewById(R.id.balanceValue);
        TextView referralCodeTextView = view.findViewById(R.id.referralCode);

        dailyCheckInButton = view.findViewById(R.id.dailyCheckInButton);
        followXButton = view.findViewById(R.id.x_icon);
        followInstagramButton = view.findViewById(R.id.instagram_icon);
        followFacebookButton = view.findViewById(R.id.facebook_icon);
        copyReferralButton = view.findViewById(R.id.copyReferralButton);
        feedbackSubmitButton = view.findViewById(R.id.feedbackSubmitButton);
        watchAd1Button = view.findViewById(R.id.watchAd1Button);
        watchAd2Button = view.findViewById(R.id.watchAd2Button);
        referralCode = view.findViewById(R.id.referralCode);
        feedbackRating = view.findViewById(R.id.feedbackRating);

        // Initialize balance (fetch from Firestore)
        updateBalance();

        // Daily Check-in Button Click Listener
        dailyCheckInButton.setOnClickListener(v -> onDailyCheckIn());

        // Social Media Follow Button Click Listeners
        followXButton.setOnClickListener(v -> onFollowSocialMedia("X"));
        followInstagramButton.setOnClickListener(v -> onFollowSocialMedia("Instagram"));
        followFacebookButton.setOnClickListener(v -> onFollowSocialMedia("Facebook"));

        // Copy Referral Code Button
        copyReferralButton.setOnClickListener(v -> onCopyReferralCode());

        // Submit Feedback Button
        feedbackSubmitButton.setOnClickListener(v -> onSubmitFeedback("x"));

        // Watch Ads Button Click Listeners
        watchAd1Button.setOnClickListener(v -> onWatchAd("a",1));
        watchAd2Button.setOnClickListener(v -> onWatchAd("b",1));

        // Retrieve or Generate the Referral Code on Fragment Creation
        retrieveOrGenerateReferralCode(referralCodeTextView);

    }

    private void updateBalance() {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null) {
            Log.e("Firestore", "User is not authenticated");
            return;
        }

        DocumentReference rewardRef = db.collection("rewards").document(email);

        rewardRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    int balance = document.getLong("balance").intValue();
                    balanceValue.setText(balance + " Points");
                } else {
                    // No reward data found, so create it
                    createRewardData(email);
                }
            } else {
                Log.e("FirestoreError", "Error fetching reward balance", task.getException());
            }
        });
    }

    private void createRewardData(String email) {
        DocumentReference rewardRef = db.collection("rewards").document(email);

        Map<String, Object> rewardData = new HashMap<>();
        rewardData.put("balance", 0);
        rewardData.put("dailyCheckInDate", null);
        rewardData.put("gains", "0");
        rewardData.put("referralCode", ""); // You can generate one later
        rewardData.put("referrals", 0);
        rewardData.put("adWatchHistory", new HashMap<String, Object>());

        rewardRef.set(rewardData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Reward data created successfully.");
                    balanceValue.setText("0 Points"); // Set balance to 0 initially
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error creating reward data", e));
    }

    private void onDailyCheckIn() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("RewardPrefs", Context.MODE_PRIVATE);
        String lastCheckInDate = sharedPreferences.getString("lastCheckInDate", "");

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());

        // Check if the last check-in was today
        if (todayDate.equals(lastCheckInDate)) {
            Toast.makeText(getContext(), "You have already checked in today!", Toast.LENGTH_SHORT).show();
        } else {
            // Get the current user's email
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            if (email != null) {
                // Reference to the user's rewards document using email
                DocumentReference rewardRef = db.collection("rewards").document(email);

                // Check if the document exists for the user
                rewardRef.get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    // Get the current balance and lifetime points
                                    int currentBalance = document.getLong("balance").intValue();
                                    int lifetimePoints = document.getLong("lifetimePoints").intValue();

                                    // Update balance and lifetime points
                                    currentBalance += 10;
                                    lifetimePoints += 10;

                                    // Determine the rank based on lifetimePoints
                                    String rank = getRank(lifetimePoints);

                                    // Update the rewards fields
                                    rewardRef.update(
                                                    "balance", currentBalance,
                                                    "lifetimePoints", lifetimePoints,
                                                    "dailyBonusClaimed", true,
                                                    "dailyCheckInDate", todayDate,
                                                    "lastDailyCheckInDate", lastCheckInDate,
                                                    "rank", rank)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Daily Check-in completed! +10 Points", Toast.LENGTH_SHORT).show();
                                                // Save the check-in date locally
                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                editor.putString("lastCheckInDate", todayDate);
                                                editor.apply();
                                            })
                                            .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating daily check-in", e));

                                } else {
                                    // Document doesn't exist, create a new one with default values
                                    Map<String, Object> newRewardData = new HashMap<>();
                                    newRewardData.put("balance", 10); // Initial balance after first check-in
                                    newRewardData.put("lifetimePoints", 10);
                                    newRewardData.put("dailyBonusClaimed", true);
                                    newRewardData.put("dailyCheckInDate", todayDate);
                                    newRewardData.put("lastDailyCheckInDate", "");
                                    newRewardData.put("rank", getRank(10)); // Set rank based on first 10 points
                                    newRewardData.put("referralCode", "");  // Empty or generate referral code
                                    newRewardData.put("referrals", 0);
                                    newRewardData.put("adWatchHistory", new HashMap<String, Object>());  // Empty ad watch history

                                    // Create the document
                                    rewardRef.set(newRewardData)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Daily Check-in completed! +10 Points", Toast.LENGTH_SHORT).show();
                                                // Save the check-in date locally
                                                SharedPreferences.Editor editor1 = sharedPreferences.edit();
                                                editor1.putString("lastCheckInDate", todayDate);
                                                editor1.apply();
                                            })
                                            .addOnFailureListener(e -> Log.e("FirestoreError", "Error creating new document", e));
                                }
                            } else {
                                Log.e("FirestoreError", "Error checking if document exists", task.getException());
                            }
                        });
            } else {
                Log.e("CheckInError", "Email is null, unable to check in.");
            }
        }
    }

    // Helper method to determine the rank based on lifetime points
    private String getRank(int lifetimePoints) {
        if (lifetimePoints >= 50) {
            return "Gold";
        } else if (lifetimePoints >= 20) {
            return "Silver";
        } else {
            return "Bronze";
        }
    }

    private void onFollowSocialMedia(String platform) {
        SharedPreferences prefs = getContext().getSharedPreferences("FollowCounts", Context.MODE_PRIVATE);
        AtomicInteger followCount = new AtomicInteger(prefs.getInt(platform, 0));

        if (followCount.get() < 2) {
            // Open the social media link
            String url = "";
            switch (platform) {
                case "X":
                    url = "https://x.com/awlchemist";
                    break;
                case "Instagram":
                    url = "https://www.instagram.com/li_quinn11/";
                    break;
                case "Facebook":
                    url = "https://facebook.com/developer";
                    break;
            }
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

            // Get the current user's email
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            if (email != null) {
                // Reference to the user's rewards document using email
                DocumentReference rewardRef = db.collection("rewards").document(email);

                // Update balance in the rewards collection
                rewardRef.update("balance", FieldValue.increment(10))
                        .addOnSuccessListener(aVoid -> {
                            followCount.getAndIncrement(); // Increment follow count
                            prefs.edit().putInt(platform, followCount.get()).apply();
                            Toast.makeText(getContext(), "Thank you for following! +10 Points", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating follow rewards", e));
            } else {
                Log.e("FollowError", "Email is null, unable to update follow rewards.");
            }
        } else {
            Toast.makeText(getContext(), "You have already earned points for this action.", Toast.LENGTH_SHORT).show();
        }
    }




    private void onCopyReferralCode() {
        String referral = referralCode.getText().toString();
        if (!referral.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", referral));
            Toast.makeText(getContext(), "Referral Code copied!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Referral Code is empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void retrieveOrGenerateReferralCode(TextView referralCodeTextView) {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();  // Get email instead of UID
        DocumentReference userRef = db.collection("users").document(email);  // Use email as document ID
        DocumentReference rewardRef = db.collection("rewards").document(email);  // Use email for rewards document

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot userDoc = task.getResult();
                if (userDoc.exists() && userDoc.contains("referralCode")) {
                    // Fetch existing referral code
                    String referralCode = userDoc.getString("referralCode");
                    referralCodeTextView.setText(referralCode);
                } else {
                    // Generate and save a new referral code in both users and rewards collections
                    String newReferralCode = generateReferralCode();

                    // Update the users collection
                    userRef.update("referralCode", newReferralCode);

                    // Initialize reward data in rewards collection
                    Map<String, Object> rewardData = new HashMap<>();
                    rewardData.put("balance", 0);
                    rewardData.put("referralCode", newReferralCode);
                    rewardData.put("referrals", 0);
                    rewardData.put("gains", 0);
                    rewardData.put("dailyCheckInDate", null);

                    rewardRef.set(rewardData)
                            .addOnSuccessListener(aVoid -> {
                                referralCodeTextView.setText(newReferralCode);
                                Log.d("Reward", "Reward data initialized successfully.");
                            })
                            .addOnFailureListener(e -> Log.e("FirestoreError", "Error initializing reward data", e));
                }
            } else {
                Log.e("FirestoreError", "Error fetching user document", task.getException());
            }
        });
    }


    private String generateReferralCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        while (code.length() < 8) { // Length of the code
            int index = (int) (random.nextFloat() * characters.length());
            code.append(characters.charAt(index));
        }
        return code.toString();
    }




    private void addPoints(String email, int points) {
        // Get the user document by email
        db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)  // Ensure only one document is returned
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the first (and only) document
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        int currentBalance = userDoc.getLong("balance").intValue();

                        // Calculate the new balance
                        int newBalance = currentBalance + points;

                        // Update the user's balance
                        db.collection("users")
                                .document(userDoc.getId())  // Use document ID from the query result
                                .update("balance", newBalance)
                                .addOnSuccessListener(aVoid -> Log.d("Reward", "Points awarded to user."))
                                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating balance", e));
                    } else {
                        Log.e("FirestoreError", "User with email " + email + " not found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error retrieving user document", e));
    }


    private void addPointsToCurrentUser(int points) {
        userBalance += points; // Add points to current user
        updateBalance(); // Update the balance UI

        // Get the current user's email
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Update Firestore with new balance for the current user
        if (email != null) {
            db.collection("rewards").document(email) // Use email instead of UID
                    .update("balance", userBalance)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Reward", "Balance successfully updated for user: " + email);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreError", "Error updating balance", e);
                    });
        } else {
            Log.e("Reward", "Error: Email is null, unable to update balance.");
        }
    }

    private void onSubmitFeedback(String email) {
        float rating = feedbackRating.getRating(); // Get the rating from the RatingBar

        if (rating > 0) {
            // Prepare the data to save
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("email", email); // Use email instead of UID
            feedbackData.put("rating", rating);
            feedbackData.put("timestamp", System.currentTimeMillis()); // Store current time

            // Reference to the "ratings" collection
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("ratings")
                    .add(feedbackData) // Add a new document with generated ID
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Thanks for your feedback!", Toast.LENGTH_SHORT).show();
                        Log.d("Firestore", "Feedback successfully stored!");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to submit feedback!", Toast.LENGTH_SHORT).show();
                        Log.e("FirestoreError", "Error adding document to ratings collection", e);
                    });
        } else {
            Toast.makeText(getContext(), "Please provide a rating before submitting!", Toast.LENGTH_SHORT).show();
        }
    }

    private void onWatchAd(String email, int adId) {
        DocumentReference adRef = db.collection("adWatchHistory").document(email); // Use email instead of UID

        adRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Map<String, Object> history = document.getData();
                if (history != null && history.containsKey("ad" + adId)) {
                    Toast.makeText(getContext(), "You've already watched this ad today.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // Award points and save history
            userBalance += 20;
            updateBalance();

            Map<String, Object> update = new HashMap<>();
            update.put("ad" + adId, System.currentTimeMillis()); // Log ad watch timestamp
            adRef.update(update).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Thanks for watching! You've earned 20 points.", Toast.LENGTH_SHORT).show();
            });
        });
    }



    public class User {
        private String email;  // Changed userId to email
        private String referralCode;
        private int balance;

        // Inner class to store referral-related data
        public class ReferralData {
            private String code;
            private int gains;

            public ReferralData(String code, int gains) {
                this.code = code;
                this.gains = gains;
            }

            public String getCode() {
                return code;
            }

            public int getGains() {
                return gains;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public void setGains(int gains) {
                this.gains = gains;
            }
        }

        // Getters and Setters for User class fields
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getReferralCode() {
            return referralCode;
        }

        public void setReferralCode(String referralCode) {
            this.referralCode = referralCode;
        }

        public int getBalance() {
            return balance;
        }

        public void setBalance(int balance) {
            this.balance = balance;
        }
    }
}
