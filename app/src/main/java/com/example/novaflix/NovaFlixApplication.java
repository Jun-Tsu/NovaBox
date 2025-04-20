package com.example.novaflix;

import android.app.Application;
import android.util.Log;
import com.backendless.Backendless;

public class NovaFlixApplication extends Application {
    private static final String TAG = "NovaFlixApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Backendless
        String appId = "9840ABC9-4647-443D-8C64-48653784757E"; // Replace with your Backendless App ID
        String apiKey = "295E7F02-0F49-4086-B587-E720B69F8824"; // Replace with your Backendless API Key
        String serverUrl = "https://api.backendless.com"; // Replace with your server URL if using a custom one

        Backendless.initApp(this, appId, apiKey);

        // Log initialization status
        if (Backendless.isInitialized()) {
            Log.d(TAG, "Backendless initialized successfully!");
        } else {
            Log.e(TAG, "Backendless initialization failed!");
        }
    }
}