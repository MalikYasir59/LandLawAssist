package com.malikyasir.landlawassist.Home;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.VideoView;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.malikyasir.landlawassist.Auth.LoginForm;
import com.malikyasir.landlawassist.R;

public class splash extends AppCompatActivity {
    
    private VideoView splashVideo;
    private static final String TAG = "SplashScreen";
    // Fixed duration for the splash screen - reduced to 2 seconds
    private static final int SPLASH_DURATION = 2000; // 2 seconds

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
            // Set the video path
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash);
            splashVideo.setVideoURI(videoUri);
            
            // Set up video properties
            splashVideo.setOnPreparedListener(mp -> {
                // Remove video sound
                mp.setVolume(0f, 0f);
                
                // Set playback speed to 1.5x for faster playback
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.media.PlaybackParams params = new android.media.PlaybackParams();
                    params.setSpeed(1.5f);
                    try {
                        mp.setPlaybackParams(params);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting playback speed: " + e.getMessage());
                    }
                }
                
                // Set a timer to stop video after 2 seconds
                new Handler().postDelayed(() -> {
                    if (splashVideo != null && splashVideo.isPlaying()) {
                        splashVideo.pause();
                    }
                }, 2000);
                
                try {
                    // Get display metrics
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int screenWidth = metrics.widthPixels;
                    int screenHeight = metrics.heightPixels;
                    
                    // Get video dimensions
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    
                    Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
                    Log.d(TAG, "Video dimensions: " + videoWidth + "x" + videoHeight);
                    
                    if (videoWidth > 0 && videoHeight > 0) {
                        // Calculate dimensions to fill most of the screen while maintaining aspect ratio
                        float videoRatio = (float) videoWidth / videoHeight;
                        float screenRatio = (float) screenWidth / screenHeight;
                        
                        int newWidth, newHeight;
                        
                        // Scale to 90% of full size to avoid cutoff
                        float scaleFactor = 0.9f;
                        
                        // Make sure video fills most of the screen without cutoff
                        if (videoRatio > screenRatio) {
                            // Video is wider than screen ratio - match height and calculate width
                            newHeight = (int)(screenHeight * scaleFactor);
                            newWidth = (int)(newHeight * videoRatio);
                        } else {
                            // Video is taller than screen ratio - match width and calculate height
                            newWidth = (int)(screenWidth * scaleFactor);
                            newHeight = (int)(newWidth / videoRatio);
                        }
                        
                        // Set the new dimensions to the VideoView
                        ViewGroup.LayoutParams layoutParams = splashVideo.getLayoutParams();
                        layoutParams.width = newWidth;
                        layoutParams.height = newHeight;
                        splashVideo.setLayoutParams(layoutParams);
                        
                        Log.d(TAG, "Setting video size to: " + newWidth + "x" + newHeight);
                    } else {
                        Log.w(TAG, "Invalid video dimensions detected, using fallback scaling");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating video dimensions: " + e.getMessage());
                }
                
                // Use SCALE_TO_FIT to ensure video fits without cropping
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                
                // Start playing
                splashVideo.start();
                
                // Log completion
                Log.d(TAG, "Video prepared and started");
            });
            
            // Handle completion
            splashVideo.setOnCompletionListener(mp -> {
                Log.d(TAG, "Video playback completed");
            });
            
            // Handle errors
            splashVideo.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "VideoView error: what=" + what + ", extra=" + extra);
                // Continue with splash screen even if video fails
                return true;
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up video: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void navigateAfterSplash() {
        new Handler().postDelayed(() -> {
            // Always navigate to login screen
            Intent intent = new Intent(splash.this, LoginForm.class);
            startActivity(intent);
            
            // Apply a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            
            // Close the splash activity
            finish();
        }, SPLASH_DURATION); // Use the same constant for consistent timing
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



