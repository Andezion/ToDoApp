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

/**
 * Утилитарный класс для управления уведомлениями
 * Путь: app/src/main/java/com/yourpackage/todoapp/utils/NotificationHelper.java
 */
public class NotificationHelper {

    // Константы для уведомлений
    public static final String CHANNEL_ID = "todo_task_notifications";
    public static final String CHANNEL_NAME = "Task Reminders";
    public static final String CHANNEL_DESCRIPTION = "Notifications for upcoming tasks";

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_TASK_DESCRIPTION = "task_description";

    private Context context;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        createNotificationChannel();
    }

    // === СОЗДАНИЕ КАНАЛА УВЕДОМЛЕНИЙ ===

    /**
     * Создает канал уведомлений для Android 8.0+
     */
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

    // === ПЛАНИРОВАНИЕ УВЕДОМЛЕНИЙ ===

    /**
     * Планирует уведомление для задачи
     */
    public void scheduleTaskNotification(Task task) {
        if (!task.isNotificationEnabled() || task.isCompleted() || task.getCompletionTime() <= 0) {
            return;
        }

        long notificationTime = task.getCompletionTime() - (task.getNotificationMinutesBefore() * 60 * 1000L);
        long currentTime = System.currentTimeMillis();

        // Не планируем уведомление если время уже прошло
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

        // Планируем уведомление
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        }
    }

    /**
     * Отменяет запланированное уведомление для задачи
     */
    public void cancelTaskNotification(int taskId) {
        Intent intent = new Intent(context, TaskNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);

        // Также удаляем уведомление из панели уведомлений если оно там есть
        notificationManager.cancel(taskId);
    }

    /**
     * Обновляет уведомление для задачи (отменяет старое и создает новое)
     */
    public void updateTaskNotification(Task task) {
        cancelTaskNotification(task.getId());
        scheduleTaskNotification(task);
    }

    // === ПОКАЗ УВЕДОМЛЕНИЙ ===

    /**
     * Показывает уведомление о задаче
     */
    public void showTaskNotification(int taskId, String title, String description, long completionTime) {
        // Intent для открытия приложения при нажатии на уведомление
        Intent contentIntent = new Intent(context, MainActivity.class);
        contentIntent.putExtra(EXTRA_TASK_ID, taskId);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Создаем уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Нужно будет создать иконку
                .setContentTitle("Напоминание: " + title)
                .setContentText(getNotificationText(description, completionTime))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);

        // Добавляем расширенный текст если описание длинное
        if (description != null && description.length() > 50) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(description)
                    .setSummaryText("До выполнения: " + DateUtils.getRelativeTimeString(completionTime)));
        }

        // Добавляем действия к уведомлению
        addNotificationActions(builder, taskId);

        // Показываем уведомление
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        try {
            notificationManagerCompat.notify(taskId, builder.build());
        } catch (SecurityException e) {
            // Пользователь отключил уведомления
            e.printStackTrace();
        }
    }

    /**
     * Добавляет действия к уведомлению (например, "Выполнено", "Отложить")
     */
    private void addNotificationActions(NotificationCompat.Builder builder, int taskId) {
        // Действие "Выполнено"
        Intent completeIntent = new Intent(context, TaskNotificationReceiver.class);
        completeIntent.setAction("ACTION_COMPLETE_TASK");
        completeIntent.putExtra(EXTRA_TASK_ID, taskId);

        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 1000 + 1, // Уникальный ID
                completeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_check, "Выполнено", completePendingIntent);

        // Действие "Отложить на 15 минут"
        Intent snoozeIntent = new Intent(context, TaskNotificationReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE_TASK");
        snoozeIntent.putExtra(EXTRA_TASK_ID, taskId);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId * 1000 + 2, // Уникальный ID
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.ic_snooze, "Отложить", snoozePendingIntent);
    }

    /**
     * Формирует текст уведомления
     */
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

    // === УПРАВЛЕНИЕ ГРУППАМИ УВЕДОМЛЕНИЙ ===

    /**
     * Показывает сводное уведомление если есть несколько активных уведомлений
     */
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

    // === УТИЛИТАРНЫЕ МЕТОДЫ ===

    /**
     * Проверяет, включены ли уведомления в системе
     */
    public boolean areNotificationsEnabled() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    /**
     * Очищает все уведомления приложения
     */
    public void clearAllNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * Получает количество активных уведомлений
     */
    public int getActiveNotificationsCount() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.getActiveNotifications().length;
        }
        return 0;
    }

    /**
     * Планирует уведомления для всех задач заново (например, после перезагрузки)
     */
    public void rescheduleAllNotifications(java.util.List<Task> tasks) {
        for (Task task : tasks) {
            if (task.isNotificationEnabled() && !task.isCompleted()) {
                scheduleTaskNotification(task);
            }
        }
    }
}