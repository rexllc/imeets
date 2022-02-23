# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

-repackageclasses
-ignorewarnings
-dontwarn
-dontnote
-optimizations !method/removal/parameter

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Debug
-keepattributes SourceFile, LineNumberTable
-keepattributes *Annotation*, Signature, Exception

# Okhttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontnote okhttp3.**

# GSon
-keep class com.google.gson.stream.** { *; }

# androidx
-keep class androidx.** { *; }

# javax，java
-keep class java.** { *; }
-keep class javax.** { *; }

# Kotlin
-keep class kotlin.** {*;}