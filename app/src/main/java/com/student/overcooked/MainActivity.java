package com.student.overcooked;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.student.overcooked.ui.MainNavActivity;

/**
 * Main Activity - Splash screen and entry point
 * Redirects to MainNavActivity after a short delay
 * In a complete app, this would check auth state and route accordingly
 */
public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1500L; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Navigate to MainNavActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, SPLASH_DELAY);
    }

    private void checkAuthAndNavigate() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainNavActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
