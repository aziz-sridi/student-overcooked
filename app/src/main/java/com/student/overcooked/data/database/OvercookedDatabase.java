package com.student.overcooked.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.student.overcooked.data.dao.GroupDao;
import com.student.overcooked.data.dao.GroupTaskDao;
import com.student.overcooked.data.dao.ProjectDao;
import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.dao.TeamMemberDao;
import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Project;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TeamMember;

/**
 * Main database class for Student OverCooked app
 * Uses Room persistence library for local SQLite storage
 */
@Database(
    entities = {Task.class, Project.class, TeamMember.class, Group.class, GroupTask.class},
    version = 9,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class OvercookedDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract ProjectDao projectDao();
    public abstract TeamMemberDao teamMemberDao();
    public abstract GroupDao groupDao();
    public abstract GroupTaskDao groupTaskDao();

    private static volatile OvercookedDatabase INSTANCE;
    private static final String DATABASE_NAME = "overcooked_database";

    /**
     * Get singleton instance of the database
     */
    public static OvercookedDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (OvercookedDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            OvercookedDatabase.class,
                            DATABASE_NAME
                    )
                            .fallbackToDestructiveMigration() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Close the database instance (for testing)
     */
    public static void closeDatabase() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
