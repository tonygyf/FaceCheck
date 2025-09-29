package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_DIMENSION = 1024;

    public static Bitmap loadAndResizeBitmap(String path, int reqWidth, int reqHeight) {
        // 首先只解码图像尺寸
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // 计算压缩比例
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // 解码完整图像
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        if (bitmap != null) {
            // 处理图像方向
            bitmap = rotateImageIfRequired(bitmap, path);
        }

        return bitmap;
    }

    public static Bitmap loadAndResizeBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            // 首先只解码图像尺寸
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // 计算压缩比例
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // 解码完整图像
            options.inJustDecodeBounds = false;
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap != null) {
                // 处理图像方向
                bitmap = rotateImageIfRequired(bitmap, uri, context);
            }

            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Error loading bitmap", e);
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, String path) {
        try {
            ExifInterface ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return rotateImage(img, getRotationAngleFromOrientation(orientation));
        } catch (IOException e) {
            Log.e(TAG, "Error getting Exif orientation", e);
            return img;
        }
    }

    private static Bitmap rotateImageIfRequired(Bitmap img, Uri uri, Context context) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return img;
            }
            
            ExifInterface ei = new ExifInterface(inputStream);
            inputStream.close();
            
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return rotateImage(img, getRotationAngleFromOrientation(orientation));
        } catch (IOException e) {
            Log.e(TAG, "Error getting Exif orientation", e);
            return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        if (degree == 0) {
            return img;
        }
        
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private static int getRotationAngleFromOrientation(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }

    public static boolean saveBitmapToFile(Bitmap bitmap, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap", e);
            return false;
        }
    }

    public static Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            return bitmap;
        }

        float scale;
        if (width > height) {
            scale = (float) MAX_IMAGE_DIMENSION / width;
        } else {
            scale = (float) MAX_IMAGE_DIMENSION / height;
        }

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.recycle();
        return resizedBitmap;
    }
}