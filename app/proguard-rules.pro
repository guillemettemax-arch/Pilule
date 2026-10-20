# Proguard rules for Ma Pilule
-keepattributes *Annotation*
-dontwarn java.awt.**
-dontwarn javax.annotation.**
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.compose.material3.** { *; }
