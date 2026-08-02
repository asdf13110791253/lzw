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
            prefab = true
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

    // ✅ Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ✅ OpenCV 4.9.0
    implementation("org.opencv:opencv:4.9.0")

    // ✅ 显式声明 Compose 编译器（双保险）
    implementation("androidx.compose.compiler:compiler:1.5.14")
}

// ✅✅✅ 关键修复：精准锁定 Kotlin 编译路径下的 Compose Compiler
// 普通 configurations.all 管不到 Kotlin 编译专用依赖池
configurations.matching { it.name.contains("kotlinCompile", ignoreCase = true) }.all {
    resolutionStrategy {
        force("androidx.compose.compiler:compiler:1.5.14")
    }
}
