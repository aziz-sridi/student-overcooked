package com.example.overcooked.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.overcooked.data.model.Group;

import java.util.List;

@Dao
public interface GroupDao {
    @Query("SELECT * FROM groups")
    LiveData<List<Group>> getAllGroups();

    @Query("SELECT * FROM groups WHERE id = :groupId")
    LiveData<Group> getGroupById(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Group group);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Group> groups);

    @Update
    void update(Group group);

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    Group getGroupByIdSync(String groupId);

    @Delete
    void delete(Group group);

    @Query("DELETE FROM groups WHERE id NOT IN (:groupIds)")
    void deleteAllExcept(List<String> groupIds);

    @Query("DELETE FROM groups")
    void deleteAll();

    @Query("DELETE FROM groups WHERE id = :groupId")
    void deleteById(String groupId);
}
