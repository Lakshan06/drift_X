# =============================================================================
# DriftGuardAI ProGuard Configuration
# Comprehensive crash prevention and optimization rules
# =============================================================================

# =============================================================================
# CRASH PREVENTION - TensorFlow Lite & Support Libraries
# =============================================================================

# TensorFlow Lite Core
-keep class org.tensorflow.** { *; }
-keepclassmembers class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# TensorFlow Lite Support (CRITICAL for preventing R8 crashes)
-keep class com.google.auto.value.** { *; }
-keepclassmembers class com.google.auto.value.** { *; }
-dontwarn com.google.auto.value.**

-keep class com.google.errorprone.** { *; }
-keepclassmembers class com.google.errorprone.** { *; }
-dontwarn com.google.errorprone.**

# TensorFlow Lite GPU
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.gpu.**

# =============================================================================
# DEPENDENCY INJECTION - Koin
# =============================================================================

-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }
-keepnames class org.koin.**
-keepclassmembers class * {
    public <init>(...);
}

# Keep Koin modules
-keep class * extends org.koin.core.module.Module { *; }

# =============================================================================
# NETWORKING - Retrofit & OkHttp
# =============================================================================

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Ktor (RunAnywhere SDK)
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# =============================================================================
# SERIALIZATION - Gson & Kotlinx Serialization
# =============================================================================

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep generic signatures
-keepattributes Signature

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# =============================================================================
# DATABASE - Room
# =============================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** Companion;
}

# Room Paging
-dontwarn androidx.room.paging.**

# SQLite
-keep class androidx.sqlite.** { *; }

# =============================================================================
# APP DATA MODELS
# =============================================================================

# Keep all data models
-keep class com.driftdetector.app.data.model.** { *; }
-keep class com.driftdetector.app.domain.model.** { *; }
-keep class com.driftdetector.app.data.local.entity.** { *; }

# Keep ViewModels
-keep class com.driftdetector.app.presentation.viewmodel.** { *; }

# =============================================================================
# JETPACK COMPOSE
# =============================================================================

-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class ** { *; }
-keep @androidx.compose.runtime.Composable interface ** { *; }

# Compose Runtime
-keep class kotlin.Metadata { *; }
-keep class androidx.compose.runtime.** { *; }

# =============================================================================
# NATIVE METHODS
# =============================================================================

-keepclasseswithmembernames class * {
    native <methods>;
}

# =============================================================================
# PARCELABLE & SERIALIZABLE
# =============================================================================

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =============================================================================
# ENUMS
# =============================================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================================================
# ANDROIDX & ANDROID COMPONENTS
# =============================================================================

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Navigation
-keep class androidx.navigation.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Security (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# =============================================================================
# THIRD-PARTY LIBRARIES
# =============================================================================

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Timber
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# =============================================================================
# KOTLIN
# =============================================================================

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin Reflect
-dontwarn kotlin.reflect.**
-keep class kotlin.reflect.** { *; }

# =============================================================================
# OPTIMIZATION SETTINGS
# =============================================================================

# Optimize but keep stack traces readable
-optimizationpasses 5
-dontpreverify

# Keep line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable

# Rename stack trace obfuscation
-renamesourcefileattribute SourceFile

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# =============================================================================
# CRASH PREVENTION - Additional Safety Rules
# =============================================================================

# Keep all exceptions
-keep public class * extends java.lang.Exception

# Keep crashlytics (if added in future)
-keepattributes *Annotation*,SourceFile,LineNumberTable

# Keep custom Application class
-keep class com.driftdetector.app.DriftDetectorApp { *; }

# Keep Activities
-keep class * extends android.app.Activity
-keep class * extends androidx.activity.ComponentActivity

# =============================================================================
# RUNANYWHERE SDK (Optional - if AAR files present)
# =============================================================================

-keep class com.runanywhere.** { *; }
-dontwarn com.runanywhere.**

# =============================================================================
# WARNINGS TO IGNORE (SAFE)
# =============================================================================

# These warnings are safe to ignore
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# =============================================================================
# NOTES
# =============================================================================

# Note about ashmem deprecation warning:
# If you see "ashmem: Pinning is deprecated since Android Q" warnings in logcat,
# this is coming from third-party libraries (MPAndroidChart or TensorFlow)
# that haven't updated to modern memory APIs yet. This is a deprecation warning,
# not an error - it's harmless and doesn't affect app functionality.
# ============================================================================
