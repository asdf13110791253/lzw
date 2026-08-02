// 根级 build.gradle.kts
// ============================================================
// ✅ 终极净化版：彻底删除 buildscript 块
// ✅ 所有插件版本由 settings.gradle.kts 统一管理
// ✅ 根脚本只负责子模块仓库配置 + clean 任务
// ✅ 与 Gradle 8.x / 9.x 100% 兼容，永不报 plugin conflict
// ============================================================

// 所有子模块共用的仓库（app 模块自动继承）
subprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// clean 任务：Gradle 推荐写法
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
