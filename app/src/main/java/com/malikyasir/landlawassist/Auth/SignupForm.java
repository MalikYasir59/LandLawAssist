package com.malikyasir.landlawassist.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.malikyasir.landlawassist.Home.MainActivity;
import com.malikyasir.landlawassist.Modelss.User;
import com.malikyasir.landlawassist.R;

public class SignupForm extends AppCompatActivity {

    private TextInputEditText fullNameInput, emailInput, passwordInput, phoneInput;
    private AutoCompleteTextView userTypeSpinner;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_form);

        mAuth = FirebaseAuth.getInstance();

        // Bind UI elements
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        phoneInput = findViewById(R.id.phoneInput);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        progressBar = findViewById(R.id.progressBar);

        String[] userTypes = {"Admin", "User", "Lawyer"};
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

        if (!validateInputs(fullName, email, password, phone, userType)) return;

        progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User newUser = new User(fullName, email, password, phone, userType);
                        saveUserToFirestore(userId, newUser);
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed.";
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String fullName, String email, String password, String phone, String userType) {
        if (TextUtils.isEmpty(fullName)) {
            fullNameInput.setError("Full Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }
        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone number is required");
            return false;
        }
        if (TextUtils.isEmpty(userType)) {
            userTypeSpinner.setError("User type is required");
            return false;
        }
        return true;
    }

    private void saveUserToFirestore(String userId, User user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
