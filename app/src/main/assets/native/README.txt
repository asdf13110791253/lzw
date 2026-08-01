============================================
  灵喵 LingMiao V2.0 - Native 库说明
============================================

⚠️ 注意: Native .so 文件不在 assets/ 目录下!

正确位置: app/src/main/jniLibs/arm64-v8a/

详见: app/src/main/jniLibs/arm64-v8a/README.txt

============================================
  本目录仅作说明, 不存放任何二进制文件
============================================

原因:
  - .so 文件由 Android Gradle Plugin 自动打包到 APK 的
    lib/arm64-v8a/ 目录
  - 放在 assets/ 下需要手动加载, 增加复杂度
  - jniLibs/ 是 Android 标准 native 库目录

所需文件:
  1. libopencv_java4.so   → OpenCV 裁剪版 (~5.9MB)
  2. liblingmiao_engine.so → 自研 JNI 引擎 (~1-2MB)

详情见: jniLibs/arm64-v8a/README.txt
