package com.example.overcooked.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.overcooked.MainActivity;
import com.example.overcooked.OvercookedApplication;
import com.example.overcooked.R;
import com.example.overcooked.data.repository.UserRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Profile Fragment - User Profile & Settings
 */
public class ProfileFragment extends Fragment {

    private TextView profileInitials;
    private TextView userName;
    private TextView userEmail;
    
    // Settings sections
    private LinearLayout cookedMeterSection;
    private LinearLayout notificationsSection;
    private LinearLayout googleClassroomSection;
    private Switch darkModeSwitch;
    private LinearLayout logoutSection;
    private LinearLayout aboutSection;
    private TextView coinScoreText;

    private FirebaseAuth auth;
    private UserRepository userRepository;

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
        
        initializeViews(view);
        setupUserInfo();
        setupClickListeners();
        observeData();
    }

    private void initializeViews(View view) {
        profileInitials = view.findViewById(R.id.profileInitials);
        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        
        cookedMeterSection = view.findViewById(R.id.cookedMeterSection);
        notificationsSection = view.findViewById(R.id.notificationsSection);
        googleClassroomSection = view.findViewById(R.id.googleClassroomSection);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        logoutSection = view.findViewById(R.id.logoutSection);
        aboutSection = view.findViewById(R.id.aboutSection);
        coinScoreText = view.findViewById(R.id.coinScoreText);
        
        // Setup coin score card click listener for shop
        View coinScoreCard = view.findViewById(R.id.coinScoreCard);
        if (coinScoreCard != null) {
            coinScoreCard.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.example.overcooked.ui.ShopActivity.class));
            });
        }
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
        cookedMeterSection.setOnClickListener(v -> 
                Toast.makeText(requireContext(), "Cooked Meter details coming soon!", Toast.LENGTH_SHORT).show());

        notificationsSection.setOnClickListener(v -> 
                Toast.makeText(requireContext(), "Notification settings coming soon!", Toast.LENGTH_SHORT).show());

        googleClassroomSection.setOnClickListener(v -> 
                Toast.makeText(requireContext(), "Google Classroom integration coming soon!", Toast.LENGTH_SHORT).show());

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        logoutSection.setOnClickListener(v -> showLogoutConfirmation());

        aboutSection.setOnClickListener(v -> showAboutDialog());
    }

    private void observeData() {
        // Observe user data for coins
        userRepository.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null && coinScoreText != null) {
                coinScoreText.setText(String.valueOf(user.getCoins()));
            }
        });
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
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

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("About Student Overcooked")
                .setMessage("Version 1.0.0\n\nA task management app designed to help students track their workload and avoid getting 'overcooked'! 🔥\n\n© 2024")
                .setPositiveButton("OK", null)
                .show();
    }
}
