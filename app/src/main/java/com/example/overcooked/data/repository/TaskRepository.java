package com.example.overcooked.data.repository;

import androidx.lifecycle.LiveData;

import com.example.overcooked.data.dao.TaskDao;
import com.example.overcooked.data.model.Task;
import com.example.overcooked.data.model.TaskStatus;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Task data operations
 * Abstracts data access from the rest of the app
 */
public class TaskRepository {
    private final TaskDao taskDao;
    private final ExecutorService executorService;

    // Observable LiveData
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Task>> pendingTasks;
    private final LiveData<List<Task>> completedTasks;
    private final LiveData<List<Task>> standaloneTasks;
    private final LiveData<Integer> pendingTaskCount;
    private final LiveData<Integer> completedTaskCount;

    public TaskRepository(TaskDao taskDao) {
        this.taskDao = taskDao;
        this.executorService = Executors.newSingleThreadExecutor();

        this.allTasks = taskDao.getAllTasks();
        this.pendingTasks = taskDao.getPendingTasks();
        this.completedTasks = taskDao.getCompletedTasks();
        this.standaloneTasks = taskDao.getStandaloneTasks();
        this.pendingTaskCount = taskDao.getPendingTaskCount();
        this.completedTaskCount = taskDao.getCompletedTaskCount();
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
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date endOfDay = calendar.getTime();

        return taskDao.getTasksDueBetween(startOfDay, endOfDay);
    }

    public LiveData<List<Task>> getTasksDueThisWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date endOfWeek = calendar.getTime();

        return taskDao.getTasksDueBetween(startOfDay, endOfWeek);
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
        executorService.execute(() -> {
            long id = taskDao.insertTask(task);
            if (callback != null) {
                callback.onResult(id);
            }
        });
    }

    public void insertTask(Task task) {
        executorService.execute(() -> taskDao.insertTask(task));
    }

    public void insertTasks(List<Task> tasks) {
        executorService.execute(() -> taskDao.insertTasks(tasks));
    }

    public void updateTask(Task task) {
        executorService.execute(() -> taskDao.updateTask(task));
    }

    public void deleteTask(Task task) {
        executorService.execute(() -> taskDao.deleteTask(task));
    }

    public void deleteTaskById(long taskId) {
        executorService.execute(() -> taskDao.deleteTaskById(taskId));
    }

    public void toggleTaskCompletion(long taskId, boolean isCompleted) {
        executorService.execute(() -> {
            Date completedAt = isCompleted ? new Date() : null;
            taskDao.updateTaskCompletion(taskId, isCompleted, completedAt);
        });
    }

    public void updateTaskStatus(long taskId, TaskStatus status) {
        executorService.execute(() -> {
            boolean isCompleted = (status == TaskStatus.DONE);
            Date completedAt = isCompleted ? new Date() : null;
            taskDao.updateTaskStatus(taskId, status, isCompleted, completedAt);
        });
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
