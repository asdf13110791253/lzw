pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.2"
        id("com.android.library") version "8.5.2"
        id("org.jetbrains.kotlin.android") version "1.9.24"
    }
}

dependencyResolutionManagement {
    // ✅ PREFER_SETTINGS：允许项目级仓库存在，不报错
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // ✅ JitPack：OpenCV 轻量包在这里
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "LingMiaoV2"
include(":app")
