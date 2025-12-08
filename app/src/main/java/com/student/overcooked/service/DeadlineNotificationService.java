package com.student.overcooked.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.data.repository.GroupRepository;
import com.student.overcooked.data.repository.TaskRepository;
import com.student.overcooked.util.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background service to check for approaching deadlines and send notifications
 */
public class DeadlineNotificationService extends Service {

    private static final String TAG = "DeadlineService";
    private static final long CHECK_INTERVAL_MINUTES = 30; // Check every 30 minutes
    
    private ScheduledExecutorService scheduler;
    private NotificationHelper notificationHelper;
    private TaskRepository taskRepository;
    private GroupRepository groupRepository;
    private final Set<String> notifiedTasks = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        taskRepository = ((OvercookedApplication) getApplication()).getTaskRepository();
        groupRepository = ((OvercookedApplication) getApplication()).getGroupRepository();
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                this::checkDeadlines,
                0,
                CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        
        Log.d(TAG, "Deadline notification service started");
    }

    private void checkDeadlines() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            // Check individual tasks
            taskRepository.getAllTasks().observeForever(tasks -> {
                if (tasks != null) {
                    for (Task task : tasks) {
                        checkTaskDeadline(task);
                    }
                }
            });

            // Check group tasks
            groupRepository.getUserGroups().observeForever(groups -> {
                if (groups != null) {
                    for (com.student.overcooked.data.model.Group group : groups) {
                        groupRepository.getGroupTasks(group.getId()).observeForever(groupTasks -> {
                            if (groupTasks != null) {
                                for (GroupTask task : groupTasks) {
                                    checkGroupTaskDeadline(task, group.getName());
                                }
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error checking deadlines", e);
        }
    }

    private void checkTaskDeadline(Task task) {
        if (task.isCompleted() || task.getDeadline() == null) {
            return;
        }

        long hoursUntilDeadline = getHoursUntilDeadline(task.getDeadline());
        
        // Notify if deadline is within 24 hours and we haven't notified yet
        if (hoursUntilDeadline <= 24 && hoursUntilDeadline >= 0) {
            String taskKey = "task_" + task.getId();
            if (!notifiedTasks.contains(taskKey)) {
                notificationHelper.showDeadlineNotification(
                        String.valueOf(task.getId()),
                        task.getTitle(),
                        "Personal Task",
                        hoursUntilDeadline
                );
                notifiedTasks.add(taskKey);
            }
        }
    }

    private void checkGroupTaskDeadline(GroupTask task, String groupName) {
        if (task.isCompleted() || task.getDeadline() == null) {
            return;
        }

        long hoursUntilDeadline = getHoursUntilDeadline(task.getDeadline());
        
        // Notify if deadline is within 24 hours and we haven't notified yet
        if (hoursUntilDeadline <= 24 && hoursUntilDeadline >= 0) {
            String taskKey = "grouptask_" + task.getId();
            if (!notifiedTasks.contains(taskKey)) {
                notificationHelper.showDeadlineNotification(
                        task.getId(),
                        task.getTitle(),
                        groupName,
                        hoursUntilDeadline
                );
                notifiedTasks.add(taskKey);
            }
        }
    }

    private long getHoursUntilDeadline(Date deadline) {
        long now = System.currentTimeMillis();
        long deadlineTime = deadline.getTime();
        return (deadlineTime - now) / (1000 * 60 * 60); // Convert to hours
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        Log.d(TAG, "Deadline notification service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
