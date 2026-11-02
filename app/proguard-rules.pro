# Modified by Vamos (2026): release/perf entry points and runtime reflection.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class com.yongpeng.vamos.shell.HelperMain { public static void main(java.lang.String[]); }
-keep class com.yongpeng.vamos.** extends android.os.Binder { *; }
-keep interface com.yongpeng.vamos.** extends android.os.IInterface { *; }
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class net.i2p.crypto.eddsa.** { *; }
-assumenosideeffects class android.util.Log {
    public static int d(...) return 0;
    public static int i(...) return 0;
}
# EdDSA 0.3.0 has an optional OpenJDK X509Key compatibility branch, absent on Android.
# Vamos passes EdDSAPublicKey built from raw Ed25519 bytes and never enters that branch.
-dontwarn sun.security.x509.X509Key
