package com.malikyasir.landlawassist.Auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.malikyasir.landlawassist.Home.MainActivity;
import com.malikyasir.landlawassist.Models.User;
import com.malikyasir.landlawassist.R;

public class SignupForm extends AppCompatActivity {

    private TextInputEditText fullNameInput, emailInput, passwordInput, phoneInput;
    private AutoCompleteTextView userTypeSpinner;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_form);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        phoneInput = findViewById(R.id.phoneInput);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        progressBar = findViewById(R.id.progressBar);

        // Set up user type dropdown
        String[] userTypes = {"Admin", "User", "Guest"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userTypes);
        userTypeSpinner.setAdapter(adapter);

        MaterialButton registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String userType = userTypeSpinner.getText().toString().trim();

        // Input validations
        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters long");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            return;
        }

        if (TextUtils.isEmpty(userType)) {
            userTypeSpinner.setError("User type is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save user details
                            String userId = firebaseUser.getUid();
                            User user = new User(fullName, email, password,phone, userType);

                            // Save user to Firestore or Realtime Database (replace with actual logic)
                            saveUserDetails(userId, user);

                            // Navigate to main activity
                            Intent intent = new Intent(SignupForm.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        String errorMessage = "Registration failed.";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "This email is already registered.";
                        } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                            errorMessage = "Password is too weak.";
                        }
                        Toast.makeText(SignupForm.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserDetails(String userId, User user) {
        // Replace with Firestore or Realtime Database logic
        Toast.makeText(this, "User saved: " + user.getFullName(), Toast.LENGTH_SHORT).show();
    }
}
