# This is a configuration file for ProGuard.
# http://proguard.sourceforge.net/index.html#manual/usage.html

-dontusemixedcaseclassnames

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Compose
-keep class androidx.compose.** { *; }

# Kotlin
-keepclassmembers class kotlin.Metadata {
    public static kotlin.Metadata read(java.lang.Class);
}

-dontwarn com.google.android.material.**
-dontwarn androidx.**
