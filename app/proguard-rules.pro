# Add project specific ProGuard rules here.

# ============================================================
# CRASH FIX: java.lang.Class cannot be cast to ParameterizedType
# R8 strips generic type signatures by default. Gson + Retrofit
# need them at runtime to deserialize ApiResponse<T> wrappers.
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ============================================================
# Gson
# ============================================================
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn com.google.gson.**

# ============================================================
# Retrofit 2
# ============================================================
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn retrofit2.Platform$Java8

# ============================================================
# OkHttp / Okio
# ============================================================
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ============================================================
# All DTO / model classes used by Gson serialization.
# Both package roots are included (com.example.* legacy aliases
# still exist in some files; com.pwm.* is the real package).
# ============================================================
-keep class com.pwm.personalwealthmanager.data.remote.dto.** { *; }
-keep class com.example.personalwealthmanager.data.remote.dto.** { *; }
-keepclassmembers class com.pwm.personalwealthmanager.data.remote.dto.** { *; }
-keepclassmembers class com.example.personalwealthmanager.data.remote.dto.** { *; }

# ApiResponse generic wrapper — critical for Gson to resolve T at runtime
-keep class com.pwm.personalwealthmanager.data.remote.dto.ApiResponse { *; }
-keep class com.example.personalwealthmanager.data.remote.dto.ApiResponse { *; }

# ============================================================
# Domain models
# ============================================================
-keep class com.pwm.personalwealthmanager.domain.model.** { *; }
-keep class com.example.personalwealthmanager.domain.model.** { *; }

# ============================================================
# Hilt / Dagger
# ============================================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-dontwarn dagger.**
-dontwarn dagger.hilt.**

# ============================================================
# Coroutines
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Debugging: preserve source file names and line numbers
# so stack traces are readable in crash reports.
# ============================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
