cmake_minimum_required(VERSION 3.18)
project(lingmiao_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ── 编译选项 ──
set(CMAKE_CXX_FLAGS_RELEASE "-O3 -flto -fvisibility=hidden -fomit-frame-pointer")
set(CMAKE_C_FLAGS_RELEASE   "-O3 -flto -fvisibility=hidden")

# ── 源文件 ──
set(SOURCES
    lingmiao_engine.cpp
)

add_library(lingmiao_engine SHARED ${SOURCES})

# ── 链接 Android log ──
find_library(LOG_LIB log)
target_link_libraries(lingmiao_engine ${LOG_LIB})

# ── 输出名 ──
set_target_properties(lingmiao_engine PROPERTIES
    OUTPUT_NAME "lingmiao_engine"
    PREFIX "lib"
    SUFFIX ".so"
)
