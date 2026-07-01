# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/steven/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Some Xiaomi devices seem to ship an outdated apache commons that's exported on the classpath.
# Obfuscate the names in this package so their's isn't accidentally used
-keepnames class !org.apache.commons.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class android.support.v7.widget.SearchView { *; }
-keeppackagenames org.jsoup.nodes

# Spongy Castle
# Not sure how much of this is really needed but it removes enough to keep us under the 64K dex limit at least.
-keep class org.spongycastle.crypto.* {*;}
-keep class org.spongycastle.crypto.agreement.** {*;}
-keep class org.spongycastle.crypto.digests.* {*;}
-keep class org.spongycastle.crypto.ec.* {*;}
-keep class org.spongycastle.crypto.encodings.* {*;}
-keep class org.spongycastle.crypto.engines.* {*;}
-keep class org.spongycastle.crypto.macs.* {*;}
-keep class org.spongycastle.crypto.modes.* {*;}
-keep class org.spongycastle.crypto.paddings.* {*;}
-keep class org.spongycastle.crypto.params.* {*;}
-keep class org.spongycastle.crypto.prng.* {*;}
-keep class org.spongycastle.crypto.signers.* {*;}

-keep class org.spongycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.dh.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.ec.* {*;}
-keep class org.spongycastle.jcajce.provider.asymmetric.rsa.* {*;}

-keep class org.spongycastle.jcajce.provider.digest.** {*;}
-keep class org.spongycastle.jcajce.provider.keystore.** {*;}
-keep class org.spongycastle.jcajce.provider.symmetric.** {*;}
-keep class org.spongycastle.jcajce.spec.* {*;}
-keep class org.spongycastle.jce.** {*;}

-dontwarn org.spongycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.spongycastle.x509.util.LDAPStoreHelper
# End Spongy Castle

# JavaSteam — uses reflection, protobuf, and a callback system that all break under R8.
-keep class in.dragonbra.javasteam.** { *; }
-dontwarn in.dragonbra.javasteam.**

# Protobuf — JavaSteam's wire protocol depends on field names surviving obfuscation.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn android.test.**
-dontwarn org.junit.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain checked exceptions.
-keepattributes Exceptions
# Keep the SteamAPI interface and its annotated methods intact — Retrofit creates a
# dynamic proxy from this at runtime; obfuscating it causes ClassCastException.
-keep interface com.steevsapps.idledaddy.steam.SteamAPI { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
# Keep Gson model classes used as Retrofit response bodies.
-keep class com.steevsapps.idledaddy.steam.model.** { *; }
# Keep custom Retrofit converter so VdfConverterFactory survives R8.
-keep class com.steevsapps.idledaddy.steam.converter.** { *; }
# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement