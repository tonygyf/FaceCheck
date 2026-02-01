# 忽略重复的 XML 解析库警告
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *; }

# 防止机器学习库的代码被误删
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.mlkit.** { *; }