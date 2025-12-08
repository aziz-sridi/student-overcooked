package com.student.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Group model representing a study/project group
 */
@Entity(tableName = "groups")
public class Group {
    @PrimaryKey
    @androidx.annotation.NonNull
    private String id;
    private String name;
    private String subject;
    private String description;
    private String joinCode;
    private String createdBy;
    private Date createdAt;
    private int memberCount;
    private int totalTasks;
    private int completedTasks;
    private boolean individualProject;
    private Date deadline;

    public Group() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.subject = "";
        this.description = "";
        this.joinCode = generateJoinCode();
        this.createdBy = "";
        this.createdAt = new Date();
        this.memberCount = 1;
        this.totalTasks = 0;
        this.completedTasks = 0;
        this.individualProject = true;
        this.deadline = null;
    }

    public Group(@androidx.annotation.NonNull String id, String name, String subject, String description,
                 String joinCode, String createdBy, Date createdAt,
                 int memberCount, int totalTasks, int completedTasks,
                 boolean individualProject, Date deadline) {
        this.id = id;
        this.name = name;
        this.subject = subject != null ? subject : "";
        this.description = description != null ? description : "";
        this.joinCode = joinCode != null ? joinCode : generateJoinCode();
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.memberCount = memberCount;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.individualProject = individualProject;
        this.deadline = deadline;
    }

    private static String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Getters and Setters
    @androidx.annotation.NonNull
    public String getId() { return id; }
    public void setId(@androidx.annotation.NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJoinCode() { return joinCode; }
    public void setJoinCode(String joinCode) { this.joinCode = joinCode; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public boolean isIndividualProject() { return individualProject; }
    public void setIndividualProject(boolean individualProject) { this.individualProject = individualProject; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    /**
     * Calculate completion percentage
     */
    public int getCompletionPercentage() {
        if (totalTasks > 0) {
            return (int) ((completedTasks / (float) totalTasks) * 100);
        }
        return 0;
    }
    public void setCompletionPercentage(int percentage) { /* Firestore compatibility - computed property */ }

    /**
     * Get progress text like "5/12 tasks"
     */
    public String getProgressText() {
        return completedTasks + "/" + totalTasks + " tasks";
    }
    public void setProgressText(String text) { /* Firestore compatibility - computed property */ }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return memberCount == group.memberCount &&
                totalTasks == group.totalTasks &&
                completedTasks == group.completedTasks &&
                Objects.equals(id, group.id) &&
                Objects.equals(name, group.name) &&
                Objects.equals(subject, group.subject) &&
                Objects.equals(description, group.description) &&
                Objects.equals(joinCode, group.joinCode) &&
                Objects.equals(createdBy, group.createdBy) &&
                Objects.equals(createdAt, group.createdAt) &&
                individualProject == group.individualProject &&
                Objects.equals(deadline, group.deadline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, subject, description, joinCode,
                createdBy, createdAt, memberCount, totalTasks, completedTasks,
                individualProject, deadline);
    }
}
