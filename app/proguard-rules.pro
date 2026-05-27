# Add project specific ProGuard rules here.
# By default, the ProGuard rules in the Android Gradle Plugin are used.

# Room Database rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt & Dagger Rules
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends javax.inject.Provider
-keep class * extends dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories$InternalViewModelFactory { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# CameraX Rules
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.video.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-dontwarn androidx.camera.core.**
-dontwarn androidx.camera.video.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
# Androidx Security / Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Timber Logger
-keep class timber.log.Timber { *; }
-keep class timber.log.Timber$Tree { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext {
    volatile android.os.Handler _handler;
}
-dontwarn kotlinx.coroutines.**
