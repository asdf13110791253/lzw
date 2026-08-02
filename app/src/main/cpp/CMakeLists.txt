cmake_minimum_required(VERSION 3.22)

project("lingmiao")

# ✅ 从环境变量读取 OpenCV SDK 路径
set(OPENCV_SDK_PATH "$ENV{OPENCV_SDK_PATH}")

if(NOT EXISTS "${OPENCV_SDK_PATH}")
    message(FATAL_ERROR "❌ OpenCV SDK not found at: ${OPENCV_SDK_PATH}")
endif()

# ✅ 告诉 CMake 去哪里找 OpenCV 的配置文件
set(OpenCV_DIR "${OPENCV_SDK_PATH}/sdk/native/jni")
find_package(OpenCV REQUIRED)

message(STATUS "OpenCV Include Dir: ${OpenCV_INCLUDE_DIRS}")
message(STATUS "OpenCV Libraries: ${OpenCV_LIBS}")

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

target_include_directories(
    native-lib
    PRIVATE
    ${OpenCV_INCLUDE_DIRS}
)

# ✅✅✅ 关键修复：加上 jnigraphics
# AndroidBitmap_getInfo / lockPixels / unlockPixels 都在这个库里
target_link_libraries(
    native-lib
    ${OpenCV_LIBS}
    android
    log
    c++_shared
    jnigraphics    # ← 就加这一行！
)
