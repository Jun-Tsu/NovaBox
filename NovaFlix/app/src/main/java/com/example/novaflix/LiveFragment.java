package com.example.novaflix;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LiveFragment extends Fragment {

    private static final String TAG = "LiveFragment";
    private RecyclerView channelRecyclerView;

    private ChannelAdapter channelAdapter;
    private List<Channel> channelList;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final String PREF_NAME = "LiveFragmentPrefs";
    private static final String KEY_CHANNELS = "channels";
    private EditText searchField;
    private ImageButton searchIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);

        // Initialize RecyclerView
        channelRecyclerView = view.findViewById(R.id.channel_grid);
        searchField = getActivity().findViewById(R.id.search_field);
        searchIcon = getActivity().findViewById(R.id.search_icon);
        channelRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 columns

        if (searchField == null || searchIcon == null) {
            Log.e(TAG, "searchField or searchIcon is not found in the activity layout.");
        } else {
            setupSearch(); // Proceed with setup if views are not null
        }
        // Initialize channel list and adapter
        channelList = new ArrayList<>();
        channelAdapter = new ChannelAdapter(channelList, getContext());
        channelRecyclerView.setAdapter(channelAdapter);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshChannels);  // Setup refresh listener


        // Try loading channels from SharedPreferences
        loadChannelsFromPreferences();
        setupSearch();

        // If no channels are loaded from SharedPreferences, fetch from Backendless
        if (channelList.isEmpty()) {
            Log.d(TAG, "Fetching M3U file URL from Backendless");
            fetchM3UFileURLFromBackendless(); // Start fetching M3U file URL
        }
        fetchUserSubscriptionPlan();

       // getCurrentUserSubscriptionType();
        return view;
    }
    private void fetchUserSubscriptionPlan() {
        String userEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
        if (userEmail == null) {
            Log.w(TAG, "No user logged in.");
            return;
        } else {
            Log.d(TAG, "Found Logged-in User: " + userEmail);
        }

        firestore.collection("users").document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String plan = documentSnapshot.getString("plan");
                        if (plan != null) {
                            Log.d(TAG, "User subscription plan retrieved from Firestore: " + plan);
                            savePlanToSharedPreferences(plan);
                        } else {
                            Log.w(TAG, "Plan not found in Firestore document.");
                        }
                    } else {
                        Log.w(TAG, "User document does not exist in Firestore.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching plan from Firestore", e);
                    showToast("Failed to fetch user plan. Please try again later.");
                });
    }
    private void savePlanToSharedPreferences(String plan) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("plan", plan);
        editor.apply();
    }
    private String getCurrentUserSubscriptionType() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Retrieve the saved preferences
        String email = sharedPreferences.getString("email", null);  // default is null if not found
        String userSubscription = sharedPreferences.getString("plan", "Basic");


        Log.d(TAG, "User subscription plan retrieved: " + userSubscription);


        return userSubscription;
    }
    // Fetch the M3U file URL and name from Backendless
    private void fetchM3UFileURLFromBackendless() {
        String userSubscription = getCurrentUserSubscriptionType();
        DialogUtils.showLoadingDialog(requireActivity());

        if (userSubscription == null) {
            Log.w(TAG, "Plan not found in SharedPreferences.");
            showToast("No plan found.");
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Define the query to match the plan name
        String whereClause = "name = '" + userSubscription + "'";
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause);

        Backendless.Data.of("Playlist").find(queryBuilder, new AsyncCallback<List<Map>>() {
            @Override
            public void handleResponse(List<Map> response) {
                if (response != null && !response.isEmpty()) {
                    Log.d(TAG, "Received playlist data from Backendless, processing...");

                    for (Map<String, Object> playlist : response) {
                        String fileURL = (String) playlist.get("m3u");
                        String name = (String) playlist.get("name");

                        if (fileURL != null && name != null) {
                            Log.d(TAG, "M3U Name: " + name + ", URL: " + fileURL);
                            parseM3UFile(fileURL);
                            break; // Stop after one M3U file if only one per plan
                        }
                    }
                } else {
                    Log.w(TAG, "No playlists found for the plan: " + userSubscription);
                    showToast("No playlists found for the current plan.");
                }
                swipeRefreshLayout.setRefreshing(false);
                DialogUtils.dismissLoadingDialog(requireActivity());
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.e(TAG, "Failed to fetch M3U URLs: " + fault.getMessage());
                showToast("Failed to fetch M3U URLs: " + fault.getMessage());
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    // Parse the M3U file
    // Parse the M3U file
    private void parseM3UFile(String m3uUrl) {
        new Thread(() -> {


            List<Channel> channels = M3UParser.parseM3U(m3uUrl);
            if (channels != null && !channels.isEmpty()) {
                Log.d(TAG, "Channels loaded: " + channels.size());
                // Update the channel list on the main thread
                getActivity().runOnUiThread(() -> updateChannelList(channels));
                // Save channels to SharedPreferences

                saveChannelsToPreferences(channels);
            } else {
                Log.w(TAG, "No channels found in M3U file.");
                // Show toast on the main thread
                getActivity().runOnUiThread(() -> showToast("No channels found."));
            }
            // Stop the refresh animation on the main thread
            getActivity().runOnUiThread(() -> swipeRefreshLayout.setRefreshing(false));
        }).start();
    }

    // Update the RecyclerView with the parsed channels
    private void updateChannelList(List<Channel> channels) {
        // Show loading dialog at the start
        DialogUtils.dismissLoadingDialog(requireActivity());

        if (channels.size() != channelList.size()) {
            Log.d(TAG, "Channel count differs, updating SharedPreferences and channel list.");

            // Update SharedPreferences with the new list of channels
            saveChannelsToPreferences(channels);

            // Update channel list
            channelList.clear();
            channelList.addAll(channels);
            channelAdapter.notifyDataSetChanged();
        } else {
            Log.d(TAG, "Channel count matches, no update needed.");

        }

        // Stop refresh animation
        swipeRefreshLayout.setRefreshing(false);

        // Dismiss the loading dialog

    }

    // Save the loaded channels to SharedPreferences
    private void saveChannelsToPreferences(List<Channel> channels) {
        SharedPreferences preferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(channels);
        editor.putString(KEY_CHANNELS, json);
        editor.apply();
    }

    // Load the channels from SharedPreferences
    private void loadChannelsFromPreferences() {
        // Show loading dialog


        // Load channels from SharedPreferences

        SharedPreferences preferences = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = preferences.getString(KEY_CHANNELS, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Channel>>() {}.getType();
            List<Channel> channels = gson.fromJson(json, type);
            if (channels != null) {
                channelList.clear(); // Clear any existing channels to avoid duplication
                channelList.addAll(channels);
                Log.d(TAG, "Loaded channels from SharedPreferences, total channels: " + channelList.size());
                channelAdapter.notifyDataSetChanged();
            }
        } else {
            Log.d(TAG, "No channels found in SharedPreferences.");

        }

        // Dismiss loading dialog

    }

    private void refreshChannels() {
        swipeRefreshLayout.setRefreshing(true);// Show refresh indicator immediately
        channelList.clear();  // Clear current channels

        // Try loading from SharedPreferences
        loadChannelsFromPreferences();

        if (channelList.isEmpty()) {
            Log.d(TAG, "No channels found in SharedPreferences, fetching from Backendless.");
            fetchM3UFileURLFromBackendless(); // Fetch from Backendless if no channels found
        } else {
            Log.d(TAG, "Channels loaded from SharedPreferences, checking for updates.");

            // Fetch online channels and compare with the SharedPreferences data
            fetchM3UFileURLFromBackendless();  // Fetch again to check for updates
        }


    }

    // Helper method to show Toast messages
    private void showToast(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
        }
    }

    private void setupSearch() {
        // Toggle search field visibility
        searchIcon.setOnClickListener(v -> {
            if (searchField.getVisibility() == View.GONE) {
                searchField.setVisibility(View.VISIBLE);
                searchField.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                searchField.setText(""); // Clear search field
                searchField.setVisibility(View.GONE);
                channelAdapter.updateChannelList(channelList); // Reset list
            }
        });

        // Add search query listener
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("SearchField", "Query: " + s.toString()); // Debugging
                filterChannels(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Reset list if query is cleared
                if (s.toString().trim().isEmpty()) {
                    channelAdapter.updateChannelList(channelList); // Reset to full list
                }
            }
        });
    }


    private void filterChannels(String query) {
        List<Channel> filteredList = new ArrayList<>();
        for (Channel channel : channelList) {
            String name = channel.getName();
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(channel);
            }
        }
        channelAdapter.updateChannelList(filteredList);
    }

}