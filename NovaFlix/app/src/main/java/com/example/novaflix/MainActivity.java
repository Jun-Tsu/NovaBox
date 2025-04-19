package com.example.novaflix;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView toolbarTitle;
    private BottomNavigationView bottomNavigationView;
    private ImageButton menuIcon, settingsIcon;
    private UpdateManager updateManager;
    private static final int EXIT_TIME_INTERVAL = 2000; // 2 seconds time interval for double back press
    private long lastBackPressedTime = 0; // Tracks the last back pressed time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout
        setContentView(R.layout.activity_main);

        // Initialize the UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        }

        // Initialize UI elements
        toolbarTitle = findViewById(R.id.toolbar_title);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        menuIcon = findViewById(R.id.menu_icon);
        settingsIcon = findViewById(R.id.settings_icon);
        updateManager = new UpdateManager(this);

        // Perform lightweight operations in the main thread
        setupMenuDropdown(); // For fragment-based actions
        setupSettingsDropdown(); // For dialogs (Settings, Exit, Updates)

        // Initialize fragment and title maps
        Map<Integer, Fragment> fragmentMap = new HashMap<>();
        Map<Integer, String> titleMap = new HashMap<>();

        // Populate the maps for bottom navigation
        fragmentMap.put(R.id.nav_live, new LiveFragment());
        fragmentMap.put(R.id.nav_movies, new MoviesFragment());
        fragmentMap.put(R.id.nav_scores, new RewardFragment());
        fragmentMap.put(R.id.nav_account, new AccountFragment());

        titleMap.put(R.id.nav_live, "Live TV");
        titleMap.put(R.id.nav_movies, "Movies");
        titleMap.put(R.id.nav_scores, "Rewards");
        titleMap.put(R.id.nav_account, "Account");

        // Perform heavier operations in a background thread
        new Thread(() -> {
            // Update the UI on the main thread after initialization
            runOnUiThread(() -> {
                // Set up navigation item selected listener
                bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                    // Update fragment based on selection
                    Fragment selectedFragment = fragmentMap.get(item.getItemId());
                    String title = titleMap.get(item.getItemId());

                    if (selectedFragment != null) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, selectedFragment)
                                .commit();

                        toolbarTitle.setText(title);
                    }

                    return true;
                });

                // Load the default fragment if no saved state exists
                if (savedInstanceState == null) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_live); // Load LiveFragment by default
                }
            });
        }).start();
    }


    // Setup the menu dropdown for the menu icon (fragment-based actions)
    private void setupMenuDropdown() {
        menuIcon.setOnClickListener(v -> {
            // Create and show the PopupMenu for menu_icon (fragment actions)
            PopupMenu menuPopup = new PopupMenu(MainActivity.this, menuIcon);

            // Inflate the menu from XML
            menuPopup.getMenuInflater().inflate(R.menu.menu_main, menuPopup.getMenu());

            // Apply custom style to the PopupMenu for background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                menuPopup.setForceShowIcon(true);  // Forces icons to show in all screen sizes
            }

            // Customizing the icons for each menu item (optional)
            menuPopup.getMenu().getItem(0).setIcon(R.drawable.download);  // Custom icon for option 1
            menuPopup.getMenu().getItem(1).setIcon(R.drawable.switchh);  // Custom icon for option 2
            menuPopup.getMenu().getItem(2).setIcon(R.drawable.star);  // Custom icon for option 3

            // Reflection-based approach to set background for the PopupMenu
            try {
                Field field = PopupMenu.class.getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuHelper = field.get(menuPopup);

                if (menuHelper != null) {
                    // Set the background drawable for the PopupMenu
                    Method method = menuHelper.getClass().getDeclaredMethod("setBackgroundDrawable", android.graphics.drawable.Drawable.class);
                    method.setAccessible(true);
                    method.invoke(menuHelper, getResources().getDrawable(R.drawable.custom_popup_background)); // Set the custom background
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Handle item clicks for fragment options
            menuPopup.setOnMenuItemClickListener(item -> {
                Fragment selectedFragment = null;

                // Determine which fragment should be opened based on the selected menu item
                if (item.getItemId() == R.id.item_option_1) {
                    // Open Download Fragment
                    selectedFragment = new DownloadFragment();
                } else if (item.getItemId() == R.id.item_option_2) {
                    // Open Favourite Fragment
                    selectedFragment = new FavouriteFragment();
                } else if (item.getItemId() == R.id.item_option_3) {
                    // Trigger the Switch Account Dialog (Logout/SignIn Flow)
                    showSwitchAccountDialog();
                }

                // If a fragment is selected, replace it, otherwise show a Toast
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commitNowAllowingStateLoss();
                }
                    return true;
            });

            // Show the menu
            menuPopup.show();
        });
    }


    // Setup the settings dropdown for the settings icon (dialogs for Updates, Exit)
    private void setupSettingsDropdown() {
        settingsIcon.setOnClickListener(v -> {
            // Create and show the PopupMenu for settings_icon (dialogs)
            PopupMenu settingsPopup = new PopupMenu(MainActivity.this, settingsIcon, Gravity.END);

            // Apply custom style to the PopupMenu
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                settingsPopup.setForceShowIcon(true);  // Forces icons to show in all screen sizes
            }

            // Inflate the menu from XML
            settingsPopup.getMenuInflater().inflate(R.menu.menu_settings, settingsPopup.getMenu());

            // Customizing the icons for each menu item (optional)
            settingsPopup.getMenu().getItem(0).setIcon(R.drawable.update);  // Custom icon for option 1
            settingsPopup.getMenu().getItem(1).setIcon(R.drawable.settingss);  // Custom icon for option 2
            settingsPopup.getMenu().getItem(2).setIcon(R.drawable.exit);  // Custom icon for option 3

            // Reflection-based approach to set background (this should be done post-menu inflation)
            try {
                Field field = PopupMenu.class.getDeclaredField("mPopup");
                field.setAccessible(true);
                Object menuHelper = field.get(settingsPopup);

                if (menuHelper != null) {
                    // Set the background drawable for the PopupMenu
                    Method method = menuHelper.getClass().getDeclaredMethod("setBackgroundDrawable", android.graphics.drawable.Drawable.class);
                    method.setAccessible(true);
                    method.invoke(menuHelper, getResources().getDrawable(R.drawable.custom_popup_background));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Handle item clicks for settings options (dialogs)
            settingsPopup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.item_settings_option_1) {
                    // Show Dialog for "Check Updates"
                    showCheckUpdatesDialog();
                } else if (item.getItemId() == R.id.item_settings_option_2) {
                    // Show Dialog for "Exit"
                    showExitDialog();
                }
                return true;
            });

            // Show the settings menu
            settingsPopup.show();
        });
    }


    // Show dialog for "Check Updates"
    private void showCheckUpdatesDialog() {
        new AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle("Check for Updates")
                .setMessage("Would you like to check for updates?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show a loading message
                    Toast.makeText(MainActivity.this, "Checking for updates...", Toast.LENGTH_SHORT).show();

                    // Call the method to check for updates via UpdateManager
                    updateManager.checkForUpdates();
                })
                .setNegativeButton("No", null)
                .show();
    }
    // Show dialog for "Exit"
    private void showExitDialog() {
        new AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Cancel all downloads
                    cancelAllDownloads();

                    // Exit the app and kill all processes
                    finishAndRemoveTask();
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                })
                .setNegativeButton("No", null)
                .show();
    }

    // Handle back button press to implement double tap to exit
    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();

        // Check if the back button is pressed twice within the defined interval (2 seconds)
        if (currentTime - lastBackPressedTime <= EXIT_TIME_INTERVAL) {
            // If pressed twice, exit the app
            super.onBackPressed();
        } else {
            // If pressed once, show the "Press again to exit" toast
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }

        // Update the time of the last back button press
        lastBackPressedTime = currentTime;
    }

    private void showSwitchAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Switch Account")
                .setMessage("Do you want to log out or log in with another account?")
                .setPositiveButton("Log out", (dialog, which) -> {
                    // Perform the logout operation
                    logOutUser();
                })
                .setNegativeButton("Log in", (dialog, which) -> {
                    // Show the login fragment or navigate to login activity
                    navigateToLogin();
                })
                .setNeutralButton("Cancel", null) // Option to cancel the action
                .show();
    }

    // Method to log out the current user (Backendless example)
    private void logOutUser() {
        // Logout the user via Backendless
        Backendless.UserService.logout(new AsyncCallback<Void>() {
            @Override
            public void handleResponse(Void response) {
                // Successfully logged out
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                // Navigate to the login screen after logging out
                navigateToLogin();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                // Handle error (e.g., show a message if logout fails)
                Toast.makeText(MainActivity.this, "Logout failed: " + fault.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to navigate to the login screen (Login Fragment or Login Activity)
    private void navigateToLogin() {
        // Navigate to the Login Fragment or Login Activity
        // If using Fragment:
        Fragment loginFragment = new LoginFragment(); // Replace with your actual login fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .commit();

        // If using an Activity:
        // Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // startActivity(intent);
        // finish(); // Optionally close the current activity
    }

    // Cancel all active downloads
    private void cancelAllDownloads() {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (downloadManager != null) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterByStatus(
                    DownloadManager.STATUS_RUNNING |
                            DownloadManager.STATUS_PAUSED |
                            DownloadManager.STATUS_PENDING
            );

            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        @SuppressLint("Range") long downloadId = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
                        downloadManager.remove(downloadId);
                    }
                }
            }
        }
    }
}
