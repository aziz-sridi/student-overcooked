package com.student.overcooked;

import android.app.Application;
import android.util.Log;

import com.student.overcooked.data.database.OvercookedDatabase;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.data.repository.ProjectRepository;
import com.student.overcooked.data.repository.TaskRepository;
import com.student.overcooked.util.SessionManager;
import com.student.overcooked.util.FirebaseDataMigration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application class for Student OverCooked
 * Provides singleton access to database and repositories
 */
public class OvercookedApplication extends Application {

    private static volatile OvercookedApplication instance;

    // Lazy initialized instances
    private OvercookedDatabase database;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private GroupRepository groupRepository;
    private com.student.overcooked.data.repository.UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        // Configure Firebase Realtime Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        // Run Firebase data migration to populate missing user displayNames
        runFirebaseDataMigration();
    }

    private void runFirebaseDataMigration() {
        FirebaseDataMigration migration = new FirebaseDataMigration(
                FirebaseFirestore.getInstance(),
                FirebaseAuth.getInstance()
        );
        
        migration.migrateUserDisplayNames(new FirebaseDataMigration.OnMigrationCompleteListener() {
            @Override
            public void onComplete(int migratedCount, int errorCount) {
                Log.d("FirebaseMigration", "Migration complete. Migrated: " + migratedCount + ", Errors: " + errorCount);
            }

            @Override
            public void onError(Exception e) {
                Log.e("FirebaseMigration", "Migration failed: " + e.getMessage(), e);
            }
        });
    }

    public static OvercookedApplication getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Application not initialized");
        }
        return instance;
    }

    public synchronized OvercookedDatabase getDatabase() {
        if (database == null) {
            database = OvercookedDatabase.getDatabase(this);
        }
        return database;
    }

    public synchronized void resetLocalCache() {
        if (database != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                database.clearAllTables();
                database.close();
                OvercookedDatabase.closeDatabase();
                database = null;
            });
            executor.shutdown();
        }
    }

    public synchronized TaskRepository getTaskRepository() {
        if (taskRepository == null) {
            taskRepository = new TaskRepository(getDatabase().taskDao());
        }
        return taskRepository;
    }

    public synchronized ProjectRepository getProjectRepository() {
        if (projectRepository == null) {
            projectRepository = new ProjectRepository(
                    getDatabase().projectDao(),
                    getDatabase().teamMemberDao()
            );
        }
        return projectRepository;
    }

    public synchronized GroupRepository getGroupRepository() {
        if (groupRepository == null) {
            groupRepository = new GroupRepository(getDatabase().groupDao());
        }
        return groupRepository;
    }

    public synchronized com.student.overcooked.data.repository.UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new com.student.overcooked.data.repository.UserRepository();
        }
        return userRepository;
    }

    public synchronized SessionManager getSessionManager() {
        if (sessionManager == null) {
            sessionManager = new SessionManager(this);
        }
        return sessionManager;
    }
}
