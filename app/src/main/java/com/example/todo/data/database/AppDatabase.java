package com.example.todo.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.todo.data.database.dao.AttachmentDao;
import com.example.todo.data.database.dao.TaskDao;
import com.example.todo.data.database.entities.Attachment;
import com.example.todo.data.database.entities.Task;

@Database(
        entities = {Task.class, Attachment.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase
{
    public abstract TaskDao taskDao();
    public abstract AttachmentDao attachmentDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "todo_database";

    public static AppDatabase getInstance(Context context)
    {
        if (INSTANCE == null)
        {
            synchronized (AppDatabase.class)
            {
                if (INSTANCE == null)
                {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            ).build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}