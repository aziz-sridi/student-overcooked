package com.example.overcooked.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.overcooked.data.model.Project;
import com.example.overcooked.data.model.ProjectWithTasks;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Project operations
 */
@Dao
public interface ProjectDao {

    // ================= Query Operations =================

    @Query("SELECT * FROM projects ORDER BY deadline ASC")
    LiveData<List<Project>> getAllProjects();

    @Query("SELECT * FROM projects WHERE isCompleted = 0 ORDER BY deadline ASC")
    LiveData<List<Project>> getActiveProjects();

    @Query("SELECT * FROM projects WHERE isCompleted = 1 ORDER BY completedAt DESC")
    LiveData<List<Project>> getCompletedProjects();

    @Query("SELECT * FROM projects WHERE isTeamProject = 1 ORDER BY deadline ASC")
    LiveData<List<Project>> getTeamProjects();

    @Query("SELECT * FROM projects WHERE isTeamProject = 0 ORDER BY deadline ASC")
    LiveData<List<Project>> getIndividualProjects();

    @Query("SELECT * FROM projects WHERE id = :projectId")
    Project getProjectById(long projectId);

    @Query("SELECT * FROM projects WHERE course = :course ORDER BY deadline ASC")
    LiveData<List<Project>> getProjectsByCourse(String course);

    @Query("SELECT COUNT(*) FROM projects WHERE isCompleted = 0")
    LiveData<Integer> getActiveProjectCount();

    // ================= Transaction Queries (with relations) =================

    @Transaction
    @Query("SELECT * FROM projects ORDER BY deadline ASC")
    LiveData<List<ProjectWithTasks>> getAllProjectsWithTasks();

    @Transaction
    @Query("SELECT * FROM projects WHERE isCompleted = 0 ORDER BY deadline ASC")
    LiveData<List<ProjectWithTasks>> getActiveProjectsWithTasks();

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    LiveData<ProjectWithTasks> getProjectWithTasks(long projectId);

    // ================= Insert/Update/Delete Operations =================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProject(Project project);

    @Update
    void updateProject(Project project);

    @Delete
    void deleteProject(Project project);

    @Query("DELETE FROM projects WHERE id = :projectId")
    void deleteProjectById(long projectId);

    @Query("UPDATE projects SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :projectId")
    void updateProjectCompletion(long projectId, boolean isCompleted, Date completedAt);

    @Query("DELETE FROM projects")
    void deleteAllProjects();
}
