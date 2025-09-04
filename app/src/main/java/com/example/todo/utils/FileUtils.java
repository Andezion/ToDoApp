package com.example.todo.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class FileUtils {


    public static final String ATTACHMENTS_DIR = "attachments";
    public static final String IMAGES_DIR = "images";
    public static final String DOCUMENTS_DIR = "documents";

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    public static final int MAX_IMAGE_WIDTH = 1920;
    public static final int MAX_IMAGE_HEIGHT = 1920;

    public static File createAttachmentsDirectory(Context context) {
        File attachmentsDir = new File(context.getFilesDir(), ATTACHMENTS_DIR);
        if (!attachmentsDir.exists()) {
            attachmentsDir.mkdirs();
        }
        return attachmentsDir;
    }

    public static File createImagesDirectory(Context context) {
        File attachmentsDir = createAttachmentsDirectory(context);
        File imagesDir = new File(attachmentsDir, IMAGES_DIR);
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        return imagesDir;
    }

    public static File createDocumentsDirectory(Context context) {
        File attachmentsDir = createAttachmentsDirectory(context);
        File documentsDir = new File(attachmentsDir, DOCUMENTS_DIR);
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }
        return documentsDir;
    }

    public static String generateUniqueFileName(String originalName) {
        long timestamp = System.currentTimeMillis();
        String extension = getFileExtension(originalName);
        return "file_" + timestamp + (extension.isEmpty() ? "" : "." + extension);
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDotIndex);
    }

    public static String getFileType(String fileName) {
        String extension = getFileExtension(fileName);

        if (extension.matches("(?i)(jpg|jpeg|png|gif|bmp|webp|svg)")) {
            return "image";
        }

        if (extension.matches("(?i)(pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|odt)")) {
            return "document";
        }

        if (extension.matches("(?i)(mp4|avi|mkv|mov|wmv|flv|webm)")) {
            return "video";
        }

        if (extension.matches("(?i)(mp3|wav|flac|aac|ogg|wma)")) {
            return "audio";
        }

        return "other";
    }

    public static String getMimeType(String fileName) {
        String extension = getFileExtension(fileName);
        if (extension.isEmpty()) return "application/octet-stream";

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    public static File copyFileToAppDirectory(Context context, Uri sourceUri, String targetFileName) throws IOException {
        String fileType = getFileType(targetFileName);
        File targetDir;

        if ("image".equals(fileType)) {
            targetDir = createImagesDirectory(context);
        } else {
            targetDir = createDocumentsDirectory(context);
        }

        File targetFile = new File(targetDir, targetFileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {

            if (inputStream == null) {
                throw new IOException("Cannot open input stream from URI");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return targetFile;
    }


    public static boolean copyFile(File source, File destination) {
        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(destination)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static File compressImageIfNeeded(File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) return imageFile;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT &&
                    imageFile.length() <= MAX_IMAGE_SIZE) {
                return imageFile;
            }

            float aspectRatio = (float) width / height;
            int newWidth, newHeight;

            if (width > height) {
                newWidth = MAX_IMAGE_WIDTH;
                newHeight = (int) (newWidth / aspectRatio);
            } else {
                newHeight = MAX_IMAGE_HEIGHT;
                newWidth = (int) (newHeight * aspectRatio);
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            }

            bitmap.recycle();
            scaledBitmap.recycle();

            return imageFile;
        } catch (Exception e) {
            e.printStackTrace();
            return imageFile;
        }
    }


    public static File createThumbnail(Context context, File imageFile, String thumbnailName) {
        try {
            File thumbnailsDir = new File(createImagesDirectory(context), "thumbnails");
            if (!thumbnailsDir.exists()) {
                thumbnailsDir.mkdirs();
            }

            File thumbnailFile = new File(thumbnailsDir, thumbnailName);

            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap == null) return null;

            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, 150, 150, true);

            try (FileOutputStream outputStream = new FileOutputStream(thumbnailFile)) {
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            }

            bitmap.recycle();
            thumbnail.recycle();

            return thumbnailFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = sizeInBytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }

    public static boolean isFileSizeValid(long fileSizeInBytes, String fileType) {
        if ("image".equals(fileType)) {
            return fileSizeInBytes <= MAX_IMAGE_SIZE;
        } else {
            return fileSizeInBytes <= MAX_FILE_SIZE;
        }
    }

    public static boolean isImageFile(String fileName) {
        return "image".equals(getFileType(fileName));
    }

    public static boolean isFileAccessible(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;

        File file = new File(filePath);
        return file.exists() && file.canRead();
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;

        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    public static boolean deleteFileWithThumbnail(Context context, String filePath) {
        boolean mainDeleted = deleteFile(filePath);
        if (isImageFile(filePath)) {
            String fileName = new File(filePath).getName();
            String thumbnailName = "thumb_" + fileName;
            File thumbnailsDir = new File(createImagesDirectory(context), "thumbnails");
            File thumbnailFile = new File(thumbnailsDir, thumbnailName);

            if (thumbnailFile.exists()) {
                thumbnailFile.delete();
            }
        }

        return mainDeleted;
    }

    public static void cleanupOldFiles(Context context, long maxAgeInMillis) {
        File attachmentsDir = new File(context.getFilesDir(), ATTACHMENTS_DIR);
        if (!attachmentsDir.exists()) return;

        long cutoffTime = System.currentTimeMillis() - maxAgeInMillis;
        cleanupDirectory(attachmentsDir, cutoffTime);
    }

    private static void cleanupDirectory(File directory, long cutoffTime) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                cleanupDirectory(file, cutoffTime);
                if (file.list() != null && file.list().length == 0) {
                    file.delete();
                }
            } else if (file.lastModified() < cutoffTime) {
                file.delete();
            }
        }
    }

    public static long getTotalAttachmentsSize(Context context) {
        File attachmentsDir = new File(context.getFilesDir(), ATTACHMENTS_DIR);
        return getDirectorySize(attachmentsDir);
    }

    private static long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
}