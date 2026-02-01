plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.example.facecheck"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.facecheck"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 注意：这里删除了 ndk { abiFilters } 块，因为它与下方的 splits 冲突
    }

    // 【核心修改】开启分架构打包
    splits {
        abi {
            isEnable = true // 开启分包
            reset() // 重置默认架构
            include("armeabi-v7a", "arm64-v8a") // 仅打包手机常用的两个架构
            isUniversalApk = false // 不生成包含所有架构的巨无霸包
        }
    }

    buildTypes {
        release {
            // 开启代码混淆和资源压缩，有助于进一步减小体积
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 【新增】解决 ML 相关库可能产生的文件冲突
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/LICENSE.txt"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.generativeai)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.sardine)
    implementation(libs.androidx.sqlite)
    implementation(libs.glide)
    implementation(libs.circleimageview)

    // Lottie 动画
    implementation("com.airbnb.android:lottie:6.4.0")

    // Google ML Kit 人脸检测
    implementation("com.google.mlkit:face-detection:16.1.6")

    // TensorFlow Lite 推理
    implementation("org.tensorflow:tensorflow-lite:2.12.0")

    // 【体积大户】只有模型报错说缺少算子时才需要它。
    // 如果打包后体积依然无法接受，且模型较标准，可以尝试注释掉这一行看是否报错。
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}