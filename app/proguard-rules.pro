# Add project specific ProGuard rules here.
-keep class com.gastozen.data.model.** { *; }
-keep class com.gastozen.data.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
