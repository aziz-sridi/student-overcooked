package com.student.overcooked.util;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.student.overcooked.R;
import com.student.overcooked.ui.MainNavActivity;

/**
 * Helper class for managing notifications in the app
 */
public class NotificationHelper {

    private static final String CHANNEL_ID_MESSAGES = "group_messages";
    private static final String CHANNEL_ID_GROUP_TASKS = "group_tasks";
    private static final String CHANNEL_ID_DEADLINES = "task_deadlines";
    private static final String CHANNEL_NAME_MESSAGES = "Group Messages";
    private static final String CHANNEL_NAME_GROUP_TASKS = "Group Tasks";
    private static final String CHANNEL_NAME_DEADLINES = "Task Deadlines";

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = NotificationManagerCompat.from(this.context);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // Messages channel
                NotificationChannel messagesChannel = new NotificationChannel(
                        CHANNEL_ID_MESSAGES,
                        CHANNEL_NAME_MESSAGES,
                        NotificationManager.IMPORTANCE_HIGH
                );
                messagesChannel.setDescription("Notifications for new group messages");
                manager.createNotificationChannel(messagesChannel);

            // Group tasks channel
            NotificationChannel groupTasksChannel = new NotificationChannel(
                CHANNEL_ID_GROUP_TASKS,
                CHANNEL_NAME_GROUP_TASKS,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            groupTasksChannel.setDescription("Notifications for new group tasks");
            manager.createNotificationChannel(groupTasksChannel);

                // Deadlines channel
                NotificationChannel deadlinesChannel = new NotificationChannel(
                        CHANNEL_ID_DEADLINES,
                        CHANNEL_NAME_DEADLINES,
                        NotificationManager.IMPORTANCE_HIGH
                );
                deadlinesChannel.setDescription("Notifications for approaching task deadlines");
                manager.createNotificationChannel(deadlinesChannel);
            }
        }
    }

    /**
     * Show a notification for a new group message
     */
    @SuppressLint("MissingPermission")
    public void showMessageNotification(String groupId, String groupName, String senderName, String message) {
        if (!hasNotificationPermission()) {
            return;
        }

        Intent intent = new Intent(context, MainNavActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("group_id", groupId);
        intent.putExtra("navigate_to", "groups");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                groupId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(groupName)
                .setContentText(senderName + ": " + message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(senderName + ": " + message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(groupId.hashCode(), builder.build());
    }

    /**
     * Show a notification for a newly created group task
     */
    @SuppressLint("MissingPermission")
    public void showGroupTaskNotification(String groupId, String groupName, String taskTitle) {
        if (!hasNotificationPermission()) {
            return;
        }

        Intent intent = new Intent(context, MainNavActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("group_id", groupId);
        intent.putExtra("navigate_to", "groups");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (groupId + "_task").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_GROUP_TASKS)
                .setSmallIcon(R.drawable.ic_add_task)
                .setContentTitle(groupName)
                .setContentText("New task: " + taskTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("New task: " + taskTitle))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((groupId + "_task").hashCode(), builder.build());
    }

    /**
     * Show a notification for an approaching deadline
     */
    @SuppressLint("MissingPermission")
    public void showDeadlineNotification(String taskId, String taskTitle, String projectName, long hoursRemaining) {
        if (!hasNotificationPermission()) {
            return;
        }

        Intent intent = new Intent(context, MainNavActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("navigate_to", "tasks");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeText;
        if (hoursRemaining <= 1) {
            timeText = "Due in less than 1 hour!";
        } else if (hoursRemaining < 24) {
            timeText = "Due in " + hoursRemaining + " hours";
        } else {
            long daysRemaining = hoursRemaining / 24;
            timeText = "Due in " + daysRemaining + " day" + (daysRemaining > 1 ? "s" : "");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DEADLINES)
                .setSmallIcon(R.drawable.ic_time)
                .setContentTitle("⏰ " + taskTitle)
                .setContentText(projectName + " - " + timeText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(projectName + " - " + timeText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(taskId.hashCode(), builder.build());
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Cancel a specific notification
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
