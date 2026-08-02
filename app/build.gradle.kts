plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lingmiao.v2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lingmiao.v2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildFeatures {
            compose = true
            buildConfig = true
            prefab = true
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-O3", "-flto")
                arguments("-DANDROID_STL=c++_shared")
                abiFilters("arm64-v8a")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            // storeFile = file("release-key.jks")
            // storePassword = "your_password"
            // keyAlias = "your_alias"
            // keyPassword = "your_password"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // ✅ Kotlin 1.9.24 官方对应 Compose Compiler 1.5.14
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // AndroidX 基础
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.1")

    // ✅ Compose BOM（统一管理 Compose 库版本）
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ✅ OpenCV 4.9.0（Maven Central 官方包 + Prefab）
    implementation("org.opencv:opencv:4.9.0")
}

// ✅✅✅ 关键修复：强制锁定 Compose Compiler 版本
// 防止某个传递依赖把 compiler 降级到 1.3.2，导致与 Kotlin 1.9.24 冲突
configurations.all {
    resolutionStrategy {
        force("androidx.compose.compiler:compiler:1.5.14")
    }
}
