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
        // 【新增】只保留主流手机架构，剔除不常用的能省很多空间
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }

    }
    // 【新增】分架构打包配置：这是解决你 600MB 问题的核心
    splits {
        abi {
            isEnable = true // 开启分包
            reset() // 重置默认列表
            include("armeabi-v7a", "arm64-v8a") // 手机端主要这两个就够了，x86一般用于模拟器
            isUniversalApk = false // 设置为 false，这样就不会生成那个包含所有架构的“巨无霸”包了
        }
    }

    buildTypes {
        release {
            // 【修改】开启代码混淆和资源缩减
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

    // TensorFlow Lite 推理（用于 MobileFaceNet / FaceNet 特征提取）
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    // FaceNet 需要 Flex 算子支持（如 FlexFusedBatchNormV3）
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.12.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
