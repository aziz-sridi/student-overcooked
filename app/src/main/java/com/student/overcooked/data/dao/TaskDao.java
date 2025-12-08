package com.student.overcooked.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;

import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Task operations
 */
@Dao
public interface TaskDao {

    // ================= Query Operations =================

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY deadline ASC")
    LiveData<List<Task>> getPendingTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    LiveData<List<Task>> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE projectId IS NULL ORDER BY deadline ASC")
    LiveData<List<Task>> getStandaloneTasks();

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY deadline ASC")
    LiveData<List<Task>> getTasksByProject(long projectId);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(long taskId);

    @Query("SELECT * FROM tasks WHERE deadline < :now AND isCompleted = 0 ORDER BY deadline ASC")
    LiveData<List<Task>> getOverdueTasks(Date now);

    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :start AND :end AND isCompleted = 0 ORDER BY deadline ASC")
    LiveData<List<Task>> getTasksDueBetween(Date start, Date end);

    @Query("SELECT * FROM tasks WHERE course = :course ORDER BY deadline ASC")
    LiveData<List<Task>> getTasksByCourse(String course);

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    LiveData<Integer> getPendingTaskCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    LiveData<Integer> getCompletedTaskCount();

    @Query("SELECT COUNT(*) FROM tasks WHERE deadline < :now AND isCompleted = 0")
    LiveData<Integer> getOverdueTaskCount(Date now);

    // ================= Insert/Update/Delete Operations =================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertTask(Task task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTasks(List<Task> tasks);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteTaskById(long taskId);

    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    void deleteTasksByProject(long projectId);

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    void updateTaskCompletion(long taskId, boolean isCompleted, Date completedAt);

    @Query("UPDATE tasks SET status = :status, isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    void updateTaskStatus(long taskId, TaskStatus status, boolean isCompleted, Date completedAt);

    @Query("DELETE FROM tasks")
    void deleteAllTasks();
}
