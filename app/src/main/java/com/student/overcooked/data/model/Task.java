package com.student.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * Task entity for Room database
 * Represents a student task (homework, assignment, exam, etc.)
 */
@Entity(tableName = "tasks")
public class Task extends BaseTask {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String firestoreId; // UUID for Firestore compatibility
    private String userId; // Owner of the task for multi-user sync
    private String course;
    private TaskType taskType;
    private Long projectId; // null means standalone task
    private String notes;

    // Local-first sync fields (Room source-of-truth).
    private boolean pendingSync;
    private boolean pendingDelete;
    private boolean lastSyncedExists;
    private boolean lastSyncedCompleted;

    public Task() {
        super();
        this.id = 0;
        this.firestoreId = null;
        this.userId = null;
        this.course = "";
        this.taskType = TaskType.HOMEWORK;
        this.projectId = null;
        this.notes = "";

        this.pendingSync = false;
        this.pendingDelete = false;
        this.lastSyncedExists = false;
        this.lastSyncedCompleted = false;
    }

    public Task(long id, String title, String description, String course, TaskType taskType,
                Date deadline, boolean isCompleted, Long projectId, Priority priority,
                Date createdAt, Date completedAt, String notes) {
        super();
        this.id = id;
        this.title = title;
        this.description = description != null ? description : "";
        this.course = course != null ? course : "";
        this.taskType = taskType != null ? taskType : TaskType.HOMEWORK;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.projectId = projectId;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.completedAt = completedAt;
        this.notes = notes != null ? notes : "";
        this.status = isCompleted ? TaskStatus.DONE : TaskStatus.NOT_STARTED;

        this.pendingSync = false;
        this.pendingDelete = false;
        this.lastSyncedExists = true;
        this.lastSyncedCompleted = isCompleted;
    }

    public Task(long id, String title, String description, String course, TaskType taskType,
                Date deadline, boolean isCompleted, Long projectId, Priority priority,
                Date createdAt, Date completedAt, String notes, TaskStatus status) {
        super();
        this.id = id;
        this.title = title;
        this.description = description != null ? description : "";
        this.course = course != null ? course : "";
        this.taskType = taskType != null ? taskType : TaskType.HOMEWORK;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.projectId = projectId;
        this.priority = priority != null ? priority : Priority.MEDIUM;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.completedAt = completedAt;
        this.notes = notes != null ? notes : "";
        this.status = status != null ? status : TaskStatus.NOT_STARTED;

        this.pendingSync = false;
        this.pendingDelete = false;
        this.lastSyncedExists = true;
        this.lastSyncedCompleted = isCompleted;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isPendingSync() { return pendingSync; }
    public void setPendingSync(boolean pendingSync) { this.pendingSync = pendingSync; }

    public boolean isPendingDelete() { return pendingDelete; }
    public void setPendingDelete(boolean pendingDelete) { this.pendingDelete = pendingDelete; }

    public boolean isLastSyncedExists() { return lastSyncedExists; }
    public void setLastSyncedExists(boolean lastSyncedExists) { this.lastSyncedExists = lastSyncedExists; }

    public boolean isLastSyncedCompleted() { return lastSyncedCompleted; }
    public void setLastSyncedCompleted(boolean lastSyncedCompleted) { this.lastSyncedCompleted = lastSyncedCompleted; }

    /**
     * Check if the task is overdue
     */
    public boolean isOverdue() {
        return !isCompleted && deadline != null && deadline.before(new Date());
    }

    /**
     * Check if the task is due today
     */
    public boolean isDueToday() {
        if (deadline == null) return false;
        
        Date now = new Date();
        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(now);
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);
        Date today = todayCal.getTime();

        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.setTime(now);
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);
        tomorrowCal.set(Calendar.HOUR_OF_DAY, 0);
        tomorrowCal.set(Calendar.MINUTE, 0);
        tomorrowCal.set(Calendar.SECOND, 0);
        tomorrowCal.set(Calendar.MILLISECOND, 0);
        Date tomorrow = tomorrowCal.getTime();

        return !deadline.before(today) && deadline.before(tomorrow);
    }

    /**
     * Get days until deadline (negative if overdue)
     */
    public int daysUntilDeadline() {
        if (deadline == null) return Integer.MAX_VALUE;
        Date now = new Date();
        long diffInMillis = deadline.getTime() - now.getTime();
        return (int) (diffInMillis / (1000 * 60 * 60 * 24));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
                isCompleted == task.isCompleted &&
                Objects.equals(title, task.title) &&
                Objects.equals(description, task.description) &&
                Objects.equals(course, task.course) &&
                taskType == task.taskType &&
                Objects.equals(deadline, task.deadline) &&
                Objects.equals(projectId, task.projectId) &&
                priority == task.priority &&
                Objects.equals(createdAt, task.createdAt) &&
                Objects.equals(completedAt, task.completedAt) &&
                Objects.equals(notes, task.notes) &&
                status == task.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, course, taskType, deadline,
                isCompleted, projectId, priority, createdAt, completedAt, notes, status);
    }
}
