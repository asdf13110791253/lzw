cmake_minimum_required(VERSION 3.22)
project("lingmiao")

# ✅ 通过 prefab 查找 OpenCV（来自 org.opencv:opencv:4.9.0 的 AAR）
find_package(OpenCV REQUIRED COMPONENTS OpenCV::opencv_java4)

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

target_link_libraries(
    native-lib
    OpenCV::opencv_java4
    android
    log
    jnigraphics
)
