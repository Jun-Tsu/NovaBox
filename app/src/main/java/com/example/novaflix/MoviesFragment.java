package com.example.novaflix;

import static com.example.novaflix.LoginFragment.PREF_NAME;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoviesFragment extends Fragment {

    private RecyclerView latestMoviesRecyclerView;
    private RecyclerView featuredMoviesRecyclerView;
    private RecyclerView othersMoviesRecyclerView;
    private RecyclerView ratedMoviesRecyclerView;
    private RecyclerView actionMoviesRecyclerView;
    private RecyclerView eighteenMoviesRecyclerView;
    private RecyclerView scifiMoviesRecyclerView;
    private RecyclerView hotMoviesRecyclerView;
    private RecyclerView bestMoviesRecyclerView;

    private MoviesAdapter latestMoviesAdapter;
    private MoviesAdapter scifiMoviesAdapter;
    private MoviesAdapter hotMoviesAdapter;
    private MoviesAdapter actionMoviesAdapter;
    private MoviesAdapter eighteenMoviesAdapter;
    private MoviesAdapter ratedMoviesAdapter;
    private MoviesAdapter featuredMoviesAdapter;
    private MoviesAdapter othersMoviesAdapter;
    private MoviesAdapter bestMoviesAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Movies> latestMoviesList;
    private List<Movies> featuredMoviesList;
    private List<Movies> othersMoviesList;
    private List<Movies> eighteenMoviesList;

    private List<Movies> scifiMoviesList;
    private List<Movies> hotMoviesList;
    private List<Movies> ratedMoviesList;

    private List<Movies> actionMoviesList;
    private List<Movies> bestMoviesList;
    private List<DownloadItem> downloadItems = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


    private static final String TAG = "MoviesFragment";
    private static final String PREMIUM_PLAN = "Premium";
    private static final String PREMIUM_PRO_PLAN = "Premium Pro";
    private static final String SUBSCRIPTION_BASIC = "Basic";
    private static final int MOVIES_PER_CATEGORY = 25;
    private EditText searchField;
    private ImageButton downloadsIcon;
    private long downloadId; // To track the current download
    private ImageButton searchIcon;
    private LoadingDialogFragment loadingDialogFragment;
    private boolean isDialogVisible = false;
    private DownloadListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movies, container, false);



        // Initialize RecyclerViews
        latestMoviesRecyclerView = view.findViewById(R.id.latestMoviesRecyclerView);
        ratedMoviesRecyclerView = view.findViewById(R.id.ratedMoviesRecyclerView);
        actionMoviesRecyclerView = view.findViewById(R.id.actionMoviesRecyclerView);
        eighteenMoviesRecyclerView = view.findViewById(R.id.eighteenMoviesRecyclerView);
        scifiMoviesRecyclerView = view.findViewById(R.id.scifiMoviesRecyclerView);
        hotMoviesRecyclerView = view.findViewById(R.id.hotMoviesRecyclerView);
        featuredMoviesRecyclerView = view.findViewById(R.id.featuredMoviesRecyclerView);
        othersMoviesRecyclerView = view.findViewById(R.id.othersMoviesRecyclerView);
        bestMoviesRecyclerView = view.findViewById(R.id.bestMoviesRecyclerView);
        downloadsIcon=getActivity().findViewById(R.id.downloads_icon);

        // Set up each RecyclerView with a LinearLayoutManager
        setupRecyclerView(latestMoviesRecyclerView);
        setupRecyclerView(hotMoviesRecyclerView);
        setupRecyclerView(actionMoviesRecyclerView);
        setupRecyclerView(ratedMoviesRecyclerView);
        setupRecyclerView(eighteenMoviesRecyclerView);
        setupRecyclerView(scifiMoviesRecyclerView);
        setupRecyclerView(featuredMoviesRecyclerView);
        setupRecyclerView(othersMoviesRecyclerView);
        setupRecyclerView(bestMoviesRecyclerView);

        // Initialize movie lists
        latestMoviesList = new ArrayList<>();
        scifiMoviesList=new ArrayList<>();
        hotMoviesList=new ArrayList<>();
        eighteenMoviesList=new ArrayList<>();
        ratedMoviesList=new ArrayList<>();
        actionMoviesList=new ArrayList<>();
        featuredMoviesList = new ArrayList<>();
        othersMoviesList = new ArrayList<>();
        bestMoviesList = new ArrayList<>();

        // Set up adapters
        latestMoviesAdapter = new MoviesAdapter(latestMoviesList, getContext(), this::downloadVideo, this::openPlayerActivity);
        featuredMoviesAdapter = new MoviesAdapter(featuredMoviesList, getContext(), this::downloadVideo, this::openPlayerActivity);
        othersMoviesAdapter = new MoviesAdapter(othersMoviesList, getContext(), this::downloadVideo, this::openPlayerActivity);
        bestMoviesAdapter = new MoviesAdapter(bestMoviesList, getContext(), this::downloadVideo, this::openPlayerActivity);
        hotMoviesAdapter=new MoviesAdapter(hotMoviesList,getContext(),this::downloadVideo,this::openPlayerActivity);
        actionMoviesAdapter=new MoviesAdapter(actionMoviesList,getContext(),this::downloadVideo,this::openPlayerActivity);
scifiMoviesAdapter=new MoviesAdapter(scifiMoviesList,getContext(),this::downloadVideo,this::openPlayerActivity);
eighteenMoviesAdapter=new MoviesAdapter(eighteenMoviesList,getContext(),this::downloadVideo,this::openPlayerActivity);
ratedMoviesAdapter=new MoviesAdapter(ratedMoviesList,getContext(),this::downloadVideo,this::openPlayerActivity);
        // Link adapters to RecyclerViews
        latestMoviesRecyclerView.setAdapter(latestMoviesAdapter);
        eighteenMoviesRecyclerView.setAdapter(eighteenMoviesAdapter);
        ratedMoviesRecyclerView.setAdapter(ratedMoviesAdapter);
        actionMoviesRecyclerView.setAdapter(actionMoviesAdapter);
        scifiMoviesRecyclerView.setAdapter(scifiMoviesAdapter);
        hotMoviesRecyclerView.setAdapter(hotMoviesAdapter);
        featuredMoviesRecyclerView.setAdapter(featuredMoviesAdapter);
        othersMoviesRecyclerView.setAdapter(othersMoviesAdapter);
        bestMoviesRecyclerView.setAdapter(bestMoviesAdapter);

        // Fetch and load movies from Backendless
        checkAndHandleMoviesList();

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            clearMoviesFromSharedPreferences();
            fetchM3UFileURLFromBackendless();
            swipeRefreshLayout.setRefreshing(false); // Stop the refresh animation once data is updated
        });
        searchField = getActivity().findViewById(R.id.search_field);
        searchIcon = getActivity().findViewById(R.id.search_icon);

        // Set listener for search icon
        searchIcon.setOnClickListener(v -> {

            if (searchField.getVisibility() == View.VISIBLE) {
                // If search field is already visible, hide it
                searchField.setVisibility(View.GONE);
                searchIcon.setVisibility(View.VISIBLE);  // Show the search icon again
                hideKeyboard();  // Hide the keyboard
            } else {
                // Show search field when search icon is clicked
                searchField.setVisibility(View.VISIBLE);
                searchIcon.setVisibility(View.GONE);  // Hide the search icon
                searchField.requestFocus();  // Focus on the EditText
                showKeyboard();  // Show the keyboard
            }
        });

        // Set listener for search field text change
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override

            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String query = charSequence.toString().trim();

                if (query.isEmpty()) {
                    // If the query is empty, show the full list again
                    updateMovieList(latestMoviesList);
                    updateMovieList(actionMoviesList);
                    updateMovieList(eighteenMoviesList);
                    updateMovieList(scifiMoviesList);
                    updateMovieList(hotMoviesList);
                    updateMovieList(ratedMoviesList);
                    updateMovieList(bestMoviesList);
                    updateMovieList(featuredMoviesList);
                    updateMovieList(othersMoviesList);// Reset to original list
                } else {
                    // Otherwise, filter the list based on the query
                    filterMoviesList(query);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });


        downloadsIcon.setOnClickListener(v -> {
            // Open the download list dialog
            showDownloadListDialog();
        });



        return view;
    }

    private void setupRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

    }
    @Override
    public void onPause() {
        super.onPause();
        // Clear Glide image loading tasks to avoid any hangs
        Glide.with(requireContext()).pauseRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume Glide image loading requests when fragment becomes visible again
        Glide.with(requireContext()).resumeRequests();

        // Reinitialize ExecutorService if it's already shut down
        if (executorService.isShutdown()) {
            Log.e(TAG, "ExecutorService is shut down, reinitializing.");
            executorService = Executors.newSingleThreadExecutor();  // Reinitialize ExecutorService
        }
    }


    private void fetchM3UFileURLFromBackendless() {
        List<Movies> moviesFromPrefs = loadMoviesFromSharedPreferences();
        if (!moviesFromPrefs.isEmpty()) {
            // If movies are already loaded, use them
            distributeMovies(moviesFromPrefs);
        } else {
            // Fetch movies from Backendless if not found in SharedPreferences
            Backendless.Data.of("Playlist").find(new AsyncCallback<List<Map>>() {
                @Override
                public void handleResponse(List<Map> response) {
                    if (response != null && !response.isEmpty()) {
                        boolean moviePlaylistFound = false;
                        for (Map<String, Object> playlist : response) {
                            String subscriptionType = (String) playlist.get("sub_type");
                            if ("movies".equals(subscriptionType)) {
                                String movieUrl = (String) playlist.get("m3u");
                                downloadAndProcessM3UFile(movieUrl);
                                moviePlaylistFound = true;
                                break;
                            }
                        }
                        if (!moviePlaylistFound) {
                            showErrorMessage("No movie playlist found in the database.");
                        }
                    } else {
                        showErrorMessage("No playlists found in the database.");
                    }
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    showErrorMessage("Failed to fetch movie playlists. Please check your connection and try again.");
                }
            });
        }
    }

    private void clearMoviesFromSharedPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("movies_pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Clear all data in the SharedPreferences
        editor.clear();
        editor.apply(); // Apply the changes to actually remove the data

        // Confirm the action with a log or a toast message
        Log.d(TAG, "Movies data cleared from SharedPreferences.");
        Toast.makeText(getActivity(), "Movies data cleared successfully.", Toast.LENGTH_SHORT).show();
    checkAndHandleMoviesList();
    }



    private void showErrorMessage(String message) {
        // Show a Toast for minor issues
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        // Optionally, show a dialog with a retry option for critical issues
        new AlertDialog.Builder(getContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> fetchM3UFileURLFromBackendless())
                .setNegativeButton("Cancel", null)
                .show();
    }
    @Override

    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();  // Cleanup the ExecutorService to prevent memory leaks

        // Nullify references to avoid memory leaks
        latestMoviesRecyclerView.setAdapter(null);
        hotMoviesRecyclerView.setAdapter(null);
        actionMoviesRecyclerView.setAdapter(null);
        ratedMoviesRecyclerView.setAdapter(null);
        eighteenMoviesRecyclerView.setAdapter(null);
        scifiMoviesRecyclerView.setAdapter(null);
        featuredMoviesRecyclerView.setAdapter(null);
        othersMoviesRecyclerView.setAdapter(null);
        bestMoviesRecyclerView.setAdapter(null);

        latestMoviesList.clear();
        hotMoviesList.clear();
        scifiMoviesList.clear();
        eighteenMoviesList.clear();
        actionMoviesList.clear();
        ratedMoviesList.clear();
        featuredMoviesList.clear();
        othersMoviesList.clear();
        bestMoviesList.clear();

        // Optionally, set other member variables to null if they're not needed anymore
        latestMoviesAdapter = null;
        hotMoviesAdapter = null;
        actionMoviesAdapter = null;
        ratedMoviesAdapter = null;
        eighteenMoviesAdapter = null;
        scifiMoviesAdapter = null;
        featuredMoviesAdapter = null;
        othersMoviesAdapter = null;
        bestMoviesAdapter = null;


    }
    private void checkAndHandleMoviesList() {
        Log.d(TAG, "checkAndHandleMoviesList() called");
        List<Movies> movieList = loadMoviesFromSharedPreferences();

        if (movieList.isEmpty()) {
            Log.d(TAG, "Movies list is empty, loading default movies.");
            fetchM3UFileURLFromBackendless(); // Method to load default movies if the list is empty
        } else {
            Log.d(TAG, "Movies list is not empty, distributing movies.");
            distributeMovies(movieList);
        }
    }


    private boolean isFetchingMovies = false; // Flag to prevent recursion

    private List<Movies> loadMoviesFromSharedPreferences() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("movies_pref", Context.MODE_PRIVATE);

        // Retrieve the stored JSON string for the movies list
        String moviesJson = sharedPreferences.getString("movies_list", null);
        Gson gson = new Gson();

        // Deserialize the JSON string back into a List of Movies objects
        List<Movies> movieList = moviesJson != null
                ? gson.fromJson(moviesJson, new TypeToken<List<Movies>>() {}.getType())
                : new ArrayList<>();

        Log.d(TAG, "Movies list loaded from SharedPreferences. Size: " + movieList.size());

        // Check if the movie list is empty
        if (movieList.isEmpty()) {
            if (!isFetchingMovies) { // Prevent recursion if fetching is not already in progress
                Log.d(TAG, "Movies list is empty, loading default movies.");
                isFetchingMovies = true; // Set the flag to indicate fetching is in progress
                fetchM3UFileURLFromBackendless(); // Fetch default movies
            } else {
                Log.d(TAG, "Fetch operation is already in progress. Skipping.");
            }
        } else {
            Log.d(TAG, "Movies list is not empty, distributing movies.");
            distributeMovies(movieList); // Run the distribute method if not empty
        }

        return movieList;
    }

    public void onMoviesFetched() {
        isFetchingMovies = false; // Reset the flag when the fetch operation is complete
    }




    private void saveMoviesToSharedPreferences(List<Movies> movieList) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("movies_pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Load existing movies list from SharedPreferences
        String existingMoviesJson = sharedPreferences.getString("movies_list", null);
        Gson gson = new Gson();

        // Deserialize existing movies list
        List<Movies> existingMovieList = existingMoviesJson != null
                ? gson.fromJson(existingMoviesJson, new TypeToken<List<Movies>>() {}.getType())
                : new ArrayList<>();

        // Check if the new list is the same as the existing one
        if (existingMovieList.equals(movieList)) {
            Log.d(TAG, "Movies list is the same, not saving.");
            return; // Skip saving if the list is identical
        }

        // Serialize the new list and save to SharedPreferences
        String moviesJson = gson.toJson(movieList);
        editor.putString("movies_list", moviesJson);
        editor.apply(); // Apply changes asynchronously

        Log.d(TAG, "Movies list saved to SharedPreferences.");
    }



    private void downloadAndProcessM3UFile(String fileURL) {
        List<Movies> movieList = loadMoviesFromSharedPreferences();

        if (movieList.isEmpty()) {
            DialogUtils.showLoadingDialog(requireActivity());

            if (executorService.isShutdown()) {
                Log.e(TAG, "ExecutorService is already shut down. Task rejected.");
                return;
            }

            executorService.execute(() -> {
                List<Movies> completeMovieList = new ArrayList<>();
                try {
                    URL url = new URL(fileURL);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000); // 5 seconds timeout
                    urlConnection.setReadTimeout(5000);    // 5 seconds read timeout

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("#EXTINF")) {
                                String title = line.substring(line.indexOf(",") + 1).trim();
                                String videoURL = reader.readLine();
                                if (videoURL != null) {
                                    completeMovieList.add(new Movies(title, 0, videoURL, extractLogoUrl(line)));
                                }
                            }
                        }

                        // Save the complete list to SharedPreferences after all processing is done
                        saveMoviesToSharedPreferences(completeMovieList);

                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing M3U file: " + e.getMessage());
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Error loading movies. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error opening connection: " + e.getMessage());
                } finally {
                    // Ensure the loading dialog is dismissed
                    requireActivity().runOnUiThread(() -> {
                        DialogUtils.dismissLoadingDialog(requireActivity());
                        // Distribute all movies only once
                        distributeMovies(completeMovieList);
                    });
                }
            });
        } else {
            // If movies list is already available, distribute it
            distributeMovies(movieList);
        }
    }



    private void distributeMovies(List<Movies> movieList) {
        // Clear previous lists
        latestMoviesList.clear();
        featuredMoviesList.clear();
        othersMoviesList.clear();
        actionMoviesList.clear();
        eighteenMoviesList.clear();
        ratedMoviesList.clear();
        scifiMoviesList.clear();
        hotMoviesList.clear();
        bestMoviesList.clear();

        // Ensure movieList is not empty
        if (movieList == null || movieList.isEmpty()) {
            updateMovieAdapters();
            return;
        }

        int totalMovies = movieList.size();
        int countPerCategory = totalMovies / 10;

        // Distribute movies into categories
        for (int i = 0; i < totalMovies; i++) {
            Movies movie = movieList.get(i);
            if (i < countPerCategory) {
                latestMoviesList.add(movie);
            } else if (i < 2 * countPerCategory) {
                featuredMoviesList.add(movie);
            } else if (i < 3 * countPerCategory) {
                bestMoviesList.add(movie);

            }

            else if (i < 4 * countPerCategory) {
                scifiMoviesList.add(movie);
            }
            else if (i < 5 * countPerCategory) {
                hotMoviesList.add(movie);
            }   else if (i < 6 * countPerCategory) {
                eighteenMoviesList.add(movie);
            }   else if (i < 7 * countPerCategory) {
                ratedMoviesList.add(movie);
            }   else if (i < 8 * countPerCategory) {
                othersMoviesList.add(movie);
            }

            else {
                actionMoviesList.add(movie);
            }
        }

        // Update adapters once all lists are populated
        updateMovieAdapters();
    }



    private void updateMovieAdapters() {
        // Notify data changed only once for all adapters to minimize UI updates
        latestMoviesAdapter.notifyDataSetChanged();
        featuredMoviesAdapter.notifyDataSetChanged();
        scifiMoviesAdapter.notifyDataSetChanged();
        hotMoviesAdapter.notifyDataSetChanged();
        eighteenMoviesAdapter.notifyDataSetChanged();
        ratedMoviesAdapter.notifyDataSetChanged();
        bestMoviesAdapter.notifyDataSetChanged();
        othersMoviesAdapter.notifyDataSetChanged();
    }



    // Improved updateMovieList to handle empty checks better
    private void updateMovieList(List<Movies> filteredMovies) {
        latestMoviesList.clear();
        latestMoviesList.addAll(filteredMovies); // Add all filtered movies

        // Only update adapter for the latestMoviesRecyclerView
        latestMoviesAdapter.notifyDataSetChanged();

        bestMoviesList.clear();
        bestMoviesList.addAll(filteredMovies); // Add all filtered movies
        bestMoviesAdapter.notifyDataSetChanged();

        featuredMoviesList.clear();
        featuredMoviesList.addAll(filteredMovies);
        featuredMoviesAdapter.notifyDataSetChanged();

        hotMoviesList.clear();
        hotMoviesList.addAll(filteredMovies);
        hotMoviesAdapter.notifyDataSetChanged();

        actionMoviesList.clear();
        actionMoviesList.addAll(filteredMovies);
        actionMoviesAdapter.notifyDataSetChanged();

        scifiMoviesList.clear();
        scifiMoviesList.addAll(filteredMovies);
        scifiMoviesAdapter.notifyDataSetChanged();

        ratedMoviesList.clear();
        ratedMoviesList.addAll(filteredMovies);
        ratedMoviesAdapter.notifyDataSetChanged();

        othersMoviesList.clear();
        othersMoviesList.addAll(filteredMovies);
        othersMoviesAdapter.notifyDataSetChanged();
    }

    private boolean canDownload() {
        String userSubscription = getCurrentUserSubscriptionType();

        if (userSubscription == null) {
            Toast.makeText(getContext(), "Please log in to download videos.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (userSubscription.equals(SUBSCRIPTION_BASIC) || userSubscription.equals(PREMIUM_PRO_PLAN)) {
            return true; // User has a valid subscription to download
        } else {
            Toast.makeText(getContext(), "Only Premium or Premium Pro members can download videos.", Toast.LENGTH_SHORT).show();
            return false; // User does not have permission to download
        }
    }



    private void downloadVideo(String videoUrl, String fileName) {
        // Check if the user can download videos
        if (!canDownload()) {
            return; // Exit if the user cannot download
        }

        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "Download URL is empty or null.");
            Toast.makeText(getContext(), "Invalid video URL.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Start the actual download
        startDownload(videoUrl, fileName);
    }

    private void startDownload(String videoUrl, String fileName) {
        try {
            // Validate video URL
            if (TextUtils.isEmpty(videoUrl)) {
                Log.e("Download", "Invalid video URL");
                return;
            }

            // Check if the movie is already in the list
            for (DownloadItem item : downloadItems) {
                if (item.getFileName().equals(fileName)) {
                    // Movie already exists in the list
                    Toast.makeText(getContext(), "This movie is already in the download list.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Check if the maximum number of downloads (3) is reached
            if (downloadItems.size() >= 3) {
                Toast.makeText(getContext(), "Max number of concurrent downloads reached (3).", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create request for download
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setTitle(fileName);

            // Set the destination path for the file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, sanitizeFileName(fileName) + ".mp4");
            } else {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, sanitizeFileName(fileName) + ".mp4");
            }

            // Get the DownloadManager system service
            DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                Log.e("Download", "DownloadManager service is not available.");
                Toast.makeText(getContext(), "Unable to initiate download.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Enqueue the download request
            long downloadId = manager.enqueue(request);

            // Initialize downloadItems if null
            if (downloadItems == null) {
                downloadItems = new ArrayList<>();
            }

            // Add the download item with the downloadId, fileName, initial progress 0, and fileUrl
            downloadItems.add(new DownloadItem(fileName, 0, videoUrl, downloadId));

            // Initialize the adapter if it's null
            if (adapter == null) {
                adapter = new DownloadListAdapter(getContext(), downloadItems);
            }

            // Notify the adapter to update the list
            adapter.notifyDataSetChanged();

            // Show the download list dialog if it's the first download
            if (adapter.getCount() <= 3) {
                showDownloadListDialog();
            }

            // Register the receiver to track download completion
            getContext().registerReceiver(progressReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            // Catch any exceptions and log the error
            Log.e("Download", "Error: " + e.getMessage());
            Toast.makeText(getContext(), "Failed to start download.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDownloadListDialog() {
        // Query the DownloadManager to fetch existing downloads
        populateDownloadItems();


        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomDialogStyle);


        // Inflate the custom dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.download_list_dialog, null);

        // Set the dialog layout
        builder.setView(dialogView);
        builder.setCancelable(false);

        // Set up ListView with adapter
        ListView listView = dialogView.findViewById(R.id.downloadListView);

        // Initialize the adapter if it's null
        if (adapter == null) {
            adapter = new DownloadListAdapter(getContext(), downloadItems);
        }

        // Bind adapter to ListView
        listView.setAdapter(adapter);

        // Set dialog title
        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText("Downloading Files");

        // Add a "Close" button
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Show the dialog
        builder.show();
    }
    private void populateDownloadItems() {
        // Get the DownloadManager instance
        DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;

        // Query the DownloadManager
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = manager.query(query);

        if (cursor != null) {
            downloadItems.clear(); // Clear existing items to avoid duplication

            // Check if all required columns are available
            int idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
            int titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
            int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI); // Retrieve file URL
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

            // Ensure all indices are valid
            if (idIndex == -1 || titleIndex == -1 || uriIndex == -1 ||
                    statusIndex == -1 || downloadedIndex == -1 || totalIndex == -1) {
                Log.e("DownloadManager", "One or more columns are missing.");
                cursor.close();
                return;
            }

            // Iterate through the cursor
            while (cursor.moveToNext()) {
                // Extract download details safely
                long id = cursor.getLong(idIndex);
                String title = cursor.getString(titleIndex);
                String fileUrl = cursor.getString(uriIndex); // Get the file URL
                int status = cursor.getInt(statusIndex);
                int bytesDownloaded = cursor.getInt(downloadedIndex);
                int bytesTotal = cursor.getInt(totalIndex);
                int progress = (bytesTotal > 0) ? (int) ((bytesDownloaded * 100L) / bytesTotal) : 0;

                // Add to download items list
                DownloadItem item = new DownloadItem(
                        title != null ? title : "Unknown", // Use "Unknown" if title is null
                        progress,
                        fileUrl != null ? fileUrl : "", // Use an empty string if URL is null
                        id
                );
                item.setPaused(status == DownloadManager.STATUS_PAUSED);
                downloadItems.add(item);
            }
            cursor.close();
        }

        // Notify the adapter of data changes
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            // Update the download items list
            populateDownloadItems();
            // Call the runnable again after 1 second (1000ms)
            progressHandler.postDelayed(this, 1000);
        }
    };



    private String getCurrentUserSubscriptionType() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Retrieve the saved preferences
        String email = sharedPreferences.getString("email", null);  // default is null if not found
        String subscription = sharedPreferences.getString("plan", null);


            Log.d(TAG, "User subscription plan retrieved: " + subscription);


        return subscription;
    }

    // Utility method to sanitize file names
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    }

    // Add this method to handle the menu click event for downloads

    private void openPlayerActivity(Movies movie) {
        Intent intent = new Intent(getContext(), MoviesPlayerActivity.class);
        intent.putExtra("videoUrl", movie.getDownloadUrl()); // Pass the video URL
        intent.putExtra("title", movie.getTitle()); // Pass the movie title for display
        startActivity(intent);
    }
    private String extractLogoUrl(String line) {
        String logoUrl = null; // Use null initially
        String[] parts = line.split(" ");

        for (String part : parts) {
            if (part.startsWith("tvg-logo=\"") && part.endsWith("\"")) {
                int startIndex = part.indexOf("\"") + 1;
                int endIndex = part.lastIndexOf("\"");
                if (startIndex >= 0 && endIndex > startIndex) {
                    logoUrl = part.substring(startIndex, endIndex);
                }
                break; // Exit after finding the logo
            }
        }

        // Check if the logoUrl is valid or empty
        if (logoUrl == null || logoUrl.isEmpty() || logoUrl.equals("null")) {
            Log.w(TAG, "Invalid logo URL found in line: " + line);
            logoUrl = String.valueOf(R.drawable.featured); // Use a default drawable
        }

        return logoUrl;
    }

    private void loadFromPreferences() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Retrieve the saved preferences
        String email = sharedPreferences.getString("email", null);  // default is null if not found
        String plan = sharedPreferences.getString("plan", "Basic");  // Provide a default value
        boolean rememberMe = sharedPreferences.getBoolean("rememberMe", false);  // default is false if not found

        // Use the retrieved values as needed
        Log.d("MoviesFragment", "Email: " + email);
        Log.d("MoviesFragment", "Plan: " + plan);
        Log.d("MoviesFragment", "Remember Me: " + rememberMe);

        // Example: Set the plan on a TextView or other UI element in your MoviesFragment

    }
    private BroadcastReceiver progressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (downloadId == -1) return;  // If no download ID is provided, exit early

                // Query the download status for the specific downloadId
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = manager.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int titleColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                        int bytesDownloadedColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                        int bytesTotalColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                        // Ensure all necessary columns are available
                        if (statusColumnIndex >= 0 && titleColumnIndex >= 0 &&
                                bytesDownloadedColumnIndex >= 0 && bytesTotalColumnIndex >= 0) {

                            int status = cursor.getInt(statusColumnIndex);
                            String fileName = cursor.getString(titleColumnIndex);
                            int bytesDownloaded = cursor.getInt(bytesDownloadedColumnIndex);
                            int bytesTotal = cursor.getInt(bytesTotalColumnIndex);

                            // Calculate progress and update if needed
                            if (bytesTotal > 0) {
                                int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);

                                // Find and update the corresponding download item
                                for (DownloadItem item : downloadItems) {
                                    if (item.getFileName().equals(fileName)) {
                                        item.setProgress(progress);  // Update the progress
                                        break;
                                    }
                                }

                                // Notify the adapter to refresh the view
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        // Start periodic updates when the fragment starts
        progressHandler.post(progressRunnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop periodic updates when the fragment stops
        progressHandler.removeCallbacks(progressRunnable);
    }





    private void filterMoviesList(String query) {
        List<Movies> filteredMovies = new ArrayList<>();

        // Filter the movies from the latest movies list
        for (Movies movie : latestMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        // Filter the movies from the popular movies list
        for (Movies movie : othersMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        // Filter the movies from the trending movies list
        for (Movies movie : bestMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }
        for (Movies movie : hotMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        for (Movies movie : actionMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        for (Movies movie : eighteenMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        for (Movies movie : ratedMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }

        for (Movies movie : scifiMoviesList) {
            if (movie.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredMovies.add(movie);
            }
        }
        // Update the RecyclerView
        updateMovieList(filteredMovies);
    }


    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && searchField != null) {
            imm.showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT);
        }
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }


}
