package com.example.novaflix;

import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DialogUtils {

    private static boolean isDialogVisible = false;
    private static LoadingDialogFragment loadingDialogFragment;

    // Show the loading dialog
    public static void showLoadingDialog(FragmentActivity activity) {
        if (activity == null || activity.isFinishing()) {
            Log.w("DialogUtils", "Activity is null or finishing. Cannot show dialog.");
            return;
        }

        try {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();

            // Check if the dialog is already present
            Fragment existingDialog = fragmentManager.findFragmentByTag("loadingDialog");
            if (existingDialog != null && existingDialog.isAdded()) {
                Log.d("DialogUtils", "Dialog is already visible. Skipping show.");
                return;
            }

            // Create and show the dialog
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.show(fragmentManager, "loadingDialog");
            isDialogVisible = true;
            Log.d("DialogUtils", "Loading dialog shown.");
        } catch (IllegalStateException e) {
            Log.e("DialogUtils", "Error showing loading dialog: " + e.getMessage());
        }
    }

    // Hide the loading dialog
    public static void dismissLoadingDialog(FragmentActivity activity) {
        if (activity == null || activity.isFinishing()) {
            Log.w("DialogUtils", "Activity is null or finishing. Cannot dismiss dialog.");
            return;
        }

        if (isDialogVisible && loadingDialogFragment != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();

            if (loadingDialogFragment.isAdded()) {
                try {
                    loadingDialogFragment.dismissAllowingStateLoss();
                    isDialogVisible = false;
                    loadingDialogFragment = null;
                    Log.d("DialogUtils", "Loading dialog dismissed.");
                } catch (IllegalStateException e) {
                    Log.e("DialogUtils", "Error dismissing dialog: " + e.getMessage());
                }
            } else {
                Log.d("DialogUtils", "Dialog is not added. Skipping dismiss.");
            }
        } else {
            Log.d("DialogUtils", "No visible dialog to dismiss.");
        }
    }

    // Ensure the flag resets if the dialog is dismissed unexpectedly
    public static void onDialogDismissed() {
        isDialogVisible = false;
        loadingDialogFragment = null;
    }
}
