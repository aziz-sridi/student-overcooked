package com.student.overcooked.data.model;

import java.util.Date;

/**
 * Base task model shared by personal and group tasks.
 *
 * Note: Concrete types (`Task`, `GroupTask`) still define their own identifiers and
 * additional fields (task type, assignee, group id, etc.).
 */
public class BaseTask {
    protected String title;
    protected String description;
    protected Priority priority;
    protected Date deadline;
    protected boolean isCompleted;
    protected Date completedAt;
    protected Date createdAt;
    protected TaskStatus status;

    // Coins should be granted only once per task.
    protected boolean rewardClaimed;

    public BaseTask() {
        this.title = "";
        this.description = "";
        this.priority = Priority.MEDIUM;
        this.deadline = new Date();
        this.isCompleted = false;
        this.completedAt = null;
        this.createdAt = new Date();
        this.status = TaskStatus.NOT_STARTED;
        this.rewardClaimed = false;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public TaskStatus getStatus() { return status != null ? status : TaskStatus.NOT_STARTED; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public boolean isRewardClaimed() { return rewardClaimed; }
    public void setRewardClaimed(boolean rewardClaimed) { this.rewardClaimed = rewardClaimed; }
}
