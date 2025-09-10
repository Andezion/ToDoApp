package com.example.todo.ui.task;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.todo.R;
import com.example.todo.data.database.entities.Task;
import com.example.todo.utils.DateUtils;
import com.example.todo.viewmodel.TaskViewModel;
import com.example.todo.viewmodel.SettingsViewModel;

import java.util.Calendar;
import java.util.Objects;

import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.example.todo.utils.FileUtils;
import com.example.todo.data.database.entities.Attachment;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class AddEditTaskActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private AutoCompleteTextView etCategory;
    private MaterialButton btnSetDate;
    private MaterialButton btnSetTime;
    private SwitchMaterial switchNotification;
    private ChipGroup chipGroupNotificationTime;
    private TextInputLayout tilTitle;
    private TextInputLayout tilCategory;
    private TaskViewModel taskViewModel;

    private Task currentTask;
    private boolean isEditMode = false;
    private Calendar selectedDateTime;
    private int selectedNotificationMinutes = 15;

    private List<String> temporaryAttachments = new ArrayList<>();
    private List<Attachment> currentAttachments = new ArrayList<>();

    private final String[] categories = {
            "General", "Work", "Personal", "Education",
            "Shopping", "Health", "Finance", "Hobbies", "Travel"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_task);

        initViews();
        initViewModels();
        setupCategoryDropdown();
        setupDateTime();
        setupNotificationOptions();

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_TASK_ID)) {
            int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
            if (taskId != -1) {
                isEditMode = true;
                loadTask(taskId);
            }
        }

        updateUI();
        setupActionBar();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etCategory = findViewById(R.id.etCategory);
        btnSetDate = findViewById(R.id.btnSetDate);
        btnSetTime = findViewById(R.id.btnSetTime);
        MaterialButton btnAttachments = findViewById(R.id.btnAttachments);
        switchNotification = findViewById(R.id.switchNotification);
        chipGroupNotificationTime = findViewById(R.id.chipGroupNotificationTime);
        tilTitle = findViewById(R.id.tilTitle);
        TextInputLayout tilDescription = findViewById(R.id.tilDescription);
        tilCategory = findViewById(R.id.tilCategory);

        temporaryAttachments = new ArrayList<>();
        currentAttachments = new ArrayList<>();

        btnSetDate.setOnClickListener(v -> showDatePicker());
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnAttachments.setOnClickListener(v -> showAttachmentsDialog());

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chipGroupNotificationTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void initViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        SettingsViewModel settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        settingsViewModel.getDefaultCategory().observe(this, defaultCategory -> {
            if (defaultCategory != null && !isEditMode) {
                etCategory.setText(defaultCategory);
            }
        });
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Edit task" : "New task");
        }
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);

        etCategory.setThreshold(0);

        etCategory.setOnClickListener(v -> {
            etCategory.showDropDown();
        });

        etCategory.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etCategory.showDropDown();
            }
        });

        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            etCategory.clearFocus();
        });
    }

    private void setupDateTime() {
        selectedDateTime = Calendar.getInstance();
        selectedDateTime.add(Calendar.DAY_OF_MONTH, 1);
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
        selectedDateTime.set(Calendar.MINUTE, 0);
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        updateDateTimeButtons();
    }

    private void setupNotificationOptions() {
        chipGroupNotificationTime.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip5min) {
                    selectedNotificationMinutes = 5;
                } else if (checkedId == R.id.chip15min) {
                    selectedNotificationMinutes = 15;
                } else if (checkedId == R.id.chip30min) {
                    selectedNotificationMinutes = 30;
                } else if (checkedId == R.id.chip1hour) {
                    selectedNotificationMinutes = 60;
                } else if (checkedId == R.id.chip1day) {
                    selectedNotificationMinutes = 24 * 60;
                }
            }
        });

        chipGroupNotificationTime.check(R.id.chip15min);
    }

    private void loadTask(int taskId) {
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task != null) {
                currentTask = task;
                populateFields();
            }
        });
    }

    private void populateFields() {
        if (currentTask == null) return;

        etTitle.setText(currentTask.getTitle());
        etDescription.setText(currentTask.getDescription());
        etCategory.setText(currentTask.getCategory());

        if (currentTask.getCompletionTime() > 0) {
            selectedDateTime.setTimeInMillis(currentTask.getCompletionTime());
            updateDateTimeButtons();
        }

        switchNotification.setChecked(currentTask.isNotificationEnabled());
        selectedNotificationMinutes = currentTask.getNotificationMinutesBefore();
        selectNotificationChip();

        chipGroupNotificationTime.setVisibility(
                currentTask.isNotificationEnabled() ? View.VISIBLE : View.GONE);

        taskViewModel.getAttachmentsByTaskId(currentTask.getId()).observe(this, attachments -> {
            if (attachments != null) {
                currentAttachments.clear();
                currentAttachments.addAll(attachments);
            }
        });
    }

    private void selectNotificationChip() {
        int chipId;
        switch (selectedNotificationMinutes) {
            case 5:
                chipId = R.id.chip5min;
                break;
            case 15:
                chipId = R.id.chip15min;
                break;
            case 30:
                chipId = R.id.chip30min;
                break;
            case 60:
                chipId = R.id.chip1hour;
                break;
            case 24 * 60:
                chipId = R.id.chip1day;
                break;
            default:
                chipId = R.id.chip15min;
                break;
        }
        chipGroupNotificationTime.check(chipId);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateTimeButtons();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    updateDateTimeButtons();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                true
        );

        timePickerDialog.show();
    }

    private void showAttachmentsDialog() {
        String[] options = {"Camera", "Gallery", "Files", "View attachments"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attachments");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    openFilePicker();
                    break;
                case 3:
                    viewAttachments();
                    break;
            }
        });

        builder.show();
    }

    private void openCamera() {
        // TODO: Реализовать открытие камеры
        Toast.makeText(this, "The camera feature will be added later", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        // TODO: Реализовать выбор из галереи
        Toast.makeText(this, "The gallery feature will be added later", Toast.LENGTH_SHORT).show();
    }



    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите файл"), 2001);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2001 && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                handleSelectedFile(fileUri);
            }
        }
    }

    private void handleSelectedFile(Uri fileUri) {
        try {
            String fileName = getFileNameFromUri(fileUri);
            String uniqueFileName = FileUtils.generateUniqueFileName(fileName);
            File copiedFile = FileUtils.copyFileToAppDirectory(this, fileUri, uniqueFileName);

            long fileSize = copiedFile.length();
            String fileType = FileUtils.getFileType(fileName);

            if (!FileUtils.isFileSizeValid(fileSize, fileType)) {
                copiedFile.delete();
                Toast.makeText(this, "Файл слишком большой", Toast.LENGTH_SHORT).show();
                return;
            }

            if (FileUtils.isImageFile(fileName)) {
                copiedFile = FileUtils.compressImageIfNeeded(copiedFile);
                fileSize = copiedFile.length(); // Обновляем размер после сжатия
            }

            // Сохраняем информацию о файле
            if (isEditMode && currentTask != null) {
                // Для редактируемой задачи создаем attachment сразу
                Attachment attachment = new Attachment();
                attachment.setTaskId(currentTask.getId());
                attachment.setFileName(fileName);
                attachment.setFilePath(copiedFile.getAbsolutePath());
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);

                currentAttachments.add(attachment);

                // Сохраняем в базу сразу
                taskViewModel.insertAttachment(attachment);
            } else {
                // Для новой задачи сохраняем временно
                String attachmentData = copiedFile.getAbsolutePath() + "|" + fileName + "|" + fileSize + "|" + fileType;
                temporaryAttachments.add(attachmentData);
            }

            Toast.makeText(this, "Файл прикреплен: " + fileName, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при прикреплении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "unknown_file";
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } else {
            fileName = uri.getLastPathSegment();
        }
        return fileName != null ? fileName : "unknown_file";
    }

    private void showAttachmentsList(List<Attachment> attachments) {
        String[] fileNames = new String[attachments.size()];
        for (int i = 0; i < attachments.size(); i++) {
            fileNames[i] = attachments.get(i).getFileName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Вложения (" + attachments.size() + ")");
        builder.setItems(fileNames, (dialog, which) -> {
            // Можно добавить действия при клике на вложение (открыть, удалить и т.д.)
            Attachment selectedAttachment = attachments.get(which);
            Toast.makeText(this, "Файл: " + selectedAttachment.getFileName(), Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void showTemporaryAttachmentsList() {
        String[] fileNames = new String[temporaryAttachments.size()];
        for (int i = 0; i < temporaryAttachments.size(); i++) {
            String[] parts = temporaryAttachments.get(i).split("\\|");
            if (parts.length >= 2) {
                fileNames[i] = parts[1]; // Имя файла
            } else {
                fileNames[i] = "Неизвестный файл";
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Временные вложения (" + temporaryAttachments.size() + ")");
        builder.setItems(fileNames, (dialog, which) -> {
            Toast.makeText(this, "Файл: " + fileNames[which], Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void viewAttachments() {
        if (isEditMode && currentTask != null) {
            taskViewModel.getAttachmentsForTask(currentTask.getId()).observe(this, attachments -> {
                if (attachments != null && !attachments.isEmpty()) {
                    showAttachmentsList(attachments);
                } else {
                    Toast.makeText(this, "Нет вложений", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (!temporaryAttachments.isEmpty()) {
                showTemporaryAttachmentsList();
            } else {
                Toast.makeText(this, "Нет вложений", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateDateTimeButtons() {
        btnSetDate.setText(DateUtils.formatShortDate(selectedDateTime.getTimeInMillis()));
        btnSetTime.setText(DateUtils.formatTime(selectedDateTime.getTimeInMillis()));
    }

    private void updateUI() {

    }

    private boolean validateInput() {
        boolean isValid = true;

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tilTitle.setError("Enter the name of the task");
            isValid = false;
        } else {
            tilTitle.setError(null);
        }

        String category = etCategory.getText().toString().trim();
        if (category.isEmpty()) {
            tilCategory.setError("Select a category");
            isValid = false;
        } else {
            tilCategory.setError(null);
        }

        return isValid;
    }

    private void saveTask() {
        if (!validateInput()) {
            return;
        }

        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String description = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String category = etCategory.getText().toString().trim();
        boolean notificationEnabled = switchNotification.isChecked();

        if (isEditMode && currentTask != null) {
            currentTask.setTitle(title);
            currentTask.setDescription(description);
            currentTask.setCategory(category);
            currentTask.setCompletionTime(selectedDateTime.getTimeInMillis());
            currentTask.setNotificationEnabled(notificationEnabled);
            currentTask.setNotificationMinutesBefore(selectedNotificationMinutes);

            taskViewModel.updateTask(currentTask);

            // Новые вложения уже были сохранены в handleSelectedFile

        } else {
            Task newTask = new Task();
            newTask.setTitle(title);
            newTask.setDescription(description);
            newTask.setCategory(category);
            newTask.setCompletionTime(selectedDateTime.getTimeInMillis());
            newTask.setNotificationEnabled(notificationEnabled);
            newTask.setNotificationMinutesBefore(selectedNotificationMinutes);

            // Вставляем задачу вместе с вложениями
            taskViewModel.insertTaskWithAttachments(newTask, temporaryAttachments);
        }

        setResult(RESULT_OK);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_task, menu);

        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        if (deleteItem != null) {
            deleteItem.setVisible(isEditMode);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_save) {
            saveTask();
            return true;
        } else if (itemId == R.id.action_delete) {
            showDeleteConfirmation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmation() {
        if (!isEditMode || currentTask == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete task");
        builder.setMessage("Are you sure you want to delete this task?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            taskViewModel.deleteTask(currentTask);
            setResult(RESULT_OK);
            finish();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Unsaved changes");
            builder.setMessage("You have unsaved changes. Save the task?");

            builder.setPositiveButton("Save", (dialog, which) -> saveTask());
            builder.setNegativeButton("Don't save", (dialog, which) -> super.onBackPressed());
            builder.setNeutralButton("Cancel", null);

            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        String currentTitle = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String currentDescription = Objects.requireNonNull(etDescription.getText()).toString().trim();
        String currentCategory = etCategory.getText().toString().trim();

        if (isEditMode && currentTask != null) {
            return !currentTitle.equals(currentTask.getTitle()) ||
                    !currentDescription.equals(currentTask.getDescription()) ||
                    !currentCategory.equals(currentTask.getCategory()) ||
                    selectedDateTime.getTimeInMillis() != currentTask.getCompletionTime() ||
                    switchNotification.isChecked() != currentTask.isNotificationEnabled() ||
                    selectedNotificationMinutes != currentTask.getNotificationMinutesBefore();
        } else {
            return !currentTitle.isEmpty() || !currentDescription.isEmpty() || !currentCategory.isEmpty();
        }
    }
}