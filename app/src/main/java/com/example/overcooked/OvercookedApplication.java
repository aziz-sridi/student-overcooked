package com.example.overcooked;

import android.app.Application;

import com.example.overcooked.data.database.OvercookedDatabase;
import com.example.overcooked.data.repository.GroupRepository;
import com.example.overcooked.data.repository.ProjectRepository;
import com.example.overcooked.data.repository.TaskRepository;

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
    private com.example.overcooked.data.repository.UserRepository userRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
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

    public synchronized com.example.overcooked.data.repository.UserRepository getUserRepository() {
        if (userRepository == null) {
            userRepository = new com.example.overcooked.data.repository.UserRepository();
        }
        return userRepository;
    }
}
