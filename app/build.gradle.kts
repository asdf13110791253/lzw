plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lingmiao.v2"
    compileSdk = 34
    ndkVersion = "25.2.9519653"
    ndkPath = System.getenv("ANDROID_NDK_HOME") ?: "/usr/local/lib/android/sdk/ndk/25.2.9519653"

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
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        create("release") {
            // 正式发布再填
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

    packaging {
        jniLibs {
            useLegacyPackaging = false
            excludes += "**/libc++_shared.so"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Compose依赖全部写死版本，无动态解析
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.ui:ui-graphics:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
    // ✅ 显式声明Compose Compiler，优先级高于插件默认引入的版本
    implementation("androidx.compose.compiler:compiler:1.5.14")

    implementation("org.opencv:opencv:4.9.0")
}

// ✅ Gradle最高优先级锁定：比插件、传递依赖的优先级都高，直接替换所有旧版本
configurations.all {
    resolutionStrategy {
        // 强制指定Compose Compiler版本，插件偷偷拉的1.3.2会被直接替换为1.5.14
        force("androidx.compose.compiler:compiler:1.5.14")
        // 兜底锁定，双重保险
        eachDependency { details ->
            if (details.requested.group == "androidx.compose.compiler" &&
                details.requested.name == "compiler") {
                details.useVersion("1.5.14")
            }
        }
    }
}
