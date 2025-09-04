package com.example.todo.data.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks")
public class Task
{
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String description;
    private long creationTime;
    private long completionTime;
    private boolean isCompleted;
    private boolean notificationEnabled;
    private String category;
    private boolean hasAttachments;
    private int notificationMinutesBefore;

    public Task()
    {
        this.creationTime = System.currentTimeMillis();
        this.isCompleted = false;
        this.notificationEnabled = true;
        this.hasAttachments = false;
        this.notificationMinutesBefore = 15;
        this.category = "Общие";
    }

    public int getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public long getCompletionTime()
    {
        return completionTime;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }

    public boolean isNotificationEnabled()
    {
        return notificationEnabled;
    }

    public String getCategory()
    {
        return category;
    }

    public boolean isHasAttachments()
    {
        return hasAttachments;
    }

    public int getNotificationMinutesBefore()
    {
        return notificationMinutesBefore;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setCreationTime(long creationTime)
    {
        this.creationTime = creationTime;
    }

    public void setCompletionTime(long completionTime)
    {
        this.completionTime = completionTime;
    }

    public void setCompleted(boolean completed)
    {
        isCompleted = completed;
    }

    public void setNotificationEnabled(boolean notificationEnabled)
    {
        this.notificationEnabled = notificationEnabled;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public void setHasAttachments(boolean hasAttachments)
    {
        this.hasAttachments = hasAttachments;
    }

    public void setNotificationMinutesBefore(int notificationMinutesBefore)
    {
        this.notificationMinutesBefore = notificationMinutesBefore;
    }
}