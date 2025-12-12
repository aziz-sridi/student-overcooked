package com.student.overcooked.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.student.overcooked.R;
import com.student.overcooked.ui.adapter.QuickStatItem;
import com.student.overcooked.ui.adapter.QuickStatsAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the Quick Stats carousel and coin score in sync with current counts.
 */
public class HomeStatsController {

    private final Fragment fragment;
    private final QuickStatsAdapter quickStatsAdapter = new QuickStatsAdapter();

    private int pendingCount;
    private int completedCount;
    private int overdueCount;
    private int projectCount;
    private int totalProjectCount;
    private int groupCount;
    private int completedGroupTasks;
    private int totalGroupTasks;

    public HomeStatsController(@NonNull Fragment fragment,
                               @Nullable RecyclerView quickStatsRecycler) {
        this.fragment = fragment;
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

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
        renderStats();
    }

    public void setGroupTaskStats(int completedGroupTasks, int totalGroupTasks) {
        this.completedGroupTasks = completedGroupTasks;
        this.totalGroupTasks = totalGroupTasks;
        renderStats();
    }

    private void renderStats() {
        if (!fragment.isAdded()) {
            return;
        }
        List<QuickStatItem> statsList = new ArrayList<>();
        // Include both individual and group tasks
        int totalTasks = completedCount + pendingCount + totalGroupTasks;
        int allCompletedTasks = completedCount + completedGroupTasks;
        statsList.add(new QuickStatItem(
                "Tasks",
                allCompletedTasks + "/" + totalTasks,
                R.drawable.ic_task_stat,
                R.color.burntOrange
        ));
        // Include both individual projects and groups
        int allProjects = projectCount + groupCount;
        int totalAllProjects = totalProjectCount + groupCount;
        statsList.add(new QuickStatItem(
                "Projects",
                allProjects + "/" + totalAllProjects,
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

}
