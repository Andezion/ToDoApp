package com.example.todo.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.todo.R;
import com.example.todo.data.database.entities.Task;
import com.example.todo.ui.task.AddEditTaskActivity;
import com.example.todo.ui.settings.SettingsActivity;
import com.example.todo.utils.NotificationHelper;
import com.example.todo.viewmodel.TaskViewModel;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private static final int REQUEST_ADD_TASK = 1001;
    private static final int REQUEST_EDIT_TASK = 1002;

    private RecyclerView recyclerViewTasks;
    private TaskAdapter taskAdapter;
    private FloatingActionButton fabAddTask;
    private View emptyStateView;

    private TaskViewModel taskViewModel;

    private SearchView searchView;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initViewModel();
        setupRecyclerView();
        setupFab();
        observeData();

        taskViewModel.getIncompleteTaskCount().observe(this, count -> {
            updateTitle(count);
        });

        checkNotificationPermissions();

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }


    private void initViews() {
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        emptyStateView = findViewById(R.id.emptyStateView);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My tasks");
        }
    }

    private void initViewModel() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        android.util.Log.d("MainActivity", "showCompletedTasks: " + taskViewModel.getCurrentShowCompletedTasks());
        android.util.Log.d("MainActivity", "selectedCategory: " + taskViewModel.getCurrentSelectedCategory());
        android.util.Log.d("MainActivity", "searchQuery: " + taskViewModel.getCurrentSearchQuery());
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this, taskViewModel);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTasks.setAdapter(taskAdapter);

        recyclerViewTasks.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    @Override
    public void onAttachmentClick(Task task) {
        viewTaskAttachments(task);
    }

    private void viewTaskAttachments(Task task) {
        taskViewModel.getAttachmentsForTask(task.getId()).observe(this, attachments -> {
            if (attachments != null && !attachments.isEmpty()) {
                showAttachmentsList(task, attachments);
            } else {
                Toast.makeText(this, "No attachments for this task", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAttachmentsList(Task task, java.util.List<com.example.todo.data.database.entities.Attachment> attachments) {
        String[] fileNames = new String[attachments.size()];
        for (int i = 0; i < attachments.size(); i++) {
            com.example.todo.data.database.entities.Attachment attachment = attachments.get(i);
            fileNames[i] = attachment.getFileName() + " (" + formatFileSize(attachment.getFileSize()) + ")";
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Attachments: " + task.getTitle());
        builder.setItems(fileNames, (dialog, which) -> {
            com.example.todo.data.database.entities.Attachment selectedAttachment = attachments.get(which);
            openAttachment(selectedAttachment);
        });
        builder.setNegativeButton("Close", null);
        builder.show();
    }

    @SuppressLint("DefaultLocale")
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void openAttachment(com.example.todo.data.database.entities.Attachment attachment) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            java.io.File file = new java.io.File(attachment.getFilePath());

            if (!file.exists()) {
                Toast.makeText(this, "File not found: " + attachment.getFileName(), Toast.LENGTH_SHORT).show();
                return;
            }

            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            intent.setDataAndType(fileUri, getMimeType(attachment.getFileName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "txt": return "text/plain";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            default: return "*/*";
        }
    }


    private void setupFab() {
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
        });
    }


    private void observeData() {
        android.util.Log.d("MainActivity", "=== observeData() called ===");

        taskViewModel.getAllTasks().observe(this, tasks -> {
            android.util.Log.d("MainActivity", "AllTasks received: " + (tasks != null ? tasks.size() : "null"));
            if (tasks != null) {
                for (int i = 0; i < tasks.size(); i++) {
                    android.util.Log.d("MainActivity", "Task " + i + ": " + tasks.get(i).getTitle());
                }
            }
        });

        taskViewModel.getFilteredTasks().observe(this, tasks -> {
            android.util.Log.d("MainActivity", "FilteredTasks received: " + (tasks != null ? tasks.size() : "null"));
            if (currentSearchQuery.isEmpty()) {
                android.util.Log.d("MainActivity", "Updating adapter with FilteredTasks");
                taskAdapter.submitList(tasks);
                updateEmptyState(tasks == null || tasks.isEmpty());
            }
        });

        taskViewModel.getSearchResults().observe(this, tasks -> {
            android.util.Log.d("MainActivity", "SearchResults received: " + (tasks != null ? tasks.size() : "null"));
            if (!currentSearchQuery.isEmpty()) {
                android.util.Log.d("MainActivity", "Updating adapter with SearchResults");
                taskAdapter.submitList(tasks);
                updateEmptyState(tasks == null || tasks.isEmpty());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.length() != 1) {
                        performSearch(newText);
                    }
                    return true;
                }
            });

            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                    performSearch("");
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (itemId == R.id.action_sort) {
            showSortDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void performSearch(String query) {
        currentSearchQuery = query.trim();
        taskViewModel.setSearchQuery(currentSearchQuery);


        //observeData();
    }

    private void showFilterDialog() {
        // TODO: Реализовать диалог фильтрации
        boolean currentShowCompleted = taskViewModel.getCurrentShowCompletedTasks();
        taskViewModel.setShowCompletedTasks(!currentShowCompleted);

        String message = currentShowCompleted ? "Completed tasks hidden" : "All tasks shown";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSortDialog() {
        // TODO: Реализовать диалог сортировки
        Toast.makeText(this, "Tasks sorted by completion time", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(this, AddEditTaskActivity.class);
        intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.getId());
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }

    @Override
    public void onTaskLongClick(Task task) {
        showTaskContextMenu(task);
    }

    @Override
    public void onTaskCheckboxClick(Task task) {
        taskViewModel.toggleTaskCompletion(task);

        String message = task.isCompleted() ?
                "Task \"" + task.getTitle() + "\" completed!" :
                "Task \"" + task.getTitle() + "\" not completed";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }


    private void showTaskContextMenu(Task task) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(task.getTitle());

        String[] options = {
                "Edit",
                "Duplicate",
                task.isCompleted() ? "Mark as not completed" : "Execute",
                "Delete"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    onTaskClick(task);
                    break;
                case 1:
                    duplicateTask(task);
                    break;
                case 2:
                    onTaskCheckboxClick(task);
                    break;
                case 3:
                    showDeleteConfirmation(task);
                    break;
            }
        });

        builder.show();
    }

    private void duplicateTask(Task task) {
        Task newTask = new Task();
        newTask.setTitle(task.getTitle() + " (copy)");
        newTask.setDescription(task.getDescription());
        newTask.setCategory(task.getCategory());
        newTask.setNotificationEnabled(task.isNotificationEnabled());
        newTask.setNotificationMinutesBefore(task.getNotificationMinutesBefore());

        newTask.setCompletionTime(task.getCompletionTime() + (24 * 60 * 60 * 1000L));

        taskViewModel.insertTask(newTask);
        Toast.makeText(this, "Task duplicated", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmation(Task task) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Delete task");
        builder.setMessage("Are you sure you want to delete the task? \"" + task.getTitle() + "\"?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            taskViewModel.deleteTask(task);
            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancellation", null);
        builder.show();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            recyclerViewTasks.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            recyclerViewTasks.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void updateTitle(Integer count) {
        if (getSupportActionBar() != null) {
            if (count != null && count > 0) {
                getSupportActionBar().setTitle("My tasks (" + count + ")");
            } else {
                getSupportActionBar().setTitle("My tasks");
            }
        }
    }

    private void checkNotificationPermissions() {
        NotificationHelper notificationHelper = new NotificationHelper(this);
        if (!notificationHelper.areNotificationsEnabled()) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Permission for notifications");
            builder.setMessage("For task reminders, it is recommended to enable notifications in the application settings.");
            builder.setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
            builder.setNegativeButton("Later", null);
            builder.show();
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra(NotificationHelper.EXTRA_TASK_ID)) {
            int taskId = intent.getIntExtra(NotificationHelper.EXTRA_TASK_ID, -1);
            if (taskId != -1) {
                Intent editIntent = new Intent(this, AddEditTaskActivity.class);
                editIntent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, taskId);
                startActivityForResult(editIntent, REQUEST_EDIT_TASK);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        android.util.Log.d("MainActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == RESULT_OK) {
            String message = "";
            if (requestCode == REQUEST_ADD_TASK) {
                message = "Task created";
            } else if (requestCode == REQUEST_EDIT_TASK) {
                message = "Task updated";
            }

            if (!message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}