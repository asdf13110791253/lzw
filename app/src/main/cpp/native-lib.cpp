#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#define LOG_TAG "LingMiao-Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

// -------------------------------
// 1. 基础测试：验证 JNI + OpenCV
// -------------------------------
JNIEXPORT jstring JNICALL
Java_com_lingmiao_v2_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    // 创建一个 OpenCV Mat 矩阵
    cv::Mat kernel = cv::Mat::eye(3, 3, CV_32FC1);

    LOGD("OpenCV loaded successfully!");
    LOGD("Kernel size: %dx%d", kernel.rows, kernel.cols);

    std::string hello = "LingMiao V2 | OpenCV ";
    hello += cv::getVersionString();
    hello += " | Mat size: ";
    hello += std::to_string(kernel.total());

    return env->NewStringUTF(hello.c_str());
}

// --------------------------------------------------
// 2. 实战函数：Bitmap 转灰度（CV 核心能力演示）
//    对应 Java: native fun bitmapToGray(bitmap): Bitmap
// --------------------------------------------------
JNIEXPORT jboolean JNICALL
Java_com_lingmiao_v2_MainActivity_bitmapToGray(
        JNIEnv* env,
        jobject /* this */,
        jobject bitmap) {

    if (bitmap == nullptr) {
        LOGE("Bitmap is null");
        return JNI_FALSE;
    }

    AndroidBitmapInfo info;
    void* pixels = nullptr;

    // 1. 获取 Bitmap 信息
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("AndroidBitmap_getInfo failed");
        return JNI_FALSE;
    }

    // 2. 锁定 Bitmap 像素
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("AndroidBitmap_lockPixels failed");
        return JNI_FALSE;
    }

    // 3. Bitmap -> cv::Mat
    cv::Mat src(info.height, info.width, CV_8UC4, pixels);

    // 4. 转为灰度图
    cv::Mat gray;
    cv::cvtColor(src, gray, cv::COLOR_RGBA2GRAY);

    // 5. 灰度图 -> 写回原 Bitmap (RGBA)
    cv::cvtColor(gray, src, cv::COLOR_GRAY2RGBA);

    // 6. 解锁像素
    AndroidBitmap_unlockPixels(env, bitmap);

    LOGD("Bitmap converted to Gray: %dx%d", info.width, info.height);
    return JNI_TRUE;
}

} // extern "C"
