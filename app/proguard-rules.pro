# --- MeteoTrack release rules -------------------------------------------------

# Keep line numbers so crash reports from users stay readable.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Moshi: DTOs are (de)serialised through generated adapters / Kotlin reflection.
-keep class com.example.data.model.** { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# Retrofit service interfaces are proxied at runtime.
-keep interface com.example.data.remote.OpenMeteoApi { *; }
-keep interface com.example.data.remote.OpenMeteoGeocodingApi { *; }

# Optional TLS providers referenced by OkHttp.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
