package com.student.overcooked.data.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Data class that holds a project with all its related tasks and team members
 * Used for Room database relationships
 */
public class ProjectWithTasks {
    @Embedded
    public Project project;

    @Relation(
            parentColumn = "id",
            entityColumn = "projectId"
    )
    public List<Task> tasks;

    @Relation(
            parentColumn = "id",
            entityColumn = "projectId"
    )
    public List<TeamMember> teamMembers;

    public ProjectWithTasks() {}

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<TeamMember> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMember> teamMembers) {
        this.teamMembers = teamMembers;
    }

    /**
     * Calculate project completion percentage
     */
    public int getCompletionPercentage() {
        if (tasks == null || tasks.isEmpty()) return 0;
        int completedTasks = 0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                completedTasks++;
            }
        }
        return (int) ((completedTasks / (float) tasks.size()) * 100);
    }

    /**
     * Get count of completed tasks
     */
    public int getCompletedTaskCount() {
        if (tasks == null) return 0;
        int count = 0;
        for (Task task : tasks) {
            if (task.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get count of pending tasks
     */
    public int getPendingTaskCount() {
        if (tasks == null) return 0;
        int count = 0;
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                count++;
            }
        }
        return count;
    }
}
