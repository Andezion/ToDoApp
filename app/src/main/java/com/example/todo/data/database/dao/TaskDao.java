package com.example.todo.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todo.data.database.entities.Task;

import java.util.List;

@Dao
public interface TaskDao
{
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteById(int taskId);

    @Query("SELECT * FROM tasks ORDER BY completionTime ASC")
    LiveData<List<Task>> getAllTasksSortedByDueTime();

    @Query("SELECT * FROM tasks ORDER BY creationTime DESC")
    LiveData<List<Task>> getAllTasksSortedByCreationTime();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskById(int taskId);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY completionTime ASC")
    LiveData<List<Task>> getIncompleteTasks();

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completionTime ASC")
    LiveData<List<Task>> getCompleteTasks();

    @Query("SELECT * FROM tasks WHERE title LIKE :searchQuery OR description LIKE :searchQuery ORDER BY completionTime ASC")
    LiveData<List<Task>> searchTasks(String searchQuery);

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY completionTime ASC")
    LiveData<List<Task>> getTasksByCategory(String category);

    @Query("SELECT * FROM tasks WHERE notificationEnabled = 1 AND isCompleted = 0 AND completionTime > :currentTime")
    List<Task> getTasksForNotification(long currentTime);

    @Query("SELECT DISTINCT category FROM tasks ORDER BY category ASC")
    LiveData<List<String>> getAllCategories();

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    LiveData<Integer> getIncompleteTaskCount();

    @Query("SELECT * FROM tasks WHERE " +
            "(:showCompleted = 1 OR isCompleted = 0) AND " +
            "(:category IS NULL OR category = :category) " +
            "ORDER BY completionTime ASC")
    LiveData<List<Task>> getFilteredTasks(boolean showCompleted, String category);
}