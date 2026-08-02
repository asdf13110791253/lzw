cmake_minimum_required(VERSION 3.22)
project("lingmiao")

set(OPENCV_SDK_PATH "$ENV{OPENCV_SDK_PATH}")
if(NOT EXISTS "${OPENCV_SDK_PATH}")
    message(FATAL_ERROR "❌ OpenCV SDK not found at: ${OPENCV_SDK_PATH}")
endif()

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

target_link_libraries(
    native-lib
    ${OpenCV_LIBS}
    android
    log
    jnigraphics
)
