package com.example.todo.ui.settings;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.todo.R;
import com.example.todo.viewmodel.SettingsViewModel;

/**
 * Активность настроек приложения
 * Путь: app/src/main/java/com/yourpackage/todoapp/ui/settings/SettingsActivity.java
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Настройка ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройки");
        }

        // Загружаем фрагмент с настройками
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Фрагмент с настройками приложения
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SettingsViewModel settingsViewModel;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            // Инициализируем ViewModel
            settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

            setupPreferences();
        }

        private void setupPreferences() {
            // Скрытие завершенных задач
            SwitchPreferenceCompat hideCompletedPref = findPreference(SettingsViewModel.PREF_HIDE_COMPLETED);
            if (hideCompletedPref != null) {
                hideCompletedPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsViewModel.setHideCompletedTasks((Boolean) newValue);
                    return true;
                });
            }

            // Время уведомления
            Preference notificationTimePref = findPreference(SettingsViewModel.PREF_NOTIFICATION_TIME);
            if (notificationTimePref != null) {
                updateNotificationTimeSummary();
                notificationTimePref.setOnPreferenceClickListener(preference -> {
                    showNotificationTimeDialog();
                    return true;
                });
            }

            // Категория по умолчанию
            Preference defaultCategoryPref = findPreference(SettingsViewModel.PREF_DEFAULT_CATEGORY);
            if (defaultCategoryPref != null) {
                updateDefaultCategorySummary();
                defaultCategoryPref.setOnPreferenceClickListener(preference -> {
                    showDefaultCategoryDialog();
                    return true;
                });
            }

            // Тема приложения
            Preference themePref = findPreference(SettingsViewModel.PREF_THEME_MODE);
            if (themePref != null) {
                updateThemeSummary();
                themePref.setOnPreferenceClickListener(preference -> {
                    showThemeDialog();
                    return true;
                });
            }

            // Сброс настроек
            Preference resetPref = findPreference("reset_settings");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(preference -> {
                    showResetConfirmationDialog();
                    return true;
                });
            }

            // О приложении
            Preference aboutPref = findPreference("about");
            if (aboutPref != null) {
                aboutPref.setOnPreferenceClickListener(preference -> {
                    showAboutDialog();
                    return true;
                });
            }
        }

        private void updateNotificationTimeSummary() {
            Preference notificationTimePref = findPreference(SettingsViewModel.PREF_NOTIFICATION_TIME);
            if (notificationTimePref != null) {
                int minutes = settingsViewModel.getCurrentNotificationTimeMinutes();
                String summary = getNotificationTimeText(minutes);
                notificationTimePref.setSummary(summary);
            }
        }

        private void updateDefaultCategorySummary() {
            Preference defaultCategoryPref = findPreference(SettingsViewModel.PREF_DEFAULT_CATEGORY);
            if (defaultCategoryPref != null) {
                String category = settingsViewModel.getCurrentDefaultCategory();
                defaultCategoryPref.setSummary(category);
            }
        }

        private void updateThemeSummary() {
            Preference themePref = findPreference(SettingsViewModel.PREF_THEME_MODE);
            if (themePref != null) {
                String theme = settingsViewModel.getCurrentThemeMode();
                String summary = getThemeText(theme);
                themePref.setSummary(summary);
            }
        }

        private void showNotificationTimeDialog() {
            String[] options = {"5 минут", "15 минут", "30 минут", "1 час", "2 часа", "1 день"};
            int[] values = {5, 15, 30, 60, 120, 24 * 60};

            int currentValue = settingsViewModel.getCurrentNotificationTimeMinutes();
            int selectedIndex = 1; // По умолчанию 15 минут

            for (int i = 0; i < values.length; i++) {
                if (values[i] == currentValue) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Время уведомления");
            builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                settingsViewModel.setNotificationTimeMinutes(values[which]);
                updateNotificationTimeSummary();
                dialog.dismiss();
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void showDefaultCategoryDialog() {
            String[] categories = {"Общие", "Работа", "Личное", "Учеба", "Покупки", "Здоровье"};
            String currentCategory = settingsViewModel.getCurrentDefaultCategory();

            int selectedIndex = 0;
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(currentCategory)) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Категория по умолчанию");
            builder.setSingleChoiceItems(categories, selectedIndex, (dialog, which) -> {
                settingsViewModel.setDefaultCategory(categories[which]);
                updateDefaultCategorySummary();
                dialog.dismiss();
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void showThemeDialog() {
            String[] themes = {"Светлая", "Темная", "Системная"};
            String[] values = {"light", "dark", "system"};
            String currentTheme = settingsViewModel.getCurrentThemeMode();

            int selectedIndex = 2; // По умолчанию системная
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(currentTheme)) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Тема приложения");
            builder.setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                settingsViewModel.setThemeMode(values[which]);
                updateThemeSummary();
                // Здесь можно добавить применение темы
                dialog.dismiss();
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void showResetConfirmationDialog() {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Сброс настроек");
            builder.setMessage("Вы уверены, что хотите сбросить все настройки к значениям по умолчанию?");
            builder.setPositiveButton("Сбросить", (dialog, which) -> {
                settingsViewModel.resetToDefaults();
                // Обновляем отображение всех настроек
                updateNotificationTimeSummary();
                updateDefaultCategorySummary();
                updateThemeSummary();

                // Обновляем переключатели
                SwitchPreferenceCompat hideCompletedPref = findPreference(SettingsViewModel.PREF_HIDE_COMPLETED);
                if (hideCompletedPref != null) {
                    hideCompletedPref.setChecked(false);
                }
            });
            builder.setNegativeButton("Отмена", null);
            builder.show();
        }

        private void showAboutDialog() {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("О приложении");
            builder.setMessage(
                    "Todo App\n" +
                            "Версия: 1.0.0\n\n" +
                            "Приложение для управления задачами с уведомлениями, вложениями и категориями.\n\n" +
                            "Разработано для Android"
            );
            builder.setPositiveButton("OK", null);
            builder.show();
        }

        private String getNotificationTimeText(int minutes) {
            if (minutes < 60) {
                return minutes + " минут";
            } else if (minutes < 24 * 60) {
                int hours = minutes / 60;
                return hours + (hours == 1 ? " час" : " часа");
            } else {
                int days = minutes / (24 * 60);
                return days + (days == 1 ? " день" : " дня");
            }
        }

        private String getThemeText(String theme) {
            switch (theme) {
                case "light":
                    return "Светлая";
                case "dark":
                    return "Темная";
                case "system":
                default:
                    return "Системная";
            }
        }
    }
}