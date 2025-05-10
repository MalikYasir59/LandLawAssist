package com.malikyasir.landlawassist.Auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.malikyasir.landlawassist.Home.MainActivity;
import com.malikyasir.landlawassist.R;
import com.malikyasir.landlawassist.Modelss.User;

import java.util.ArrayList;
import java.util.List;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class LoginForm extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set status bar color to deep_blue
        getWindow().setStatusBarColor(getResources().getColor(R.color.deep_blue));

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Clear any existing auth state
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        // Redirect if already logged in
        if (mAuth.getCurrentUser() != null) {
            navigateToMainActivity();
            return;
        }

        // Set layout
        setContentView(R.layout.activity_login_form);

        // Bind UI elements
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton loginButton = findViewById(R.id.loginButton);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        TextView registerText = findViewById(R.id.registerText);

        // Set up listeners
        loginButton.setOnClickListener(v -> loginUser());
        registerText.setOnClickListener(v -> startActivity(new Intent(this, SignupForm.class)));
        forgotPasswordText.setOnClickListener(v -> sendPasswordResetEmail());
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .get()
                            .addOnSuccessListener(document -> {
                                if (document.exists()) {
                                    String userType = document.getString("userType");
                                    // Store userType in SharedPreferences
                                    getSharedPreferences("app_settings", MODE_PRIVATE)
                                        .edit().putString("user_type", userType).apply();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                }
                            });
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }
        return true;
    }

    private void navigateToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
