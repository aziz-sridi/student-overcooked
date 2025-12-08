package com.student.overcooked;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.student.overcooked.ui.MainNavActivity;
import com.student.overcooked.util.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private SignInButton googleLoginButton;
    private TextView signupLink;
    private TextView forgotPasswordLink;

    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;

    private void handlePostLoginSuccess() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            OvercookedApplication app = (OvercookedApplication) getApplication();
            com.student.overcooked.util.SessionManager session = app.getSessionManager();
            String lastUserId = session.getLastUserId();
            if (lastUserId == null || !lastUserId.equals(user.getUid())) {
                app.resetLocalCache();
            }
            session.setLastUserId(user.getUid());
        }
        startActivity(new Intent(this, MainNavActivity.class));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        signupLink = findViewById(R.id.signupLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

        loginButton.setOnClickListener(v -> login());
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
        signupLink.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
        forgotPasswordLink.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google sign in failed: Missing token", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        handlePostLoginSuccess();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText(R.string.logging_in);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user != null && user.isEmailVerified()) {
                        resetLoginButton();
                        handlePostLoginSuccess();
                    } else if (user != null) {
                        launchVerificationScreen(user);
                    } else {
                        Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        resetLoginButton();
                    }
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void launchVerificationScreen(@NonNull FirebaseUser user) {
        user.sendEmailVerification();
        Intent intent = new Intent(this, EmailVerificationActivity.class);
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, user.getEmail());
        startActivity(intent);
        resetLoginButton();
    }

    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText(R.string.login_button);
    }
}
