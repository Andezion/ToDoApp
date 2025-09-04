package com.example.todo.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.todo.data.database.entities.Attachment;

import java.util.List;

@Dao
public interface AttachmentDao {

    @Insert
    long insert(Attachment attachment);

    @Update
    void update(Attachment attachment);

    @Delete
    void delete(Attachment attachment);

    @Query("DELETE FROM attachments WHERE id = :attachmentId")
    void deleteById(int attachmentId);

    @Query("SELECT * FROM attachments WHERE taskId = :taskId ORDER BY createdTime ASC")
    LiveData<List<Attachment>> getAttachmentsForTask(int taskId);

    @Query("SELECT * FROM attachments WHERE id = :attachmentId")
    LiveData<Attachment> getAttachmentById(int attachmentId);

    @Query("DELETE FROM attachments WHERE taskId = :taskId")
    void deleteAttachmentsForTask(int taskId);

    @Query("SELECT COUNT(*) FROM attachments WHERE taskId = :taskId")
    LiveData<Integer> getAttachmentCountForTask(int taskId);

    @Query("SELECT * FROM attachments WHERE taskId = :taskId AND fileType = :fileType ORDER BY createdTime ASC")
    LiveData<List<Attachment>> getAttachmentsByType(int taskId, String fileType);

    @Query("SELECT filePath FROM attachments WHERE taskId = :taskId")
    List<String> getFilePathsForTask(int taskId);
}