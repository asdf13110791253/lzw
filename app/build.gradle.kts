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

    // ✅ 所有Compose依赖写死版本，无动态解析
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.ui:ui-graphics:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
    // ✅ 显式声明Compose编译器，优先级极高
    implementation("androidx.compose.compiler:compiler:1.5.14")

    implementation("org.opencv:opencv:4.9.0")
}

// ✅ Gradle最高优先级锁定：比插件、传递依赖的优先级都高，直接强制替换所有匹配模块的版本
configurations.all {
    resolutionStrategy {
        // 强制指定Compose编译器版本，任何地方引入的旧版本（比如1.3.2）都会被直接替换
        forcedModules.add("androidx.compose.compiler:compiler:1.5.14")
        // 兜底锁定，双重保险
        eachDependency { details ->
            if (details.requested.group == "androidx.compose.compiler" &&
                details.requested.name == "compiler") {
                details.useVersion("1.5.14")
            }
        }
    }
}
