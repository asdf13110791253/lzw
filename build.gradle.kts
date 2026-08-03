// 根级 build.gradle.kts（最终纯净版 · 针对 OpenCV 冲突修正）
// ============================================================
// ✅ 无任何仓库配置，完全符合 FAIL_ON_PROJECT_REPOS 严格模式
// ✅ 所有仓库由 settings.gradle.kts 统一管理
// ✅ 【新增】强制锁定 Compose Compiler 版本，阻断 OpenCV 传递性依赖冲突
// ============================================================

// 强制所有子项目使用指定版本的 Compose Compiler，覆盖 OpenCV 引入的旧版本
allprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.compose.compiler:compiler:1.5.14")
        }
    }
}

// clean 任务：Gradle 推荐写法
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
