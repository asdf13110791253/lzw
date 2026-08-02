// 根级 build.gradle.kts
// AGP 不再声明在 buildscript 里，
// 改由 settings.gradle.kts 的 pluginManagement 统一管理，
// 避免 Gradle 9.x 报 "plugin already on classpath with unknown version"。

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // 只保留 Kotlin Gradle Plugin，AGP 交给 settings.gradle.kts 管理
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    }
}

// 干净的根脚本，不再有 allprojects / subprojects 仓库声明
// 所有仓库（google / mavenCentral / jitpack）统一在 settings.gradle.kts 里配置

// clean 任务：使用 Gradle 推荐的 register 写法
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
