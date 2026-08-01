# ============================================
# 灵喵 LingMiao V2.0 ProGuard / R8 混淆规则
# ============================================

# ── 基础 Android 规则 ──
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Room 数据库 ──
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Jetpack Compose ──
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# ── Native 方法（必须保留！）──
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── OpenCV ──
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# ── TensorFlow Lite ──
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**
-keep class com.google.flatbuffers.** { *; }

# ── 反射用到的模型类 ──
-keep class com.lingmiao.v2.data.entity.** { *; }

# ── 事件总线 ──
-keep class com.lingmiao.v2.core.event.** { *; }

# ── 枚举 ──
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── WebView（如果有）──
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

# ── 移除日志（release 时）──
-assumenosideeffects class com.lingmiao.v2.core.log.LogManager {
    public static void v(...);
    public static void d(...);
}

# ── 优化 ──
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses 'l'
