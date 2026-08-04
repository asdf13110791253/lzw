# 🎱 灵喵 LingMiao —— 通用型台球辅助

> **不依赖任何特定游戏UI，纯图像识别，支持任意台球APP**

## 📐 技术架构

```
┌─────────────────────────────────────────────────────────┐
│  UI 层 (MainActivity / SettingsActivity / Calibration)   │  Compose UI
├─────────────────────────────────────────────────────────┤
│  Service 层                                              │
│  ├── CaptureService (MediaProjection 抓帧 → 前台服务)    │  :capture 进程
│  └── OverlayService  (SurfaceView 绘制 → 前台服务)     │  :overlay 进程
├─────────────────────────────────────────────────────────┤
│  Core 层 (Kotlin)                                        │
│  ├── BallDetector     (JNI 桥接 + 结果解析)             │
│  ├── PhysicsEngine   (镜像点法 + 角度补偿 + 翻袋)       │
│  └── AppConfig       (SharedPreferences 全局配置)         │
├─────────────────────────────────────────────────────────┤
│  Native 层 (C++17)                                      │
│  ├── native_detect.cpp    (HSV分割 + 霍夫圆变换)        │
│  ├── native_physics.cpp   (透视矩阵 + 镜像点法)          │
│  ├── native-lib.cpp      (JNI 入口)                     │
│  └── utils.cpp           (Bitmap 工具)                   │
└─────────────────────────────────────────────────────────┘
```

## 🚀 使用流程

1. **打开 APP** → 点击「开启辅助」
2. **授权悬浮窗权限** → 系统弹窗点「允许」
3. **授权录屏权限** → 系统弹窗点「立即开始」
4. **打开任意台球游戏** → 辅助线自动出现
5. **(可选) 桌布校准** → 点击球桌四个角，提高精度

## 🔧 核心参数说明

| 参数 | 作用 | 推荐值 |
|---|---|---|
| V (亮度阈值) | 识别白球的最低亮度 | 200-255 |
| S (饱和度阈值) | 排除彩色球的干扰 | 0-30 |
| P (圆检测灵敏度) | 霍夫圆累加器阈值 | 10-30 |
| 反射模式 | 镜像反射 vs 角度补偿 | 角度补偿 |
| 翻袋库数 | 库边反弹次数 | 1-3 |

## 📋 权限清单

- `SYSTEM_ALERT_WINDOW` — 悬浮窗（画辅助线）
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` — 录屏（Android 14+）
- `FOREGROUND_SERVICE` — 前台服务保活
- `POST_NOTIFICATIONS` — 通知栏常驻

## ⚠️ 注意事项

- **仅供学习研究**，请勿用于商业或破坏游戏平衡
- 不同游戏画面风格不同，可能需要调整 HSV 阈值
- 建议将 APP 加入电池白名单，防止被杀后台
- 首次使用建议先做桌布校准

## 🛠️ 编译要求

- Android Studio Hedgehog+
- JDK 17
- NDK 25+
- Gradle 8.4
- CMake 3.22+
- compileSdk 34

## 🤖 GitHub Actions 自动打包

本项目内置 GitHub Actions 工作流（`.github/workflows/build.yml`），上传代码后**自动编译 APK**，无需本地安装编译器。

### 使用方式

1. 在 GitHub 上新建仓库（如 `LingMiao`）
2. 将本项目所有文件推送到仓库
3. 进入仓库 → **Actions** 标签页 → 等待 5~10 分钟
4. 构建完成后，在 **Artifacts** 区域下载 `LingMiao-Debug` APK

### 手动触发

进入 **Actions → Build APK → Run workflow**，无需修改代码即可重新编译。

### 每次推送自动构建

```bash
git add .
git commit -m "优化：提升球体识别准确率"
git push
# → 自动触发构建 → 等待 → 下载最新 APK
```

详细说明见 `GITHUB_UPLOAD_GUIDE.md`。

## 📄 License

MIT License —— 仅供学习研究使用
