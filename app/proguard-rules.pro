# Keep rules untuk release (isMinifyEnabled=true).
# Tanpa ini R8 merusak kotlinx.serialization, Retrofit, dan Room.

# kotlinx.serialization: pertahankan serializer + field model @Serializable.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclasseswithmembernames class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit/OkHttp: pertahankan signature generic + annotation HTTP.
-keepattributes Signature
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Room: pertahankan entity/DAO yang dipakai via KSP/codegen.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# DataStore/Preferences + WorkManager: pertahankan via reflection.
-keep class androidx.datastore.** { *; }
-keep class androidx.work.** { *; }
