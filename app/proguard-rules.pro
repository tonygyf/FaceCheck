# 忽略重复的 XML 解析库警告
-dontwarn org.xmlpull.v1.**
-keep class org.xmlpull.v1.** { *; }
-keep class com.amap.api.location.** { *; }
-dontwarn com.amap.api.**
# 本地检测相关库已移除

# 预留：若未来重新开启 minify，保留登录/接口反射链路所需元数据
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault, EnclosingMethod, InnerClasses
-keep class com.example.facecheck.api.** { *; }
-keep class com.example.facecheck.data.model.** { *; }
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn javax.annotation.**
