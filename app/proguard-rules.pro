# ─────────────────────────────────────────────────────────────────────────
# SoundPad — Release ProGuard / R8 rules
# minifyEnabled + shrinkResources are ON for release. These rules ensure
# Billing, AdMob, and reflective code don't crash after obfuscation.
# ─────────────────────────────────────────────────────────────────────────

# Keep source line numbers for Play Console crash symbolication
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ── Google Play Billing ─────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Google Play Services & AdMob ────────────────────────────────────────
-keep public class com.google.android.gms.ads.** { public *; }
-keep public class com.google.android.gms.common.** { public *; }
-keep class com.google.android.gms.internal.ads.** { *; }
-dontwarn com.google.android.gms.**

# ── User Messaging Platform (UMP — GDPR/CCPA consent) ───────────────────
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

# ── Play In-App Review ──────────────────────────────────────────────────
-keep class com.google.android.play.core.review.** { *; }
-dontwarn com.google.android.play.core.**

# ── AndroidX & Material ─────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class com.google.android.material.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**

# ── Kotlin coroutines ───────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── App: keep models / enums used reflectively via Kotlin's valueOf ────
-keep class com.soundpad.sleep.SoundType { *; }
-keepclassmembers enum com.soundpad.sleep.** { *; }

# ── Native methods (just in case any library adds them) ────────────────
-keepclasseswithmembernames class * { native <methods>; }

# ── Parcelables (none yet, but future-proof) ───────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
