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

# --- Global attributes ---
# SourceFile/LineNumberTable: line numbers for release stack traces (de-obfuscate via mapping.txt).
# Signature/Exceptions: generic-type + checked-exception metadata Retrofit reads reflectively.
-keepattributes SourceFile,LineNumberTable,Signature,Exceptions
# Hide original source file names in stack traces; mapping.txt still retraces them.
-renamesourcefileattribute SourceFile

# jsoup loads parser resources by package path, so its package names must survive.
-keeppackagenames org.jsoup.nodes

# --- Bouncy Castle ---
# The JCE provider is registered and invoked reflectively (by algorithm name), so the provider
# and crypto primitives must be kept with their members intact.
-keep class org.bouncycastle.crypto.* {*;}
-keep class org.bouncycastle.crypto.agreement.** {*;}
-keep class org.bouncycastle.crypto.digests.* {*;}
-keep class org.bouncycastle.crypto.ec.* {*;}
-keep class org.bouncycastle.crypto.encodings.* {*;}
-keep class org.bouncycastle.crypto.engines.* {*;}
-keep class org.bouncycastle.crypto.macs.* {*;}
-keep class org.bouncycastle.crypto.modes.* {*;}
-keep class org.bouncycastle.crypto.paddings.* {*;}
-keep class org.bouncycastle.crypto.params.* {*;}
-keep class org.bouncycastle.crypto.prng.* {*;}
-keep class org.bouncycastle.crypto.signers.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.util.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.dh.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.ec.* {*;}
-keep class org.bouncycastle.jcajce.provider.asymmetric.rsa.* {*;}
-keep class org.bouncycastle.jcajce.provider.digest.** {*;}
-keep class org.bouncycastle.jcajce.provider.keystore.** {*;}
-keep class org.bouncycastle.jcajce.provider.symmetric.** {*;}
-keep class org.bouncycastle.jcajce.spec.* {*;}
-keep class org.bouncycastle.jce.** {*;}
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper

# --- R8 Mappings File ---
# -printseeds seeds.txt

# --- JavaSteam ---
# Reflection, protobuf wire types, and a name-based callback system all break under R8.
-keep class in.dragonbra.javasteam.** { *; }
-dontwarn in.dragonbra.javasteam.**

# --- Protobuf ---
# JavaSteam's wire protocol depends on field names surviving obfuscation.
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# --- OkHttp / Okio (Retrofit + Coil transport) ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# PublicSuffixDatabase loads a resource by relative path; keep its package.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# --- Retrofit ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
# Retrofit builds a dynamic proxy from this interface at runtime; obfuscation -> ClassCastException.
-keep interface com.steevsapps.idledaddy.steam.SteamAPI { *; }
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
# kotlinx.serialization model classes used as Retrofit response bodies + the custom VDF converter.
-keep class com.steevsapps.idledaddy.steam.model.** { *; }
-keep class com.steevsapps.idledaddy.steam.converter.** { *; }