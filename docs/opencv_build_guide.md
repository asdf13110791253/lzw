# OpenCV 裁剪编译指南

> 目标：将 `libopencv_java4.so` 从 ~25MB 裁到 ~5.9MB

## 1. 准备环境

```bash
# Ubuntu/Debian
sudo apt-get install -y build-essential cmake ninja-build

# macOS
brew install cmake ninja
```

## 2. 下载 OpenCV 4.9.0 源码

```bash
wget https://github.com/opencv/opencv/archive/4.9.0.tar.gz
tar -xzf 4.9.0.tar.gz
cd opencv-4.9.0
```

## 3. CMake 配置（关键参数）

```bash
mkdir build && cd build

cmake \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DBUILD_LIST=core,imgproc \
  -DBUILD_opencv_java=ON \
  -DBUILD_ANDROID_PROJECTS=OFF \
  -DBUILD_TESTS=OFF \
  -DBUILD_PERF_TESTS=OFF \
  -DBUILD_EXAMPLES=OFF \
  -DBUILD_DOCS=OFF \
  -DWITH_OPENCL=OFF \
  -DWITH_IPP=OFF \
  -DWITH_TBB=OFF \
  -DWITH_EIGEN=OFF \
  -DWITH_GTK=OFF \
  -DWITH_CUDA=OFF \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_CXX_FLAGS_RELEASE="-O3 -flto -fvisibility=hidden -ffunction-sections -fdata-sections" \
  -DCMAKE_C_FLAGS_RELEASE="-O3 -flto -fvisibility=hidden -ffunction-sections -fdata-sections" \
  -DCMAKE_SHARED_LINKER_FLAGS="-Wl,--gc-sections,--strip-all" \
  -GNinja \
  ..
```

### 参数说明

| 参数 | 作用 | 体积影响 |
|---|---|---|
| `BUILD_LIST=core,imgproc` | 只编 2 个模块 | **-15MB** |
| `WITH_OPENCL=OFF` | 去掉 OpenCL | -2MB |
| `-flto` | 链接时优化 | -1MB |
| `-fvisibility=hidden` | 隐藏符号 | -0.5MB |
| `--gc-sections` | 删除未用代码段 | -1MB |

## 4. 编译

```bash
ninja -j$(nproc)
```

## 5. 取出 so 文件

```bash
# Java 绑定库
find . -name "libopencv_java4.so" -exec cp {} libopencv_java4.so \;

# 查看大小
ls -lh libopencv_java4.so
# 预期: ~5.9MB
```

## 6. 放入项目

```bash
cp libopencv_java4.so /path/to/LingMiaoV2/app/src/main/jniLibs/arm64-v8a/
```

## 7. 验证

```bash
# 检查 so 架构
file libopencv_java4.so
# 应显示: ELF 64-bit LSB shared object, ARM aarch64

# 检查导出符号（应有 Java_org_opencv_ 前缀）
nm -D libopencv_java4.so | grep "Java_org_opencv" | head -5
```

## 8. 常见问题

### Q: so 还是很大（>10MB）？
A: 检查是否漏了 `-flto` 或 `--gc-sections`，这两个最关键。

### Q: 运行时报 `UnsatisfiedLinkError`？
A: 确认 so 是 arm64-v8a 架构，且放在 `jniLibs/arm64-v8a/` 目录下。

### Q: 需要 OpenCV 的 video/features2d 模块怎么办？
A: 把模块名加到 `BUILD_LIST`，例如 `BUILD_LIST=core,imgproc,video,features2d`。
注意：每多一个模块大约增加 2-5MB。

## 9. CI 自动编译

`.github/workflows/build.yml` 已配置自动下载+裁剪 OpenCV，
无需手动操作。本地开发时按上述步骤手动编译即可。
