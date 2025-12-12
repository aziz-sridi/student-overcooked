package com.student.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Group task - tasks specific to a group
 */
@Entity(tableName = "group_tasks")
public class GroupTask extends BaseTask {
    @PrimaryKey
    @androidx.annotation.NonNull
    private String id;
    private String groupId;
    private String assigneeId;
    private String assigneeName;
    private String createdBy;

    // Local-first sync fields (Room source-of-truth). Excluded from Firestore.
    private boolean pendingSync;
    private boolean pendingDelete;
    private boolean lastSyncedExists;
    private boolean lastSyncedCompleted;

    public GroupTask() {
        super();
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.assigneeId = null;
        this.assigneeName = null;
        this.createdBy = "";
        this.status = TaskStatus.NOT_STARTED;

        this.pendingSync = false;
        this.pendingDelete = false;
        this.lastSyncedExists = false;
        this.lastSyncedCompleted = false;
    }

    public GroupTask(@androidx.annotation.NonNull String id, String groupId, String title, String description,
                     String assigneeId, String assigneeName, Priority priority, Date deadline, boolean isCompleted,
                     Date completedAt, String createdBy, Date createdAt) {
        super();
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
        this.status = isCompleted ? TaskStatus.DONE : TaskStatus.NOT_STARTED;

        this.pendingSync = false;
        this.pendingDelete = false;
        this.lastSyncedExists = true;
        this.lastSyncedCompleted = isCompleted;
    }

    // Getters and Setters
    @androidx.annotation.NonNull
    public String getId() { return id; }
    public void setId(@androidx.annotation.NonNull String id) { this.id = id; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    // Alias for adapter compatibility
    public String getAssignedToName() { return assigneeName; }
    public void setAssignedToName(String name) { /* Firestore compatibility - maps to assigneeName */ }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Exclude
    public boolean isPendingSync() { return pendingSync; }

    public void setPendingSync(boolean pendingSync) { this.pendingSync = pendingSync; }

    @Exclude
    public boolean isPendingDelete() { return pendingDelete; }

    public void setPendingDelete(boolean pendingDelete) { this.pendingDelete = pendingDelete; }

    @Exclude
    public boolean isLastSyncedExists() { return lastSyncedExists; }

    public void setLastSyncedExists(boolean lastSyncedExists) { this.lastSyncedExists = lastSyncedExists; }

    @Exclude
    public boolean isLastSyncedCompleted() { return lastSyncedCompleted; }

    public void setLastSyncedCompleted(boolean lastSyncedCompleted) { this.lastSyncedCompleted = lastSyncedCompleted; }


    public boolean isOverdue() {
        return !isCompleted && deadline != null && deadline.before(new Date());
    }
    public void setOverdue(boolean overdue) { /* Firestore compatibility - computed property */ }

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
            Objects.equals(createdAt, groupTask.createdAt) &&
            status == groupTask.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupId, title, description, assigneeId, assigneeName,
            deadline, isCompleted, completedAt, createdBy, createdAt, status);
    }
}
