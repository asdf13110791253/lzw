# 1. 指定 CMake 最低版本（AGP 8.x 推荐 3.22+）
cmake_minimum_required(VERSION 3.22)

# 2. 项目名称（随意，不影响最终产物）
project("lingmiao")

# 3. ✅ 关键：通过 Prefab 查找 Maven 下载的 OpenCV
#    不需要手动指定 OpenCV 路径，Gradle 会自动注入
find_package(OpenCV REQUIRED)

# 4. 打印 OpenCV 信息（构建时可见，用于 Debug）
message(STATUS "OpenCV Include Dir: ${OpenCV_INCLUDE_DIRS}")
message(STATUS "OpenCV Libraries: ${OpenCV_LIBRARIES}")

# 5. 创建你的 Native 动态库
add_library(
        native-lib           # 库名称，最终生成 libnative-lib.so
        SHARED               # 动态库
        native-lib.cpp       # 你的 C++ 源文件
)

# 6. 引入头文件路径
target_include_directories(
        native-lib
        PRIVATE
        ${OpenCV_INCLUDE_DIRS}
)

# 7. ✅ 链接库
#    - OpenCV 自动注入的库
#    - Android 系统库
#    - C++ 运行时（与 build.gradle.kts 中的 c++_shared 对应）
target_link_libraries(
        native-lib
        ${OpenCV_LIBRARIES}
        android
        log
        c++_shared
)
