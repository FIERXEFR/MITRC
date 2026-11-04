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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Native (JNI) - keep the generated JNI method names intact
-keep class com.mitrc.ac.in.utils.NativeUtils { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep annotations and signatures used by kotlinx.serialization and Firebase.
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations,AnnotationDefault

# Enum helpers used by serializers and app code.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# kotlinx.serialization: generated serializers and their companions.
-keep,includedescriptorclasses class com.mitrc.ac.in.**$$serializer { *; }
-keepclassmembers class com.mitrc.ac.in.** {
    *** Companion;
}
-keepclasseswithmembers class com.mitrc.ac.in.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil loads components through its public API; keep its entry points intact.
-keep class coil.** { *; }
-dontwarn coil.**

# Optional dependencies referenced by Ktor/Supabase but not shipped in the app.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**