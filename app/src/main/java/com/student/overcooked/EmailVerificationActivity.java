package com.student.overcooked;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Guides users through verifying their email after signup or login.
 */
public class EmailVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private TextView emailLabel;
    private MaterialButton resendButton;
    private MaterialButton openInboxButton;
    private TextView continueToLogin;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        auth = FirebaseAuth.getInstance();
        emailLabel = findViewById(R.id.verificationEmail);
        resendButton = findViewById(R.id.resendVerificationButton);
        openInboxButton = findViewById(R.id.openInboxButton);
        continueToLogin = findViewById(R.id.continueToLogin);

        String email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (TextUtils.isEmpty(email) && auth.getCurrentUser() != null) {
            email = auth.getCurrentUser().getEmail();
        }
        if (!TextUtils.isEmpty(email)) {
            emailLabel.setText(email);
        }

        resendButton.setOnClickListener(v -> resendVerificationEmail());
        openInboxButton.setOnClickListener(v -> openEmailApp());
        continueToLogin.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void resendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
            return;
        }
        resendButton.setEnabled(false);
        resendButton.setText(R.string.sending_email);
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    resendButton.setEnabled(true);
                    resendButton.setText(R.string.resend_verification_email);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.verification_email_sent, Toast.LENGTH_SHORT).show();
                    } else {
                        String message = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_occurred);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"));
            startActivity(browserIntent);
        }
    }
}
