# 保留行号，崩溃方便排查
-keepattributes SourceFile,LineNumberTable

# 保留四大组件、Application不被混淆
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 完整保留你的项目所有代码
-keep class com.lingmiao.** { *; }

# OpenCV 忽略警告不混淆
-dontwarn org.opencv.**
-keep class org.opencv.** { *; }

# 安卓基础库不报错
-dontwarn androidx.**
-keep class androidx.** {*;}
