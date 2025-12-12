package com.student.overcooked.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.student.overcooked.data.model.GroupTask;

import java.util.Date;
import java.util.List;

@Dao
public interface GroupTaskDao {

    @Query("SELECT * FROM group_tasks WHERE groupId = :groupId AND pendingDelete = 0 ORDER BY (deadline IS NULL) ASC, deadline ASC")
    LiveData<List<GroupTask>> getGroupTasks(String groupId);

    @Query("SELECT * FROM group_tasks WHERE groupId = :groupId")
    List<GroupTask> getGroupTasksSync(String groupId);

    @Query("SELECT * FROM group_tasks")
    List<GroupTask> getAllSync();

    @Query("SELECT * FROM group_tasks WHERE pendingDelete = 0 AND isCompleted = 0 AND deadline BETWEEN :start AND :end ORDER BY deadline ASC")
    List<GroupTask> getTasksDueBetweenSync(Date start, Date end);

    @Query("SELECT * FROM group_tasks WHERE id = :taskId LIMIT 1")
    GroupTask getByIdSync(String taskId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(GroupTask task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<GroupTask> tasks);

    @Update
    void update(GroupTask task);

    @Query("DELETE FROM group_tasks WHERE id = :taskId")
    void deleteById(String taskId);

    @Query("SELECT * FROM group_tasks WHERE pendingSync = 1")
    List<GroupTask> getPendingSyncTasksSync();
}
