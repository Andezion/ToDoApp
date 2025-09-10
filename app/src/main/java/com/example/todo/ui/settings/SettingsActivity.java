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

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

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

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SettingsViewModel settingsViewModel;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

            setupPreferences();
        }

        private void setupPreferences() {
            SwitchPreferenceCompat hideCompletedPref = findPreference(SettingsViewModel.PREF_HIDE_COMPLETED);
            if (hideCompletedPref != null) {
                hideCompletedPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    settingsViewModel.setHideCompletedTasks((Boolean) newValue);
                    return true;
                });
            }

            Preference notificationTimePref = findPreference(SettingsViewModel.PREF_NOTIFICATION_TIME);
            if (notificationTimePref != null) {
                updateNotificationTimeSummary();
                notificationTimePref.setOnPreferenceClickListener(preference -> {
                    showNotificationTimeDialog();
                    return true;
                });
            }

            Preference defaultCategoryPref = findPreference(SettingsViewModel.PREF_DEFAULT_CATEGORY);
            if (defaultCategoryPref != null) {
                updateDefaultCategorySummary();
                defaultCategoryPref.setOnPreferenceClickListener(preference -> {
                    showDefaultCategoryDialog();
                    return true;
                });
            }

            Preference themePref = findPreference(SettingsViewModel.PREF_THEME_MODE);
            if (themePref != null) {
                updateThemeSummary();
                themePref.setOnPreferenceClickListener(preference -> {
                    showThemeDialog();
                    return true;
                });
            }

            Preference resetPref = findPreference("reset_settings");
            if (resetPref != null) {
                resetPref.setOnPreferenceClickListener(preference -> {
                    showResetConfirmationDialog();
                    return true;
                });
            }

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
            String[] options = {"5 minutes", "15 minutes", "30 minutes", "1 hour", "2 hours", "1 day"};
            int[] values = {5, 15, 30, 60, 120, 24 * 60};

            int currentValue = settingsViewModel.getCurrentNotificationTimeMinutes();
            int selectedIndex = 1;

            for (int i = 0; i < values.length; i++) {
                if (values[i] == currentValue) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Notification time");
            builder.setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                settingsViewModel.setNotificationTimeMinutes(values[which]);
                updateNotificationTimeSummary();
                dialog.dismiss();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showDefaultCategoryDialog() {
            String[] categories = {"General", "Work", "Personal", "Education", "Shopping", "Health"};
            String currentCategory = settingsViewModel.getCurrentDefaultCategory();

            int selectedIndex = 0;
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(currentCategory)) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Default category");
            builder.setSingleChoiceItems(categories, selectedIndex, (dialog, which) -> {
                settingsViewModel.setDefaultCategory(categories[which]);
                updateDefaultCategorySummary();
                dialog.dismiss();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showThemeDialog() {
            String[] themes = {"Light", "Dark", "system"};
            String[] values = {"light", "dark", "system"};
            String currentTheme = settingsViewModel.getCurrentThemeMode();

            int selectedIndex = 2;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(currentTheme)) {
                    selectedIndex = i;
                    break;
                }
            }

            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Application topic");
            builder.setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                settingsViewModel.setThemeMode(values[which]);
                updateThemeSummary();
                dialog.dismiss();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showResetConfirmationDialog() {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Reset settings");
            builder.setMessage("Are you sure you want to reset all settings to their default values?");
            builder.setPositiveButton("Reset", (dialog, which) -> {
                settingsViewModel.resetToDefaults();
                updateNotificationTimeSummary();
                updateDefaultCategorySummary();
                updateThemeSummary();

                SwitchPreferenceCompat hideCompletedPref = findPreference(SettingsViewModel.PREF_HIDE_COMPLETED);
                if (hideCompletedPref != null) {
                    hideCompletedPref.setChecked(false);
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showAboutDialog() {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("About the app");
            builder.setMessage(
                    "Todo App\n" +
                            "Version: 1.0.0\n\n" +
                            "An app for managing tasks with notifications, attachments, and categories.\n\n" +
                            "Designed for Android"
            );
            builder.setPositiveButton("OK", null);
            builder.show();
        }

        private String getNotificationTimeText(int minutes) {
            if (minutes < 60) {
                return minutes + " minutes";
            } else if (minutes < 24 * 60) {
                int hours = minutes / 60;
                return hours + (hours == 1 ? " hour" : " hours");
            } else {
                int days = minutes / (24 * 60);
                return days + (days == 1 ? " day" : " days");
            }
        }

        private String getThemeText(String theme) {
            switch (theme) {
                case "light":
                    return "Light";
                case "dark":
                    return "Dark";
                case "system":
                default:
                    return "System";
            }
        }
    }
}