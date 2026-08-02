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
        // 👇 新增清华公开OpenCV镜像，完全免费不需要登录
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/opencv/maven2/") }
    }
}



rootProject.name = "LingMiaoV2"
include(":app")
