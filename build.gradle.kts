// 根级 build.gradle.kts（最终纯净版）
// ============================================================
// ✅ 无任何仓库配置，完全符合 FAIL_ON_PROJECT_REPOS 严格模式
// ✅ 所有仓库由 settings.gradle.kts 统一管理
// ============================================================

// clean 任务：Gradle 推荐写法
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
