package com.student.overcooked.ui.focus;

import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

final class FocusTimerController {

    private static final long DEFAULT_POMODORO_FOCUS_MS = 25L * 60L * 1000L;
    private static final long DEFAULT_BREAK_MS = 5L * 60L * 1000L;

    enum TimerPhase { FOCUS, BREAK }

    private final CircularProgressIndicator ring;
    private final TextView timerText;
    private final TextView modeText;
    private final MaterialButton btnStartPause;
    private final MaterialButton btnReset;
    private final MaterialButton btnSkipBreak;

    private CountDownTimer timer;
    private boolean running = false;
    private TimerPhase phase = TimerPhase.FOCUS;

    private long focusDurationMs = DEFAULT_POMODORO_FOCUS_MS;
    private long breakDurationMs = DEFAULT_BREAK_MS;
    private long remainingMs = DEFAULT_POMODORO_FOCUS_MS;

    FocusTimerController(@NonNull CircularProgressIndicator ring,
                         @NonNull TextView timerText,
                         @NonNull TextView modeText,
                         @NonNull MaterialButton btnStartPause,
                         @NonNull MaterialButton btnReset,
                         @NonNull MaterialButton btnSkipBreak) {
        this.ring = ring;
        this.timerText = timerText;
        this.modeText = modeText;
        this.btnStartPause = btnStartPause;
        this.btnReset = btnReset;
        this.btnSkipBreak = btnSkipBreak;

        wireButtons();
        setPhase(TimerPhase.FOCUS);
        setDurations(DEFAULT_POMODORO_FOCUS_MS, DEFAULT_BREAK_MS);
        resetTimer();
    }

    void setPomodoroDefaults() {
        setDurations(DEFAULT_POMODORO_FOCUS_MS, DEFAULT_BREAK_MS);
        resetTimer();
    }

    void setCustomFocusMinutes(long minutes) {
        setDurations(minutes * 60L * 1000L, DEFAULT_BREAK_MS);
        resetTimer();
    }

    void destroy() {
        stopTimer();
    }

    private void wireButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (running) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());

        btnSkipBreak.setOnClickListener(v -> {
            if (phase == TimerPhase.BREAK) {
                setPhase(TimerPhase.FOCUS);
                resetTimer();
            }
        });
    }

    private void setDurations(long focusMs, long breakMs) {
        focusDurationMs = Math.max(60_000L, focusMs);
        breakDurationMs = Math.max(60_000L, breakMs);
    }

    private void setPhase(@NonNull TimerPhase phase) {
        this.phase = phase;
        modeText.setText(phase == TimerPhase.FOCUS ? "Focus" : "Break");
        btnSkipBreak.setVisibility(phase == TimerPhase.BREAK ? View.VISIBLE : View.GONE);
    }

    private long currentPhaseTotalMs() {
        return phase == TimerPhase.FOCUS ? focusDurationMs : breakDurationMs;
    }

    private void resetTimer() {
        pauseTimer();
        remainingMs = currentPhaseTotalMs();
        renderTimerUi();
    }

    private void startTimer() {
        if (running) return;
        running = true;
        btnStartPause.setText("Pause");
        startCountDown(remainingMs);
    }

    private void pauseTimer() {
        running = false;
        btnStartPause.setText("Start");
        stopTimer();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startCountDown(long fromMs) {
        stopTimer();
        timer = new CountDownTimer(fromMs, 250) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                renderTimerUi();
            }

            @Override
            public void onFinish() {
                remainingMs = 0;
                renderTimerUi();

                if (phase == TimerPhase.FOCUS) {
                    setPhase(TimerPhase.BREAK);
                } else {
                    setPhase(TimerPhase.FOCUS);
                }
                resetTimer();
            }
        };
        timer.start();
    }

    private void renderTimerUi() {
        timerText.setText(formatTime(remainingMs));

        long total = currentPhaseTotalMs();
        int max = (int) Math.max(1, total / 1000L);
        int progress = (int) Math.max(0, Math.min(max, (remainingMs / 1000L)));

        ring.setMax(max);
        ring.setProgress(progress);
    }

    private static String formatTime(long ms) {
        long totalSeconds = Math.max(0, ms / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
