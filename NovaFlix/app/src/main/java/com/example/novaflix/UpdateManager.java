package com.example.novaflix;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;

public class UpdateManager {

    private Context context;
    private FirebaseFirestore firestore;

    public UpdateManager(Context context) {
        this.context = context;
        firestore = FirebaseFirestore.getInstance();
    }

    // Method to check for updates from Firestore
    public void checkForUpdates() {
        int currentVersionCode = 1;
        Log.d("UpdateManager", "Current app version code: " + currentVersionCode);

        firestore.collection("updates")
                .orderBy("versionCode", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                            // Safely retrieve the versionCode (as a String, then parse)
                            Object versionCodeObject = document.get("versionCode");
                            Integer latestVersionCode = null;

                            if (versionCodeObject instanceof Long) {
                                latestVersionCode = ((Long) versionCodeObject).intValue();
                            } else if (versionCodeObject instanceof String) {
                                try {
                                    latestVersionCode = Integer.parseInt((String) versionCodeObject);
                                } catch (NumberFormatException e) {
                                    Log.e("UpdateManager", "Invalid versionCode format: " + versionCodeObject);
                                }
                            }

                            String downloadUrl = document.getString("fileUrl");

                            // Log the fetched version code and download URL
                            Log.d("UpdateManager", "Fetched version code from Firestore: " + latestVersionCode);
                            Log.d("UpdateManager", "Download URL: " + downloadUrl);

                            // Compare the version codes
                            if (latestVersionCode != null && latestVersionCode > currentVersionCode) {
                                // A newer version is available, initiate download
                                Log.d("UpdateManager", "A new version is available. Starting download...");
                                downloadUpdate(downloadUrl);
                            } else {
                                // No update available
                                Log.d("UpdateManager", "No new update found.");
                                Toast.makeText(context, "Your app is up to date!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Handle case where no updates are found
                            Log.d("UpdateManager", "No updates found in Firestore.");
                            Toast.makeText(context, "Error: No update information found.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Handle Firestore error
                        Log.e("UpdateManager", "Error getting update: " + task.getException().getMessage());
                        Toast.makeText(context, "Error checking for updates.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Method to download the update APK using DownloadManager
    private void downloadUpdate(String url) {
        // Get the DownloadManager service
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

        // Ensure the URL is valid
        if (url == null || !url.startsWith("http")) {
            Toast.makeText(context, "Invalid download URL!", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri downloadUri = Uri.parse(url);

        // Set up the download request
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setTitle("NovaBox Update");
        request.setDescription("Downloading the latest version of the app...");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Enqueue the request and save the download ID
        long downloadId = downloadManager.enqueue(request);

        // Register a BroadcastReceiver to listen for download completion
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    // Once the download is complete, attempt to install the APK
                    try {
                        installUpdate();
                    } catch (Exception e) {
                        Log.e("UpdateManager", "Error installing update: " + e.getMessage());
                        Toast.makeText(context, "Failed to install update.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);

        Log.d("UpdateManager", "Downloading update from: " + url);
    }


    // Method to install the APK once the download is complete
    private void installUpdate() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "update.apk");

        if (file.exists()) {
            Uri fileUri;

            // For Android N (API level 24) and above, use FileProvider for security
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            } else {
                // For older Android versions, you can use the file URI directly
                fileUri = Uri.fromFile(file);
            }

            // Intent to install the APK
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Add necessary flag to grant read permission
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // For Android 8.0 (API level 26) and above, check if the user is allowed to install from unknown sources
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context.getPackageManager().canRequestPackageInstalls()) {
                    // Start the installation if allowed
                    context.startActivity(intent);
                } else {
                    // Redirect the user to the settings to enable installation from unknown sources
                    Intent settingsIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    Uri uri = Uri.fromFile(file);
                    settingsIntent.setData(uri);
                    context.startActivity(settingsIntent);
                }
            } else {
                // For versions below Android 8.0, simply start the installation process
                context.startActivity(intent);
            }
        } else {
            Toast.makeText(context, "Error: APK file not found.", Toast.LENGTH_LONG).show();
        }
    }


    // Don't forget to unregister the receiver when it's no longer needed
    public void unregisterReceiver(BroadcastReceiver receiver) {
        context.unregisterReceiver(receiver);
    }
}
