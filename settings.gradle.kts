pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // ✅ 关键：在这里声明插件版本，避免 Gradle 9.x 报
    // "plugin already on classpath with unknown version"
    plugins {
        id("com.android.application") version "8.5.2"
        id("com.android.library")     version "8.5.2"
        id("org.jetbrains.kotlin.android") version "1.9.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // 👇 新增这一行：OpenCV的专用下载地址，之前缺的就是它！
        maven { url = uri("https://maven.aliyun.com/repository/opencv") }

    }
}


rootProject.name = "LingMiaoV2"
include(":app")
