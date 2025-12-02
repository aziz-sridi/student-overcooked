package com.example.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Group task - tasks specific to a group
 */
@Entity(tableName = "group_tasks")
public class GroupTask {
    @PrimaryKey
    @androidx.annotation.NonNull
    private String id;
    private String groupId;
    private String title;
    private String description;
    private String assigneeId;
    private String assigneeName;
    private Priority priority;
    private Date deadline;
    private boolean isCompleted;
    private Date completedAt;
    private String createdBy;
    private Date createdAt;

    public GroupTask() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.title = "";
        this.description = "";
        this.assigneeId = null;
        this.assigneeName = null;
        this.priority = Priority.MEDIUM;
        this.deadline = new Date();
        this.isCompleted = false;
        this.completedAt = null;
        this.createdBy = "";
        this.createdAt = new Date();
    }

    public GroupTask(@androidx.annotation.NonNull String id, String groupId, String title, String description,
                     String assigneeId, String assigneeName, Priority priority, Date deadline, boolean isCompleted,
                     Date completedAt, String createdBy, Date createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.description = description != null ? description : "";
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.completedAt = completedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : new Date();
    }

    // Getters and Setters
    @androidx.annotation.NonNull
    public String getId() { return id; }
    public void setId(@androidx.annotation.NonNull String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    // Alias for adapter compatibility
    public String getAssignedToName() { return assigneeName; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isOverdue() {
        return !isCompleted && deadline.before(new Date());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupTask groupTask = (GroupTask) o;
        return isCompleted == groupTask.isCompleted &&
                Objects.equals(id, groupTask.id) &&
                Objects.equals(groupId, groupTask.groupId) &&
                Objects.equals(title, groupTask.title) &&
                Objects.equals(description, groupTask.description) &&
                Objects.equals(assigneeId, groupTask.assigneeId) &&
                Objects.equals(assigneeName, groupTask.assigneeName) &&
                Objects.equals(deadline, groupTask.deadline) &&
                Objects.equals(completedAt, groupTask.completedAt) &&
                Objects.equals(createdBy, groupTask.createdBy) &&
                Objects.equals(createdAt, groupTask.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupId, title, description, assigneeId, assigneeName,
                deadline, isCompleted, completedAt, createdBy, createdAt);
    }
}
