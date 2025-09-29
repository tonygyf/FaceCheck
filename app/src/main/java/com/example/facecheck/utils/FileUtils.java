package com.example.facecheck.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static File createImageFile(Context context, String prefix) throws IOException {
        // 创建图片文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = prefix + "_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public static String copyUriToFile(Context context, Uri sourceUri, File destFile) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(destFile)) {
            
            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return null;
        }
    }

    public static boolean deleteFile(String path) {
        if (path == null) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.delete();
    }

    public static String getRelativePath(File baseDir, File file) {
        String basePath = baseDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length());
            return relativePath.startsWith(File.separator) ? 
                   relativePath.substring(1) : relativePath;
        }
        return null;
    }

    public static void ensureDirectoryExists(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + directory);
        }
    }
}