package com.example.todo.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.todo.R;
import com.example.todo.data.database.entities.Task;
import com.example.todo.receivers.TaskNotificationReceiver;
import com.example.todo.ui.main.MainActivity;


public class NotificationHelper {

    public static final String CHANNEL_ID = "todo_task_notifications";
    public static final String CHANNEL_NAME = "Task Reminders";
    public static final String CHANNEL_DESCRIPTION = "Notifications for upcoming tasks";

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_DESCRIPTION = "task_description";

    private final Context context;
    private final NotificationManager notificationManager;
    private final AlarmManager alarmManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleTaskNotification(Task task) {
        if (!task.isNotificationEnabled() || task.isCompleted() || task.getCompletionTime() <= 0) {
            return;
        }

        long notificationTime = task.getCompletionTime() - (task.getNotificationMinutesBefore() * 60 * 1000L);
        long currentTime = System.currentTimeMillis();

        if (notificationTime <= currentTime) {
            return;
        }

        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, task.getId());
        intent.putExtra(EXTRA_TASK_TITLE, task.getTitle());
        intent.putExtra(EXTRA_TASK_DESCRIPTION, task.getDescription());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        }
    }

    public void cancelTaskNotification(int taskId) {
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);

        notificationManager.cancel(taskId);
    }
    public void updateTaskNotification(Task task) {
        cancelTaskNotification(task.getId());
        scheduleTaskNotification(task);
    }

    public void showTaskNotification(int taskId, String title, String description, long completionTime) {
        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.putExtra(EXTRA_TASK_ID, taskId);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Напоминание: " + title)
                .setContentText(getNotificationText(description, completionTime))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        if (description != null && description.length() > 50) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(description)
                    .setSummaryText("До выполнения: " + DateUtils.getRelativeTimeString(completionTime)));
        }

        addNotificationActions(builder, taskId);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        try {
            notificationManagerCompat.notify(taskId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void addNotificationActions(NotificationCompat.Builder builder, int taskId) {
        Intent completeIntent = new Intent(context, TaskNotificationReceiver.class);
        completeIntent.setAction("ACTION_COMPLETE_TASK");
        completeIntent.putExtra(EXTRA_TASK_ID, taskId);

        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 1000 + 1,
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_check, "Выполнено", completePendingIntent);

        Intent snoozeIntent = new Intent(context, TaskNotificationReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE_TASK");
        snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 1000 + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_snooze, "Отложить", snoozePendingIntent);
    }

    private String getNotificationText(String description, long completionTime) {
        String timeText = DateUtils.getRelativeTimeString(completionTime);

        if (description != null && !description.trim().isEmpty()) {
            return description.length() > 100 ?
                    description.substring(0, 100) + "... • " + timeText :
                    description + " • " + timeText;
        } else {
            return "До выполнения: " + timeText;
        }
    }

    public void showSummaryNotification(int activeNotificationsCount) {
        if (activeNotificationsCount < 2) return;

        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Напоминания о задачах")
                .setContentText("У вас " + activeNotificationsCount + " предстоящих задач")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setGroup("todo_tasks")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent);

        notificationManager.notify(999999, builder.build()); // Специальный ID для сводки
    }

    public boolean areNotificationsEnabled() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }

    public int getActiveNotificationsCount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.getActiveNotifications().length;
        }
        return 0;
    }

    public void rescheduleAllNotifications(java.util.List<Task> tasks) {
        for (Task task : tasks) {
            if (task.isNotificationEnabled() && !task.isCompleted()) {
                scheduleTaskNotification(task);
            }
        }
    }
}