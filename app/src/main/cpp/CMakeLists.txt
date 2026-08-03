cmake_minimum_required(VERSION 3.22)
project("lingmiao")

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
