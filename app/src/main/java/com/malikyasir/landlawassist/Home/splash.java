package com.malikyasir.landlawassist.Home;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.malikyasir.landlawassist.Auth.LoginForm;
import com.malikyasir.landlawassist.R;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Thread thread = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    FirebaseUser currentUser = auth.getCurrentUser();
                    Intent intent;
                    
                    // Clear any existing auth state
                    if (currentUser != null) {
                        auth.signOut();
                    }
                    
                    // Always direct to login first
                    intent = new Intent(splash.this, LoginForm.class);
                    startActivity(intent);
                    finish();
                }
            }
        };
        thread.start();
    }
}



