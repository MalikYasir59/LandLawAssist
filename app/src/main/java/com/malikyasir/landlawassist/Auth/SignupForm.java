package com.malikyasir.landlawassist.Auth;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.malikyasir.landlawassist.Home.MainActivity;
import com.malikyasir.landlawassist.Modelss.User;
import com.malikyasir.landlawassist.R;

public class SignupForm extends AppCompatActivity {

    private TextInputEditText fullNameInput, emailInput, passwordInput, phoneInput;
    private AutoCompleteTextView userTypeSpinner;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private VideoView videoBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_form);

        mAuth = FirebaseAuth.getInstance();

        // Set up video background
        setupVideoBackground();
        
        // Bind UI elements
        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        phoneInput = findViewById(R.id.phoneInput);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up login text click listener
        TextView loginPrompt = findViewById(R.id.loginPrompt);
        loginPrompt.setOnClickListener(v -> {
            // Navigate to login screen
            startActivity(new Intent(SignupForm.this, LoginForm.class));
            finish();
        });

        String[] userTypes = {"User", "Lawyer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, userTypes);
        userTypeSpinner.setAdapter(adapter);

        MaterialButton registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(v -> registerUser());
    }
    
    private void setupVideoBackground() {
        try {
            videoBackground = findViewById(R.id.videoBackground);
            
            // If we can't find the video view, it means we need to add it to the layout
            if (videoBackground == null) {
                // We'll add it programmatically
                View videoLayout = getLayoutInflater().inflate(R.layout.video_background, null);
                videoBackground = videoLayout.findViewById(R.id.videoBackground);
                
                // Add at the beginning of the root view to make it the background
                ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
                ViewGroup contentView = (ViewGroup) rootView.getChildAt(0);
                rootView.removeView(contentView);
                
                ViewGroup newRoot = (ViewGroup) videoLayout;
                newRoot.addView(contentView);
                setContentView(newRoot);
                
                // Find the video view again
                videoBackground = findViewById(R.id.videoBackground);
            }
            
            // Set the video path - using the new signup.mp4 video
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.signup);
            videoBackground.setVideoURI(videoUri);
            
            // Set looping and start the video
            videoBackground.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                
                // Set volume to 0 (mute)
                mp.setVolume(0f, 0f);
                
                // Set video scaling to fill the screen properly to avoid cutoffs
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                
                // Start the video
                if (!videoBackground.isPlaying()) {
                    videoBackground.start();
                }
            });
            
            videoBackground.setOnCompletionListener(mp -> {
                // Restart video when it completes (although looping should handle this)
                if (videoBackground != null) {
                    videoBackground.start();
                }
            });
            
            // Make sure we start the video
            videoBackground.start();
            
            // Ensure VideoView uses the full screen without getting cut off
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            videoBackground.setLayoutParams(layoutParams);
            
        } catch (Exception e) {
            // If anything fails, we can fallback to the static background
            e.printStackTrace();
            Toast.makeText(this, "Error setting up video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (videoBackground != null) {
            videoBackground.start();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (videoBackground != null) {
            videoBackground.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoBackground != null) {
            videoBackground.stopPlayback();
        }
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

