/**
 * native-lib.cpp
 *
 * JNI 入口文件 —— 兼容旧接口
 *
 * 这个文件保持简单，主要把 JNI 调用转发到
 * native_detect.cpp / native_physics.cpp 中的实现。
 *
 * 这样可以保持模块清晰：
 *   - native_detect.cpp  → 图像识别
 *   - native_physics.cpp → 物理计算
 *   - native-lib.cpp     → JNI 桥接（薄层）
 */

#include <jni.h>
#include <android/log.h>

#define TAG "LingMiaoJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

// ===== 加载时打印日志 =====
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    LOGI("LingMiao native library loaded ✅");
    return JNI_VERSION_1_6;
}

// ===== 测试函数（兼容旧代码） =====
extern "C"
JNIEXPORT jstring JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeTest(
    JNIEnv* env, jclass clazz) {
    return env->NewStringUTF("LingMiao Native v2.0 ✅");
}
