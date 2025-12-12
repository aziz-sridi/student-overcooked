package com.student.overcooked.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.student.overcooked.MainActivity;
import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.R;
import com.student.overcooked.data.repository.UserRepository;
import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.data.repository.TaskRepository;
import com.student.overcooked.notify.DeadlineNotificationWorker;
import com.student.overcooked.ui.common.CoinTopBarController;
import com.student.overcooked.util.NotificationSettings;
import com.student.overcooked.util.UiModeSettings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Profile Fragment - User Profile & Settings
 */
public class ProfileFragment extends Fragment {

    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private TextView profileInitials;
    private TextView userName;
    private TextView userEmail;

    private MaterialButton btnEditName;

    private Switch darkModeSwitch;
    private Switch notificationsSwitch;
    private View logoutSection;

    private FirebaseAuth auth;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private CoinTopBarController coinTopBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        auth = FirebaseAuth.getInstance();
        userRepository = ((OvercookedApplication) requireActivity().getApplication()).getUserRepository();
        taskRepository = ((OvercookedApplication) requireActivity().getApplication()).getTaskRepository();
        coinTopBar = new CoinTopBarController(this, new LocalCoinStore(requireContext()), userRepository);
        
        initializeViews(view);
        coinTopBar.bind(view);
        setupNotificationPermissionLauncher();
        setupUserInfo();
        setupClickListeners();
        observeData();
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (notificationsSwitch == null) return;
                    if (isGranted != null && isGranted) {
                        NotificationSettings.setNotificationsEnabled(requireContext(), true);
                        notificationsSwitch.setChecked(true);
                        DeadlineNotificationWorker.schedule(requireContext().getApplicationContext());
                        Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
                    } else {
                        NotificationSettings.setNotificationsEnabled(requireContext(), false);
                        notificationsSwitch.setChecked(false);
                        Toast.makeText(requireContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initializeViews(View view) {
        profileInitials = view.findViewById(R.id.profileInitials);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);

        btnEditName = view.findViewById(R.id.btnEditName);

        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        logoutSection = view.findViewById(R.id.logoutSection);
    }

    private void setupUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Student";
            userName.setText(displayName);
            userEmail.setText(user.getEmail() != null ? user.getEmail() : "No email");
            
            // Set initials
            String[] nameParts = displayName.split(" ");
            StringBuilder initials = new StringBuilder();
            for (int i = 0; i < Math.min(2, nameParts.length); i++) {
                if (!nameParts[i].isEmpty()) {
                    initials.append(Character.toUpperCase(nameParts[i].charAt(0)));
                }
            }
            profileInitials.setText(initials.length() > 0 ? initials.toString() : "S");
        } else {
            userName.setText("Guest User");
            userEmail.setText("Not signed in");
            profileInitials.setText("G");
        }
    }

    private void setupClickListeners() {
        if (btnEditName != null) {
            btnEditName.setOnClickListener(v -> showEditNameDialog());
        }

        if (darkModeSwitch != null) {
            darkModeSwitch.setChecked(UiModeSettings.isDarkModeEnabled(requireContext()));
            darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                UiModeSettings.setDarkModeEnabled(requireContext(), isChecked);
                AppCompatDelegate.setDefaultNightMode(isChecked
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        if (notificationsSwitch != null) {
            notificationsSwitch.setChecked(NotificationSettings.areNotificationsEnabled(requireContext()));
            notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isChecked) {
                    NotificationSettings.setNotificationsEnabled(requireContext(), false);
                    DeadlineNotificationWorker.cancel(requireContext().getApplicationContext());
                    Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Turning on notifications.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    int state = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS);
                    if (state != PackageManager.PERMISSION_GRANTED) {
                        // Request permission and only enable if granted.
                        NotificationSettings.setNotificationsEnabled(requireContext(), false);
                        notificationsSwitch.setChecked(false);
                        if (notificationPermissionLauncher != null) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                        }
                        return;
                    }
                }

                NotificationSettings.setNotificationsEnabled(requireContext(), true);
                DeadlineNotificationWorker.schedule(requireContext().getApplicationContext());
                Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
            });
        }

        if (logoutSection != null) {
            logoutSection.setOnClickListener(v -> showLogoutConfirmation());
        }

        // Working list controller
        // (Upcoming deadlines removed from Profile)
    }

    private void observeData() {
        // (Upcoming deadlines removed from Profile)
    }

    private void showEditNameDialog() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Your name");
        input.setText(userName != null ? userName.getText() : "");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText() != null ? input.getText().toString().trim() : "";
                    if (newName.isEmpty()) return;

                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(newName)
                            .build();
                    user.updateProfile(req)
                            .addOnSuccessListener(unused -> {
                                if (userName != null) userName.setText(newName);
                                if (profileInitials != null) profileInitials.setText(computeInitials(newName));
                                userRepository.updateUserDisplayName(newName, v -> {}, e -> {});
                                Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String computeInitials(@NonNull String displayName) {
        String[] parts = displayName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) {
                initials.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return initials.length() > 0 ? initials.toString() : "S";
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    OvercookedApplication app = (OvercookedApplication) requireActivity().getApplication();
                    app.resetLocalCache();
                    app.getSessionManager().clear();
                    auth.signOut();
                    navigateToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

}
