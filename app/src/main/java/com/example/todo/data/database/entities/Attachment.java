package com.example.todo.data.database.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "attachments",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Attachment
{
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int taskId;
    private String fileName;
    private String filePath;
    private String fileType;
    private long fileSize;
    private long createdTime;

    public Attachment()
    {
        this.createdTime = System.currentTimeMillis();
    }

    public Attachment(int taskId, String fileName, String filePath, String fileType, long fileSize) {
        this();
        this.taskId = taskId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

    public int getId()
    {
        return id;
    }

    public int getTaskId()
    {
        return taskId;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public String getFileType()
    {
        return fileType;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public long getCreatedTime()
    {
        return createdTime;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public void setTaskId(int taskId)
    {
        this.taskId = taskId;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public void setFileSize(long fileSize)
    {
        this.fileSize = fileSize;
    }

    public void setCreatedTime(long createdTime)
    {
        this.createdTime = createdTime;
    }
}