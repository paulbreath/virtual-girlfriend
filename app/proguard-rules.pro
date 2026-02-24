# Add project specific ProGuard rules here.

# ========== 安全相关规则 ==========

# 保护 BuildConfig（不要混淆）
-keep class com.example.universal.BuildConfig { *; }

# 保护 SecurityUtils
-keep class com.example.universal.SecurityUtils { *; }
-keep class com.example.universal.LogSecurity { *; }

# 保护枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 混淆网络请求相关（防止直接看到 API 调用）
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# 移除日志（Release 打包时完全移除）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-assumenosideeffects class com.example.universal.LogSecurity {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# 混淆 PackageInfo（增加逆向难度）
- obfuscationmapping.txt
- printmapping mapping.txt
