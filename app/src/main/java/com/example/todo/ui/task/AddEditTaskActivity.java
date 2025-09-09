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

/**
 * Активность для создания и редактирования задач
 * Путь: app/src/main/java/com/yourpackage/todoapp/ui/task/AddEditTaskActivity.java
 */
public class AddEditTaskActivity extends AppCompatActivity {

    // Константы
    public static final String EXTRA_TASK_ID = "task_id";
    private static final int REQUEST_ATTACHMENTS = 2001;

    // UI компоненты
    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private AutoCompleteTextView etCategory;
    private MaterialButton btnSetDate;
    private MaterialButton btnSetTime;
    private MaterialButton btnAttachments;
    private SwitchMaterial switchNotification;
    private ChipGroup chipGroupNotificationTime;
    private TextInputLayout tilTitle;
    private TextInputLayout tilDescription;
    private TextInputLayout tilCategory;

    // ViewModels
    private TaskViewModel taskViewModel;
    private SettingsViewModel settingsViewModel;

    // Данные
    private Task currentTask;
    private boolean isEditMode = false;
    private Calendar selectedDateTime;
    private int selectedNotificationMinutes = 15;

    // Список категорий
    private String[] categories = {
            "Общие", "Работа", "Личное", "Учеба",
            "Покупки", "Здоровье", "Финансы", "Хобби", "Путешествия"
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

        // Проверяем, редактируем ли существующую задачу
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

    // === ИНИЦИАЛИЗАЦИЯ ===

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etCategory = findViewById(R.id.etCategory);
        btnSetDate = findViewById(R.id.btnSetDate);
        btnSetTime = findViewById(R.id.btnSetTime);
        btnAttachments = findViewById(R.id.btnAttachments);
        switchNotification = findViewById(R.id.switchNotification);
        chipGroupNotificationTime = findViewById(R.id.chipGroupNotificationTime);
        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        tilCategory = findViewById(R.id.tilCategory);

        // Обработчики кликов
        btnSetDate.setOnClickListener(v -> showDatePicker());
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnAttachments.setOnClickListener(v -> showAttachmentsDialog());

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chipGroupNotificationTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void initViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Устанавливаем категорию по умолчанию из настроек
        settingsViewModel.getDefaultCategory().observe(this, defaultCategory -> {
            if (defaultCategory != null && !isEditMode) {
                etCategory.setText(defaultCategory);
            }
        });
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isEditMode ? "Редактировать задачу" : "Новая задача");
        }
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);
        etCategory.setOnItemClickListener((parent, view, position, id) -> {
            // Скрываем клавиатуру при выборе из списка
            etCategory.clearFocus();
        });
    }

    private void setupDateTime() {
        selectedDateTime = Calendar.getInstance();
        // По умолчанию устанавливаем на завтра в 9:00
        selectedDateTime.add(Calendar.DAY_OF_MONTH, 1);
        selectedDateTime.set(Calendar.HOUR_OF_DAY, 9);
        selectedDateTime.set(Calendar.MINUTE, 0);
        selectedDateTime.set(Calendar.SECOND, 0);
        selectedDateTime.set(Calendar.MILLISECOND, 0);

        updateDateTimeButtons();
    }

    private void setupNotificationOptions() {
        // Настройка ChipGroup для выбора времени уведомления
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

        // По умолчанию выбираем 15 минут
        chipGroupNotificationTime.check(R.id.chip15min);
    }

    // === ЗАГРУЗКА И СОХРАНЕНИЕ ДАННЫХ ===

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

        // Дата и время
        if (currentTask.getCompletionTime() > 0) {
            selectedDateTime.setTimeInMillis(currentTask.getCompletionTime());
            updateDateTimeButtons();
        }

        // Уведомления
        switchNotification.setChecked(currentTask.isNotificationEnabled());
        selectedNotificationMinutes = currentTask.getNotificationMinutesBefore();
        selectNotificationChip();

        chipGroupNotificationTime.setVisibility(
                currentTask.isNotificationEnabled() ? View.VISIBLE : View.GONE);
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

    // === ДИАЛОГИ ===

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

        // Устанавливаем минимальную дату - сегодня
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
        String[] options = {"Камера", "Галерея", "Файлы", "Просмотр вложений"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Вложения");
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

    // === РАБОТА С ВЛОЖЕНИЯМИ ===

    private void openCamera() {
        // TODO: Реализовать открытие камеры
        Toast.makeText(this, "Функция камеры будет добавлена позже", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        // TODO: Реализовать выбор из галереи
        Toast.makeText(this, "Функция галереи будет добавлена позже", Toast.LENGTH_SHORT).show();
    }

    private void openFilePicker() {
        // TODO: Реализовать выбор файлов
        Toast.makeText(this, "Функция выбора файлов будет добавлена позже", Toast.LENGTH_SHORT).show();
    }

    private void viewAttachments() {
        // TODO: Реализовать просмотр вложений
        if (isEditMode && currentTask != null) {
            Toast.makeText(this, "Просмотр вложений будет добавлен позже", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Сначала сохраните задачу", Toast.LENGTH_SHORT).show();
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private void updateDateTimeButtons() {
        btnSetDate.setText(DateUtils.formatShortDate(selectedDateTime.getTimeInMillis()));
        btnSetTime.setText(DateUtils.formatTime(selectedDateTime.getTimeInMillis()));
    }

    private void updateUI() {
        // Обновляем видимость элементов в зависимости от режима
    }

    private boolean validateInput() {
        boolean isValid = true;

        // Проверка заголовка
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            tilTitle.setError("Введите название задачи");
            isValid = false;
        } else {
            tilTitle.setError(null);
        }

        // Проверка категории
        String category = etCategory.getText().toString().trim();
        if (category.isEmpty()) {
            tilCategory.setError("Выберите категорию");
            isValid = false;
        } else {
            tilCategory.setError(null);
        }

        return isValid;
    }

    // === СОХРАНЕНИЕ ===

    private void saveTask() {
        if (!validateInput()) {
            return;
        }

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        boolean notificationEnabled = switchNotification.isChecked();

        if (isEditMode && currentTask != null) {
            // Обновляем существующую задачу
            currentTask.setTitle(title);
            currentTask.setDescription(description);
            currentTask.setCategory(category);
            currentTask.setCompletionTime(selectedDateTime.getTimeInMillis());
            currentTask.setNotificationEnabled(notificationEnabled);
            currentTask.setNotificationMinutesBefore(selectedNotificationMinutes);

            taskViewModel.updateTask(currentTask);
        } else {
            // Создаем новую задачу
            Task newTask = new Task();
            newTask.setTitle(title);
            newTask.setDescription(description);
            newTask.setCategory(category);
            newTask.setCompletionTime(selectedDateTime.getTimeInMillis());
            newTask.setNotificationEnabled(notificationEnabled);
            newTask.setNotificationMinutesBefore(selectedNotificationMinutes);

            taskViewModel.insertTask(newTask);
        }

        setResult(RESULT_OK);
        finish();
    }

    // === МЕНЮ ===

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_task, menu);

        // Показываем пункт удаления только в режиме редактирования
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
            // Кнопка "Назад" в ActionBar
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
        builder.setTitle("Удалить задачу");
        builder.setMessage("Вы уверены, что хотите удалить эту задачу?");

        builder.setPositiveButton("Удалить", (dialog, which) -> {
            taskViewModel.deleteTask(currentTask);
            setResult(RESULT_OK);
            finish();
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    @Override
    public void onBackPressed() {
        // Проверяем, есть ли несохраненные изменения
        if (hasUnsavedChanges()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Несохраненные изменения");
            builder.setMessage("У вас есть несохраненные изменения. Сохранить задачу?");

            builder.setPositiveButton("Сохранить", (dialog, which) -> saveTask());
            builder.setNegativeButton("Не сохранять", (dialog, which) -> super.onBackPressed());
            builder.setNeutralButton("Отмена", null);

            builder.show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        // Проверяем, изменились ли поля с момента загрузки
        String currentTitle = etTitle.getText().toString().trim();
        String currentDescription = etDescription.getText().toString().trim();
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