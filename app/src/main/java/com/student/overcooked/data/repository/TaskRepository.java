package com.student.overcooked.data.repository;

import androidx.lifecycle.LiveData;

import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.model.TaskStatus;
import com.student.overcooked.data.repository.task.TaskRealtimeDataSource;
import com.student.overcooked.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Task data operations
 * Abstracts data access from the rest of the app
 * Now syncs tasks to Firebase Realtime Database for instant real-time multi-device access
 */
public class TaskRepository {
    private static final int TASK_COMPLETION_REWARD = 50;

    private final TaskDao taskDao;
    private final ExecutorService executorService;
    private final TaskRealtimeDataSource realtimeDataSource;
    private final UserRepository userRepository;
    private final android.content.Context appContext;

    // Observable LiveData
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Task>> pendingTasks;
    private final LiveData<List<Task>> completedTasks;
    private final LiveData<List<Task>> standaloneTasks;
    private final LiveData<Integer> pendingTaskCount;
    private final LiveData<Integer> completedTaskCount;

    public TaskRepository(TaskDao taskDao, UserRepository userRepository) {
        this.taskDao = taskDao;
        this.userRepository = userRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        // Try to capture application context from UserRepository if available
        this.appContext = com.student.overcooked.OvercookedApplication.getInstance();
        // Configure Firebase Realtime Database with correct Europe West region URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://studnetovercooked-default-rtdb.europe-west1.firebasedatabase.app");
        
        this.realtimeDataSource = new TaskRealtimeDataSource(
                FirebaseAuth.getInstance(),
                database,
                taskDao,
            executorService,
            appContext
        );

        this.allTasks = taskDao.getAllTasks();
        this.pendingTasks = taskDao.getPendingTasks();
        this.completedTasks = taskDao.getCompletedTasks();
        this.standaloneTasks = taskDao.getStandaloneTasks();
        this.pendingTaskCount = taskDao.getPendingTaskCount();
        this.completedTaskCount = taskDao.getCompletedTaskCount();
        // Start real-time sync with Firebase Realtime Database
        realtimeDataSource.startSync();
    }

    // ================= Observe Tasks =================

    public LiveData<List<Task>> getAllTasks() { return allTasks; }
    public LiveData<List<Task>> getPendingTasks() { return pendingTasks; }
    public LiveData<List<Task>> getCompletedTasks() { return completedTasks; }
    public LiveData<List<Task>> getStandaloneTasks() { return standaloneTasks; }
    public LiveData<Integer> getPendingTaskCount() { return pendingTaskCount; }
    public LiveData<Integer> getCompletedTaskCount() { return completedTaskCount; }

    public LiveData<List<Task>> getOverdueTasks() {
        return taskDao.getOverdueTasks(new Date());
    }

    public LiveData<Integer> getOverdueTaskCount() {
        return taskDao.getOverdueTaskCount(new Date());
    }

    public LiveData<List<Task>> getTasksByProject(long projectId) {
        return taskDao.getTasksByProject(projectId);
    }

    public LiveData<List<Task>> getTasksDueToday() {
        return TaskDateRangeQueries.dueToday(taskDao);
    }

    public LiveData<List<Task>> getTasksDueThisWeek() {
        return TaskDateRangeQueries.dueWithinDays(taskDao, 7);
    }

    public LiveData<List<Task>> getTasksByCourse(String course) {
        return taskDao.getTasksByCourse(course);
    }

    // ================= Single Task Operations =================

    public void getTaskById(long taskId, Callback<Task> callback) {
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            callback.onResult(task);
        });
    }

    public void insertTask(Task task, Callback<Long> callback) {
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(new Date());
        }
        realtimeDataSource.createTask(task,
                () -> {
                    if (callback != null) {
                        callback.onResult(task.getId());
                    }
                },
                () -> {
                    if (callback != null) {
                        callback.onResult(-1L);
                    }
                });
    }

    public void insertTask(Task task) {
        insertTask(task, null);
    }

    public void insertTasks(List<Task> tasks) {
        for (Task task : tasks) {
            insertTask(task);
        }
    }

    public void updateTask(Task task) {
        realtimeDataSource.updateTask(task, 
            () -> {},
            () -> {
                // Firebase failed, but local was already updated by TaskRealtimeDataSource
                android.util.Log.w("TaskRepository", "Firebase update failed, local update was applied");
            });
    }
    
    /**
     * Update task with completion callback
     */
    public void updateTaskWithCallback(Task task, Runnable onSuccess, Runnable onFailure) {
        realtimeDataSource.updateTask(task, onSuccess, onFailure);
    }

    public void deleteTask(Task task) {
        android.util.Log.d("TaskRepository", "Deleting task: " + task.getTitle() + " (ID: " + task.getId() + ", FirestoreID: " + task.getFirestoreId() + ")");
        realtimeDataSource.deleteTask(task,
            () -> {
                android.util.Log.d("TaskRepository", "Task deleted successfully: " + task.getTitle());
            },
            () -> {
                // Fallback to local-only if Firebase fails
                android.util.Log.w("TaskRepository", "Firebase delete failed, deleting locally: " + task.getTitle());
                executorService.execute(() -> taskDao.deleteTask(task));
            });
    }

    public void deleteTaskById(long taskId) {
        executorService.execute(() -> taskDao.deleteTaskById(taskId));
    }

    public void toggleTaskCompletion(long taskId, boolean isCompleted) {
        android.util.Log.d("TaskRepository", "toggleTaskCompletion called: taskId=" + taskId + ", isCompleted=" + isCompleted);
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                android.util.Log.d("TaskRepository", "Task found: " + task.getTitle() + ", was completed: " + task.isCompleted());
                boolean wasCompleted = task.isCompleted();
                if (wasCompleted == isCompleted) {
                    android.util.Log.d("TaskRepository", "No state change, returning");
                    return;
                }
                task.setCompleted(isCompleted);
                task.setCompletedAt(isCompleted ? new Date() : null);
                if (isCompleted) {
                    task.setStatus(TaskStatus.DONE);
                } else {
                    task.setStatus(TaskStatus.NOT_STARTED);
                }

                // Award coins only once per task (first time it reaches DONE).
                if (isCompleted && !task.isRewardClaimed()) {
                    task.setRewardClaimed(true);
                    applyCompletionReward(true);
                }

                updateTaskWithCallback(task,
                        () -> android.util.Log.d("TaskRepository", "Remote update confirmed"),
                        () -> android.util.Log.e("TaskRepository", "Remote update failed"));
            } else {
                android.util.Log.e("TaskRepository", "Task not found for ID: " + taskId);
            }
        });
    }

    public void updateTaskStatus(long taskId, TaskStatus status) {
        android.util.Log.d("TaskRepository", "🔄 updateTaskStatus called - taskId: " + taskId + ", status: " + status);
        executorService.execute(() -> {
            Task task = taskDao.getTaskById(taskId);
            if (task != null) {
                boolean wasCompleted = task.isCompleted();
                android.util.Log.d("TaskRepository", "Task found: " + task.getTitle() + ", wasCompleted: " + wasCompleted);
                
                task.setStatus(status);
                boolean isCompleted = (status == TaskStatus.DONE);
                task.setCompleted(isCompleted);
                task.setCompletedAt(isCompleted ? new Date() : null);
                
                final boolean shouldAwardCoins = wasCompleted != isCompleted;
                final boolean rewardCompleted = isCompleted;
                
                android.util.Log.d("TaskRepository", "shouldAwardCoins: " + shouldAwardCoins + ", rewardCompleted: " + rewardCompleted);
                
                if (shouldAwardCoins && rewardCompleted && !task.isRewardClaimed()) {
                    task.setRewardClaimed(true);
                    applyCompletionReward(true);
                }

                updateTaskWithCallback(task,
                        () -> android.util.Log.d("TaskRepository", "✅ Remote update success"),
                        () -> android.util.Log.e("TaskRepository", "❌ Remote update failed"));
            } else {
                android.util.Log.e("TaskRepository", "❌ Task not found for ID: " + taskId);
            }
        });
    }

    private void applyCompletionReward(boolean completed) {
        if (!completed) {
            return;
        }
        int delta = TASK_COMPLETION_REWARD;
        android.util.Log.d("TaskRepository", "Applying coin reward. Completed: " + completed + ", Delta: " + delta);
        // Always persist locally for offline-first behavior
        try {
            if (appContext != null) {
                com.student.overcooked.data.LocalCoinStore local = new com.student.overcooked.data.LocalCoinStore(appContext);
                local.addCoins(delta);
                android.util.Log.d("TaskRepository", "Local coins updated (mirror + pending delta)");
            }
        } catch (Exception e) {
            android.util.Log.w("TaskRepository", "Failed to update local coins", e);
        }
        if (userRepository == null) {
            android.util.Log.e("TaskRepository", "ERROR: userRepository is null!");
            return;
        }
        android.util.Log.d("TaskRepository", "userRepository is not null, calling updateCoinsBy with delta=" + delta);
        userRepository.updateCoinsBy(delta, 
            newBalance -> {
                android.util.Log.d("TaskRepository", "Coins updated successfully: " + newBalance);
                try {
                    if (appContext != null) {
                        com.student.overcooked.data.LocalCoinStore local = new com.student.overcooked.data.LocalCoinStore(appContext);
                        local.setBalanceFromServer(newBalance);
                    }
                } catch (Exception ignore) {}
            },
            e -> android.util.Log.e("TaskRepository", "Failed to update coins", e));
    }

    public void deleteTasksByProject(long projectId) {
        executorService.execute(() -> taskDao.deleteTasksByProject(projectId));
    }

    public void deleteAllTasks() {
        executorService.execute(taskDao::deleteAllTasks);
    }

    /**
     * Callback interface for async operations
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}
