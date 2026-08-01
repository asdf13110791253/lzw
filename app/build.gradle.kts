// ... 文件头部 plugins 和 android 配置不变 ...

android {
    namespace = "com.lingmiao.v2"
    compileSdk = 34

    defaultConfig {
        // ... 其他配置 ...
        
        // 关键：启用 Prefab，让 CMake 能找到 Maven 下载的 OpenCV
        buildFeatures {
            compose = true
            buildConfig = true
            prefab = true // 👈 加上这一行
        }

        externalNativeBuild {
            cmake {
                cppFlags "-std=c++17", "-O3", "-flto"
                arguments "-DANDROID_STL=c++_shared"
                abiFilters "arm64-v8a"
            }
        }
    }

    // ... signingConfigs, buildTypes 等配置 ...
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // TFLite (取消注释，启用)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // 👇👇👇 关键修改：使用 OpenCV 5.0.0 Maven 依赖 👇👇👇
    implementation("org.opencv:opencv:5.0.0")
    
    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
