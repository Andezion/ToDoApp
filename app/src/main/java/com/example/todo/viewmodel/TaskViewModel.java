package com.example.todo.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.todo.data.database.entities.Attachment;
import com.example.todo.data.database.entities.Task;
import com.example.todo.data.repository.TaskRepository;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;

    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Task>> incompleteTasks;
    private final LiveData<List<Task>> completeTasks;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showCompletedTasks = new MutableLiveData<>(true);

    private final LiveData<List<Task>> searchResults;
    private final LiveData<List<Task>> filteredTasks;

    private final LiveData<List<String>> allCategories;
    private final LiveData<Integer> incompleteTaskCount;

    private final MediatorLiveData<List<Task>> combinedTasks = new MediatorLiveData<>();

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);

        allTasks = repository.getAllTasks();
        incompleteTasks = repository.getIncompleteTasks();
        completeTasks = repository.getCompleteTasks();
        allCategories = repository.getAllCategories();
        incompleteTaskCount = repository.getIncompleteTaskCount();

        searchResults = Transformations.switchMap(searchQuery, query -> {
            if (query == null || query.trim().isEmpty()) {
                return allTasks;
            } else {
                return repository.searchTasks(query.trim());
            }
        });

        filteredTasks = Transformations.switchMap(showCompletedTasks, showCompleted ->
                Transformations.switchMap(selectedCategory, category ->
                        repository.getFilteredTasks(showCompleted, category)
                )
        );

        showCompletedTasks.setValue(true);
        selectedCategory.setValue(null);
    }

    public LiveData<List<Task>> getAllTasks() {
        return allTasks;
    }

    public LiveData<List<Task>> getIncompleteTasks() {
        return incompleteTasks;
    }

    public LiveData<List<Task>> getCompleteTasks() {
        return completeTasks;
    }

    public LiveData<List<Task>> getSearchResults() {
        return searchResults;
    }

    public LiveData<List<Task>> getFilteredTasks() {
        return filteredTasks;
    }

    public LiveData<List<String>> getAllCategories() {
        return allCategories;
    }

    public LiveData<Integer> getIncompleteTaskCount() {
        return incompleteTaskCount;
    }

    public LiveData<Task> getTaskById(int taskId) {
        return repository.getTaskById(taskId);
    }

    public LiveData<List<Task>> getTasksByCategory(String category) {
        return repository.getTasksByCategory(category);
    }


    public void insertTask(Task task) {
        repository.insert(task, insertedTask -> {

        });
    }

    public void updateTask(Task task) {
        repository.update(task);
    }

    public void deleteTask(Task task) {
        repository.delete(task);
    }

    public void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        repository.update(task);
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
    }

    public void setSelectedCategory(String category) {
        selectedCategory.setValue(category);
    }

    public LiveData<List<Attachment>> getAttachmentsByTaskId(int taskId) {
        return repository.getAttachmentsForTask(taskId);
    }

    public void insertTaskWithAttachments(Task task, List<String> attachmentPaths) {
        repository.insert(task, insertedTask -> {
            if (insertedTask != null && attachmentPaths != null && !attachmentPaths.isEmpty()) {
                for (String attachmentData : attachmentPaths) {
                    String[] parts = attachmentData.split("\\|");
                    if (parts.length >= 4) {
                        Attachment attachment = new Attachment();
                        attachment.setTaskId(insertedTask.getId());
                        attachment.setFilePath(parts[0]);
                        attachment.setFileName(parts[1]);
                        attachment.setFileSize(Long.parseLong(parts[2]));
                        attachment.setFileType(parts[3]);

                        insertAttachment(attachment);
                    }
                }
            }
        });
    }

    public void setShowCompletedTasks(boolean showCompleted) {
        showCompletedTasks.setValue(showCompleted);
    }

    public void clearSearch() {
        searchQuery.setValue("");
    }

    public void clearFilter() {
        selectedCategory.setValue(null);
        showCompletedTasks.setValue(true);
    }


    public String getCurrentSearchQuery() {
        return searchQuery.getValue();
    }

    public String getCurrentSelectedCategory() {
        return selectedCategory.getValue();
    }

    public Boolean getCurrentShowCompletedTasks() {
        return showCompletedTasks.getValue();
    }


    public LiveData<List<Attachment>> getAttachmentsForTask(int taskId) {
        return repository.getAttachmentsForTask(taskId);
    }

    public LiveData<Integer> getAttachmentCountForTask(int taskId) {
        return repository.getAttachmentCountForTask(taskId);
    }

    public void insertAttachment(Attachment attachment) {
        repository.insertAttachment(attachment, insertedAttachment -> {

        });
    }

    public void deleteAttachment(Attachment attachment) {
        repository.deleteAttachment(attachment);
    }


    public void getTasksForNotification(long currentTime, TaskRepository.OnTasksForNotificationListener listener) {
        repository.getTasksForNotification(currentTime, listener);
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        repository.cleanup();
    }
}