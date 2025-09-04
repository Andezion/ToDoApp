package com.example.todo.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static final String FORMAT_FULL_DATE = "dd MMMM yyyy, HH:mm";
    public static final String FORMAT_SHORT_DATE = "dd.MM.yyyy";
    public static final String FORMAT_TIME = "HH:mm";
    public static final String FORMAT_DATE_TIME = "dd.MM.yyyy HH:mm";
    public static final String FORMAT_DAY_MONTH = "dd MMM";


    public static String formatDate(long timestamp, String format) {
        if (timestamp <= 0) return "";

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


    public static String formatFullDate(long timestamp) {
        return formatDate(timestamp, FORMAT_FULL_DATE);
    }


    public static String formatShortDate(long timestamp) {
        return formatDate(timestamp, FORMAT_SHORT_DATE);
    }


    public static String formatTime(long timestamp) {
        return formatDate(timestamp, FORMAT_TIME);
    }


    public static String formatDateTime(long timestamp) {
        return formatDate(timestamp, FORMAT_DATE_TIME);
    }


    public static String getRelativeTimeString(long timestamp) {
        if (timestamp <= 0) return "";

        Calendar taskCal = Calendar.getInstance();
        taskCal.setTimeInMillis(timestamp);

        Calendar todayCal = Calendar.getInstance();

        if (isSameDay(taskCal, todayCal)) {
            return "Сегодня, " + formatTime(timestamp);
        }

        todayCal.add(Calendar.DAY_OF_MONTH, 1);
        if (isSameDay(taskCal, todayCal)) {
            return "Завтра, " + formatTime(timestamp);
        }

        todayCal.add(Calendar.DAY_OF_MONTH, -2);
        if (isSameDay(taskCal, todayCal)) {
            return "Вчера, " + formatTime(timestamp);
        }
        todayCal.setTimeInMillis(System.currentTimeMillis());
        long diffInMillis = Math.abs(timestamp - todayCal.getTimeInMillis());
        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

        if (diffInDays < 7) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }


        return formatFullDate(timestamp);
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }


    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    public static long createTimestamp(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    public static long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    public static long getEndOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }


    public static long addMinutes(long timestamp, int minutes) {
        return timestamp + (minutes * 60 * 1000L);
    }


    public static long addDays(long timestamp, int days) {
        return timestamp + (days * 24 * 60 * 60 * 1000L);
    }

    public static boolean isOverdue(long completionTime) {
        return completionTime > 0 && completionTime < getCurrentTimestamp();
    }


    public static boolean shouldShowNotification(long completionTime, int minutesBefore) {
        if (completionTime <= 0) return false;

        long notificationTime = completionTime - (minutesBefore * 60 * 1000L);
        long currentTime = getCurrentTimestamp();

        return currentTime >= notificationTime && currentTime < completionTime;
    }


    public static int getDaysUntilDue(long completionTime) {
        if (completionTime <= 0) return -1;

        long currentTime = getCurrentTimestamp();
        long diffInMillis = completionTime - currentTime;

        if (diffInMillis < 0) return -1;

        return (int) (diffInMillis / (24 * 60 * 60 * 1000L));
    }


    public static String getTimeUntilDueString(long completionTime) {
        if (completionTime <= 0) return "";

        long currentTime = getCurrentTimestamp();
        long diffInMillis = completionTime - currentTime;

        if (diffInMillis < 0) {
            return "Просрочено";
        }

        long diffInMinutes = diffInMillis / (60 * 1000L);
        long diffInHours = diffInMinutes / 60;
        long diffInDays = diffInHours / 24;

        if (diffInDays > 0) {
            return diffInDays == 1 ? "Через 1 день" : "Через " + diffInDays + " дней";
        } else if (diffInHours > 0) {
            return diffInHours == 1 ? "Через 1 час" : "Через " + diffInHours + " часов";
        } else if (diffInMinutes > 0) {
            return diffInMinutes == 1 ? "Через 1 минуту" : "Через " + diffInMinutes + " минут";
        } else {
            return "Сейчас";
        }
    }


    public static long parseDate(String dateString, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
            Date date = sdf.parse(dateString);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }


    public static int[] getDateComponents(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        return new int[] {
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
        };
    }
}