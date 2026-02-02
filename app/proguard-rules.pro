# Add project specific ProGuard rules here.

# Keep line number information for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Jsoup
-keeppackagenames org.jsoup.nodes

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep data classes
-keep class com.example.rssreader.data.local.entity.** { *; }
-keep class com.example.rssreader.domain.model.** { *; }
-keep class com.example.rssreader.network.RssParser$* { *; }
-keep class com.example.rssreader.network.FeedDiscovery$* { *; }
