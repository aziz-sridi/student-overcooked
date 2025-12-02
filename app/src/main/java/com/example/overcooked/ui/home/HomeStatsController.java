package com.example.overcooked.ui.home;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.overcooked.R;
import com.example.overcooked.ui.adapter.QuickStatItem;
import com.example.overcooked.ui.adapter.QuickStatsAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the Quick Stats carousel and coin score in sync with current counts.
 */
public class HomeStatsController {

    private final Fragment fragment;
    private final QuickStatsAdapter quickStatsAdapter = new QuickStatsAdapter();
    private final TextView coinScoreText;

    private int pendingCount;
    private int completedCount;
    private int overdueCount;
    private int projectCount;
    private int totalProjectCount;

    public HomeStatsController(@NonNull Fragment fragment,
                               @Nullable RecyclerView quickStatsRecycler,
                               @Nullable TextView coinScoreText) {
        this.fragment = fragment;
        this.coinScoreText = coinScoreText;
        if (quickStatsRecycler != null) {
            quickStatsRecycler.setLayoutManager(new LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.HORIZONTAL, false));
            quickStatsRecycler.setAdapter(quickStatsAdapter);
        }
    }

    public void setPendingCount(int pendingCount) {
        this.pendingCount = pendingCount;
        renderStats();
    }

    public void setCompletedCount(int completedCount) {
        this.completedCount = completedCount;
        renderStats();
        updateCoinScore();
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
        renderStats();
    }

    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
        renderStats();
    }

    public void setTotalProjectCount(int totalProjectCount) {
        this.totalProjectCount = totalProjectCount;
        renderStats();
    }

    private void renderStats() {
        if (!fragment.isAdded()) {
            return;
        }
        List<QuickStatItem> statsList = new ArrayList<>();
        int totalTasks = completedCount + pendingCount;
        statsList.add(new QuickStatItem(
                "Tasks",
                (completedCount) + "/" + totalTasks,
                R.drawable.ic_task_stat,
                R.color.burntOrange
        ));
        statsList.add(new QuickStatItem(
                "Projects",
                projectCount + "/" + totalProjectCount,
                R.drawable.ic_add_project,
                R.color.mustardYellow
        ));
        statsList.add(new QuickStatItem(
                "Overdue",
                String.valueOf(overdueCount),
                R.drawable.ic_time,
                overdueCount > 0 ? R.color.tomatoRed : R.color.successGreen
        ));
        quickStatsAdapter.updateStats(statsList);
    }

    private void updateCoinScore() {
        if (coinScoreText == null || !fragment.isAdded()) {
            return;
        }
        int coinScore = completedCount * 10;
        coinScoreText.setText(String.valueOf(coinScore));
    }
}
