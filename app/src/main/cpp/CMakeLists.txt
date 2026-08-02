cmake_minimum_required(VERSION 3.22)
project("lingmiao")

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

# ✅ 不需要 OpenCV 头文件了（用 JitPack 的 AAR，Java 层调用）
# 如果你 C++ 里确实要用 OpenCV，再单独处理

target_link_libraries(
    native-lib
    android
    log
    jnigraphics
)
