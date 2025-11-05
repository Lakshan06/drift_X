# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep TensorFlow Lite
-keep class org.tensorflow.** { *; }
-keepclassmembers class org.tensorflow.** { *; }

# Keep Koin
-keep class org.koin.** { *; }
-keepclassmembers class org.koin.** { *; }

# Keep Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Model classes
-keep class com.driftdetector.app.data.model.** { *; }
-keep class com.driftdetector.app.domain.model.** { *; }

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ============================================================================
# Note about ashmem deprecation warning:
# ============================================================================
# If you see "ashmem: Pinning is deprecated since Android Q" warnings in logcat,
# this is coming from third-party libraries (likely MPAndroidChart or TensorFlow)
# that haven't updated to modern memory APIs yet. This is a deprecation warning,
# not an error - it's harmless and doesn't affect app functionality.
# The warning can be safely ignored until library maintainers update their code.
# ============================================================================
