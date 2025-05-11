package com.malikyasir.landlawassist.Home;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.VideoView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.malikyasir.landlawassist.Auth.LoginForm;
import com.malikyasir.landlawassist.R;

public class splash extends AppCompatActivity {
    
    private VideoView splashVideo;
    
    // Longer delay for the splash screen to show the video
    private static final int SPLASH_DURATION = 4000; // 4 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Initialize views
        splashVideo = findViewById(R.id.splashVideo);
        
        // Setup and play the video
        setupVideo();
        
        // Navigate after the splash duration
        navigateAfterSplash();
    }
    
    private void setupVideo() {
        try {
            // Set the video path - using the same space background video
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bg_space);
            splashVideo.setVideoURI(videoUri);
            
            // Set up video properties
            splashVideo.setOnPreparedListener(mp -> {
                // Remove video sound
                mp.setVolume(0f, 0f);
                
                // Scale video to fill the screen properly
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                
                // Start playing
                splashVideo.start();
            });
            
            // Loop video if needed (for longer splash)
            splashVideo.setOnCompletionListener(mp -> splashVideo.start());
            
            // Start the video
            splashVideo.start();
        } catch (Exception e) {
            e.printStackTrace();
            // If video fails, splash screen will still work with the static background
        }
    }
    
    private void navigateAfterSplash() {
        new Handler().postDelayed(() -> {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            Intent intent;
            
            // Clear any existing auth state if needed
            if (currentUser != null) {
                auth.signOut();
            }
            
            // Navigate to login screen
            intent = new Intent(splash.this, LoginForm.class);
            startActivity(intent);
            
            // Apply a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Close the splash activity
            finish();
        }, SPLASH_DURATION);
    }
    
    @Override
    protected void onPause() {
        // Pause video if activity pauses
        if (splashVideo != null && splashVideo.isPlaying()) {
            splashVideo.pause();
        }
        super.onPause();
    }
    
    @Override
    protected void onDestroy() {
        // Clean up resources
        if (splashVideo != null) {
            splashVideo.stopPlayback();
        }
        super.onDestroy();
    }
}



