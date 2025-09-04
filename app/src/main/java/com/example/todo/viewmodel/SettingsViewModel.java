package com.example.todo.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.HashSet;
import java.util.Set;

public class SettingsViewModel extends AndroidViewModel {

    public static final String PREF_HIDE_COMPLETED = "hide_completed_tasks";
    public static final String PREF_NOTIFICATION_TIME = "notification_time_minutes";
    public static final String PREF_VISIBLE_CATEGORIES = "visible_categories";
    public static final String PREF_DEFAULT_CATEGORY = "default_category";
    public static final String PREF_THEME_MODE = "theme_mode";
    public static final String PREF_SORT_ORDER = "sort_order";

    public static final int DEFAULT_NOTIFICATION_TIME = 15;
    public static final String DEFAULT_CATEGORY = "Общие";
    public static final String DEFAULT_THEME = "system";
    public static final String DEFAULT_SORT_ORDER = "due_time";

    private SharedPreferences sharedPreferences;

    private MutableLiveData<Boolean> hideCompletedTasks = new MutableLiveData<>();
    private MutableLiveData<Integer> notificationTimeMinutes = new MutableLiveData<>();
    private MutableLiveData<Set<String>> visibleCategories = new MutableLiveData<>();
    private MutableLiveData<String> defaultCategory = new MutableLiveData<>();
    private MutableLiveData<String> themeMode = new MutableLiveData<>();
    private MutableLiveData<String> sortOrder = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        loadSettings();
    }


    private void loadSettings() {
        hideCompletedTasks.setValue(sharedPreferences.getBoolean(PREF_HIDE_COMPLETED, false));
        notificationTimeMinutes.setValue(sharedPreferences.getInt(PREF_NOTIFICATION_TIME, DEFAULT_NOTIFICATION_TIME));

        Set<String> defaultVisibleCategories = new HashSet<>();
        defaultVisibleCategories.add(DEFAULT_CATEGORY);
        visibleCategories.setValue(sharedPreferences.getStringSet(PREF_VISIBLE_CATEGORIES, defaultVisibleCategories));

        defaultCategory.setValue(sharedPreferences.getString(PREF_DEFAULT_CATEGORY, DEFAULT_CATEGORY));
        themeMode.setValue(sharedPreferences.getString(PREF_THEME_MODE, DEFAULT_THEME));
        sortOrder.setValue(sharedPreferences.getString(PREF_SORT_ORDER, DEFAULT_SORT_ORDER));
    }


    public MutableLiveData<Boolean> getHideCompletedTasks() {
        return hideCompletedTasks;
    }

    public MutableLiveData<Integer> getNotificationTimeMinutes() {
        return notificationTimeMinutes;
    }

    public MutableLiveData<Set<String>> getVisibleCategories() {
        return visibleCategories;
    }

    public MutableLiveData<String> getDefaultCategory() {
        return defaultCategory;
    }

    public MutableLiveData<String> getThemeMode() {
        return themeMode;
    }

    public MutableLiveData<String> getSortOrder() {
        return sortOrder;
    }


    public void setHideCompletedTasks(boolean hide) {
        sharedPreferences.edit().putBoolean(PREF_HIDE_COMPLETED, hide).apply();
        hideCompletedTasks.setValue(hide);
    }

    public void setNotificationTimeMinutes(int minutes) {
        sharedPreferences.edit().putInt(PREF_NOTIFICATION_TIME, minutes).apply();
        notificationTimeMinutes.setValue(minutes);
    }

    public void setVisibleCategories(Set<String> categories) {
        sharedPreferences.edit().putStringSet(PREF_VISIBLE_CATEGORIES, categories).apply();
        visibleCategories.setValue(categories);
    }

    public void setDefaultCategory(String category) {
        sharedPreferences.edit().putString(PREF_DEFAULT_CATEGORY, category).apply();
        defaultCategory.setValue(category);
    }

    public void setThemeMode(String theme) {
        sharedPreferences.edit().putString(PREF_THEME_MODE, theme).apply();
        themeMode.setValue(theme);
    }

    public void setSortOrder(String order) {
        sharedPreferences.edit().putString(PREF_SORT_ORDER, order).apply();
        sortOrder.setValue(order);
    }


    public boolean isCategoryVisible(String category) {
        Set<String> visible = visibleCategories.getValue();
        return visible != null && visible.contains(category);
    }

    public void addVisibleCategory(String category) {
        Set<String> visible = visibleCategories.getValue();
        if (visible != null) {
            visible.add(category);
            setVisibleCategories(visible);
        }
    }

    public void removeVisibleCategory(String category) {
        Set<String> visible = visibleCategories.getValue();
        if (visible != null) {
            visible.remove(category);
            setVisibleCategories(visible);
        }
    }


    public void resetToDefaults() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_HIDE_COMPLETED, false);
        editor.putInt(PREF_NOTIFICATION_TIME, DEFAULT_NOTIFICATION_TIME);

        Set<String> defaultCategories = new HashSet<>();
        defaultCategories.add(DEFAULT_CATEGORY);
        editor.putStringSet(PREF_VISIBLE_CATEGORIES, defaultCategories);

        editor.putString(PREF_DEFAULT_CATEGORY, DEFAULT_CATEGORY);
        editor.putString(PREF_THEME_MODE, DEFAULT_THEME);
        editor.putString(PREF_SORT_ORDER, DEFAULT_SORT_ORDER);
        editor.apply();

        loadSettings();
    }



    public boolean getCurrentHideCompletedTasks() {
        return Boolean.TRUE.equals(hideCompletedTasks.getValue());
    }

    public int getCurrentNotificationTimeMinutes() {
        Integer value = notificationTimeMinutes.getValue();
        return value != null ? value : DEFAULT_NOTIFICATION_TIME;
    }

    public String getCurrentDefaultCategory() {
        String value = defaultCategory.getValue();
        return value != null ? value : DEFAULT_CATEGORY;
    }

    public String getCurrentThemeMode() {
        String value = themeMode.getValue();
        return value != null ? value : DEFAULT_THEME;
    }

    public String getCurrentSortOrder() {
        String value = sortOrder.getValue();
        return value != null ? value : DEFAULT_SORT_ORDER;
    }



    public String exportSettings() {

        StringBuilder sb = new StringBuilder();
        sb.append("hideCompleted:").append(getCurrentHideCompletedTasks()).append(";");
        sb.append("notificationTime:").append(getCurrentNotificationTimeMinutes()).append(";");
        sb.append("defaultCategory:").append(getCurrentDefaultCategory()).append(";");
        sb.append("theme:").append(getCurrentThemeMode()).append(";");
        sb.append("sortOrder:").append(getCurrentSortOrder()).append(";");
        return sb.toString();
    }

    public void importSettings(String settingsString) {
        if (settingsString == null || settingsString.isEmpty()) return;

        String[] pairs = settingsString.split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                switch (key) {
                    case "hideCompleted":
                        setHideCompletedTasks(Boolean.parseBoolean(value));
                        break;
                    case "notificationTime":
                        setNotificationTimeMinutes(Integer.parseInt(value));
                        break;
                    case "defaultCategory":
                        setDefaultCategory(value);
                        break;
                    case "theme":
                        setThemeMode(value);
                        break;
                    case "sortOrder":
                        setSortOrder(value);
                        break;
                }
            }
        }
    }
}