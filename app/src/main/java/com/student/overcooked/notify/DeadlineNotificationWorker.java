package com.student.overcooked.notify;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.student.overcooked.OvercookedApplication;
import com.student.overcooked.data.dao.GroupDao;
import com.student.overcooked.data.dao.GroupTaskDao;
import com.student.overcooked.data.dao.TaskDao;
import com.student.overcooked.data.model.Group;
import com.student.overcooked.data.model.GroupTask;
import com.student.overcooked.data.model.Task;
import com.student.overcooked.util.NotificationHelper;
import com.student.overcooked.util.NotificationSettings;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeadlineNotificationWorker extends Worker {

    private static final String UNIQUE_WORK_NAME = "deadline_notifications";

    private static final String PREFS = "deadline_notification_prefs";
    private static final String KEY_LAST_NOTIFIED_PREFIX = "last_notified_";

    // Notify when within 24 hours.
    private static final long WINDOW_MILLIS = 24L * 60L * 60L * 1000L;

    public DeadlineNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(@NonNull Context context) {
        Constraints constraints = new Constraints.Builder()
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DeadlineNotificationWorker.class,
                6, TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(@NonNull Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!NotificationSettings.areNotificationsEnabled(context)) {
            return Result.success();
        }

        OvercookedApplication app = (OvercookedApplication) context.getApplicationContext();
        TaskDao taskDao = app.getDatabase().taskDao();
        GroupTaskDao groupTaskDao = app.getDatabase().groupTaskDao();
        GroupDao groupDao = app.getDatabase().groupDao();

        NotificationHelper notifications = new NotificationHelper(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        Date now = new Date();
        Date end = new Date(now.getTime() + WINDOW_MILLIS);

        List<Task> tasks = taskDao.getTasksDueBetweenSync(now, end);
        for (Task task : tasks) {
            if (task.getDeadline() == null) continue;
            long hoursRemaining = Math.max(0, (task.getDeadline().getTime() - now.getTime()) / (60L * 60L * 1000L));
            maybeNotifyDeadline(
                    prefs,
                    notifications,
                    "task_" + task.getId(),
                    task.getTitle(),
                    task.getCourse() != null && !task.getCourse().isEmpty() ? task.getCourse() : "Personal task",
                    hoursRemaining
            );
        }

        List<GroupTask> groupTasks = groupTaskDao.getTasksDueBetweenSync(now, end);
        for (GroupTask task : groupTasks) {
            if (task.getDeadline() == null) continue;
            long hoursRemaining = Math.max(0, (task.getDeadline().getTime() - now.getTime()) / (60L * 60L * 1000L));
            String groupName = "Group";
            if (task.getGroupId() != null) {
                Group group = groupDao.getGroupByIdSync(task.getGroupId());
                if (group != null && group.getName() != null && !group.getName().isEmpty()) {
                    groupName = group.getName();
                }
            }

            maybeNotifyDeadline(
                    prefs,
                    notifications,
                    "group_task_" + task.getId(),
                    task.getTitle(),
                    groupName,
                    hoursRemaining
            );
        }

        return Result.success();
    }

    private static void maybeNotifyDeadline(
            @NonNull SharedPreferences prefs,
            @NonNull NotificationHelper notifications,
            @NonNull String stableId,
            @NonNull String title,
            @NonNull String projectName,
            long hoursRemaining
    ) {
        // Avoid spamming: notify each task at most once every 12 hours.
        long now = System.currentTimeMillis();
        long last = prefs.getLong(KEY_LAST_NOTIFIED_PREFIX + stableId, 0L);
        if (last != 0L && (now - last) < (12L * 60L * 60L * 1000L)) {
            return;
        }

        notifications.showDeadlineNotification(stableId, title, projectName, hoursRemaining);
        prefs.edit().putLong(KEY_LAST_NOTIFIED_PREFIX + stableId, now).apply();
    }
}
