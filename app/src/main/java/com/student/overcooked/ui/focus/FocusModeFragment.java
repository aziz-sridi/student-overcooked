package com.student.overcooked.ui.focus;

import android.app.AlertDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.student.overcooked.R;
import com.student.overcooked.data.LocalCoinStore;
import com.student.overcooked.ui.common.CoinTopBarController;

/**
 * Focus Mode – offline-only.
 * Timer: Pomodoro or Custom duration.
 * Ambient sounds: presets + import from phone (no database sync).
 */
public class FocusModeFragment extends Fragment {

    private @Nullable FocusTimerController timerController;
    private @Nullable FocusSoundController soundController;

    private CircularProgressIndicator ring;
    private TextView timerText;
    private TextView modeText;
    private MaterialButton btnStartPause;
    private MaterialButton btnReset;
    private MaterialButton btnSkipBreak;
    private MaterialButtonToggleGroup modeToggle;
    private MaterialButton btnPomodoro;
    private MaterialButton btnCustom;
    private TextView customHintText;

    private View presetsContainer;
    private TextView selectedSoundText;
    private SeekBar volumeSeek;
    private MaterialButton btnSoundPlayPause;

    private ActivityResultLauncher<String[]> importSoundLauncher;

    // Coin top bar
    private @Nullable CoinTopBarController coinTopBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_focus_mode, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LocalCoinStore localCoinStore = new LocalCoinStore(requireContext());

        ring = view.findViewById(R.id.focusTimerRing);
        timerText = view.findViewById(R.id.focusTimerText);
        modeText = view.findViewById(R.id.focusModeText);
        btnStartPause = view.findViewById(R.id.btnStartPause);
        btnReset = view.findViewById(R.id.btnReset);
        btnSkipBreak = view.findViewById(R.id.btnSkipBreak);
        modeToggle = view.findViewById(R.id.modeToggleGroup);
        btnPomodoro = view.findViewById(R.id.btnPomodoro);
        btnCustom = view.findViewById(R.id.btnCustom);
        customHintText = view.findViewById(R.id.customHintText);

        presetsContainer = view.findViewById(R.id.soundPresetsContainer);
        selectedSoundText = view.findViewById(R.id.selectedSoundText);
        volumeSeek = view.findViewById(R.id.volumeSeekBar);
        btnSoundPlayPause = view.findViewById(R.id.btnSoundPlayPause);

        coinTopBar = new CoinTopBarController(this, localCoinStore, null);
        coinTopBar.bind(view);

        importSoundLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            if (soundController != null) {
                soundController.onImportedUri(uri);
            }
        });

        timerController = new FocusTimerController(
                ring,
                timerText,
                modeText,
                btnStartPause,
                btnReset,
                btnSkipBreak
        );

        soundController = new FocusSoundController(
                view.getContext(),
                importSoundLauncher,
                presetsContainer,
                selectedSoundText,
                volumeSeek,
                btnSoundPlayPause
        );

        setupModeToggle();
    }

    @Override
    public void onDestroyView() {
        if (timerController != null) {
            timerController.destroy();
            timerController = null;
        }
        if (soundController != null) {
            soundController.destroy();
            soundController = null;
        }
        super.onDestroyView();
    }

    private void setupModeToggle() {
        if (modeToggle == null) return;

        modeToggle.check(R.id.btnPomodoro);
        customHintText.setVisibility(View.GONE);

        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnPomodoro) {
                customHintText.setVisibility(View.GONE);
                if (timerController != null) {
                    timerController.setPomodoroDefaults();
                }
            } else if (checkedId == R.id.btnCustom) {
                customHintText.setVisibility(View.VISIBLE);
                showCustomDurationDialog();
            }
        });

        // Allow tapping the hint to re-open the picker.
        customHintText.setOnClickListener(v -> showCustomDurationDialog());
    }

    private void showCustomDurationDialog() {
        // Simplest implementation: choose minutes from a small list.
        String[] options = new String[]{"10 min", "15 min", "20 min", "25 min", "30 min", "45 min", "60 min"};
        long[] values = new long[]{10, 15, 20, 25, 30, 45, 60};

        new AlertDialog.Builder(requireContext())
                .setTitle("Custom focus duration")
                .setItems(options, (dialog, which) -> {
                    long mins = values[Math.max(0, Math.min(which, values.length - 1))];
                    if (timerController != null) {
                        timerController.setCustomFocusMinutes(mins);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
