package com.student.overcooked.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.student.overcooked.data.model.TeamMember;

import java.util.List;

/**
 * Data Access Object for TeamMember operations
 */
@Dao
public interface TeamMemberDao {

    @Query("SELECT * FROM team_members WHERE projectId = :projectId")
    LiveData<List<TeamMember>> getMembersByProject(long projectId);

    @Query("SELECT * FROM team_members WHERE id = :memberId")
    TeamMember getMemberById(long memberId);

    @Query("SELECT COUNT(*) FROM team_members WHERE projectId = :projectId")
    LiveData<Integer> getMemberCountByProject(long projectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMember(TeamMember member);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMembers(List<TeamMember> members);

    @Update
    void updateMember(TeamMember member);

    @Delete
    void deleteMember(TeamMember member);

    @Query("DELETE FROM team_members WHERE id = :memberId")
    void deleteMemberById(long memberId);

    @Query("DELETE FROM team_members WHERE projectId = :projectId")
    void deleteMembersByProject(long projectId);
}
