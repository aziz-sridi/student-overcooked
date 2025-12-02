package com.example.overcooked.util;

import com.example.overcooked.data.model.CookedLevel;
import com.example.overcooked.data.model.Priority;
import com.example.overcooked.data.model.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Calculator for the "Cooked Meter" - a stress/workload indicator for students
 * 
 * The meter is expressed as a percentage (0-100%) based on:
 * - Number of pending tasks
 * - Deadline proximity (tasks due sooner = more stress)
 * - Task priority/weight
 * 
 * Meter States:
 * - Cozy (0-30%): Low workload
 * - Crispy (31-60%): Moderate workload  
 * - Cooked (61-85%): High workload
 * - Overcooked (86-100%): Critical workload
 */
public class CookedMeterCalculator {

    // Configuration constants
    private static final int MAX_TASKS_BASELINE = 15; // Beyond this, you're at risk
    private static final int DAYS_URGENT_THRESHOLD = 2; // Tasks due within 2 days are urgent
    private static final int DAYS_SOON_THRESHOLD = 7; // Tasks due within a week add moderate stress

    // Weight multipliers
    private static final double OVERDUE_MULTIPLIER = 3.0;
    private static final double URGENT_MULTIPLIER = 2.5;
    private static final double SOON_MULTIPLIER = 1.5;
    private static final double NORMAL_MULTIPLIER = 1.0;

    private CookedMeterCalculator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Calculate the cooked percentage based on pending tasks
     * 
     * @param tasks List of all tasks (will filter to pending only)
     * @return Percentage from 0 to 100
     */
    public static int calculateCookedPercentage(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        
        List<Task> pendingTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                pendingTasks.add(task);
            }
        }
        
        if (pendingTasks.isEmpty()) {
            return 0;
        }

        double totalStressScore = 0.0;
        Date now = new Date();

        for (Task task : pendingTasks) {
            double baseScore = getBaseScore(task.getPriority());
            double timeMultiplier = getTimeMultiplier(task.getDeadline(), now);
            totalStressScore += baseScore * timeMultiplier;
        }

        // Normalize to percentage
        // Max score per task = HIGH priority (3) * OVERDUE multiplier (3.0) = 9
        // With MAX_TASKS_BASELINE tasks at maximum stress = 9 * 15 = 135
        double maxPossibleScore = 9.0 * MAX_TASKS_BASELINE;
        double percentage = (totalStressScore / maxPossibleScore) * 100;

        return Math.min(100, (int) percentage);
    }

    /**
     * Get the cooked level enum based on tasks
     */
    public static CookedLevel getCookedLevel(List<Task> tasks) {
        int percentage = calculateCookedPercentage(tasks);
        return CookedLevel.fromPercentage(percentage);
    }

    /**
     * Get detailed cooked meter result with all information
     */
    public static CookedMeterResult getCookedMeterResult(List<Task> tasks) {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        
        List<Task> pendingTasks = new ArrayList<>();
        List<Task> completedTasks = new ArrayList<>();
        List<Task> overdueTasks = new ArrayList<>();
        List<Task> urgentTasks = new ArrayList<>();
        List<Task> todayTasks = new ArrayList<>();

        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks.add(task);
            } else {
                pendingTasks.add(task);
                if (task.isOverdue()) {
                    overdueTasks.add(task);
                } else if (task.daysUntilDeadline() <= DAYS_URGENT_THRESHOLD) {
                    urgentTasks.add(task);
                }
                if (task.isDueToday()) {
                    todayTasks.add(task);
                }
            }
        }

        int percentage = calculateCookedPercentage(tasks);
        CookedLevel level = CookedLevel.fromPercentage(percentage);

        return new CookedMeterResult(
                percentage,
                level,
                tasks.size(),
                pendingTasks.size(),
                completedTasks.size(),
                overdueTasks.size(),
                urgentTasks.size(),
                todayTasks.size(),
                level.getMotivationalMessage(),
                level.getStatusText()
        );
    }

    /**
     * Get base stress score based on priority
     */
    private static double getBaseScore(Priority priority) {
        return priority.getWeight();
    }

    /**
     * Get time-based multiplier based on deadline proximity
     */
    private static double getTimeMultiplier(Date deadline, Date now) {
        if (deadline == null) {
            return NORMAL_MULTIPLIER; // No deadline = treat as normal
        }
        int daysUntilDeadline = (int) ((deadline.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

        if (daysUntilDeadline < 0) {
            return OVERDUE_MULTIPLIER; // Overdue
        } else if (daysUntilDeadline <= DAYS_URGENT_THRESHOLD) {
            return URGENT_MULTIPLIER; // Due very soon
        } else if (daysUntilDeadline <= DAYS_SOON_THRESHOLD) {
            return SOON_MULTIPLIER; // Due within a week
        } else {
            return NORMAL_MULTIPLIER; // More time available
        }
    }
}
