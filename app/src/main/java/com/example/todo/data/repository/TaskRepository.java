package com.example.todo.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.todo.data.database.AppDatabase;
import com.example.todo.data.database.dao.AttachmentDao;
import com.example.todo.data.database.dao.TaskDao;
import com.example.todo.data.database.entities.Attachment;
import com.example.todo.data.database.entities.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository
{
    private final TaskDao taskDao;
    private final AttachmentDao attachmentDao;
    private final LiveData<List<Task>> allTasks;
    private final ExecutorService executorService;

    public TaskRepository(Application application)
    {
        AppDatabase database = AppDatabase.getInstance(application);
        taskDao = database.taskDao();
        attachmentDao = database.attachmentDao();
        allTasks = taskDao.getAllTasksSortedByDueTime();

        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<Task>> getAllTasks()
    {
        return allTasks;
    }

    public LiveData<List<Task>> getIncompleteTasks()
    {
        return taskDao.getIncompleteTasks();
    }
    public LiveData<List<Task>> getCompleteTasks()
    {
        return taskDao.getCompleteTasks();
    }

    public LiveData<Task> getTaskById(int taskId)
    {
        return taskDao.getTaskById(taskId);
    }

    public LiveData<List<Task>> searchTasks(String query)
    {
        return taskDao.searchTasks("%" + query + "%");
    }

    public LiveData<List<Task>> getTasksByCategory(String category)
    {
        return taskDao.getTasksByCategory(category);
    }

    public LiveData<List<String>> getAllCategories()
    {
        return taskDao.getAllCategories();
    }

    public LiveData<List<Task>> getFilteredTasks(boolean showCompleted, String category)
    {
        return taskDao.getFilteredTasks(showCompleted, category);
    }

    public LiveData<Integer> getIncompleteTaskCount()
    {
        return taskDao.getIncompleteTaskCount();
    }

    public void insert(Task task, OnTaskInsertedListener listener)
    {
        executorService.execute(() ->
        {
            long taskId = taskDao.insert(task);
            task.setId((int) taskId);
            if (listener != null) {
                listener.onTaskInserted(task);
            }
        });
    }

    public void update(Task task)
    {
        executorService.execute(() -> taskDao.update(task));
    }

    public void delete(Task task)
    {
        executorService.execute(() ->
        {
            List<String> filePaths = attachmentDao.getFilePathsForTask(task.getId());
            // TODO: Здесь нужно будет добавить удаление файлов из файловой системы

            taskDao.delete(task);
        });
    }

    public LiveData<List<Attachment>> getAttachmentsForTask(int taskId)
    {
        return attachmentDao.getAttachmentsForTask(taskId);
    }

    public LiveData<Integer> getAttachmentCountForTask(int taskId)
    {
        return attachmentDao.getAttachmentCountForTask(taskId);
    }

    public void insertAttachment(Attachment attachment, OnAttachmentInsertedListener listener)
    {
        executorService.execute(() ->
        {
            long attachmentId = attachmentDao.insert(attachment);
            attachment.setId((int) attachmentId);

            updateTaskAttachmentFlag(attachment.getTaskId());

            if (listener != null)
            {
                listener.onAttachmentInserted(attachment);
            }
        });
    }

    public void deleteAttachment(Attachment attachment)
    {
        executorService.execute(() -> {
            attachmentDao.delete(attachment);
            // TODO: Удалить файл из файловой системы

            updateTaskAttachmentFlag(attachment.getTaskId());
        });
    }

    private void updateTaskAttachmentFlag(int taskId)
    {
        executorService.execute(() -> {

            LiveData<Integer> countLiveData = attachmentDao.getAttachmentCountForTask(taskId);
        });
    }

    public void getTasksForNotification(long currentTime, OnTasksForNotificationListener listener)
    {
        executorService.execute(() ->
        {
            List<Task> tasks = taskDao.getTasksForNotification(currentTime);
            if (listener != null) {
                listener.onTasksForNotificationLoaded(tasks);
            }
        });
    }

    public interface OnTaskInsertedListener
    {
        void onTaskInserted(Task task);
    }

    public interface OnAttachmentInsertedListener
    {
        void onAttachmentInserted(Attachment attachment);
    }

    public interface OnTasksForNotificationListener
    {
        void onTasksForNotificationLoaded(List<Task> tasks);
    }

    public void cleanup()
    {
        if (executorService != null && !executorService.isShutdown())
        {
            executorService.shutdown();
        }
    }
}