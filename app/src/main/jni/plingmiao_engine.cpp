/*
 * lingmiao_engine.cpp
 * 灵喵 LingMiao V2.0 — 自研 JNI 引擎
 *
 * 功能:
 *  - 球检测 (native 加速版 Hough Circle)
 *  - 透视变换 (NEON 加速)
 *  - 瞄准计算 (BFS 多库翻袋)
 *  - 力度补偿模型
 *
 * 编译: CMake + Android NDK (arm64-v8a)
 */

#include <jni.h>
#include <android/log.h>
#include <cmath>
#include <vector>
#include <algorithm>
#include <cstring>

#define TAG "LingMiaoEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─────────────────────────────────────────────
//  JNI 方法实现
// ─────────────────────────────────────────────

extern "C"
JNIEXPORT jstring JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_checkOpenCVVersion(
    JNIEnv* env, jclass) {
    return env->NewStringUTF("LingMiao Engine 2.0 (native build)");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_initEngine(
    JNIEnv* env, jclass, jstring configPath) {
    LOGI("Engine init (config path passed)");
    return JNI_TRUE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_releaseEngine(
    JNIEnv* env, jclass) {
    LOGI("Engine released");
}

// ── 球检测 ──
// 简化版霍夫圆检测 (CPU 参考实现, 实际可替换为 NEON 优化)
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_detectBalls(
    JNIEnv* env, jclass,
    jbyteArray pixelData, jint width, jint height,
    jint stride, jint mode) {

    jbyte* pixels = env->GetByteArrayElements(pixelData, nullptr);
    if (!pixels) return nullptr;

    // 这里放实际检测逻辑
    // 当前为 stub: 返回空结果, Kotlin 层会 fallback 到 CPU 模式
    // TODO: 接入 OpenCV HoughCircles / TFLite GPU delegate

    env->ReleaseByteArrayElements(pixelData, pixels, JNI_ABORT);
    return nullptr;  // 空结果 → Kotlin CPU fallback
}

// ── 透视变换 ──
extern "C"
JNIEXPORT void JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_setHomography(
    JNIEnv* env, jclass, jfloatArray matrix) {
    // TODO: 缓存 3x3 单应矩阵, 用于后续 NEON 加速变换
    jfloat* m = env->GetFloatArrayElements(matrix, nullptr);
    // 缓存到全局变量 ...
    env->ReleaseFloatArrayElements(matrix, m, JNI_ABORT);
}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_perspectiveTransform(
    JNIEnv* env, jclass, jfloatArray src) {

    jfloat* s = env->GetFloatArrayElements(src, nullptr);
    jsize len = env->GetArrayLength(src);

    // 简化: 直接返回输入 (TODO: 乘单应矩阵)
    jfloatArray result = env->NewFloatArray(len);
    env->SetFloatArrayRegion(result, 0, len, s);

    env->ReleaseFloatArrayElements(src, s, JNI_ABORT);
    return result;
}

// ── 瞄准计算 (BFS 多库翻袋) ──
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_engine_opencv_NativeLoader_calcAim(
    JNIEnv* env, jclass,
    jfloat cueX,    jfloat cueY,
    jfloat targetX, jfloat targetY,
    jfloat pocketX, jfloat pocketY,
    jint   maxBanks) {

    // TODO: 实现 BFS 反射搜索 + 力度补偿
    // 当前返回 null → Kotlin 层的 AimEngine 会用纯 Kotlin 版兜底

    return nullptr;
}

// ─────────────────────────────────────────────
//  JNI 动态注册 (可选, 提高加载速度)
// ─────────────────────────────────────────────
static const JNINativeMethod methods[] = {
    {"checkOpenCVVersion", "()Ljava/lang/String;",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_checkOpenCVVersion},
    {"initEngine",        "(Ljava/lang/String;)Z",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_initEngine},
    {"releaseEngine",     "()V",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_releaseEngine},
    {"detectBalls",       "([BIIII)[F",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_detectBalls},
    {"setHomography",    "([F)V",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_setHomography},
    {"perspectiveTransform", "([F)[F",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_perspectiveTransform},
    {"calcAim",          "(FFFFFFI)[F",
     (void*)Java_com_lingmiao_v2_engine_opencv_NativeLoader_calcAim},
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    jclass clazz = env->FindClass("com/lingmiao/v2/engine/opencv/NativeLoader");
    if (clazz) {
        env->RegisterNatives(clazz, methods,
                             sizeof(methods) / sizeof(methods[0]));
        LOGI("✅ JNI methods registered (%zu methods)",
              sizeof(methods) / sizeof(methods[0]));
    }
    LOGI("LingMiao Engine 2.0 loaded (stub build)");
    return JNI_VERSION_1_6;
}
