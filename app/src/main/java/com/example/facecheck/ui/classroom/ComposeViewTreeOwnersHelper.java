package com.example.facecheck.ui.classroom;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;
import androidx.savedstate.SavedStateRegistryOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

/**
 * 将 Lifecycle / ViewModelStore / SavedState 挂到任意 View 上，供 BottomSheet 内 ComposeView 使用。
 * 放在 Java 中可避免部分 Kotlin + Lifecycle 版本组合下对 ViewTree* 静态方法的解析问题。
 */
public final class ComposeViewTreeOwnersHelper {

    private ComposeViewTreeOwnersHelper() {
    }

    public static void apply(
            @NonNull View view,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull ViewModelStoreOwner viewModelStoreOwner,
            @NonNull SavedStateRegistryOwner savedStateRegistryOwner) {
        ViewTreeLifecycleOwner.set(view, lifecycleOwner);
        ViewTreeViewModelStoreOwner.set(view, viewModelStoreOwner);
        ViewTreeSavedStateRegistryOwner.set(view, savedStateRegistryOwner);
    }
}
