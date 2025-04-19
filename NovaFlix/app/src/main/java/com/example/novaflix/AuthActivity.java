package com.example.novaflix;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initially load the LoginFragment
        if (savedInstanceState == null) {
            loadFragment(new LoginFragment());
        }
    }

    // Method to load a fragment dynamically
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // Assuming fragment_container is the container view ID
        transaction.addToBackStack(null); // Optional: if you want to allow the back button to work
        transaction.commit();
    }

    // Method to navigate to the RegisterFragment (this would be called from the LoginFragment)
    public void navigateToRegister(View view) {
        loadFragment(new RegisterFragment());
    }
}
