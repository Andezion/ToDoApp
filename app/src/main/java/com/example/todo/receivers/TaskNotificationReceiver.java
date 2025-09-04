package com.example.todo.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.todo.data.database.AppDatabase;
import com.example.todo.data.database.entities.Task;
import com.example.todo.utils.NotificationHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);

        if (taskId == -1) return;

        if (action == null)
        {
            showTaskNotification(context, intent);
        }
        else
        {

            switch (action)
            {
                case "ACTION_COMPLETE_TASK":
                    completeTask(context, taskId);
                    break;
                case "ACTION_SNOOZE_TASK":
                    snoozeTask(context, taskId);
                    break;
                case Intent.ACTION_BOOT_COMPLETED:
                    rescheduleNotifications(context);
                    break;
            }
        }
    }

    private void showTaskNotification(Context context, Intent intent) {
        int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);
        String title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE);
        String description = intent.getStringExtra(NotificationHelper.EXTRA_TASK_DESCRIPTION);

        if (taskId == -1 || title == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            Task task = getTaskSync(database, taskId);

            if (task != null && !task.isCompleted() && task.isNotificationEnabled()) {
                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.showTaskNotification(
                        taskId,
                        title,
                        description,
                        task.getCompletionTime()
                );
            }
        });
    }

    private void completeTask(Context context, int taskId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            Task task = getTaskSync(database, taskId);

            if (task != null && !task.isCompleted()) {
                task.setCompleted(true);
                database.taskDao().update(task);

                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.cancelTaskNotification(taskId);

                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, "Задача \"" + task.getTitle() + "\" выполнена!",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void snoozeTask(Context context, int taskId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase database = AppDatabase.getInstance(context);
            Task task = getTaskSync(database, taskId);

            if (task != null && !task.isCompleted()) {
                long newCompletionTime = task.getCompletionTime() + (15 * 60 * 1000L);
                task.setCompletionTime(newCompletionTime);
                database.taskDao().update(task);

                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.cancelTaskNotification(taskId);
                notificationHelper.scheduleTaskNotification(task);

                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    Toast.makeText(context, "Задача отложена на 15 минут",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}