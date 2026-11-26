# FitTrack ProGuard / R8 rules

# Keep all app classes intact
-keep class com.example.fittrack.** { *; }

# Keep SQLite-related entity fields (accessed reflectively by DatabaseHelper)
-keepclassmembers class com.example.fittrack.** {
    <fields>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# AndroidX / Material Components
-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
