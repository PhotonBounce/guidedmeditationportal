# Keep Billing classes
-keep class com.android.billingclient.** { *; }

# Keep app classes from obfuscation for debugging
-keepattributes SourceFile,LineNumberTable

# AdMob
-keep public class com.google.android.gms.ads.** { public *; }
