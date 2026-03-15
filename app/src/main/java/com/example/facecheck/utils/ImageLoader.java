package com.example.facecheck.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.example.facecheck.R;

import java.io.File;

/**
 * Glide 图片加载工具类，用于统一管理应用的图片加载、缓存和失效策略。
 */
public class ImageLoader {

    /**
     * 获取通用的 Glide 请求配置，包括占位符和错误图。
     *
     * @return RequestOptions 配置实例
     */
    private static RequestOptions getBaseOptions() {
        return new RequestOptions()
                .placeholder(R.drawable.ic_person_placeholder) // 统一的占位符
                .error(R.drawable.ic_person_placeholder); // 统一的错误图
    }

    /**
     * 加载用户头像。
     *
     * @param context   上下文
     * @param model     图片路径（可以是 URL, File, Uri, or resource id）
     * @param target    目标 ImageView
     * @param signature 用于缓存失效的签名，通常是文件修改时间或版本号
     */
    public static void loadAvatar(Context context, Object model, ImageView target, String signature) {
        RequestOptions options = getBaseOptions()
                .signature(new ObjectKey(signature != null ? signature : "default_signature"));

        Glide.with(context)
                .load(model)
                .apply(options)
                .into(target);
    }

    /**
     * 加载普通图片，不带特定缓存失效签名。
     *
     * @param context 上下文
     * @param model   图片路径
     * @param target  目标 ImageView
     */
    public static void loadImage(Context context, Object model, ImageView target) {
        Glide.with(context)
                .load(model)
                .apply(getBaseOptions())
                .into(target);
    }

    /**
     * 清除所有 Glide 缓存（内存和磁盘）。
     * 磁盘缓存的清除必须在后台线程执行。
     *
     * @param context 上下文
     */
    public static void clearAllCache(final Context context) {
        // 清除内存缓存（必须在主线程）
        Glide.get(context).clearMemory();

        // 清除磁盘缓存（必须在后台线程）
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
    }

    /**
     * 使指定路径的图片缓存失效。
     * 注意：此方法并不能直接从缓存中删除文件，而是通过改变签名来强制 Glide 重新加载。
     * 要与 loadAvatar 方法中的 signature 配合使用。
     * 如果需要物理删除，需要更复杂的操作。
     *
     * @param context 上下文
     * @param path    需要失效的图片路径
     */
    public static void invalidateImageCache(Context context, String path) {
        // Glide 的缓存键是基于路径、尺寸、变换等多种因素生成的。
        // 最可靠的失效方法是改变加载时的签名（Signature）。
        // 当下次使用 loadAvatar 并传入新的 signature (如文件修改时间戳) 时，Glide会认为这是一个新的请求。
        // 这里我们仅提供一个示例性的清除方法，实际应用中应更新加载时的签名。
        // 如果确实需要主动清除，可以考虑直接删除 Glide 缓存目录下的特定文件，但这很复杂且不推荐。
    }
}
