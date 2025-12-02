package com.example.overcooked.util;

import com.example.overcooked.data.model.CookedLevel;

/**
 * Data class containing all cooked meter information
 */
public class CookedMeterResult {
    private final int percentage;
    private final CookedLevel level;
    private final int totalTasks;
    private final int pendingTasks;
    private final int completedTasks;
    private final int overdueTasks;
    private final int urgentTasks;
    private final int todayTasks;
    private final String motivationalMessage;
    private final String statusText;

    public CookedMeterResult(int percentage, CookedLevel level, int totalTasks, int pendingTasks,
                             int completedTasks, int overdueTasks, int urgentTasks, int todayTasks,
                             String motivationalMessage, String statusText) {
        this.percentage = percentage;
        this.level = level;
        this.totalTasks = totalTasks;
        this.pendingTasks = pendingTasks;
        this.completedTasks = completedTasks;
        this.overdueTasks = overdueTasks;
        this.urgentTasks = urgentTasks;
        this.todayTasks = todayTasks;
        this.motivationalMessage = motivationalMessage;
        this.statusText = statusText;
    }

    public int getPercentage() { return percentage; }
    public CookedLevel getLevel() { return level; }
    public int getTotalTasks() { return totalTasks; }
    public int getPendingTasks() { return pendingTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public int getOverdueTasks() { return overdueTasks; }
    public int getUrgentTasks() { return urgentTasks; }
    public int getTodayTasks() { return todayTasks; }
    public String getMotivationalMessage() { return motivationalMessage; }
    public String getStatusText() { return statusText; }

    /**
     * Get the display text for the cooked level
     */
    public String getLevelDisplayText() {
        return "You're " + level.getDisplayName() + " " + level.getEmoji();
    }

    /**
     * Get contextual message based on upcoming tasks
     */
    public String getContextMessage() {
        if (overdueTasks > 0) {
            return overdueTasks + " overdue task" + (overdueTasks > 1 ? "s" : "") + "! Time to catch up.";
        } else if (urgentTasks > 0) {
            return urgentTasks + " task" + (urgentTasks > 1 ? "s" : "") + " due in the next 48 hours.";
        } else if (todayTasks > 0) {
            return todayTasks + " task" + (todayTasks > 1 ? "s" : "") + " due today.";
        } else if (pendingTasks > 0) {
            return pendingTasks + " task" + (pendingTasks > 1 ? "s" : "") + " to go. You got this!";
        } else {
            return "All caught up! Time to relax. 🎉";
        }
    }

    /**
     * Check if user is at critical stress level
     */
    public boolean isCritical() {
        return level == CookedLevel.OVERCOOKED;
    }

    /**
     * Check if user has overdue tasks
     */
    public boolean hasOverdueTasks() {
        return overdueTasks > 0;
    }
}
