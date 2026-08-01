-keep class com.lingmiao.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn androidx.**
-dontwarn android.**
-dontwarn org.opencv.**
