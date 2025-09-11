# ProGuard rules for AIDestinyMaster
# Keep Jetpack Compose runtime and generated classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn androidx.compose.**

# Keep data classes and enums' names
-keepclassmembers class * {
    @kotlin.Metadata *;
}

# Gson: keep model fields used by reflection
-keep class com.aidestinymaster.** { *; }

# Room: keep generated code and entities/daos
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Google Play services / Billing / Ads often add META-INF; safe to ignore warnings
-dontwarn com.google.**

# WorkManager
-dontwarn androidx.work.**

# OkHttp/Okio (if present via Google libraries)
-dontwarn okhttp3.**
-dontwarn okio.**

# Apache HttpClient optional desktop-only pieces referenced transitively by google-http-client-apache-v2
-dontwarn org.apache.http.**
-dontwarn org.ietf.jgss.**
-dontwarn javax.naming.**
-dontwarn javax.naming.directory.**
-dontwarn javax.naming.ldap.**
