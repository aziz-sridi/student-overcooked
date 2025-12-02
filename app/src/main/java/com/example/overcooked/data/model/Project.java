package com.example.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;

/**
 * Project entity for Room database
 * Represents a student project (individual or team)
 */
@Entity(tableName = "projects")
public class Project {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String description;
    private String course;
    private Date deadline;
    private boolean isTeamProject;
    private boolean isCompleted;
    private String colorHex;
    private Date createdAt;
    private Date completedAt;

    public Project() {
        this.id = 0;
        this.name = "";
        this.description = "";
        this.course = "";
        this.deadline = new Date();
        this.isTeamProject = false;
        this.isCompleted = false;
        this.colorHex = "#FF6B35";
        this.createdAt = new Date();
        this.completedAt = null;
    }

    public Project(long id, String name, String description, String course, Date deadline,
                   boolean isTeamProject, boolean isCompleted, String colorHex,
                   Date createdAt, Date completedAt) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.course = course != null ? course : "";
        this.deadline = deadline;
        this.isTeamProject = isTeamProject;
        this.isCompleted = isCompleted;
        this.colorHex = colorHex != null ? colorHex : "#FF6B35";
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    // Alias for adapter compatibility
    public String getSubject() { return course; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public boolean isTeamProject() { return isTeamProject; }
    public void setTeamProject(boolean teamProject) { isTeamProject = teamProject; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    /**
     * Check if the project is overdue
     */
    public boolean isOverdue() {
        return !isCompleted && deadline.before(new Date());
    }

    /**
     * Get days until deadline (negative if overdue)
     */
    public int daysUntilDeadline() {
        Date now = new Date();
        long diffInMillis = deadline.getTime() - now.getTime();
        return (int) (diffInMillis / (1000 * 60 * 60 * 24));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return id == project.id &&
                isTeamProject == project.isTeamProject &&
                isCompleted == project.isCompleted &&
                Objects.equals(name, project.name) &&
                Objects.equals(description, project.description) &&
                Objects.equals(course, project.course) &&
                Objects.equals(deadline, project.deadline) &&
                Objects.equals(colorHex, project.colorHex) &&
                Objects.equals(createdAt, project.createdAt) &&
                Objects.equals(completedAt, project.completedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, course, deadline,
                isTeamProject, isCompleted, colorHex, createdAt, completedAt);
    }
}
