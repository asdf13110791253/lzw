# 灵喵 LingMiao V2.0

> 台球辅助 APP — 实时悬浮窗分析台球轨迹
> Android 9+ | Kotlin + Jetpack Compose | NDK C++ | Room

## ✨ 功能一览

- ✅ 自动判定母球与子球最优路线
- ✅ 直球 / 反带球 / 多库翻袋 (1-5 库, BFS)
- ✅ 透视矫正 (梯形 → 矩形, DLT 算法)
- ✅ 自适应球桌大小 (任意分辨率 / 小窗模式)
- ✅ 60fps 悬浮窗渲染 (SurfaceView 硬件加速)
- ✅ 前台服务保活 & 开机自启
- ✅ 4 种球检测方案融合 + 8 套参数预设
- ✅ 多语言 (中 / 英 / 日)
- ✅ 三级 Native 加载兜底

## 📦 体积目标：22MB ± 0.5MB

| 模块 | 大小 | 占比 |
|---|---|---|
| OpenCV (裁剪后) | ~5.9 MB | 27% |
| DEX 代码 | ~3.0 MB | 14% |
| TFLite 模型 (4个) | ~4.0 MB | 19% |
| 资源文件 | ~1.5 MB | 7% |
| 其他 | ~2.5 MB | 12% |
| R8 压缩优化 | -1.5 MB | -7% |
| **合计** | **~21.5 MB** | 100% |

---

## 🚀 纯手机云端打包（你只需要做这些）

> **没有电脑？没关系。只有一步需要电脑帮忙（5分钟），其余全在手机上点就行。**

### 唯一需要电脑的一步（借朋友的也行）

让电脑帮你生成"钥匙"并传给你手机：

1. 电脑打开浏览器 → 下载安装 Git：**https://git-scm.com/download/win**
2. 按 `Win+R` → 输入 `cmd` → 回车
3. 粘贴这行，回车：
   ```
   keytool -genkey -v -keystore lingmiao.keystore -alias lingmiao -keyalg RSA -keysize 2048 -validity 10000
   ```
4. 密码填 `LingMiao2024!`（不显示正常），其他回车跳过
5. 再粘贴这行，回车：
   ```
   certutil -encode lingmiao.keystore keystore_b64.txt
   ```
6. 用记事本打开 `keystore_b64.txt` → **全选复制内容** → 微信发给自己
7. 把 `lingmiao.keystore` 文件也微信发给自己

> 电脑的事完了。剩下全在手机上。

### 手机上的操作（共7步）

#### ① 下载3个APP
- **GitHub**（蓝色图标，应用商店搜）
- **MT管理器**（解压用）
- **微信/QQ**（你有了）

#### ② 注册 GitHub
- 手机浏览器打开 **github.com** → Sign up → 用邮箱注册

#### ③ 建仓库
- 登录后右上角 `+` → New repository
- 名字：`LingMiaoV2` → 选 **Private** → Create

#### ④ 上传我给你的 ZIP
- 仓库页面找 **"uploading an existing file"** → 点它
- 把 `LingMiaoV2.zip` 拖进去 → 等上传完 → 点 **Commit changes**

#### ⑤ 填4个密钥（Settings → Secrets → Actions）
| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | 微信里那段"乱码"全选粘贴 |
| `KEYSTORE_PASSWORD` | `LingMiao2024!` |
| `KEY_PASSWORD` | `LingMiao2024!` |
| `KEY_ALIAS` | `lingmiao` |

#### ⑥ 点一下开始打包
- 右侧栏找 **Releases** → **Create a new release**
- Tag 填 `v1.0` → Title 填 `LingMiao V2.0` → 点 **Publish release**

#### ⑦ 等15分钟 → 下载APK
- 点顶部 **Actions** → 看黄色圆圈转 → 变绿色对勾 ✅
- 点进去 → 底部 **Artifacts** → 下载 APK → 安装！

---

## 🔧 CI 云端自动做了什么？

你点下 Publish 之后，GitHub 的免费云电脑会自动：

1. ✅ 下载 JDK 17
2. ✅ 下载 Android SDK 34 + NDK r27d + CMake
3. ✅ 下载 OpenCV 4.9.0 源码 → 裁剪编译（仅 core+imgproc，~5.9MB）
4. ✅ 编译 `liblingmiao_engine.so`（JNI 桥接引擎）
5. ✅ 处理 TFLite 模型文件
6. ✅ Gradle 编译 + R8 混淆压缩 + 资源裁剪
7. ✅ 用你的密钥签名 APK
8. ✅ 检查体积是否在 20-24MB 之间
9. ✅ 上传最终 APK 供你下载

**你什么都不用管。**

---

## 📁 项目结构

```
LingMiaoV2/
├── .github/workflows/build.yml   ← CI 自动打包脚本（核心）
├── app/
│   ├── build.gradle.kts          ← R8压缩 + ABI过滤(仅arm64) + 签名
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lingmiao/v2/  ← 21个Kt源码文件
│       ├── jni/                     ← C++源码(云端自动编译)
│       │   ├── CMakeLists.txt
│       │   └── lingmiao_engine.cpp
│       ├── assets/                  ← Haar/TFLite/桌布纹理
│       ├── jniLibs/arm64-v8a/      ← 云端自动放入编译好的.so
│       └── res/                     ← 图标/音效/字符串/布局
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
└── docs/opencv_build_guide.md
```

---

## ❓ 出问题了？

| 现象 | 解决 |
|---|---|
| Actions 没反应 | Settings → Actions → 点 "I understand, enable" |
| 红色叉号 + "secret not found" | 回去检查4个密钥是否都填了 |
| 红色叉号 + "build failed" | Actions 页面 → 右上角 "Re-run all jobs" 重试 |
| 红色叉号 + "size check failed" | 通知开发者修复，你只需重新点 Release |
| APK 安装不了 | 设置 → 安全 → 允许安装未知来源 → 打开 |
| 打开闪退 | 通常重试一次构建就好 |

---

## 📄 详细指南

- **纯手机操作完整指南**：见压缩包内的 `灵喵打包指南_纯手机版.txt`
- **OpenCV 裁剪编译指南**：见 `docs/opencv_build_guide.md`

---

**你只需要：上传 → 填密钥 → 点一下 → 等 → 下载安装。就这么简单。**
