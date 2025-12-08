package com.student.overcooked.ui.home;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.student.overcooked.R;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.util.CookedMeterCalculator;
import com.student.overcooked.util.CookedMeterResult;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Collections;
import java.util.List;

/**
 * Handles cooked meter UI updates for the Home screen.
 */
public class CookedMeterController {

    private final Fragment fragment;
    private final TextView cookedLevelText;
    private final TextView cookedPercentageText;
    private final TextView cookedStatusText;
    private final TextView cookedContextText;
    private final LinearProgressIndicator cookedProgressBar;
    private final ImageView cookedIcon;

    public CookedMeterController(@NonNull Fragment fragment,
                                 @Nullable TextView cookedLevelText,
                                 @Nullable TextView cookedPercentageText,
                                 @Nullable TextView cookedStatusText,
                                 @Nullable TextView cookedContextText,
                                 @Nullable LinearProgressIndicator cookedProgressBar,
                                 @Nullable ImageView cookedIcon) {
        this.fragment = fragment;
        this.cookedLevelText = cookedLevelText;
        this.cookedPercentageText = cookedPercentageText;
        this.cookedStatusText = cookedStatusText;
        this.cookedContextText = cookedContextText;
        this.cookedProgressBar = cookedProgressBar;
        this.cookedIcon = cookedIcon;
    }

    public void render(@Nullable List<Task> tasks) {
        if (!fragment.isAdded()) {
            return;
        }
        List<Task> safeTasks = tasks != null ? tasks : Collections.emptyList();
        CookedMeterResult result = CookedMeterCalculator.getCookedMeterResult(safeTasks);

        if (cookedLevelText != null) {
            cookedLevelText.setText(result.getLevelDisplayText());
            cookedLevelText.setTextColor(ContextCompat.getColor(fragment.requireContext(), getLevelColor(result)));
        }
        if (cookedPercentageText != null) {
            cookedPercentageText.setText(result.getPercentage() + "%");
        }
        if (cookedStatusText != null) {
            cookedStatusText.setText(result.getStatusText());
        }
        if (cookedContextText != null) {
            cookedContextText.setText(result.getContextMessage());
        }
        if (cookedProgressBar != null) {
            cookedProgressBar.setProgressCompat(result.getPercentage(), true);
            cookedProgressBar.setIndicatorColor(ContextCompat.getColor(fragment.requireContext(), getLevelColor(result)));
        }
        if (cookedIcon != null) {
            cookedIcon.setImageResource(getLevelIcon(result));
        }
    }

    private int getLevelColor(@NonNull CookedMeterResult result) {
        int colorRes;
        switch (result.getLevel()) {
            case COZY:
                colorRes = R.color.successGreen;
                break;
            case CRISPY:
                colorRes = R.color.mustardYellow;
                break;
            case COOKED:
                colorRes = R.color.burntOrange;
                break;
            case OVERCOOKED:
            default:
                colorRes = R.color.tomatoRed;
                break;
        }
        return colorRes;
    }

    private int getLevelIcon(@NonNull CookedMeterResult result) {
        switch (result.getLevel()) {
            case COZY:
                return R.drawable.mascot_cozy;
            case CRISPY:
                return R.drawable.mascot_crispy;
            case COOKED:
                return R.drawable.mascot_cooked;
            case OVERCOOKED:
            default:
                return R.drawable.mascot_overcooked;
        }
    }
}
