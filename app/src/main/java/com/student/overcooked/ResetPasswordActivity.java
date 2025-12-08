package com.student.overcooked;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Allows users to request a password reset email and guides them to their inbox.
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private MaterialButton sendButton;
    private MaterialButton openEmailButton;
    private View confirmationContainer;
    private TextView confirmationMessage;
    private TextView backToLogin;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        auth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.resetEmailInput);
        sendButton = findViewById(R.id.sendResetButton);
        openEmailButton = findViewById(R.id.openEmailButton);
        confirmationContainer = findViewById(R.id.resetConfirmationContainer);
        confirmationMessage = findViewById(R.id.resetConfirmationMessage);
        backToLogin = findViewById(R.id.backToLogin);

        sendButton.setOnClickListener(v -> sendResetEmail());
        openEmailButton.setOnClickListener(v -> openEmailApp());
        backToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void sendResetEmail() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.email_required));
            return;
        }
        toggleSendingState(true);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    toggleSendingState(false);
                    if (task.isSuccessful()) {
                        confirmationContainer.setVisibility(View.VISIBLE);
                        confirmationMessage.setText(getString(R.string.reset_email_sent_message, email));
                        Toast.makeText(this, R.string.reset_email_sent_toast, Toast.LENGTH_SHORT).show();
                    } else {
                        String message = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_occurred);
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void toggleSendingState(boolean sending) {
        sendButton.setEnabled(!sending);
        sendButton.setText(sending ? R.string.sending_email : R.string.send_reset_link);
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
