// 根级 build.gradle.kts
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
    }
}

// 整个allprojects块删掉！仓库已经在settings.gradle.kts里全局配置了
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//         maven { url = uri("https://jitpack.io") }
//     }
// }

// 可选：把clean任务改成Gradle推荐的注册式写法（不改也能正常跑）
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
