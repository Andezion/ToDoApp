package com.example.todo.ui.main;

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
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(this);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTasks.setAdapter(taskAdapter);

        recyclerViewTasks.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());
    }

    private void setupFab() {
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditTaskActivity.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
        });
    }


    private void observeData() {
        if (currentSearchQuery.isEmpty()) {
            taskViewModel.getFilteredTasks().observe(this, tasks -> {
                taskAdapter.submitList(tasks);
                updateEmptyState(tasks == null || tasks.isEmpty());
            });
        } else {
            taskViewModel.getSearchResults().observe(this, tasks -> {
                taskAdapter.submitList(tasks);
                updateEmptyState(tasks == null || tasks.isEmpty());
            });
        }

        taskViewModel.getIncompleteTaskCount().observe(this, count -> {
            updateTitle(count);
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


        observeData();
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