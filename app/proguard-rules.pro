# Room Database Keep Rules
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

-keep class com.example.data.local.AppDatabase { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.entity.** { *; }
-keepclassmembers class com.example.data.local.entity.** { *; }

# DataStore & UserSettings Keep Rules
-keep class com.example.data.datastore.** { *; }
-keepclassmembers class com.example.data.datastore.** { *; }

# Backup & Repository Keep Rules
-keep class com.example.data.repository.** { *; }
-keepclassmembers class com.example.data.repository.** { *; }

# Utility & Serializer Keep Rules
-keep class com.example.util.** { *; }
-keepclassmembers class com.example.util.** { *; }

# AI Engine & Provider Keep Rules
-keep class com.example.ai.** { *; }
-keepclassmembers class com.example.ai.** { *; }

# JSON Keep Rules
-keep class org.json.** { *; }
-keepclassmembers class org.json.** { *; }

# Luaj Script Engine Keep Rules
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**
