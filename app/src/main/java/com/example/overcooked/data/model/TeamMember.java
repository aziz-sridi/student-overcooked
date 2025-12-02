package com.example.overcooked.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Objects;

/**
 * Team member entity for Room database
 * Represents a member of a team project
 */
@Entity(
        tableName = "team_members",
        foreignKeys = @ForeignKey(
                entity = Project.class,
                parentColumns = "id",
                childColumns = "projectId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("projectId")
)
public class TeamMember {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long projectId;
    private String name;
    private String email;
    private MemberRole role;

    public TeamMember() {
        this.id = 0;
        this.projectId = 0;
        this.name = "";
        this.email = "";
        this.role = MemberRole.MEMBER;
    }

    public TeamMember(long id, long projectId, String name, String email, MemberRole role) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.email = email != null ? email : "";
        this.role = role != null ? role : MemberRole.MEMBER;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public MemberRole getRole() { return role; }
    public void setRole(MemberRole role) { this.role = role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMember that = (TeamMember) o;
        return id == that.id &&
                projectId == that.projectId &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId, name, email, role);
    }
}
