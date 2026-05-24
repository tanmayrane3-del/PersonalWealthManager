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
# Retrofit 2 — official R8 rules from square/retrofit
# https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro
# ============================================================
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Retain Retrofit service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# R8 full mode strips generic signatures from runtime-annotated classes.
# This -if rule says: for every interface that has Retrofit HTTP annotated
# methods, keep the interface itself (and its members + Signature attribute).
# This is THE rule that fixes "Class cannot be cast to ParameterizedType"
# crashes from Kotlin suspend functions in Retrofit interfaces.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Belt-and-suspenders: also explicitly keep our API interface package roots.
-keep interface com.pwm.personalwealthmanager.data.remote.api.** { *; }
-keep interface com.example.personalwealthmanager.data.remote.api.** { *; }

# Kotlin coroutines Continuation is referenced by Retrofit's suspend handling.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-dontwarn retrofit2.**
-dontwarn retrofit2.Platform$Java8
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn kotlin.Unit

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
