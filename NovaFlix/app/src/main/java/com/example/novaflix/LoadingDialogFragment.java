package com.example.novaflix;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;

public class LoadingDialogFragment extends DialogFragment {

    private LottieAnimationView lottieAnimationView;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.TransparentDialog); // Use a custom style if needed

        // Inflate the custom layout
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        // Initialize Lottie animation view
        lottieAnimationView = dialogView.findViewById(R.id.loadingAnimation);
        lottieAnimationView.setAnimation("loading_animation.json"); // Replace with your Lottie file
        lottieAnimationView.loop(true);
        lottieAnimationView.playAnimation();

        builder.setView(dialogView);

        // Create and return the dialog
        Dialog dialog = builder.create();

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Disable touch outside to prevent dismissing the dialog
            dialog.setCanceledOnTouchOutside(false);

            // Optional: Adjust dialog dimensions if needed
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Notify DialogUtils that the dialog has been dismissed
        DialogUtils.onDialogDismissed();
    }
}
