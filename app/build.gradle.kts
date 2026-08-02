plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lingmiao.v2"
    compileSdk = 34

    // ✅ NDK 版本和 CI 对齐
    ndkVersion = "25.2.9519653"

    defaultConfig {
        applicationId = "com.lingmiao.v2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildFeatures {
            compose = true
            buildConfig = true
            // ❌ 关掉 prefab，我们用 OpenCV Maven 包 + 手动 CMake 路径
            // prefab = true
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-O3", "-flto")
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DOPENCV_SDK_PATH=$System.env.OPENCV_SDK_PATH"
                )
                abiFilters("arm64-v8a")
            }
        }

        ndk { abiFilters += "arm64-v8a" }
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
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // ✅ 过滤 OpenCV 自带的 libc++_shared.so，用 NDK 自己的，解决冲突
    packaging {
        jniLibs {
            useLegacyPackaging = false
            excludes += "**/libc++_shared.so"
        }
    }
}

dependencies {
    // AndroidX 基础
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.1")

    // ✅ 手动指定 Compose 版本，不用 BOM，杜绝降级
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.ui:ui-graphics:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")

    // ✅ OpenCV 4.9.0 官方 Maven 包
    implementation("org.opencv:opencv:4.9.0")
}

// ✅✅✅ 终极锁定：精准命中 Compose 编译器，任何传递依赖都无法降级
configurations.all {
    resolutionStrategy {
        eachDependency { details ->
            if (details.requested.group == "androidx.compose.compiler" &&
                details.requested.name == "compiler") {
                details.useVersion("1.5.14")
            }
        }
    }
}
