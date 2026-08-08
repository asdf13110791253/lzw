/**
 * utils.cpp
 *
 * 通用工具函数库
 */

#include <jni.h>
#include <android/bitmap.h>  // 【已修复】必须加这行，不然找不到 AndroidBitmapInfo
#include <cmath>
#include <cstring>

// ===== 简单 Bitmap 操作工具 =====

// 获取 Bitmap 信息
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeBitmapInfo(
    JNIEnv* env, jclass clazz, jobject bitmap,
    jintArray infoOut) {

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return JNI_FALSE;

    jint* out = env->GetIntArrayElements(infoOut, nullptr);
    out[0] = info.width;
    out[1] = info.height;
    out[2] = info.stride;
    out[3] = info.format;
    env->ReleaseIntArrayElements(infoOut, out, 0);

    return JNI_TRUE;
}

// 快速颜色距离计算（在 RGB 空间中）
extern "C"
JNIEXPORT jfloat JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeColorDistance(
    JNIEnv* env, jclass clazz,
    jint r1, jint g1, jint b1,
    jint r2, jint g2, jint b2) {

    float dr = r1 - r2;
    float dg = g1 - g2;
    float db = b1 - b2;
    return sqrtf(dr * dr + dg * dg + db * db);
}

// 内存拷贝（零拷贝共享给 Kotlin）
extern "C"
JNIEXPORT void JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeMemcpy(
    JNIEnv* env, jclass clazz,
    jbyteArray src, jint srcOffset,
    jbyteArray dst, jint dstOffset, jint length) {

    jbyte* s = env->GetByteArrayElements(src, nullptr);
    jbyte* d = env->GetByteArrayElements(dst, nullptr);

    memcpy(d + dstOffset, s + srcOffset, length);

    env->ReleaseByteArrayElements(src, s, JNI_ABORT);
    env->ReleaseByteArrayElements(dst, d, 0);
}
