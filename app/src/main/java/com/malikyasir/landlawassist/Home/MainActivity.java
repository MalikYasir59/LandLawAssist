package com.malikyasir.landlawassist.Home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.malikyasir.landlawassist.Auth.LoginForm;
import com.malikyasir.landlawassist.Auth.SignupForm;
import com.malikyasir.landlawassist.R;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private MaterialButton loginButton;
    private TextView registerText, forgotPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // If user is not authenticated, send them to the login form
            startActivity(new Intent(MainActivity.this, LoginForm.class));
            finish();
        } else {
            // User is authenticated, load the main dashboard layout
            setContentView(R.layout.activity_main);
            setupBottomNavigationView(); // Initialize UI elements
        }
        FirebaseAuth.getInstance().signOut();

    }


    // Set up UI elements related to login form
    private void setupLoginUI() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerText = findViewById(R.id.registerText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        progressBar = findViewById(R.id.progressBar);

        // Set up click listeners
        loginButton.setOnClickListener(v -> {
            hideKeyboard();
            loginUser();
        });

        registerText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignupForm.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v -> sendPasswordReset());
    }

    // Handle login functionality
    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Firebase authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Navigate to home screen
    private void navigateToHome() {
        setContentView(R.layout.activity_main); // Switch to the home screen layout

        // Load home fragment and set up bottom navigation
        loadFragment(new Homefragment());
        setupBottomNavigationView();
    }

    // Set up BottomNavigationView
    private void setupBottomNavigationView() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (bottomNavigationView == null) {
            throw new NullPointerException("BottomNavigationView not found!");
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new Homefragment();
            } else if (item.getItemId() == R.id.nav_legal_resources) {
                selectedFragment = new LegalResourcesFragment();
            } else if (item.getItemId() == R.id.nav_case_management) {
                selectedFragment = new CaseManagementFragment();
            } else if (item.getItemId() == R.id.nav_ai_assistant) {
                selectedFragment = new AIAssistantFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    // Load a fragment into the container
    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Handle password reset functionality
    private void sendPasswordReset() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required to reset password");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Validate user inputs
    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            progressBar.setVisibility(View.GONE);
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            progressBar.setVisibility(View.GONE);
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            progressBar.setVisibility(View.GONE);
            return false;
        }
        return true;
    }

    // Hide the keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
