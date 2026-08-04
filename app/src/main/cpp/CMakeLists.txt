cmake_minimum_required(VERSION 3.18)
project("lingmiao_native")

# ===== OpenCV 配置 =====
# 方式1: 如果你有 OpenCV Android SDK，设置路径
# set(OpenCV_DIR "/path/to/OpenCV-android-sdk/sdk/native/jni")
# find_package(OpenCV REQUIRED)

# 方式2: 使用 jitpack 的 OpenCV 包（推荐，自动下载）
# 在 app/build.gradle 中已添加 implementation 'com.github.OpenCV:OpenCV-Android:4.8.0'
# 这里用轻量级替代方案（如果不想依赖完整 OpenCV）

# 由于 OpenCV 在 Android 上通常通过 Gradle 依赖，
# CMake 这边我们用 NDK 自带的数学库 + 手写图像处理
# 这样可以不依赖外部 OpenCV 包，编译更简单

# ===== 源文件 =====
add_library(
    lingmiao_native
    SHARED
    native_detect.cpp
    native_physics.cpp
    native_perspective.cpp
    utils.cpp
)

# ===== 头文件目录 =====
target_include_directories(
    lingmiao_native
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# ===== 链接库 =====
target_link_libraries(
    lingmiao_native
    log
    jnigraphics  # Bitmap 操作
    android
    EGL
    GLESv2
)

# ===== 编译选项 =====
target_compile_options(
    lingmiao_native
    PRIVATE
    -std=c++17
    -O3
    -ffast-math
    -funroll-loops
    -fvisibility=hidden
)

# ===== 架构特定优化 =====
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(lingmiao_native PRIVATE -march=armv8-a+fp+simd -mtune=cortex-a75)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_options(lingmiao_native PRIVATE -march=armv7-a+neon -mfpu=neon-vfpv4 -mtune=cortex-a15)
endif()
